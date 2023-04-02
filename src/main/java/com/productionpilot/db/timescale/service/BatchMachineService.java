/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.service;


import com.productionpilot.db.timescale.entities.Batch;
import com.productionpilot.db.timescale.entities.BatchMachine;
import com.productionpilot.db.timescale.entities.Machine;
import com.productionpilot.db.timescale.repository.BatchMachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchMachineService {
    private final BatchMachineRepository batchMachineRepository;

    public List<BatchMachine> findAll() {
        return batchMachineRepository.findAll();
    }

    public List<BatchMachine> findAllByBatch(Batch batch) {
        return batchMachineRepository.findByBatch(batch);
    }

    @Transactional
    public BatchMachine create(Batch batch, Machine machine, Instant startTime, Instant endTime) {
        BatchMachine batchMachine = new BatchMachine();
        batchMachine.setBatch(batch);
        batchMachine.setMachine(machine);
        batchMachine.setStartTime(startTime);
        batchMachine.setEndTime(endTime);
        batchMachineRepository.save(batchMachine);
        return batchMachine;
    }

    public void update(BatchMachine batchMachine) {
        batchMachineRepository.save(batchMachine);
    }

    public void delete(BatchMachine batchMachine) {
        batchMachineRepository.delete(batchMachine);
    }
}
