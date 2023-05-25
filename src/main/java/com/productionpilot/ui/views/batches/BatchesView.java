/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.views.batches;

import com.productionpilot.api.BatchApi;
import com.productionpilot.db.timescale.entities.Batch;
import com.productionpilot.db.timescale.entities.BatchMachine;
import com.productionpilot.db.timescale.service.BatchMachineService;
import com.productionpilot.db.timescale.service.BatchService;
import com.productionpilot.ui.components.StateRetainingTreeGrid;
import com.productionpilot.ui.util.*;
import com.productionpilot.ui.views.MainLayout;
import com.productionpilot.ui.views.batches.machines.BatchMachineDialog;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Route(value = "batches", layout = MainLayout.class)
@PageTitle("Batches")
@RequiredArgsConstructor
@Slf4j
public class BatchesView extends VerticalLayout {
    // Dependencies - injected by Spring
    private final BatchService batchService;
    private final BatchMachineService batchMachineService;
    private final BatchDialog batchDialog;
    private final BatchMachineDialog batchMachineDialog;

    // UI components
    private final HorizontalLayout toolbar = new HorizontalLayout();
    private final StateRetainingTreeGrid<BatchTreeData> grid = new StateRetainingTreeGrid<>();
    private final Button exportButton = new Button("Export CSV", new Icon("lumo", "download"));

    // State variables
    private final LazyUIRefresher lazyUIRefresher = new LazyUIRefresher();

    @PostConstruct
    private void init() {
        setSizeFull();
        toolbar.setAlignItems(Alignment.BASELINE);

        MenuBar batchMenuBar = new MenuBar();

        /*back = batchMenuBar.addItem(new Icon("lumo", "arrow-left"));
        selectedBatch = batchMenuBar.addItem("Selected Batch");
        selectedBatch.setEnabled(false);
        toolbar.add(batchMenuBar);*/

        var menuBar = new MenuBar();
        var create = menuBar.addItem(new Icon("lumo", "plus"));
        create.add("Create");
        create.getSubMenu()
                .addItem(
                        new LineAwesomeIcon("la la-folder"),
                        e -> batchDialog.openForCreation(this::select).setParentBatch(getSelectedBatch()))
                .add(" Batch...");
        create.getSubMenu()
                .addItem(
                        new LineAwesomeIcon("la la-list-ul"),
                        e -> batchMachineDialog.openForCreation(this::select).setBatch(getSelectedBatch()))
                .add(" Measurement Range...");

        exportButton.setEnabled(false);
        grid.addSelectionListener(event -> {
            var selected = event.getFirstSelectedItem().orElse(null);
            if (selected != null && selected.isBatch()) {
                exportButton
                        .getElement()
                        .setAttribute(
                                "onclick", "window.open('" + BatchApi.getLinkToBatchExport(selected.batch) + "')");
                exportButton.setEnabled(true);
            } else {
                exportButton.setEnabled(false);
            }
        });

        toolbar.add(menuBar, exportButton);

        add(toolbar);
        configureGrid();

        PeriodicUIRefresher.create(this, 1000, this::refresh);
    }

    private Batch getSelectedBatch() {
        return grid.getSelectedItems().stream()
                .findFirst()
                .map(batchTreeData -> batchTreeData.batch)
                .orElse(null);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addComponentHierarchyColumn(b -> {
                    var layout = new HorizontalLayout();
                    layout.setAlignItems(Alignment.BASELINE);
                    layout.add(
                            b.isBatch() ? new LineAwesomeIcon("la la-folder ") : new LineAwesomeIcon("la la-list-ul"));
                    layout.add(new Text(b.getName()));
                    return layout;
                })
                .setHeader("Name")
                .setFlexGrow(1)
                .setResizable(true)
                .setSortable(true);
        grid.addColumn(BatchTreeData::getLastModified)
                .setHeader("Modified")
                .setFlexGrow(0)
                .setWidth("20ch")
                .setResizable(true)
                .setSortable(true);
        grid.addColumn(new ComponentRenderer<>(b -> {
                    var menuBar = new MenuBar();
                    menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
                    menuBar.getStyle().set("margin-top", "-10px !important").set("margin-bottom", "-10px !important");
                    MenuItem create = menuBar.addItem(new Icon("lumo", "plus"));
                    if (b.isBatch()) {
                        create.getSubMenu()
                                .addItem(new LineAwesomeIcon("la la-folder"), e -> {
                                    select(b.batch);
                                    batchDialog.openForCreation(this::select).setParentBatch(b.batch);
                                })
                                .add(" Batch...");
                        create.getSubMenu()
                                .addItem(new LineAwesomeIcon("la la-list-ul"), e -> {
                                    select(b.batch);
                                    batchMachineDialog
                                            .openForCreation(this::select)
                                            .setBatch(b.batch);
                                })
                                .add(" Measurement Range...");
                    } else {
                        create.setEnabled(false);
                        create.getElement().getStyle().set("visibility", "hidden");
                    }
                    menuBar.addItem(new Icon("lumo", "edit"), e -> {
                        if (b.isBatch()) {
                            select(b.batch);
                            batchDialog.openForUpdate(b.batch, this::select, this::select);
                        } else {
                            select(b.batchMachine);
                            batchMachineDialog.openForUpdate(b.batchMachine, this::select, this::select);
                        }
                    });
                    return menuBar;
                }))
                .setFlexGrow(0)
                .setWidth("12ch");
        refresh();
        add(grid);
    }

