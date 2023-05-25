/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import java.time.Duration;

public interface OpcSubscriptionManager {
    default OpcSubscription subscribe(OpcNode node, Duration reportingInterval, OpcSubscriptionListener listener) {
        return subscribe(node.getId(), reportingInterval, listener);
    }

    default OpcSubscription subscribe(OpcNodeId nodeId, Duration reportingInterval, OpcSubscriptionListener listener) {
        return subscribe(OpcSubscriptionRequest.builder()
                .addNode(nodeId, reportingInterval, listener)
                .build());
    }

    OpcSubscription subscribe(OpcSubscriptionRequest request);
}
