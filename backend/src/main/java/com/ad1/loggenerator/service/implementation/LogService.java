package com.ad1.loggenerator.service.implementation;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.ad1.loggenerator.model.CustomLog;
import com.ad1.loggenerator.model.FieldSettings;
import com.ad1.loggenerator.model.LogModel;
import com.ad1.loggenerator.model.SelectionModel;

import lombok.Data;

/**
 * Contains logic for generating outputs specific to the user defined parameters
 */
@Data
@Service
public class LogService {

    /**
     * Generates each log line as defined by the parameters
     * 
     * @param selectionModel defines all the parameters to be included in the log
     *                       lines as per the user
     * @return a single log line in JSON format
     */
    public JSONObject generateLogLine(SelectionModel selectionModel, Set<String> masterFieldList) {

        // generated fields settings
        FieldSettings fieldSettings = selectionModel.getFieldSettings();

        // Create logline objects
        LogModel logLine = new LogModel();
        JSONObject logLineJSON = new JSONObject();

        List<CustomLog> customLogs = selectionModel.getCustomLogs();
        // choose a random custom log based on frequency
        CustomLog customLog = chooseCustomLog(customLogs);

        if (customLog != null) {
            addCustomLogFields(logLineJSON, customLog);
        }

        // Generate log line data
        if (fieldSettings.getTimeStamp().getInclude() && !logLineJSON.containsKey("timeStamp")) {
            List<Long> values = fieldSettings.getTimeStamp().getValues();
            logLine.setTimeStamp(generateTimeStamp(values));
            logLineJSON.put("timeStamp", logLine.getTimeStamp());
        }
        if (fieldSettings.getProcessingTime().getInclude() && !logLineJSON.containsKey("processingTime")) {
            List<Long> values = fieldSettings.getProcessingTime().getValues();
            logLine.setProcessingTime(generateProcessingTime(values));
            logLineJSON.put("processingTime", logLine.getProcessingTime());
        }
        if (fieldSettings.getCurrentUserID().getInclude() && !logLineJSON.containsKey("currentUserID")) {
            List<String> values = fieldSettings.getCurrentUserID().getValues();
            logLine.setCurrentUserID(generateUserId(values));
            logLineJSON.put("currentUserID", logLine.getCurrentUserID());
        }
        if (fieldSettings.getBusinessGUID().getInclude() && !logLineJSON.containsKey("businessGUID")) {
            List<String> values = fieldSettings.getBusinessGUID().getValues();
            logLine.setBusinessGUID(generateBusinessId(values));
            logLineJSON.put("businessGUID", logLine.getBusinessGUID());
        }
        if (fieldSettings.getPathToFile().getInclude() && !logLineJSON.containsKey("pathToFile")) {
            List<String> values = fieldSettings.getPathToFile().getValues();
            logLine.setPathToFile(generateFilepath(values));
            logLineJSON.put("pathToFile", logLine.getPathToFile().replace("\\\\", "\\"));
        }
        if (fieldSettings.getFileSHA256().getInclude() && !logLineJSON.containsKey("fileSHA256")) {
            List<String> values = fieldSettings.getFileSHA256().getValues();
            logLine.setFileSHA256(generateFileSHA256(values));
            logLineJSON.put("fileSHA256", logLine.getFileSHA256());
        }
        if (fieldSettings.getDisposition().getInclude() && !logLineJSON.containsKey("disposition")) {
            List<Integer> values = fieldSettings.getDisposition().getValues();
            logLine.setDisposition(generateDisposition(values)); // 1 = Clean, 2 = Suspicious, 3 = Malicious, 4 = Unknown
            logLineJSON.put("disposition", logLine.getDisposition());
        }

        addMasterFieldList(logLineJSON, masterFieldList);

        return logLineJSON;
    }

    public void addMasterFieldList(JSONObject logLineJSON, Set<String> masterFieldList) {

        for (String field: masterFieldList) {
            if (!logLineJSON.containsKey(field)) {
                logLineJSON.put(field, null);
            }
        }
    }

    /**
     * Utility function to preprocess custom logs by removing fields that
     * should not be included in the custom logs and then modifying the
     * custom logs to all have the same fields
     * 
     * @param customLogs
     * @param selectionModel
     */
    public void preProcessCustomLogs(List<CustomLog> customLogs, SelectionModel selectionModel) {
        removeExcludedFields(customLogs, selectionModel);
    }

