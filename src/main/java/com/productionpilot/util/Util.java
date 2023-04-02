/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.util;

public class Util {
    private Util() {
    }

    public static String filterFilename(String name) {
        return name.replace("\"", "")
                .replace("/", "-")
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", " ")
                .replace("\\", "-")
                .replace("*", "")
                .replace("?", "")
                .replace("<", "")
                .replace(">", "")
                .replace("|", "")
                .replace(":", "-");
    }
}
