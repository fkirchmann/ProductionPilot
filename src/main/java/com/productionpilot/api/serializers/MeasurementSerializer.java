/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.api.serializers;

import com.productionpilot.db.timescale.entities.Measurement;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class MeasurementSerializer extends StdSerializer<Measurement> {
    public MeasurementSerializer() {
        super(Measurement.class);
    }

    @Override
    public void serialize(Measurement measurement, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("id", measurement.getId());
        gen.writeNumberField("opcStatusCode", measurement.getOpcStatusCode());
        gen.writeStringField("clientTime", ApiFormatters.API_DATETIME_FORMATTER.format(measurement.getClientTime()));
        gen.writeStringField("serverTime", ApiFormatters.API_DATETIME_FORMATTER.format(measurement.getServerTime()));
        gen.writeStringField("sourceTime", ApiFormatters.API_DATETIME_FORMATTER.format(measurement.getSourceTime()));
        if(measurement.getValueDouble() != null) {
            gen.writeNumberField("value", measurement.getValueDouble());
        } else if(measurement.getValueLong() != null) {
            gen.writeNumberField("value", measurement.getValueLong());
        } else if(measurement.getValueBoolean() != null) {
            gen.writeBooleanField("value", measurement.getValueBoolean());
        } else if(measurement.getValueString() != null) {
            gen.writeStringField("value", measurement.getValueString());
        } else {
            gen.writeObjectField("value", measurement.getValue());
        }
        gen.writeEndObject();
    }
}
