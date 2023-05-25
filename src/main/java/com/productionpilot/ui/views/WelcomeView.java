/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.views;

import com.productionpilot.ui.util.LineAwesomeIcon;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import javax.annotation.PostConstruct;

/**
 * The welcome page is shown to the user when first accessing the application, and should provide a high-level overview
 * of the application and its capabilities, providing some guidance to the user on how to achieve their goal.
 */
@PageTitle("Welcome")
@Route(value = "welcome", layout = MainLayout.class)
public class WelcomeView extends VerticalLayout {

    @PostConstruct
    public void init() {
        setSizeFull();
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        setAlignItems(FlexComponent.Alignment.CENTER);

        createButtonWithLayout(MainLayout.MenuItem.DEVICES, "Configure new tags to record");
        createButtonWithLayout(MainLayout.MenuItem.PARAMETERS, "See what is being recorded");
        createButtonWithLayout(MainLayout.MenuItem.BATCHES, "Select and export data for research");
    }

    private void createButtonWithLayout(MainLayout.MenuItem item, String description) {
        createButtonWithLayout(item.getIconClass(), item.getName(), description, item.getView());
    }

    private void createButtonWithLayout(
            String iconClass, String header, String description, Class<? extends Component> forwardTo) {
        var buttonIcon = new LineAwesomeIcon(iconClass);
        buttonIcon.getElement().getStyle().set("font-size", "2em").set("margin-top", "3px");

        Label headerLabel = new Label(header);
        headerLabel.getStyle().set("font-weight", "bold");
        headerLabel.getStyle().set("font-size", "24px");
        headerLabel.getStyle().set("cursor", "pointer");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyle().set("font-size", "16px");
        descriptionLabel.getStyle().set("cursor", "pointer");

        VerticalLayout textLayout = new VerticalLayout(headerLabel, descriptionLabel);
        textLayout.addClassName("text-body");
        textLayout.getStyle().set("padding", "0");
        textLayout.setSpacing(false);
        textLayout.setMargin(false);

        var spacer = new Div();
        HorizontalLayout buttonContent = new HorizontalLayout(buttonIcon, textLayout, spacer);
        buttonContent.setAlignItems(Alignment.CENTER);
        buttonContent.getStyle().set("width", "330px");
        buttonContent.getStyle().set("margin-bottom", "5px");

        Button button = new Button(buttonContent);
        button.getElement().getStyle().set("font-size", "24px");
        button.getElement().getStyle().set("min-width", "350px");
        button.getElement().getStyle().set("min-height", "2.4em");
        button.getElement().getStyle().set("padding", "10px");
        button.getElement().getStyle().set("cursor", "pointer");
        button.setHeight("fit-content");
        button.addClickListener(e -> button.getUI().ifPresent(ui -> ui.navigate(forwardTo)));
        add(button);
    }
}
