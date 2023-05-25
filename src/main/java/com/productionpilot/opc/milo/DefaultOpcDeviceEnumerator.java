/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc.milo;

import com.productionpilot.opc.*;
import java.util.List;

public class DefaultOpcDeviceEnumerator implements OpcDeviceEnumerator {
    private final OpcConnection connection;

    public DefaultOpcDeviceEnumerator(OpcConnection connection) {
        this.connection = connection;
    }

    @Override
    public List<OpcDevice> getDevices() throws OpcException {
        return connection.browseRoot().stream()
                .map(RawOpcDevice::new)
                .map(device -> (OpcDevice) device)
                .toList();
    }

    public static class RawOpcDevice implements OpcDevice {
        private final OpcNode node;

        private RawOpcDevice(OpcNode node) {
            this.node = node;
        }

        @Override
        public String getName() {
            return node.getName();
        }

        @Override
        public OpcNode getNode() {
            return node;
        }

        @Override
        public OpcDeviceStatus getStatus() {
            return OpcDeviceStatus.UNKNOWN;
        }
    }
}
