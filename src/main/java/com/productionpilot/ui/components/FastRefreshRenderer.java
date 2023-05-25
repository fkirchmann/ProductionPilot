/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.components;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.data.renderer.LitRenderer;
import elemental.json.Json;
import elemental.json.JsonObject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

// e.g. for 100k Tags and each Tag requiring (just!) 30 bytes of data per refresh, this would
// require 3MB of data per refresh!!! At a refresh rate of 1/s, this would be 3MB/s
// How could we restrict the updates to only that which is currently displayed?
// Nevermind, this way of thinking is dumb! For p much any machine, only a few dozen Tags are
// refreshed per second, so this is not a problem at all.
/* PLAN:
  1. Track server- & client-side state in a map: <Long, String> (id -> value)
  2. On refresh, diff the server- & client-side state and only update the client-side state
     for those values that have changed
*/
@Slf4j
public class FastRefreshRenderer<T> {
    private long columnIdCounter = 0;
    private final Map<Long, FastRefreshColumn> columns = new HashMap<>();
    private final Map<Long, T> knownObjects = new ConcurrentHashMap<>();
    private final HasElement parent;

    // this is null as long as there are no updates queued
    private JsonObject queuedUpdates = null;
    private final Object queuedUpdatesLock = new Object();
    private final ToLongFunction<T> idExtractor;

    public FastRefreshRenderer(ToLongFunction<T> idExtractor, HasElement parent) {
        this.idExtractor = idExtractor;
        this.parent = parent;
        initialize();
    }

    private void initialize() {
        parent.getElement()
                .executeJs(
                        """
                    if(window.frtms === undefined) {
                      window.frtms = {};
                      window.frtm_refresh = function() {
                        const elems = document.getElementsByClassName("fr-txt");
                        for(let i = 0; i < elems.length; i++) {
                          const elem = elems[i];
                          const id = elem.getAttribute("data-fr-id");
                          const val = window.frtms[id];
                          if(val !== undefined) {
                            elem.childNodes[1].textContent = val;
                          }
                         }
                       };
                     };
            """);
    }

    @Synchronized
    public LitRenderer<T> createColumn(Function<T, Object> renderingFunction) {
        var renderingFunctionToString = (Function<T, String>) o -> Objects.toString(renderingFunction.apply(o));
        var columnId = columnIdCounter++;
        var column = new FastRefreshColumn(columnId, renderingFunctionToString);
        columns.put(columnId, column);
        return LitRenderer.<T>of("<span class=\"fr-txt\" data-fr-id=\"${item.id}\">${item.value}</span>")
                .withProperty("id", column::getId)
                .withProperty("value", object -> {
                    var id = idExtractor.applyAsLong(object);
                    var newValue = renderingFunctionToString.apply(object);
                    column.serverState.put(id, newValue);
                    knownObjects.put(id, object);
                    return newValue;
                });
    }

    public void queueRefresh(T object) {
        log.debug("Queueing refresh for {}", object);
        knownObjects.put(idExtractor.applyAsLong(object), object);
        // Pre-acquire lock to improve performance
        synchronized (queuedUpdatesLock) {
            for (var column : columns.values()) {
                column.queueRefresh(object);
            }
        }
    }

    public void queueRefresh(Collection<T> objects) {
        log.debug("Queueing refresh for {} objects", objects.size());
        for (var object : objects) {
            var id = idExtractor.applyAsLong(object);
            knownObjects.put(id, object);
        }
        // Pre-acquire lock to improve performance
        synchronized (queuedUpdatesLock) {
            for (var column : columns.values()) {
                for (var object : objects) {
                    column.queueRefresh(object);
                }
            }
        }
    }

    public void queueRefreshAll() {
        log.debug("Queueing refresh for all objects");
        // Pre-acquire lock to improve performance
        synchronized (queuedUpdatesLock) {
            for (var column : columns.values()) {
                for (var object : knownObjects.values()) {
                    column.queueRefresh(object);
                }
            }
        }
    }

    public boolean pushRefresh() {
        JsonObject toPush;
        synchronized (queuedUpdatesLock) {
            toPush = queuedUpdates;
            queuedUpdates = null;
        }
        if (toPush == null) {
            return false;
        }
        log.debug("Pushing refresh for {} objects", toPush.keys().length);
        // push JS to update the fields for the given objects
        parent.getElement()
                .executeJs(
                        " Object.assign(window.frtms, $0);" + " console.log(\"refresh!!\");"
                                + " window.frtm_refresh();",
                        toPush);
        return true;
    }

    @RequiredArgsConstructor
    private class FastRefreshColumn {
        private final Long columnId;
        private final Function<T, String> renderingFunction;
        private final Map<Long, String> serverState = new HashMap<>();

        private void queueRefresh(T object) {
            var id = idExtractor.applyAsLong(object);
            var newValue = renderingFunction.apply(object);
            var previousValue = serverState.get(id);
            if (previousValue == null || !previousValue.equals(newValue)) {
                serverState.put(id, newValue);
                var queueId = getId(object);
                synchronized (queuedUpdatesLock) {
                    if (queuedUpdates == null) {
                        queuedUpdates = Json.createObject();
                    }
                    queuedUpdates.remove(queueId);
                    queuedUpdates.put(queueId, newValue);
                    log.debug("Queued refresh for {} (id: {}), new value: {}", object, queueId, newValue);
                }
            }
        }

        private String getId(T object) {
            return columnId + "." + idExtractor.applyAsLong(object);
        }
    }
}
