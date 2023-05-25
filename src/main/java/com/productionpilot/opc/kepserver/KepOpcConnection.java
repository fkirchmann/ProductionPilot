/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc.kepserver;

import com.productionpilot.opc.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KepOpcConnection implements OpcConnection {
    @Getter
    private final OpcConnection connection;

    @Override
    public List<OpcNode> browse(OpcNode parent) throws OpcException {
        // If the node is wrapped in a KepOpcNode, unwrap it before passing it to the underlying implementation
        if (parent instanceof KepOpcNode parentKepOpcNode) {
            return browse(parentKepOpcNode.getOpcNode());
        }
        return connection.browse(parent).stream()
                .map(node -> (OpcNode) KepOpcNode.from(this, node))
                .collect(Collectors.toList());
    }

    @Override
    public List<List<OpcNode>> browse(List<OpcNode> parents) throws OpcException {
        List<OpcNode> parentNodesUnwrapped = new ArrayList<>(parents.size());
        for (var parent : parents) {
            while (parent instanceof KepOpcNode parentKepOpcNode) {
                parent = parentKepOpcNode.getOpcNode();
            }
            parentNodesUnwrapped.add(parent);
        }
        return connection.browse(parentNodesUnwrapped).stream()
                .map(nodes -> nodes.stream()
                        .map(node -> (OpcNode) KepOpcNode.from(this, node))
                        .toList())
                .toList();
    }

    @Override
    public List<OpcNode> getNodesFromNodeIds(List<OpcNodeId> nodeIdentifiers) throws OpcException {
        return connection.getNodesFromNodeIds(nodeIdentifiers).stream()
                .map(node -> node == null ? null : KepOpcNode.from(this, node))
                .collect(Collectors.toList());
    }

    @Override
    public OpcNodeId parseNodeId(String nodeId) throws OpcException {
        return connection.parseNodeId(nodeId);
    }

    @Override
    public OpcSubscriptionManager getSubscriptionManager() {
        return connection.getSubscriptionManager();
    }

    @Override
    public OpcMeasuredValue read(OpcNode node) throws OpcException {
        // If the node is wrapped in a KepOpcNode, unwrap it before passing it to the underlying implementation
        if (node instanceof KepOpcNode nodeKepOpcNode) {
            return read(nodeKepOpcNode.getOpcNode());
        }
        return connection.read(node);
    }
}
