/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.service;

import com.productionpilot.db.timescale.repository.ParameterRepository;
import com.productionpilot.db.timescale.entities.Machine;
import com.productionpilot.db.timescale.entities.Parameter;
import com.productionpilot.db.timescale.entities.UnitOfMeasurement;
import com.productionpilot.opc.OpcNode;
import com.productionpilot.opc.OpcNodeId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParameterService {
    private final ParameterRepository parameterRepository;

    public List<Parameter> findAll() {
        return parameterRepository.findByOrderByIdAsc();
    }

    public List<Parameter> findByUnitOfMeasurement(UnitOfMeasurement unitOfMeasurement) {
        return parameterRepository.findByUnitOfMeasurementOrderByIdAsc(unitOfMeasurement);
    }

    public Parameter findByIdentifier(String identifier) {
        return parameterRepository.findByIdentifier(identifier);
    }

    public Parameter findById(long id) {
        return parameterRepository.findById(id).orElse(null);
    }

    public void update(Parameter parameter) {
        parameterRepository.save(parameter);
    }

    public void delete(Parameter parameter) {
        //parameterRepository.delete(parameter); -- did not work, for some reason
        parameter.setDeleted(true);
        parameterRepository.save(parameter);
    }

    @Transactional
    public Parameter create(OpcNodeId nodeId, Machine machine, String name) {
        Parameter parameter = new Parameter();
        parameter.setOpcNodeId(nodeId.toParseableString());
        parameter.setMachine(machine);
        parameter.setName(name);
        parameterRepository.save(parameter);
        return parameter;
    }
}
