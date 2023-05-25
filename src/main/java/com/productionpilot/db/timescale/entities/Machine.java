/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.db.timescale.entities;

import java.util.*;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.ReadOnlyProperty;

@Getter
@Setter
@Entity
// Provide soft delete
@SQLDelete(sql = "UPDATE Machine SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
// @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Machine extends AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 255)
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull
    private String description = "";

    @NotNull
    private Boolean deleted = false;

    @NotNull
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "machine", cascade = CascadeType.REMOVE)
    @ReadOnlyProperty
    private List<Parameter> parameters = new ArrayList<>();

    public String toString() {
        return name;
    }

    @Override
    public boolean deepEquals(AbstractEntity other) {
        return EqualsBuilder.reflectionEquals(this, other, "parameters");
    }
}
