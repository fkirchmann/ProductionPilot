/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.influx;

import com.influxdb.annotations.Column;
import com.influxdb.client.domain.WritePrecision;
import java.time.Instant;

@com.influxdb.annotations.Measurement(name = InfluxMeasurement.MEASUREMENT_NAME)
public class InfluxMeasurement {
    public static final WritePrecision WRITE_PRECISION = WritePrecision.MS;

    public static final String MEASUREMENT_NAME = "measurement",
            MEASUREMENT_ID = "measurement_id",
            PARAMETER_ID = "parameter_id",
            PARAMETER_IDENTIFIER = "parameter_identifier",
            VALUE = "value",
            TIME = "time",
            OPC_STATUS_CODE = "opc_status_code",
            SOURCE_TIME = "source_time",
            SERVER_TIME = "server_time",
            CLIENT_TIME = "client_time";

    @Column(name = MEASUREMENT_ID)
    Long measurement_id;

    @Column(name = PARAMETER_ID, tag = true) // Tags must be strings, or else InfluxDB's query API will blow up
    String parameter_id;

    @Column(name = PARAMETER_IDENTIFIER, tag = true)
    String parameter_identifier;

    @Column(name = VALUE)
    Object value;

    @Column(name = TIME, timestamp = true)
    Instant time;

    @Column(name = OPC_STATUS_CODE, tag = true)
    String opc_status_code;

    @Column(name = SOURCE_TIME)
    long source_time;

    @Column(name = SERVER_TIME)
    long server_time;

    @Column(name = CLIENT_TIME)
    long client_time;
}
