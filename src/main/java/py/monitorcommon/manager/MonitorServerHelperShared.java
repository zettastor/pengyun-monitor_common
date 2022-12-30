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

package py.monitorcommon.manager;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.exception.IllegalParameterException;
import py.monitor.common.AlertLevel;
import py.monitor.common.AlertMessage;
import py.monitor.common.AlertRule;
import py.monitor.common.AlertRuleAndObjectKey;
import py.monitor.common.AlertRuleException;
import py.monitor.common.AlertTemplate;
import py.monitor.common.AlertType;
import py.monitor.common.CounterName;
import py.monitor.common.EventDataInfo;
import py.monitor.common.EventLogCompressed;
import py.monitor.common.LogicOperator;
import py.monitor.common.MonitorObjectEnum;
import py.monitor.common.OperationName;
import py.monitor.common.PerformanceMessage;
import py.monitor.common.PerformanceMessageHistory;
import py.monitor.common.RelationOperator;
import py.monitor.common.UserDefineName;
import py.monitor.utils.MonitorServerUtils;
import py.monitorcommon.dao.AlertMessageDao;

public class MonitorServerHelperShared {

  public static final String Unknown = "unknown";
  public static final long ALERT_TRIGGER = 0;
  public static final long ALERT_RECOVERY = 1;
  protected static final Logger logger = LoggerFactory.getLogger(MonitorServerHelperShared.class);

  public static PerformanceMessage buildPerformanceMessageFrom(EventDataInfo eventDataInfo,
      String counterKey) {
    String sourceId = getSourceKey(eventDataInfo, counterKey);
    if (sourceId == null) {
      logger.warn("sourceId is null, eventDataInfo is: {}", eventDataInfo);
      return null;
    }
    PerformanceMessage performanceMessage = new PerformanceMessage();
    performanceMessage.setStartTime(eventDataInfo.getStartTime());
    performanceMessage.setSourceId(sourceId);
    performanceMessage.setCounterKey(counterKey);
    performanceMessage.setCounterValue(eventDataInfo.getCounter(counterKey));
    return performanceMessage;
  }

