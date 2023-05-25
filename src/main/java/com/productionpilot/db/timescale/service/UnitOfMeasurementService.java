/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.timescale.service;

import com.productionpilot.db.timescale.entities.UnitOfMeasurement;
import com.productionpilot.db.timescale.repository.UnitOfMeasurementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UnitOfMeasurementService {
    private final UnitOfMeasurementRepository unitOfMeasurementRepository;

    public List<UnitOfMeasurement> findAll() {
        return unitOfMeasurementRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public UnitOfMeasurement create(String name, String abbreviation) {
        UnitOfMeasurement unitOfMeasurement = new UnitOfMeasurement();
        unitOfMeasurement.setName(name);
        unitOfMeasurement.setAbbreviation(abbreviation);
        unitOfMeasurementRepository.save(unitOfMeasurement);
        return unitOfMeasurement;
    }

    public void update(UnitOfMeasurement unitOfMeasurement) {
        unitOfMeasurementRepository.save(unitOfMeasurement);
    }

    public void delete(UnitOfMeasurement unitOfMeasurement) {
        unitOfMeasurementRepository.delete(unitOfMeasurement);
    }
}
