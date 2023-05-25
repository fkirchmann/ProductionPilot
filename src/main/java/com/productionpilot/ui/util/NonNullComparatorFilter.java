/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.util;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 *
 * @param <T>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NonNullComparatorFilter<T> implements java.util.Comparator<T> {
    private final java.util.Comparator<T> comparator;

    public static <T> NonNullComparatorFilter<T> of(java.util.Comparator<T> comparator) {
        return new NonNullComparatorFilter<>(comparator);
    }

    @Override
    public int compare(T o1, T o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null && o2 != null) {
            return -1;
        } else if (o1 != null && o2 == null) {
            return 1;
        } else {
            return comparator.compare(o1, o2);
        }
    }
}
