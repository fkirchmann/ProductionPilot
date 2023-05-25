/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.timescale.service.event;

import com.productionpilot.db.timescale.entities.AbstractEntity;

public class EntityUpdatedEvent<T extends AbstractEntity> extends EntityModifiedEvent<T> {
    public EntityUpdatedEvent(T entity) {
        super(entity);
    }
}