    /**
     * Removes the fields that should not be included in the custom logs
     * 
     * @param customLogs
     * @param selectionModel
     */
    public void removeExcludedFields(List<CustomLog> customLogs, SelectionModel selectionModel) {

        FieldSettings fieldSettings = selectionModel.getFieldSettings();

        if (customLogs == null) {
            return;
        }

        for (CustomLog customLog: customLogs) {
            if (customLog.getFields().containsKey("timeStamp")
                        && !fieldSettings.getTimeStamp().getInclude()) {
                customLog.getFields().remove("timeStamp");
            }
            if (customLog.getFields().containsKey("processingTime")
                        && !fieldSettings.getProcessingTime().getInclude()) {
                customLog.getFields().remove("processingTime");
            }
            if (customLog.getFields().containsKey("currentUserID")
                        && !fieldSettings.getCurrentUserID().getInclude()) {
                customLog.getFields().remove("currentUserID");
            }
            if (customLog.getFields().containsKey("businessGUID")
                        && !fieldSettings.getBusinessGUID().getInclude()) {
                customLog.getFields().remove("businessGUID");
            }
            if (customLog.getFields().containsKey("pathToFile")
                        && !fieldSettings.getPathToFile().getInclude()) {
                customLog.getFields().remove("pathToFile");
            }
            if (customLog.getFields().containsKey("fileSHA256")
                        && !fieldSettings.getFileSHA256().getInclude()) {
                customLog.getFields().remove("fileSHA256");
            }
            if (customLog.getFields().containsKey("disposition")
                        && !fieldSettings.getDisposition().getInclude()) {
                customLog.getFields().remove("disposition");
            }
        }
    }

    /**
     * Utility function to align all custom logs to have the same fields
     * 
     * @param customLogs
     */
    public Set<String> getMasterFieldsList(List<CustomLog> customLogs) {

        if (customLogs == null) {
            return new HashSet<String>();
        }
        
        // Contains a set of all fields for all custom logs
        Set<String> masterFieldList = new HashSet<>();

        // Add each unique custom log field to the master list
        for (CustomLog customLog : customLogs) {
            if (customLog == null) {
                continue;
            }

            Map<String, Object> fields = customLog.getFields();

            if (fields == null) {
                continue;
            }
            
            Set<String> fieldsList = fields.keySet();
            masterFieldList.addAll(fieldsList);
        }

        return masterFieldList;
    }

    /**
     * This method takes the fields in the custom log and adds them to the log
     * line generated. If the custom log has a field that is part of our model,
     * it will overwrite the randomly generated value
     * 
     * @param logLineJSON
     * @param customLog
     */
    public void addCustomLogFields(JSONObject logLineJSON, CustomLog customLog) {

        if (customLog == null) {
            return;
        }

        if (logLineJSON == null) {
            throw new IllegalArgumentException("logLineJSON is null");
        }

        for (String key: customLog.getFields().keySet()) {
            logLineJSON.put(key, customLog.getFields().get(key));
        }

    }

    /**
     * Randomly choose a custom log to generate. The frequencies in the List 
     * must total <= 1.00. This method also returns null when a custom log
     * is not selected and a random log should be generated
     * 
     * @param customLogs
     * @return
     */
    public CustomLog chooseCustomLog(List<CustomLog> customLogs) {
        
        if (customLogs == null) {
            return null;
        }

        // get a random value
        Random random = new Random();
        double randomDouble = random.nextDouble(1);

        double lower = 0;
        double upper = 0;
        for (CustomLog customLog: customLogs) {

            if (customLog == null) {
                continue;
            }

            upper += customLog.getFrequency();
            
            // return the custom log chosen
            if (randomDouble <= upper && randomDouble > lower) {
                return customLog;
            }
            lower = upper;
        }

        // return null if no custom log chosen
        return null;
    }

    /**
     * Utility method to generate a timestamp
     * 
     * @return a timestamp
     */
    public long generateTimeStamp(List<Long> values) {
        
        if (values.size() == 0) {
            // if no value provided, generate random timestamp
            return generateRandomTimeStamp();
        }

        // if value provided, select one at random
        Random random = new Random();
        int randomIndex = random.nextInt(values.size());
        return values.get(randomIndex);
    }

