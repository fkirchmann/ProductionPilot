/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.api;

import com.productionpilot.db.timescale.entities.Parameter;
import com.productionpilot.db.timescale.service.ParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/parameters")
@RequiredArgsConstructor
public class ParameterApi {
    private final ParameterService parameterService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Parameter> getParameters() {
        return parameterService.findAll();
    }

    @GetMapping(value = "/id/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Parameter getParameterById(@PathVariable long id) {
        return Optional.ofNullable(parameterService.findById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found"));
    }

    @GetMapping(value = "/identifier/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Parameter getParameterByIdentifier(@PathVariable String identifier) {
        return Optional.ofNullable(parameterService.findByIdentifier(identifier))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found"));
    }
}
