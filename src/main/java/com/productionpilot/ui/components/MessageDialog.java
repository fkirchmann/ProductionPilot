/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;

public class MessageDialog extends Dialog {
    public static void show(String message) {
        MessageDialog dialog = new MessageDialog(message, "OK");
        dialog.open();
    }

    private MessageDialog(String message, String buttonText) {
        add(new Paragraph(message));
        Button closeButton = new Button(buttonText, event -> close());
        getFooter().add(closeButton);
    }
}
