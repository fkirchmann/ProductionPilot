/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.timescale.repository;

import com.productionpilot.db.timescale.entities.Parameter;
import com.productionpilot.db.timescale.entities.UnitOfMeasurement;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface ParameterRepository extends CrudRepository<Parameter, Long> {
    List<Parameter> findByOrderByIdAsc();

    List<Parameter> findByUnitOfMeasurementOrderByIdAsc(UnitOfMeasurement unitOfMeasurement);

    Parameter findByIdentifier(String identifier);
}
