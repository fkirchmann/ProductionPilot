/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class OpcStatusCode {

    public static final OpcStatusCode GOOD = new OpcStatusCode("Good", null, 0x00000000, true);
    public static final OpcStatusCode BAD = new OpcStatusCode("Bad", null, 0xC0000000, false);
    public static final OpcStatusCode BAD_UNEXPECTED_ERROR =
            new OpcStatusCode("BadUnexpectedError", null, 0x80010000, false);
    public static final OpcStatusCode BAD_NO_DATA = new OpcStatusCode(
            "BadNoData", "No data exists for the requested time range or event filter.", 0x809B0000, false);

    public static OpcStatusCode of(String name, String description, long code, boolean isGood) {
        return new OpcStatusCode(name, description, code, isGood);
    }

    private final String name, description;
    private final long code;
    private final boolean good;

    public String toString() {
        if (code == 0 && name != null) {
            return name;
        }
        if (name != null) {
            if (description != null) {
                return name + " (" + description + " / 0x" + Long.toHexString(code) + ")";
            } else {
                return name + " (0x" + Long.toHexString(code) + ")";
            }
        } else {
            if (description != null) {
                return description + " (0x" + Long.toHexString(code) + ")";
            } else {
                return "0x" + Long.toHexString(code);
            }
        }
    }
}
