package com.ad1.loggenerator.service;
import com.ad1.loggenerator.model.*;
import com.ad1.loggenerator.model.fieldsettingsmodels.*;
import com.ad1.loggenerator.service.implementation.AWSBatchService;
import com.ad1.loggenerator.service.implementation.LogService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.S3Object;
import mockit.Mocked;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Test;



@ExtendWith(MockitoExtension.class)
public class AWSBatchServiceTest {
    @InjectMocks
    private AWSBatchService awsBatchService;

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
    private BatchTracker batchJobTracker;


    @Before
    public void setUp() throws IOException {
        // given - set up the mock objects and their behavior
        selectionModel = mock(SelectionModel.class);
        batchJobTracker = mock(BatchTracker.class);
        BatchSettings batchSettings = mock(BatchSettings.class);
        FieldSettings fieldSettings = mock(FieldSettings.class);
        TimeStamp timeStamp = mock(TimeStamp.class);
        Disposition disposition = mock(Disposition.class);
        ProcessingTime processingTime = mock(ProcessingTime.class);
        FileSha256 fileSha256 = mock(FileSha256.class);
        BusinessGuid businessGuid = mock(BusinessGuid.class);
        PathToFile pathToFile = mock(PathToFile.class);
        CurrentUserId currentUserId = mock(CurrentUserId.class);
        masterFieldList = mock(Set.class);
        CustomLog customLog1 = mock(CustomLog.class);
        List<CustomLog> customLogs = new ArrayList<>();
        customLogs.add(customLog1);
        awsLogService = mock(AWSLogService.class);
        logService = mock(LogService.class);
        s3Client = mock(AmazonS3.class);
        JSONObject logLine = mock(JSONObject.class);
        S3Object s3Object = mock(S3Object.class);

        when(customLog1.getFrequency()).thenReturn(0.2);
        when(selectionModel.getCustomLogs()).thenReturn(customLogs);
        when(batchSettings.getNumberOfLogs()).thenReturn(10);
        when(selectionModel.getBatchSettings()).thenReturn(batchSettings);
        when(selectionModel.getFieldSettings()).thenReturn(fieldSettings);
        when(logService.getMasterFieldsList(customLogs)).thenReturn(masterFieldList);
        when(timeStamp.getInclude()).thenReturn(true);
        when(fieldSettings.getTimeStamp()).thenReturn(timeStamp);
        when(disposition.getInclude()).thenReturn(true);
        when(fieldSettings.getDisposition()).thenReturn(disposition);
        when(processingTime.getInclude()).thenReturn(true);
        when(fieldSettings.getProcessingTime()).thenReturn(processingTime);
        when(fileSha256.getInclude()).thenReturn(true);
        when(fieldSettings.getFileSHA256()).thenReturn(fileSha256);
        when(businessGuid.getInclude()).thenReturn(true);
        when(fieldSettings.getBusinessGUID()).thenReturn(businessGuid);
        when(pathToFile.getInclude()).thenReturn(true);
        when(fieldSettings.getPathToFile()).thenReturn(pathToFile);
        when(currentUserId.getInclude()).thenReturn(true);
        when(fieldSettings.getCurrentUserID()).thenReturn(currentUserId);
        when(logService.generateLogLine(selectionModel, masterFieldList)).thenReturn(logLine);
        when(s3Client.getObject(anyString(), anyString())).thenReturn(s3Object);
        when(awsLogService.getLogCount(any(AmazonS3.class), any(S3Object.class), anyString(), anyString())).thenReturn(10);
        when(awsLogService.createS3Client()).thenReturn(s3Client);

        // when - call the method being tested
        awsBatchService = new AWSBatchService(logService, awsLogService);
        awsBatchService.upLoadBatchLogsToS3(selectionModel, batchJobTracker);
    }

//    @DisplayName("Testing upload logs to AWS S3 bucket in batch mode- verify generate log line was invoked 10 times")
//    @Test
//    public void testGenerateLogLines() {
//        // then - verify that the expected interactions occurred
//        verify(logService, times(10)).generateLogLine(selectionModel, masterFieldList);
//    }

    @DisplayName("Testing upload logs to AWS S3 bucket in batch mode- verify an object was put in AmazonS3 bucket once")
    @Test
    public void testPutObjectInAmazonS3() {
        // then - verify that the expected interactions occurred
        verify(s3Client, times(1)).putObject(anyString(), anyString(), anyString());
    }

    @DisplayName("Testing upload logs to AWS S3 bucket in batch mode - verify the Object ACL is set to public read while uploading log lines")
    @Test
    public void testObjectACLSetUp() {
        // then - verify that the expected interactions occurred
        verify(s3Client, times(1)).setObjectAcl(anyString(), anyString(), eq(CannedAccessControlList.PublicRead));
    }

    @DisplayName("Testing upload logs to AWS S3 bucket in batch mode - verify object url is got once")
    @Test
    public void testGetObjectUrl() {
        // then - verify that the expected interactions occurred
        verify(s3Client, times(1)).getUrl(anyString(), anyString());
    }


