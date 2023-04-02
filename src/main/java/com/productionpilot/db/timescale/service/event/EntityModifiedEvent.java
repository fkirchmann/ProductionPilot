/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.service.event;

import com.productionpilot.db.timescale.entities.AbstractEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

@RequiredArgsConstructor
@Getter
public abstract class EntityModifiedEvent<T extends AbstractEntity> implements ResolvableTypeProvider {
    private final T entity;

    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(entity));
    }
}
