/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class UIFormatters {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")
            .withLocale(Locale.ENGLISH).withZone(ZoneId.systemDefault());
    public static final DateTimeFormatter DATE_TIME_FORMATTER_SECONDS = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss")
            .withLocale(Locale.ENGLISH).withZone(ZoneId.systemDefault());
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withLocale(Locale.ENGLISH).withZone(ZoneId.systemDefault());
}
