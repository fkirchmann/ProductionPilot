/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.opc;

import java.time.Duration;

public interface OpcSubscribedItem {
    /**
     * Gets the node of the subscribed item.
     *
     * @return The node of the subscribed item.
     */
    OpcNode getNode();

    Duration getSamplingInterval();

    OpcSubscriptionListener getListener();

    long getUpdateCount();

    OpcStatusCode getStatusCode();

    OpcMeasuredValue getLastValue();
}
