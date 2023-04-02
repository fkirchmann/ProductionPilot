/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc.milo;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class SubscribedItemHandleMap<T extends SubscribedItemHandleMap.HasHandle> {
    private final SortedMap<Long, T> subscribedItemsByHandle = new TreeMap<>();

    private List<Long> getFreeHandles(final int handlesRequired) {
        var freeHandles = new ArrayList<Long>(handlesRequired);
        var lastHandle = 0L;
        for(long handle : subscribedItemsByHandle.keySet()) {
            if(handle <= lastHandle) { throw new IllegalStateException("Handle map is not sorted: "
                    + handle + " <= " + lastHandle); }
            if(freeHandles.size() >= handlesRequired) {
                break;
            }
            long availableHandles = handle - lastHandle - 1;
            if(availableHandles > 0) {
                var handlesToGet = Math.min(handlesRequired - freeHandles.size(), availableHandles);
                for(long i = lastHandle + 1; i <= lastHandle + handlesToGet; i++) {
                    freeHandles.add(i);
                }
            }
            lastHandle = handle;
        }
        if(freeHandles.size() < handlesRequired) {
            var handlesToGet = handlesRequired - freeHandles.size();
            for(long i = lastHandle + 1; i <= lastHandle + handlesToGet; i++) {
                freeHandles.add(i);
            }
        }
        if(freeHandles.size() != handlesRequired) {
            throw new IllegalStateException("Found invalid number of free handles: " + freeHandles.size()
                    + " (expected " + handlesRequired + ")");
        }
        return freeHandles;
    }

    @Synchronized
    public void addAllAndAssignHandles(List<T> items) {
        items.stream().filter(item -> item.getHandle() != null).findAny().ifPresent(item -> {
            throw new IllegalStateException("Item already has a handle: " + item);
        });
        var freeHandles = getFreeHandles(items.size());
        for(int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            var handle = freeHandles.get(i);
            item.setHandle(handle);
            subscribedItemsByHandle.put(handle, item);
        }
    }

    @Synchronized
    public void addAndAssignHandle(T item) {
        addAllAndAssignHandles(List.of(item));
    }

    @Synchronized
    public void removeAll(List<T> items) {
        for(var item : items) {
            subscribedItemsByHandle.remove(item.getHandle());
            item.setHandle(null);
        }
    }

    @Synchronized
    public void remove(T item) {
        removeAll(List.of(item));
    }

    @Synchronized
    public T getByHandle(long handle) {
        return subscribedItemsByHandle.get(handle);
    }

    @Synchronized
    public List<T> values() {
        return new ArrayList<>(subscribedItemsByHandle.values());
    }

    public static class HasHandle {
        @Getter(AccessLevel.PROTECTED)
        @Setter(AccessLevel.PROTECTED)
        private Long handle;
    }
}
