/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import java.util.Collections;
import java.util.List;

/**
 * Represents a connection to an OPC server.
 */
public interface OpcConnection {
    /**
     * Returns a list of the child nodes of the root node of the OPC server.
     *
     * @return a list of the child nodes of the root node of the OPC server
     * @throws OpcException if an error occurs while browsing the server
     */
    default List<OpcNode> browseRoot() throws OpcException {
        return browse((OpcNode) null);
    }

    /**
     * Returns a list of the child nodes of the specified parent node.
     *
     * @param parent the parent node to browse
     * @return a list of the child nodes of the specified parent node
     * @throws OpcException if an error occurs while browsing the server
     */
    default List<OpcNode> browse(OpcNode parent) throws OpcException {
        // Using Collections.nCopies(1, ...) instead of List.of(...), as the latter won't accept null values
        return browse(Collections.nCopies(1, parent)).get(0);
    }

    /**
     * Returns a list of the child nodes of the specified parent nodes.
     *
     * @param parents the parent nodes to browse
     * @return a list of the child nodes of the specified parent nodes. For each parent node, a list of its child nodes is returned
     * @throws OpcException if an error occurs while browsing the server
     */
    List<List<OpcNode>> browse(List<OpcNode> parents) throws OpcException;

    /**
     * Returns the node with the specified node ID.
     *
     * @param nodeId the ID of the node to retrieve
     * @return the node with the specified node ID
     * @throws OpcException if an error occurs while retrieving the node
     */
    default OpcNode getNodeFromNodeId(OpcNodeId nodeId) throws OpcException {
        return getNodesFromNodeIds(List.of(nodeId)).get(0);
    }

    /**
     * Returns a list of nodes with the specified node IDs.
     *
     * @param nodeIds the IDs of the nodes to retrieve
     * @return a list of nodes with the specified node IDs
     * @throws OpcException if an error occurs while retrieving the nodes
     */
    List<OpcNode> getNodesFromNodeIds(List<OpcNodeId> nodeIds) throws OpcException;

    /**
     * Parses the specified node ID string into an {@link OpcNodeId} object.
     *
     * @param nodeId the node ID string to parse
     * @return the {@link OpcNodeId} object representing the specified node ID string
     * @throws OpcException if the specified node ID string is invalid
     */
    OpcNodeId parseNodeId(String nodeId) throws OpcException;

    /**
     * @return the subscription manager for this connection
     */
    OpcSubscriptionManager getSubscriptionManager();

    /**
     * Reads the value of the specified node.
     *
     * @param node the node to read
     * @return the value of the specified node
     * @throws OpcException if an error occurs while reading the node
     */
    OpcMeasuredValue read(OpcNode node) throws OpcException;
}
