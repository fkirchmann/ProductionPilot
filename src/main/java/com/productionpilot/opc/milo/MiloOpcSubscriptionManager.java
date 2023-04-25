/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc.milo;

import com.productionpilot.opc.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.api.UaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

@Slf4j
public class MiloOpcSubscriptionManager implements UaMonitoredItem.ValueConsumer,
        UaSubscriptionManager.SubscriptionListener, OpcSubscriptionManager {
    // This is used to calculate the OPC-server side queue size for each individual monitored tag
    // This configures how many milliseconds of item values th e server should keep in memory
    private static final int QUEUE_SIZE_MS = 5000;
    // Regardless of the queue size, the OPC server queue will never be smaller than this
    private static final int MINIMUM_QUEUE_SIZE = 5;

    private final MiloOpcConnection connection;

    private final Object subscriptionLock = new Object();

    private volatile UaClient client;

    private final Set<OpcSubscriptionImpl> subscriptions = ConcurrentHashMap.newKeySet();

    private final SubscribedItemHandleMap<OpcSubscribedItemImpl> subscribedItemsByHandle = new SubscribedItemHandleMap<>();

    private final LinkedBlockingDeque<OpcSubscriptionImpl> subscriptionsToCreate = new LinkedBlockingDeque<>(),
                                                            subscriptionsToDestroy = new LinkedBlockingDeque<>();

    public MiloOpcSubscriptionManager(MiloOpcConnection connection) {
        this.connection = connection;
        // Start the thread, which will create the subscriptions on the server
        new SubscriptionManagerThread().start();
    }

    @Override
    public OpcSubscription subscribe(OpcSubscriptionRequest request) {
        // Create a new subscription, but just the "empty shell" of it
        var subscription = new OpcSubscriptionImpl();
        for(int i = 0; i < request.getNodeIds().size(); i++) {
            var nodeId = request.getNodeIds().get(i);
            var samplingInterval = request.getSamplingIntervals().get(i);
            var listener = request.getListeners().get(i);
            var subscribedItem = new OpcSubscribedItemImpl(subscription, samplingInterval, listener);
            subscribedItem.node = MiloOpcNode.newPlaceholder(connection, MiloOpcNodeId.from(nodeId));
            subscription.subscribedItems.add(subscribedItem);
        }
        // Notify the thread, so that it can create the subscription on the server
        subscriptions.add(subscription);
        queueSubscribe(subscription);
        return subscription;
    }

    void setClient(@Nonnull UaClient newUaClient) {
        synchronized (subscriptionLock) {
            if(this.client == newUaClient) {
                return;
            } else if(this.client != null) {
                // Remove the listener from the old client
                this.client.getSubscriptionManager().removeSubscriptionListener(this);
            }
            this.client = newUaClient;
            this.client.getSubscriptionManager().addSubscriptionListener(this);
            // Move all subscriptions to the new connection
            for(var subscription : subscriptions) {
                if(subscription.uaClient != newUaClient) {
                    subscriptionsToDestroy.add(subscription);
                    subscriptionsToCreate.add(subscription);
                }
            }
            subscriptionLock.notifyAll();
        }
    }

    private class SubscriptionManagerThread extends Thread implements UaSubscriptionManager.SubscriptionListener {
        public SubscriptionManagerThread() {
            super("MiloOpcSubscriptionManager");
            setDaemon(true);
        }

        public void run() {
            while (true) {
                try {
                    UaClient client;
                    OpcSubscriptionImpl subscriptionToCreate, subscriptionToDestroy;
                    synchronized (subscriptionLock) {
                        subscriptionToCreate = subscriptionsToCreate.peekFirst();
                        subscriptionToDestroy = subscriptionsToDestroy.peekFirst();
                        client = MiloOpcSubscriptionManager.this.client;
                        // Prioritize destroying subscriptions before creating new ones
                        if (subscriptionToDestroy != null) {
                            subscriptionsToDestroy.removeFirst();
                        } else if (subscriptionToCreate != null && client != null) {
                            subscriptionsToCreate.removeFirst();
                        } else {
                            // Nothing to do, so we can just wait
                            subscriptionLock.wait();
                        }
                    }
                    if (subscriptionToDestroy != null) {
                        try {
                            performUnsubscribe(subscriptionToDestroy);
                        } catch (ExecutionException e) {
                            log.debug("Error while destroying subscription {}, putting it back in the queue",
                                    subscriptionToDestroy);
                            subscriptionsToDestroy.addLast(subscriptionToDestroy);
                        }
                    } else if (subscriptionToCreate != null && client != null) {
                        try {
                            performSubscribe(client, subscriptionToCreate);
                        } catch (ExecutionException e) {
                            log.debug("Error while creating subscription {}, putting it back in the queue",
                                    subscriptionToCreate);
                            subscriptionsToCreate.addLast(subscriptionToCreate);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error while creating subscription", e);
                }
            }
        }

        private void performSubscribe(@Nonnull UaClient client, @Nonnull OpcSubscriptionImpl subscription)
                throws ExecutionException, InterruptedException {
            if (subscription.subscribed || subscription.subscribedItems.isEmpty()) {
                log.debug("Subscription {} is already subscribed or has no items", subscription);
                // Nothing to do
                return;
            }
            log.debug("Creating subscription {}", subscription);
            // See https://reference.opcfoundation.org/v104/Core/docs/Part4/5.12.1/
            double requestedPublishingInterval = subscription.subscribedItems.stream()
                    .map(OpcSubscribedItemImpl::getSamplingInterval)
                    .map(Duration::toMillis)
                    .min(Long::compareTo)
                    .get().doubleValue() / 1000.0;

            subscription.uaClient = client;
            subscription.uaSubscription = subscription.uaClient.getSubscriptionManager()
                    .createSubscription(requestedPublishingInterval).get();
            subscribedItemsByHandle.addAllAndAssignHandles(subscription.subscribedItems);
            subscription.subscribed = true;

            var monitoredItems = subscription.uaSubscription.createMonitoredItems(TimestampsToReturn.Both,
                    subscription.subscribedItems.stream().map(subscribedItem -> {
                                var nodeId = NodeId.parse(subscribedItem.node.getId().toParseableString());
                                var queueSize = Math.min(MINIMUM_QUEUE_SIZE,
                                        QUEUE_SIZE_MS / subscribedItem.samplingInterval.toMillis());
                                var readValueId = new ReadValueId(
                                        nodeId,
                                        AttributeId.Value.uid(),
                                        null,
                                        QualifiedName.NULL_VALUE);
                                var monitoringParameters = new MonitoringParameters(
                                        uint(subscribedItem.getHandle()),
                                        subscribedItem.samplingInterval.toMillis() / 1000.0,
                                        null,
                                        uint(queueSize),
                                        true
                                );
                                return new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting,
                                        monitoringParameters);
                            })
                            .toList(),
                    (monitoredItem, i) -> {
                        var subscribedItem = subscription.subscribedItems.get(i);
                        monitoredItem.setValueConsumer(MiloOpcSubscriptionManager.this);
                        subscribedItem.statusCode = MiloOpcTypeMapper.mapStatusCode(Optional.ofNullable(
                                        monitoredItem.getStatusCode()).map(StatusCode::getValue)
                                .orElse(StatusCodes.Bad_NoData));
                    }).get();
            if (monitoredItems.size() != subscription.subscribedItems.size()) {
                throw new IllegalStateException("Failed to create all monitored items");
            }
            log.debug("Created subscription {}", subscription);
        }

        private void performUnsubscribe(@Nonnull OpcSubscriptionImpl subscription)
                throws ExecutionException, InterruptedException {
            log.debug("Unsubscribing from {}", subscription);
            if (!subscription.subscribed || subscription.subscribedItems.isEmpty()) {
                return;
            }
            subscription.uaClient.getSubscriptionManager().deleteSubscription(
                subscription.uaSubscription.getSubscriptionId()).get();

            subscribedItemsByHandle.removeAll(subscription.subscribedItems);
            subscription.subscribed = false;
            subscription.uaSubscription = null;
            subscription.uaClient = null;
        }
    }

    private void queueSubscribe(@Nonnull OpcSubscriptionImpl subscription) {
        synchronized (subscriptionLock) {
            subscriptionsToCreate.add(subscription);
            subscriptionLock.notifyAll();
        }
    }

    private void queueUnsubscribe(@Nonnull OpcSubscriptionImpl subscription) {
        synchronized (subscriptionLock) {
            subscriptionsToDestroy.add(subscription);
            subscriptionLock.notifyAll();
        }
    }

    @Override
    public void onValueArrived(UaMonitoredItem item, DataValue wrappedValue) {
        var subscribedItem = subscribedItemsByHandle.getByHandle(item.getClientHandle().longValue());
        if(subscribedItem == null) {
            log.warn("Received value for unknown item: {}", item);
            return;
        }
        subscribedItem.statusCode = MiloOpcTypeMapper.mapStatusCode(
                Optional.ofNullable(wrappedValue.getStatusCode()).orElse(StatusCode.BAD));
        var measuredValue = MiloOpcTypeMapper.mapMeasuredValue(subscribedItem.node, wrappedValue, Instant.now());
        if(measuredValue != null) {
            subscribedItem.updateCount.incrementAndGet();
            subscribedItem.lastValue = measuredValue;
            var listener = subscribedItem.listener;
            if(listener != null) {
                listener.onVariableUpdate(measuredValue);
            }
        }
    }

    @Override
    public void onStatusChanged(UaSubscription uaSubscription, StatusCode status) {
        log.debug("Subscription {} status changed to {}", uaSubscription.getSubscriptionId(), status);
        if(!status.isGood()) {
            log.warn("Subscription {} status changed to non-good value: {}, resubscribing", uaSubscription.getSubscriptionId(), status);
            resubscribe(uaSubscription);
        }
    }

    @Override
    public void onNotificationDataLost(UaSubscription uaSubscription) {
        log.warn("Subscription {} lost notification data", uaSubscription.getSubscriptionId());
    }

    @Override
    public void onSubscriptionTransferFailed(UaSubscription uaSubscription, StatusCode statusCode) {
        log.debug("Subscription {} transfer failed: {}, resubscribing", uaSubscription.getSubscriptionId(), statusCode);
        resubscribe(uaSubscription);
    }

    @Override
    public void onSubscriptionWatchdogTimerElapsed(UaSubscription uaSubscription) {
        log.debug("Subscription {} watchdog timer elapsed, resubscribing", uaSubscription.getSubscriptionId());
        resubscribe(uaSubscription);
    }

    private void resubscribe(UaSubscription uaSubscription) {
        log.debug("Resubscribing for subscription {}", uaSubscription.getSubscriptionId());
        var monitoredItems = uaSubscription.getMonitoredItems();
        if(monitoredItems.isEmpty()) {
            log.warn("Subscription {} has no monitored items", uaSubscription.getSubscriptionId());
            return;
        }
        var subscribedItem = subscribedItemsByHandle.getByHandle(monitoredItems.get(0).getClientHandle().longValue());
        if(subscribedItem == null) {
            log.warn("Could not find subscribedItem for Subscription {} and ClientHandle {}", uaSubscription.getSubscriptionId(), monitoredItems.get(0).getClientHandle());
            return;
        }
        var subscription = subscribedItem.subscription;
        for(var item : subscription.subscribedItems) {
            item.statusCode = OpcStatusCode.BAD;
        }
        queueUnsubscribe(subscription);
        if(subscriptions.contains(subscription)) {
            queueSubscribe(subscription);
        }
    }

    @RequiredArgsConstructor
    private class OpcSubscriptionImpl implements OpcSubscription {
        private final List<OpcSubscribedItemImpl> subscribedItems = new ArrayList<>();
        private UaClient uaClient;
        private UaSubscription uaSubscription;
        private volatile boolean subscribed = false;

        @Override
        @SneakyThrows
        public void unsubscribe() {
            subscriptions.remove(this);
            MiloOpcSubscriptionManager.this.queueUnsubscribe(this);
        }

        @Override
        public List<OpcSubscribedItem> getSubscribedItems() {
            return subscribedItems.stream().map(item -> (OpcSubscribedItem) item).toList();
        }

        public String toString() {
            var limit = 3;
            return "OpcSubscriptionImpl{" +
                    "subscribedItems=" + subscribedItems.stream()
                    .limit(limit)
                    .map(item -> item.node.getId().toParseableString())
                                    .collect(Collectors.joining(", "))
                    + (subscribedItems.size() > limit ? ", ..." : "")
                    + '}';
        }
    }

    @RequiredArgsConstructor
    private static class OpcSubscribedItemImpl extends SubscribedItemHandleMap.HasHandle implements OpcSubscribedItem {
        private final OpcSubscriptionImpl subscription;
        @Getter
        private volatile OpcNode node;
        @Getter
        private final Duration samplingInterval;
        @Getter
        private final OpcSubscriptionListener listener;
        private final AtomicLong updateCount = new AtomicLong(0);
        @Getter
        private volatile OpcStatusCode statusCode = OpcStatusCode.BAD;
        @Getter
        private volatile OpcMeasuredValue lastValue = null;
        @Override
        public long getUpdateCount() {
            return updateCount.get();
        }
    }
}
