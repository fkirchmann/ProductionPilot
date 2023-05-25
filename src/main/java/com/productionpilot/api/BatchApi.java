/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.api;

import com.productionpilot.db.timescale.entities.Batch;
import com.productionpilot.db.timescale.service.BatchService;
import com.productionpilot.ui.util.UIFormatters;
import com.productionpilot.util.Util;
import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchApi {

    private final BatchService batchService;
    private final BatchCSVExporter batchCSVExporter;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<Batch> getBatches() {
        return batchService.findAll();
    }

    @GetMapping(value = "id/{id}/export/csv-zip", produces = "application/zip")
    @SneakyThrows(IOException.class)
    @Transactional(readOnly = true)
    public void getBatchZip(@PathVariable long id, HttpServletResponse response) {
        Batch batch = batchService.findById(id);
        if (batch == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Machine not found");
        }
        var filename = batch.getName() + " - Exported on "
                + UIFormatters.DATE_TIME_FORMATTER
                        .format(LocalDateTime.now(ZoneId.systemDefault()))
                        .replace(":", ".") + ".zip";
        // setting headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"" + Util.filterFilename(filename) + "\"");

        batchCSVExporter.writeBatchToZip(response.getOutputStream(), batch);
    }

    public static String getLinkToBatchExport(Batch batch) {
        return "/api/v1/batches/id/" + batch.getId() + "/export/csv-zip";
    }
}
