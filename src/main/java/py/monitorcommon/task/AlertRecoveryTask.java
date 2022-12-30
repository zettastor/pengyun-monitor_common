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

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.app.context.AppContext;
import py.instance.InstanceStatus;
import py.monitor.common.AlertClearType;
import py.monitor.common.AlertMessage;
import py.monitor.common.CounterName;
import py.monitor.common.QueueManager;
import py.monitorcommon.dao.AlertMessageDao;
import py.monitorcommon.manager.MemoryAlertMessageManager;
import py.monitorcommon.websocketserver.WebSocketServer;

public class AlertRecoveryTask extends BaseTask {

  private static final Logger logger = LoggerFactory.getLogger(AlertRecoveryTask.class);

  private AlertMessageDao alertMessageDao;
  private WebSocketServer webSocketServer;
  private AlertMessageFilterByTimeTask alertMessageFilterByTimeTask;
  private boolean needSendDto;
  private MemoryAlertMessageManager memoryAlertMessageManager;
  private AppContext appContext;

  public AlertRecoveryTask(QueueManager queueManager, AlertMessageDao alertMessageDao,
      WebSocketServer webSocketServer, MemoryAlertMessageManager memoryAlertMessageManager,
      AppContext appContext) {
    super(queueManager);
    this.alertMessageDao = alertMessageDao;
    this.webSocketServer = webSocketServer;
    this.memoryAlertMessageManager = memoryAlertMessageManager;
    this.appContext = appContext;
  }

  @Override
  public void startJob() {
    long lastTime = System.currentTimeMillis();
    long currentTime = 0;
    LinkedBlockingQueue<AlertMessage> alertQueue = queueManager.getAlertRecoveryQueue();
    while (!isThreadPoolExecutorStop) {
      final AlertMessage alertMessage;
      try {
        if (logger.isInfoEnabled()) {
          currentTime = System.currentTimeMillis();
          if (currentTime - lastTime > 60000) {
            lastTime = currentTime;
            logger.warn("queue length:recovery task has message:{}", alertQueue.size());
            logger.warn("queue length:recovery task pool queue size:{}. active count:{}",
                poolExecutor.getQueue().size(), poolExecutor.getActiveCount());
            logger.warn("memory alert message length:{}",
                memoryAlertMessageManager.memoryAlertLength());
          }
        }
        alertMessage = alertQueue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      poolExecutor.execute(() -> {
        doWork(alertMessage);
        logger.debug("runnableQueue size in alertRecoveryTask is: {}", runnableQueue.size());
      });
    }
  }

  private void doWork(AlertMessage alertMessage) {
    boolean needRecovery = false;
    if (memoryAlertMessageManager
        .containsMemoryAlertKey(alertMessage.getSourceId(), alertMessage.getCounterKey())) {
      needRecovery = true;
    } else if (alertMessage.isNetSubHealthAlert()) {
      if (memoryAlertMessageManager.containsMemoryAlertKey(
          alertMessage.getLatestEventDataInfo().getRemoteAddr(),
          CounterName.NET_SUB_HEALTH.name())) {
        needRecovery = true;
      }
    }

    if (!needRecovery) {
      logger.debug("memory alert message:{}", memoryAlertMessageManager.printMemoryAlert());
      logger.debug("current alert has no alert message, no need do recovery."
              + " sourceId:{}, sourceName:{}, counterKey:{}",
          alertMessage.getSourceId(), alertMessage.getSourceName(), alertMessage.getCounterKey());
      return;
    }

    logger.warn("need recovery alert:{}", alertMessage);

    List<AlertMessage> alertMessages = alertMessageDao
        .listAlertMessagesBySourceId(alertMessage.getSourceId());
    for (AlertMessage alertMessageInDb : alertMessages) {
      if (alertMessageInDb.getCounterKey().equals(alertMessage.getCounterKey())) {
        if (!alertMessageInDb.isAlertClear()) {
          logger.warn("clear alert message, alertMessageInDb is: {}, alertMesssage is: {}",
              alertMessageInDb,
              alertMessage);
          alertMessageFilterByTimeTask
              .removeLastAlertMessageKey(alertMessageInDb.getAlertMessageKey());

          alertMessageInDb.setAlertClear(true);
          alertMessageInDb.setAlertClearType(AlertClearType.AUTOMATIC.toString());
          long clearTime = System.currentTimeMillis();
          alertMessageInDb.setClearTime(clearTime);
          //The purpose of the loop is to solve the problem that the database cannot be inserted
          //just after the database is restarted
          alertMessage.setAlertClear(true);
          alertMessage.setClearTime(clearTime);
          alertMessage.setAlertLevel(alertMessageInDb.getAlertLevel());
          int times = 10;
          while (times-- > 0) {
            try {
              if (appContext.getStatus() == InstanceStatus.HEALTHY) {
                alertMessageDao.saveOrUpdateAlertMessage(alertMessageInDb);
              } else {
                // if suspend, cannot recovery directly, need check alert time
                alertMessageDao.saveAlertMessageWithCheck(alertMessageInDb);
              }

              // remove alert message from memory
              memoryAlertMessageManager
                  .removeMemoryAlert(alertMessage.getSourceId(), alertMessage.getCounterKey());

              try {
                webSocketServer.write(alertMessage);
              } catch (Exception e) {
                logger.warn("caught an exception when send alertMessage by websocket: ", e);
              }

              try {
                alertMessageFilterByTimeTask.sendToNms(alertMessage);
              } catch (Exception e) {
                logger.warn("caught an exception when trap alertMessage by snmp : ", e);
              }

              if (needSendDto) {
                try {
                  alertMessageFilterByTimeTask.sendToDto(alertMessage, "1");
                } catch (Exception e) {
                  logger.warn(
                      "caught an exception when trap alertMessage recovery when send to DTO: ",
                      e);
                }
              }

              try {
                alertMessageFilterByTimeTask.sendToSmtp(alertMessage);
              } catch (Exception e) {
                logger.warn("caught an exception when send alertMessage by email: ", e);
              }
              logger.warn(
                  "clear alertMessage automatic, alertMessage id is: {}, sourceId is: {}, "
                      + "recovery level is: {}", alertMessage.getId(), alertMessage.getSourceId(),
                  alertMessage.getAlertLevel());
              break;
            } catch (Exception e) {
              logger.warn("recovery retry {} times . caught an exception:", times, e);
              try {
                Thread.sleep(5000);
              } catch (InterruptedException e1) {
                logger.warn("caught an exception", e1);
              }
            }
          }

        }   // if (!alertMessageInDb.isAlertClear()) {
      }   // if (alertMessageInDb.getCounterKey().equals(alertMessage.getCounterKey())) {
    }   // for (AlertMessage alertMessageInDb : alertMessages) {

    //save recovery alert message, net sub health always say recovery
    if (alertMessage.isNetSubHealthAlert()) {
      alertMessage.setAlertClear(true);
      logger.warn("sub net health may be recovery, alertMessage:{}", alertMessage);
      queueManager.getNetSubHealthQueue().offer(alertMessage);
    }

  }

  public void setAlertMessageFilterByTimeTask(
      AlertMessageFilterByTimeTask alertMessageFilterByTimeTask) {
    this.alertMessageFilterByTimeTask = alertMessageFilterByTimeTask;
  }

  public void setNeedSendDto(boolean needSendDto) {
    this.needSendDto = needSendDto;
  }
}
