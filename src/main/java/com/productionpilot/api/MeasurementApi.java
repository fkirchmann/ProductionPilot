/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.api;

import com.productionpilot.db.timescale.service.MeasurementService;
import com.productionpilot.db.timescale.service.ParameterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/v1/measurements")
@RequiredArgsConstructor
public class MeasurementApi {
    private final MeasurementService measurementService;
    private final ParameterService parameterService;

    private final ObjectMapper mapper;

    @SneakyThrows(IOException.class)
    @GetMapping(value = "parameter_id/{parameterId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public void getMeasurements(HttpServletResponse response, @PathVariable long parameterId,
                                @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime start,
                                @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime end) {
        var startInstant = start == null ? Instant.EPOCH : start.toInstant();
        var endInstant = end == null ? Instant.now().plusSeconds(60 * 60 * 24) : end.toInstant();
        mapper.writeValue(response.getWriter(),
                measurementService.streamByParameterAndTimeRange(parameterId, startInstant, endInstant));
    }

    @GetMapping(value = "parameter_identifier/{parameterIdentifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public void getMeasurements(HttpServletResponse response, @PathVariable String parameterIdentifier,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime start,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime end) {
        var parameter = parameterService.findByIdentifier(parameterIdentifier);
        if(parameter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found");
        }
        getMeasurements(response, parameter.getId(), start, end);
    }
}
