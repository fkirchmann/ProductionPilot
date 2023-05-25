/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import javax.annotation.Nullable;

/**
 * This interface represents an instance of a OPC Node ID.
 */
public interface OpcNodeId {
    /**
     * @return the namespace index of the node ID, e.g. the node ID "ns=2;s=MyNode" has the namespace index 2
     */
    Integer getNamespaceIndex();

    /**
     * @return the namespace URI of the node ID, e.g. the node ID "ns=2;s=MyNode" has a null namespace URI
     */
    @Nullable
    String getNamespaceUri();

    /**
     * @return the identifier of the node ID, e.g. the node ID "ns=2;s=MyNode" has the identifier "MyNode"
     */
    String getIdentifier();

    /**
     * @return the identifier type of the node ID, e.g. "s" for a string identifier.
     */
    String getIdentifierType();

    /**
     * @return a parseable string representation of the node ID, which can be provided to {@link OpcConnection#parseNodeId(String)}.
     */
    String toParseableString();
}
