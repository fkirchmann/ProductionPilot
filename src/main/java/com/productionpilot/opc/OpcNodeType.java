/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum OpcNodeType {
    // TODO: refactor to use a generics (and a class hierarchy?) instead of this enum
    VAR_DOUBLE(Double.class, "Double", Double::parseDouble),
    VAR_LONG(Long.class, "Long", Long::parseLong),
    VAR_BOOLEAN(Boolean.class, "Boolean", Boolean::parseBoolean),
    VAR_STRING(String.class, "String", null),
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

    public boolean isVariable() {
        return !isObject() && isFound();
    }

    /**
     * @return true if this node type is not {@link #NOT_FOUND} or {@link #NOT_DETERMINED}
     */
    public boolean isFound() { return this != NOT_FOUND; }

    public boolean isNotFound() { return this == NOT_FOUND; }

    /**
     * @return true if this node type is {@link #NOT_DETERMINED}
     */
    public boolean isUndetermined() { return this == NOT_DETERMINED; }

    public boolean isObject() {
        return this == OBJECT && isFound();
    }

    public String toString() {
        return name;
    }

    public Object convertFromString(String string) {
        if(valueConverter == null) {
            return string;
        }
        return valueConverter.apply(string);
    }
}