  public static String getSourceKey(EventDataInfo eventDataInfo, String counterKey) {
    switch (OperationName.valueOf(eventDataInfo.getOperation())) {
      case Equipment:
        return eventDataInfo.getRemoteAddr();
      case Volume:
        return eventDataInfo.getNameValues().get(UserDefineName.VolumeID.toString());
      case Driver:
        return eventDataInfo.getNameValues().get(UserDefineName.VolumeID.toString());
      case CsiVolume:
        return eventDataInfo.getNameValues().get(UserDefineName.VolumeName.toString());
      case StoragePool:
        return eventDataInfo.getNameValues().get(UserDefineName.StoragePoolID.toString());
      case NETCARD:
        return eventDataInfo.getNameValues().get(UserDefineName.NetCardID.toString());
      case SERVICE:
        String name = eventDataInfo
            .getNameValues().get(UserDefineName.ServiceName.toString());
        String serviceName = "";
        if (!StringUtils.isEmpty(name)) {
          serviceName = ":" + name;
        }
        return eventDataInfo.getNameValues().get(UserDefineName.ServiceIP.toString()) + serviceName;
      case ServerNode:
        return eventDataInfo.getNameValues().get(UserDefineName.ServerNodeIp.toString());
      case Disk:
        return eventDataInfo.getRemoteAddr() + ":" + eventDataInfo.getNameValues()
            .get(UserDefineName.ArchiveName.toString());
      case Disk_Space:
        return eventDataInfo.getNameValues().get(UserDefineName.DiskHost.toString()) + ":"
            + eventDataInfo.getNameValues().get(UserDefineName.DiskName.toString());
      case DiskPerformance:
        return eventDataInfo.getRemoteAddr() + ":" + eventDataInfo.getNameValues()
            .get(UserDefineName.DiskName.toString());
      case DatanodeIo:
      case DataNode:
        if (counterKey.equals(CounterName.DATANODE_REQUEST_QUEUE_LENGTH.toString())) {
          return eventDataInfo.getRemoteAddr() + ":" + OperationName.DataNode.toString() + ":"
              + eventDataInfo.getNameValues().get(UserDefineName.QueueName.toString());
        }
        return eventDataInfo.getRemoteAddr() + ":" + OperationName.DataNode.toString();
      case SYSTEM:
        return eventDataInfo.getOperation();
      case NETWORK_STATUS: {
        // if remote addr contains "UNKNOWN REMOTE ADDRESS",then filtered.
        // For example, when you pull out the Gigabit network cable
        logger.warn("eventDataInfo.getRemoteAddr(): {}", eventDataInfo.getRemoteAddr());
        if (eventDataInfo.getRemoteAddr().contains("UNKNOWN REMOTE ADDRESS")) {
          return null;
        } else {
          return eventDataInfo.getRemoteAddr() + ":" + eventDataInfo.getNameValues()
              .get(UserDefineName.NetCardName.toString());
        }
      }
      case PYD:
      case ISCSI:
        return eventDataInfo.getRemoteAddr() + ":" + eventDataInfo.getOperation();
      case NETWORK: {
        if (counterKey.equals(CounterName.NETWORK_DROPPED.toString())
            || counterKey.equals(CounterName.NETCARD_DELAY.toString())) {
          String pingSrcIp = eventDataInfo.getNameValues()
              .get(UserDefineName.PingSourceIp.toString());
          return pingSrcIp;
          //                        String netCardName = eventDataInfo.getNameValues()
          //                        .get(UserDefineName.NetCardName.toString());
          //                        if (Unknown.equals(netCardName)) {
          //                            return pingSrcIp;
          //                        } else {
          //                            return pingSrcIp + ":" + netCardName;
          //                        }
        } else if (counterKey.equals(CounterName.NETWORK_CONGESTION.toString())) {
          String pingSrcIp = eventDataInfo.getNameValues()
              .get(UserDefineName.PingSourceIp.toString());
          return pingSrcIp;
        } else {
          return eventDataInfo.getRemoteAddr();
        }
      }
      default:
        return null;
    }
  }

  public static AlertRule getAlertRule(Map<String, AlertTemplate> alertTemplateMap,
      String sourceKey,
      String counterKey) {
    AlertTemplate alertTemplate;

    if (sourceKey != null && alertTemplateMap.containsKey(sourceKey)) {
      alertTemplate = alertTemplateMap.get(sourceKey);
    } else {
      alertTemplate = alertTemplateMap.get("defaultSourceId");
    }
    return alertTemplate.getAlertRuleMap().get(counterKey);
  }

