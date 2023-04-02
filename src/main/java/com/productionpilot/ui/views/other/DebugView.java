/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.views.other;

import com.productionpilot.ui.components.FastRefreshRenderer;
import com.productionpilot.ui.components.StateRetainingTreeGrid;
import com.productionpilot.ui.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.List;

@PageTitle("Debug")
@Route(value = "debug", layout = MainLayout.class)
@RequiredArgsConstructor
@Slf4j
// TODO remove
public class DebugView extends VerticalLayout {

    private final StateRetainingTreeGrid<DebugObject> grid = new StateRetainingTreeGrid<>();
    private final FastRefreshRenderer<DebugObject> fastRefreshRenderer =
            new FastRefreshRenderer<>(DebugObject::getId, this);

    String dbgJs = """
                    console.log("executing js");
                    if(window.frtms === undefined) {
                      console.log("init!!");
                      window.frtms = {};
                      window.frtm_refresh = function() {
                        const elems = document.getElementsByClassName("fr-txt");
                        for(let i = 0; i < elems.length; i++) {
                          const elem = elems[i];
                          const id = elem.getAttribute("data-fr-id");
                          const val = window.frtms[id];
                          if(val !== undefined) {
                            elem.innerText = val;
                          }
                         }
                       };
                       console.log("init done");
                     };
            """;

    @PostConstruct
    public void init() {
        setSizeFull();
        var o = new DebugObject("ayy", 4);

        var dbgjsarea = new TextArea();
        dbgjsarea.setValue(dbgJs);
        dbgjsarea.setWidthFull();
        add(dbgjsarea);

        var toolbar = new HorizontalLayout();
        toolbar.add(new Button("++", e -> { o.value++; }));
        toolbar.add(new Button("Q1", e -> { fastRefreshRenderer.queueRefresh(o); }));
        toolbar.add(new Button("Qa", e -> { fastRefreshRenderer.queueRefreshAll(); }));
        toolbar.add(new Button("Push", e -> { fastRefreshRenderer.pushRefresh(); }));
        toolbar.add(new Button("dbg", e -> { this.getElement().executeJs(dbgjsarea.getValue()); }));
        add(toolbar);

        grid.addColumn(fastRefreshRenderer.createColumn(DebugObject::getName))
                .setHeader("Name");
        grid.addColumn(fastRefreshRenderer.createColumn(obj -> Integer.toString(obj.getValue())))
                .setHeader("Value");
        grid.setSizeFull();
        grid.setItems(List.of(o), obj -> List.of());
        add(grid);
    }

    private class DebugObject {
        private static int idCounter = 0;
        private DebugObject(String name, int value) {
            this.name = name;
            id = idCounter++;
            this.value = value;
        }

        @Getter
        int id;
        String name;

        public String getName() {
            //log.debug("Getting name for {} : {}", id, name);
            return name;
        }
        int value;

        public int getValue() {
            //log.debug("Getting value for {} : {}", id, value);
            return value;
        }
    }
}
