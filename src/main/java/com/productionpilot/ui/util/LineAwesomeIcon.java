/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.util;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Span;

/**
 * Simple wrapper to create icons using LineAwesome iconset. See
 * https://icons8.com/line-awesome
 */
@NpmPackage(value = "line-awesome", version = "1.3.0")
public class LineAwesomeIcon extends Span {
    public LineAwesomeIcon(String lineawesomeClassnames) {
        // Use Lumo classnames for suitable font styling
        addClassNames("text-l", "text-secondary");
        if (!lineawesomeClassnames.isEmpty()) {
            addClassNames(lineawesomeClassnames);
        }
    }
}