  public static AlertMessage buildThresholdAlertMessage(String sourceId, CounterName counterKey,
      EventDataInfo eventDataInfo, AlertRule alertRule, String alertLevel) {
    AlertMessage alertMessage = new AlertMessage();
    alertMessage.setSourceId(sourceId);
    alertMessage.setAlertDescription(alertRule.getDescription());
    alertMessage.setAlertRuleName(alertRule.getName());
    alertMessage.setAlertAcknowledge(false);
    alertMessage.setFirstAlertTime(eventDataInfo.getStartTime());
    alertMessage.setLastAlertTime(eventDataInfo.getStartTime());
    alertMessage.setAlertLevel(alertLevel);
    alertMessage
        .setLastActualValue(String.valueOf(eventDataInfo.getCounter(counterKey.toString())));

    alertMessage.setCounterKey(alertRule.getCounterKey());
    logger.info("counterKey's operationName is {}, counterKey: {}", counterKey.getOperationName(),
        counterKey);
    switch (counterKey.getOperationName()) {
      case Equipment:
      case Disk:
      case Disk_Space:
      case NETWORK:
        alertMessage.setSourceName(sourceId);
        alertMessage.setAlertType(AlertType.EQUIPMENT.name());
        break;
      case Volume:
        alertMessage
            .setSourceName(eventDataInfo.getNameValues().get(UserDefineName.VolumeName.toString()));
        alertMessage.setAlertType(AlertType.CONSUMER.name());
        break;
      case Driver:
        alertMessage
            .setSourceName(eventDataInfo.getNameValues().get(UserDefineName.VolumeName.toString()));
        alertMessage.setAlertType(AlertType.CONSUMER.name());
        break;
      case CsiVolume:
        alertMessage
            .setSourceName(eventDataInfo.getNameValues().get(UserDefineName.VolumeName.toString()));
        alertMessage.setAlertType(AlertType.CONSUMER.name());
        break;
      case StoragePool:
        alertMessage.setSourceName(
            eventDataInfo.getNameValues().get(UserDefineName.StoragePoolName.toString()));
        alertMessage.setAlertType(AlertType.CONSUMER.name());
        break;
      case NETCARD:
        alertMessage.setSourceName(
            eventDataInfo.getNameValues().get(UserDefineName.NetCardIP.toString()) + ":"
                + eventDataInfo
                .getNameValues().get(UserDefineName.NetCardName.toString()));
        alertMessage.setAlertType(AlertType.EQUIPMENT.name());
        break;
      case DatanodeIo:
      case DataNode:
      case SERVICE:
        alertMessage.setSourceName(sourceId);
        alertMessage.setAlertType(AlertType.CONSUMER.name());
        break;
      default:
        logger.warn("counterKey's operationName is unknow, counterKey: {}", counterKey);
        return null;
    }
    return alertMessage;
  }

  /**
   * compare value1 to value2 expressOperatorStr : it must be a express operator.
   *
   * @return : the value of the express  e.g 5 >= 7 return false, 3 <= 3 return true
   */
  public static boolean compareValuesAccordingOperator(long value1, String expressOperatorStr,
      long value2) {
    switch (expressOperatorStr) {
      case "GT":
        return value1 > value2;
      case "EGT":
        return value1 >= value2;
      case "LT":
        return value1 < value2;
      case "ELT":
        return value1 <= value2;
      case "EQ":
        return value1 == value2;
      case "NEQ":
        return value1 != value2;
      default:
        return false;
    }
  }

  public static PerformanceMessageHistory buildPerformanceMessageHistoryFrom(
      EventLogCompressed eventLogCompressed) {
    PerformanceMessageHistory performanceMessageHistory = new PerformanceMessageHistory();
    performanceMessageHistory.setId(eventLogCompressed.getId());
    performanceMessageHistory.setSourceId(eventLogCompressed.getSourceId());
    performanceMessageHistory.setCounterKey(eventLogCompressed.getCounterKey());
    performanceMessageHistory.setCounterTotal(eventLogCompressed.getCounterTotal().get());
    performanceMessageHistory.setFrequency(eventLogCompressed.getFrequency().get());
    performanceMessageHistory.setStartTime(new Date(eventLogCompressed.getStartTime().get()));
    performanceMessageHistory.setEndTime(new Date(eventLogCompressed.getEndTime().get()));
    performanceMessageHistory.setOperation(eventLogCompressed.getOperation());
    return performanceMessageHistory;
  }

  public static void reloadClearedAlertMessage(AlertMessageDao alertMessageDao,
      MemoryAlertMessageManager memoryAlertMessageManager) {
    List<AlertMessage> allAlertMessage = alertMessageDao.getAllAlertMessage();
    for (AlertMessage alertMessage : allAlertMessage) {
      if (alertMessage.getAlertLevel() == null) {
        logger.error("alert message's level is null, alert message is: {}", alertMessage);
        continue;
      }
      if (!alertMessage.isAlertClear()) {
        memoryAlertMessageManager
            .putIfAbsentLastAlertMessage(alertMessage.getAlertMessageKey(), alertMessage);
        memoryAlertMessageManager.putMemoryAlert(alertMessage);
      }
    }
  }

