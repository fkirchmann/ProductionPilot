/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */

package com.productionpilot.api;

import com.productionpilot.api.serializers.ApiFormatters;
import com.productionpilot.db.timescale.entities.Batch;
import com.productionpilot.db.timescale.entities.Parameter;
import com.productionpilot.db.timescale.service.BatchMachineService;
import com.productionpilot.db.timescale.service.BatchService;
import com.productionpilot.db.timescale.service.MeasurementService;
import com.productionpilot.util.Util;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class BatchCSVExporter {
    private static final String[] CSV_HEADER =
            { "ID", "Value", "OPC_Status_Code", "Source_Time", "Server_Time", "Client_Time"};
    private static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL;
    private static final String CSV_FORMAT_NAME = "Excel";
    private static final Charset CSV_CHARSET = StandardCharsets.UTF_8;
    private static final DateTimeFormatter SHORT_FORMATTER = DateTimeFormatter.ofPattern("yy-MM-dd HH.mm")
            .withLocale(Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private static final int ZIP_BUFSIZE = 10 * 1000 * 1000; // 10 MB

    private final BatchService batchService;
    private final MeasurementService measurementService;
    private final BatchMachineService batchMachineService;

    public void writeBatchToZip(OutputStream outputStream, Batch batch) throws IOException {
        @Cleanup
        var bufferedOutputStream = new BufferedOutputStream(outputStream, ZIP_BUFSIZE);
        @Cleanup
        var zipOutputStream = new ZipOutputStream(bufferedOutputStream);
        writeBatchToZipCSVImpl(zipOutputStream, "", batch);
        zipOutputStream.flush();
        bufferedOutputStream.flush();
    }

    private void writeBatchToZipCSVImpl(ZipOutputStream zipOutputStream, String prefix, Batch batch)
            throws IOException {
        for(var range : batchMachineService.findAllByBatch(batch)) {
            var machine = range.getMachine();
            var machineName = machine.getName();
            var machinePath = prefix + Util.filterFilename(machineName + " from "
                    + SHORT_FORMATTER.format(range.getStartTime())
                    + " to " + SHORT_FORMATTER.format(range.getEndTime())) + "/";
            zipOutputStream.putNextEntry(new ZipEntry(machinePath));
            for(var paramteter : range.getMachine().getParameters()) {
                var parameterCsvName = (paramteter.getIdentifier() != null ? paramteter.getIdentifier()
                        : "Parameter " + paramteter.getId()) + " - " + paramteter.getName() + ".csv";
                zipOutputStream.putNextEntry(new ZipEntry(machinePath + Util.filterFilename(parameterCsvName)));
                writeParameterMeasurementsCSV(zipOutputStream, paramteter, range.getStartTime(), range.getEndTime());
                zipOutputStream.flush();
            }
        }
        for(var childBatch : batchService.findAllByParentBatch(batch)) {
            var newPrefix = prefix + Util.filterFilename(childBatch.getName()) + "/";
            zipOutputStream.putNextEntry(new ZipEntry(newPrefix));
            writeBatchToZipCSVImpl(zipOutputStream, newPrefix, childBatch);
        }
    }

    public void writeParameterMeasurementsCSV(OutputStream out, Parameter parameter, Instant start, Instant end)
            throws IOException {
        var outputStreamWriter = new OutputStreamWriter(out, CSV_CHARSET);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        final CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSV_FORMAT.builder().setHeader(CSV_HEADER).build());

        csvPrinter.printComment("CSV Format: " + CSV_FORMAT_NAME);
        csvPrinter.printComment("CSV Charset: " + CSV_CHARSET.name());
        csvPrinter.printComment("Parameter Name: " + parameter.getName());
        csvPrinter.printComment("Parameter ID: " + parameter.getId());
        if(parameter.getIdentifier() != null) {
            csvPrinter.printComment("Parameter Identifier: " + parameter.getIdentifier());
        } else {
            csvPrinter.printComment("Parameter Identifier not configured.");
        }
        if(parameter.getUnitOfMeasurement() != null) {
            csvPrinter.printComment("Parameter Unit of Measurement Name: " + parameter.getUnitOfMeasurement().getName());
            csvPrinter.printComment("Parameter Unit of Measurement Abbreviation: " + parameter.getUnitOfMeasurement().getAbbreviation());
            csvPrinter.printComment("Parameter Unit of Measurement ID: " + parameter.getUnitOfMeasurement().getId());
        } else {
            csvPrinter.printComment("Parameter Unit of Measurement not configured.");
        }
        csvPrinter.printComment("Parameter Machine Name: " + parameter.getMachine().getName());
        csvPrinter.printComment("Parameter Machine ID: " + parameter.getMachine().getId());
        csvPrinter.printComment("Start: " + ApiFormatters.API_DATETIME_FORMATTER.format(start));
        csvPrinter.printComment("End: " + ApiFormatters.API_DATETIME_FORMATTER.format(end));
        csvPrinter.printComment("Exported on: " + ApiFormatters.API_DATETIME_FORMATTER.format(Instant.now()));
        var measurementIterator = measurementService
                .streamByParameterAndTimeRange(parameter, start, end).iterator();
        while (measurementIterator.hasNext()) {
            var measurement = measurementIterator.next();
            csvPrinter.printRecord(measurement.getId(), measurement.getValue(),
                    measurement.getOpcStatusCode(),
                    ApiFormatters.API_DATETIME_FORMATTER.format(measurement.getSourceTime()),
                    ApiFormatters.API_DATETIME_FORMATTER.format(measurement.getServerTime()),
                    ApiFormatters.API_DATETIME_FORMATTER.format(measurement.getClientTime()));
        }
        csvPrinter.flush();
        bufferedWriter.flush();
        outputStreamWriter.flush();
    }
}
