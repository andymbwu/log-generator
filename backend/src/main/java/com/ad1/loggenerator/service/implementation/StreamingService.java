package com.ad1.loggenerator.service.implementation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.ad1.loggenerator.exception.FilePathNotFoundException;
import com.ad1.loggenerator.model.JobStatus;
import com.ad1.loggenerator.model.SelectionModel;
import com.ad1.loggenerator.model.StreamTracker;

import lombok.Data;
import reactor.core.publisher.Mono;

/**
 * Contains logic for processing user requests and generating outputs specific
 * to the user defined parameters
 */
@Data
@Service
public class StreamingService {

    private LogService logService;

    public StreamingService(@Autowired LogService logService) {
        this.logService = logService;
    }

    /**
     * Generates streaming log lines until it is told to stop
     *
     * @param selectionModel defines all the parameters to be included in the stream
     *                       log lines as per the user
     * @return
     */
    @Async("asyncTaskExecutor")
    public void streamMode(SelectionModel selectionModel, StreamTracker streamJobTracker) throws InterruptedException {

        // stream to address
        if (!selectionModel.getStreamSettings().getStreamAddress().isEmpty()) {
            streamToAddress(selectionModel, streamJobTracker);
        }
        // stream to file
        else {
            streamToFile(selectionModel, streamJobTracker);
        }
    }

    public String streamToAddress(SelectionModel selectionModel, StreamTracker streamJobTracker)
            throws InterruptedException {

        // how many logs will be sent in each request
        int batchSize = 10;

        // times
        long nsBetweenRequests = (long) (1000000000.0
                / ((double) selectionModel.getStreamSettings().getLogRate() / batchSize));
        long nextSendNanoTime = System.nanoTime() + nsBetweenRequests;
        long nsToNextRequest = 0;

        // array with given batch size
        JSONObject[] logs = new JSONObject[batchSize];

        // temporary repeating log
        JSONObject repeatingLog = null;

        // Setup web client for post request to user specified address
        String streamAddress = selectionModel.getStreamSettings().getStreamAddress();
        WebClient webClient = WebClient.create(streamAddress);
        String[] errorMessage = { "" };

        // remove fields that should not be included in custom logs
        logService.preProcessCustomLogs(selectionModel.getCustomLogs(), selectionModel);
        Set<String> masterFieldList = logService.getMasterFieldsList(selectionModel.getCustomLogs());

        // should logs be saved
        boolean saveLogs = selectionModel.getStreamSettings().isSaveLogs();

        // File and FileWriter for savings log lines
        File tempLogFile = null;
        FileWriter fileWriter = null;

        try {
            boolean firstLogSaved = false;

            if (saveLogs) {
                // FileWriter for savings log lines
                tempLogFile = new File("logs\\stream\\" + streamJobTracker.getJobId() + ".json");
                fileWriter = new FileWriter(tempLogFile, true);

                // delete file if it already exists
                if (tempLogFile.exists()) {
                    tempLogFile.delete();
                }

                // Check if the file exists, create a new one if it doesn't exist
                if (!tempLogFile.exists()) {
                    tempLogFile.createNewFile();
                }

                // write a [ to begin the log file
                fileWriter.write("[");
            }
            while (streamJobTracker.getStatus() == JobStatus.ACTIVE) {
                // generate batchSize number of logs
                for (int i = 0; i < batchSize; i++) {
                    // if no repeating log, generate a new log, otherwise add the repeating log
                    if (repeatingLog == null) {
                        logs[i] = logService.generateLogLine(selectionModel, masterFieldList);

                        // determine if a log lines repeats
                        if (Math.random() < selectionModel.getRepeatingLoglinesPercent()) {
                            repeatingLog = logs[i];
                        }
                    } else {
                        logs[i] = repeatingLog;
                        repeatingLog = null;
                    }
                    // writing logs to temp file
                    if (saveLogs) {
                        if (firstLogSaved) { // add delimiter if not first log line written
                            fileWriter.write(",\n");
                        }
                        // Write the log lines to the temp log file
                        fileWriter.write(logs[i].toString());

                        firstLogSaved = true;
                    }
                }
                // nanoseconds until next request time
                nsToNextRequest = nextSendNanoTime - System.nanoTime();

                // sleep for nsToNextRequest
                if (nsToNextRequest >= 0) {
                    // sleep time is subject to precision of system schedulers
                    Thread.sleep(nsToNextRequest / 1000000, (int) nsToNextRequest % 1000000);

                    // update next send time
                    nextSendNanoTime += nsBetweenRequests;
                } else if (nsToNextRequest >= -100000000) {
                    // if nsToNextRequest is behind but within 100ms, attempt to catch up next
                    // request
                    nextSendNanoTime += nsBetweenRequests;
                } else {
                    // if nsToNextRequest is behind by more than 100ms, reset nextSendNanoTime
                    nextSendNanoTime = System.nanoTime() + nsBetweenRequests;
                }

                // set up post request
                Mono<String> response = webClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(logs)
                        .retrieve()
                        .bodyToMono(String.class);

                // initiate post request and receive response from address
                response.subscribe(
                        res -> {
                            // System.out.println("Successful response: " + res);
                        },
                        error -> {
                            streamJobTracker.setStatus(JobStatus.FAILED);
                            errorMessage[0] = "Error while making the request: " + error;
                            System.out.println(error);
                        });

                // increment log count by batch size
                streamJobTracker.setLogCount(streamJobTracker.getLogCount() + batchSize);
            }

            if (saveLogs) {
                // write a ] to end the log file
                fileWriter.write("]");
                fileWriter.close();
            }

        } catch (IOException e) {
            // Mark the job as failed if exception occurred
            streamJobTracker.setStatus(JobStatus.FAILED);

            throw new FilePathNotFoundException(e.getMessage());
        }

        if (!errorMessage[0].isEmpty()) {
            return errorMessage[0];
        }

        // Mark the job as completed
        streamJobTracker.setStatus(JobStatus.COMPLETED);

        return "Successfully streamed to address.";
    }

