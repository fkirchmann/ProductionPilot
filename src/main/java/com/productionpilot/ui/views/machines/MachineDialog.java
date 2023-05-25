/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.views.machines;

import com.productionpilot.db.timescale.entities.Machine;
import com.productionpilot.db.timescale.service.MachineService;
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
public class MachineDialog extends CrudDialog<MachineDialog, Machine> {
    private final MachineService machineService;

    private final TextField name = new TextField("Name");
    private final TextArea description = new TextArea("Description");

    @Override
    protected Class<Machine> getEntityClass() {
        return Machine.class;
    }

    @Override
    protected void initializeUi(VerticalLayout dialogLayout) {
        dialogLayout.add(name, description);
    }

    @Override
    protected void openDeletionDialog(Runnable onConfirm) {
        var numParams = getEntity().getParameters().size();
        ConfirmDeletionDialog.open(
                getEntity().getName(),
                "Are you sure you want to delete this Machine? "
                        + (numParams > 0
                                ? "This will also delete " + numParams + " Parameter(s) belonging to this machine!"
                                : "It does not currently have any parameters."),
                onConfirm);
    }

    @Override
    protected Machine onCreate() {
        return machineService.create(name.getValue());
    }

    @Override
    protected void onUpdate(Machine entity) {
        machineService.update(entity);
    }

    @Override
    protected void onDelete(Machine entity) {
        machineService.delete(entity);
    }
}
