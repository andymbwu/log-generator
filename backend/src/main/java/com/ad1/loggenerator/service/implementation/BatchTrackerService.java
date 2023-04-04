package com.ad1.loggenerator.service.implementation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ad1.loggenerator.model.BatchTracker;
import com.ad1.loggenerator.model.JobStatus;
import com.ad1.loggenerator.model.LogMessage;

import lombok.Data;

/**
 * Contains logic to send stream data for each batch job
 */
@Data
@Service
public class BatchTrackerService {

    /**
     * Milliseconds to wait between sending data to frontend
     */
    private final long millsecondsPerMessage = 1000;
    /**
     * Object used to send messages to the broker channel
     */
    @Autowired
    private SimpMessagingTemplate template;
    /**
     * HashMap of all active batch jobs
     */
    private Map<String, BatchTracker> activeJobsList = new ConcurrentHashMap<String, BatchTracker>();
    /**
     * HashMap of all completed batch jobs and active batch jobs
     */
    private Map<String, BatchTracker> historyJobsList = new ConcurrentHashMap<String, BatchTracker>();

    /**
     * Sends log data for every batch job in the jobs list. Then removes batch jobs
     * that have finished processing
     */
    @Async("asyncTaskExecutor")
    public void sendBatchData() throws InterruptedException {
        if (activeJobsList.size() == 0) {
            return;
        }

        BatchTracker job = null;
        String destination = "/topic/job";

        while (activeJobsList.size() > 0) {

            Thread.sleep(millsecondsPerMessage);

            for (String jobId : activeJobsList.keySet()) {
                job = activeJobsList.get(jobId);

                template.convertAndSend(destination + "/" + job.getJobId(),
                        new LogMessage(job.getStatus(), job.getLogCount(),
                                System.currentTimeMillis(), job.getBatchObjectURL()));

                // if job isn't active or finalizing, remove from active jobs
                if (job.getStatus() != JobStatus.ACTIVE && job.getStatus() != JobStatus.FINALIZING) {
                    setBatchJobToCompleted(job);
                }
            }
        }
    }

    /**
     * Utility method to process a batch job tracker as not active
     * 
     * @param job The batch job tracker that is completed
     */
    private void setBatchJobToCompleted(BatchTracker job) {
        activeJobsList.remove(job.getJobId());
        job.setEndTime(System.currentTimeMillis() / 1000);
    }

    /**
     * Stop a batch job right away
     * 
     * @param jobId the batch job id
     * @return
     */
    public boolean stopBatchJob(String jobId) {
        BatchTracker batchTracker = activeJobsList.get(jobId);
        if (batchTracker == null) {
            return false;
        }

        batchTracker.setStatus(JobStatus.CANCELLED);
        return true;
    }

    /**
     * Add a new batch job to the historyJobsList and activeJobsList
     * 
     * @param batchTracker the new batch job tracker
     */
    public void addNewJob(BatchTracker batchTracker) {
        historyJobsList.put(batchTracker.getJobId(), batchTracker);
        activeJobsList.put(batchTracker.getJobId(), batchTracker);
    }

    /**
     * Return the number of active batch jobs
     * 
     * @return the number of batch jobs
     */
    public int getActiveJobsListSize() {
        return activeJobsList.size();
    }

    /**
     * Return the number of completed batch jobs and active batch jobs
     * 
     * @return the number of completed and active batch jobs
     */
    public int getHistoryJobsListSize() {
        return historyJobsList.size();
    }

    /**
     * Get the list of all completed batch jobs and active batch jobs
     * 
     * @return the list of all completed batch jobs and active batch jobs
     */
    public Map<String, BatchTracker> getHistoryJobsList() {
        return historyJobsList;
    }

    /**
     * Get a specific batch job tracker from the historyJobsList
     * 
     * @param jobId the id of the batch job tracker
     * @return the batch job tracker
     */
    public BatchTracker getBatchJobTracker(String jobId) {
        return historyJobsList.get(jobId);
    }
}
