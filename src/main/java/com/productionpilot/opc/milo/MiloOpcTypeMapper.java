/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc.milo;

import com.productionpilot.opc.OpcMeasuredValue;
import com.productionpilot.opc.OpcNode;
import com.productionpilot.opc.OpcNodeType;
import com.productionpilot.opc.OpcStatusCode;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

@Slf4j
public class MiloOpcTypeMapper {
    private static final Map<Class<?>, OpcNodeType> VARIABLE_TYPE_MAP = createVariableTypeMap();

    private static Map<Class<?>, OpcNodeType> createVariableTypeMap() {
        var map = new HashMap<Class<?>, OpcNodeType>();
        map.put(Boolean.class, OpcNodeType.VAR_BOOLEAN);
        map.put(Byte.class, OpcNodeType.VAR_LONG);
        map.put(UByte.class, OpcNodeType.VAR_LONG);
        map.put(Short.class, OpcNodeType.VAR_LONG);
        map.put(UShort.class, OpcNodeType.VAR_LONG);
        map.put(Integer.class, OpcNodeType.VAR_LONG);
        map.put(UInteger.class, OpcNodeType.VAR_LONG);
        map.put(Long.class, OpcNodeType.VAR_LONG);
        map.put(ULong.class, OpcNodeType.VAR_LONG);
        map.put(Float.class, OpcNodeType.VAR_DOUBLE);
        map.put(Double.class, OpcNodeType.VAR_DOUBLE);
        map.put(String.class, OpcNodeType.VAR_STRING);
        // Unsupported types
        map.put(DateTime.class, OpcNodeType.VAR_OTHER);
        map.put(Object.class, OpcNodeType.VAR_OTHER);
        map.put(NodeId.class, OpcNodeType.VAR_OTHER);
        map.put(ByteString.class, OpcNodeType.VAR_OTHER);
        map.put(ExtensionObject.class, OpcNodeType.VAR_OTHER);

        return Collections.unmodifiableMap(map);
    }

    public static OpcNodeType mapVariableType(Class<?> type) {
        if (!VARIABLE_TYPE_MAP.containsKey(type)) {
            log.debug("Unknown variable type: {}", type);
        }
        return VARIABLE_TYPE_MAP.getOrDefault(type, OpcNodeType.VAR_OTHER);
    }

    /**
     * Maps a Milo value (e.g., UInteger) to a plain Java value (e.g., Long).
     */
    public static Object mapVariableValueToJavaType(Object value) {
        if (value == null) {
            return null;
        }
        // most specific class matching the value
        Class<?> bestType = Object.class;
        OpcNodeType bestTypeNode = OpcNodeType.VAR_OTHER;
        for (var entry : VARIABLE_TYPE_MAP.entrySet()) {
            if (entry.getKey().isInstance(value) && !entry.getKey().isAssignableFrom(bestType)) {
                bestType = entry.getKey();
                bestTypeNode = entry.getValue();
            }
        }
        if (bestTypeNode == OpcNodeType.VAR_OTHER) {
            return value;
        } else {
            return bestTypeNode.convertFromString(value.toString());
        }
    }

    public static OpcStatusCode mapStatusCode(StatusCode code) {
        var nameAndDesc = StatusCodes.lookup(code.getValue()).orElse(new String[] {null, null});
        return OpcStatusCode.of(nameAndDesc[0], nameAndDesc[1], code.getValue(), code.isGood());
    }

    public static OpcStatusCode mapStatusCode(long code) {
        return mapStatusCode(new StatusCode(code));
    }

    public static @Nullable OpcMeasuredValue mapMeasuredValue(
            @Nonnull OpcNode node, @Nonnull DataValue wrappedValue, @Nonnull Instant clientTime) {
        if (wrappedValue.getStatusCode() == null) {
            return null;
        }
        var statusCode = mapStatusCode(wrappedValue.getStatusCode());
        var value = mapVariableValueToJavaType(wrappedValue.getValue().getValue());
        if (value != null) {
            var sourceTime = Optional.ofNullable(wrappedValue.getSourceTime())
                    .map(DateTime::getJavaInstant)
                    .orElse(null);
            var serverTime = Optional.ofNullable(wrappedValue.getServerTime())
                    .map(DateTime::getJavaInstant)
                    .orElse(null);
            return new OpcMeasuredValue(node, statusCode, value, sourceTime, serverTime, clientTime);
        } else {
            return null;
        }
    }
}
