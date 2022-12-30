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
import py.monitor.common.AlertMessage;
import py.monitor.common.AlertRule;
import py.monitor.common.AlertRuleAndObjectKey;
import py.monitor.common.AlertTemplate;
import py.monitor.common.QueueManager;
import py.monitorcommon.manager.MonitorServerHelperShared;

public class AlertRecoveryByRuleTask extends BaseTask {

  private static final Logger logger = LoggerFactory.getLogger(AlertRecoveryByRuleTask.class);

  private Map<AlertRuleAndObjectKey, Long> mixAlertRecoveryMap;
  private Map<AlertRuleAndObjectKey, Long> mixAlertMap;
  private Map<String, AlertTemplate> alertTemplateMap;

  public AlertRecoveryByRuleTask(Map<String, AlertTemplate> alertTemplateMap,
      QueueManager queueManager) {
    super(queueManager);
    this.mixAlertRecoveryMap = new ConcurrentHashMap<>();
    this.alertTemplateMap = alertTemplateMap;
  }

  @Override
  public void startJob() {
    long lastTime = System.currentTimeMillis();
    long currentTime = 0;
    LinkedBlockingQueue<AlertMessage> alertRecoveryQueueByRule = queueManager
        .getAlertRecoveryQueueByRule();
    while (!isThreadPoolExecutorStop) {
      AlertMessage alertMessage;
      try {
        if (logger.isInfoEnabled()) {
          currentTime = System.currentTimeMillis();
          if (currentTime - lastTime > 60000) {
            lastTime = currentTime;
            logger.warn("queue length:recovery by rule task has message:{}",
                alertRecoveryQueueByRule.size());
            logger.warn("queue length:recovery by rule task pool queue size:{}. active count:{}",
                poolExecutor.getQueue().size(), poolExecutor.getActiveCount());
          }
        }
        alertMessage = alertRecoveryQueueByRule.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      poolExecutor.execute(() -> {
        doWork(alertMessage);
        logger.debug("runnableQueue size in recoveryRuleFilterTask is: {}", runnableQueue.size());
      });
    }
  }

  public void doWork(AlertMessage alertMessage) {
    logger
        .debug("alert message filter by recovery alert rule, plainAlertMessage : {}", alertMessage);
    String sourceId = alertMessage.getSourceId();
    String counterKey = alertMessage.getCounterKey();
    AlertRule alertRule = MonitorServerHelperShared
        .getAlertRule(alertTemplateMap, sourceId, counterKey);

    if (alertRule == null) {
      logger.error(
          "there exists no alert rule, maybe the alert template had been modified or removed ");
      return;
    }

    AlertRule parentAlertRule = MonitorServerHelperShared.getRootAlertRule(alertRule);
    logger.debug("ancestorAlertRule : {}", parentAlertRule);
    boolean isGenerateAlertMessage = MonitorServerHelperShared
        .isGenerateMixAlert(parentAlertRule, alertMessage, mixAlertRecoveryMap, mixAlertMap, true);
    logger.debug("is generate alert message : {}", isGenerateAlertMessage);

    if (isGenerateAlertMessage) {
      MonitorServerHelperShared
          .clearMixAlertMap(parentAlertRule, alertMessage.getSourceId(), mixAlertRecoveryMap);
      AlertMessage alertMessageByRule = MonitorServerHelperShared
          .buildMixAlertMessage(alertMessage, parentAlertRule,
              true);
      logger.debug("alertMessage : {}", alertMessageByRule);
      boolean offer = queueManager.getAlertRecoveryQueue().offer(alertMessageByRule);
      if (!offer) {
        logger.error("offer to alertRecoveryQueue failed, alertMessage is: {}", alertMessageByRule);
      }
    }
  }

  public void clearMixMap() {
    MonitorServerHelperShared.clearMixAlertMap(mixAlertRecoveryMap, alertTemplateMap);
  }

  public Map<AlertRuleAndObjectKey, Long> getMixAlertRecoveryMap() {
    return mixAlertRecoveryMap;
  }

  public void setMixAlertMap(Map<AlertRuleAndObjectKey, Long> mixAlertMap) {
    this.mixAlertMap = mixAlertMap;
  }
}
