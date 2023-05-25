/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import java.time.Instant;
import java.util.Arrays;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class OpcMeasuredValue {
    @NotNull
    private final OpcNode node;

    @NotNull
    private final OpcStatusCode statusCode;

    private final Object value;
    private final Instant sourceTime, serverTime;

    @NotNull
    private final Instant clientTime;

    public String getValueAsString() {
        var value = getValue();
        if (value == null) {
            return "null";
        }
        // Unwrap if array
        if (value instanceof Object[] valueArray) {
            return Arrays.toString(valueArray);
        } else if (value instanceof byte[] byteArray) {
            return Arrays.toString(byteArray);
        } else if (value instanceof short[] shortArray) {
            return Arrays.toString(shortArray);
        } else if (value instanceof int[] intArray) {
            return Arrays.toString(intArray);
        } else if (value instanceof long[] longArray) {
            return Arrays.toString(longArray);
        } else if (value instanceof float[] floatArray) {
            return Arrays.toString(floatArray);
        } else if (value instanceof double[] doubleArray) {
            return Arrays.toString(doubleArray);
        } else if (value instanceof boolean[] booleanArray) {
            return Arrays.toString(booleanArray);
        } else if (value instanceof char[] charArray) {
            return Arrays.toString(charArray);
        }
        // Otherwise just return the toString
        return value.toString();
    }
}
