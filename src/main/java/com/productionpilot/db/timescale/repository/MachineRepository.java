/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.repository;

import com.productionpilot.db.timescale.entities.Machine;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MachineRepository extends CrudRepository<Machine, Long> {
    List<Machine> findByOrderByIdAsc();
}
