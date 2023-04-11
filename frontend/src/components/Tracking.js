import { DownloadIcon } from '@chakra-ui/icons';
import {
  Box,
  Button,
  HStack,
  Link,
  Text,
  Tooltip,
  useBoolean,
  VStack,
} from '@chakra-ui/react';
import { Client } from '@stomp/stompjs';
import { React, useEffect, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import Chart from './Chart';

// converts seconds to a custom time string
const secondsToTimeString = totalSeconds => {
  const hours = Math.floor(totalSeconds / 3600);
  totalSeconds %= 3600;
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = Math.floor(totalSeconds % 60);
  let time = '';

  if (Number(hours) > 0) {
    time += hours + 'hr ';
  }
  if (Number(minutes) > 0) {
    time += minutes + 'min ';
  }
  time += seconds + 'sec';

  return time;
};

function Tracking({ jobID, setJobID, startTime, batchMode, batchSize }) {
  const [loading, setLoading] = useBoolean(false);
  const [running, setRunning] = useBoolean(false);
  const [uptime, setUptime] = useState(0);
  const [data, setData] = useState({
    timeStamp: [],
    logRate: [],
  });
  const [logsCreated, setlogsCreated] = useState(0);
  const lastResponseRef = useRef({ time: 0, response: null });
  const [showChart, setShowChart] = useBoolean(startTime ? true : false);
  const [lastJob, setLastJob] = useState({
    id: null,
    status: 'Standby',
    url: null,
  });

  //conenct to socket once there is a new jobID
  useEffect(() => {
    if (!jobID) {
      return;
    }

    // reset values on new jobID
    if (startTime) {
      setUptime((Date.now() - startTime) / 1000);
    } else {
      setUptime(0);
    }
    setlogsCreated(0);
    lastResponseRef.current = { time: Date.now(), response: null };
    setData({
      timeStamp: [],
      logRate: [],
    });
    setLastJob({ id: jobID, status: 'Active', url: null });

    // set running on
    setRunning.on();

    // create new socket using STOMP.js and SockJS
    const stompClient = new Client({
      webSocketFactory: () => new SockJS(process.env.REACT_APP_SOCKET_URL),
    });

    // on connect subscribe to the jobID
    stompClient.onConnect = function (frame) {
      stompClient.subscribe('/topic/job/' + jobID, function (response) {
        // parse response
        response = JSON.parse(response.body);

        // update logsCreated
        setlogsCreated(response.logLineCount);

        // update chart data
        if (!lastResponseRef.current.response) {
          // first response
          if (!startTime) {
            // set initial data
            if (response.logLineCount === 0) {
              // if no logs generated yet, initial log rate of 0
              setData({
                timeStamp: [response.timeStamp],
                logRate: [0],
              });
            } else {
              // if some logs generated, initial log rate of 0, then number generated
              setData({
                timeStamp: [response.timeStamp - 1000, response.timeStamp],
                logRate: [0, response.logLineCount],
              });
            }
          }

          // show chart once first data recieved
          setShowChart.on();
        } else {
          // ongoing response
          const logRate = Math.round(
            (response.logLineCount -
              lastResponseRef.current.response.logLineCount) /
              ((response.timeStamp -
                lastResponseRef.current.response.timeStamp) /
                1000)
          );

          setData(prev => ({
            timeStamp: [...prev.timeStamp, response.timeStamp],
            logRate: [...prev.logRate, logRate],
          }));
        }

        // if job not active, update status
        if (response.status !== 'ACTIVE') {
          setLastJob({
            id: jobID,
            status:
              response.status.charAt(0).toUpperCase() +
              response.status.slice(1).toLowerCase(),
            url: response.url,
          });

          // if job isn't Active or Finalizing, stop job
          if (response.status !== 'FINALIZING') {
            setRunning.off();
            setLoading.off();
            setJobID(null);
          }
        }

        // update last response ref
        lastResponseRef.current.response = response;
        lastResponseRef.current.time = Date.now();
      });
    };

    // connect to socket
    stompClient.activate();

    // cleanup
    return () => {
      // deactivate socket
      stompClient.deactivate();
    };
  }, [
    jobID,
    startTime,
    setJobID,
    setRunning,
    setData,
    setShowChart,
    setLoading,
  ]);

  // uptime and active intervals while job is running
  useEffect(() => {
    let timerInterval;
    let activeInterval;
    if (running) {
      // interval to update timer
      timerInterval = setInterval(() => {
        setUptime(prevTime => prevTime + 1);
      }, 1000);

      // interval to timeout socket
      activeInterval = setInterval(() => {
        if (Number(Date.now() - lastResponseRef.current.time) > 2500) {
          // time out job
          setRunning.off();
          setLoading.off();
          setLastJob({
            id: jobID,
            status: 'Timed out',
            url: null,
          });
          setJobID(null);
        }
      }, 1000);
    } else if (!running) {
      clearInterval(timerInterval);
      clearInterval(activeInterval);
    }

    // cleanup
    return () => {
      clearInterval(timerInterval);
      clearInterval(activeInterval);
    };
  }, [running, setRunning, setJobID, jobID, setLastJob, setLoading]);

  // handle cancel button
  const handleCancel = () => {
    setLoading.on();

    let url = process.env.REACT_APP_API_URL + 'generate/';
    if (batchMode) {
      url += 'batch/stop/' + jobID;
    } else {
      url += 'stream/stop/' + jobID;
    }
    fetch(url, {
      method: 'POST',
    })
      .then(response => {
        if (response.ok) {
          return response.text();
        }

        throw Error("Couldn't cancel at this time, please try again later");
      })
      .then(data => {
        setRunning.off();
      })
      .catch(err => {
        setLoading.off();
        alert(err);
      });
  };

  return (
    <div>
      <HStack justify="space-between">
        <VStack spacing="0.1em" align="start">
          <Text>{lastJob.status}</Text>
          {lastJob.id && <Text>Job id: {lastJob.id}</Text>}

          <Text>Uptime: {secondsToTimeString(uptime)}</Text>
          {batchSize && <Text>Batch Size: {batchSize}</Text>}
          <HStack>
            <Text>Logs created: {logsCreated}</Text>
            {lastJob.url && (
              <Link href={lastJob.url} isExternal>
                <Tooltip label="Download">
                  <DownloadIcon mx="2px" />
                </Tooltip>
              </Link>
            )}
          </HStack>
        </VStack>
        <Box pr="12%">
          <Button
            type="submit"
            colorScheme="red"
            onClick={handleCancel}
            isLoading={loading}
            isDisabled={!jobID}
          >
            Cancel
          </Button>
        </Box>
      </HStack>
      {showChart && (
        <Box w="100%" pt="1em" ml="-1em">
          <Chart data={data} />
        </Box>
      )}
    </div>
  );
}

export default Tracking;
