/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.util;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class NotificationUtil {
    private static final int DEFAULT_DURATION = 5000; // ms

    public static void showError(String message) {
        show(message, VaadinIcon.WARNING.create(), NotificationVariant.LUMO_ERROR);
    }

    public static void showSuccess(String message) {
        show(message, VaadinIcon.CHECK_CIRCLE.create(), NotificationVariant.LUMO_SUCCESS);
    }

    private static void show(String message, Icon icon, NotificationVariant variant) {
        var notification = new Notification();
        notification.setDuration(DEFAULT_DURATION);
        var info = new Div(new Text(message));
        notification.add(new HorizontalLayout(icon, info));
        notification.addThemeVariants(variant);
        notification.open();
    }
}
