/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.service;

import com.productionpilot.db.timescale.entities.Measurement;
import com.productionpilot.db.timescale.entities.Parameter;
import com.productionpilot.db.timescale.service.MeasurementService;
import com.productionpilot.db.timescale.service.ParameterService;
import com.productionpilot.db.timescale.service.event.EntityCreatedEvent;
import com.productionpilot.db.timescale.service.event.EntityDeletedEvent;
import com.productionpilot.db.timescale.service.event.EntityUpdatedEvent;
import com.productionpilot.opc.*;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParameterRecordingService implements OpcSubscriptionListener {
    /**
     * The OPC UA server does not always respect the sampling interval, e.g. by delivering new values every 200 ms when
     * we set a 1000 ms sampling interval. Thus, we enforce it by dropping values that arrive too quickly.
     * To avoid dropping values that arrive a bit early (e.g., two values arrive 950 ms apart at a sampling interval of
     * 1000 ms), we add a tolerance.
     * This choice of 90 ms should still filter out early values arriving at a 100 ms rate.
     */
    private static final Duration EARLY_PARAMETER_DROP_TOLERANCE = Duration.ofMillis(90);

    private final OpcService opcService;
    private final MeasurementService measurementService;
    private final ParameterService parameterService;

    private final Map<Parameter, ParameterRecording> parameterRecordingsMap = new ConcurrentHashMap<>();

    private final Map<OpcNodeId, List<Parameter>> nodeIdParameterMap = new ConcurrentHashMap<>();

    // Only accessed from @Synchronized methods
    private final Set<Parameter> parametersToRecord = new HashSet<>();

    @PostConstruct
    @Synchronized
    private void init() {
        parametersToRecord.addAll(parameterService.findAll());
        updateRecordingParameters();
    }

    /**
     * Updates the parameter recording subscriptions. Non-existent parameters will no longer be recorded, and new ones
     * will start recording.
     */
    @Synchronized
    private void updateRecordingParameters() throws OpcException {
        log.debug("Updating recording parameters: {} parameters to record", parametersToRecord.size());
        // first, subscribe to all new parameters
        var newParameters = new ArrayList<Parameter>(parametersToRecord.size());
        var newNodes = new ArrayList<OpcNodeId>(parametersToRecord.size());
        // Iterate through all parameters - ignore the already subscribed ones, and add the new ones to the list
        for(Parameter parameter : parametersToRecord) {
            if(parameterRecordingsMap.containsKey(parameter)) {
                continue;
            }
            OpcNodeId nodeId = null;
            try {
                nodeId = opcService.getParameterRecordingConnection().parseNodeId(parameter.getOpcNodeId());
            } catch (OpcException e) {
                log.error("Could not parse node ID for parameter {}, this parameter will not be recorded",
                        parameter, e);
                parametersToRecord.remove(parameter);
                continue;
            }
            newParameters.add(parameter);
            newNodes.add(nodeId);
        }
        // then, subscribe to all new parameters
        for(int i = 0; i < newParameters.size(); i++) {
            var nodeId = newNodes.get(i);
            var parameter = newParameters.get(i);
            var parameterSubscription = new ParameterRecording(parameter, nodeId,
                    new AtomicLong(measurementService.countByParameter(parameter)));
            parameterRecordingsMap.put(parameter, parameterSubscription);
            nodeIdParameterMap.computeIfAbsent(nodeId, n -> new ArrayList<>()).add(parameter);
            parameterSubscription.lastMeasurement = measurementService.getLastMeasurement(parameter);
        }
        for (Parameter parameter : newParameters) {
            var parameterSubscription = parameterRecordingsMap.get(parameter);
            if(parameterSubscription != null) {
                log.debug("Subscribing to parameter {}", parameter);
                parameterSubscription.opcSubscription = opcService.getParameterRecordingConnection().getSubscriptionManager()
                        .subscribe(parameterSubscription.nodeId, parameter.getSamplingInterval(),
                                ParameterRecordingService.this);
            }
        }
        // then, unsubscribe from all old parameters
        var iterator = parameterRecordingsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var parameter = entry.getKey();
            var subscription = entry.getValue();
            if (!parametersToRecord.contains(parameter)) {
                if (subscription.opcSubscription != null) {
                    subscription.opcSubscription.unsubscribe();
                }
                iterator.remove();
                nodeIdParameterMap.remove(subscription.nodeId);
            }
        }
        log.debug("Now recording {} parameters", parametersToRecord.size());
    }

    @RequiredArgsConstructor
    private static class ParameterRecording {
        @Nonnull
        private final Parameter parameter;
        @Nonnull
        private final OpcNodeId nodeId;
        @Nullable
        private OpcSubscription opcSubscription = null;
        @Nonnull
        private final AtomicLong measurementCount;
        @Nullable
        private volatile Measurement lastMeasurement = null;
    }

    @Override
    public void onSubscriptionActive(OpcSubscription subscription) {
        var nodeId = subscription.getSubscribedItems().get(0).getNode().getId();
        var parameterSubscription = nodeIdParameterMap.get(nodeId).stream()
                .map(parameterRecordingsMap::get)
                .filter(parameterRecordingCandidate -> parameterRecordingCandidate.opcSubscription == subscription)
                .findFirst().orElse(null);
        if(parameterSubscription == null) {
            log.warn("Received onSubscriptionActive for unknown subscription: {}", subscription);
            return;
        }
        var parameter = parameterSubscription.parameter;
        var node = subscription.getSubscribedItems().get(0).getNode();
        if(parameterSubscription.opcSubscription == subscription) {
            if(node.getType().isUndetermined()) {
                log.warn("OPC Node type \"{}\" for Parameter {} could not be determined",
                        node.getType(), parameter);
            } else if(!node.getType().isFound()) {
                log.warn("OPC Node \"{}\" for Parameter {} does not exist", node, parameter);
                return;
            } else if(!node.getType().isVariable()) {
                log.warn("OPC Node {} for Parameter {} is not a variable, cannot record it", node, parameter);
                return;
            }
            log.debug("Subscription for parameter {} is now active", parameter);
            // If the node has no value yet, we read it once to get the initial value
            if(parameterSubscription.lastMeasurement == null) {
                log.debug("Reading initial value of parameter {} (node {})", parameter, node);
                CompletableFuture.runAsync(() -> {
                    try {
                        var value = opcService.getParameterRecordingConnection().read(node);
                        if (value != null) {
                            onVariableUpdate(value);
                        }
                    } catch (OpcException e) {
                        log.warn("Error while reading initial value of parameter {}, node {}", parameter, node, e);
                    }
                });
            }
        }
    }

    @Override
    public void onVariableUpdate(OpcMeasuredValue value) {
        var parameters = nodeIdParameterMap.get(value.getNode().getId());
        if(parameters == null) {
            log.warn("Received OPC value for unknown node {}", value.getNode());
            return;
        }
        for(Parameter parameter : parameters) {
            var subscription = parameterRecordingsMap.get(parameter);
            if (subscription == null) {
                log.warn("Missing ParameterSubscription for node: {} and parameter: {}", value.getNode(), parameter);
                continue;
            }
            var lastMeasurement = subscription.lastMeasurement;
            if (lastMeasurement != null && value.getClientTime().isBefore(lastMeasurement.getClientTime().plus(
                            parameter.getSamplingInterval()).minus(EARLY_PARAMETER_DROP_TOLERANCE))) {
                // Ignore values that are too early
                continue;
            }
            subscription.lastMeasurement = measurementService.recordMeasurement(parameter, value);
            subscription.measurementCount.incrementAndGet();
        }
    }

    @EventListener
    @Synchronized
    public void onParameterCreated(EntityCreatedEvent<Parameter> event) {
        log.debug("Parameter {} was created, updating recording parameters", event.getEntity());
        parametersToRecord.add(event.getEntity());
        updateRecordingParameters();
    }

    @EventListener
    @Synchronized
    public void onParameterUpdated(EntityUpdatedEvent<Parameter> event) {
        log.debug("Parameter {} was updated, updating recording parameters", event.getEntity());
        parametersToRecord.removeIf(p -> p.getId().equals(event.getEntity().getId()));
        parametersToRecord.add(event.getEntity());
        updateRecordingParameters();
    }

    @EventListener
    @Synchronized
    public void onParameterDeleted(EntityDeletedEvent<Parameter> event) {
        log.debug("Parameter {} was deleted, updating recording parameters", event.getEntity());
        parametersToRecord.removeIf(p -> p.getId().equals(event.getEntity().getId()));
        updateRecordingParameters();
    }

    public OpcStatusCode getStatusCode(Parameter parameter) {
        return Optional.ofNullable(getSubscribedItem(parameter))
                .map(OpcSubscribedItem::getStatusCode)
                .orElse(OpcStatusCode.BAD_UNEXPECTED_ERROR);
    }

    public Map<Parameter, OpcSubscribedItem> listSubscribedItems() {
        return parameterRecordingsMap.entrySet().stream()
                .filter(e -> e.getValue().opcSubscription != null)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().opcSubscription.getSubscribedItems().get(0)));
    }

    public OpcSubscribedItem getSubscribedItem(Parameter parameter) {
        return Optional.ofNullable(parameterRecordingsMap.get(parameter))
                .map(subscription -> subscription.opcSubscription)
                .map(OpcSubscription::getSubscribedItems)
                .map(items -> items.get(0))
                .orElse(null);
    }

    public long getMeasurementCount(Parameter parameter) {
        return Optional.ofNullable(parameterRecordingsMap.get(parameter))
                .map(subscription -> subscription.measurementCount.get())
                .orElse(0L);
    }

    public Measurement getLastMeasurement(Parameter parameter) {
        return Optional.ofNullable(parameterRecordingsMap.get(parameter))
                .map(subscription -> subscription.lastMeasurement)
                .orElse(null);
    }
}
