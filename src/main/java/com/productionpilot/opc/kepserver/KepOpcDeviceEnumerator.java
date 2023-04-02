/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc.kepserver;

import com.productionpilot.opc.*;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class KepOpcDeviceEnumerator implements OpcDeviceEnumerator, OpcSubscriptionListener {
    private final static Duration DEVICE_ONLINE_CHECK_INTERVAL = Duration.ofSeconds(1);

    private final KepOpcConnection connection;

    private final Set<String> singleDeviceNodes;
    private final Map<OpcNode, OpcSubscription> devicesOnlineSubscriptions = new HashMap<>();

    public KepOpcDeviceEnumerator(KepOpcConnection connection, Set<String> singleDeviceNodes) {
        this.connection = connection;
        this.singleDeviceNodes = Set.copyOf(singleDeviceNodes);
        /*try {
            // Initializes the device status subscriptions, making future calls to getDevices() faster
            getDevices();
        } catch (OpcException ex) {
            // If getDevices() fails, we'll just have to wait for the next call to getDevices() to initialize the
            // subscriptions
            log.warn("Failed to pre-create device status subscriptions on startup. " +
                    "Is the connection to the OPC server working?", ex);
        }*/
    }

    @Synchronized
    @Override
    public List<OpcDevice> getDevices() throws OpcException {
        Set<OpcNode> devicesOnlineNodes = new HashSet<>();
        var devices = connection.browseRoot().stream()
                .filter(tag -> tag.getType() == OpcNodeType.OBJECT
                        && !tag.getName().equals("Server")
                        && !tag.getName().equals("_IoT_Gateway")
                        && !tag.getName().equals("_System")
                        && !tag.getName().equals("_DataLogger")
                        && !tag.getName().equals("_ThingWorx")
                )
                .flatMap(tag -> {
                        if (singleDeviceNodes.contains(tag.getName())) {
                            return Stream.of(tag).map(tag1 -> (OpcDevice) new KepOpcDevice(tag1,
                                    tag1.getName().equals("_AdvancedTags") ? "Advanced Tags": tag1.getName(),
                                    () -> OpcDeviceStatus.UNKNOWN));
                        } else {
                            return connection.browse(tag).stream()
                                    .filter(subtag -> !subtag.getName().startsWith("_Statistics")
                                            && !subtag.getName().startsWith("_System")
                                            && !subtag.getName().startsWith("_CommunicationSerialization"))
                                    .map(node ->
                                            Optional.ofNullable(node.getChild("_System"))
                                                .map(t -> t.getChild("_NoError"))
                                                .map(t -> {
                                                    devicesOnlineNodes.add(t);
                                                    return new KepOpcDevice(node, () -> getDeviceStatus(t));
                                                })
                                                .orElseGet(() -> {
                                                    log.warn("Device {} does not have _System._NoError tag", node.getPath());
                                                    return new KepOpcDevice(node, () -> OpcDeviceStatus.UNKNOWN);
                                                })
                                    );
                        }})
                .toList();
        // Remove all subscriptions that are not needed anymore
        var iterator = devicesOnlineSubscriptions.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if(!devicesOnlineNodes.contains(entry.getKey())) {
                entry.getValue().unsubscribe();
                iterator.remove();
            }
        }
        // Subscribe to all devices that are not subscribed yet
        devicesOnlineNodes.forEach(node -> {
            if(!devicesOnlineSubscriptions.containsKey(node)) {
                var subscription = connection.getSubscriptionManager()
                        .subscribe(node, DEVICE_ONLINE_CHECK_INTERVAL, this);
                devicesOnlineSubscriptions.put(node, subscription);
            }
        });
        return devices;
    }

    private OpcDeviceStatus getDeviceStatus(OpcNode deviceStatusNode) {
        return Optional.ofNullable(devicesOnlineSubscriptions.get(deviceStatusNode))
                .map(sub -> sub.getSubscribedItems().get(0).getLastValue())
                .map(measuredValue -> Objects.equals(measuredValue.getValue(), true))
                .map(isOnline -> isOnline ? OpcDeviceStatus.ONLINE : OpcDeviceStatus.OFFLINE)
                .orElse(OpcDeviceStatus.UNKNOWN);
    }

    @Override
    public void onVariableUpdate(OpcMeasuredValue value) {
        log.debug("Device {} is now {}", value.getNode(), getDeviceStatus(value.getNode()));
    }
}
