/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

/**
 * This interface represents an OPC device. An OPC UA device represents an entity on an OPC UA server, that has an {@link OpcNode} as root node and an {@link OpcDeviceStatus online status}.
 */
public interface OpcDevice {

    /**
     * Returns the name of the OPC device.
     * @return the name of the OPC device
     */
    String getName();

    /**
     * Returns the root node of the OPC device.
     * @return the root node of the OPC device
     */
    OpcNode getNode();

    /**
     * Returns the status of the OPC device.
     * @return the status of the OPC device
     */
    OpcDeviceStatus getStatus();
}
