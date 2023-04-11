package com.ad1.loggenerator.controller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.ad1.loggenerator.model.AllJobMetrics;
import com.ad1.loggenerator.model.BatchJobMetrics;
import com.ad1.loggenerator.model.BatchSettings;
import com.ad1.loggenerator.model.CustomLog;
import com.ad1.loggenerator.model.FieldSettings;
import com.ad1.loggenerator.model.JobStatus;
import com.ad1.loggenerator.model.MalwareSettings;
import com.ad1.loggenerator.model.SelectionModel;
import com.ad1.loggenerator.model.StreamJobMetrics;
import com.ad1.loggenerator.model.StreamSettings;
import com.ad1.loggenerator.model.fieldsettingsmodels.BusinessGuid;
import com.ad1.loggenerator.model.fieldsettingsmodels.CurrentUserId;
import com.ad1.loggenerator.model.fieldsettingsmodels.Disposition;
import com.ad1.loggenerator.model.fieldsettingsmodels.FileSha256;
import com.ad1.loggenerator.model.fieldsettingsmodels.PathToFile;
import com.ad1.loggenerator.model.fieldsettingsmodels.ProcessingTime;
import com.ad1.loggenerator.model.fieldsettingsmodels.TimeStamp;
import com.ad1.loggenerator.service.AWSLogService;
import com.ad1.loggenerator.service.implementation.AWSBatchService;
import com.ad1.loggenerator.service.implementation.AWSStreamService;
import com.ad1.loggenerator.service.implementation.BatchTrackerService;
import com.ad1.loggenerator.service.implementation.StatisticsUtilitiesService;
import com.ad1.loggenerator.service.implementation.StreamTrackerService;
import com.ad1.loggenerator.service.implementation.StreamingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@WebMvcTest(LogController.class)
public class LogControllerTest {

    @MockBean
    private AWSBatchService awsBatchService;
    @MockBean
    private BatchTrackerService batchTrackerService;
    @MockBean
    private AWSStreamService awsStreamService;
    @MockBean
    private StreamTrackerService streamTrackerService;
    @MockBean
    private StatisticsUtilitiesService statisticsUtilitiesService;
    @MockBean
    private AWSLogService awsLogService;
    @MockBean
    private StreamingService streamingService;

    private SelectionModel selectionModelBatch;
    private SelectionModel selectionModelStream;
    private String jobId;

    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private String batchModeUrl = "/api/v1/generate/batch/s3";
    private String streamModeUrl = "/api/v1/generate/stream/s3";
    private String streamModeBufferUrl = "/api/v1/generate/stream/s3/Buffer";
    private String stopBatchUrl = "/api/v1/generate/batch/stop/";
    private String stopStreamUrl = "/api/v1/generate/stream/stop/";
    private String continueStreamUrl = "/api/v1/generate/stream/continue/";
    private String testStreamUrl = "/api/v1/generate/stream/toAddress";
    private String batchMetricsUrl = "/api/v1/generate/stats/batch/";
    private String streamMetricsUrl = "/api/v1/generate/stats/stream/";
    private String metricsUrl = "/api/v1/generate/stats";

