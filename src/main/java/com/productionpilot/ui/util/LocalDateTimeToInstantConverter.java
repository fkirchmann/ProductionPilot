/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.util;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class LocalDateTimeToInstantConverter implements Converter<LocalDateTime, Instant> {
    @Override
    public Result<Instant> convertToModel(LocalDateTime value, ValueContext context) {
        return Result.ok(convert(value));
    }

    @Override
    public LocalDateTime convertToPresentation(Instant value, ValueContext context) {
        return convert(value);
    }

    public Instant convert(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneOffset.systemDefault()).toInstant();
    }

    public LocalDateTime convert(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.systemDefault());
    }
}
