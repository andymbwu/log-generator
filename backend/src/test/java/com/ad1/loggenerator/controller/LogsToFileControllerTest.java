package com.ad1.loggenerator.controller;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.*;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalMatchers.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.ad1.loggenerator.controller.LogsToFileController;
import com.ad1.loggenerator.model.BatchSettings;
import com.ad1.loggenerator.model.MalwareSettings;
import com.ad1.loggenerator.model.SelectionModel;
import com.ad1.loggenerator.model.StreamSettings;
import com.ad1.loggenerator.model.fieldsettingsmodels.BusinessGuid;
import com.ad1.loggenerator.model.fieldsettingsmodels.CurrentUserId;
import com.ad1.loggenerator.model.fieldsettingsmodels.Disposition;
import com.ad1.loggenerator.model.fieldsettingsmodels.FileSha256;
import com.ad1.loggenerator.model.fieldsettingsmodels.PathToFile;
import com.ad1.loggenerator.model.fieldsettingsmodels.ProcessingTime;
import com.ad1.loggenerator.model.fieldsettingsmodels.TimeStamp;
import com.ad1.loggenerator.model.CustomLog;
import com.ad1.loggenerator.model.FieldSettings;
import com.ad1.loggenerator.service.implementation.BatchService;
import com.ad1.loggenerator.service.implementation.BatchTrackerService;
import com.ad1.loggenerator.service.implementation.StreamTrackerService;
import com.ad1.loggenerator.service.implementation.StreamingService;

@RunWith(SpringRunner.class)
@WebMvcTest(LogsToFileController.class)
public class LogsToFileControllerTest {

    @MockBean
    private BatchService batchService;
    @MockBean
    private BatchTrackerService batchTrackerService;
    @MockBean
    private StreamingService streamingService;
    @MockBean
    private StreamTrackerService streamTrackerService;
    private SelectionModel selectionModelBatch;
    private SelectionModel selectionModelStream;
    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private String batchModeURL = "/api/v1/generate/batch";
    private String streamModeURL = "/api/v1/generate/stream";
    private String jobId;

    @BeforeEach
    public void setUp() {
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
            disposition
        );

        selectionModelStream = new SelectionModel(
            jobId,
            repeatingLoglinesPercentage,
            fieldSettings,
            malwareSettings,
            streamMode,
            streamSettings,
            batchSettings,
            customLogs
        );

        selectionModelBatch = new SelectionModel(
            jobId,
            repeatingLoglinesPercentage,
            fieldSettings,
            malwareSettings,
            batchMode,
            streamSettings,
            batchSettings,
            customLogs
        );

        setUpBatchServiceMock();
        setUpBatchTrackerServiceMock();
        setUpStreamingServiceMock();
        setUpStreamingTrackerServiceMock();
    }

    @Test
    public void test_generateBatchRequest_shouldReturnOKRequest_1() throws Exception {
        mockMvc.perform(post(batchModeURL)
            .content(objectMapper.writeValueAsString(selectionModelBatch))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(jobId));
    }

    @Test
    public void test_generateBatchRequest_shouldReturnBadRequest_2() throws Exception {
        mockMvc.perform(post(batchModeURL)
            .content(objectMapper.writeValueAsString(selectionModelStream))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Invalid Request. Try again"));
    }

    @Test
    public void test_generateStreamRequest_shouldReturnOKRequest_3() throws Exception {
        when(streamingService.isAddressAvailable(any(SelectionModel.class))).thenReturn(true);
        mockMvc.perform(post(streamModeURL)
            .content(objectMapper.writeValueAsString(selectionModelStream))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(jobId));
    }

    @Test
    public void test_generateStreamRequest_shouldReturnBadRequest_4() throws Exception {
        when(streamingService.isAddressAvailable(any(SelectionModel.class))).thenReturn(true);
        mockMvc.perform(post(streamModeURL)
            .content(objectMapper.writeValueAsString(selectionModelBatch))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Invalid Request. Try again"));
    }

    @Test
    public void test_generateStreamRequest_shouldReturnNotFoundRequest_5() throws Exception {
        when(streamingService.isAddressAvailable(any(SelectionModel.class))).thenReturn(false);

        mockMvc.perform(post(streamModeURL)
            .content(objectMapper.writeValueAsString(selectionModelStream))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message", equalTo("Stream address   is not available.")));
    }

    public void setUpBatchServiceMock() {
        when(batchService.generateJobId()).thenReturn(jobId);
    }

    public void setUpBatchTrackerServiceMock() {
        
    }

    public void setUpStreamingServiceMock() {
        when(streamingService.generateJobId()).thenReturn(jobId);
    }

    public void setUpStreamingTrackerServiceMock() {
        
    }
}