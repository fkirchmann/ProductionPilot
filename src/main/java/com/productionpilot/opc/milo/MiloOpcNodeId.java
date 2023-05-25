/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc.milo;

import com.productionpilot.opc.OpcException;
import com.productionpilot.opc.OpcNodeId;
import java.util.Optional;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

public class MiloOpcNodeId implements OpcNodeId {
    private final NodeId nodeId;
    private final String parseableString;

    private MiloOpcNodeId(NodeId nodeId) {
        this.nodeId = nodeId;
        this.parseableString = nodeId.toParseableString();
    }

    static MiloOpcNodeId from(OpcNodeId nodeId) {
        if (nodeId instanceof MiloOpcNodeId) {
            return (MiloOpcNodeId) nodeId;
        } else {
            return MiloOpcNodeId.from(nodeId.toParseableString());
        }
    }

    static MiloOpcNodeId from(String nodeId) throws OpcException {
        try {
            return new MiloOpcNodeId(NodeId.parse(nodeId));
        } catch (UaRuntimeException e) {
            throw new OpcException(e);
        }
    }

    static MiloOpcNodeId from(NodeId nodeId) {
        return new MiloOpcNodeId(nodeId);
    }

    NodeId getMiloNodeId() {
        return nodeId;
    }

    @Override
    public Integer getNamespaceIndex() {
        return Optional.ofNullable(nodeId.getNamespaceIndex())
                .map(UShort::intValue)
                .orElse(null);
    }

    @Override
    public String getNamespaceUri() {
        return null;
    }

    @Override
    public String getIdentifier() {
        return nodeId.getIdentifier().toString();
    }

    @Override
    public String getIdentifierType() {
        switch (nodeId.getType()) {
            case Numeric:
                return "i";
            case String:
                return "s";
            case Guid:
                return "g";
            case Opaque:
                return "b";
            default:
                return null;
        }
    }

    public String toParseableString() {
        return parseableString;
    }

    public String toString() {
        return parseableString;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MiloOpcNodeId that = (MiloOpcNodeId) o;
        return parseableString.equals(that.parseableString);
    }

    public int hashCode() {
        return parseableString.hashCode();
    }
}