    @DisplayName("Testing upload logs to AWS S3 bucket in batch mode - " +
            "verify job status is set to completed after log generation and set to batchJobTracker")
    @Test
    public void testJobStatusSetToCompleted() {
        // then - verify that the expected interactions occurred
        verify(batchJobTracker, times(1)).setStatus(eq(JobStatus.COMPLETED));
    }

    @DisplayName("Testing upload logs to AWS S3 bucket in batch mode - verify batchJobTracker is set to equivalent log generated")
    @Test
    public void testSetLogCountToBatchTracker() {
        // then - verify that the expected interactions occurred
        verify(batchJobTracker, times(1)).setLogCount(eq(10));
    }
}

























//@ExtendWith(MockitoExtension.class)
//public class AWSBatchServiceTest {
//    @InjectMocks
//    private AWSBatchService awsBatchService;
//
//    @DisplayName("Testing upload logs to AWS S3 bucket in batch mode")
//    @Test
//    public void testUpLoadBatchLogsToS3() throws IOException {
//        // given - set up the mock objects and their behavior
//        SelectionModel selectionModel = mock(SelectionModel.class);
//        BatchTracker batchJobTracker = mock(BatchTracker.class);
//        BatchSettings batchSettings = mock(BatchSettings.class);
//        FieldSettings fieldSettings = mock(FieldSettings.class);
//        TimeStamp timeStamp = mock(TimeStamp.class);
//        Disposition disposition = mock(Disposition.class);
//        ProcessingTime processingTime = mock(ProcessingTime.class);
//        FileSha256 fileSha256 = mock(FileSha256.class);
//        BusinessGuid businessGuid = mock(BusinessGuid.class);
//        PathToFile pathToFile = mock(PathToFile.class);
//        CurrentUserId currentUserId = mock(CurrentUserId.class);
//        Set<String> masterFieldList = mock(Set.class);
//
//        CustomLog customLog1 = mock(CustomLog.class);
//        List<CustomLog> customLogs = new ArrayList<>();
//        customLogs.add(customLog1);
//
//        AWSLogService awsLogService = mock(AWSLogService.class);
//        LogService logService = mock(LogService.class);
//        AmazonS3 s3Client = mock(AmazonS3.class);
//        JSONObject logLine = mock(JSONObject.class);
//        S3Object s3Object = mock(S3Object.class);
//
//        when(customLog1.getFrequency()).thenReturn(0.2);
//        when(selectionModel.getCustomLogs()).thenReturn(customLogs);
//
//        when(batchSettings.getNumberOfLogs()).thenReturn(10);
//        when(selectionModel.getBatchSettings()).thenReturn(batchSettings);
//        when(selectionModel.getFieldSettings()).thenReturn(fieldSettings);
//        when(logService.getMasterFieldsList(customLogs)).thenReturn(masterFieldList);
//
//
//        when(timeStamp.getInclude()).thenReturn(true);
//        when(fieldSettings.getTimeStamp()).thenReturn(timeStamp);
//
//        when(disposition.getInclude()).thenReturn(true);
//        when(fieldSettings.getDisposition()).thenReturn(disposition);
//
//        when(processingTime.getInclude()).thenReturn(true);
//        when(fieldSettings.getProcessingTime()).thenReturn(processingTime);
//
//        when(fileSha256.getInclude()).thenReturn(true);
//        when(fieldSettings.getFileSHA256()).thenReturn(fileSha256);
//
//        when(businessGuid.getInclude()).thenReturn(true);
//        when(fieldSettings.getBusinessGUID()).thenReturn(businessGuid);
//
//        when(pathToFile.getInclude()).thenReturn(true);
//        when(fieldSettings.getPathToFile()).thenReturn(pathToFile);
//
//        when(currentUserId.getInclude()).thenReturn(true);
//        when(fieldSettings.getCurrentUserID()).thenReturn(currentUserId);
//
//        when(logService.generateLogLine(selectionModel, masterFieldList)).thenReturn(logLine);
//
//
//        when(s3Client.getObject(anyString(), anyString())).thenReturn(s3Object);
//        when(awsLogService.getLogCount(any(AmazonS3.class), any(S3Object.class), anyString(), anyString())).thenReturn(10);
//        when(awsLogService.createS3Client()).thenReturn(s3Client);
//
//
//        // when - call the method being tested
//        awsBatchService = new AWSBatchService(logService, awsLogService);
//        awsBatchService.upLoadBatchLogsToS3(selectionModel, batchJobTracker);
//
//        // then - verify that the expected interactions occurred
//        verify(logService, times(10)).generateLogLine(selectionModel, masterFieldList);
//        verify(s3Client, times(1)).putObject(anyString(), anyString(), anyString());
//        verify(s3Client, times(1)).setObjectAcl(anyString(), anyString(), eq(CannedAccessControlList.PublicRead));
//        verify(s3Client, times(1)).getUrl(anyString(), anyString());
//        verify(batchJobTracker, times(1)).setStatus(eq(JobStatus.COMPLETED));
//        verify(batchJobTracker, times(1)).setLogCount(eq(10));
//    }
//
//}