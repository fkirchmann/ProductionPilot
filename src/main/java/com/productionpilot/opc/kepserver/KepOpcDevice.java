/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc.kepserver;

import com.productionpilot.opc.OpcDevice;
import com.productionpilot.opc.OpcDeviceStatus;
import com.productionpilot.opc.OpcNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@Getter
@RequiredArgsConstructor
public class KepOpcDevice implements OpcDevice {
    private final OpcNode node;

    private final String name;

    private final Supplier<OpcDeviceStatus> statusSupplier;

    public KepOpcDevice(OpcNode node, Supplier<OpcDeviceStatus> statusSupplier) {
        this(node, node.getPath(), statusSupplier);
    }

    @Override
    public OpcDeviceStatus getStatus() {
        return statusSupplier.get();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof KepOpcDevice device) {
            return device.getNode().equals(node);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    @Override
    public String toString() {
        return "OPC Device: " + name + ", node: " + node;
    }
}
