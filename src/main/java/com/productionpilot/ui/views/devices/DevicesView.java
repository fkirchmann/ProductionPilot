/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.views.devices;

import com.productionpilot.db.timescale.entities.Machine;
import com.productionpilot.opc.*;
import com.productionpilot.service.OpcService;
import com.productionpilot.service.ParameterRecordingService;
import com.productionpilot.ui.util.*;
import com.productionpilot.util.*;
import com.productionpilot.ui.views.MainLayout;
import com.productionpilot.ui.views.parameters.ParameterDialog;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PageTitle("Devices")
@Route(value = "devices", layout = MainLayout.class)
@RequiredArgsConstructor
@Slf4j
public class DevicesView extends VerticalLayout {
    // Static fields
    private static final Duration SAMPLING_INTERVAL = Duration.ofMillis(1000);
    private static final long SORTING_UPDATE_COUNT_EQUAL_DISTANCE = 2;
    private static final Duration SORTING_UPDATE_TIME_EQUAL_DISTANCE_THRESHOLD = Duration.ofSeconds(2);

    // Dependencies - injected automatically by Spring
    private final OpcService opcService;
    private final ParameterDialog parameterDialog;
    private final ParameterRecordingService parameterRecordingService;

    // UI components
    private final VerticalLayout left = new VerticalLayout(), right = new VerticalLayout();
    private final SplitLayout splitLayout = new SplitLayout(left, right);
    private final TreeGrid<OpcNode> nodeTree = new TreeGrid<>();
    private final Grid<OpcNode> nodeList = new Grid<>();
    private final ComboBox<OpcDevice> deviceSelector = new ComboBox<>();
    private final Checkbox autoRefreshValues = new Checkbox("Auto-Refresh Values", true);
    private final TextField filter = new TextField();

    // State variables
    private OpcDevice selectedDevice = null;
    private OpcNode selectedDeviceNode = null;
    private List<OpcNode> selectedDeviceChildren = null;
    private OpcSubscription selectedDeviceSubscription = null;
    private final Map<OpcNode, OpcSubscribedItem> subscriptionItemsByNode = new HashMap<>();
    private final LazyUIRefresher lazyUIRefresher = new LazyUIRefresher();
    private Machine lastSelectedMachine = null;

    private Set<OpcNode> nodesAlreadyRecorded = new HashSet<>();

    @PostConstruct
    private void init() {
        setSizeFull();
        getStyle().set("padding", "0");
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(20);
        add(splitLayout);

        initializeLeft();
        initializeRight();

        refresh();
        setDevice(null);
        setFilterNodes(null);

        // Start polling for updates
        PeriodicUIRefresher.create(this, 1000, this::refresh);
    }

    private void initializeLeft() {
        deviceSelector.setItemLabelGenerator(device -> {
            String status;
            switch (device.getStatus()) {
                case ONLINE -> status = Emoji.GREEN_CIRCLE;
                case OFFLINE -> status = Emoji.RED_CIRCLE;
                // A blue circle has shown to be too confusing in user trials
                default -> status = Emoji.GREEN_CIRCLE; //Emoji.BLUE_CIRCLE;
            }
            return status + " " + device.getName();
        });
        deviceSelector.getStyle().set("--vaadin-combo-box-overlay-width", "60ch");
        deviceSelector.setPlaceholder("Select device");
        deviceSelector.setWidthFull();
        deviceSelector.addValueChangeListener(event -> {
            // Ignore value changes from server-side, as they are caused by a refresh
            if(event.isFromClient()) {
                setDevice(event.getValue());
            }
            UIUtil.scrollInputBegin(deviceSelector);
            nodeList.focus();
        });
        deviceSelector.addBlurListener(event -> UIUtil.scrollInputBegin(deviceSelector));
        left.add(deviceSelector);

        nodeTree.getStyle().set("user-select", "none"); // prevent user from selecting text in the tree
        nodeTree.addHierarchyColumn(OpcNode::getName);
        nodeTree.addItemClickListener(event -> nodeTree.select(event.getItem()));
        nodeTree.addExpandListener(event -> event.getItems().stream().findFirst().ifPresent(nodeTree::select));
        nodeTree.addCellFocusListener(event -> event.getItem().ifPresent(nodeTree::select));
        nodeTree.addSelectionListener(event -> setFilterNodes(event.getAllSelectedItems()));
        left.add(nodeTree);
    }

