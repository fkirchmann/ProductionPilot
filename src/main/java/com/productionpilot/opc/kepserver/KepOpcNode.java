/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc.kepserver;

import com.productionpilot.opc.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public class KepOpcNode implements OpcNode {
    /**
     * Nodes that don't match this filter will always return an empty list of children.
     */
    private static final Predicate<OpcNode> BROWSE_FILTER = node -> node.getType().isObject();
    /**
     * Nodes that don't match this filter will be removed from any children lists.
     */
    private static final Predicate<OpcNode> NODE_FILTER = node ->
            !node.getName().equals("_InternalTags")
            && !node.getName().equals("_Hints");

    /**
     * Regex pattern to extract the string identifier from a String NodeId.
     */
    private static final Pattern STRING_NODE_ID_PATTERN = Pattern.compile("^ns=[^;]+;s=(.*)$");

    @Getter(AccessLevel.PROTECTED)
    private final OpcNode opcNode;

    @Getter
    private final String name, path;
    
    private final KepOpcConnection connection;

    @Getter(lazy = true, onMethod = @__({@SneakyThrows}))
    private final List<OpcNode> children =
            BROWSE_FILTER.test(this) ?
                    opcNode.getChildren().stream()
                            .map(node -> (OpcNode) KepOpcNode.from(connection, node))
                            .filter(NODE_FILTER)
                            .toList()
                    : Collections.emptyList();
    
    private KepOpcNode(KepOpcConnection connection, OpcNode opcNode) {
        this.opcNode = opcNode;
        this.connection = connection;
        var path = opcNode.getPath();
        var name = opcNode.getName();
        // In general, we can't always determine the name and path from the NodeId.
        // However, if the NodeId is a string, we can use it to make a reasonable guess at the name and path, based on
        // KepServer's default naming scheme.
        if(path == null || name == null) {
            var matcher = STRING_NODE_ID_PATTERN.matcher(opcNode.getId().getIdentifier());
            if(matcher.find()) {
                var stringIdentifier = matcher.group(1);
                if(path == null) {
                    path = stringIdentifier;
                }
                if(name == null) {
                    var pathSplit = path.split(Pattern.quote("."));
                    name = pathSplit[pathSplit.length - 1];
                }
            }
        }
        this.name = name;
        this.path = path;
    }

    protected static KepOpcNode from(KepOpcConnection connection, OpcNode opcNode) {
        if(opcNode instanceof KepOpcNode) {
            log.warn("Wrapping a KepOpcNode in a KepOpcNode. This is not necessary and should be avoided.",
                    new RuntimeException("Stack trace"));
            return (KepOpcNode) opcNode;
        }
        return new KepOpcNode(connection, opcNode);
    }

    @Override
    public OpcConnection getConnection() {
        return opcNode.getConnection();
    }

    @Override
    public OpcNodeId getId() { return opcNode.getId(); }

    @Override
    public OpcNodeType getType() {
        return opcNode.getType();
    }

    @Override
    public String toString() {
        return opcNode.toString();
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof OpcNode otherNode) {
            return opcNode.equals(otherNode);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return opcNode.hashCode();
    }

    @Override
    public String getPathRelativeTo(@NotNull OpcNode parent) {
        return opcNode.getPathRelativeTo(parent);
    }

    @Override
    public Stream<OpcNode> streamChildrenRecursively(Predicate<OpcNode> nodeFilter, Predicate<OpcNode> browseFilter)
            throws OpcException {
        return opcNode.streamChildrenRecursively(n -> nodeFilter.test(n) && NODE_FILTER.test(n),
                        n -> browseFilter.test(n) && BROWSE_FILTER.test(n))
                .map(node -> KepOpcNode.from(connection, node));
    }
}
