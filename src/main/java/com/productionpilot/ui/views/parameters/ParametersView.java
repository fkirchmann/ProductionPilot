/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.views.parameters;

import com.productionpilot.db.timescale.entities.Machine;
import com.productionpilot.db.timescale.entities.Measurement;
import com.productionpilot.db.timescale.entities.Parameter;
import com.productionpilot.db.timescale.entities.UnitOfMeasurement;
import com.productionpilot.db.timescale.service.MachineService;
import com.productionpilot.db.timescale.service.ParameterService;
import com.productionpilot.service.ParameterRecordingService;
import com.productionpilot.ui.components.ImprovedRefreshTreeGrid;
import com.productionpilot.ui.util.*;
import com.productionpilot.ui.views.MainLayout;
import com.productionpilot.ui.views.machines.MachineDialog;
import com.google.common.collect.Streams;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

@PageTitle("Parameters")
@Route(value = "parameters", layout = MainLayout.class)
@RequiredArgsConstructor
@Slf4j
public class ParametersView extends VerticalLayout {
    private final ParameterService parameterService;
    private final MachineService machineService;
    private final ParameterRecordingService parameterRecordingService;
    private final ParameterDialog parameterDialog;
    private final MachineDialog machineDialog;

    private final ImprovedRefreshTreeGrid<ParameterTreeItem> grid = new ImprovedRefreshTreeGrid<>();
    private final LazyUIRefresher lazyUIRefresher = new LazyUIRefresher();

    @PostConstruct
    private void init() {
        setSizeFull();
        initializeGrid();

        PeriodicUIRefresher.create(this, 1000, this::refresh).refreshNow();
    }

    private void initializeGrid() {
        grid.addColumn(td -> td.get().getStatus())
                .setHeader(Emoji.WHITE_CIRCLE)
                .setFlexGrow(0).setWidth("6ch").setSortable(true);
        var nameColumn = grid.addComponentHierarchyColumn(dataWrapped -> {
                    var data = dataWrapped.get();
                    var layout = new HorizontalLayout();
                    layout.setAlignItems(Alignment.BASELINE);
                    layout.add(data.isMachine() ? new LineAwesomeIcon("la la-cog")
                            : new LineAwesomeIcon("la la-list-ul"));
                    layout.add(new Text(data.getName()));
                    if(data.isMachine()) {
                        var text = new Span();
                        if(data.getTotalParameters() == 0) {
                            text.setText(" - No Parameters defined");
                        } else {
                            text.setText(" - " + data.getOnlineParameters() + " / " + data.getTotalParameters()
                                    + " Parameters online");
                        }
                        text.getElement().getStyle().set("color", "var(--lumo-secondary-text-color)");
                        layout.add(text);
                    }
                    return layout;
                }).setHeader("Name")
                .setFlexGrow(5).setResizable(true).setSortable(true)
                .setComparator(Comparator.comparing(td -> td.get().getName()));
        grid.addColumn(td -> td.get().getCount()).setHeader("Recorded")
                .setFlexGrow(0).setWidth("14ch").setResizable(true).setSortable(true).setTextAlign(ColumnTextAlign.END);
        grid.addColumn(td -> td.get().getLastMeasurementTime()).setHeader("Last Measurement")
                .setFlexGrow(0).setWidth("17ch").setResizable(true).setSortable(true);
        grid.addColumn(td -> td.get().getLastMeasurementValue()).setHeader("Last Value")
                .setFlexGrow(0).setWidth("12ch").setResizable(true).setSortable(true);
        grid.addColumn(td -> td.get().getUnit()).setHeader("Unit")
                .setFlexGrow(0).setWidth("10ch").setResizable(true).setSortable(true);
        grid.addColumn(new ComponentRenderer<>(dataWrapped -> {
                    var data = dataWrapped.get();
                    var anchor = new Anchor("#", new Icon("lumo", "edit"));
                    anchor.getElement().addEventListener("click", e -> {
                                grid.select(dataWrapped);
                                if(data.isMachine()) {
                                    machineDialog.openForUpdate(data.machine, m -> refresh(), m -> refresh());
                                } else {
                                    parameterDialog.openForUpdate(data.parameter, p -> refresh(), p -> refresh())
                                            .setSubscribedNode(
                                                    parameterRecordingService.getSubscribedItem(data.parameter));
                                }
                            })
                            .addEventData("event.preventDefault()");
                    return anchor;
                })).setFlexGrow(0).setWidth("6ch");
        grid.setSizeFull();
        grid.sort(GridSortOrder.asc(nameColumn).build());
        add(grid);
    }

    private void refresh() {
        grid.improvedSetValues(() -> Streams.concat(
                machineService.findAll().stream().map(ParameterTreeItem::new),
                parameterService.findAll().stream().map(ParameterTreeItem::new)
            ).toList());
    }

