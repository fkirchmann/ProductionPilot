/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface OpcNode {
    default String getPathRelativeTo(@Nonnull OpcNode parent) {
        if (this.getPath() == null || parent.getPath() == null) {
            throw new IllegalArgumentException("Cannot get relative path for nodes without path");
        }
        if (!getPath().startsWith(parent.getPath())) {
            throw new IllegalArgumentException("Tag " + getPath() + " is not a subtag of " + parent.getPath());
        }
        return getPath().substring(parent.getPath().length() + 1);
    }

    List<OpcNode> getChildren() throws OpcException;

    default OpcNode getChild(String name) throws OpcException {
        return getChildren().stream()
                .filter(node -> node.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    Stream<OpcNode> streamChildrenRecursively(Predicate<OpcNode> nodeFilter, Predicate<OpcNode> browseFilter)
            throws OpcException;

    @Nullable
    String getName();

    @Nullable
    String getPath();

    /**
     * @return The OPC UA ID for this node, e.g. ns=2;s=Example.MyTag
     */
    OpcNodeId getId();

    OpcNodeType getType();

    default boolean equals(OpcNode other) {
        return this.getId().equals(other.getId());
    }
}
