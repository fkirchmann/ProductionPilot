/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.timescale.service.event;

import com.productionpilot.db.timescale.entities.AbstractEntity;
import org.springframework.core.ResolvableType;

public class EntityCreatedEvent<T extends AbstractEntity> extends EntityModifiedEvent<T> {
    public EntityCreatedEvent(T entity) {
        super(entity);
    }

    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(getEntity()));
    }
}
