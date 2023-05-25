/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.api.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.productionpilot.db.timescale.entities.Parameter;
import java.io.IOException;

public class ParameterSerializer extends StdSerializer<Parameter> {
    public ParameterSerializer() {
        super(Parameter.class);
    }

    @Override
    public void serialize(Parameter parameter, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("id", parameter.getId());
        gen.writeStringField("name", parameter.getName());
        gen.writeStringField("description", parameter.getDescription());
        gen.writeStringField("identifier", parameter.getIdentifier());
        gen.writeNumberField(
                "samplingIntervalMs", parameter.getSamplingInterval().toMillis());
        gen.writeObjectField("machineId", parameter.getMachine().getId());
        gen.writeObjectField("unitOfMeasurement", parameter.getUnitOfMeasurement());
        gen.writeEndObject();
    }
}