    @BeforeEach
    public void setUp() throws Exception {
        // Set up SelectionModel to make requests

        jobId = "1";
        double repeatingLoglinesPercentage = 0;
        MalwareSettings malwareSettings = new MalwareSettings(true, true, true);
        String streamMode = "Stream";
        String batchMode = "Batch";
        StreamSettings streamSettings = new StreamSettings(" ", 10, true);
        BatchSettings batchSettings = new BatchSettings(0);
        List<CustomLog> customLogs = new ArrayList<CustomLog>();

        TimeStamp timeStamp = new TimeStamp(true, new ArrayList<Long>());
        ProcessingTime processingTime = new ProcessingTime(true, new ArrayList<Long>());
        CurrentUserId currentUserId = new CurrentUserId(true, new ArrayList<String>());
        BusinessGuid businessGuid = new BusinessGuid(true, new ArrayList<String>());
        PathToFile pathToFile = new PathToFile(true, new ArrayList<String>());
        FileSha256 fileSha256 = new FileSha256(true, new ArrayList<String>());
        Disposition disposition = new Disposition(false, new ArrayList<Integer>());
        FieldSettings fieldSettings = new FieldSettings(
                timeStamp,
                processingTime,
                currentUserId,
                businessGuid,
                pathToFile,
                fileSha256,
                disposition);

        selectionModelStream = new SelectionModel(
                jobId,
                repeatingLoglinesPercentage,
                fieldSettings,
                malwareSettings,
                streamMode,
                streamSettings,
                batchSettings,
                customLogs);

        selectionModelBatch = new SelectionModel(
                jobId,
                repeatingLoglinesPercentage,
                fieldSettings,
                malwareSettings,
                batchMode,
                streamSettings,
                batchSettings,
                customLogs);

        setUpAWSBatchServiceMock();
        setUpBatchTrackerServiceMock();
        setUpAWSStreamingServiceMock();
        setUpStreamingTrackerServiceMock();
        setUpStatisticsUtilitiesServiceMock();
        setUpAwsLogServiceMock();
    }

    @Test
    public void test_generateBatchRequestToS3_shouldReturnOK_1() throws Exception {
        mockMvc.perform(post(batchModeUrl)
                .content(objectMapper.writeValueAsString(selectionModelBatch))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(jobId));
    }

