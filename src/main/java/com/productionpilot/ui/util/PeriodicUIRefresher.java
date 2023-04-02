/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.util;

import com.vaadin.flow.component.Component;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
public class PeriodicUIRefresher {
    public static PeriodicUIRefresher create(Component element, long updateInterval, Runnable refreshAction) {
        return new PeriodicUIRefresher(element, updateInterval, (long) (updateInterval * 0.5), refreshAction).init();
    }

    private UIRefresherThread thread;
    private final Component component;
    /**
     * The interval in milliseconds between updates.
     */
    private final long updateInterval;
    /**
     * The minimum interval in milliseconds between updates. This is used to prevent
     * the UI from being updated too often.
     */
    private final long updateIntervalMin;
    private final Runnable refreshAction;

    private PeriodicUIRefresher init() {
        component.addAttachListener(event -> start());
        if(component.isAttached()) {
            start();
        }
        component.addDetachListener(event -> stop());
        return this;
    }

    public void refreshNow() {
        refreshAction.run();
    }

    @Synchronized
    private void start() {
        if(thread != null && thread.run) {
            return;
        }
        thread = new UIRefresherThread();
        thread.start();
    }

    @Synchronized
    public void stop() {
        thread.run = false;
    }

    private class UIRefresherThread extends Thread {
        private UIRefresherThread() {
            super("UIRefresher");
            setDaemon(true);
        }

        private boolean run = true;

        @Override
        public void run() {
            try {
                while (run) {
                    var ui = component.getUI().orElse(null);
                    if(ui == null) { break; }
                    ui.access(() -> {
                        try {
                            refreshAction.run();
                        } catch (Exception e) {
                            log.error("Error while refreshing UI", e);
                        }
                    }).get();
                    if(!run) { break; }
                    Thread.sleep(updateInterval);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error while refreshing UI", e);
            }
        }
    }
}
