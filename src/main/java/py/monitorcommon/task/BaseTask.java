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