  public static float convertToFloat(float data, int reduceMultiple, int decimals) {
    float b = data / reduceMultiple;
    int pow = (int) Math.pow(10, decimals);
    return (float) Math.round(b * pow) / pow;
  }

  protected static double formatDouble(double d) {
    return (double) Math.round(d * 100) / 100;
  }

  public static void validateAlertRule(AlertRule alertRule) throws IllegalParameterException {
    validateAlertLevel(alertRule);
    validateAlertRuleRelationOperator(alertRule);
    validateAlertRuleThreshold(alertRule);
  }

  public static void validateNotNull(AlertRule alertRule) throws IllegalParameterException {
    if (alertRule.getAlertLevelOne() == null) {
      throw new IllegalParameterException(
          AlertRuleException.ALERT_LEVEL_ONE_CANNOT_BE_NULL.toString());
    }

    if (alertRule.getRelationOperator() != null
        && alertRule.getAlertRecoveryRelationOperator() == null) {
      throw new IllegalParameterException(
          AlertRuleException.ALERT_RECOVERY_RELATION_OPERATOR_CANNOT_BE_NULL.toString());
    }
    if (alertRule.getAlertRecoveryRelationOperator() != null
        && alertRule.getRelationOperator() == null) {
      throw new IllegalParameterException(
          AlertRuleException.ALERT_RELATION_OPERATOR_CANNOT_BE_NULL.toString());
    }
  }

  public static void validateAlertLevel(AlertRule alertRule) throws IllegalParameterException {
    String alertLevelOne = alertRule.getAlertLevelOne();
    String alertLevelTwo = alertRule.getAlertLevelTwo();

    validateNotNull(alertRule);

    if (AlertLevel.valueOf(alertLevelOne) == AlertLevel.CLEARED) {
      throw new IllegalParameterException(
          AlertRuleException.ALERT_LEVEL_ONE_CANNOT_BE_CLEARED.toString());
    }

    if (alertLevelTwo != null) {
      if (AlertLevel.valueOf(alertLevelTwo) == AlertLevel.CLEARED) {
        throw new IllegalParameterException(
            AlertRuleException.ALERT_LEVEL_TWO_CANNOT_BE_CLEARED.toString());
      }
      if (AlertLevel.valueOf(alertLevelOne).getLevel() >= AlertLevel.valueOf(alertLevelTwo)
          .getLevel()) {
        throw new IllegalParameterException(
            AlertRuleException.ALERT_LEVEL_ONE_CANNOT_LET_ALERT_LEVEL_TWO.toString());
      }

    }
  }

  public static void validateAlertRuleRelationOperator(AlertRule alertRule)
      throws IllegalParameterException {
    String relationOperator = alertRule.getRelationOperator();
    String alertRecoveryRelationOperator = alertRule.getAlertRecoveryRelationOperator();

    validateNotNull(alertRule);

    if (relationOperator != null) {
      RelationOperator relationOperatorEnum = RelationOperator.valueOf(relationOperator);
      RelationOperator recoveryRelationOperatorEnum = RelationOperator
          .valueOf(alertRecoveryRelationOperator);
      switch (relationOperatorEnum) {
        case GT:
        case EGT:
          if (recoveryRelationOperatorEnum == RelationOperator.GT
              || recoveryRelationOperatorEnum == RelationOperator.EGT) {
            throw new IllegalParameterException(
                AlertRuleException.ALERT_AND_ALERT_RECOVERY_RELATION_OPERATOR_IS_ILLEGAL
                    .toString());
          }
          break;
        case LT:
        case ELT:
          if (recoveryRelationOperatorEnum == RelationOperator.LT
              || recoveryRelationOperatorEnum == RelationOperator.ELT) {
            throw new IllegalParameterException(
                AlertRuleException.ALERT_AND_ALERT_RECOVERY_RELATION_OPERATOR_IS_ILLEGAL
                    .toString());
          }
          break;
        case EQ:
          throw new IllegalParameterException(
              AlertRuleException.ALERT_RELATION_OPERATOR_CANNOT_BE_EQ.toString());
        case NEQ:
          throw new IllegalParameterException(
              AlertRuleException.ALERT_RELATION_OPERATOR_CANNOT_BE_NEQ.toString());
        default:
          break;
      }
      if (recoveryRelationOperatorEnum == RelationOperator.NEQ) {
        throw new IllegalParameterException(
            AlertRuleException.ALERT_RECOVERY_RELATION_OPERATOR_CANNOT_BE_NEQ.toString());
      }
    }
  }

