package com.ad1.loggenerator.service.implementation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ad1.loggenerator.exception.AWSServiceNotAvailableException;
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
        File tempLogFile = new File(streamJobTracker.getJobId() + ".json");
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
