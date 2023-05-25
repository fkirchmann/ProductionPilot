/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

public class OpcException extends RuntimeException {
    public OpcException(String message) {
        super(message);
    }

    public OpcException(String message, Throwable e) {
        super(message, e);
    }

    public OpcException(Throwable e) {
        super(e);
    }
}
