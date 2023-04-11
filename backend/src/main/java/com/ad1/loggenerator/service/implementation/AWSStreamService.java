package com.ad1.loggenerator.service.implementation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ad1.loggenerator.exception.AWSServiceNotAvailableException;
import com.ad1.loggenerator.exception.FilePathNotFoundException;
import com.ad1.loggenerator.model.JobStatus;
import com.ad1.loggenerator.model.SelectionModel;
import com.ad1.loggenerator.model.StreamTracker;
import com.ad1.loggenerator.service.AWSLogService;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import reactor.core.publisher.Mono;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Service
public class AWSStreamService {

    @Autowired
    private LogService logService;
    @Autowired
    private AWSLogService awsLogService;

    @Async("asyncTaskExecutor")
    public void streamToAddress(SelectionModel selectionModel, StreamTracker streamJobTracker)
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

            // upload logs to s3 is saveLogs
            if (saveLogs) {
                // write a ] to end the log file
                fileWriter.write("]");
                fileWriter.close();

                // upload logs to s3
                try {
                    saveLogsToAWSS3(streamJobTracker);
                } catch (AWSServiceNotAvailableException e) {
                    // Mark the job as failed if exception occurred
                    streamJobTracker.setStatus(JobStatus.FAILED);

                    throw new AWSServiceNotAvailableException(e.getMessage());
                } catch (RuntimeException e) {
                    // Mark the job as failed if exception occurred
                    streamJobTracker.setStatus(JobStatus.FAILED);

                    throw new RuntimeException(e.getMessage());
                } finally {
                    // delete temp file if exists
                    if (tempLogFile.exists()) {
                        tempLogFile.delete();
                    }
                }
            }
        } catch (IOException e) {
            // Mark the job as failed if exception occurred
            streamJobTracker.setStatus(JobStatus.FAILED);

            // delete temp file if exists
            if (tempLogFile != null && tempLogFile.exists()) {
                tempLogFile.delete();
            }
            throw new FilePathNotFoundException(e.getMessage());
        }

        // Mark the job as completed if no errors, failed otherwise
        if (errorMessage[0].isEmpty()) {
            streamJobTracker.setStatus(JobStatus.COMPLETED);
        } else {
            streamJobTracker.setStatus(JobStatus.FAILED);
        }
    }

    @Async("asyncTaskExecutor")
    public void streamToS3Buffer(SelectionModel selectionModel, StreamTracker streamJobTracker)
            throws IOException, AWSServiceNotAvailableException {
        // specify the s3 bucket and key for the log file
        String bucketName = "stream-s3-log-generator";
        String key = "stream/" + awsLogService.createCurrentTimeDate() + ".json";
        // remove fields that should not be included in custom logs
        logService.preProcessCustomLogs(selectionModel.getCustomLogs(), selectionModel);
        Set<String> masterFieldList = logService.getMasterFieldsList(selectionModel.getCustomLogs());
        // create s3 client instance
        AmazonS3 s3Client = awsLogService.createS3Client();
        // reset the start time of the stream
        streamJobTracker.setLastPing(System.currentTimeMillis() / 1000);
        // specify buffer size for uploading log lines to S3
        int bufferSize = 20 * 1024 * 1024; // 20MB buffer
        // create a temporary buffer to store log lines
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
        // keep track of the number of log lines written to the buffer
        int numLogLines = 0;

        try {
            // generate and write log lines to buffer
            while (streamJobTracker.getStatus() == JobStatus.ACTIVE) {

                // generate log line
                JSONObject logLine = logService.generateLogLine(selectionModel, masterFieldList);
                // add a comma before log line if it's not the first one
                if (numLogLines > 0) {
                    buffer.write(",\n".getBytes());
                }
                // write log line to buffer
                buffer.write(logLine.toString().getBytes());

                // determine if a log line repeats
                if (Math.random() < selectionModel.getRepeatingLoglinesPercent()) {
                    if (numLogLines > 0) {
                        buffer.write(",\n".getBytes());
                    }
                    buffer.write(logLine.toString().getBytes());
                }
                numLogLines++;

                // upload buffer to S3 when it is full
                if (buffer.size() >= bufferSize) {
                    try (InputStream inputStream = new ByteArrayInputStream(
                            ("[" + buffer.toString() + "]").getBytes())) {
                        ObjectMetadata metadata = new ObjectMetadata();
                        metadata.setContentLength(buffer.size() + 2);
                        s3Client.putObject(bucketName, key, inputStream, metadata);
                    }
                    buffer.reset();
                    numLogLines = 0;
                }
            }

            // upload remaining log lines to S3
            if (buffer.size() > 0) {
                try (InputStream inputStream = new ByteArrayInputStream(("[" + buffer.toString() + "]").getBytes())) {
                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(buffer.size() + 2);
                    s3Client.putObject(bucketName, key, inputStream, metadata);
                }
                buffer.reset();
            }

            // Make the s3 object public
            s3Client.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);
            // Get the url of the s3 object and the s3 object
            URL objectURL = s3Client.getUrl(bucketName, key);
            S3Object s3Object = s3Client.getObject(bucketName, key);
            // Set the url of the s3 object and count of the log lines saved to the bucket
            // object to StreamJobTracker
            streamJobTracker.setStreamObjectURL(objectURL);
            streamJobTracker.setLogCount(awsLogService.getLogCount(s3Client, s3Object, bucketName, key));

            // Mark the job as completed
            streamJobTracker.setStatus(JobStatus.COMPLETED);
        } catch (SdkClientException e) {
            // Mark the job as failed if exception occurred
            streamJobTracker.setStatus(JobStatus.FAILED);
            throw new AWSServiceNotAvailableException(e.getMessage());
        } catch (IOException e) {
            // Mark the job as failed if exception occurred
            streamJobTracker.setStatus(JobStatus.FAILED);
            throw new IOException(e.getMessage());
        }

    }

    @Async("asyncTaskExecutor")
    public void streamToS3(SelectionModel selectionModel, StreamTracker streamJobTracker)
            throws IOException, AWSServiceNotAvailableException {
        // specify the s3 bucket and key for the log file
        String bucketName = "stream-s3-log-generator";
        String key = "stream/" + awsLogService.createCurrentTimeDate() + ".json";

        // remove fields that should not be included in custom logs
        logService.preProcessCustomLogs(selectionModel.getCustomLogs(), selectionModel);
        Set<String> masterFieldList = logService.getMasterFieldsList(selectionModel.getCustomLogs());

        // create s3 client instance
        AmazonS3 s3Client = awsLogService.createS3Client();

        // reset the start time of the stream
        streamJobTracker.setLastPing(System.currentTimeMillis() / 1000);

        // create StringBuilder object to append log lines
        StringBuilder stringBuilder = new StringBuilder();
        // keep track of the number of log lines written to the stringBuilder
        int numLogLines = 0;

        try {
            while (streamJobTracker.getStatus() == JobStatus.ACTIVE) {
                // append a delimiter if not the first log line generated
                if (numLogLines > 0) {
                    stringBuilder.append(",\n");
                }
                // generate log line
                JSONObject logLine = logService.generateLogLine(selectionModel, masterFieldList);
                // append log line to StringBuilder
                stringBuilder.append(logLine.toString());
                // determine if a log line repeats
                if (Math.random() < selectionModel.getRepeatingLoglinesPercent()) {
                    // append a delimiter
                    if (numLogLines > 0) {
                        stringBuilder.append(",\n");
                    }
                    // append repeated log line to StringBuilder
                    stringBuilder.append(logLine.toString());
                }
                numLogLines++;
            }
            if (streamJobTracker.getStatus() != JobStatus.ACTIVE && streamJobTracker.getStreamObjectURL() == null)
                streamJobTracker.setStatus(JobStatus.FAILED);
            // write StringBuilder content to S3
            byte[] contentAsBytes = ("[" + stringBuilder.toString() + "]").getBytes();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentAsBytes.length);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(contentAsBytes);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, metadata);
            s3Client.putObject(putObjectRequest);
            // Make the s3 object public
            s3Client.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);
            // Get the url of the s3 object and the s3 object itself
            URL objectURL = s3Client.getUrl(bucketName, key);
            S3Object s3Object = s3Client.getObject(bucketName, key);
            // Count the log lines saved to the bucket object, set url and the log count to
            // the stream job tracker
            streamJobTracker.setLogCount(awsLogService.getLogCount(s3Client, s3Object, bucketName, key));
            streamJobTracker.setStreamObjectURL(objectURL);

            // Mark the job as completed
            streamJobTracker.setStatus(JobStatus.COMPLETED);
        } catch (SdkClientException e) {
            // Mark the job as failed if exception occurred
            streamJobTracker.setStatus(JobStatus.FAILED);
            throw new AWSServiceNotAvailableException(e.getMessage());
        } catch (IOException e) {
            // Mark the job as failed if exception occurred
            streamJobTracker.setStatus(JobStatus.FAILED);
            throw new IOException(e.getMessage());
        }
    }

    public void saveLogsToAWSS3(StreamTracker streamJobTracker)
            throws IOException, AWSServiceNotAvailableException, RuntimeException {
        // specify the s3 bucket and key for the log file
        String bucketName = "stream-s3-log-generator";
        String key = "stream/" + awsLogService.createCurrentTimeDate() + ".json";
        // create s3 client instance
        AmazonS3 s3Client = awsLogService.createS3Client();
        // specify the local file path to upload
        File tempLogFile = new File("logs\\stream\\" + streamJobTracker.getJobId() + ".json");
        if (!tempLogFile.exists()) {
            throw new RuntimeException("temp log file do not exist");
        }
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, tempLogFile);
            s3Client.putObject(putObjectRequest);

            // Make the s3 object public
            s3Client.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);
            // Get the url of the s3 object and the s3 object itself
            URL objectURL = s3Client.getUrl(bucketName, key);
            S3Object s3Object = s3Client.getObject(bucketName, key);
            // Count the log lines saved to the bucket object, set url and the log count to
            // the stream job tracker
            streamJobTracker.setLogCount(awsLogService.getLogCount(s3Client, s3Object, bucketName, key));
            streamJobTracker.setStreamObjectURL(objectURL);

            // Mark the job as completed
            streamJobTracker.setStatus(JobStatus.COMPLETED);
        } catch (SdkClientException e) {
            // Mark the job as failed if exception occurred
            streamJobTracker.setStatus(JobStatus.FAILED);
            throw new AWSServiceNotAvailableException(e.getMessage());
        }
    }

}
