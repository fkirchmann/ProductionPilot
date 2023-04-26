/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc.milo;

import com.productionpilot.opc.OpcException;
import com.productionpilot.opc.OpcNode;
import com.productionpilot.opc.OpcNodeId;
import com.productionpilot.opc.OpcNodeType;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;


@Slf4j
public class MiloOpcNode implements OpcNode {
    @Getter
    private final MiloOpcConnection connection;
    @Getter
    private final String name;
    @Getter
    private final String path;
    private final OpcNodeId nodeId;
    private final String nodeIdString;
    @Getter
    private final OpcNodeType type;

    private List<OpcNode> children;

    protected static OpcNode fromReferenceDescription(MiloOpcConnection connection, OpcNode parent,
                                                      ReferenceDescription rd, OpcNodeType type) {
        var nodeId = MiloOpcNodeId.from(rd.getNodeId().toNodeId(connection.client.getNamespaceTable())
                .orElseThrow(() -> new IllegalArgumentException("Illegal Node ID: "+ rd.getNodeId())));
        var name = rd.getBrowseName().getName();
        var path = parent == null ? rd.getBrowseName().getName()
                : parent.getPath() + "." + rd.getBrowseName().getName();
        return new MiloOpcNode(connection, nodeId, name, path, type);
    }

    protected static OpcNode newPlaceholder(MiloOpcConnection connection, OpcNodeId nodeId) {
        return new MiloOpcNode(connection, nodeId, null, null, OpcNodeType.NOT_DETERMINED);
    }

    @SneakyThrows
    protected MiloOpcNode(MiloOpcConnection connection, OpcNodeId nodeId, String name, String path, OpcNodeType type) {
        this.connection = connection;
        this.name = name;
        this.path = path;
        this.nodeId = nodeId;
        this.nodeIdString = nodeId.toParseableString();
        this.type = type;
    }

    @Override
    public List<OpcNode> getChildren() throws OpcException {
        if(this.children == null) {
            this.children = connection.browse(this);
        }
        return this.children;
    }

    @Override
    public Stream<OpcNode> streamChildrenRecursively(Predicate<OpcNode> nodeFilter, Predicate<OpcNode> browseFilter)
            throws OpcException {
        List<OpcNode> children = getChildren();
        Stream.Builder<OpcNode> streamBuilder = Stream.builder();

        while(!children.isEmpty()) {
            // First, add all children to the result
            // Then filter: we only want to browse the children that match the browseFilter
            final var filteredChildren = children.stream()
                    .filter(nodeFilter)
                    .peek(streamBuilder::add)
                    .filter(browseFilter)
                    .toList();
            if(filteredChildren.isEmpty()) { break; }
            // Now, we need to fetch the children of each of the filtered children
            // However, some of the filtered children may already have their children fetched earlier,
            // so we don't need to fetch them again
            final Map<OpcNode, List<OpcNode>> childrenOfFilteredChildren = filteredChildren.stream()
                    .map(child -> (MiloOpcNode) child)
                    .filter(child -> child.children != null)
                    .collect(HashMap::new, (map, child) -> map.put(child, child.children), HashMap::putAll);
            // Fetch the children of the remaining filtered children (those that don't have their children fetched yet)
            final List<OpcNode> childrenToFetch = filteredChildren.stream()
                    .filter(child -> !childrenOfFilteredChildren.containsKey(child))
                    .toList();
            if(!childrenToFetch.isEmpty()) {
                var fetchedChildren = connection.browse(childrenToFetch);
                IntStream.range(0, childrenToFetch.size())
                        .forEach(i -> {
                            var childToFetch = childrenToFetch.get(i);
                            var childrenOfChildToFetch = fetchedChildren.get(i);
                            ((MiloOpcNode) childToFetch).children = childrenOfChildToFetch;
                            childrenOfFilteredChildren.put(childToFetch, childrenOfChildToFetch);
                        });
            }
            // Finally, aggregate the children of the filtered children into a list.
            children = filteredChildren.stream()
                    .map(childrenOfFilteredChildren::get)
                    .flatMap(List::stream)
                    .toList();
        }
        return streamBuilder.build();
    }

    @Override
    public String toString() {
        return path != null ? path : nodeIdString;
    }

    @Override
    public OpcNodeId getId() {
        return nodeId;
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof OpcNode otherNode) {
            return this.getId().equals(otherNode.getId());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }
}
