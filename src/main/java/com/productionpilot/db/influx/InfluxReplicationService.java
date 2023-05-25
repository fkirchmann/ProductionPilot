/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.influx;

import com.productionpilot.db.timescale.entities.Measurement;
import com.productionpilot.db.timescale.service.MeasurementService;
import com.productionpilot.db.timescale.service.ParameterService;
import com.productionpilot.db.timescale.service.event.EntityCreatedEvent;
import com.productionpilot.service.ParameterRecordingService;
import com.productionpilot.util.DebugPerfTimer;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(InfluxDBService.class)
public class InfluxReplicationService {
    private final InfluxMeasurementService influxMeasurementService;
    private final ParameterRecordingService parameterRecordingService;

    private final ParameterService parameterService;
    private final MeasurementService measurementService;

    private final InfluxReplicationServiceTransactional transactional;

    private final BlockingQueue<Measurement> queue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void init() {
        var thread = new Thread(() -> {
            // Replicate previously recorded measurements
            log.info("Beginning replication of all existing measurements to InfluxDB");
            var timer = DebugPerfTimer.start("InfluxDB replication");
            transactional.replicateAllMeasurements();
            timer.endAndPrint(log);
            // Replicate new measurements
            try {
                while (true) {
                    var measurement = queue.take();
                    influxMeasurementService.record(measurement);
                }
            } catch (InterruptedException e) {
                log.warn("InfluxDB replication thread interrupted, stopping replication", e);
            } catch (Exception e) {
                log.error("Error while replicating measurement to InfluxDB, stopping replication", e);
            }
        });
        thread.setName("InfluxReplication");
        thread.setDaemon(true);
        thread.start();
    }

    @EventListener(EntityCreatedEvent.class)
    public void onMeasurementCreated(EntityCreatedEvent<Measurement> event) {
        queue.add(event.getEntity());
    }

    @Service
    @RequiredArgsConstructor
    @ConditionalOnBean(InfluxDBService.class)
    public static class InfluxReplicationServiceTransactional {
        private final InfluxMeasurementService influxMeasurementService;
        private final ParameterRecordingService parameterRecordingService;

        private final ParameterService parameterService;
        private final MeasurementService measurementService;

        /*@Transactional(readOnly = true)
        public void replicateHistory(Parameter parameter) {
            var lastMeasurementId = Optional.of(parameterRecordingService.getLastMeasurement(parameter))
                    .map(Measurement::getId);
            var lastInfluxMeasurementId = Optional.of(influxMeasurementService.getLastMeasurement(parameter)).map(InfluxMeasurement::getId);
            if(lastMeasurementId.isPresent()) {
                log.info("No measurements for {}", parameter);
                return;
            }
            if(lastInfluxMeasurement == null) {
                log.info("No influx measurements for {}, replicating all...", parameter);
                measurementService.streamByParameter(parameter).forEach(influxMeasurementService::record);
                log.info("Replication for {} finished", parameter);
            } else if(lastInfluxMeasurement.getId() < lastMeasurement.getId()) {
                log.info("Replicating missing measurements (last: {}, lastInflux: {}) for {}...", lastMeasurement.getId(),
                        lastInfluxMeasurement.getId(), parameter);
                measurementService.streamAfterMeasurement(lastInfluxMeasurement).forEach(influxMeasurementService::record);
                log.info("Replication for {} finished", parameter);
            }
        }*/

        @Transactional(readOnly = true)
        public void replicateAllMeasurements() {
            var lastMeasurement = measurementService.getLastMeasurement();
            var lastMeasurementId =
                    Optional.ofNullable(lastMeasurement).map(Measurement::getId).orElse(null);
            var lastInfluxMeasurementId = influxMeasurementService.getLastMeasurementId();
            if (lastMeasurement == null) {
                log.info("No measurements");
                return;
            }
            if (lastMeasurementId == null) {
                log.error("Last measurement has no id!! {}", lastMeasurement);
                return;
            }
            if (lastInfluxMeasurementId == null) {
                log.info("No influx measurements, replicating all...");
                measurementService.streamAll().forEach(influxMeasurementService::record);
                log.info("Replication finished");
            } else if (lastInfluxMeasurementId < lastMeasurementId) {
                log.info(
                        "Replicating missing measurements (last: {}, lastInflux: {})...",
                        lastMeasurement.getId(),
                        lastInfluxMeasurementId);
                measurementService
                        .streamByIdGreaterThan(lastInfluxMeasurementId)
                        .forEach(influxMeasurementService::record);
                log.info("Replication finished");
            }
        }
    }
}
