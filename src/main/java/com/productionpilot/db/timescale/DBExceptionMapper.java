/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import javax.validation.ValidationException;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class DBExceptionMapper {
    private static final Pattern CONSTRAINT_NAME_PATTERN = Pattern.compile("; constraint \\[([^;]+?)];");
    //private static final Pattern CONSTRAINT_NAME_PATTERN = Pattern.compile("constraint \"([^\"]+)\"");

    private static final Map<String, String> CONSTRAINT_ERROR_MESSAGES = Map.of(
            "machine_unique", "A machine with this name already exists.",
            "parameter_unique", "A Parameter with this name already exists for this Machine.",
            "parameter_unique_identifier", "This Parameter identifier is already in use by another Parameter.",
            "batch_unique", "A Batch with this name already exists under the specified parent Batch.",
            "batch_machine_unique", "This machine with this time range already exists in this batch.",
            "batch_machine_end_time_after_start_time", "The end time must be after the start time.",
            "uom_unique", "A Unit of Measurement with this name and abbreviation already exists.");

    public static String getMessage(Exception e) {
        if (e instanceof DataIntegrityViolationException && e.getMessage() != null) {
            var matcher = CONSTRAINT_NAME_PATTERN.matcher(e.getMessage());
            if(matcher.find()) {
                var constraintName = matcher.group(1);
                var message = CONSTRAINT_ERROR_MESSAGES.get(constraintName);
                if(message != null) {
                    return message;
                }
            }
        } else if (e instanceof ValidationException) {
            return e.getMessage();
        }
        log.warn("Could not map Database Exception", e);
        return "An unknown error occurred. Please contact the administrator.";
    }
}
