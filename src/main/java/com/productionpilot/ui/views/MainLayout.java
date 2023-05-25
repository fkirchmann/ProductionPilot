/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.views;

import com.productionpilot.ui.util.LineAwesomeIcon;
import com.productionpilot.ui.views.about.AboutView;
import com.productionpilot.ui.views.batches.BatchesView;
import com.productionpilot.ui.views.devices.DevicesView;
import com.productionpilot.ui.views.parameters.ParametersView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.router.RouterLink;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends AppLayout {

    /**
     * A simple navigation item component, based on ListItem element.
     */
    public static class MenuItemInfo extends ListItem {

        private final Class<? extends Component> view;

        public MenuItemInfo(String menuTitle, String iconClass, Class<? extends Component> view) {
            this.view = view;
            RouterLink link = new RouterLink();
            // Use Lumo classnames for various styling
            link.addClassNames("flex", "gap-xs", "h-m", "items-center", "px-s", "text-body");
            link.setRoute(view);

            Span text = new Span(menuTitle);
            // Use Lumo classnames for various styling
            text.addClassNames("font-medium", "text-m", "whitespace-nowrap");

            link.add(new LineAwesomeIcon(iconClass), text);
            add(link);
        }

        public Class<?> getView() {
            return view;
        }
    }

    public MainLayout() {
        addToNavbar(createHeaderContent());
    }

    private Component createHeaderContent() {
        Header header = new Header();
        header.addClassNames("box-border", "flex", "flex-col", "w-full");

        Div layout = new Div();
        layout.addClassNames("flex", "items-center", "px-l");

        // Make ProductionPilot on the top left a link to the root of the app
        var appNameLink = new Anchor("/", "ProductionPilot");
        appNameLink.addClassNames("text-body"); // prevents link from being blue
        var appName = new H1(appNameLink);
        appName.addClassNames("my-m", "me-auto", "text-l");
        layout.add(appName);

        // add logo to right side of header, set height to 50px
        Image logo = new Image("icons/icon.png", "logo");
        logo.setHeight("30px");
        layout.add(logo);

        Nav nav = new Nav();
        nav.addClassNames("flex", "overflow-auto", "px-m", "py-xs");

        // Wrap the links in a list; improves accessibility
        UnorderedList list = new UnorderedList();
        list.addClassNames("flex", "gap-s", "list-none", "m-0", "p-0");
        nav.add(list);

        Arrays.stream(MenuItem.values()).map(MenuItem::toMenuItemInfo).forEach(list::add);

        header.add(layout, nav);
        return header;
    }

    @RequiredArgsConstructor
    @Getter
    public enum MenuItem {
        DEVICES("Devices", "la la-tags", DevicesView.class),
        PARAMETERS("Parameters", "la la-list-ul", ParametersView.class),
        BATCHES("Batches & Export", "la la-folder", BatchesView.class),
        ABOUT("About", "la la-info-circle", AboutView.class);

        private final String name;
        private final String iconClass;
        private final Class<? extends Component> view;

        public MenuItemInfo toMenuItemInfo() {
            return new MenuItemInfo(name, iconClass, view);
        }
    }
}
