/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class OpcSubscriptionRequest {
    private final List<OpcNodeId> nodeIds;
    private final List<Duration> samplingIntervals;
    private final List<OpcSubscriptionListener> listeners;

    public static OpcSubscriptionRequestBuilder builder() {
        return new OpcSubscriptionRequestBuilder();
    }

    public static class OpcSubscriptionRequestBuilder {
        private final List<OpcNodeId> nodeIds = new ArrayList<>();
        private final List<Duration> samplingIntervals = new ArrayList<>();
        private final List<OpcSubscriptionListener> listeners = new ArrayList<>();
        private OpcSubscriptionRequestBuilder() {}

        public OpcSubscriptionRequestBuilder addNode(OpcNode node, Duration samplingInterval,
                                                     OpcSubscriptionListener listener) {
            nodeIds.add(node.getId());
            samplingIntervals.add(samplingInterval);
            listeners.add(listener);
            return this;
        }

        public OpcSubscriptionRequestBuilder addNode(OpcNodeId nodeId, Duration samplingInterval,
                                                     OpcSubscriptionListener listener) {
            nodeIds.add(nodeId);
            samplingIntervals.add(samplingInterval);
            listeners.add(listener);
            return this;
        }

        public OpcSubscriptionRequest build() {
            return new OpcSubscriptionRequest(
                    Collections.unmodifiableList(nodeIds),
                    Collections.unmodifiableList(samplingIntervals),
                    Collections.unmodifiableList(listeners));
        }
    }
}
