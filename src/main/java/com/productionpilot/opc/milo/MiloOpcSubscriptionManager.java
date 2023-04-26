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
import org.eclipse.milo.opcua.sdk.client.subscriptions.ManagedSubscription;
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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
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
    // Some OPC UA Servers have a limit on how many items can be subscribed to in a single subscription, this workaround
    // will split up a big subscription into multiple smaller ones if the number of items exceeds this limit
    private static final int MAX_ITEMS_PER_SUBSCRIPTION = 2500;

    private final MiloOpcConnection connection;

    private final Object subscriptionLock = new Object();

    private volatile UaClient client;

    private final Set<OpcSubscriptionImpl> subscriptions = ConcurrentHashMap.newKeySet();

    private final SubscribedItemHandleMap<OpcSubscribedItemImpl> subscribedItemsByHandle = new SubscribedItemHandleMap<>();

    private final LinkedBlockingDeque<OpcSubscriptionImpl> subscriptionsToCreate = new LinkedBlockingDeque<>(),
                                                            subscriptionsToUnsubscribe = new LinkedBlockingDeque<>(),
                                                            subscriptionsToResubscribe = new LinkedBlockingDeque<>();

    public MiloOpcSubscriptionManager(MiloOpcConnection connection) {
        this.connection = connection;
        // Start the thread, which will create the subscriptions on the server
        new SubscriptionManagerThread().start();
    }

    @Override
    public OpcSubscription subscribe(OpcSubscriptionRequest request) {
        // Create a new subscription, but just the "empty shell" of it
        var subscription = new OpcSubscriptionImpl();
        for(int i = 0; i < request.getNodes().size(); i++) {
            var nodeId = request.getNodes().get(i);
            var samplingInterval = request.getSamplingIntervals().get(i);
            var listener = request.getListeners().get(i);
            var subscribedItem = new OpcSubscribedItemImpl(subscription, samplingInterval, listener);
            subscribedItem.node = request.getNodes().get(i);
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
                    subscriptionsToResubscribe.add(subscription);
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
                        if(MiloOpcSubscriptionManager.this.client == null || (subscriptionsToUnsubscribe.isEmpty()
                                && subscriptionsToCreate.isEmpty() && subscriptionsToResubscribe.isEmpty())) {
                            // Without a client and any tasks there's nothing to do, so we can just wait
                            subscriptionLock.wait();
                        }
                    }
                    performQueueAction(subscriptionsToUnsubscribe, this::performUnsubscribe);
                    client = MiloOpcSubscriptionManager.this.client;
                    if(client != null) {
                        performQueueAction(subscriptionsToCreate, subscription -> performSubscribe(client, subscription));
                        performQueueAction(subscriptionsToResubscribe, subscription -> {
                            try {
                                performUnsubscribe(subscription);
                            } catch (Exception e) {
                                log.debug("Error while unsubscribing from {}, ignoring", subscription, e);
                            }
                            performSubscribe(client, subscription);
                        });
                    }
                } catch (Exception e) {
                    log.error("Uncaught Exception in subscription management thread", e);
                }
            }
        }

        private static <T> void performQueueAction(BlockingDeque<T> queue, Consumer<T> action) {
            // Take the head from the queue, perform the action, if it throws, put it back at the end
            T head = queue.pollFirst();
            if(head != null) {
                try {
                    action.accept(head);
                } catch (Exception e) {
                    log.debug("Error while performing action on {}, putting it back in the queue", head, e);
                    queue.addLast(head);
                }
            }
        }

        @SneakyThrows({ExecutionException.class, InterruptedException.class})
        private void performSubscribe(@Nonnull UaClient client, @Nonnull OpcSubscriptionImpl subscription) {
            if (subscription.uaClient != null) {
                log.debug("Subscription {} is already subscribed", subscription);
                return;
            }
            if(subscription.subscribedItems.isEmpty()) {
                log.debug("Subscription {} has no items to subscribe to", subscription);
                return;
            }
            if(subscription.scheduleDestroy) {
                log.debug("Subscription {} has been destroyed, not subscribing", subscription);
                return;
            }
            // Reserve handles for all items
            subscribedItemsByHandle.addAllAndAssignHandles(subscription.subscribedItems);
            List<List<OpcSubscribedItemImpl>> itemGroups = new ArrayList<>();
            for (int i = 0; i < subscription.subscribedItems.size(); i += MAX_ITEMS_PER_SUBSCRIPTION) {
                itemGroups.add(subscription.subscribedItems.subList(i,
                        Math.min(i + MAX_ITEMS_PER_SUBSCRIPTION, subscription.subscribedItems.size())));
            }
            subscription.uaClient = client;
            for (List<OpcSubscribedItemImpl> itemGroup : itemGroups) {
                if(itemGroups.size() == 1) {
                    log.debug("Creating subscription for {} items", itemGroup.size());
                } else {
                    log.debug("Creating partial subscription {} of {} for {} items",
                            itemGroups.indexOf(itemGroup) + 1, itemGroups.size(), itemGroup.size());
                }
                // See https://reference.opcfoundation.org/v104/Core/docs/Part4/5.12.1/
                double requestedPublishingInterval = itemGroup.stream()
                        .map(OpcSubscribedItemImpl::getSamplingInterval)
                        .map(Duration::toMillis)
                        .min(Long::compareTo)
                        .get().doubleValue() / 1000.0;

                // Refresh Node IDs with type information
                if(itemGroup.stream().anyMatch(item -> item.node.getType().isUndetermined())) {
                    log.debug("Refreshing node IDs for {} items", itemGroup.size());
                    var nodeIds = connection.getNodesFromNodeIds(itemGroup.stream().map(item -> item.node.getId())
                            .collect(Collectors.toList()));
                    for (int i = 0; i < itemGroup.size(); i++) {
                        var item = itemGroup.get(i).node = nodeIds.get(i);
                    }
                }

                var uaSubscription = subscription.uaClient.getSubscriptionManager()
                        .createSubscription(requestedPublishingInterval).get();
                itemGroup.forEach(item -> item.uaSubscription = uaSubscription);

                log.debug("Creating monitored items for {} items", itemGroup.size());
                var monitoredItems = uaSubscription.createMonitoredItems(TimestampsToReturn.Both,
                        itemGroup.stream().map(subscribedItem -> {
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
                            var subscribedItem = itemGroup.get(i);
                            monitoredItem.setValueConsumer(MiloOpcSubscriptionManager.this);
                            /* The monitoredItem statusCode can not be relied on, as it may be "Good" even if the
                             * item's value status is Bad. Instead, we set it to Bad_NoData initially, and when the
                             * first value arrives, it is updated accordingly. */
                            /*subscribedItem.statusCode = MiloOpcTypeMapper.mapStatusCode(Optional.ofNullable(
                                            monitoredItem.getStatusCode()).map(StatusCode::getValue)
                                    .orElse(StatusCodes.Bad_NoData)); */
                            subscribedItem.statusCode = MiloOpcTypeMapper.mapStatusCode(StatusCodes.Bad_NoData);
                        }).get();
                if (monitoredItems.size() != itemGroup.size()) {
                    throw new IllegalStateException("Failed to create all monitored items");
                }
            }
            log.debug("Created subscription(s) {}", subscription);
        }

        private void performUnsubscribe(@Nonnull OpcSubscriptionImpl subscription) {
            log.debug("Unsubscribing from {}", subscription);
            if (subscription.subscribedItems.isEmpty()) {
                log.debug("Subscription {} has no items to unsubscribe from", subscription);
                return;
            }
            if(subscription.uaClient == null) {
                log.debug("Subscription {} is not subscribed, can't unsubscribe", subscription);
                return;
            }
            // A subscription may be broken up into several parts. This gets all parts, and then cancels each partial
            // subscription.
            subscription.subscribedItems.stream().map(item -> item.uaSubscription)
                    .collect(Collectors.toSet())
                .forEach(uaSubscription -> {
                    if(uaSubscription == null) {
                        return;
                    }
                    try {
                        subscription.uaClient.getSubscriptionManager()
                                .deleteSubscription(uaSubscription.getSubscriptionId()).get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.debug("Error while unsubscribing from {}, ignoring", subscription, e);
                    }
                });
            subscription.uaClient = null;
            synchronized (subscriptionLock) {
                subscribedItemsByHandle.removeAll(subscription.subscribedItems);
            }
        }
    }

    private void queueSubscribe(@Nonnull OpcSubscriptionImpl subscription) {
        log.debug("Queueing subscription {}", subscription);
        synchronized (subscriptionLock) {
            subscriptionsToCreate.add(subscription);
            subscriptionLock.notifyAll();
        }
    }

    private void queueDestroy(@Nonnull OpcSubscriptionImpl subscription) {
        log.debug("Queueing destroy of subscription {}", subscription);
        synchronized (subscriptionLock) {
            // Prevents any queued subscribe from being executed
            subscription.scheduleDestroy = true;
            subscriptionsToUnsubscribe.add(subscription);
            subscriptionLock.notifyAll();
        }
    }

    private void queueResubscribe(@Nonnull OpcSubscriptionImpl subscription) {
        log.debug("Queueing resubscribe of subscription {}", subscription);
        synchronized (subscriptionLock) {
            subscriptionsToResubscribe.add(subscription);
            subscriptionLock.notifyAll();
        }
    }

    @Override
    public void onValueArrived(UaMonitoredItem item, DataValue wrappedValue) {
        var subscribedItem = subscribedItemsByHandle.getByHandle(item.getClientHandle().longValue());
        if(subscribedItem == null) {
            // This can happen quite often for large subscriptions that were just destroyed
            log.trace("Received value for unknown item: {}", item);
            return;
        }
        subscribedItem.statusCode = MiloOpcTypeMapper.mapStatusCode(
                Optional.ofNullable(wrappedValue.getStatusCode()).orElse(StatusCode.BAD));
        var measuredValue = MiloOpcTypeMapper.mapMeasuredValue(subscribedItem.node, wrappedValue, Instant.now());
        if(measuredValue != null) {
            subscribedItem.updateCount.incrementAndGet();
            subscribedItem.lastValue = measuredValue;
            // If the subscription is queued for resubscribe, remove it from the queue
            // Because if we're receiving values, it's not broken
            if(subscribedItem.statusCode.isGood()) {
                synchronized (subscriptionLock) {
                    subscriptionsToResubscribe.removeIf(sub -> sub.equals(subscribedItem.subscription));
                }
            }
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
            resubscribeIfExists(uaSubscription);
        }
    }

    @Override
    public void onNotificationDataLost(UaSubscription uaSubscription) {
        log.warn("Subscription {} lost notification data", uaSubscription.getSubscriptionId());
    }

    @Override
    public void onSubscriptionTransferFailed(UaSubscription uaSubscription, StatusCode statusCode) {
        log.debug("Subscription {} transfer failed: {}, resubscribing", uaSubscription.getSubscriptionId(), statusCode);
        resubscribeIfExists(uaSubscription);
    }

    @Override
    public void onSubscriptionWatchdogTimerElapsed(UaSubscription uaSubscription) {
        log.debug("Subscription {} watchdog timer elapsed", uaSubscription.getSubscriptionId());
        resubscribeIfExists(uaSubscription);
    }

    private void resubscribeIfExists(UaSubscription uaSubscription) {
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
        if(!subscriptions.contains(subscription)) {
            queueDestroy(subscription);
        } else {
            queueResubscribe(subscription);
        }
    }

    @RequiredArgsConstructor
    private class OpcSubscriptionImpl implements OpcSubscription {
        private final List<OpcSubscribedItemImpl> subscribedItems = new ArrayList<>();
        private UaClient uaClient;
        private volatile boolean scheduleDestroy = false;

        @Override
        @SneakyThrows
        public void unsubscribe() {
            subscriptions.remove(this);
            MiloOpcSubscriptionManager.this.queueDestroy(this);
        }

        @Override
        public List<OpcSubscribedItem> getSubscribedItems() {
            return subscribedItems.stream().map(item -> (OpcSubscribedItem) item).toList();
        }

        public String toString() {
            var limit = 3;
            return "OpcSubscriptionImpl{" + subscribedItems.size() + " item(s): "
                    + subscribedItems.stream()
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
        private UaSubscription uaSubscription;
        @Override
        public long getUpdateCount() {
            return updateCount.get();
        }
    }
}
