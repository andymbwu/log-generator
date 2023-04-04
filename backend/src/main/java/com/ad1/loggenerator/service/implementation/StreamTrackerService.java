package com.ad1.loggenerator.service.implementation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ad1.loggenerator.model.JobStatus;
import com.ad1.loggenerator.model.LogMessage;
import com.ad1.loggenerator.model.StreamTracker;

import lombok.Data;

/**
 * Contains logic for checking whether to stop a stream job
 * and to send stream data for each stream job
 */
@Data
@Service
public class StreamTrackerService {

    /**
     * Seconds before timing out a stream job
     */
    private final long secondsTimeOut = 30;
    /**
     * Milliseconds to wait between sending data to frontend
     */
    private final long millsecondsPerMessage = 1000;
    /**
     * Object used to send messages to a the broker channel
     */
    @Autowired
    private SimpMessagingTemplate template;
    /**
     * HashMap of all active stream jobs
     */
    private Map<String, StreamTracker> activeJobsList = new ConcurrentHashMap<String, StreamTracker>();
    /**
     * HashMap of all completed stream jobs and active stream jobs
     */
    private Map<String, StreamTracker> historyJobsList = new ConcurrentHashMap<String, StreamTracker>();

    /**
     * Sends stream data to the front end, checks every stream job
     * and sets continueStreaming to false if lastPing+5s < current time.
     * Then removes all stream jobs with continueStreaming set to false
     */
    @Async("asyncTaskExecutor")
    public void checkLastPings() throws InterruptedException {

        if (activeJobsList.size() == 0) {
            return;
        }

        while (activeJobsList.size() > 0) {

            Thread.sleep(millsecondsPerMessage);

            for (String jobId : activeJobsList.keySet()) {
                StreamTracker job = activeJobsList.get(jobId);

                sendStreamData(job);

                // Optional: Cancel job if ping not recieved during secondsTimeOut
                /*
                 * if (job.getLastPing() + secondsTimeOut < System.currentTimeMillis() / 1000) {
                 * job.setStatus(JobStatus.CANCELLED);
                 * }
                 */

                // if job isn't active or finalizing, remove from active jobs
                if (job.getStatus() != JobStatus.ACTIVE && job.getStatus() != JobStatus.FINALIZING) {
                    setStreamJobToCompleted(job);
                }

            }

        }

    }

    /**
     * Sends log data for a stream job
     * 
     * @throws InterruptedException
     */
    public void sendStreamData(StreamTracker job) throws InterruptedException {

        if (activeJobsList.size() == 0) {
            return;
        }

        String destination = "/topic/job";

        if (job != null) {
            template.convertAndSend(destination + "/" + job.getJobId(),
                    new LogMessage(job.getStatus(), job.getLogCount(),
                            System.currentTimeMillis(), job.getStreamObjectURL()));
        }
    }

    /**
     * Utility method to process a stream job tracker as completed
     * 
     * @param job The stream job tracker that is completed
     */
    private void setStreamJobToCompleted(StreamTracker job) {
        activeJobsList.remove(job.getJobId());
        job.setEndTime(System.currentTimeMillis() / 1000);
    }

    /**
     * Add a new stream job to the historyJobsList and activeJobsList
     * 
     * @param streamTracker the new stream job tracker
     */
    public void addNewJob(StreamTracker streamTracker) {
        historyJobsList.put(streamTracker.getJobId(), streamTracker);
        activeJobsList.put(streamTracker.getJobId(), streamTracker);
    }

    /**
     * Stops a stream job. Sets status to FINALIZING
     * 
     * @param jobId
     * @return
     */
    public boolean stopStreamJob(String jobId) {
        StreamTracker streamTracker = activeJobsList.get(jobId);
        if (streamTracker == null) {
            return false;
        }

        // set job status to FINALIZING
        streamTracker.setStatus(JobStatus.FINALIZING);
        return true;
    }

    /**
     * Update the lastPing to continue a stream job
     */
    public boolean continueStreamJob(String jobId) {
        StreamTracker streamTracker = activeJobsList.get(jobId);
        if (streamTracker == null) {
            return false;
        }

        streamTracker.setLastPing(System.currentTimeMillis() / 1000);
        return true;
    }

    /**
     * Returns the number of active stream jobs
     * 
     * @return the number of jobs
     */
    public int getActiveJobsListSize() {
        return activeJobsList.size();
    }

    /**
     * Returns the number of all active and completed stream jobs
     * 
     * @return the number of all active and completed jobs
     */
    public int getHistoryJobsListSize() {
        return historyJobsList.size();
    }

    /**
     * Get the list of all completed stream jobs and active stream jobs
     * 
     * @return the list of all completed stream jobs and active stream jobs
     */
    public Map<String, StreamTracker> getHistoryJobsList() {
        return historyJobsList;
    }

    /**
     * Get a specific stream job tracker from the historyJobsList
     * 
     * @param jobId the id of the stream job tracker
     * @return the stream job tracker
     */
    public StreamTracker getStreamJobTracker(String jobId) {
        return historyJobsList.get(jobId);
    }

}
