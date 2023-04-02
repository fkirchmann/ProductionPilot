/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.views.other;

import com.productionpilot.db.timescale.entities.Machine;
import com.productionpilot.db.timescale.service.MachineService;
import com.productionpilot.db.timescale.service.ParameterService;
import com.productionpilot.service.ParameterRecordingService;
import com.productionpilot.ui.views.MainLayout;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.PostConstruct;

@PageTitle("Generate Telegraf Config")
@Route(value = "make_telegraf", layout = MainLayout.class)
@RequiredArgsConstructor
@Slf4j
public class TelegrafConfigGenerator extends VerticalLayout {

    private final MachineService machineService;
    private final ParameterService parameterService;
    private final ParameterRecordingService parameterRecordingService;

    private final ComboBox<Machine> machineComboBox = new ComboBox<>();
    private final TextArea textArea = new TextArea();

    @PostConstruct
    private void init() {
        setSizeFull();

        machineComboBox.setWidth("30em");
        var toolbar = new HorizontalLayout(new Text("Machine: "), machineComboBox);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.BASELINE);
        add(toolbar);
        add(textArea);

        machineComboBox.setPlaceholder("Please select a machine");
        machineComboBox.setItems(machineService.findAll());
        machineComboBox.addValueChangeListener(event -> {
            var machine = event.getValue();
            if (machine == null) {
                textArea.clear();
                return;
            }
            textArea.setValue(generateConfig(machine));
        });

        setFlexGrow(0, toolbar);
        setFlexGrow(1, textArea);
        textArea.setReadOnly(true);
        textArea.setWidthFull();
        textArea.setHeightFull();
        textArea.getStyle().set("white-space", "pre")
                .set("overflow", "auto")
                .set("padding-bottom", "1em");
    }

    private String generateConfig(Machine machine) {
        StringBuilder sb = new StringBuilder();
        sb.append("nodes = [\n");
        var list = machine.getParameters().stream()
                .filter(param -> param.getIdentifier() != null)
                .map(param -> Pair.of(param, parameterRecordingService.getSubscribedItem(param)))
                .filter(pair -> pair.getRight() != null)
                .toList();
        for(int i = 0; i < list.size(); i++) {
            var entry = list.get(i);
            sb.append("\t{ name=\"%s\", namespace=\"3\", identifier_type=\"s\", identifier=\"%s\" }"
                    .formatted(entry.getKey().getIdentifier(), entry.getValue().getNode().getPath()));
            if(i < list.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        var parametersMissingIdentifiers = machine.getParameters().stream()
                .filter(param -> param.getIdentifier() == null)
                .toList();
        if(!parametersMissingIdentifiers.isEmpty()) {
            sb.append("\n\n");
            sb.append("## The following parameters are missing identifiers and are not included in the list above:\n");
            for(var param : parametersMissingIdentifiers) {
                sb.append("# %s".formatted(param.getName()));
                var paramSubscribedItem = parameterRecordingService.getSubscribedItem(param);
                if(paramSubscribedItem != null) {
                    sb.append(" (Tag: %s)".formatted(paramSubscribedItem.getNode().getPath()));
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
