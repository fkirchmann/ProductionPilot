/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.entities;

import com.productionpilot.db.timescale.service.event.EntityEventService;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.Hibernate;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.util.Objects;

@MappedSuperclass
@EntityListeners(EntityEventService.class)
public abstract class AbstractEntity {
    public abstract Long getId();

    public String toString() {
        return Hibernate.getClass(this).getSimpleName() + ": id=" + getId();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || !Hibernate.getClass(this).equals(Hibernate.getClass(o))) return false;
        AbstractEntity that = (AbstractEntity) o;
        if(Objects.isNull(this.getId()) || Objects.isNull(that.getId())) { return false; }
        return Objects.equals(this.getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    public boolean deepEquals(AbstractEntity other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
}
