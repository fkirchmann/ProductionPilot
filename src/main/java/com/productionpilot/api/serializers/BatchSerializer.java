/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.api.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.productionpilot.db.timescale.entities.Batch;
import java.io.IOException;
import java.util.Optional;

public class BatchSerializer extends StdSerializer<Batch> {
    public BatchSerializer() {
        super(Batch.class);
    }

    @Override
    public void serialize(Batch value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("id", value.getId());
        gen.writeStringField("name", value.getName());
        gen.writeStringField("description", value.getDescription());
        gen.writeStringField("creationTime", ApiFormatters.API_DATETIME_FORMATTER.format(value.getCreationTime()));
        gen.writeStringField(
                "modificationTime", ApiFormatters.API_DATETIME_FORMATTER.format(value.getModificationTime()));
        gen.writeObjectField(
                "parentBatchId",
                Optional.ofNullable(value.getParentBatch()).map(Batch::getId).orElse(null));
        gen.writeEndObject();
    }
}
