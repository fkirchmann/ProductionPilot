/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

public interface OpcNodeId {
    Integer getNamespaceIndex();

    String getNamespaceUri();

    String getIdentifier();

    String getIdentifierType();

    String toParseableString();
}
