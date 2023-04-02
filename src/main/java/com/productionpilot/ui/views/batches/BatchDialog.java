/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.views.batches;

import com.productionpilot.db.timescale.entities.Batch;
import com.productionpilot.db.timescale.service.BatchService;
import com.productionpilot.ui.util.ConfirmDeletionDialog;
import com.productionpilot.ui.util.CrudDialog;
import com.productionpilot.ui.util.LazyUIRefresher;
import com.productionpilot.ui.util.PeriodicUIRefresher;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.HasListDataView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class BatchDialog extends CrudDialog<BatchDialog, Batch> {
    private final BatchService batchService;

    private final TextField name = new TextField("Name");
    private final TextArea description = new TextArea("Description");

    private final ComboBox<Batch> parentBatch = new ComboBox<>("Parent Batch");

    private final LazyUIRefresher uiRefresher = new LazyUIRefresher();

    @Override
    protected Class<Batch> getEntityClass() {
        return Batch.class;
    }

    @Override
    protected void initializeUi(VerticalLayout dialogLayout) {
        parentBatch.setClearButtonVisible(true);
        dialogLayout.add(name, parentBatch, description);
        PeriodicUIRefresher.create(this, 1000, this::refresh).refreshNow();
    }

    @Override
    public void refresh() {
        uiRefresher.refreshIfNecessaryKeepValue(parentBatch,
                batchService.findAll().stream().filter(b -> !b.equals(getEntity())).toList(),
                HasListDataView::setItems);
    }

    public BatchDialog setParentBatch(Batch parentBatch) {
        checkOpened();
        this.parentBatch.setValue(parentBatch);
        return this;
    }

    @Override
    protected Batch onCreate() {
        return batchService.create(name.getValue());
    }

    @Override
    protected void onUpdate(Batch entity) {
        batchService.update(entity);
    }

    @Override
    protected void onDelete(Batch entity) {
        batchService.delete(entity);
    }

    protected void openDeletionDialog(Runnable onConfirm) {
        ConfirmDeletionDialog.open(getEntity().getName(), "Are you sure you want to delete this Batch?" +
                " This will also delete all child batches, their children, and any associations." +
                " This action cannot be undone.", onConfirm);
    }
}
