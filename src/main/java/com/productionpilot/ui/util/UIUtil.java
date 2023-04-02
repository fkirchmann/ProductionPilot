/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.util;

import com.vaadin.flow.component.HasElement;

public class UIUtil {
    public static void scrollInputBegin(HasElement e) {
        e.getElement().executeJs("this.inputElement.scrollTop = 0; this.inputElement.scrollLeft = 0;");
    }

    public static void scrollInputEnd(HasElement e) {
        e.getElement().executeJs("this.inputElement.scrollTop = this.inputElement.scrollHeight;" +
                "this.inputElement.scrollLeft = this.inputElement.scrollWidth;");
    }
}
