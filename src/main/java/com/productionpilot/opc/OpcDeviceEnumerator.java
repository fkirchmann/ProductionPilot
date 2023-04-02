/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc;

import java.util.List;

public interface OpcDeviceEnumerator {
    List<OpcDevice> getDevices() throws OpcException;
}