    private void initializeRight() {
        var toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.BASELINE);
        filter.setPlaceholder("Filter by Path & Value");
        filter.setWidth("18em");
        filter.setPrefixComponent(VaadinIcon.SEARCH.create());
        filter.addValueChangeListener(event -> nodeList.getDataProvider().refreshAll());
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        toolbar.add(filter);
        toolbar.add(autoRefreshValues);
        right.add(toolbar);

        var pathColumn = nodeList.addColumn(node ->
                    Optional.ofNullable(deviceSelector.getValue())
                                    .map(device -> node.getPathRelativeTo(device.getNode()))
                                        .orElse(node.getPath())
                ).setHeader("Path")
                .setSortable(true)
                .setResizable(true)
                .setFlexGrow(1).setAutoWidth(true)
                .setComparator((a, b) -> {
                    // This comparator causes System Tags (starting with _) to be sorted to the end by default
                    if(a.getName().startsWith("_") && !b.getName().startsWith("_")) {
                        return 1;
                    } else if(!a.getName().startsWith("_") && b.getName().startsWith("_")) {
                        return -1;
                    } else {
                        return a.getPath().compareTo(b.getPath());
                    }
                });

        nodeList.addColumn(node -> {
                    var item = subscriptionItemsByNode.get(node);
                    if(item != null) {
                        return item.getStatusCode();
                    } else {
                        return "Error: No subscription";
                    }
                }).setHeader("Status")
                .setWidth("9ch")
                .setFlexGrow(0)
                .setSortable(true)
                .setResizable(true);

        nodeList.addColumn(node -> {
                    var item = subscriptionItemsByNode.get(node);
                    if(item != null) {
                        if(item.getLastValue() != null) {
                            return UIFormatters.TIME_FORMATTER.format(item.getLastValue().getClientTime());
                        } else {
                            return "-";
                        }
                    } else {
                        return "Error: No subscription";
                    }
                }).setHeader("Updated")
                .setWidth("12ch")
                .setFlexGrow(0)
                .setSortable(true)
                .setResizable(true);

        nodeList.addColumn(node -> {
                    var item = subscriptionItemsByNode.get(node);
                    if(item != null) {
                        return item.getUpdateCount();
                    } else {
                        return "Error: No subscription";
                    }
                }).setHeader("Updates")
                .setWidth("10ch")
                .setFlexGrow(0)
                .setSortable(true)
                .setResizable(true)
                /*.setComparator((a, b) -> {
                    var aItem = subscriptionItemsByNode.get(a);
                    var bItem = subscriptionItemsByNode.get(b);
                    if(aItem == null && bItem != null) {
                        return 1;
                    } else if(aItem != null && bItem == null) {
                        return -1;
                    } else if(aItem != null && bItem != null && Math.abs(aItem.getUpdateCount() - bItem.getUpdateCount())
                            < SORTING_UPDATE_COUNT_EQUAL_DISTANCE) {
                        return 0;
                    } else {
                        return Long.compare(aItem.getUpdateCount(), bItem.getUpdateCount());
                    }
                })*/;

        nodeList.addColumn(node -> Optional.ofNullable(subscriptionItemsByNode.get(node))
                        .map(OpcSubscribedItem::getLastValue)
                        .map(OpcMeasuredValue::getValueAsString)
                        .orElse("Error: No subscription"))
                .setHeader("Value")
                .setFlexGrow(0)
                .setWidth("12ch")
                .setResizable(true)
                .setSortable(true);
        nodeList.addColumn(LitRenderer.<OpcNode>of(
                "<a @click=${click} href=\"javascript:\">" +
                        "<i class=\"las la-plus-circle\" style=${item.style}></i></a>")
                    .withProperty("style", node ->
                            nodesAlreadyRecorded.contains(node) ? "color: var(--lumo-secondary-text-color)" : "")
                    .withFunction("click", node ->
                            parameterDialog.openForCreation(createdParameter ->
                                            lastSelectedMachine = createdParameter.getMachine())
                                    .setSubscribedNode(subscriptionItemsByNode.get(node))
                                    .setMachine(lastSelectedMachine)))

