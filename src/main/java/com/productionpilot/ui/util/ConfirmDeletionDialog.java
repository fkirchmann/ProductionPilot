/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.util;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

public class ConfirmDeletionDialog extends ConfirmDialog {
    public static ConfirmDeletionDialog open(Runnable onConfirm) {
        return new ConfirmDeletionDialog(
                "Confirm Deletion",
                "Are you sure you want to delete this item? This action cannot be undone.",
                onConfirm);
    }

    public static ConfirmDeletionDialog open(String item, String text, Runnable onConfirm) {
        return new ConfirmDeletionDialog("Delete \"" + item + "\"?", text, onConfirm);
    }

    public static ConfirmDeletionDialog open(String item, Runnable onConfirm) {
        return new ConfirmDeletionDialog(
                "Delete \"" + item + "\"?",
                "Are you sure you want to delete this item? This action cannot be undone.",
                onConfirm);
    }

    private ConfirmDeletionDialog(String header, String text, Runnable onConfirm) {
        setHeader(header);
        setText(text);

        setCancelable(true);

        setConfirmText("Delete");
        setConfirmButtonTheme("error primary");
        addConfirmListener(event -> onConfirm.run());
        open();
    }
}
