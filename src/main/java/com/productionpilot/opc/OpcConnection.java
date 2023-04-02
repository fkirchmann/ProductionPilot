/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc;

import java.util.Collections;
import java.util.List;

public interface OpcConnection {
    default List<OpcNode> browseRoot() throws OpcException {
        return browse((OpcNode) null);
    }

    default List<OpcNode> browse(OpcNode parent) throws OpcException {
        // Using Collections.nCopies(1, ...) instead of List.of(...), as the latter won't accept null values
        return browse(Collections.nCopies(1, parent)).get(0);
    }

    List<List<OpcNode>> browse(List<OpcNode> parents) throws OpcException;

    default OpcNode getNodeFromNodeId(OpcNodeId nodeId) throws OpcException {
        return getNodesFromNodeIds(List.of(nodeId)).get(0);
    }

    List<OpcNode> getNodesFromNodeIds(List<OpcNodeId> nodeIds) throws OpcException;

    OpcNodeId parseNodeId(String nodeId) throws OpcException;

    OpcSubscriptionManager getSubscriptionManager();

    OpcMeasuredValue read(OpcNode node) throws OpcException;
}