                .setFlexGrow(0)
                .setResizable(true)
                .setWidth("6ch");
        nodeList.sort(GridSortOrder.asc(pathColumn).build());
        nodeList.setSelectionMode(Grid.SelectionMode.NONE);
        right.add(nodeList);
    }

    private void refresh() {
        try {
            var currentDeviceStatus = opcService.getDeviceEnumerator().getDevices().stream()
                    .collect(Collectors.toMap(device -> device, OpcDevice::getStatus));
            lazyUIRefresher.refreshIfNecessary(deviceSelector, currentDeviceStatus, (v, i) -> {
                deviceSelector.setItems(currentDeviceStatus.keySet());
                deviceSelector.setValue(selectedDevice);
            });
            nodesAlreadyRecorded = parameterRecordingService.listSubscribedItems().values().stream()
                    .map(OpcSubscribedItem::getNode).collect(Collectors.toSet());
            if(autoRefreshValues.getValue()) {
                nodeList.getDataProvider().refreshAll();
            }
        } catch (OpcException e) {
            log.warn("Error refreshing device list", e);
            Notification.show("Error refreshing device list", 3000, Notification.Position.MIDDLE);
        }
    }

    @Override
    public void onDetach(DetachEvent event) {
        Optional.ofNullable(selectedDeviceSubscription).ifPresent(OpcSubscription::unsubscribe);
    }

    private void setDevice(OpcDevice device) {
        if(Objects.equals(device, selectedDevice)) { return; }
        if(selectedDeviceSubscription != null) {
            selectedDeviceSubscription.unsubscribe();
            selectedDeviceSubscription = null;
        }
        selectedDevice = device;
        subscriptionItemsByNode.clear();
        if(device == null) {
            right.setEnabled(false);
            return;
        }
        right.setEnabled(true);

        selectedDeviceNode = deviceSelector.getValue().getNode();
        try {
            // Pre-fetch all child nodes so that they are cached in-memory
            var timer1 = DebugPerfTimer.start("Retrieving all device nodes for " + selectedDevice.getName());
            selectedDeviceChildren = selectedDeviceNode.streamChildrenRecursively(d -> true, d -> true).toList();
            timer1.endAndPrint(log);

            OpcSubscriptionRequest.OpcSubscriptionRequestBuilder builder = OpcSubscriptionRequest.builder();
            selectedDeviceChildren.forEach(node -> builder.addNode(node, SAMPLING_INTERVAL, null));
            selectedDeviceSubscription = opcService.getConnection().getSubscriptionManager().subscribe(builder.build());

            selectedDeviceSubscription.getSubscribedItems().forEach(opcSubscribedItem ->
                    subscriptionItemsByNode.put(opcSubscribedItem.getNode(), opcSubscribedItem));
            log.debug("Subscribed to {} items for device {}", subscriptionItemsByNode.size(), selectedDevice.getName());

            nodeTree.setItems(List.of(selectedDeviceNode),
                    tag -> tag.getChildren().stream()
                            .filter(subTag -> subTag.getType().isObject())
                            .collect(Collectors.toList())
            );
            nodeTree.select(selectedDeviceNode);
            nodeTree.expandRecursively(Stream.of(selectedDeviceNode), 2);
        } catch (OpcException e) {
            log.warn("Error fetching tags", e);
            Notification.show("Error fetching tags", 3000, Notification.Position.MIDDLE);
        }
    }

    private void setFilterNodes(Set<OpcNode> opcNodes) {
        if(opcNodes == null || opcNodes.isEmpty()) {
            right.setEnabled(false);
            nodeList.setItems(List.of());
            return;
        }
        right.setEnabled(true);
        var dataView = nodeList.setItems(
                opcNodes.stream()
                        .flatMap(tag -> tag.streamChildrenRecursively(n -> true, n -> true))
                        .filter(tag -> tag.getType().isVariable())
                .collect(Collectors.toList())
        );
        dataView.setFilter(node -> {
            if(node.getPath().toLowerCase().contains(filter.getValue().toLowerCase())) {
                return true;
            }
            var item = subscriptionItemsByNode.get(node);
            if(item == null) {
                return false;
            }
            var value = item.getLastValue();
            if(value == null) {
                return false;
            }
            var valueString = value.getValueAsString();
            if(valueString == null) {
                return false;
            }
            return valueString.toLowerCase().contains(filter.getValue().toLowerCase());
        });
    }
}
