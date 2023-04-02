/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.api.serializers;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ApiFormatters {
    public static final DateTimeFormatter API_DATETIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault());
}
