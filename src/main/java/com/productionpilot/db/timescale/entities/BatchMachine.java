/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.timescale.entities;

import java.time.Instant;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
// @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class BatchMachine extends AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Batch is required")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Batch batch;

    @NotNull(message = "Machine is required")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Machine machine;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    @NotNull
    @CreationTimestamp
    private Instant creationTime = Instant.now();

    @NotNull
    @UpdateTimestamp
    private Instant modificationTime = Instant.now();
}
