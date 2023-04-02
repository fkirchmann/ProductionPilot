/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.views.about;

import com.productionpilot.ui.views.MainLayout;
import com.productionpilot.ui.views.other.TelegrafConfigGenerator;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;

@PageTitle("About")
@Route(value = "about", layout = MainLayout.class)
@RequiredArgsConstructor
public class AboutView extends VerticalLayout {
    @PostConstruct
    private void init() {
        setSpacing(false);

        Image img = new Image("icons/icon.png", "logo");
        img.setWidth("100px");
        add(img);

        add(new H2("ProductionPilot"));
        add(new Paragraph("Created by Felix Kirchmann in 2022 - 23"));
        add(new Paragraph("At the Chair of Production Engineering of E-Mobility Components" +
                " at RWTH Aachen University"));

        add(new Paragraph("Advanced Features:"));
        add(new RouterLink("Telegraf Config Generator", TelegrafConfigGenerator.class));


        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        getStyle().set("text-align", "center");
    }
}
