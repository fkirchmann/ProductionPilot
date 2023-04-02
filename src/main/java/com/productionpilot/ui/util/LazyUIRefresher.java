/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.ui.util;

import com.productionpilot.db.timescale.entities.AbstractEntity;
import com.vaadin.flow.component.HasValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;

@Slf4j
public class LazyUIRefresher implements Serializable {
    private final Map<Object, Object> cache = new HashMap<>();

    /**
     * Refreshes a view (compare to {@link #refreshIfNecessary(Object, Object, BiConsumer)}). Additionally, it will
     * get the value before the refresh and afterwards, find the same value in the new list of items (with equals())
     * and use that value to set the value of the component.
     *
     * This is useful for combo boxes: If their contents are refreshed, the previously selected value will be
     * re-selected, and because we use the equals() method, the value will be the same (due to the same ID), but
     * the value will use new data (e.g., a new name).
     */
    public <V extends HasValue<?, IT>, I extends Collection<IT>, IT> boolean refreshIfNecessaryKeepValue
            (V view, I items, BiConsumer<V, I> applicationFunction) {
        var before = view.getValue();
        if(refreshIfNecessary(view, items, applicationFunction)) {
            if(before != null) {
                for(var item : items) {
                    if(Objects.equals(before, item)) {
                        view.setValue(item);
                    }
                }
            } else {
                view.setValue(null);
            }
            return true;
        }
        return false;
    }

    /**
     * Update the given view if the items have changed. This is compared by using a deep comparator that also checks the
     * field values of each item. A deep comparison is necessary because two item instances may have the same ID
     * (resulting in equals() returning true) but one of them may have updated field values.
     *
     * @param view the view to update
     * @param items the new value
     * @param applicationFunction the function to apply to the field, if the value has changed
     */
    public <V, I, IT> boolean refreshIfNecessary(V view, I items, BiConsumer<V, I> applicationFunction) {
        if(cache.containsKey(view)) {
            Object cachedItems = cache.get(view);
            if(deepCompare(cachedItems, items)) {
                return false;
            }
        }
        cache.put(view, items);
        applicationFunction.accept(view, items);
        return true;
    }

    private static boolean deepCompare(Object a, Object b) {
        if(a instanceof Pair<?,?> aPair && b instanceof Pair<?,?> bPair) {
            return deepCompare(aPair.getLeft(), bPair.getLeft())
                    && deepCompare(aPair.getRight(), bPair.getRight());
        } else if(a instanceof Iterable<?> aIterable && b instanceof Iterable<?> bIterable) {
            var aIterator = aIterable.iterator();
            var bIterator = bIterable.iterator();
            while(aIterator.hasNext() && bIterator.hasNext()) {
                if(!deepCompare(aIterator.next(), bIterator.next())) {
                    return false;
                }
            }
            // If one of the iterators has more elements, the lists are not equal
            return aIterator.hasNext() == bIterator.hasNext();
        } else if(a instanceof Map<?, ?> aMap && b instanceof Map<?, ?> bMap) {
            if(aMap.size() != bMap.size()) {
                return false;
            }
            for(var entry : aMap.entrySet()) {
                if(!deepCompare(entry.getValue(), bMap.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        } else if(a instanceof AbstractEntity aAbstractEntity && b instanceof AbstractEntity bAbstractEntity) {
            return aAbstractEntity.deepEquals(bAbstractEntity);
        } else if(Objects.equals(a, b)) {
            return EqualsBuilder.reflectionEquals(a, b, false);
        } else {
            return false;
        }
    }

    public <V, I1, I2> boolean refreshIfNecessary(V view, I1 items1, I2 items2,
                                               TriConsumer<V, I1, I2> applicationFunction) {
        var items = Pair.of(items1, items2);
        return refreshIfNecessary(view, items, (v, i) -> applicationFunction.accept(v, i.getLeft(), i.getRight()));
    }

    /**
     * Update the given field if the value has changed.
     *
     * @param view the field to update
     * @param items the new value
     * @param applicationFunction the function to apply to the field, if the value has changed
     * @param otherDependentItems other items that should trigger an update if any of them have changed since the last
     *                            call.
     */
    /*public <V, I> void refreshIfNecessary(V view, I items, BiConsumer<V, I> applicationFunction,
                                          Object ... otherDependentItems) {
        var allItems = Streams.concat(Stream.ofNullable(items), Arrays.stream(otherDependentItems)).toList();
        refreshIfNecessary(view, allItems, (v, i) -> applicationFunction.accept(v, items));
    }*/
}
