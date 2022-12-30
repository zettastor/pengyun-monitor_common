/*
 * Copyright (c) 2022. PengYunNetWork
 *
 * This program is free software: you can use, redistribute, and/or modify it
 * under the terms of the GNU Affero General Public License, version 3 or later ("AGPL"),
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  You should have received a copy of the GNU Affero General Public License along with
 *  this program. If not, see <http://www.gnu.org/licenses/>.
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
