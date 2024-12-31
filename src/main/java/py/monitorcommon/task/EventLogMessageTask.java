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

import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.monitor.common.PerformanceMessageHistory;
import py.monitor.common.QueueManager;
import py.monitorcommon.dao.EventLogMessageDao;

public class EventLogMessageTask extends BaseTask {

  private static final Logger logger = LoggerFactory.getLogger(EventLogMessageTask.class);
  private EventLogMessageDao eventLogMessageDao;

  public EventLogMessageTask(QueueManager queueManager, EventLogMessageDao eventLogMessageDao) {
    super(queueManager);
    this.eventLogMessageDao = eventLogMessageDao;
  }

  @Override
  public void startJob() {
    LinkedBlockingQueue<PerformanceMessageHistory> eventLogQueue = queueManager.getEventLogQueue();
    while (!isThreadPoolExecutorStop) {
      final PerformanceMessageHistory performanceMessageHistory;
      try {
        performanceMessageHistory = eventLogQueue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      poolExecutor.execute(() -> {
        doWork(performanceMessageHistory);
        logger.debug("runnableQueue size in eventLogMessage is: {}", runnableQueue.size());
      });
    }
  }

  public void doWork(PerformanceMessageHistory performanceMessageHistory) {
    logger.info("save event log message to five_minute table: {}", performanceMessageHistory);
    eventLogMessageDao.saveEventLogMessage(performanceMessageHistory);
  }
}
