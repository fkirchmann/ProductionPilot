/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.util;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class DebugPerfTimer {
    public static DebugPerfTimer start(String description) {
        return new DebugPerfTimer(description);
    }

    private final String description;
    private final long start = System.nanoTime();

    public void endAndPrint(Logger logger) {
        final long end = System.nanoTime();
        log.debug("{} took {} ms", description, (end - start) / 1000000);
    }

    public static void timeAndPrint(String description, Runnable taskToTime, Logger logger) {
        var timer = new DebugPerfTimer(description);
        taskToTime.run();
        timer.endAndPrint(logger);
    }
}
