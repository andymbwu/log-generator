package com.ad1.loggenerator.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ad1.loggenerator.model.CustomLog;
import com.ad1.loggenerator.model.FieldSettings;
import com.ad1.loggenerator.model.SelectionModel;
import com.ad1.loggenerator.model.fieldsettingsmodels.BusinessGuid;
import com.ad1.loggenerator.model.fieldsettingsmodels.CurrentUserId;
import com.ad1.loggenerator.model.fieldsettingsmodels.Disposition;
import com.ad1.loggenerator.model.fieldsettingsmodels.FileSha256;
import com.ad1.loggenerator.model.fieldsettingsmodels.PathToFile;
import com.ad1.loggenerator.model.fieldsettingsmodels.ProcessingTime;
import com.ad1.loggenerator.model.fieldsettingsmodels.TimeStamp;
import com.ad1.loggenerator.service.implementation.LogService;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class LogServiceTest {
    
    private LogService logService;
    private final String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private final String windowsFilePathRegex = "^[A-z]:\\\\([A-z0-9-_+\\s\\(\\)]+\\\\)*([A-z0-9-_+]+\\.([A-z]+))$";
    private final String unixFilePathRegex = "^\\/([A-z0-9-_+]+\\/)*([A-z0-9-_+]+\\.([A-z]+))$";
    private JSONObject logLineJSON;
    private SelectionModel selectionModel;

    @BeforeEach
    public void setUp() {
        logService = new LogService();

        logLineJSON = new JSONObject();
        logLineJSON.put("field1", "value1");
        logLineJSON.put("field2", 2);
        logLineJSON.put("field3", null);

        List<Long> timeStampValues = new ArrayList<Long>();
        timeStampValues.add(Long.valueOf(1));
        TimeStamp timeStamp = new TimeStamp(true, timeStampValues);
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
        selectionModel = new SelectionModel();
        selectionModel.setFieldSettings(fieldSettings);
    }

    @Test
    public void test_generateLogLine_logGeneratedShouldContainAllOurFields_1() {
        selectionModel.getFieldSettings().getDisposition().setInclude(true);
        selectionModel.setCustomLogs(new ArrayList<CustomLog>());
        Set<String> masterFieldList = new HashSet<String>();

        JSONObject expected = new JSONObject();
        expected.put("timeStamp", null);
        expected.put("processingTime", null);
        expected.put("currentUserID", null);
        expected.put("businessGUID", null);
        expected.put("pathToFile", null);
        expected.put("fileSHA256", null);
        expected.put("disposition", null);

        JSONObject actual = logService.generateLogLine(selectionModel, masterFieldList);

        assertEquals(
            expected.keySet(),
            actual.keySet(),
            "No custom logs given, all our fields should be included"
        );
    }

    @Test
    public void test_generateLogLine_logGeneratedShouldOurFieldsOnly_2() {
        selectionModel.getFieldSettings().getDisposition().setInclude(false);
        selectionModel.setCustomLogs(new ArrayList<CustomLog>());
        Set<String> masterFieldList = new HashSet<String>();

        JSONObject expected = new JSONObject();
        expected.put("timeStamp", null);
        expected.put("processingTime", null);
        expected.put("currentUserID", null);
        expected.put("businessGUID", null);
        expected.put("pathToFile", null);
        expected.put("fileSHA256", null);

        JSONObject actual = logService.generateLogLine(selectionModel, masterFieldList);

        assertEquals(
            expected.keySet(),
            actual.keySet(),
            "No custom logs given, all our fields except disposition should be included"
        );
    }

    @Test
    public void test_generateLogLine_logGeneratedShouldBeEmpty_3() {
        selectionModel.getFieldSettings().getDisposition().setInclude(false);
        selectionModel.getFieldSettings().getBusinessGUID().setInclude(false);
        selectionModel.getFieldSettings().getCurrentUserID().setInclude(false);
        selectionModel.getFieldSettings().getFileSHA256().setInclude(false);
        selectionModel.getFieldSettings().getPathToFile().setInclude(false);
        selectionModel.getFieldSettings().getProcessingTime().setInclude(false);
        selectionModel.getFieldSettings().getTimeStamp().setInclude(false);
        selectionModel.setCustomLogs(new ArrayList<CustomLog>());
        Set<String> masterFieldList = new HashSet<String>();

        JSONObject expected = new JSONObject();

        JSONObject actual = logService.generateLogLine(selectionModel, masterFieldList);

        assertEquals(
            expected.keySet(),
            actual.keySet(),
            "No custom logs given, all our fields should be excluded"
        );
    }

    @Test
    public void test_generateLogLine_logGeneratedShouldContainAllCustomLogValues_4() {
        selectionModel.getFieldSettings().getDisposition().setInclude(true);

        ArrayList<CustomLog> customLogs = new ArrayList<CustomLog>();

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("timeStamp", 1);
        fields.put("processingTime", 1);
        fields.put("currentUserID", 1);
        fields.put("businessGUID", 1);
        fields.put("pathToFile", 1);
        fields.put("fileSHA256", 1);
        fields.put("disposition", 1);
        CustomLog customLog = new CustomLog(Double.valueOf(1), fields);

        customLogs.add(customLog);
        selectionModel.setCustomLogs(customLogs);

        Set<String> masterFieldList = new HashSet<String>();
        masterFieldList.add("timeStamp");
        masterFieldList.add("processingTime");
        masterFieldList.add("currentUserID");
        masterFieldList.add("businessGUID");
        masterFieldList.add("pathToFile");
        masterFieldList.add("fileSHA256");
        masterFieldList.add("disposition");

        JSONObject expected = new JSONObject();
        expected.put("timeStamp", 1);
        expected.put("processingTime", 1);
        expected.put("currentUserID", 1);
        expected.put("businessGUID", 1);
        expected.put("pathToFile", 1);
        expected.put("fileSHA256", 1);
        expected.put("disposition", 1);

        JSONObject actual = logService.generateLogLine(selectionModel, masterFieldList);

        assertEquals(
            expected,
            actual,
            "Custom log with all our fields at 100% frequency, all our fields should be included"
        );
    }

    @Test
    public void test_generateLogLine_logGeneratedShouldContainAllCustomLogValues_5() {
        selectionModel.getFieldSettings().getDisposition().setInclude(false);

        ArrayList<CustomLog> customLogs = new ArrayList<CustomLog>();

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("timeStamp", 1);
        fields.put("processingTime", 1);
        fields.put("currentUserID", 1);
        fields.put("businessGUID", 1);
        fields.put("pathToFile", 1);
        fields.put("fileSHA256", 1);
        CustomLog customLog = new CustomLog(Double.valueOf(1), fields);

        customLogs.add(customLog);
        selectionModel.setCustomLogs(customLogs);

        Set<String> masterFieldList = new HashSet<String>();
        masterFieldList.add("timeStamp");
        masterFieldList.add("processingTime");
        masterFieldList.add("currentUserID");
        masterFieldList.add("businessGUID");
        masterFieldList.add("pathToFile");
        masterFieldList.add("fileSHA256");

        JSONObject expected = new JSONObject();
        expected.put("timeStamp", 1);
        expected.put("processingTime", 1);
        expected.put("currentUserID", 1);
        expected.put("businessGUID", 1);
        expected.put("pathToFile", 1);
        expected.put("fileSHA256", 1);

        JSONObject actual = logService.generateLogLine(selectionModel, masterFieldList);

        assertEquals(
            expected,
            actual,
            "Custom log with all our fields but disposition at 100% frequency, all our fields except disposition should be included"
        );
    }

    @Test
    public void test_generateLogLine_logGeneratedShouldContainNewFields_6() {
        selectionModel.getFieldSettings().getDisposition().setInclude(true);

        ArrayList<CustomLog> customLogs = new ArrayList<CustomLog>();

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("field1", 1);
        fields.put("field2", 1);
        CustomLog customLog = new CustomLog(Double.valueOf(1), fields);

        customLogs.add(customLog);
        selectionModel.setCustomLogs(customLogs);

        Set<String> masterFieldList = new HashSet<String>();
        masterFieldList.add("field1");
        masterFieldList.add("field2");

        JSONObject expected = new JSONObject();
        expected.put("timeStamp", 1);
        expected.put("processingTime", 1);
        expected.put("currentUserID", 1);
        expected.put("businessGUID", 1);
        expected.put("pathToFile", 1);
        expected.put("fileSHA256", 1);
        expected.put("disposition", 1);
        expected.put("field1", 1);
        expected.put("field2", 1);

        JSONObject actual = logService.generateLogLine(selectionModel, masterFieldList);

        assertEquals(
            expected.keySet(),
            actual.keySet(),
            "Custom log with all new fields at 100% frequency, all our fields + new fields should be included"
        );
    }

    @Test
    public void test_generateLogLine_logGeneratedShouldContainAllCustomValues_7() {
        selectionModel.getFieldSettings().getDisposition().setInclude(true);

        ArrayList<Long> timeStampValues = new ArrayList<Long>();
        timeStampValues.add(1L);
        ArrayList<Long> proccesingTimeValues = new ArrayList<Long>();
        proccesingTimeValues.add(2L);
        ArrayList<String> currentUserIdValues = new ArrayList<String>();
        currentUserIdValues.add("3");
        ArrayList<String> businnessIdValues = new ArrayList<String>();
        businnessIdValues.add("4");
        ArrayList<String> pathToFileValues = new ArrayList<String>();
        pathToFileValues.add("5");
        ArrayList<String> fileSha256Values = new ArrayList<String>();
        fileSha256Values.add("6");
        ArrayList<Integer> dispositionValues = new ArrayList<Integer>();
        dispositionValues.add(Integer.valueOf(7));

        selectionModel.setCustomLogs(new ArrayList<CustomLog>());
        selectionModel.getFieldSettings().getTimeStamp().setValues(timeStampValues);
        selectionModel.getFieldSettings().getProcessingTime().setValues(proccesingTimeValues);
        selectionModel.getFieldSettings().getCurrentUserID().setValues(currentUserIdValues);
        selectionModel.getFieldSettings().getBusinessGUID().setValues(businnessIdValues);
        selectionModel.getFieldSettings().getPathToFile().setValues(pathToFileValues);
        selectionModel.getFieldSettings().getFileSHA256().setValues(fileSha256Values);
        selectionModel.getFieldSettings().getDisposition().setValues(dispositionValues);

        Set<String> masterFieldList = new HashSet<String>();

        JSONObject expected = new JSONObject();
        expected.put("timeStamp", 1L);
        expected.put("processingTime", 2L);
        expected.put("currentUserID", "3");
        expected.put("businessGUID", "4");
        expected.put("pathToFile", "5");
        expected.put("fileSHA256", "6");
        expected.put("disposition", Integer.valueOf(7));

        JSONObject actual = logService.generateLogLine(selectionModel, masterFieldList);

        assertEquals(
            expected,
            actual,
            "No custom logs given, all our fields should be included, custom values for all fields given"
        );
    }

    @Test
    public void test_generateLogLine_logGeneratedShouldContainAllCustomLogValues_8() {
        selectionModel.getFieldSettings().getDisposition().setInclude(true);

        ArrayList<Long> timeStampValues = new ArrayList<Long>();
        timeStampValues.add(1L);
        ArrayList<Long> proccesingTimeValues = new ArrayList<Long>();
        proccesingTimeValues.add(2L);
        ArrayList<String> currentUserIdValues = new ArrayList<String>();
        currentUserIdValues.add("3");
        ArrayList<String> businnessIdValues = new ArrayList<String>();
        businnessIdValues.add("4");
        ArrayList<String> pathToFileValues = new ArrayList<String>();
        pathToFileValues.add("5");
        ArrayList<String> fileSha256Values = new ArrayList<String>();
        fileSha256Values.add("6");
        ArrayList<Integer> dispositionValues = new ArrayList<Integer>();
        dispositionValues.add(Integer.valueOf(7));

        selectionModel.getFieldSettings().getTimeStamp().setValues(timeStampValues);
        selectionModel.getFieldSettings().getProcessingTime().setValues(proccesingTimeValues);
        selectionModel.getFieldSettings().getCurrentUserID().setValues(currentUserIdValues);
        selectionModel.getFieldSettings().getBusinessGUID().setValues(businnessIdValues);
        selectionModel.getFieldSettings().getPathToFile().setValues(pathToFileValues);
        selectionModel.getFieldSettings().getFileSHA256().setValues(fileSha256Values);
        selectionModel.getFieldSettings().getDisposition().setValues(dispositionValues);

        List<CustomLog> customLogs = new ArrayList<CustomLog>();
        Map<String, Object> customLogFields1 = new HashMap<String, Object>();
        customLogFields1.put("timeStamp", null);
        customLogFields1.put("processingTime", null);
        customLogFields1.put("currentUserID", null);
        customLogFields1.put("businessGUID", null);
        customLogFields1.put("pathToFile", null);
        customLogFields1.put("fileSHA256", null);
        customLogFields1.put("disposition", null);
        CustomLog customLog1 = new CustomLog(Double.valueOf(1), customLogFields1);
        customLogs.add(customLog1);
        selectionModel.setCustomLogs(customLogs);

        Set<String> masterFieldList = new HashSet<String>();
        masterFieldList.add("timeStamp");
        masterFieldList.add("processingTime");
        masterFieldList.add("currentUserID");
        masterFieldList.add("businessGUID");
        masterFieldList.add("pathToFile");
        masterFieldList.add("fileSHA256");
        masterFieldList.add("disposition");

        JSONObject expected = new JSONObject();
        expected.put("timeStamp", null);
        expected.put("processingTime", null);
        expected.put("currentUserID", null);
        expected.put("businessGUID", null);
        expected.put("pathToFile", null);
        expected.put("fileSHA256", null);
        expected.put("disposition", null);

        JSONObject actual = logService.generateLogLine(selectionModel, masterFieldList);

        assertEquals(
            expected,
            actual,
            "Custom log with all our fields given with 100% frequency, all our fields should be included, custom values for all fields given"
        );
    }

    @Test
    public void test_generateLogLine_logGeneratedShouldBeEmpty_9() {
        selectionModel.getFieldSettings().getDisposition().setInclude(false);
        selectionModel.getFieldSettings().getBusinessGUID().setInclude(false);
        selectionModel.getFieldSettings().getCurrentUserID().setInclude(false);
        selectionModel.getFieldSettings().getFileSHA256().setInclude(false);
        selectionModel.getFieldSettings().getPathToFile().setInclude(false);
        selectionModel.getFieldSettings().getProcessingTime().setInclude(false);
        selectionModel.getFieldSettings().getTimeStamp().setInclude(false);

        ArrayList<Long> timeStampValues = new ArrayList<Long>();
        timeStampValues.add(1L);
        ArrayList<Long> proccesingTimeValues = new ArrayList<Long>();
        proccesingTimeValues.add(2L);
        ArrayList<String> currentUserIdValues = new ArrayList<String>();
        currentUserIdValues.add("3");
        ArrayList<String> businnessIdValues = new ArrayList<String>();
        businnessIdValues.add("4");
        ArrayList<String> pathToFileValues = new ArrayList<String>();
        pathToFileValues.add("5");
        ArrayList<String> fileSha256Values = new ArrayList<String>();
        fileSha256Values.add("6");
        ArrayList<Integer> dispositionValues = new ArrayList<Integer>();
        dispositionValues.add(Integer.valueOf(7));

        selectionModel.getFieldSettings().getTimeStamp().setValues(timeStampValues);
        selectionModel.getFieldSettings().getProcessingTime().setValues(proccesingTimeValues);
        selectionModel.getFieldSettings().getCurrentUserID().setValues(currentUserIdValues);
        selectionModel.getFieldSettings().getBusinessGUID().setValues(businnessIdValues);
        selectionModel.getFieldSettings().getPathToFile().setValues(pathToFileValues);
        selectionModel.getFieldSettings().getFileSHA256().setValues(fileSha256Values);
        selectionModel.getFieldSettings().getDisposition().setValues(dispositionValues);

        List<CustomLog> customLogs = new ArrayList<CustomLog>();
        Map<String, Object> customLogFields1 = new HashMap<String, Object>();
        customLogFields1.put("field1", null);
        CustomLog customLog1 = new CustomLog(Double.valueOf(1), customLogFields1);
        customLogs.add(customLog1);
        selectionModel.setCustomLogs(customLogs);

        Set<String> masterFieldList = new HashSet<String>();
        masterFieldList.add("field1");

        JSONObject expected = new JSONObject();
        expected.put("field1", null);

        JSONObject actual = logService.generateLogLine(selectionModel, masterFieldList);

        assertEquals(
            expected,
            actual,
            "Custom log with one extra field at 100% frequency, all our fields should be excluded, custom values for all fields given"
        );
    }

    @Test
    public void test_addMasterFieldList_shouldNotAddAnyFields_1() {
        Set<String> masterFieldList = new HashSet<String>();
        masterFieldList.add("field1");
        masterFieldList.add("field2");
        masterFieldList.add("field3");

        JSONObject expected = new JSONObject();
        expected.put("field1", "value1");
        expected.put("field2", 2);
        expected.put("field3", null);

        logService.addMasterFieldList(logLineJSON, masterFieldList);
        
        assertEquals(expected, logLineJSON);
    }

    @Test
    public void test_addMasterFieldList_shouldAddExtraFields_2() {
        Set<String> masterFieldList = new HashSet<String>();
        masterFieldList.add("field4");
        masterFieldList.add("field5");
        masterFieldList.add("field6");

        JSONObject expected = new JSONObject();
        expected.put("field1", "value1");
        expected.put("field2", 2);
        expected.put("field3", null);
        expected.put("field4", null);
        expected.put("field5", null);
        expected.put("field6", null);

        logService.addMasterFieldList(logLineJSON, masterFieldList);
        
        assertEquals(expected, logLineJSON);
    }

    @Test
    public void test_addMasterFieldList_shouldAddOneExtraField_3() {
        Set<String> masterFieldList = new HashSet<String>();
        masterFieldList.add("field1");
        masterFieldList.add("field2");
        masterFieldList.add("field4");

        JSONObject expected = new JSONObject();
        expected.put("field1", "value1");
        expected.put("field2", 2);
        expected.put("field3", null);
        expected.put("field4", null);

        logService.addMasterFieldList(logLineJSON, masterFieldList);
        
        assertEquals(expected, logLineJSON);
    }


    @Test
    public void test_removeExcludedFields_shouldRemoveExcludedFieldsFromCustomLogs_1() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();

        Map<String, Object> fields1 = new HashMap<String, Object>();
        fields1.put("field1", "value1");
        fields1.put("disposition", "value1");
        CustomLog customLog1 = new CustomLog(null, fields1);

        Map<String, Object> fields2 = new HashMap<String, Object>();
        fields2.put("field2", 2);
        fields2.put("disposition", 2);
        CustomLog customLog2 = new CustomLog(null, fields2);

        customLogs.add(customLog1);
        customLogs.add(customLog2);

        logService.removeExcludedFields(customLogs, selectionModel);

        for (CustomLog customLog: customLogs) {
            assertThat(
                "Custom logs should not have fields that should be excluded",
                customLog.getFields().keySet(),
                not(hasItem("disposition"))
            );
        }
    }

    @Test
    public void test_removeExcludedFields_shouldNotRemoveAnyFields_2() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();

        Map<String, Object> fields1 = new HashMap<String, Object>();
        fields1.put("field1", "value1");
        CustomLog customLog1 = new CustomLog(Double.valueOf(0), fields1);

        Map<String, Object> fields2 = new HashMap<String, Object>();
        fields2.put("field2", 2);
        CustomLog customLog2 = new CustomLog(Double.valueOf(0), fields2);

        customLogs.add(customLog1);
        customLogs.add(customLog2);

        logService.removeExcludedFields(customLogs, selectionModel);

        Map<String, Object> expectedFields1 = new HashMap<String, Object>();
        expectedFields1.put("field1", "value1");
        CustomLog expectedCustomLog1 = new CustomLog(Double.valueOf(0), expectedFields1);

        Map<String, Object> expectedFields2 = new HashMap<String, Object>();
        expectedFields2.put("field2", 2);
        CustomLog expectedCustomLog2 = new CustomLog(Double.valueOf(0), expectedFields2);

        assertThat(
            null,
            customLogs,
            hasItems(expectedCustomLog1, expectedCustomLog2)
        );
    }

    @Test
    public void test_removeExcludedFields_shouldRemoveExcludedFieldsFromCustomLogs_3() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();

        Map<String, Object> fields1 = new HashMap<String, Object>();
        fields1.put("disposition", "value1");
        CustomLog customLog1 = new CustomLog(null, fields1);

        Map<String, Object> fields2 = new HashMap<String, Object>();
        fields2.put("disposition", 2);
        CustomLog customLog2 = new CustomLog(null, fields2);

        customLogs.add(customLog1);
        customLogs.add(customLog2);

        logService.removeExcludedFields(customLogs, selectionModel);

        for (CustomLog customLog: customLogs) {
            assertThat(
                "Custom logs should not have fields that should be excluded",
                customLog.getFields().keySet(),
                not(hasItem("disposition"))
            );
        }
    }

    @Test
    public void test_getMasterFieldsList_shouldReturnEmptySet_1() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();

        Set<String> actual = logService.getMasterFieldsList(customLogs);

        assertTrue(
            actual.size() == 0,
            "Master fields list should be empty"
        );
    }

    @Test
    public void test_getMasterFieldsList_shouldReturnEmptySet_2() {
        List<CustomLog> customLogs = null;

        Set<String> actual = logService.getMasterFieldsList(customLogs);

        assertTrue(
            actual.size() == 0,
            "Master fields list should be empty"
        );
    }

    @Test
    public void test_getMasterFieldsList_shouldContainAllFieldsUniqueCustomLogs_3() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();

        Map<String, Object> fields1 = new HashMap<String, Object>();
        fields1.put("field1", "value1");
        CustomLog customLog1 = new CustomLog(null, fields1);

        Map<String, Object> fields2 = new HashMap<String, Object>();
        fields2.put("field2", 2);
        CustomLog customLog2 = new CustomLog(null, fields2);

        Map<String, Object> fields3 = new HashMap<String, Object>();
        fields3.put("field3", null);
        CustomLog customLog3 = new CustomLog(null, fields3);

        customLogs.add(customLog1);
        customLogs.add(customLog2);
        customLogs.add(customLog3);

        Set<String> expected = new HashSet<String>();
        expected.add("field1");
        expected.add("field2");
        expected.add("field3");

        Set<String> actual = logService.getMasterFieldsList(customLogs);

        assertEquals(
            expected,
            actual,
            "Master field list should contain all unique fields"
        );
    }

    @Test
    public void test_getMasterFieldsList_shouldContainAllFieldsHalfUniqueCustomLogs_4() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();

        Map<String, Object> fields1 = new HashMap<String, Object>();
        fields1.put("field1", "value1");
        fields1.put("field4", "value1");
        CustomLog customLog1 = new CustomLog(null, fields1);

        Map<String, Object> fields2 = new HashMap<String, Object>();
        fields2.put("field2", 2);
        fields2.put("field4", "value1");
        CustomLog customLog2 = new CustomLog(null, fields2);

        Map<String, Object> fields3 = new HashMap<String, Object>();
        fields3.put("field3", null);
        fields3.put("field4", "value1");
        CustomLog customLog3 = new CustomLog(null, fields3);

        customLogs.add(customLog1);
        customLogs.add(customLog2);
        customLogs.add(customLog3);

        Set<String> expected = new HashSet<String>();
        expected.add("field1");
        expected.add("field2");
        expected.add("field3");
        expected.add("field4");

        Set<String> actual = logService.getMasterFieldsList(customLogs);

        assertEquals(
            expected,
            actual,
            "Master field list should contain all unique fields"
        );
    }

    @Test
    public void test_getMasterFieldsList_shouldContainAllFieldsSameCustomLogs_5() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();

        Map<String, Object> fields1 = new HashMap<String, Object>();
        fields1.put("field1", "value1");
        CustomLog customLog1 = new CustomLog(null, fields1);

        Map<String, Object> fields2 = new HashMap<String, Object>();
        fields2.put("field1", 2);
        CustomLog customLog2 = new CustomLog(null, fields2);

        Map<String, Object> fields3 = new HashMap<String, Object>();
        fields3.put("field1", null);
        CustomLog customLog3 = new CustomLog(null, fields3);

        customLogs.add(customLog1);
        customLogs.add(customLog2);
        customLogs.add(customLog3);

        Set<String> expected = new HashSet<String>();
        expected.add("field1");

        Set<String> actual = logService.getMasterFieldsList(customLogs);

        assertEquals(
            expected,
            actual,
            "Master field list should contain all unique fields"
        );
    }

    @Test
    public void test_addCustomLogFields_shouldThrowException_1() {
        CustomLog customLog = new CustomLog(null, null);
        assertThrows(
            IllegalArgumentException.class,
            () -> logService.addCustomLogFields(null, customLog)
        );
    }

    @Test
    public void test_addCustomLogFields_shouldNotChangeLogLineJSON_2() {
        logService.addCustomLogFields(logLineJSON, null);

        JSONObject logLineExpected = new JSONObject();
        logLineExpected.put("field1", "value1");
        logLineExpected.put("field2", 2);
        logLineExpected.put("field3", null);

        assertEquals(
            logLineExpected,
            logLineJSON
        );
    }

    @Test
    public void test_addCustomLogFields_shouldOverwriteAllValues_3() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("field1", 2);
        fields.put("field2", "value1");
        fields.put("field3", 1);
        CustomLog customLog = new CustomLog(null, fields);

        logService.addCustomLogFields(logLineJSON, customLog);

        JSONObject logLineExpected = new JSONObject();
        logLineExpected.put("field1", 2);
        logLineExpected.put("field2", "value1");
        logLineExpected.put("field3", 1);

        assertEquals(
            logLineExpected,
            logLineJSON
        );
    }

    @Test
    public void test_addCustomLogFields_shouldOnlyAddNewFields_4() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("field4", 2);
        fields.put("field5", "value1");
        fields.put("field6", 1);
        CustomLog customLog = new CustomLog(null, fields);

        logService.addCustomLogFields(logLineJSON, customLog);

        JSONObject logLineExpected = new JSONObject();
        logLineExpected.put("field1", "value1");
        logLineExpected.put("field2", 2);
        logLineExpected.put("field3", null);
        logLineExpected.put("field4", 2);
        logLineExpected.put("field5", "value1");
        logLineExpected.put("field6", 1);

        assertEquals(
            logLineExpected,
            logLineJSON
        );
    }

    @Test
    public void test_addCustomLogFields_shouldAddNewFieldsAndOverwriteAllValues_5() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("field1", 2);
        fields.put("field2", "value1");
        fields.put("field3", 1);
        fields.put("field4", 2);
        fields.put("field5", "value1");
        fields.put("field6", 1);
        CustomLog customLog = new CustomLog(null, fields);

        logService.addCustomLogFields(logLineJSON, customLog);

        JSONObject logLineExpected = new JSONObject();
        logLineExpected.put("field1", 2);
        logLineExpected.put("field2", "value1");
        logLineExpected.put("field3", 1);
        logLineExpected.put("field4", 2);
        logLineExpected.put("field5", "value1");
        logLineExpected.put("field6", 1);

        assertEquals(
            logLineExpected,
            logLineJSON
        );
    }

    @Test
    public void test_chooseCustomLog_shouldReturnNull_1() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();
        CustomLog customLog1 = new CustomLog(Double.valueOf(0), new HashMap<>());
        CustomLog customLog2 = new CustomLog(Double.valueOf(0), new HashMap<>());
        CustomLog customLog3 = new CustomLog(Double.valueOf(0), new HashMap<>());

        CustomLog actual = logService.chooseCustomLog(customLogs);

        assertNull(actual, "Custom log should be null if all frequencies are 0");
    }

    @Test
    public void test_chooseCustomLog_shouldReturnNull_2() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();

        CustomLog actual = logService.chooseCustomLog(customLogs);

        assertNull(actual, "Custom log should be null if list is empty");
    }

    @Test
    public void test_chooseCustomLog_shouldReturn1stLog_3() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();
        CustomLog customLog1 = new CustomLog(Double.valueOf(1), new HashMap<>());
        CustomLog customLog2 = new CustomLog(Double.valueOf(0), new HashMap<>());
        CustomLog customLog3 = new CustomLog(Double.valueOf(0), new HashMap<>());
        customLogs.add(customLog1);
        customLogs.add(customLog2);
        customLogs.add(customLog3);

        CustomLog actual = logService.chooseCustomLog(customLogs);

        assertSame(customLog1, actual, "Should choose 1st log if frequency is 100%");
    }

    @Test
    public void test_chooseCustomLog_shouldReturn2ndLog_4() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();
        CustomLog customLog1 = new CustomLog(Double.valueOf(0), new HashMap<>());
        CustomLog customLog2 = new CustomLog(Double.valueOf(1), new HashMap<>());
        CustomLog customLog3 = new CustomLog(Double.valueOf(0), new HashMap<>());
        customLogs.add(customLog1);
        customLogs.add(customLog2);
        customLogs.add(customLog3);

        CustomLog actual = logService.chooseCustomLog(customLogs);

        assertSame(customLog2, actual, "Should choose 2nd log if frequency is 100%");
    }

    @Test
    public void test_chooseCustomLog_shouldReturnLastLog_5() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();
        CustomLog customLog1 = new CustomLog(Double.valueOf(0), new HashMap<>());
        CustomLog customLog2 = new CustomLog(Double.valueOf(0), new HashMap<>());
        CustomLog customLog3 = new CustomLog(Double.valueOf(1), new HashMap<>());
        customLogs.add(customLog1);
        customLogs.add(customLog2);
        customLogs.add(customLog3);

        CustomLog actual = logService.chooseCustomLog(customLogs);

        assertSame(customLog3, actual, "Should choose last log if frequency is 100%");
    }

    @Test
    public void test_chooseCustomLog_shouldReturnAnyCustomLog_6() {
        List<CustomLog> customLogs = new ArrayList<CustomLog>();
        CustomLog customLog1 = new CustomLog(Double.valueOf(0.33), new HashMap<>());
        CustomLog customLog2 = new CustomLog(Double.valueOf(0.33), new HashMap<>());
        CustomLog customLog3 = new CustomLog(Double.valueOf(0.34), new HashMap<>());
        customLogs.add(customLog1);
        customLogs.add(customLog2);
        customLogs.add(customLog3);

        CustomLog actual = logService.chooseCustomLog(customLogs);

        assertThat(
            "Custom log should be one of the values",
            customLogs,
            hasItem(actual)
        );
    }

    @Test
    public void test_generateTimeStamp_shouldBeCustomValue_1() {
        List<Long> values = new ArrayList<Long>();
        values.add(Long.valueOf(-1));
        values.add(Long.valueOf(System.currentTimeMillis()));

        Long actual = logService.generateTimeStamp(values);

        assertThat(
            "Time stamp should be oen of the custom values",
            values,
            hasItem(actual)
        );
    }

    @Test
    public void test_generateTimeStamp_shouldBeInThePast_2() {
        List<Long> values = new ArrayList<Long>();
        
        Long actual = logService.generateTimeStamp(values);

        assertTrue(
            actual < System.currentTimeMillis() / 1000,
            "Random time stamp should be generated and less than current time"
        );
    }

    @Test
    public void test_generateRandomTimeStamp_shouldBeInThePast_1() {
        long actual = logService.generateRandomTimeStamp();

        assertTrue(
            actual < System.currentTimeMillis() / 1000,
            "Random time stamp should be less than current time"
        );
    }

    @Test
    public void test_generateProcessingTime_shouldBeCustomValue_1() {
        List<Long> values = new ArrayList<Long>();
        values.add(Long.valueOf(-1));
        values.add(Long.valueOf(1001));
        values.add(Long.valueOf(10000));

        Long actual = logService.generateProcessingTime(values);

        assertThat(
            "Processing time should be one of the custom values",
            values,
            hasItem(actual)
        );
    }

    @Test
    public void test_generateProcessingTime_shouldBeGreaterThanOrEqualTo0_1() {
        List<Long> values = new ArrayList<Long>();

        Long actual = logService.generateProcessingTime(values);

        assertTrue(
            actual >= 0,
            "Random processing time should be generated and >= 0"
        );
    }

    @Test
    public void test_generateProcessingTime_shouldBeLessThan1000_2() {
        List<Long> values = new ArrayList<Long>();

        Long actual = logService.generateProcessingTime(values);

        assertTrue(
            actual < 1000,
            "Random processing time should be generated and < 1000"
        );
    }

    @Test
    public void test_generateRandomProcessingTime_shouldBeGreaterThanOrEqualTo0_1() {
        long actual = logService.generateRandomProcessingTime();

        assertTrue(
            actual >= 0,
            "Random processing time should be greater than 0"
        );
    }

    @Test
    public void test_generateRandomProcessingTime_shouldBeLessThan1000_2() {
        long actual = logService.generateRandomProcessingTime();

        assertTrue(
            actual < 1000,
            "Random processting time should be less than 1000"
        );
    }

    @Test
    public void test_generateUserId_shouldBeCustomValue_1() {
        List<String> values = new ArrayList<String>();
        values.add("Dummy user id 1");
        values.add("Dummy user id 2");
        values.add("Dummy user id 3");

        String actual = logService.generateUserId(values);

        assertThat(
            "User id should be one of the custom values",
            values,
            hasItem(actual)
        );
    }

    @Test
    public void test_generateUserId_checkRandomUserIdFormat_2() {
        List<String> values = new ArrayList<String>();

        String actual = logService.generateUserId(values);

        assertTrue(
            actual.matches(uuidRegex),
            "Random user id should be generated and have UUID format"
        );
    }

    @Test
    public void test_generateRandomUserId_checkRandomUserIdFormat_1() {
        String actual = logService.generateRandomUserId();

        assertTrue(
            actual.matches(uuidRegex),
            "Random user id should have UUID format"
        );
    }

    @Test
    public void test_generateBusinessId_shouldBeCustomValue_1() {
        List<String> values = new ArrayList<String>();
        values.add("Dummy business id 1");
        values.add("Dummy business id 2");
        values.add("Dummy business id 3");

        String actual = logService.generateBusinessId(values);

        assertThat(
            "Business Id should be one of the custom values",
            values, 
            hasItem(actual)
        );
    }

    @Test
    public void test_generateBusinessId_checkRandomValueFormat_2() throws Exception {
        List<String> values = new ArrayList<String>();

        String actual = logService.generateBusinessId(values);

        assertTrue(
            actual.matches(uuidRegex),
            "Random business UUID should be generated and have correct format"
        );
    }

    @Test
    public void test_generateRandomBusinessId_checkRandomValueFormat_1() {
        String actual = logService.generateRandomBusinessId();

        assertTrue(
            actual.matches(uuidRegex),
            "Random buiness UUID should have correct format"
        );
    }

    @Test
    public void test_generateFilePath_shouldBeCustomValue_1() {
        List<String> values = new ArrayList<String>();
        values.add("C:\\LogGenerator\\log.json");

        String actual = logService.generateFilepath(values);

        assertThat(
            "File path sohuld be one of the custom values",
            values,
            hasItem(actual)
        );
    }

    @Test
    public void test_generateFilePath_checkRandomValueFormat_2() {
        List<String> values = new ArrayList<String>();

        String actual = logService.generateFilepath(values);

        assertTrue(
            actual.matches(windowsFilePathRegex) ||
            actual.matches(unixFilePathRegex),
            "Random file path should be generated and should have correct format "
            + actual
        );
    }

    @Test
    public void test_generateRandomFilePath_checkRandomValueFormat_1() {
        String actual = logService.generateRandomFilepath();

        assertTrue(
            actual.matches(windowsFilePathRegex) ||
            actual.matches(unixFilePathRegex),
            "Random file path should have correct format " + actual
        );
    }

    @Test
    public void test_generateFileSHA256_shouldBeCustomValue_1() {
        List<String> values = new ArrayList<String>();
        values.add("Dummy File SHA256 1");
        values.add("Dummy File SHA256 2");
        values.add("Dummy File SHA256 3");
        
        String actual = logService.generateFileSHA256(values);

        assertThat(
            "File SHA256 should be one of the custom values",
            values,
            hasItem(actual)
        );
    }

    @Test
    public void test_generateFileSHA256_checkRandomValueFormat_2() {
        List<String> values = new ArrayList<String>();

        String actual = logService.generateFileSHA256(values);

        assertTrue(
            actual.matches(uuidRegex),
            "Random UUID should be generated and have correct format for UUID"
        );
    }

    @Test
    public void test_generateRandomFileSHA256_checkRandomValueFormat_1() {
        String actual = logService.generateRandomFileSHA256();

        assertTrue(
            actual.matches(uuidRegex),
            "Random UUID should have correct format for UUID"
        );
    }

    @Test
    public void test_generateDisposition_shouldBeGreaterThan0_1() {
        List<Integer> values = new ArrayList<Integer>();

        int actual = logService.generateDisposition(values);

        assertTrue(actual > 0, "Disposition should be random value and between 1 and 4 inclusive");
    }

    @Test
    public void test_generateDisposition_shouldBeLessThan5_2() {
        List<Integer> values = new ArrayList<Integer>();

        int actual = logService.generateDisposition(values);
        assertTrue(actual < 5, "Disposition should be random value and between 1 and 4 inclusive");
    }

    @Test
    public void test_generateDisposition_shouldBeCustomValue_3() {
        List<Integer> values = new ArrayList<Integer>();
        values.add(0);
        values.add(50);
        values.add(100);

        int actual = logService.generateDisposition(values);

        assertThat(
            "Disposition should be a one of the custom values",
            values,
            hasItems(Integer.valueOf(actual))
        );
    }

    @Test
    public void test_generateRandomDisposition_shouldBeGreaterThan0_1() {
        int actual = logService.generateRandomDisposition();

        assertTrue(actual > 0, "Disposition should be between 1 and 4 inclusive");
    }

    @Test
    public void test_generateRandomDisposition_shouldBeLessThan5_2() {
        int actual = logService.generateRandomDisposition();

        assertTrue(actual < 5, "Disposition should be between 1 and 4 inclusive");
    }
}
