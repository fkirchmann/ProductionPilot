/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.service;

import com.productionpilot.opc.OpcConnection;
import com.productionpilot.opc.OpcDeviceEnumerator;
import com.productionpilot.opc.kepserver.KepOpcConnection;
import com.productionpilot.opc.kepserver.KepOpcDeviceEnumerator;
import com.productionpilot.opc.milo.DefaultOpcDeviceEnumerator;
import com.productionpilot.opc.milo.MiloOpcConnection;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OpcService {
    @Value("${com.productionpilot.opc.server.url}")
    private String opcServerUrl;

    @Value("${com.productionpilot.opc.server.hostname-override:#{null}}")
    private String opcServerHostnameOverride;

    @Value("${com.productionpilot.opc.server.username}")
    private String opcUser;

    @Value("${com.productionpilot.opc.server.password}")
    private String opcPassword;

    @Value("${com.productionpilot.opc.server.timeout:5000}")
    private int opcTimeout;

    @Value("${com.productionpilot.opc.server.driver:raw}")
    private String opcDriver;

    @Value("${com.productionpilot.opc.server.kepserver.single-device-nodes}")
    private String kepserverSingleDeviceNodes;

    @Getter
    private OpcConnection connection;

    @Getter
    private OpcConnection parameterRecordingConnection;

    @Getter
    private OpcDeviceEnumerator deviceEnumerator;

    @PostConstruct
    private void init() {
        var rawOpcConnection =
                new MiloOpcConnection(opcServerUrl, opcServerHostnameOverride, opcUser, opcPassword, opcTimeout);
        if (opcDriver.equals("kepserver")) {
            var kepOpcConnection = new KepOpcConnection(rawOpcConnection);
            connection = kepOpcConnection;
            deviceEnumerator = new KepOpcDeviceEnumerator(
                    kepOpcConnection, Set.of(kepserverSingleDeviceNodes.split(Pattern.quote(","))));
        } else if (opcDriver.equals("raw")) {
            connection = rawOpcConnection;
            deviceEnumerator = new DefaultOpcDeviceEnumerator(rawOpcConnection);
        } else {
            throw new UnsupportedOperationException("Unknown OPC server driver: " + opcDriver);
        }
        parameterRecordingConnection =
                new MiloOpcConnection(opcServerUrl, opcServerHostnameOverride, opcUser, opcPassword, opcTimeout);
    }
}
