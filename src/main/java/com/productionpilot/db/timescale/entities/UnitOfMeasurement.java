/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
@Entity
// Provide soft delete
@Table(name = "Unit_Of_Measurement")
//@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class UnitOfMeasurement extends AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 255)
    @NotBlank(message = "Name is required")
    private String name;

    @Size(max = 16, message = "Must be 16 characters or less")
    @NotBlank(message = "Abbreviation is required")
    private String abbreviation;

    public String toString() {
        return name + " (" + abbreviation + ")";
    }
}