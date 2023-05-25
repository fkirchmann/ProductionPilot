/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.timescale.entities;

import java.time.Instant;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
// @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Batch extends AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 255)
    @NotNull
    @NotBlank(message = "A name is required")
    private String name;

    @NotNull
    private String description = "";

    @NotNull
    @CreationTimestamp
    private Instant creationTime = Instant.now();

    @NotNull
    @UpdateTimestamp
    private Instant modificationTime = Instant.now();

    @ManyToOne(fetch = FetchType.EAGER)
    private Batch parentBatch;

    /*@OneToMany(mappedBy = "batch", fetch = FetchType.EAGER)
    @ReadOnlyProperty
    private List<BatchMachine> batchMachines;*/

    @Transient
    public boolean isTopLayer() {
        return parentBatch == null;
    }

    public String toString() {
        return name;
    }
}
