package com.ad1.loggenerator.controller;

import java.net.URL;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ad1.loggenerator.exception.AddressNotFoundException;
import com.ad1.loggenerator.model.BatchTracker;
import com.ad1.loggenerator.model.JobStatus;
import com.ad1.loggenerator.model.SelectionModel;
import com.ad1.loggenerator.model.StreamTracker;
import com.ad1.loggenerator.service.implementation.BatchService;
import com.ad1.loggenerator.service.implementation.BatchTrackerService;
import com.ad1.loggenerator.service.implementation.StreamTrackerService;
import com.ad1.loggenerator.service.implementation.StreamingService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

/**
 * Receives user requests and returns responses via REST API
 */
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/generate")
@CrossOrigin("http://localhost:3000/")
public class LogsToFileController {

    private final BatchService batchService;
    private final BatchTrackerService batchServiceTracker;
    private final StreamingService streamingService;
    private final StreamTrackerService streamServiceTracker;

    /**
     * Method to generate log files in batch mode and save locally
     * 
     * @param selectionModel
     * @return
     * @throws InterruptedException
     */
    @PostMapping("/batch")
    public ResponseEntity<String> generateBatchRequest(
            @Valid @RequestBody SelectionModel selectionModel) throws InterruptedException {

        URL object = null;
        if (selectionModel.getMode().equals("Batch")) {
            String jobId = batchService.generateJobId();
            selectionModel.setJobId(jobId);
            BatchTracker batchJobTracker = new BatchTracker(
                    jobId,
                    0,
                    selectionModel.getBatchSettings().getNumberOfLogs(),
                    System.currentTimeMillis() / 1000,
                    -1,
                    object,
                    JobStatus.ACTIVE);
            batchService.batchMode(selectionModel, batchJobTracker);
            batchServiceTracker.addNewJob(batchJobTracker);
            if (batchServiceTracker.getActiveJobsListSize() == 1) {
                batchServiceTracker.sendBatchData();
            }
            return new ResponseEntity<>(jobId, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invalid Request. Try again", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Method to stream logs to an address and optionally also save locally
     * 
     * @param selectionModel
     * @return
     * @throws InterruptedException
     */
    @PostMapping("/stream")
    public ResponseEntity<String> generateStreamRequest(
            @Valid @RequestBody SelectionModel selectionModel) throws InterruptedException {
        URL object = null;
        if (selectionModel.getMode().equals("Stream")) {

            // If a stream address was specified, check the address
            if (!selectionModel.getStreamSettings().getStreamAddress().isEmpty()) {
                boolean isAddressAvailable = streamingService.isAddressAvailable(selectionModel);
                if (!isAddressAvailable) {
                    throw new AddressNotFoundException("Stream address " +
                            selectionModel.getStreamSettings().getStreamAddress() +
                            " is not available.");

                }
            }

            // if lograte is negative, set it to max value
            if (selectionModel.getStreamSettings().getLogRate() <= 0) {
                selectionModel.getStreamSettings().setLogRate(Integer.MAX_VALUE);
            }

            String jobId = streamingService.generateJobId();
            selectionModel.setJobId(jobId);
            StreamTracker streamJobTracker = new StreamTracker(
                    jobId,
                    0,
                    System.currentTimeMillis() / 1000,
                    JobStatus.ACTIVE,
                    System.currentTimeMillis() / 1000,
                    -1,
                    object);
            streamingService.streamMode(selectionModel, streamJobTracker);
            streamServiceTracker.addNewJob(streamJobTracker);
            if (streamServiceTracker.getActiveJobsListSize() == 1) {
                streamServiceTracker.checkLastPings();
            }
            return new ResponseEntity<>(jobId, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invalid Request. Try again", HttpStatus.BAD_REQUEST);
        }
    }
}