    private void refresh() {
        if (lazyUIRefresher.refreshIfNecessary(
                grid, batchService.findAll(), batchMachineService.findAll(), (grid, batches, batchMachines) -> {
                    var batchToChildBatches = batches.stream()
                            .filter(b -> b.getParentBatch() != null)
                            .collect(Collectors.groupingBy(
                                    Batch::getParentBatch, Collectors.toCollection(ArrayList::new)));
                    var batchToChildBatchMachines = batchMachines.stream()
                            .collect(Collectors.groupingBy(
                                    BatchMachine::getBatch, Collectors.toCollection(ArrayList::new)));
                    grid.setItems(
                            // Top level batches
                            batches.stream()
                                    .filter(Batch::isTopLayer)
                                    .map(BatchTreeData::new)
                                    .toList(),
                            // For a batch, this returns its child batches and child batch machines
                            b -> b.isBatch()
                                    ? Stream.concat(
                                                    batchToChildBatches
                                                            .getOrDefault(b.batch, new ArrayList<>())
                                                            .stream()
                                                            .map(BatchTreeData::new),
                                                    batchToChildBatchMachines
                                                            .getOrDefault(b.batch, new ArrayList<>())
                                                            .stream()
                                                            .map(BatchTreeData::new))
                                            .toList()
                                    : Collections.emptyList());
                })) {
            grid.restoreState();
        }
        // grid.getDataProvider().refreshAll();
    }

    private void select(BatchMachine batchMachine) {
        refresh();
        select(batchMachine.getBatch());
        grid.expand(new BatchTreeData(batchMachine.getBatch()));
        grid.select(new BatchTreeData(batchMachine));
    }

    private void select(Batch batch) {
        refresh();
        batchService.getFullPath(batch).forEach(b -> {
            if (batch.equals(b)) {
                grid.select(new BatchTreeData(b));
            } else {
                grid.expand(new BatchTreeData(b));
            }
        });
    }

    private void onCsvExport() {
        var selected = grid.getSelectedItems().stream().findFirst().orElse(null);
        if (selected == null || !selected.isBatch()) {
            NotificationUtil.showError("Please select a batch to export");
            return;
        }
        var batch = selected.batch;
        UI.getCurrent().getPage().open(BatchApi.getLinkToBatchExport(batch), null);
    }

    @EqualsAndHashCode
    private static class BatchTreeData {
        private final Batch batch;
        private final BatchMachine batchMachine;

        private BatchTreeData(Batch batch) {
            this.batch = batch;
            this.batchMachine = null;
        }

        private BatchTreeData(BatchMachine batchMachine) {
            this.batch = null;
            this.batchMachine = batchMachine;
        }

        private boolean isBatch() {
            return batch != null;
        }

        public String getName() {
            return isBatch()
                    ? batch.getName()
                    : batchMachine.getMachine().getName()
                            + " from " + UIFormatters.DATE_TIME_FORMATTER.format(batchMachine.getStartTime())
                            + " until " + UIFormatters.DATE_TIME_FORMATTER.format(batchMachine.getEndTime());
        }

        public String getLastModified() {
            return UIFormatters.DATE_TIME_FORMATTER.format(
                    isBatch() ? batch.getModificationTime() : batchMachine.getModificationTime());
        }
    }
}
