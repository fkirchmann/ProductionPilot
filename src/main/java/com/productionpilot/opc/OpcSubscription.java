/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import java.util.List;

public interface OpcSubscription {
    /**
     * Unsubscribes from the subscription.
     */
    void unsubscribe();

    List<OpcSubscribedItem> getSubscribedItems();
}