  public static void validateAlertRuleThreshold(AlertRule alertRule)
      throws IllegalParameterException {
    String alertLevelTwo = alertRule.getAlertLevelTwo();
    int alertLevelOneThreshold = alertRule.getAlertLevelOneThreshold();
    int alertLevelTwoThreshold = alertRule.getAlertLevelTwoThreshold();
    int alertRecoveryThreshold = alertRule.getAlertRecoveryThreshold();

    AlertLevel alertLevelTwoEnum = null;
    if (!StringUtils.isEmpty(alertLevelTwo)) {
      alertLevelTwoEnum = AlertLevel.valueOf(alertLevelTwo);
    }

    if (StringUtils.isEmpty(alertRule.getRelationOperator())) {
      return;
    }

    validateNotNull(alertRule);

    RelationOperator relationOperatorEnum = RelationOperator
        .valueOf(alertRule.getRelationOperator());
    RelationOperator recoveryRelationOperatorEnum = RelationOperator
        .valueOf(alertRule.getAlertRecoveryRelationOperator());
    boolean operatorEqualIntersecting;
    boolean thresholdEqualIntersecting;
    switch (relationOperatorEnum) {
      case GT:
        if (alertRecoveryThreshold > alertLevelOneThreshold) {
          throw new IllegalParameterException(
              AlertRuleException.ALERT_RECOVERY_THRESHOLD_CANNOT_GT_ALERT_LEVEL_ONE_THRESHOLD
                  .toString());
        }
        if (alertLevelTwoEnum != null) {
          if (alertLevelTwoThreshold >= alertLevelOneThreshold) {
            throw new IllegalParameterException(
                AlertRuleException.ALERT_LEVEL_TWO_THRESHOLD_CANNOT_GET_ALERT_LEVEL_ONE_THRESHOLD
                    .toString());
          }
          if (alertRecoveryThreshold > alertLevelTwoThreshold) {
            throw new IllegalParameterException(
                AlertRuleException.ALERT_RECOVERY_THRESHOLD_CANNOT_GT_ALERT_LEVEL_TWO_THRESHOLD
                    .toString());
          }
        }
        break;
      case EGT:
        operatorEqualIntersecting = (recoveryRelationOperatorEnum == RelationOperator.ELT
            || recoveryRelationOperatorEnum == RelationOperator.EQ);
        thresholdEqualIntersecting =
            operatorEqualIntersecting && (alertRecoveryThreshold == alertLevelOneThreshold);
        if (alertRecoveryThreshold > alertLevelOneThreshold || thresholdEqualIntersecting) {
          throw new IllegalParameterException(
              AlertRuleException.ALERT_RECOVERY_THRESHOLD_CANNOT_GET_ALERT_LEVEL_ONE_THRESHOLD
                  .toString());
        }
        if (alertLevelTwoEnum != null) {
          if (alertLevelTwoThreshold >= alertLevelOneThreshold) {
            throw new IllegalParameterException(
                AlertRuleException.ALERT_LEVEL_TWO_THRESHOLD_CANNOT_GET_ALERT_LEVEL_ONE_THRESHOLD
                    .toString());
          }
          thresholdEqualIntersecting =
              operatorEqualIntersecting && (alertRecoveryThreshold == alertLevelTwoThreshold);
          if (alertRecoveryThreshold > alertLevelTwoThreshold || thresholdEqualIntersecting) {
            throw new IllegalParameterException(
                AlertRuleException.ALERT_RECOVERY_THRESHOLD_CANNOT_GET_ALERT_LEVEL_TWO_THRESHOLD
                    .toString());
          }
        }
        break;
      case LT:
        if (alertRecoveryThreshold < alertLevelOneThreshold) {
          throw new IllegalParameterException(
              AlertRuleException.ALERT_RECOVERY_THRESHOLD_CANNOT_LT_ALERT_LEVEL_ONE_THRESHOLD
                  .toString());
        }
        if (alertLevelTwoEnum != null) {
          if (alertLevelTwoThreshold <= alertLevelOneThreshold) {
            throw new IllegalParameterException(
                AlertRuleException.ALERT_LEVEL_TWO_THRESHOLD_CANNOT_LET_ALERT_LEVEL_ONE_THRESHOLD
                    .toString());
          }
          if (alertRecoveryThreshold < alertLevelTwoThreshold) {
            throw new IllegalParameterException(
                AlertRuleException.ALERT_RECOVERY_THRESHOLD_CANNOT_LT_ALERT_LEVEL_TWO_THRESHOLD
                    .toString());
          }
        }
        break;
      case ELT:
        operatorEqualIntersecting = (recoveryRelationOperatorEnum == RelationOperator.EGT
            || recoveryRelationOperatorEnum == RelationOperator.EQ);
        thresholdEqualIntersecting =
            operatorEqualIntersecting && (alertRecoveryThreshold == alertLevelOneThreshold);
        if (alertRecoveryThreshold < alertLevelOneThreshold || thresholdEqualIntersecting) {
          throw new IllegalParameterException(
              AlertRuleException.ALERT_RECOVERY_THRESHOLD_CANNOT_LET_ALERT_LEVEL_ONE_THRESHOLD
                  .toString());
        }
        if (alertLevelTwoEnum != null) {
          if (alertLevelTwoThreshold <= alertLevelOneThreshold) {
            throw new IllegalParameterException(
                AlertRuleException.ALERT_LEVEL_TWO_THRESHOLD_CANNOT_LET_ALERT_LEVEL_ONE_THRESHOLD
                    .toString());
          }
          thresholdEqualIntersecting =
              operatorEqualIntersecting && (alertRecoveryThreshold == alertLevelTwoThreshold);
          if (alertRecoveryThreshold < alertLevelTwoThreshold || thresholdEqualIntersecting) {
            throw new IllegalParameterException(
                AlertRuleException.ALERT_RECOVERY_THRESHOLD_CANNOT_LET_ALERT_LEVEL_TWO_THRESHOLD
                    .toString());
          }
        }
        break;
      case EQ:
        break;
      case NEQ:
        break;
      default:
        break;
    }
  }

