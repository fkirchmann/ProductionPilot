/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.views.parameters;

import com.productionpilot.db.timescale.entities.Machine;
import com.productionpilot.db.timescale.entities.Parameter;
import com.productionpilot.db.timescale.entities.UnitOfMeasurement;
import com.productionpilot.db.timescale.service.MachineService;
import com.productionpilot.db.timescale.service.ParameterService;
import com.productionpilot.db.timescale.service.UnitOfMeasurementService;
import com.productionpilot.opc.OpcMeasuredValue;
import com.productionpilot.opc.OpcSubscribedItem;
import com.productionpilot.service.MLCompletionService;
import com.productionpilot.ui.util.*;
import com.productionpilot.ui.views.machines.MachineDialog;
import com.productionpilot.ui.views.units.UnitOfMeasurementDialog;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.time.Duration;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class ParameterDialog extends CrudDialog<ParameterDialog, Parameter> {
    private OpcSubscribedItem subscription;

    private final ParameterService parameterService;
    private final MachineService machineService;
    private final UnitOfMeasurementService unitOfMeasurementService;
    private final MLCompletionService mlCompletionService;
    private final MachineDialog machineDialog;
    private final UnitOfMeasurementDialog uomDialog;

    private final LazyUIRefresher lazyUIRefresher = new LazyUIRefresher();

    private final TextField tag = new TextField("OPC UA Tag");
    private final ComboBox<String> name = new ComboBox<>("Name");
    private final TextField identifier = new TextField("Identifier (for external Applications)");
    private final TextArea description = new TextArea("Description");
    private final NumberField samplingInterval = new NumberField("Sampling interval");
    private final ComboBox<Machine> machine = new ComboBox<>("Machine");
    private final ComboBox<UnitOfMeasurement> unitOfMeasurement = new ComboBox<>("Unit of Measurement");
    private Button newMachineButton, editMachineButton, newUomButton, editUomButton;

    @Override
    protected Class<Parameter> getEntityClass() {
        return Parameter.class;
    }

    @Override
    protected void initializeUi(VerticalLayout dialogLayout) {
        dialogLayout.getStyle().set("width", "25rem");

        var advancedSettings = new VerticalLayout(samplingInterval);
        advancedSettings.setPadding(false);
        advancedSettings.setSpacing(false);
        advancedSettings.setAlignItems(FlexComponent.Alignment.STRETCH);
        advancedSettings.setWidthFull();

        tag.setReadOnly(true);
        UIUtil.scrollInputEnd(tag);
        dialogLayout.add(tag);

        machine.setWidthFull();
        machine.addValueChangeListener(e -> editMachineButton.setEnabled(e.getValue() != null));
        editMachineButton = new Button(
                new Icon(VaadinIcon.EDIT),
                e -> machineDialog.openForUpdate(
                        machine.getValue(),
                        mUpdated -> {
                            refresh();
                            machine.setValue(mUpdated);
                        },
                        mDeleted -> {
                            refresh();
                            machine.setValue(null);
                        }));
        editMachineButton.setEnabled(false);
        newMachineButton = new Button(
                new Icon(VaadinIcon.PLUS),
                e -> machineDialog.openForCreation(newMachine -> {
                    refresh();
                    machine.setValue(newMachine);
                }));
        var machineLayout = new HorizontalLayout(machine, editMachineButton, newMachineButton);
        machineLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        dialogLayout.add(machineLayout);

        name.setAllowCustomValue(true);
        name.setItems(query -> {
            // this is an ugly but effective hack, as otherwise vaadin will complain about us not caring about most
            // of the query
            query.getLimit();
            query.getPageSize();
            query.getOffset();
            return mlCompletionService
                    .getCompletion(subscription.getNode(), query.getFilter().orElse(""))
                    .stream();
        });
        name.addCustomValueSetListener(e -> name.setValue(e.getDetail()));
        dialogLayout.add(name);

        identifier.setHelperText("Should *not* be changed once chosen!");
        advancedSettings.add(identifier);

        unitOfMeasurement.setClearButtonVisible(true);
        unitOfMeasurement.setWidthFull();
        unitOfMeasurement.addValueChangeListener(e -> editUomButton.setEnabled(e.getValue() != null));
        editUomButton = new Button(
                new Icon(VaadinIcon.EDIT),
                e -> uomDialog.openForUpdate(
                        unitOfMeasurement.getValue(),
                        uUpdated -> {
                            refresh();
                            unitOfMeasurement.setValue(uUpdated);
                        },
                        uDeleted -> {
                            refresh();
                            unitOfMeasurement.setValue(null);
                        }));
        editUomButton.setEnabled(false);
        newUomButton = new Button(
                new Icon(VaadinIcon.PLUS),
                e -> uomDialog.openForCreation(newUom -> {
                    refresh();
                    unitOfMeasurement.setValue(newUom);
                }));
        var uomLayout = new HorizontalLayout(unitOfMeasurement, editUomButton, newUomButton);
        uomLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        samplingInterval.setMin(10);
        samplingInterval.setPlaceholder(String.valueOf(Parameter.DEFAULT_SAMPLING_INTERVAL.toMillis()));
        samplingInterval.setSuffixComponent(new Div(new Text("ms")));

        dialogLayout.add(description, uomLayout, new Details("Advanced Settings", advancedSettings));

        PeriodicUIRefresher.create(this, 1000, this::refresh).refreshNow();
    }

    @Override
    protected void initializeAdditionalBindings(Binder<Parameter> binder) {
        binder.forField(samplingInterval)
                .withValidator(
                        s -> s == null || s >= Parameter.MINIMUM_SAMPLING_INTERVAL_MS,
                        "Must be at least " + Parameter.MINIMUM_SAMPLING_INTERVAL_MS + " ms")
                .bind(s -> (double) s.getSamplingInterval().toMillis(), (s, v) -> {
                    if (v != null) s.setSamplingInterval(Duration.ofMillis(v.longValue()));
                });
    }

    public ParameterDialog setSubscribedNode(@NotNull OpcSubscribedItem item) {
        checkOpened();
        this.subscription = item;
        tag.setValue(Optional.ofNullable(item.getNode().getPath())
                .orElse(item.getNode().getId().getIdentifier()));
        refresh();
        return this;
    }

    public ParameterDialog setMachine(Machine machine) {
        checkOpened();
        this.machine.setValue(machine);
        return this;
    }

    @Override
    public void refresh() {
        lazyUIRefresher.refreshIfNecessaryKeepValue(
                unitOfMeasurement, unitOfMeasurementService.findAll(), (v, uoms) -> unitOfMeasurement.setItems(uoms));
        lazyUIRefresher.refreshIfNecessaryKeepValue(
                machine, machineService.findAll(), (v, machines) -> machine.setItems(machines));
        var currentSubscription = this.subscription;
        if (currentSubscription == null) {
            tag.setHelperText("No subscription");
        } else if (currentSubscription.getNode().getType().isNotFound()) {
            tag.setHelperText("Tag not found on OPC UA server");
        } else if (currentSubscription.getNode().getType().isUndetermined()) {
            tag.setHelperText("Tag type unknown");
        } else {
            tag.setHelperText("Type: " + currentSubscription.getNode().getType()
                    + ", Current value: "
                    + Optional.ofNullable(currentSubscription.getLastValue())
                            .map(OpcMeasuredValue::getValueAsString)
                            .orElse(""));
        }
    }

    @Override
    protected void openDeletionDialog(Runnable onConfirm) {
        ConfirmDeletionDialog.open(
                getEntity().getName(),
                "Are you sure you want to delete this Parameter?"
                        + " Recorded values will become inaccessible, and no new values will be recorded.",
                onConfirm);
    }

    @Override
    protected Parameter onCreate() {
        var node = subscription.getNode();
        var machine = this.machine.getValue();
        var name = this.name.getValue();
        return parameterService.create(node.getId(), machine, name);
    }

    @Override
    protected void onUpdate(Parameter entity) {
        if (entity.getIdentifier() != null && entity.getIdentifier().isBlank()) {
            entity.setIdentifier(null);
        }
        parameterService.update(entity);
    }

    @Override
    protected void onDelete(Parameter entity) {
        parameterService.delete(entity);
    }
}
