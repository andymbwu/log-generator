package com.ad1.loggenerator.service.implementation;

import com.ad1.loggenerator.model.BatchSettings;
import com.ad1.loggenerator.model.BatchTracker;
import com.ad1.loggenerator.model.SelectionModel;
import com.ad1.loggenerator.service.AmazonService;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AWSBatchService implements AmazonService {
    @Autowired
    private LogService logService;

    /**
     * Method to create AmazonS3 client
     * @return s3 client
     */
    @Override
    public AmazonS3 createS3Client() {
        String accessKey = "AKIATRCCSGZZS2Q5MIUZ";
        String secretKey = "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";

        // Create Amazon S3 client
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withRegion(Regions.US_EAST_2)
                .build();
        return s3Client;

    }

    /**
     * Method to create currentTimeDate as a String to append to filepath
     * @return current time
     */
    @Override
    public String createCurrentTimeDate() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatDateTime = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = currentDateTime.format(formatDateTime);
        return  timestamp;
    }

    /**
     * Generates and populates logs in batch mode
     *
     * @param selectionModel defines all the parameters to be included in the batch
     *                       files as per the user
     */
    @Override
    public void upLoadBatchLogsToS3(SelectionModel selectionModel, BatchTracker batchJobTracker) {
        // specify s3 bucketname and key
        String bucketName = "batch-s3-log-generator";
        String key = "batch/" + createCurrentTimeDate() + ".json";
        AmazonS3 s3Client = createS3Client();

        try {
            // batch settings
            BatchSettings batchSettings = selectionModel.getBatchSettings();
            // Add log lines to a StringBuilder
            StringBuilder logLines = new StringBuilder();
            for (int i = 0; i < batchSettings.getNumberOfLogs(); i++) {
                JSONObject logLine = logService.generateLogLine(selectionModel);
                logLines.append(logLine.toString() + "\n");
                batchJobTracker.setLogCount(batchJobTracker.getLogCount() + 1);

                // determine if a log lines repeats
                if (Math.random() < selectionModel.getRepeatingLoglinesPercent()) {
                    logLines.append(logLine.toString() + "\n");
                    i++;
                    batchJobTracker.setLogCount(batchJobTracker.getLogCount() + 1);
                }
            }
            // Upload the batch file to S3
            s3Client.putObject(bucketName, key, logLines.toString());

        } catch(Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error occurred while saving file to aws S3");
        }
        //Make the s3 object public
        s3Client.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);
        // Get the url of the s3 object and set it to the BatchTracker
        URL objectURL = s3Client.getUrl(bucketName, key);
        batchJobTracker.setGetBatchObjectURL(objectURL);
    }

    @Override
    public String generateJobId() {
        return UUID.randomUUID().toString();
    }

}