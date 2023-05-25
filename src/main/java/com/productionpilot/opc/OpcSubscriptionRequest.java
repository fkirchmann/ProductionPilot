/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * This class represents a subscription request for OPC UA nodes.
 * It contains a list of nodes to subscribe to, their corresponding sampling intervals,
 * and a list of listeners to be notified when the values of the subscribed nodes change.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class OpcSubscriptionRequest {
    private final List<OpcNode> nodes;
    private final List<Duration> samplingIntervals;
    private final List<OpcSubscriptionListener> listeners;

    /**
     * Returns a new builder instance for creating an {@link OpcSubscriptionRequest}.
     *
     * @return a new {@link OpcSubscriptionRequestBuilder} instance
     */
    public static OpcSubscriptionRequestBuilder builder() {
        return new OpcSubscriptionRequestBuilder();
    }

    public static class OpcSubscriptionRequestBuilder {
        private final List<OpcNode> nodes = new ArrayList<>();
        private final List<Duration> samplingIntervals = new ArrayList<>();
        private final List<OpcSubscriptionListener> listeners = new ArrayList<>();

        private OpcSubscriptionRequestBuilder() {}

        public OpcSubscriptionRequestBuilder addNode(
                OpcNode node, Duration samplingInterval, OpcSubscriptionListener listener) {
            nodes.add(node);
            samplingIntervals.add(samplingInterval);
            listeners.add(listener);
            return this;
        }

        public OpcSubscriptionRequestBuilder addNode(
                OpcNodeId nodeId, Duration samplingInterval, OpcSubscriptionListener listener) {
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
