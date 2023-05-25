/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.timescale.service;

import com.productionpilot.db.timescale.entities.Measurement;
import com.productionpilot.db.timescale.entities.Parameter;
import com.productionpilot.db.timescale.repository.MeasurementRepository;
import com.productionpilot.opc.OpcMeasuredValue;
import java.time.Instant;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeasurementService {
    private final MeasurementRepository measurementRepository;

    public Measurement recordMeasurement(Parameter parameter, OpcMeasuredValue measuredValue) {
        var measurement = new Measurement();
        measurement.setParameterId(parameter.getId());
        measurement.setOpcStatusCode(measuredValue.getStatusCode().getCode());
        measurement.setSourceTime(measuredValue.getSourceTime());
        measurement.setServerTime(measuredValue.getServerTime());
        measurement.setClientTime(measuredValue.getClientTime());
        var value = measuredValue.getValue();
        if (value instanceof Double doubleValue) {
            measurement.setValueDouble(doubleValue);
        } else if (value instanceof Long longValue) {
            measurement.setValueLong(longValue);
        } else if (value instanceof Boolean boolValue) {
            measurement.setValueBoolean(boolValue);
        } else {
            measurement.setValueString(measuredValue.getValueAsString());
        }
        measurementRepository.save(measurement);
        return measurement;
    }

    public long countByParameter(Parameter parameter) {
        return measurementRepository.countByParameterId(parameter.getId());
    }

    public Measurement getLastMeasurement() {
        return measurementRepository.findFirstByOrderByIdDesc();
    }

    public Measurement getLastMeasurement(Parameter parameter) {
        return measurementRepository
                .findFirstByParameterIdOrderByIdDesc(parameter.getId())
                .orElse(null);
    }

    public Stream<Measurement> streamAll() {
        return measurementRepository.streamAllByOrderByIdAsc();
    }

    public Stream<Measurement> streamByIdGreaterThan(long id) {
        return measurementRepository.streamByIdGreaterThanOrderByIdAsc(id);
    }

    public Stream<Measurement> streamByParameterAndTimeRange(Parameter parameter, Instant startTime, Instant endTime) {
        return this.streamByParameterAndTimeRange(parameter.getId(), startTime, endTime);
    }

    public Stream<Measurement> streamByParameterAndTimeRange(long parameterId, Instant startTime, Instant endTime) {
        return measurementRepository.streamByParameterIdAndClientTimeBetweenOrderByIdAsc(
                parameterId, startTime, endTime);
    }

    /**
     * Returns all measurements for the given measurement's parameter that follow after the given measurement
     * (i.e., have a higher id).
     */
    public Stream<Measurement> streamAfterMeasurement(Measurement measurement) {
        return measurementRepository.streamByParameterIdAndIdGreaterThanOrderByIdAsc(
                measurement.getParameterId(), measurement.getId());
    }

    /**
     * Returns all measurements for the given parameter in ascending order (by ID).
     */
    public Stream<Measurement> streamByParameter(Parameter parameter) {
        return measurementRepository.streamByParameterIdOrderByIdAsc(parameter.getId());
    }

    public void update(Measurement measurement) {
        measurementRepository.save(measurement);
    }

    public void delete(Measurement measurement) {
        measurementRepository.delete(measurement);
    }
}