    /**
     * Utility method to generate a random timestamp
     * 
     * @return a timestamp
     */
    public long generateRandomTimeStamp() {
        return (long) ((System.currentTimeMillis() / 1000) * Math.random());
    }

    /**
     * Utility method to generate processing time
     * 
     * @return processing time in seconds
     */
    public long generateProcessingTime(List<Long> values) {

        if (values.size() == 0) {
            // if no value provided, generate random processing time
            return generateRandomProcessingTime();
        }

        // if value provided, select one at random
        Random random = new Random();
        int randomIndex = random.nextInt(values.size());
        return values.get(randomIndex);
    }

    /**
     * Utility method to generate a random processing time
     * 
     * @return random processing time in seconds
     */
    public long generateRandomProcessingTime() {
        return (long) (Math.random() * 1000);
    }

    /**
     * Utility method to generate a user ID
     * 
     * @return a unique id a user
     */
    public String generateUserId(List<String> values) {

        if (values.size() == 0) {
            // if no value provided, generate random user id
            return generateRandomUserId();
        }

        // if value provided, select one at random
        Random random = new Random();
        int randomIndex = random.nextInt(values.size());
        return values.get(randomIndex);
    }

    /**
     * Utility method to generate a random user ID
     * 
     * @return a random unique id a user
     */
    public String generateRandomUserId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Utility method to generate a business ID
     * 
     * @return a unique id for a business
     */
    public String generateBusinessId(List<String> values) {

        if (values.size() == 0) {
            // if no value provided, generate random business id
            return generateRandomBusinessId();
        }

        // if value provided, select one at random
        Random random = new Random();
        int randomIndex = random.nextInt(values.size());
        return values.get(randomIndex);
    }

    /**
     * Utility method to generate a random business ID
     * 
     * @return a random unique id for a business
     */
    public String generateRandomBusinessId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Utility method to generate a filepath
     * 
     * @return a random filepath
     */
    public String generateFilepath(List<String> values) {

        if (values.size() == 0) {
            // if no value provided, generate random file path
            return generateRandomFilepath();
        }

        // if value provided, select one at random
        Random random = new Random();
        int randomIndex = random.nextInt(values.size());
        return values.get(randomIndex);
    }

    /**
     * Utility method to generate a random filepath
     * 
     * @return a random filepath
     */
    public String generateRandomFilepath() {
        // random information
        String[] folders = { "C:\\Program Files", "C:\\Windows", "C:\\Program Files (x86)",
                "C:\\Program Files (x86)\\Common Files", "/tmp", "/home" };
        String[] ext = { ".pdf", ".xlsx", ".csv", ".txt", ".json", ".sys", ".docx", ".jpg", ".zip" };

        // get random path to file
        Random random = new Random();
        int index = random.nextInt(folders.length);
        String pathToFile = folders[index];

        // get random filename and extension
        index = random.nextInt(ext.length);
        String filename = UUID.randomUUID().toString() + ext[index];

        // combine and return
        File file = new File(pathToFile, filename);
        return file.getAbsolutePath();
    }

    /**
     * Utility method to generate a file SHA256
     * 
     * @return a file SHA256
     */
    public String generateFileSHA256(List<String> values) {

        if (values.size() == 0) {
            // if no value provided, generate random file sha256
            return generateRandomFileSHA256();
        }

        // if value provided, select one at random
        Random random = new Random();
        int randomIndex = random.nextInt(values.size());
        return values.get(randomIndex);
    }

    /**
     * Utility method to generate a random file SHA256
     * 
     * @return a random file SHA256
     */
    public String generateRandomFileSHA256() {
        return UUID.randomUUID().toString();
    }

    /**
     * Utility method to generate disposition
     * 
     * @return a disposition
     */
    public int generateDisposition(List<Integer> values) {

        if (values.size() == 0) {
            // if no value provided, generate random disposition
            return generateRandomDisposition();
        }

        // if value provided, select one at random
        Random random = new Random();
        int randomIndex = random.nextInt(values.size());
        return values.get(randomIndex);
    }

    /**
     * Utility method to generate a random disposition
     * 
     * @return a random disposition
     */
    public int generateRandomDisposition() {
        return (int) (Math.random() * ((4 - 1) + 1) + 1);
    }

}