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
