/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.repository;

import com.productionpilot.db.timescale.entities.Measurement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface MeasurementRepository extends CrudRepository<Measurement, Long> {

    long countByParameterId(Long parameterId);

    @Query("SELECT m.parameterId, COUNT(m) FROM Measurement m GROUP BY m.parameterId")
    Map<Long, Long> countGroupByParameterId();

    Stream<Measurement> streamAllByOrderByIdAsc();

    Stream<Measurement> streamByParameterIdAndClientTimeBetweenOrderByIdAsc(Long parameterId, Instant startTime,
                                                                            Instant endTime);

    Stream<Measurement> streamByParameterIdAndIdGreaterThanOrderByIdAsc(Long parameterId, Long id);

    Stream<Measurement> streamByParameterIdOrderByIdAsc(Long parameterId);

    Optional<Measurement> findFirstByParameterIdOrderByIdDesc(Long parameterId);

    Stream<Measurement> streamByIdGreaterThanOrderByIdAsc(long id);

    Measurement findFirstByOrderByIdDesc();
}
