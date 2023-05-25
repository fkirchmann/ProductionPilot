/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.timescale.entities;

import java.time.Duration;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.data.annotation.ReadOnlyProperty;

@Getter
@Setter
@Entity
// Provide soft delete
@SQLDelete(sql = "UPDATE Parameter SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
// @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Parameter extends AbstractEntity {
    public static final long MINIMUM_SAMPLING_INTERVAL_MS = 10;
    public static final Duration DEFAULT_SAMPLING_INTERVAL = Duration.ofMillis(1000);
    public static final String IDENTIFIER_ALLOWED_CHAR_PATTERN = "[a-zA-Z0-9_.-]";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "Opc_Node_Id")
    @NotNull
    @NotBlank
    private String OpcNodeId;

    @Size(max = 255)
    @NotNull(message = "Name is required")
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull
    private String description = "";

    @NotNull(message = "Please specify a Machine")
    @ManyToOne
    private Machine machine;

    @ManyToOne
    private UnitOfMeasurement unitOfMeasurement;

    @Pattern(regexp = "^" + IDENTIFIER_ALLOWED_CHAR_PATTERN + "+$")
    private String identifier;

    @NotNull
    @ReadOnlyProperty
    private Boolean deleted = false;

    @NotNull
    @DurationMin(millis = MINIMUM_SAMPLING_INTERVAL_MS)
    private Duration samplingInterval = DEFAULT_SAMPLING_INTERVAL;

    public String toString() {
        if (identifier != null) {
            return "Parameter " + identifier + " (ID " + id + ")";
        } else {
            return "Parameter \"" + name + "\" (ID " + id + ")";
        }
    }
}
