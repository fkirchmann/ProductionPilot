/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.views.units;

import com.productionpilot.db.timescale.entities.UnitOfMeasurement;
import com.productionpilot.db.timescale.service.ParameterService;
import com.productionpilot.db.timescale.service.UnitOfMeasurementService;
import com.productionpilot.ui.util.ConfirmDeletionDialog;
import com.productionpilot.ui.util.CrudDialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class UnitOfMeasurementDialog extends CrudDialog<UnitOfMeasurementDialog, UnitOfMeasurement> {
    private final UnitOfMeasurementService unitOfMeasurementService;
    private final ParameterService parameterService;

    private final TextField name = new TextField("Name", "Bathtubs per Second");
    private final TextField abbreviation = new TextField("Abbreviation", "b/s");
    private final TextArea description = new TextArea("Description");

    @Override
    protected Class<UnitOfMeasurement> getEntityClass() {
        return UnitOfMeasurement.class;
    }

    @Override
    protected void initializeUi(VerticalLayout dialogLayout) {
        dialogLayout.add(name, abbreviation, description);
    }

    @Override
    protected void openDeletionDialog(Runnable onConfirm) {
        var numParams = parameterService.findByUnitOfMeasurement(getEntity()).size();
        ConfirmDeletionDialog.open(
                getEntity().getName(),
                "Are you sure you want to delete this Unit of Measurement? "
                        + (numParams > 0
                                ? "This will remove it from " + numParams + " Parameter(s)!"
                                : "It is not currently in use by any Parameters."),
                onConfirm);
    }

    @Override
    protected UnitOfMeasurement onCreate() {
        return unitOfMeasurementService.create(name.getValue(), abbreviation.getValue());
    }

    @Override
    protected void onUpdate(UnitOfMeasurement entity) {
        unitOfMeasurementService.update(entity);
    }

    @Override
    protected void onDelete(UnitOfMeasurement entity) {
        unitOfMeasurementService.delete(entity);
    }

    protected String getEntityName() {
        return "Unit of Measurement";
    }
}
