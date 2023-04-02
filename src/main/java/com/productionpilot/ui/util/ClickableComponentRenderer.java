/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ClickableComponentRenderer<T> extends ComponentRenderer<Anchor, T> {
    public ClickableComponentRenderer(Supplier<Component> componentSupplier, Consumer<T> onClick) {
        super(batch -> {
            var anchor = new Anchor("#", componentSupplier.get());
            anchor.getElement().addEventListener("click", e -> onClick.accept(batch))
                    .addEventData("event.preventDefault()");
            return anchor;
        });
    }
}