    @Test
    public void test_generateBatchRequestToS3_shouldReturnBadRequest_2() throws Exception {
        mockMvc.perform(post(batchModeUrl)
                .content(objectMapper.writeValueAsString(selectionModelStream))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid Request. Try again"));
    }

    @Test
    public void test_generateStreamRequestToS3_shouldReturnOK_3() throws Exception {
        mockMvc.perform(post(streamModeUrl)
                .content(objectMapper.writeValueAsString(selectionModelStream))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(jobId));
    }

    @Test
    public void test_generateStreamRequestToS3_shouldReturnBadRequest_4() throws Exception {
        mockMvc.perform(post(streamModeUrl)
                .content(objectMapper.writeValueAsString(selectionModelBatch))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid Request. Try again"));
    }

    @Test
    public void test_generateStreamRequestToS3Buffer_shouldReturnOk_5() throws Exception {
        mockMvc.perform(post(streamModeBufferUrl)
                .content(objectMapper.writeValueAsString(selectionModelStream))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(jobId));
    }

    @Test
    public void test_generateStreamRequestToS3Buffer_shouldReturnBadRequest_6() throws Exception {
        mockMvc.perform(post(streamModeBufferUrl)
                .content(objectMapper.writeValueAsString(selectionModelBatch))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid Request. Try again"));
    }

    @Test
    public void test_stopBatchRequest_shouldReturnOk_7() throws Exception {
        when(batchTrackerService.stopBatchJob(anyString())).thenReturn(true);

        mockMvc.perform(post(stopBatchUrl + jobId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("Batch job has stopped."));
    }

    @Test
    public void test_stopBatchRequest_shouldReturnNotFound_8() throws Exception {
        when(batchTrackerService.stopBatchJob(anyString())).thenReturn(false);

        mockMvc.perform(post(stopBatchUrl + jobId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message", equalTo("Job Id not found for " + jobId)));
    }

    @Test
    public void test_stopStreamRequest_shouldReturnOk_9() throws Exception {
        when(streamTrackerService.stopStreamJob(anyString())).thenReturn(true);

        mockMvc.perform(post(stopStreamUrl + jobId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("Streaming has stopped."));
    }

    @Test
    public void test_stopStreamRequest_shouldReturnNotFound_10() throws Exception {
        when(streamTrackerService.stopStreamJob(anyString())).thenReturn(false);

        mockMvc.perform(post(stopStreamUrl + jobId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message", equalTo("Job Id not found for " + jobId)));
    }

    @Test
    public void test_continueStreamRequest_shouldReturnOk_11() throws Exception {
        when(streamTrackerService.continueStreamJob(anyString())).thenReturn(true);

        mockMvc.perform(post(continueStreamUrl + jobId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("Streaming will continue."));
    }

    @Test
    public void test_continueStreamRequest_shouldReturnNotFound_12() throws Exception {
        when(streamTrackerService.continueStreamJob(anyString())).thenReturn(false);

        mockMvc.perform(post(continueStreamUrl + jobId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message", equalTo("Job Id not found for " + jobId)));
    }

    @Test
    public void test_addressStream_shouldReturnOk_13() throws Exception {

        ArrayList<JSONObject> streamData = new ArrayList<JSONObject>();

        mockMvc.perform(post(testStreamUrl)
                .content(objectMapper.writeValueAsString(streamData))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Data successfully received."));
    }

    @Test
    public void test_getBatchJobMetrics_shouldReturnBatchJobMetrics_14() throws Exception {
        BatchJobMetrics metrics = new BatchJobMetrics(
                jobId,
                10,
                System.currentTimeMillis() / 1000,
                10,
                System.currentTimeMillis() / 1000 + 10,
                100,
                null,
                JobStatus.COMPLETED);

        when(statisticsUtilitiesService.generateBatchJobMetrics(anyString())).thenReturn(metrics);

        mockMvc.perform(get(batchMetricsUrl + jobId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("jobId", equalTo(metrics.getJobId())))
                .andExpect(jsonPath("logCount", equalTo(metrics.getLogCount())))
                .andExpect(jsonPath("startTime", equalTo(Integer.valueOf("" + metrics.getStartTime()))))
                .andExpect(jsonPath("runTime", equalTo(Integer.valueOf("" + metrics.getRunTime()))))
                .andExpect(jsonPath("endTime", equalTo(Integer.valueOf("" + metrics.getEndTime()))))
                .andExpect(jsonPath("batchSize", equalTo(metrics.getBatchSize())))
                .andExpect(jsonPath("batchObjectURL", nullValue()))
                .andExpect(jsonPath("status", equalTo(metrics.getStatus().toString())));
    }

    @Test
    public void test_getBatchJobMetrics_shouldReturnNotFound_15() throws Exception {
        BatchJobMetrics metrics = null;

        when(statisticsUtilitiesService.generateBatchJobMetrics(anyString())).thenReturn(metrics);

        mockMvc.perform(get(batchMetricsUrl + jobId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("message", equalTo("Job Id not found for job " + jobId)));
    }

    @Test
    public void test_getStreamJobMetrics_shouldReturnStreamJobMetrics_16() throws Exception {
        StreamJobMetrics metrics = new StreamJobMetrics(
                jobId,
                10,
                System.currentTimeMillis() / 1000,
                10,
                System.currentTimeMillis() / 1000 + 10,
                JobStatus.COMPLETED,
                null);

        when(statisticsUtilitiesService.generateStreamJobMetrics(anyString())).thenReturn(metrics);

        mockMvc.perform(get(streamMetricsUrl + jobId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("jobId", equalTo(metrics.getJobId())))
                .andExpect(jsonPath("logCount", equalTo(metrics.getLogCount())))
                .andExpect(jsonPath("startTime", equalTo(Integer.valueOf("" + metrics.getStartTime()))))
                .andExpect(jsonPath("runTime", equalTo(Integer.valueOf("" + metrics.getRunTime()))))
                .andExpect(jsonPath("endTime", equalTo(Integer.valueOf("" + metrics.getEndTime()))))
                .andExpect(jsonPath("streamObjectURL", nullValue()))
                .andExpect(jsonPath("status", equalTo(metrics.getStatus().toString())));
    }

    @Test
    public void test_getStreamJobMetrics_shouldReturnNotFound_17() throws Exception {
        StreamJobMetrics metrics = null;

        when(statisticsUtilitiesService.generateStreamJobMetrics(anyString())).thenReturn(metrics);

        mockMvc.perform(get(streamMetricsUrl + jobId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("message", equalTo("Job Id not found for job " + jobId)));
    }

    @Test
    public void test_getAllJobMetrics_shouldReturnAllJobMetrics_18() throws Exception {
        StreamJobMetrics streamMetrics = new StreamJobMetrics(
                jobId,
                10,
                System.currentTimeMillis() / 1000,
                10,
                System.currentTimeMillis() / 1000 + 10,
                JobStatus.COMPLETED,
                null);

        BatchJobMetrics batchMetrics = new BatchJobMetrics(
                jobId,
                10,
                System.currentTimeMillis() / 1000,
                10,
                System.currentTimeMillis() / 1000 + 10,
                100,
                null,
                JobStatus.COMPLETED);

        List<BatchJobMetrics> batchJobs = new ArrayList<BatchJobMetrics>();
        batchJobs.add(batchMetrics);
        List<StreamJobMetrics> streamJobs = new ArrayList<StreamJobMetrics>();
        streamJobs.add(streamMetrics);

        AllJobMetrics allJobMetrics = new AllJobMetrics(
                0,
                1,
                0,
                1,
                batchJobs,
                streamJobs);

        when(statisticsUtilitiesService.generateAllJobMetrics()).thenReturn(allJobMetrics);

        mockMvc.perform(get(metricsUrl)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("numActiveStreamJobs", equalTo(allJobMetrics.getNumActiveStreamJobs())))
                .andExpect(jsonPath("numAllStreamJobs", equalTo(allJobMetrics.getNumAllStreamJobs())))
                .andExpect(jsonPath("numActiveBatchJobs", equalTo(allJobMetrics.getNumActiveBatchJobs())))
                .andExpect(jsonPath("numAllBatchJobs", equalTo(allJobMetrics.getNumAllBatchJobs())))

                .andExpect(jsonPath("$.batchJobs[:1].jobId", hasItem(equalTo(batchMetrics.getJobId()))))
                .andExpect(jsonPath("$.batchJobs[:1].logCount", hasItem(equalTo(batchMetrics.getLogCount()))))
                .andExpect(jsonPath("$.batchJobs[:1].startTime",
                        hasItem(equalTo(Integer.valueOf("" + batchMetrics.getStartTime())))))
                .andExpect(jsonPath("$.batchJobs[:1].runTime",
                        hasItem(equalTo(Integer.valueOf("" + batchMetrics.getRunTime())))))
                .andExpect(jsonPath("$.batchJobs[:1].endTime",
                        hasItem(equalTo(Integer.valueOf("" + batchMetrics.getEndTime())))))
                .andExpect(jsonPath("$.batchJobs[:1].batchSize", hasItem(equalTo(batchMetrics.getBatchSize()))))
                .andExpect(jsonPath("$.batchJobs[:1].batchObjectURL", hasItem(nullValue())))
                .andExpect(jsonPath("$.batchJobs[:1].status", hasItem(equalTo(batchMetrics.getStatus().toString()))))

                .andExpect(jsonPath("$.streamJobs[:1].jobId", hasItem(equalTo(streamMetrics.getJobId()))))
                .andExpect(jsonPath("$.streamJobs[:1].logCount", hasItem(equalTo(streamMetrics.getLogCount()))))
                .andExpect(jsonPath("$.streamJobs[:1].startTime",
                        hasItem(equalTo(Integer.valueOf("" + streamMetrics.getStartTime())))))
                .andExpect(jsonPath("$.streamJobs[:1].runTime",
                        hasItem(equalTo(Integer.valueOf("" + streamMetrics.getRunTime())))))
                .andExpect(jsonPath("$.streamJobs[:1].endTime",
                        hasItem(equalTo(Integer.valueOf("" + streamMetrics.getEndTime())))))
                .andExpect(jsonPath("$.streamJobs[:1].status", hasItem(equalTo(streamMetrics.getStatus().toString()))))
                .andExpect(jsonPath("$.streamJobs[:1].streamObjectURL", hasItem(nullValue())));
    }

    private void setUpAWSBatchServiceMock() {

    }

    private void setUpBatchTrackerServiceMock() {

    }

    private void setUpAWSStreamingServiceMock() {

    }

    private void setUpStreamingTrackerServiceMock() {

    }

    private void setUpAwsLogServiceMock() {
        when(awsLogService.generateJobId()).thenReturn(jobId);
    }

    private void setUpStatisticsUtilitiesServiceMock() {

    }
}
