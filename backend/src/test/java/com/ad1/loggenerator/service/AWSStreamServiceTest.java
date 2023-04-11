package com.ad1.loggenerator.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ad1.loggenerator.model.CustomLog;
import com.ad1.loggenerator.model.SelectionModel;
import com.ad1.loggenerator.model.StreamTracker;
import com.ad1.loggenerator.service.implementation.AWSStreamService;
import com.ad1.loggenerator.service.implementation.LogService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.S3Object;

import mockit.Mocked;

@ExtendWith(MockitoExtension.class)
public class AWSStreamServiceTest {
    @InjectMocks
    private AWSStreamService awsStreamService;

    @Mocked
    private LogService logService;

    @Mocked
    private AWSLogService awsLogService;

    @Mocked
    private SelectionModel selectionModel;

    @Mocked
    Set<String> masterFieldList;

    @Mocked
    private AmazonS3 s3Client;

    @Mocked
    private StreamTracker streamJobTracker;

    @Mocked
    LocalDateTime currentDateTime;

    @Mocked
    DateTimeFormatter formatDateTime;

    @Before
    public void setUp() throws IOException {
        // given - set up the mock objects and their behavior
        currentDateTime = LocalDateTime.now();
        formatDateTime = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        selectionModel = mock(SelectionModel.class);
        streamJobTracker = mock(StreamTracker.class);
        masterFieldList = mock(Set.class);
        CustomLog customLog1 = mock(CustomLog.class);
        List<CustomLog> customLogs = new ArrayList<>();
        customLogs.add(customLog1);
        JSONObject logLine = mock(JSONObject.class);
        awsLogService = mock(AWSLogService.class);
        logService = mock(LogService.class);
        s3Client = mock(AmazonS3.class);
        S3Object s3Object = mock(S3Object.class);

        when(awsLogService.createCurrentTimeDate()).thenReturn(currentDateTime.format(formatDateTime));
        when(customLog1.getFrequency()).thenReturn(0.2);
        when(selectionModel.getCustomLogs()).thenReturn(customLogs);
        when(logService.getMasterFieldsList(selectionModel.getCustomLogs())).thenReturn(masterFieldList);
        when(logService.generateLogLine(selectionModel, masterFieldList)).thenReturn(logLine);
        when(s3Client.getObject(anyString(), anyString())).thenReturn(s3Object);
        when(awsLogService.getLogCount(any(AmazonS3.class), any(S3Object.class), anyString(), anyString()))
                .thenReturn(10);
        when(awsLogService.createS3Client()).thenReturn(s3Client);

        // Act (when) - call the method being tested
        awsStreamService = new AWSStreamService(logService, awsLogService);
        awsStreamService.streamToS3Buffer(selectionModel, streamJobTracker);
    }

    @DisplayName("Verify remove fields that should be included in custom logs is invoked once")
    @Test
    public void testPreprocessCustomLogs() {
        // then - verify that the expected interactions occurred
        verify(logService, times(1)).preProcessCustomLogs(selectionModel.getCustomLogs(), selectionModel);
    }

    @DisplayName("Verify get master fields list is invoked once")
    @Test
    public void testGetMasterFieldsList() {
        // then - verify that the expected interactions occurred
        verify(logService, times(1)).getMasterFieldsList(selectionModel.getCustomLogs());
    }

    // @DisplayName("Verify log lines are generated at least once")
    // @Test
    // public void testStreamToS3LogLineGeneration() {
    // // then - verify that the expected interactions occurred
    // verify(logService, atLeastOnce()).generateLogLine(selectionModel,
    // masterFieldList);
    // }

    @DisplayName("Verify currentTimeDate is created once to append to file path")
    @Test
    public void testCreateCurrentTimeDate() {
        // then - verify that the expected interactions occurred
        verify(awsLogService, times(1)).createCurrentTimeDate();
    }

    @DisplayName("Verify Amazon S3 Client is created once")
    @Test
    public void testCreateAmazonS3Client() {
        // then - verify that the expected interactions occurred
        verify(awsLogService, times(1)).createS3Client();
    }

    // @DisplayName("Verify that the log lines are put in the s3 object at least
    // once, " +
    // "given a string bucketname, a string key, an inputstream and metadata")
    // @Test
    // public void testPutObject() {
    // // then - verify that the expected interactions occurred
    // verify(s3Client, atLeastOnce()).putObject(anyString(), anyString(),
    // any(InputStream.class), any(ObjectMetadata.class));
    // }

    @DisplayName("Verify the Object ACL is set to public read while streaming log lines to S3 buffer")
    @Test
    public void testSetObjectACLToPublicRead() {
        // then - verify that the expected interactions occurred

        verify(s3Client, times(1)).setObjectAcl(anyString(), anyString(), any(CannedAccessControlList.class));
    }

    @DisplayName("Verify get object URL is invoked once given a string bucketname and a string key")
    @Test
    public void testStreamGetObjectUrl() {
        // then - verify that the expected interactions occurred
        verify(s3Client, times(1)).getUrl(anyString(), anyString());
    }

    @DisplayName("Verify getting the S3 object of S3 Client once, given String bucketname and String key")
    @Test
    public void testGetS3ClientObject() {
        // then - verify that the expected interactions occurred
        verify(s3Client, times(1)).getObject(anyString(), anyString());
    }

    @DisplayName("Test to get the number of log counts given Amazon S3 client, S3Object, a String bucketname and a String key")
    @Test
    public void testGetLogCount() throws IOException {
        // then - verify that the expected interactions occurred
        verify(awsLogService, times(1)).getLogCount(any(AmazonS3.class), any(S3Object.class), anyString(), anyString());

    }

    // @DisplayName("Test to set the stream object url to stream job tracker once")
    // @Test
    // public void testToSetStreamObjectUrlToStreamJobTracker() {
    // // then - verify that the expected interactions occurred
    // verify(streamJobTracker, times(1)).setStreamObjectURL(any(URL.class));
    // }

    @DisplayName("Test to set the number of log counts to stream job tracker")
    @Test
    public void testToSetLogCountToStreamJobTracker() {
        // then - verify that the expected interactions occurred
        verify(streamJobTracker, times(1)).setLogCount(anyInt());
    }
}
