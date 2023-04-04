package com.ad1.loggenerator.service.implementation;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ad1.loggenerator.exception.AWSServiceNotAvailableException;
import com.ad1.loggenerator.model.BatchSettings;
import com.ad1.loggenerator.model.BatchTracker;
import com.ad1.loggenerator.model.JobStatus;
import com.ad1.loggenerator.model.SelectionModel;
import com.ad1.loggenerator.service.AWSLogService;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
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
public class AWSBatchService {
    @Autowired
    private LogService logService;
    @Autowired
    private AWSLogService awsLogService;

    /**
     * Generates and populates logs in batch mode
     * Save generated logs to AWS S3
     * 
     * @param selectionModel defines all the parameters to be included in the batch
     *                       files as per the user
     */
    @Async("asyncTaskExecutor")
    public void upLoadBatchLogsToS3(SelectionModel selectionModel, BatchTracker batchJobTracker) throws IOException {
        // create s3 client instance and specify s3 bucket name and key
        String bucketName = "batch-s3-log-generator";
        String key = "batch/" + awsLogService.createCurrentTimeDate() + ".json";
        AmazonS3 s3Client = awsLogService.createS3Client();

        // remove fields that should not be included in custom logs
        logService.preProcessCustomLogs(selectionModel.getCustomLogs(), selectionModel);
        Set<String> masterFieldList = logService.getMasterFieldsList(selectionModel.getCustomLogs());

        try {
            // batch settings
            BatchSettings batchSettings = selectionModel.getBatchSettings();
            // Add log lines to a StringBuilder
            StringBuilder logLines = new StringBuilder();
            // append [ as the first character
            logLines.append("[");

            for (int i = 0; i < batchSettings.getNumberOfLogs()
                    && batchJobTracker.getStatus() == JobStatus.ACTIVE; i++) {

                if (i > 0) {
                    // add a delimiter if not the first log line generated
                    logLines.append(",\n");
                }

                JSONObject logLine = logService.generateLogLine(selectionModel, masterFieldList);
                logLines.append(logLine.toString());
                batchJobTracker.setLogCount(batchJobTracker.getLogCount() + 1);

                // determine if a log lines repeats
                if (Math.random() < selectionModel.getRepeatingLoglinesPercent()) {
                    logLines.append(",\n");
                    logLines.append(logLine.toString());
                    i++;
                    batchJobTracker.setLogCount(batchJobTracker.getLogCount() + 1);
                }
            }

            // set job status to finalizing
            batchJobTracker.setStatus(JobStatus.FINALIZING);

            // append ] as the last character
            logLines.append("]");
            // Upload the batch file to S3
            s3Client.putObject(bucketName, key, logLines.toString());

            // Make the s3 object public
            s3Client.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);
            // Get the url of the s3 object and set it to the BatchTracker
            URL objectURL = s3Client.getUrl(bucketName, key);
            batchJobTracker.setBatchObjectURL(objectURL);
            // Get the s3 object and count the log lines saved to the bucket object
            S3Object s3Object = s3Client.getObject(bucketName, key);
            batchJobTracker.setLogCount(awsLogService.getLogCount(s3Client, s3Object, bucketName, key));

            // Mark the job as completed
            batchJobTracker.setStatus(JobStatus.COMPLETED);
        } catch (SdkClientException e) {
            // Mark the job as failed if exception occurred
            batchJobTracker.setStatus(JobStatus.FAILED);
            throw new AWSServiceNotAvailableException(e.getMessage());
        } catch (IOException e) {
            // Mark the job as failed if exception occurred
            batchJobTracker.setStatus(JobStatus.FAILED);
            throw new IOException(e.getMessage());
        }

    }

}
