/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import java.time.Duration;

/**
 * The OpcSubscriptionManager interface provides methods for subscribing to OPC UA nodes and managing OPC UA subscriptions.
 *
 * <p>Implementations of this interface should provide a way to subscribe to OPC UA nodes by specifying the node ID, the reporting interval, and a listener that will be notified when the value of the node changes. The listener can be used to perform custom actions when the value of the node changes, such as updating a user interface or triggering an alarm.</p>
 *
 * <p>The {@code subscribe} method returns an {@link OpcSubscription} object that can be used to manage the subscription, such as removing the subscription.</p>
 *
 * <p>Implementations of this interface should also provide a way to subscribe to multiple nodes at once by using an {@link OpcSubscriptionRequest} object.</p>
 */
public interface OpcSubscriptionManager {

    /**
     * Subscribes to an OPC UA node with the given ID, reporting interval, and listener.
     *
     * @param nodeId the ID of the node to subscribe to
     * @param reportingInterval the reporting interval for the subscription
     * @param listener the listener to notify when the value of the node changes
     * @return an {@link OpcSubscription} object that can be used to manage the subscription
     */
    default OpcSubscription subscribe(OpcNodeId nodeId, Duration reportingInterval, OpcSubscriptionListener listener) {
        return subscribe(OpcSubscriptionRequest.builder()
                .addNode(nodeId, reportingInterval, listener)
                .build());
    }

    /**
     * Subscribes to an OPC UA node with the given node object, reporting interval, and listener.
     *
     * @param node the node to subscribe to
     * @param reportingInterval the reporting interval for the subscription
     * @param listener the listener to notify when the value of the node changes
     * @return an {@link OpcSubscription} object that can be used to manage the subscription
     */
    default OpcSubscription subscribe(OpcNode node, Duration reportingInterval, OpcSubscriptionListener listener) {
        return subscribe(node.getId(), reportingInterval, listener);
    }

    /**
     * Subscribes to multiple OPC UA nodes with the given subscription request.
     *
     * @param request the subscription request
     * @return an {@link OpcSubscription} object that can be used to manage the subscription
     */
    OpcSubscription subscribe(OpcSubscriptionRequest request);
}
