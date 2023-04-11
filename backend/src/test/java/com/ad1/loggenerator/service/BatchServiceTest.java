package com.ad1.loggenerator.service;

import static org.hamcrest.CoreMatchers.*;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.*;

import com.ad1.loggenerator.exception.FilePathNotFoundException;
import com.ad1.loggenerator.model.BatchSettings;
import com.ad1.loggenerator.model.BatchTracker;
import com.ad1.loggenerator.model.CustomLog;
import com.ad1.loggenerator.model.FieldSettings;
import com.ad1.loggenerator.model.JobStatus;
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
import com.ad1.loggenerator.service.implementation.BatchService;
import com.ad1.loggenerator.service.implementation.LogService;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalMatchers.*;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BatchServiceTest {
    
    private BatchService batchService;
    private LogService logService;

    private String jobId;
    private double repeatingLoglinesPercentage;
    private FieldSettings fieldSettings;
    private MalwareSettings malwareSettings;
    private String mode;
    private StreamSettings streamSettings;
    private List<CustomLog> customLogs;

    private BatchSettings batchSettings0;
    private BatchSettings batchSettings1;
    private BatchSettings batchSettings2;

    private SelectionModel selectionModel0;
    private SelectionModel selectionModel1;
    private SelectionModel selectionModel2;
    private SelectionModel selectionModelMock0;
    private SelectionModel selectionModelMock1;
    private SelectionModel selectionModelMock2;

    private BatchTracker batchTracker;

    private final String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private static String batchModeFolder = "C:\\log-generator\\batch\\";
    private static String jsonRegexString = "^\\[((\\{(\".*\":.*)*\\})(,\\s)?)*\\]$";

    @BeforeEach
    public void setUp() {

        logService = new LogService();
        batchService = new BatchService(logService);

        // Set up for all SelectionModels
        jobId = "1";
        repeatingLoglinesPercentage = 0;
        malwareSettings = new MalwareSettings(true, true, true);
        mode = "Batch";
        streamSettings = null;
        customLogs = null;

        TimeStamp timeStamp = new TimeStamp(true, new ArrayList<Long>());
        ProcessingTime processingTime = new ProcessingTime(true, new ArrayList<Long>());
        CurrentUserId currentUserId = new CurrentUserId(true, new ArrayList<String>());
        BusinessGuid businessGuid = new BusinessGuid(true, new ArrayList<String>());
        PathToFile pathToFile = new PathToFile(true, new ArrayList<String>());
        FileSha256 fileSha256 = new FileSha256(true, new ArrayList<String>());
        Disposition disposition = new Disposition(false, new ArrayList<Integer>());
        fieldSettings = new FieldSettings(
            timeStamp,
            processingTime,
            currentUserId,
            businessGuid,
            pathToFile,
            fileSha256,
            disposition
        );

        batchSettings0 = new BatchSettings(0);
        batchSettings1 = new BatchSettings(1);
        batchSettings2 = new BatchSettings(2);

        // Set up for selectionModel for 0, 1, 2 batch size

        selectionModel0 = new SelectionModel(
            jobId,
            repeatingLoglinesPercentage,
            fieldSettings,
            malwareSettings,
            mode,
            streamSettings,
            batchSettings0,
            customLogs
        );

        selectionModel1 = new SelectionModel(
            jobId,
            repeatingLoglinesPercentage,
            fieldSettings,
            malwareSettings,
            mode,
            streamSettings,
            batchSettings1,
            customLogs
        );

        selectionModel2 = new SelectionModel(
            jobId,
            repeatingLoglinesPercentage,
            fieldSettings,
            malwareSettings,
            mode,
            streamSettings,
            batchSettings2,
            customLogs
        );

        // Set up mock selection models for 0, 1, 2 batch size
        selectionModelMock0 = createSelectionModel(batchSettings0, repeatingLoglinesPercentage);
        selectionModelMock1 = createSelectionModel(batchSettings1, repeatingLoglinesPercentage);
        selectionModelMock2 = createSelectionModel(batchSettings2, repeatingLoglinesPercentage);

        // Set up a single batch job tracker for all tests
        batchTracker = new BatchTracker(
            jobId,
            0,
            0,
            0,
            0,
            null,
            JobStatus.ACTIVE
        );
        
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCorrectLogCount_1() {
        batchService.batchMode(selectionModel0, batchTracker);

        assertEquals(
            0,
            batchTracker.getLogCount(),
            "Log count in batch tracker should be correct"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCorrectLogCount_2() {
        batchService.batchMode(selectionModel1, batchTracker);

        assertEquals(
            1,
            batchTracker.getLogCount(),
            "Log count in batch tracker should be correct"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCorrectLogCount_3() {
        batchService.batchMode(selectionModel2, batchTracker);

        assertEquals(
            2,
            batchTracker.getLogCount(),
            "Log count in batch tracker should be correct"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCorrectLogCountReapeatingLogLines_4() {
        selectionModel0.setRepeatingLoglinesPercent(0.5);
        batchService.batchMode(selectionModel0, batchTracker);

        assertEquals(
            0,
            batchTracker.getLogCount(),
            "Log count in batch tracker should be correct 50% repeating log lines"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCorrectLogCountRepeatingLogLines_5() {
        selectionModel1.setRepeatingLoglinesPercent(0.5);
        batchService.batchMode(selectionModel1, batchTracker);

        assertEquals(
            1,
            batchTracker.getLogCount(),
            "Log count in batch tracker should be correct 50% repeating log lines"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCorrectLogCountRepeatingLogLines_6() {
        selectionModel2.setRepeatingLoglinesPercent(0.5);
        batchService.batchMode(selectionModel2, batchTracker);

        assertEquals(
            2,
            batchTracker.getLogCount(),
            "Log count in batch tracker should be correct 50% repeating log lines"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCorrectLogCountRepeatingLogLines_7() {
        selectionModel0.setRepeatingLoglinesPercent(1);
        batchService.batchMode(selectionModel0, batchTracker);

        assertEquals(
            0,
            batchTracker.getLogCount(),
            "Log count in batch tracker should be correct 100% repeating log lines"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCorrectLogCountRepeatingLogLines_8() {
        selectionModel1.setRepeatingLoglinesPercent(1);
        batchService.batchMode(selectionModel1, batchTracker);

        assertEquals(
            1,
            batchTracker.getLogCount(),
            "Log count in batch tracker should be correct 100% repeating log lines"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCorrectLogCountRepeatingLogLines_9() {
        selectionModel2.setRepeatingLoglinesPercent(1);
        batchService.batchMode(selectionModel2, batchTracker);

        assertEquals(
            2,
            batchTracker.getLogCount(),
            "Log count in batch tracker should be correct 100% repeating log lines"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCOMPLETEDStatus_10() {
        batchService.batchMode(selectionModel0, batchTracker);

        assertEquals(
            JobStatus.COMPLETED,
            batchTracker.getStatus(),
            "Status should be COMPLETED if job finishes successfully"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCOMPLETEDStatus_11() {
        batchService.batchMode(selectionModel1, batchTracker);

        assertEquals(
            JobStatus.COMPLETED,
            batchTracker.getStatus(),
            "Status should be COMPLETED if job finishes successfully"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveCOMPLETEDStatus_12() {
        batchService.batchMode(selectionModel2, batchTracker);

        assertEquals(
            JobStatus.COMPLETED,
            batchTracker.getStatus(),
            "Status should be COMPLETED if job finishes successfully"
        );
    }

    @Test
    public void test_batchMode_batchTrackerShouldHaveFAILEDStatus_13() {
        selectionModel1.setJobId(">");
        batchTracker.setJobId(">");

        assertThrows(
            FilePathNotFoundException.class,
            () -> batchService.batchMode(selectionModel1, batchTracker)
        );
        assertEquals(
            JobStatus.FAILED,
            batchTracker.getStatus(),
            "Status should be FAILED if failure to write to file"
        );
    }

    @Test
    public void test_batchMode_fileShouldHaveCorrectOutput_14() {
        batchService.batchMode(selectionModel0, batchTracker);
        String fileName = batchModeFolder + batchTracker.getJobId() + ".json";
        File jsonLogFile = new File(fileName);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(jsonLogFile));
            String expected = "[]";
            String actual = reader.readLine();
            assertEquals(expected, actual, "Logs written to file should be correct");
        } catch (IOException e) {
            fail("IOException ocurred while trying to read the file generated");
        }
    }

    @Test
    public void test_batchMode_fileShouldHaveCorrectOutput_15() {
        batchService.batchMode(selectionModel1, batchTracker);
        String fileName = batchModeFolder + batchTracker.getJobId() + ".json";
        File jsonLogFile = new File(fileName);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(jsonLogFile));
            String line = reader.readLine();
            String actual = "";
            while (line != null) {
                actual = actual + line;
                line = reader.readLine();
            }
            assertTrue(actual.matches(jsonRegexString), "Logs written to file should be correct");
        } catch (IOException e) {
            fail("IOException ocurred while trying to read the file generated");
        }
    }

    @Test
    public void test_batchMode_fileShouldHaveCorrectOutput_16() {
        batchService.batchMode(selectionModel2, batchTracker);
        String fileName = batchModeFolder + batchTracker.getJobId() + ".json";
        File jsonLogFile = new File(fileName);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(jsonLogFile));
            String line = reader.readLine();
            String actual = "";
            while (line != null) {
                actual = actual + line;
                line = reader.readLine();
            }
            assertTrue(actual.matches(jsonRegexString), "Logs written to file should be correct");
        } catch (IOException e) {
            fail("IOException ocurred while trying to read the file generated");
        }
    }

    @Test
    public void test_batchMode_testCorrectSelectionModelCallsNoRepeatingLogs_17() {
        batchService.batchMode(selectionModelMock0, batchTracker);

        verify(selectionModelMock0, times(1)).getBatchSettings();
        verify(selectionModelMock0, times(1)).getJobId();
        verify(selectionModelMock0, times(2)).getCustomLogs();
        verify(selectionModelMock0, times(1)).getFieldSettings();
    }

    @Test
    public void test_batchMode_testCorrectSelectionModelCallsNoRepeatingLogs_18() {
        batchService.batchMode(selectionModelMock1, batchTracker);

        verify(selectionModelMock1, times(1)).getBatchSettings();
        verify(selectionModelMock1, times(1)).getJobId();
        verify(selectionModelMock1, times(3)).getCustomLogs();
        verify(selectionModelMock1, times(2)).getFieldSettings();
        verify(selectionModelMock1, times(1)).getRepeatingLoglinesPercent();
    }

    @Test
    public void test_batchMode_testCorrectSelectionModelCallsNoRepeatingLogs_19() {
        batchService.batchMode(selectionModelMock2, batchTracker);

        verify(selectionModelMock2, times(1)).getBatchSettings();
        verify(selectionModelMock2, times(1)).getJobId();
        verify(selectionModelMock2, times(4)).getCustomLogs();
        verify(selectionModelMock2, times(3)).getFieldSettings();
        verify(selectionModelMock2, times(2)).getRepeatingLoglinesPercent();
    }

    @Test
    public void test_batchMode_testCorrectSelectionModelCallsRepeatingLogs_20() {
        selectionModelMock0 = createSelectionModel(batchSettings0, 1);
        batchService.batchMode(selectionModelMock0, batchTracker);

        verify(selectionModelMock0, times(1)).getBatchSettings();
        verify(selectionModelMock0, times(1)).getJobId();
        verify(selectionModelMock0, times(2)).getCustomLogs();
        verify(selectionModelMock0, times(1)).getFieldSettings();
    }

    @Test
    public void test_batchMode_testCorrectSelectionModelCallsRepeatingLogs_21() {
        selectionModelMock1 = createSelectionModel(batchSettings1, 1);
        batchService.batchMode(selectionModelMock1, batchTracker);

        verify(selectionModelMock1, times(1)).getBatchSettings();
        verify(selectionModelMock1, times(1)).getJobId();
        verify(selectionModelMock1, times(3)).getCustomLogs();
        verify(selectionModelMock1, times(2)).getFieldSettings();
        verify(selectionModelMock1, times(1)).getRepeatingLoglinesPercent();
    }

    @Test
    public void test_batchMode_testCorrectSelectionModelCallsRepeatingLogs_22() {
        selectionModelMock2 = createSelectionModel(batchSettings2, 1);
        batchService.batchMode(selectionModelMock2, batchTracker);

        verify(selectionModelMock2, times(1)).getBatchSettings();
        verify(selectionModelMock2, times(1)).getJobId();
        verify(selectionModelMock2, times(4)).getCustomLogs();
        verify(selectionModelMock2, times(3)).getFieldSettings();
        verify(selectionModelMock2, times(2)).getRepeatingLoglinesPercent();
    }

    @Test
    public void test_batchMode_testCorrectLogServiceCallsNoRepeatingLogs_23() {
        logService = mock(LogService.class);

        when(logService.generateLogLine(
            any(SelectionModel.class),
            any(Set.class))
        ).thenReturn(new JSONObject());

        batchService = new BatchService(logService);
        batchService.batchMode(selectionModel0, batchTracker);

        verify(logService, times(0))
            .generateLogLine(any(SelectionModel.class), any(Set.class));
        verify(logService, times(1))
            .preProcessCustomLogs(or(any(List.class), eq(null)), any(SelectionModel.class));
        verify(logService, times(1))
            .getMasterFieldsList(or(any(List.class), eq(null)));
    }

    @Test
    public void test_batchMode_testCorrectLogServiceCallsNoRepeatingLogs_24() {
        logService = mock(LogService.class);

        when(logService.generateLogLine(
            any(SelectionModel.class),
            any(Set.class))
        ).thenReturn(new JSONObject());

        batchService = new BatchService(logService);
        batchService.batchMode(selectionModel1, batchTracker);

        verify(logService, times(1))
            .generateLogLine(any(SelectionModel.class), any(Set.class));
        verify(logService, times(1))
            .preProcessCustomLogs(or(any(List.class), eq(null)), any(SelectionModel.class));
        verify(logService, times(1))
            .getMasterFieldsList(or(any(List.class), eq(null)));
    }

    @Test
    public void test_batchMode_testCorrectLogServiceCallsNoRepeatingLogs_25() {
        logService = mock(LogService.class);

        when(logService.generateLogLine(
            any(SelectionModel.class),
            any(Set.class))
        ).thenReturn(new JSONObject());

        batchService = new BatchService(logService);
        batchService.batchMode(selectionModel2, batchTracker);

        verify(logService, times(2))
            .generateLogLine(any(SelectionModel.class), any(Set.class));
        verify(logService, times(1))
            .preProcessCustomLogs(or(any(List.class), eq(null)), any(SelectionModel.class));
        verify(logService, times(1))
            .getMasterFieldsList(or(any(List.class), eq(null)));
    }

    @Test
    public void test_batchMode_testCorrectLogServiceCallsRepeatingLogs_26() {
        logService = mock(LogService.class);
        selectionModel0.setRepeatingLoglinesPercent(1);

        when(logService.generateLogLine(
            any(SelectionModel.class),
            any(Set.class))
        ).thenReturn(new JSONObject());

        batchService = new BatchService(logService);
        batchService.batchMode(selectionModel0, batchTracker);

        verify(logService, times(0))
            .generateLogLine(any(SelectionModel.class), any(Set.class));
        verify(logService, times(1))
            .preProcessCustomLogs(or(any(List.class), eq(null)), any(SelectionModel.class));
        verify(logService, times(1))
            .getMasterFieldsList(or(any(List.class), eq(null)));
    }

    @Test
    public void test_batchMode_testCorrectLogServiceCallsRepeatingLogs_27() {
        logService = mock(LogService.class);
        selectionModel0.setRepeatingLoglinesPercent(1);

        when(logService.generateLogLine(
            any(SelectionModel.class),
            any(Set.class))
        ).thenReturn(new JSONObject());

        batchService = new BatchService(logService);
        batchService.batchMode(selectionModel1, batchTracker);

        verify(logService, times(1))
            .generateLogLine(any(SelectionModel.class), any(Set.class));
        verify(logService, times(1))
            .preProcessCustomLogs(or(any(List.class), eq(null)), any(SelectionModel.class));
        verify(logService, times(1))
            .getMasterFieldsList(or(any(List.class), eq(null)));
    }

    @Test
    public void test_batchMode_testCorrectLogServiceCallsRepeatingLogs_28() {
        logService = mock(LogService.class);
        selectionModel0.setRepeatingLoglinesPercent(1);

        when(logService.generateLogLine(
            any(SelectionModel.class),
            any(Set.class))
        ).thenReturn(new JSONObject());

        batchService = new BatchService(logService);
        batchService.batchMode(selectionModel0, batchTracker);

        verify(logService, times(0))
            .generateLogLine(any(SelectionModel.class), any(Set.class));
        verify(logService, times(1))
            .preProcessCustomLogs(or(any(List.class), eq(null)), any(SelectionModel.class));
        verify(logService, times(1))
            .getMasterFieldsList(or(any(List.class), eq(null)));
    }

    @Test
    public void test_batchMode_testGenerateJobIdFormat_29() {
        String jobIdActual = batchService.generateJobId();

        assertTrue(jobIdActual.matches(uuidRegex), "Job id format should be a UUID");
    }

    private SelectionModel createSelectionModel(BatchSettings batchSettings, double repeatingLogLinesPercentage) {
        SelectionModel selectionModel = mock(SelectionModel.class);

        when(selectionModel.getJobId()).thenReturn(jobId);
        when(selectionModel.getRepeatingLoglinesPercent()).thenReturn(repeatingLoglinesPercentage);
        when(selectionModel.getFieldSettings()).thenReturn(fieldSettings);
        when(selectionModel.getMode()).thenReturn(mode);
        when(selectionModel.getStreamSettings()).thenReturn(streamSettings);
        when(selectionModel.getBatchSettings()).thenReturn(batchSettings);
        when(selectionModel.getCustomLogs()).thenReturn(customLogs);

        return selectionModel;
    }
}