    public void streamToFile(SelectionModel selectionModel, StreamTracker streamJobTracker) {

        // reset the start time of the stream
        streamJobTracker.setLastPing(System.currentTimeMillis() / 1000);

        // specify filepath location for stream file
        String filename = "logs\\stream\\" + selectionModel.getJobId() + ".json";

        // remove fields that should not be included in custom logs
        logService.preProcessCustomLogs(selectionModel.getCustomLogs(), selectionModel);
        Set<String> masterFieldList = logService.getMasterFieldsList(selectionModel.getCustomLogs());

        try {
            FileWriter fileWriter = new FileWriter(filename);

            // write a [ to begin the log file
            fileWriter.write("[");

            while (streamJobTracker.getStatus() == JobStatus.ACTIVE) {

                if (streamJobTracker.getLogCount() > 0) { // add delimiter if not first log line written
                    fileWriter.write(",\n");
                }

                JSONObject logLine = logService.generateLogLine(selectionModel, masterFieldList);
                fileWriter.write(logLine.toString());
                streamJobTracker.setLogCount(streamJobTracker.getLogCount() + 1);

                // determine if a log lines repeats
                if (Math.random() < selectionModel.getRepeatingLoglinesPercent()
                        && streamJobTracker.getStatus() == JobStatus.ACTIVE) {
                    fileWriter.write(",\n");
                    fileWriter.write(logLine.toString());
                    streamJobTracker.setLogCount(streamJobTracker.getLogCount() + 1);
                }
            }

            // write a ] to end the log file
            fileWriter.write("]");

            fileWriter.close();

        } catch (IOException e) {
            // Mark the job as failed if exception occurred
            streamJobTracker.setStatus(JobStatus.FAILED);
            throw new FilePathNotFoundException(e.getMessage());
        }

        // Mark the job as completed
        streamJobTracker.setStatus(JobStatus.COMPLETED);
    }

    /**
     * Sends a single post request to specified address and returns
     * true if the request was successful
     *
     * @param selectionModel
     * @return
     */
    public boolean isAddressAvailable(SelectionModel selectionModel) {

        // Setup web client for post request to user specified address
        String streamAddress = selectionModel.getStreamSettings().getStreamAddress();
        WebClient webClient = WebClient.create(streamAddress);
        JSONObject[] logLine = { logService.generateLogLine(selectionModel, new HashSet<>()) };

        try {
            // set up post request
            ResponseEntity<String> response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(logLine)
                    .retrieve()
                    .toEntity(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            return false;
        } catch (WebClientRequestException e) {
            return false;
        }

        return true;
    }

    public String generateJobId() {
        return UUID.randomUUID().toString();
    }
}