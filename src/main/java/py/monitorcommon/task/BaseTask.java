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
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.monitor.common.QueueManager;

/**
 * xx.
 */
public abstract class BaseTask {

  public static final String EVENT_LOG_MESSAGE_TASK_NAME = "event-log-message-task";
  public static final String PERFORMANCE_MESSAGE_TASK_NAME = "performance-message-task";
  public static final String ALERT_MESSAGE_BY_RULE_TASK_NAME = "alert-rule-filter-task";
  public static final String ALERT_MESSAGE_BY_TIME_TASK_NAME = "alert-time-filter-task";
  public static final String ALERT_RECOVERY_TASK_NAME = "alert-recovery-task";
  public static final String ALERT_RECOVERY_BY_RULE_TASK_NAME = "alert-recovery-task";
  public static final String NET_SUB_HEALTH_TASK_NAME = "net-sub-health-task";
  private static final Logger logger = LoggerFactory.getLogger(BaseTask.class);
  protected QueueManager queueManager;
  protected volatile boolean isThreadPoolExecutorStop = false;
  protected ThreadPoolExecutor poolExecutor;
  protected LinkedBlockingQueue<Runnable> runnableQueue;
  private Thread thread;

  public BaseTask(QueueManager queueManager) {
    this.queueManager = queueManager;
  }

  public void start(String threadName) {
    thread = new Thread(threadName) {
      @Override
      public void run() {
        startJob();
      }
    };
    thread.start();
  }

  public abstract void startJob();

  public void stop() {
    isThreadPoolExecutorStop = true;
    if (thread != null) {
      thread.interrupt();
    }
    if (poolExecutor != null) {
      poolExecutor.shutdown();
    }
  }

  public void setPoolExecutor(ThreadPoolExecutor poolExecutor) {
    this.poolExecutor = poolExecutor;
  }

  public void setRunnableQueue(LinkedBlockingQueue<Runnable> runnableQueue) {
    this.runnableQueue = runnableQueue;
  }
}
