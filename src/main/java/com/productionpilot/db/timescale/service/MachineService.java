/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.service;

import com.productionpilot.db.timescale.repository.MachineRepository;
import com.productionpilot.db.timescale.entities.Machine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineService {
    private final MachineRepository machineRepository;

    public List<Machine> findAll() {
        return machineRepository.findByOrderByIdAsc();
    }

    public Machine findById(long id) {
        return machineRepository.findById(id).orElse(null);
    }

    public void update(Machine machine) {
        machineRepository.save(machine);
    }

    public void delete(Machine machine) {
        machineRepository.delete(machine);
    }

    @Transactional
    public Machine create(String name) {
        Machine machine = new Machine();
        machine.setName(name);
        machineRepository.save(machine);
        return machine;
    }
}
