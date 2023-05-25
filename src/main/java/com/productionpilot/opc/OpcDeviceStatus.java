/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

/**
 * Enum representing the status of an OPC device.
 */
public enum OpcDeviceStatus {
    /**
     * The device is currently online and available.
     */
    ONLINE,
    /**
     * The device is currently offline and unavailable.
     */
    OFFLINE,
    /**
     * The status of the device is unknown.
     */
    UNKNOWN
}
