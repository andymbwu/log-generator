package com.ad1.loggenerator.service.implementation;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ad1.loggenerator.exception.FilePathNotFoundException;
import com.ad1.loggenerator.model.BatchSettings;
import com.ad1.loggenerator.model.BatchTracker;
import com.ad1.loggenerator.model.JobStatus;
import com.ad1.loggenerator.model.SelectionModel;

import lombok.Data;

/**
 * Contains logic for processing user requests and generating outputs specific
 * to the user defined parameters
 */
@Data
@Service
public class BatchService {

    private LogService logService;

    public BatchService(@Autowired LogService logService) {
        this.logService = logService;
    }

    /**
     * Generates and populates the batch file
     * 
     * @param selectionModel defines all the parameters to be included in the batch
     *                       files as per the user
     * @return
     */
    @Async("asyncTaskExecutor")
    public void batchMode(SelectionModel selectionModel, BatchTracker batchJobTracker) {
        try {
            // batch settings
            BatchSettings batchSettings = selectionModel.getBatchSettings();

            // specify filepath location for batch file
            String filename = "C:\\log-generator\\batch\\" + selectionModel.getJobId() + ".json";
            FileWriter fileWriter = new FileWriter(filename);

            // remove fields that should not be included in custom logs
            logService.preProcessCustomLogs(selectionModel.getCustomLogs(), selectionModel);
            Set<String> masterFieldList = logService.getMasterFieldsList(selectionModel.getCustomLogs());

            // write a [ to begin the log file
            fileWriter.write("[");

            // add log lines to batch file
            for (int i = 0; i < batchSettings.getNumberOfLogs() 
                && batchJobTracker.getStatus() == JobStatus.ACTIVE; i++) { // repeat for specified batch size

                    // add a delimiter if it's not the first log line written
                    if (i > 0) {
                        fileWriter.write(",\n");
                    }

                    JSONObject logLine = logService.generateLogLine(selectionModel, masterFieldList);
                    fileWriter.write(logLine.toString());
                    batchJobTracker.setLogCount(batchJobTracker.getLogCount() + 1);

                    // determine if a log lines repeats
                    // check i + 1 because at this point i is one behind actual logs written
                    if (Math.random() < selectionModel.getRepeatingLoglinesPercent()
                        && i + 1 < batchSettings.getNumberOfLogs() && batchJobTracker.getStatus() == JobStatus.ACTIVE) {
                        
                        // add a delimiter
                        fileWriter.write(",\n");
                        // write the log line
                        fileWriter.write(logLine.toString());
                        i++;
                        batchJobTracker.setLogCount(batchJobTracker.getLogCount() + 1);
                    }

            }

            // write a ] to end the log file
            fileWriter.write("]");

            fileWriter.close();
        } catch (IOException e) {
            // Mark the job as failed if an exception occurred
            batchJobTracker.setStatus(JobStatus.FAILED);
            throw new FilePathNotFoundException(e.getMessage());
        }

        // Mark the job as completed if no exception occurred
        if (batchJobTracker.getStatus() == JobStatus.ACTIVE) {
            batchJobTracker.setStatus(JobStatus.COMPLETED);
        }
    }

    public String generateJobId() {
        return UUID.randomUUID().toString();
    }

}