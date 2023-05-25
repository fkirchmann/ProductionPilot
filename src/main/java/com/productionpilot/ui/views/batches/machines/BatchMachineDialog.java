/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.views.batches.machines;

import com.productionpilot.db.timescale.entities.Batch;
import com.productionpilot.db.timescale.entities.BatchMachine;
import com.productionpilot.db.timescale.entities.Machine;
import com.productionpilot.db.timescale.service.BatchMachineService;
import com.productionpilot.db.timescale.service.BatchService;
import com.productionpilot.db.timescale.service.MachineService;
import com.productionpilot.ui.util.ConfirmDeletionDialog;
import com.productionpilot.ui.util.CrudDialog;
import com.productionpilot.ui.util.LazyUIRefresher;
import com.productionpilot.ui.util.LocalDateTimeToInstantConverter;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxBase;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class BatchMachineDialog extends CrudDialog<BatchMachineDialog, BatchMachine> {
    private final BatchService batchService;
    private final MachineService machineService;
    private final BatchMachineService batchMachineService;

    private final Text description = new Text("This will associate all Measurements from the selected Machine (in the"
            + " given time range) with the selected Batch.");
    private final ComboBox<Batch> batch = new ComboBox<>("Parent Batch");

    private final ComboBox<Machine> machine = new ComboBox<>("Machine");

    private final DateTimePicker startTime = new DateTimePicker("Start Time"), endTime = new DateTimePicker("End Time");

    private final LazyUIRefresher lazyUIRefresher = new LazyUIRefresher();

    LocalDateTimeToInstantConverter timeConverter = new LocalDateTimeToInstantConverter();

    @Override
    protected Class<BatchMachine> getEntityClass() {
        return BatchMachine.class;
    }

    @Override
    protected void initializeUi(VerticalLayout dialogLayout) {
        dialogLayout.add(description, batch, machine, startTime, endTime);
        startTime.setLocale(Locale.GERMAN);
        endTime.setLocale(Locale.GERMAN);
        refresh();
    }

    @Override
    protected void initializeAdditionalBindings(Binder<BatchMachine> binder) {
        binder.forField(startTime)
                .withConverter(timeConverter)
                .withValidator(Objects::nonNull, "Start Time is required")
                .bind(BatchMachine::getStartTime, BatchMachine::setStartTime);
        binder.forField(endTime)
                .withConverter(timeConverter)
                .withValidator(Objects::nonNull, "End time is required")
                .withValidator(
                        time -> time.isAfter(timeConverter.convert(startTime.getValue())),
                        "End time must be after start time")
                .bind(BatchMachine::getEndTime, BatchMachine::setEndTime);
    }

    @Override
    public void refresh() {
        lazyUIRefresher.refreshIfNecessaryKeepValue(batch, batchService.findAll(), ComboBoxBase::setItems);
        lazyUIRefresher.refreshIfNecessaryKeepValue(machine, machineService.findAll(), ComboBoxBase::setItems);
    }

    public BatchMachineDialog setBatch(Batch batch) {
        checkOpened();
        this.batch.setValue(batch);
        return this;
    }

    @Override
    protected BatchMachine onCreate() {
        return batchMachineService.create(
                batch.getValue(),
                machine.getValue(),
                timeConverter.convert(startTime.getValue()),
                timeConverter.convert(endTime.getValue()));
    }

    @Override
    protected void onUpdate(BatchMachine entity) {
        batchMachineService.update(entity);
    }

    @Override
    protected void onDelete(BatchMachine entity) {
        batchMachineService.delete(entity);
    }

    protected void openDeletionDialog(Runnable onConfirm) {
        ConfirmDeletionDialog.open(
                "Association",
                "If you delete this, the Measurements from Machine \""
                        + getEntity().getMachine().getName()
                        + "\" in the specified time range will no longer be associated" + " with the Batch \""
                        + getEntity().getBatch().getName() + "\". The measurements itself will remain" + " untouched.",
                onConfirm);
    }

    @Override
    public String getEntityName() {
        return "Measurement Range";
    }
}
