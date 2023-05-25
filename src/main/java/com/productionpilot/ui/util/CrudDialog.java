/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.util;

import com.productionpilot.db.timescale.DBExceptionMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionSystemException;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public abstract class CrudDialog<T extends CrudDialog<T, E>, E> extends Dialog {
    @Getter(AccessLevel.PROTECTED)
    private E entity;

    @Getter(AccessLevel.PROTECTED)
    private BeanValidationBinder<E> binder;

    private Consumer<E> creationCallback, updateCallback, deletionCallback;
    @Getter(AccessLevel.PROTECTED)
    private final Button deleteButton = new Button("Delete", new Icon(VaadinIcon.TRASH), e -> delete()),
            createButton = new Button("Create", e -> create()),
            updateButton = new Button("Update", e -> update(entity));

    @PostConstruct
    private void init() {
        this.setCloseOnOutsideClick(false);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "20rem").set("max-width", "100%");

        initializeUi(dialogLayout);
        add(dialogLayout);

        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.getStyle().set("margin-right", "auto");

        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.setDisableOnClick(true);

        updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        updateButton.setDisableOnClick(true);

        Button cancelButton = new Button("Cancel", e -> close());
        getFooter().add(deleteButton, cancelButton, createButton, updateButton);

        binder = new BeanValidationBinder<>(getEntityClass());
        initializeAdditionalBindings(binder);
        binder.bindInstanceFields(this);
        binder.addStatusChangeListener(e -> refreshButtons());
    }

    protected abstract Class<E> getEntityClass();

    protected String getEntityName() {
        return getEntityClass().getSimpleName();
    }

    protected abstract void initializeUi(VerticalLayout dialogLayout);

    protected void initializeAdditionalBindings(Binder<E> binder) {}

    public T openForCreation() {
        return openForCreation(null);
    }

    public final T openForCreation(Consumer<E> creationCallback) {
        this.creationCallback = creationCallback;
        refresh();
        setModeCreate();
        open();
        return (T) this;
    }

    public final T openForUpdate(@NotNull E entity) {
        return openForUpdate(entity, null, null);
    }

    public final T openForUpdate(@NotNull E entity, Consumer<E> updateCallback, Consumer<E> deletionCallback) {
        this.updateCallback = updateCallback;
        this.deletionCallback = deletionCallback;
        refresh();
        setModeUpdate(entity);
        open();
        return (T) this;
    }

    public void refresh() {}

    private void setModeCreate() {
        setHeaderTitle("Create " + getEntityName());
        entity = null;
        binder.readBean(null);
        refreshButtons();
    }

    private void setModeUpdate(@NotNull E entity) {
        setHeaderTitle("Modify " + getEntityName());
        this.entity = entity;
        binder.readBean(entity);
        refreshButtons();
    }

    private void refreshButtons() {
        createButton.setEnabled(true);
        updateButton.setEnabled(true);

        createButton.setVisible(entity == null);
        updateButton.setVisible(entity != null);
        deleteButton.setVisible(entity != null);
    }

    private void create() {
        if (binder.validate().isOk()) {
            E entity = null;
            try {
                entity = onCreate();
                update(entity); // update the rest of the fields, also closes the dialog
                var entityFinal = entity;
                Optional.ofNullable(creationCallback).ifPresent(c -> c.accept(entityFinal));
            } catch (ValidationException | DataIntegrityViolationException | TransactionSystemException e) {
                if (entity != null) {
                    delete();
                }
                NotificationUtil.showError(DBExceptionMapper.getMessage(e));
                createButton.setEnabled(true);
            }
        } else {
            createButton.setEnabled(true);
        }
    }

    private void update(E entity) {
        if (binder.writeBeanIfValid(entity)) {
            try {
                onUpdate(entity);
                close();
                Optional.ofNullable(updateCallback).ifPresent(c -> c.accept(entity));
            } catch (ValidationException | DataIntegrityViolationException | TransactionSystemException e) {
                NotificationUtil.showError(DBExceptionMapper.getMessage(e));
                updateButton.setEnabled(true);
            }
        } else {
            updateButton.setEnabled(true);
        }
    }

    private void delete() {
        openDeletionDialog(() -> {
            onDelete(entity);
            close();
            Optional.ofNullable(deletionCallback).ifPresent(c -> c.accept(entity));
        });
    }

    protected void openDeletionDialog(Runnable onConfirm) {
        ConfirmDeletionDialog.open(onConfirm);
    }

    protected final void checkOpened() {
        if (!isOpened()) {
            throw new IllegalStateException("Dialog is not opened");
        }
    }

    protected abstract E onCreate();

    protected abstract void onUpdate(E entity);

    protected abstract void onDelete(E entity);
}
