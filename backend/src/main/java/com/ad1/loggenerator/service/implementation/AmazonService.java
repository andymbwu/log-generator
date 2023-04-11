package com.ad1.loggenerator.service.implementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ad1.loggenerator.service.AWSLogService;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class AmazonService implements AWSLogService {
    /**
     * Method to create AmazonS3 client
     * 
     * @return s3 client
     */
    @Override
    public AmazonS3 createS3Client() {
        String accessKey = "AKIATRCCSGZZ3T45TPFC";
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
     * 
     * @return current time
     */
    @Override
    public String createCurrentTimeDate() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatDateTime = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = currentDateTime.format(formatDateTime);
        return timestamp;
    }

    /**
     * Method to generate random JobIds
     * 
     * @return random job ids in UUID format
     */
    @Override
    public String generateJobId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Method to get the log counts from an S3 bucket object
     * 
     * @param s3Client
     * @param s3Object
     * @param bucketName
     * @param key
     * @return
     * @throws IOException
     */
    @Override
    public int getLogCount(AmazonS3 s3Client, S3Object s3Object, String bucketName, String key) throws IOException {
        long logCount;
        s3Object = s3Client.getObject(bucketName, key);
        try (InputStream inputStream = s3Object.getObjectContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            logCount = reader.lines().count();
        }
        return (int) logCount;
    }
}
