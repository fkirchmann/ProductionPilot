/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.repository;

import com.productionpilot.db.timescale.entities.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findAllByOrderByModificationTimeDesc();
    List<Batch> findAllByParentBatchIsNullOrderByModificationTimeDesc();
    List<Batch> findAllByParentBatchOrderByModificationTimeAsc(Batch parentBatch);

    @Query(value = "WITH RECURSIVE Batch_Parents_Recursive AS (" +
            "        SELECT id cts_child_id, id cts_parent_id, 1 cts_level, parent_batch_id cts_real_parent_id, Batch.*" +
            "        FROM Batch" +
            "    UNION ALL" +
            "        SELECT Batch_Parents_Recursive.cts_child_id, Batch.id parent_id, cts_level + 1, Batch.parent_batch_id, Batch.*" +
            "        FROM Batch_Parents_Recursive" +
            "                 INNER JOIN Batch ON Batch.id = Batch_Parents_Recursive.cts_real_parent_id" +
            " ) " +
            " SELECT * FROM Batch_Parents_Recursive WHERE cts_child_id = ?1 ORDER BY cts_level", nativeQuery = true)
    List<Batch> getFullPath(Batch batch);
}
