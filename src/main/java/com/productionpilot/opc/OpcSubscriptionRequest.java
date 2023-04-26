/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class OpcSubscriptionRequest {
    private final List<OpcNode> nodes;
    private final List<Duration> samplingIntervals;
    private final List<OpcSubscriptionListener> listeners;

    public static OpcSubscriptionRequestBuilder builder() {
        return new OpcSubscriptionRequestBuilder();
    }

    public static class OpcSubscriptionRequestBuilder {
        private final List<OpcNode> nodes = new ArrayList<>();
        private final List<Duration> samplingIntervals = new ArrayList<>();
        private final List<OpcSubscriptionListener> listeners = new ArrayList<>();
        private OpcSubscriptionRequestBuilder() {}

        public OpcSubscriptionRequestBuilder addNode(OpcNode node, Duration samplingInterval,
                                                     OpcSubscriptionListener listener) {
            nodes.add(node);
            samplingIntervals.add(samplingInterval);
            listeners.add(listener);
            return this;
        }

        public OpcSubscriptionRequestBuilder addNode(OpcNodeId nodeId, Duration samplingInterval,
                                                     OpcSubscriptionListener listener) {
            nodes.add(new NodeIdOnlyOpcNode(nodeId));
            samplingIntervals.add(samplingInterval);
            listeners.add(listener);
            return this;
        }

        public OpcSubscriptionRequest build() {
            return new OpcSubscriptionRequest(
                    Collections.unmodifiableList(nodes),
                    Collections.unmodifiableList(samplingIntervals),
                    Collections.unmodifiableList(listeners));
        }
    }

    @RequiredArgsConstructor
    public static class NodeIdOnlyOpcNode implements OpcNode {
        private final OpcNodeId nodeId;

        @Override
        public List<OpcNode> getChildren() throws OpcException {
            return List.of();
        }

        @Override
        public Stream<OpcNode> streamChildrenRecursively(Predicate<OpcNode> nodeFilter, Predicate<OpcNode> browseFilter)
                throws OpcException {
            return Stream.empty();
        }

        @Nullable
        @Override
        public String getName() {
            return null;
        }

        @Nullable
        @Override
        public String getPath() {
            return null;
        }

        @Override
        public OpcNodeId getId() {
            return nodeId;
        }

        @Override
        public OpcNodeType getType() {
            return OpcNodeType.NOT_DETERMINED;
        }
    }
}
