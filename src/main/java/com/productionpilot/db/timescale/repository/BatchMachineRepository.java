/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.repository;

import com.productionpilot.db.timescale.entities.Batch;
import com.productionpilot.db.timescale.entities.BatchMachine;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface BatchMachineRepository extends CrudRepository<BatchMachine, Long> {
    @NotNull
    List<BatchMachine> findAll();
    @NotNull
    List<BatchMachine> findByBatch(Batch batch);
}