  public static boolean validateMergeAlertRule(AlertRule alertRule1, AlertRule alertRule2,
      AlertTemplate alertTemplate) {
    MonitorObjectEnum monitorObjectEnum1 = MonitorServerUtils
        .getMonitorObjectEnum(alertRule1, alertTemplate);
    MonitorObjectEnum monitorObjectEnum2 = MonitorServerUtils
        .getMonitorObjectEnum(alertRule2, alertTemplate);
    return monitorObjectEnum1 == monitorObjectEnum2;
  }

  public static AlertRuleAndObjectKey buildAlertRuleAndObjectKey(AlertRule alertRule,
      String sourceKey) {
    return new AlertRuleAndObjectKey(alertRule.getAlertTemplate().getId(),
        alertRule.getCounterKey(), sourceKey);
  }

  /**
   * this is a recursive method, mix alert and mix alert recovery both invoke this method; 1, if
   * alert rule is leaf rule, then return whether the rule alert; 2, if alert rule is not leaf rule:
   * a, get its left child rule and right child rule, b, recursive call, c, return isLeftRuleAlert
   * operator isRightRuleAlert.
   *
   * @param alertRule           alertRule
   * @param alertMessage        alertMessage
   * @param mixAlertMap         used for mix alert, store the leaf rule alert or recovery info;
   * @param oppositeMixAlertMap used for mix alert, store the leaf rule recovery or alert info;
   * @param isRecovery          if {@code true}, indicate alert recovery; if {@code false}, indicate
   *                            alert;
   * @return <code>true</code> if mix alert occurs;
   *                           <code>false</code> otherwise.
   */
  public static boolean isGenerateMixAlert(AlertRule alertRule, AlertMessage alertMessage,
      Map<AlertRuleAndObjectKey, Long> mixAlertMap,
      Map<AlertRuleAndObjectKey, Long> oppositeMixAlertMap,
      boolean isRecovery) {
    String sourceKey = alertMessage.getSourceId();
    long findTime = alertMessage.getFirstAlertTime();
    String counterKey = alertMessage.getCounterKey();
    if (alertRule.isLeaf()) {
      AlertRuleAndObjectKey key = buildAlertRuleAndObjectKey(alertRule, sourceKey);
      if (alertRule.getCounterKey().equals(counterKey)) {
        mixAlertMap.put(key, findTime);
        oppositeMixAlertMap.remove(key);
        return true;
      } else {
        Long findBefore = mixAlertMap.get(key);
        return findBefore != null;
      }
    }
    AlertRule leftRule = getLeftAlertRule(alertRule);
    AlertRule rightRule = getRightAlertRule(alertRule);

    boolean isLeftRuleAlert = isGenerateMixAlert(leftRule, alertMessage, mixAlertMap,
        oppositeMixAlertMap,
        isRecovery);
    boolean isRightRuleAlert = isGenerateMixAlert(rightRule, alertMessage, mixAlertMap,
        oppositeMixAlertMap,
        isRecovery);

    /* for mixRecoveryAlertRule, only AND operator is supported.
       e.g. cpu OR memory,if alert operator and alert recovery operator are both OR,
      then cpu reach alert threshold and memory reach alert recovery threshold,
        we cannot judge whether alert or alert recovery occurs.  */
    if (isRecovery) {
      return isLeftRuleAlert && isRightRuleAlert;
    } else if (alertRule.getLogicOperator().equals(LogicOperator.AND.toString())) {
      return isLeftRuleAlert && isRightRuleAlert;
    } else {
      Validate.isTrue(alertRule.getLogicOperator().equals(LogicOperator.OR.toString()));
      return isLeftRuleAlert || isRightRuleAlert;
    }
  }

