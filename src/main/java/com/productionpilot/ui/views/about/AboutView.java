/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.views.about;

import com.productionpilot.ui.views.MainLayout;
import com.productionpilot.ui.views.other.TelegrafConfigGenerator;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

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
        add(new Paragraph(
                "At the Chair of Production Engineering of E-Mobility Components" + " at RWTH Aachen University"));

        add(new Paragraph("Advanced Features:"));
        var restApiLink = new Anchor("/api-docs/v1/swagger-ui.html", "REST API Documentation");
        restApiLink.setTarget(AnchorTarget.BLANK);
        add(new Paragraph(restApiLink));
        add(new Paragraph(new RouterLink("Telegraf Config Generator", TelegrafConfigGenerator.class)));

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        getStyle().set("text-align", "center");
    }
}
