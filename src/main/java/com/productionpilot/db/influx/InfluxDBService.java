/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import java.time.format.DateTimeFormatter;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "com.productionpilot.influxdb", name = "url")
public class InfluxDBService {
    public static final DateTimeFormatter INFLUX_DT_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Value("${com.productionpilot.influxdb.url}")
    private String url;

    @Value("${com.productionpilot.influxdb.token}")
    private String token;

    @Value("${com.productionpilot.influxdb.org}")
    @Getter
    private String organization;

    @Value("${com.productionpilot.influxdb.bucket}")
    @Getter
    private String bucket;

    @Value("${com.productionpilot.influxdb.batch-size:100}")
    private int batchSize;

    @Value("${com.productionpilot.influxdb.batch-interval:1000}")
    private int batchInterval;

    @Getter
    private InfluxDBClient influxDB;

    @Getter
    private WriteApi writeApi;

    @PostConstruct
    public void init() {
        influxDB = InfluxDBClientFactory.create(url, token.toCharArray(), organization, bucket);
        writeApi = influxDB.makeWriteApi(WriteOptions.builder()
                .batchSize(batchSize)
                .flushInterval(batchInterval)
                .build());
    }
}
