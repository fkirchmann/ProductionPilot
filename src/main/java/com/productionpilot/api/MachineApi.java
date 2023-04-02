/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.api;

import com.productionpilot.db.timescale.entities.Machine;
import com.productionpilot.db.timescale.service.MachineService;
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
@RequestMapping("/api/v1/machines")
@RequiredArgsConstructor
public class MachineApi {
    private final MachineService machineService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Machine> getMachines() {
        return machineService.findAll();
    }

    @GetMapping(value = "/id/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Machine getMachineById(@PathVariable long id) {
        return Optional.ofNullable(machineService.findById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Machine not found"));
    }
}
