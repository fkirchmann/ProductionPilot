/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.opc;

/**
 * This interface defines the methods that must be implemented by a class that wants to listen to updates from an OPC subscription.
 */
public interface OpcSubscriptionListener {

    /**
     * This method is called when the subscription becomes active, i.e. when it has been successfully created on the OPC server.
     *
     * @param subscription The subscription that became active.
     */
    default void onSubscriptionActive(OpcSubscription subscription) {}

    /**
     * This method is called when a variable in the subscription is updated.
     *
     * @param value The new value of the variable.
     */
    void onVariableUpdate(OpcMeasuredValue value);
}
