/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import java.util.List;

/**
 * Interface for enumerating OPC devices.
 */
public interface OpcDeviceEnumerator {

    /**
     * Returns a list of OPC devices.
     *
     * @return a list of OPC devices
     * @throws OpcException if there is an error while getting the devices
     */
    List<OpcDevice> getDevices() throws OpcException;
}
