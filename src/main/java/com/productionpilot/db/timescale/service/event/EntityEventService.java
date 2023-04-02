/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.service.event;

import com.productionpilot.db.timescale.entities.AbstractEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.persistence.*;

@Component
@Slf4j
public class EntityEventService {
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @PostPersist
    public <T extends AbstractEntity> void onEntityCreate(T entity) {
        applicationEventPublisher.publishEvent(new EntityCreatedEvent<>(entity));
    }

    @PostUpdate
    public <T extends AbstractEntity> void onEntityUpdate(T entity) {
        applicationEventPublisher.publishEvent(new EntityUpdatedEvent<>(entity));
    }

    @PostRemove
    public <T extends AbstractEntity> void onEntityDelete(T entity) {
        applicationEventPublisher.publishEvent(new EntityDeletedEvent<>(entity));
    }
}
