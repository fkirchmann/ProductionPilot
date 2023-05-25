/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.api.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.productionpilot.db.timescale.entities.Machine;
import java.io.IOException;

public class MachineSerializer extends StdSerializer<Machine> {
    public MachineSerializer() {
        super(Machine.class);
    }

    @Override
    public void serialize(Machine machine, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("id", machine.getId());
        gen.writeStringField("name", machine.getName());
        gen.writeStringField("description", machine.getDescription());
        gen.writeEndObject();
    }
}
