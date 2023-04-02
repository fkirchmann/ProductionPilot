/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.db.timescale.service;

import com.productionpilot.db.timescale.repository.BatchRepository;
import com.productionpilot.db.timescale.entities.Batch;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ValidationException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchService {
    private final BatchRepository batchRepository;

    public List<Batch> findAll() {
        return batchRepository.findAllByOrderByModificationTimeDesc();
    }

    public List<Batch> findAllTopLayer() {
        return batchRepository.findAllByParentBatchIsNullOrderByModificationTimeDesc();
    }

    @Synchronized
    public void update(Batch batch) {
        var parent = batch.getParentBatch();
        if(parent != null) {
            if(getFullPath(parent).contains(batch)) {
                throw new ValidationException("The specified parent batch is already a child of this batch.");
            }
        }
        batchRepository.save(batch);
    }

    public void delete(Batch batch) {
        batchRepository.delete(batch);
    }

    @Transactional
    public Batch create(String name) {
        Batch batch = new Batch();
        batch.setName(name);
        batchRepository.save(batch);
        return batch;
    }

    /**
     * Returns all direct children of the given batch.
     */
    public List<Batch> findAllByParentBatch(Batch parentBatch) {
        return batchRepository.findAllByParentBatchOrderByModificationTimeAsc(parentBatch);
    }

    /**
     * Returns the full path of the batch, starting with the top layer batch and ending with the batch itself. If the
     * batch has no parent, the list will contain only the batch itself.
     */
    public List<Batch> getFullPath(Batch batch) {
        return batchRepository.getFullPath(batch);
    }

    public Batch findById(long batchId) {
        return batchRepository.findById(batchId).orElse(null);
    }
}