  public static void clearMixAlertMap(AlertRule parentAlertRule, String sourceKey,
      Map<AlertRuleAndObjectKey, Long> mixAlertMap) {
    if (parentAlertRule.getLeftId() == null && parentAlertRule.getRightId() == null) {
      AlertRuleAndObjectKey key = buildAlertRuleAndObjectKey(parentAlertRule, sourceKey);
      mixAlertMap.remove(key);
      return;
    }

    AlertRule leftChild = getLeftAlertRule(parentAlertRule);
    AlertRule rightChild = getRightAlertRule(parentAlertRule);

    clearMixAlertMap(leftChild, sourceKey, mixAlertMap);
    clearMixAlertMap(rightChild, sourceKey, mixAlertMap);
  }

  public static void clearMixAlertMap(Map<AlertRuleAndObjectKey, Long> mixAlertMap,
      Map<String, AlertTemplate> alertTemplateMap) {
    Iterator<Map.Entry<AlertRuleAndObjectKey, Long>> iterator = mixAlertMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<AlertRuleAndObjectKey, Long> next = iterator.next();
      AlertRuleAndObjectKey key = next.getKey();
      AlertRule alertRule = getAlertRule(alertTemplateMap, key.getSourceKey(), key.getCounterKey());
      if (alertRule != null && StringUtils.isEmpty(alertRule.getLeftId()) && StringUtils
          .isEmpty(alertRule.getParentId())) {
        iterator.remove();
      }
    }
  }

  public static AlertRule getRootAlertRule(AlertRule alertRule) {
    Validate.notNull(alertRule, "alert rule cannot be null.");
    AlertTemplate alertTemplate = alertRule.getAlertTemplate();
    AlertRule parentAlertRule = alertRule;
    while (!StringUtils.isEmpty(parentAlertRule.getParentId())) {
      String parentAlertRuleId = parentAlertRule.getParentId();
      for (Map.Entry<String, AlertRule> entry : alertTemplate.getAlertRuleMap().entrySet()) {
        if (entry.getValue().getId().equals(parentAlertRuleId)) {
          parentAlertRule = entry.getValue();
          break;
        }
      }
    }
    return parentAlertRule;
  }

  public static AlertRule getLeftAlertRule(AlertRule alertRule) {
    Validate.notNull(alertRule, "alert rule cannot be null.");
    AlertTemplate alertTemplate = alertRule.getAlertTemplate();
    Map<String, AlertRule> alertRuleMap = alertTemplate.getAlertRuleMap();

    for (Map.Entry<String, AlertRule> entry : alertRuleMap.entrySet()) {
      AlertRule rule = entry.getValue();
      if (rule.getId().equals(alertRule.getLeftId())) {
        return rule;
      }
    }
    return null;
  }

  public static AlertRule getRightAlertRule(AlertRule alertRule) {
    Validate.notNull(alertRule, "alert rule cannot be null.");
    AlertTemplate alertTemplate = alertRule.getAlertTemplate();
    Map<String, AlertRule> alertRuleMap = alertTemplate.getAlertRuleMap();

    for (Map.Entry<String, AlertRule> entry : alertRuleMap.entrySet()) {
      AlertRule rule = entry.getValue();
      if (rule.getId().equals(alertRule.getRightId())) {
        return rule;
      }
    }
    return null;
  }

  public static AlertMessage buildMixAlertMessage(AlertMessage alertMessage, AlertRule alertRule,
      boolean isRecovery) {
    if (!StringUtils.isEmpty(alertRule.getLeftId())) {
      alertMessage.setAlertDescription(alertRule.getDescription());
      alertMessage.setAlertRuleName(alertRule.getName());
      if (isRecovery) {
        alertMessage.setAlertLevel(AlertLevel.CLEARED.toString());
      } else {
        alertMessage.setAlertLevel(alertRule.getAlertLevelOne());
      }
      alertMessage.setCounterKey(alertRule.getCounterKey());
    }
    return alertMessage;
  }

  public enum DiskStatusName {
    GOOD("正常", "Good"),
    DEGRADED("慢盘", "Degraded"),
    BROKEN("损坏", "Broken"),
    CONFIG_MISMATCH("无法匹配", "Config Mismatch"),
    OFFLINED("离线", "Offlined"),
    OFFLINING("离线中", "Offlining"),
    EJECTED("拔盘", "Ejected"),
    INPROPERLY_EJECTED("暴力拔盘", "Inproperly Ejected"),
    UNKNOWN("未知", "Unknown");

    private String cnName;
    private String enName;

    DiskStatusName(String cnName, String enName) {
      this.cnName = cnName;
      this.enName = enName;
    }

    public String getCnName() {
      return cnName;
    }

    public String getEnName() {
      return enName;
    }
  }
}
