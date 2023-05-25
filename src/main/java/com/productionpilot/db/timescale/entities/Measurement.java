/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.timescale.entities;

import java.time.Instant;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Entity
@Slf4j
public class Measurement extends AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "parameter_id")
    private long parameterId;

    @NotNull
    private Instant sourceTime;

    @NotNull
    private Instant serverTime;

    @NotNull
    private Instant clientTime;

    @NotNull
    private Long opcStatusCode;

    // @Type(type = "org.hibernate.type.TextType")
    private String valueString;

    private Boolean valueBoolean;

    private Long valueLong;

    private Double valueDouble;

    @Transient
    public Object getValue() {
        if (valueString != null) {
            return valueString;
        } else if (valueBoolean != null) {
            return valueBoolean;
        } else if (valueLong != null) {
            return valueLong;
        } else if (valueDouble != null) {
            return valueDouble;
        } else {
            log.warn("Measurement with ID {} has no value set.", id);
            return null;
        }
    }

    public String getValueAsString() {
        return getValue().toString();
    }
}
