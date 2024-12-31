/**
* Copyright (C) 2013-2024 Nanjing Pengyun Network Technology Co., Ltd.
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/ 

package py.monitorcommon.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.monitor.common.CounterName;
import py.monitor.common.PerformanceMessage;
import py.monitor.common.QueueManager;
import py.monitorcommon.manager.MonitorServerHelperShared;
import py.monitorcommon.websocketserver.WebSocketServer;

public class PerformanceMessageTask extends BaseTask {

  private static final Logger logger = LoggerFactory.getLogger(PerformanceMessageTask.class);
  private WebSocketServer webSocketServer;
  private Map<String, Long> lastPerformanceMessageTimeMap;

  public PerformanceMessageTask(QueueManager queueManager, WebSocketServer webSocketServer) {
    super(queueManager);
    this.webSocketServer = webSocketServer;
    lastPerformanceMessageTimeMap = new ConcurrentHashMap<>();
  }

  @Override
  public void startJob() {
    LinkedBlockingQueue<PerformanceMessage> performanceQueue = queueManager.getPerformanceQueue();
    while (!isThreadPoolExecutorStop) {
      final PerformanceMessage performanceMessage;
      try {
        performanceMessage = performanceQueue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      poolExecutor.execute(() -> {
        doWork(performanceMessage);
        logger.debug("runnableQueue size in performanceMessageTask is: {}", runnableQueue.size());
      });
    }
  }

  public void doWork(PerformanceMessage performanceMessage) {

    String key = performanceMessage.getSourceId() + performanceMessage.getCounterKey();

    if (lastPerformanceMessageTimeMap.get(key) == null) {
      sendToConsole(performanceMessage);
      lastPerformanceMessageTimeMap.put(key, performanceMessage.getStartTime());
    } else {
      if (lastPerformanceMessageTimeMap.get(key) < performanceMessage.getStartTime()) {
        sendToConsole(performanceMessage);
        lastPerformanceMessageTimeMap.put(key, performanceMessage.getStartTime());
      } else {
        logger
            .info("the performance is delayed, lastPerformanceTime: {}, currentPerformanceTime: {}",
                lastPerformanceMessageTimeMap.get(key), performanceMessage.getStartTime());
        return;
      }
    }
  }

  public void sendToConsole(PerformanceMessage performanceMessage) {
    switch (CounterName.valueOf(performanceMessage.getCounterKey())) {
      case VOLUME_READ_LATENCY:
      case VOLUME_WRITE_LATENCY:
      case STORAGEPOOL_READ_LATENCY:
      case STORAGEPOOL_WRITE_LATENCY:
      case SYSTEM_READ_LATENCY:
      case SYSTEM_WRITE_LATENCY:
        float counterValue = performanceMessage.getCounterValue();
        performanceMessage.setCounterValue(
            MonitorServerHelperShared.convertToFloat(counterValue, 1000, 2));
        break;
      case VOLUME_IO_BLOCK_SIZE:
      case STORAGEPOOL_IO_BLOCK_SIZE:
      case SYSTEM_IO_BLOCK_SIZE:
        counterValue = performanceMessage.getCounterValue();
        performanceMessage.setCounterValue(
            MonitorServerHelperShared.convertToFloat(counterValue, 1024, 2));
        break;
      default:
        break;
    }
    logger.info("send performance message to console : {}", performanceMessage);
    webSocketServer.write(performanceMessage);
  }
}
