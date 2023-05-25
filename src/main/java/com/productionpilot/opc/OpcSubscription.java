/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

import java.util.List;

/**
 * This interface represents an OPC subscription.
 */
public interface OpcSubscription {
    /**
     * Unsubscribes from the subscription.
     */
    void unsubscribe();

    /**
     * @return the list of items that are subscribed to.
     */
    List<OpcSubscribedItem> getSubscribedItems();
}
