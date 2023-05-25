/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.influx;

import com.influxdb.query.dsl.Flux;
import com.influxdb.query.dsl.functions.restriction.Restrictions;
import com.productionpilot.db.timescale.entities.Measurement;
import com.productionpilot.db.timescale.service.ParameterService;
import java.time.ZonedDateTime;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(InfluxDBService.class)
public class InfluxMeasurementService {
    private static final Restrictions RESTRICT_IS_MEASUREMENT =
            Restrictions.measurement().equal(InfluxMeasurement.MEASUREMENT_NAME);
    private final InfluxDBService influxDBService;

    private final ParameterService parameterService;

    @Getter
    private Long lastMeasurementId;

    @PostConstruct
    public void init() {
        lastMeasurementId = queryLastMeasurementIdFromDb();
    }

    public void record(Measurement measurement) {
        influxDBService.getWriteApi().writeMeasurement(InfluxMeasurement.WRITE_PRECISION, fromMeasurement(measurement));
        if (lastMeasurementId == null || measurement.getId() > lastMeasurementId) {
            lastMeasurementId = measurement.getId();
        }
    }

    private Long queryLastMeasurementIdFromDb() {
        var query = Flux.from(influxDBService.getBucket())
                .range(ZonedDateTime.now().minusYears(5).toInstant())
                .filter(RESTRICT_IS_MEASUREMENT)
                .filter(Restrictions.column("_field").equal(InfluxMeasurement.MEASUREMENT_ID))
                .max()
                .toString();
        log.debug("Querying last measurement id from InfluxDB: {}", query);
        var result = influxDBService.getInfluxDB().getQueryApi().query(query);
        if (result.isEmpty()) {
            log.debug("Empty result from InfluxDB");
            return null;
        }
        var records = result.get(0).getRecords();
        if (records.isEmpty()) {
            log.debug("Empty records from InfluxDB");
            return null;
        }
        var highestMeasurementId = Long.parseLong(records.get(0).getValue() + "");
        log.debug("Highest measurement id from InfluxDB: {}", highestMeasurementId);
        return highestMeasurementId;
    }

    /*
    For some reason, influxdb-client-java fails to retrieve the measurement_id. I have given up on
    trying to figure out why, as this functionality is not needed for the current use case.

    public Measurement toMeasurement(InfluxMeasurement influxMeasurement) {
        var measurement = new com.productionpilot.db.timescale.entities.Measurement();
        if(influxMeasurement.measurement_id == null) {
            throw new IllegalArgumentException("Measurement ID is null");
        }
        measurement.setId(Long.parseLong(influxMeasurement.measurement_id));
        measurement.setParameterId(Long.parseLong(influxMeasurement.parameter_id));
        measurement.setSourceTime(Instant.ofEpochMilli(influxMeasurement.source_time));
        measurement.setServerTime(Instant.ofEpochMilli(influxMeasurement.server_time));
        measurement.setClientTime(Instant.ofEpochMilli(influxMeasurement.client_time));
        measurement.setOpcStatusCode(Long.valueOf(influxMeasurement.opc_status_code));
        if(influxMeasurement.value instanceof Double) {
            measurement.setValueDouble((Double) influxMeasurement.value);
        } else if(influxMeasurement.value instanceof Long) {
            measurement.setValueLong((Long) influxMeasurement.value);
        } else if(influxMeasurement.value instanceof Boolean) {
            measurement.setValueBoolean((Boolean) influxMeasurement.value);
        } else {
            measurement.setValueString(influxMeasurement.value.toString());
        }
        return measurement;
    }*/

    public InfluxMeasurement fromMeasurement(Measurement measurement) {
        var influxMeasurement = new InfluxMeasurement();
        influxMeasurement.measurement_id = measurement.getId();
        influxMeasurement.parameter_id = Long.toString(measurement.getParameterId());
        var parameter = parameterService.findById(measurement.getParameterId());
        if (parameter != null) {
            influxMeasurement.parameter_identifier = parameter.getIdentifier();
        } else {
            influxMeasurement.parameter_identifier = null;
        }
        influxMeasurement.time = measurement.getClientTime();
        influxMeasurement.source_time = measurement.getSourceTime().toEpochMilli();
        influxMeasurement.server_time = measurement.getServerTime().toEpochMilli();
        influxMeasurement.client_time = measurement.getClientTime().toEpochMilli();
        influxMeasurement.opc_status_code = Long.toString(measurement.getOpcStatusCode());
        influxMeasurement.value = measurement.getValue();
        return influxMeasurement;
    }
}
