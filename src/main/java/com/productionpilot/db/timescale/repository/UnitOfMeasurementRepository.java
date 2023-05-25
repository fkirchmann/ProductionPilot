/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.timescale.repository;

import com.productionpilot.db.timescale.entities.UnitOfMeasurement;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface UnitOfMeasurementRepository extends CrudRepository<UnitOfMeasurement, Long> {
    List<UnitOfMeasurement> findAllByOrderByNameAsc();
}
