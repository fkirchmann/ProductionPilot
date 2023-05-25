/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
/**
 * This enum represents the different types of nodes that can be present in an OPC UA server.
 */
public enum OpcNodeType {
    /**
     * This is a type for nodes that map to a double value, e.g., floats, doubles.
     */
    VAR_DOUBLE(Double.class, "Double", Double::parseDouble),
    /**
     * This is a type for OPC UA nodes whose values map to a long value, e.g., ushorts, shorts, uints, ints, ulongs, longs.
     */
    VAR_LONG(Long.class, "Long", Long::parseLong),
    /**
     * This is a type for OPC UA nodes whose values map to a boolean value.
     */
    VAR_BOOLEAN(Boolean.class, "Boolean", Boolean::parseBoolean),
    /**
     * This is a type for String-type OPC UA nodes.
     */
    VAR_STRING(String.class, "String", null),
    /**
     * This is a catch-all type for nodes that are not of any of the other types, e.g., byte arrays, dates, etc. They will be
     * represented as strings.
     */
    VAR_OTHER(String.class, "Other", null),
    /**
     * This is a special type that is used for nodes that are not variables, e.g. folders.
     */
    OBJECT(null, "Object", null),
    /**
     * This is a special type that is used when the node type is not found, e.g. because the node does not exist on
     * the server.
     */
    NOT_FOUND(String.class, "Not Found", null),
    /**
     * This is a special type that is used when the node type is not (yet) determined e.g. because the connection to the
     * OPC UA server is not yet established.
     */
    NOT_DETERMINED(String.class, "Not Known", null);

    @Getter
    private final Class<?> javaType;

    @Getter
    private final String name;

    private final Function<String, Object> valueConverter;

    /**
     * @return true if this node type is a variable, false otherwise.
     */
    public boolean isVariable() {
        return !isObject() && isFound();
    }

    /**
     * @return true if this node type is found, false otherwise.
     */
    public boolean isFound() {
        return this != NOT_FOUND;
    }

    /**
     * @return true if this node type is not found, false otherwise.
     */
    public boolean isNotFound() {
        return this == NOT_FOUND;
    }

    /**
     * @return true if this node type is undetermined, false otherwise.
     */
    public boolean isUndetermined() {
        return this == NOT_DETERMINED;
    }

    /**
     * @return true if this node type is an object, false otherwise.
     */
    public boolean isObject() {
        return this == OBJECT && isFound();
    }

    /**
     * @return the name of this node type.
     */
    public String toString() {
        return name;
    }

    /**
     * Converts a string to an object of the appropriate type for this node type.
     *
     * @param string the string to convert.
     * @return an object of the appropriate type for this node type.
     */
    public Object convertFromString(String string) {
        if (valueConverter == null) {
            return string;
        }
        return valueConverter.apply(string);
    }
}