    private class ParameterTreeItem extends ImprovedRefreshTreeGrid.TreeItem<ParameterTreeItem> {
        private final Parameter parameter;
        private final Machine machine;

        @Getter(lazy = true)
        private final String status = calculateStatus();

        @Getter(lazy = true)
        private final String lastMeasurementTime = calculateLastMeasurementTime();

        @Getter(lazy = true)
        private final String lastMeasurementValue = calculateLastMeasurementValue();

        @Getter(lazy = true)
        private final long totalParameters = calculateTotalParameters();

        @Getter(lazy = true)
        private final long onlineParameters = calculateOnlineParameters();

        @Getter(lazy = true)
        private final Optional<Measurement> lastMeasurement = calculateLastMeasurement();

        private ParameterTreeItem(Machine machine) {
            this.parameter = null;
            this.machine = machine;
        }

        private ParameterTreeItem(Parameter parameter) {
            this.parameter = parameter;
            this.machine = parameter.getMachine();
        }

        private boolean isMachine() {
            return parameter == null;
        }

        private String getName() {
            return isMachine() ? machine.getName() : parameter.getName();
        }

        public ParameterTreeItem getParent() {
            return isMachine() ? null : new ParameterTreeItem(machine);
        }

        public String getId() {
            return isMachine() ? "m" + machine.getId() : "p" + parameter.getId();
        }

        @Override
        public boolean deepEquals(ParameterTreeItem other) {
            if(!getId().equals(other.getId())) {
                return false;
            }
            if(!Objects.equals(getName(), other.getName())
                    || !Objects.equals(getStatus(), other.getStatus())
                    || !Objects.equals(getLastMeasurementTime(), other.getLastMeasurementTime())
                    || !Objects.equals(getLastMeasurementValue(), other.getLastMeasurementValue())
                    || !Objects.equals(getTotalParameters(), other.getTotalParameters())
                    || !Objects.equals(getOnlineParameters(), other.getOnlineParameters())
                    || !Objects.equals(getLastMeasurement(), other.getLastMeasurement())) {
                return false;
            }
            if(machine != null && !machine.deepEquals(other.machine)) {
                return false;
            }
            if(parameter != null && !parameter.deepEquals(other.parameter)) {
                return false;
            }
            return true;
        }

        private long getCount() {
            if(isMachine()) {
                return machine.getParameters().stream()
                        .map(parameterRecordingService::getMeasurementCount)
                        .reduce(0L, Long::sum);
            } else {
                return parameterRecordingService.getMeasurementCount(parameter);
            }
        }

        private String calculateLastMeasurementTime() {
            return getLastMeasurement().map(Measurement::getClientTime)
                    .map(UIFormatters.DATE_TIME_FORMATTER_SECONDS::format)
                    .orElse(null);
        }

        private String calculateLastMeasurementValue() {
            return isMachine() ? "" : getLastMeasurement().map(Measurement::getValueAsString).orElse("");
        }

        private String getUnit() {
            return isMachine() ? "" :
                    Optional.ofNullable(parameter.getUnitOfMeasurement())
                    .map(UnitOfMeasurement::getAbbreviation).orElse("");
        }

        private String calculateStatus() {
            if(isMachine()) {
                var totalParameters = getTotalParameters();
                var onlineParameters = getOnlineParameters();
                if(onlineParameters > 0) {
                    if(onlineParameters == totalParameters) {
                        return Emoji.GREEN_CIRCLE;
                    } else {
                        return Emoji.YELLOW_CIRCLE;
                    }
                } else {
                    if(totalParameters == 0) {
                        return Emoji.WHITE_CIRCLE;
                    } else {
                        return Emoji.RED_CIRCLE;
                    }
                }
            } else {
                return parameterRecordingService.getStatusCode(parameter).isGood()
                        ? Emoji.GREEN_CIRCLE : Emoji.RED_CIRCLE;
            }
        }

        private long calculateOnlineParameters() {
            return !isMachine() ? 0 : machine.getParameters().stream()
                    .filter(parameter -> parameterRecordingService.getStatusCode(parameter).isGood())
                    .count();
        }

        private long calculateTotalParameters() {
            return !isMachine() ? 0 : machine.getParameters().size();
        }

        private Optional<Measurement> calculateLastMeasurement() {
            if(isMachine()) {
                return machine.getParameters().stream()
                        .map(parameterRecordingService::getLastMeasurement)
                        .filter(Objects::nonNull)
                        .max(Comparator.comparing(Measurement::getClientTime));
            } else {
                return Optional.ofNullable(parameterRecordingService.getLastMeasurement(parameter));
            }
        }
    }
}

