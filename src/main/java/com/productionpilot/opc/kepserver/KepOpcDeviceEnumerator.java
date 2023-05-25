/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc.kepserver;

import com.productionpilot.opc.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KepOpcDeviceEnumerator implements OpcDeviceEnumerator, OpcSubscriptionListener {
    private static final Duration DEVICE_ONLINE_CHECK_INTERVAL = Duration.ofSeconds(1);

    private final KepOpcConnection connection;

    private final Set<String> singleDeviceNodes;
    private final Map<OpcNode, OpcSubscription> devicesOnlineSubscriptions = new HashMap<>();

    public KepOpcDeviceEnumerator(KepOpcConnection connection, Set<String> singleDeviceNodes) {
        this.connection = connection;
        this.singleDeviceNodes = Set.copyOf(singleDeviceNodes);
    }

    @Synchronized
    @Override
    public List<OpcDevice> getDevices() throws OpcException {
        Set<OpcNode> devicesOnlineNodes = new HashSet<>();
        List<OpcDevice> devices = new ArrayList<>();
        List<OpcNode> devicesToBrowse = new ArrayList<>();
        connection.browseRoot().stream()
                .filter(tag -> tag.getType() == OpcNodeType.OBJECT
                        && tag.getName() != null
                        && !tag.getName().equals("Server")
                        && !tag.getName().equals("_IoT_Gateway")
                        && !tag.getName().equals("_System")
                        && !tag.getName().equals("_DataLogger")
                        && !tag.getName().equals("_ThingWorx"))
                .forEach(tag -> {
                    if (singleDeviceNodes.contains(tag.getName())) {
                        Stream.of(tag)
                                .map(tag1 -> (OpcDevice) new KepOpcDevice(
                                        tag1,
                                        tag1.getName().equals("_AdvancedTags") ? "Advanced Tags" : tag1.getName(),
                                        () -> OpcDeviceStatus.UNKNOWN))
                                .forEach(devices::add);
                    } else {
                        devicesToBrowse.add(tag);
                    }
                });
        // In KepServer, Devices form a tree structure:
        // Root
        //  - Driver 1
        //      - Device 1
        //      - Device 2
        //  - Driver 2
        //      - Device 3
        //      - Device 4
        // By batching the browse requests, we need just two: one for the root and one for {Driver 1, Driver 2}
        connection.browse(devicesToBrowse).forEach(tags -> {
            tags.stream()
                    .filter(subtag -> subtag.getName() != null
                            && !subtag.getName().startsWith("_Statistics")
                            && !subtag.getName().startsWith("_System")
                            && !subtag.getName().startsWith("_CommunicationSerialization"))
                    .map(node -> Optional.ofNullable(node.getChild("_System"))
                            .map(t -> t.getChild("_NoError"))
                            .map(t -> {
                                devicesOnlineNodes.add(t);
                                return new KepOpcDevice(node, () -> getDeviceStatus(t));
                            })
                            .orElseGet(() -> {
                                log.warn("Device {} does not have _System._NoError tag", node.getPath());
                                return new KepOpcDevice(node, () -> OpcDeviceStatus.UNKNOWN);
                            }))
                    .forEach(devices::add);
        });
        devices.sort(Comparator.comparing(OpcDevice::getName));
        // Remove all subscriptions that are not needed anymore
        var iterator = devicesOnlineSubscriptions.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!devicesOnlineNodes.contains(entry.getKey())) {
                entry.getValue().unsubscribe();
                iterator.remove();
            }
        }
        // Subscribe to all devices that are not subscribed yet
        devicesOnlineNodes.forEach(node -> {
            if (!devicesOnlineSubscriptions.containsKey(node)) {
                var subscription =
                        connection.getSubscriptionManager().subscribe(node, DEVICE_ONLINE_CHECK_INTERVAL, this);
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
