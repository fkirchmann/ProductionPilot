/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

public interface OpcDevice {
    String getName();

    OpcNode getNode();

    OpcDeviceStatus getStatus();
}
