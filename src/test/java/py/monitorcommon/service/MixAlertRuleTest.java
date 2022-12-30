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

package py.monitorcommon.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Assert;
import org.junit.Test;
import py.common.RequestIdBuilder;
import py.monitor.common.AlertMessage;
import py.monitor.common.AlertRule;
import py.monitor.common.AlertRuleAndObjectKey;
import py.monitor.common.AlertTemplate;
import py.monitor.common.CounterName;
import py.monitor.common.LogicOperator;
import py.monitor.common.MonitorObjectEnum;
import py.monitor.utils.MonitorServerUtils;
import py.monitorcommon.manager.MonitorServerHelperShared;
import py.test.TestBase;

/**
 * xx.
 */
public class MixAlertRuleTest extends TestBase {

  private AlertTemplate alertTemplate;
  private Map<AlertRuleAndObjectKey, Long> mixAlertMap;
  private Map<AlertRuleAndObjectKey, Long> oppositeMixAlertMap;
  private boolean isRecovery = false;

  @Override
  public void init() {
    alertTemplate = new AlertTemplate();
    mixAlertMap = new ConcurrentHashMap<>();
    oppositeMixAlertMap = new ConcurrentHashMap<>();
    Map<String, AlertRule> alertRuleMap = new HashMap<>();
    alertTemplate.setAlertRuleMap(alertRuleMap);
  }

  @Test
  public void testTwoLevelAnd() {
    AlertRule left = buildAlertRule();
    AlertRule right = buildAlertRule();
    AlertRule parent = mergeAlertRule(left, right, LogicOperator.AND);
    AlertMessage alertMessage = buildAlertMessage(left);
    boolean isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent, alertMessage, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertFalse(isGenerateMixAlert);

    AlertMessage alertMessage1 = buildAlertMessage(right);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent, alertMessage1, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertTrue(isGenerateMixAlert);
  }

  @Test
  public void testTwoLevelOr() {
    AlertRule left = buildAlertRule();
    AlertRule right = buildAlertRule();
    AlertRule parent = mergeAlertRule(left, right, LogicOperator.OR);
    AlertMessage alertMessage = buildAlertMessage(left);
    boolean isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent, alertMessage, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertTrue(isGenerateMixAlert);
  }

  /**
   * (rule1 OR rule2) AND rule3.
   */
  @Test
  public void testThreeLevel1() {
    AlertRule rule1 = buildAlertRule();
    AlertRule rule2 = buildAlertRule();
    AlertRule rule3 = buildAlertRule();
    AlertRule parent1 = mergeAlertRule(rule1, rule2, LogicOperator.OR);
    AlertRule parent2 = mergeAlertRule(parent1, rule3, LogicOperator.AND);

    AlertMessage alertMessage1 = buildAlertMessage(rule1);
    boolean isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage1, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertFalse(isGenerateMixAlert);

    AlertMessage alertMessage2 = buildAlertMessage(rule2);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage2, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertFalse(isGenerateMixAlert);

    AlertMessage alertMessage3 = buildAlertMessage(rule3);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage3, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertTrue(isGenerateMixAlert);
  }

  /**
   * (rule1 OR rule2) OR rule3.
   */
  @Test
  public void testThreeLevel2() {
    AlertRule rule1 = buildAlertRule();
    AlertRule rule2 = buildAlertRule();
    AlertRule rule3 = buildAlertRule();
    AlertRule parent1 = mergeAlertRule(rule1, rule2, LogicOperator.OR);
    AlertRule parent2 = mergeAlertRule(parent1, rule3, LogicOperator.OR);

    AlertMessage alertMessage1 = buildAlertMessage(rule1);
    boolean isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage1, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertTrue(isGenerateMixAlert);

    AlertMessage alertMessage2 = buildAlertMessage(rule2);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage2, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertTrue(isGenerateMixAlert);

    AlertMessage alertMessage3 = buildAlertMessage(rule3);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage3, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertTrue(isGenerateMixAlert);
  }

  /**
   * (rule1 AND rule2) OR rule3.
   */
  @Test
  public void testThreeLevel3() {
    AlertRule rule1 = buildAlertRule();
    AlertRule rule2 = buildAlertRule();
    AlertRule rule3 = buildAlertRule();
    AlertRule parent1 = mergeAlertRule(rule1, rule2, LogicOperator.AND);
    AlertRule parent2 = mergeAlertRule(parent1, rule3, LogicOperator.OR);

    AlertMessage alertMessage1 = buildAlertMessage(rule1);
    boolean isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage1, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertFalse(isGenerateMixAlert);

    AlertMessage alertMessage2 = buildAlertMessage(rule2);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage2, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertTrue(isGenerateMixAlert);

    AlertMessage alertMessage3 = buildAlertMessage(rule3);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage3, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertTrue(isGenerateMixAlert);
  }

  /**
   * (rule1 AND rule2) AND rule3.
   */
  @Test
  public void testThreeLevel4() {
    AlertRule rule1 = buildAlertRule();
    AlertRule rule2 = buildAlertRule();
    AlertRule rule3 = buildAlertRule();
    AlertRule parent1 = mergeAlertRule(rule1, rule2, LogicOperator.AND);
    AlertRule parent2 = mergeAlertRule(parent1, rule3, LogicOperator.AND);

    AlertMessage alertMessage1 = buildAlertMessage(rule1);
    boolean isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage1, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertFalse(isGenerateMixAlert);

    AlertMessage alertMessage2 = buildAlertMessage(rule2);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage2, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertFalse(isGenerateMixAlert);

    AlertMessage alertMessage3 = buildAlertMessage(rule3);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent2, alertMessage3, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertTrue(isGenerateMixAlert);
  }

  @Test
  public void testFourLevel() {
    AlertRule rule1 = buildAlertRule();
    AlertRule rule2 = buildAlertRule();
    AlertRule rule3 = buildAlertRule();
    AlertRule parent1 = mergeAlertRule(rule1, rule2, LogicOperator.OR);
    AlertRule parent2 = mergeAlertRule(parent1, rule3, LogicOperator.AND);

    AlertRule rule4 = buildAlertRule();
    AlertRule rule5 = buildAlertRule();
    AlertRule parent3 = mergeAlertRule(rule4, rule5, LogicOperator.AND);
    AlertRule parent4 = mergeAlertRule(parent2, parent3, LogicOperator.AND);

    AlertMessage alertMessage1 = buildAlertMessage(rule1);
    boolean isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent4, alertMessage1, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertFalse(isGenerateMixAlert);

    AlertMessage alertMessage2 = buildAlertMessage(rule2);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent4, alertMessage2, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertFalse(isGenerateMixAlert);

    AlertMessage alertMessage3 = buildAlertMessage(rule3);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent4, alertMessage3, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertFalse(isGenerateMixAlert);

    AlertMessage alertMessage4 = buildAlertMessage(rule4);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent4, alertMessage4, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertFalse(isGenerateMixAlert);

    AlertMessage alertMessage5 = buildAlertMessage(rule5);
    isGenerateMixAlert = MonitorServerHelperShared
        .isGenerateMixAlert(parent4, alertMessage5, mixAlertMap,
            oppositeMixAlertMap, isRecovery);
    Assert.assertTrue(isGenerateMixAlert);
  }

  @Test
  public void testCounterNameWithMonitorObjectEnum() {
    CounterName[] values = CounterName.values();
    for (CounterName counterName : values) {
      switch (counterName) {
        case CPU:
        case MEMORY:
        case SERVERNODE_STATUS:
          Assert.assertEquals(MonitorObjectEnum.NODE, counterName.getMonitorObjectEnum());
          break;
        case DISK_STATUS:
        case DISK_TEMPERATURE:
        case TRACK_STATUS:
        case DISK_READ_IOPS:
        case DISK_READ_THROUGHPUT:
        case DISK_READ_WAIT:
        case DISK_UTIL:
        case DISK_WRITE_IOPS:
        case DISK_WRITE_THROUGHPUT:
        case DISK_WRITE_WAIT:
          Assert.assertEquals(MonitorObjectEnum.DISK, counterName.getMonitorObjectEnum());
          break;
        //                case NETCARD_TX_ERRORS:
        //                case NETCARD_RX_ERRORS:
        //                case NETCARD_TX_DROPPED:
        //                case NETCARD_RX_DROPPED:
        //                case NETCARD_RATE:
        case NETWORK_STATUS:
          Assert.assertEquals(MonitorObjectEnum.NETWORK_CARD, counterName.getMonitorObjectEnum());
          break;
        case NETCARD_DELAY:
        case NETWORK_DROPPED:
          Assert.assertEquals(MonitorObjectEnum.NETWORK, counterName.getMonitorObjectEnum());
          break;
        case SERVICE_STATUS:
        case DATANODE_IO_DELAY:
          Assert.assertEquals(MonitorObjectEnum.SERVICE, counterName.getMonitorObjectEnum());
          break;
        case STORAGEPOOL_AVAILABLE_PSA_SEGMENT_COUNT:
        case STORAGEPOOL_AVAILABLE_PSS_SEGMENT_COUNT:
        case STORAGEPOOL_FREE_SPACE_RATIO:
        case STORAGEPOOL_GROUP_AMOUNT:
        case STORAGEPOOL_IO_BLOCK_SIZE:
        case STORAGEPOOL_LOST_DISK:
        case STORAGEPOOL_READ_IOPS:
        case STORAGEPOOL_READ_LATENCY:
        case STORAGEPOOL_READ_THROUGHPUT:
        case STORAGEPOOL_REBUILD_FAIL:
        case STORAGEPOOL_VOLUME_AMOUNT:
        case STORAGEPOOL_WRITE_IOPS:
        case STORAGEPOOL_WRITE_LATENCY:
        case STORAGEPOOL_WRITE_THROUGHPUT:
          Assert.assertEquals(MonitorObjectEnum.STORAGE_POOL, counterName.getMonitorObjectEnum());
          break;
        case VOLUME_IO_BLOCK_SIZE:
        case VOLUME_READ_IOPS:
        case VOLUME_READ_LATENCY:
        case VOLUME_READ_THROUGHPUT:
        case VOLUME_WRITE_IOPS:
        case VOLUME_WRITE_LATENCY:
        case VOLUME_WRITE_THROUGHPUT:
          Assert.assertEquals(MonitorObjectEnum.VOLUME, counterName.getMonitorObjectEnum());
          break;
        case SYSTEM_READ_IOPS:
        case SYSTEM_WRITE_IOPS:
        case SYSTEM_READ_LATENCY:
        case SYSTEM_IO_BLOCK_SIZE:
        case SYSTEM_WRITE_LATENCY:
        case SYSTEM_READ_THROUGHPUT:
        case SYSTEM_WRITE_THROUGHPUT:
          Assert.assertEquals(MonitorObjectEnum.SYSTEM, counterName.getMonitorObjectEnum());
          break;
        default:
      }
    }
  }

  @Test
  public void testGetMonitorObjectEnum() {
    AlertRule rule1 = buildAlertRule(CounterName.CPU);
    MonitorObjectEnum monitorObjectEnum = MonitorServerUtils
        .getMonitorObjectEnum(rule1, alertTemplate);
    CounterName counterName = CounterName.valueOf(rule1.getCounterKey());
    Assert.assertEquals(MonitorObjectEnum.NODE, monitorObjectEnum);
    Assert.assertEquals(counterName.getMonitorObjectEnum(), monitorObjectEnum);

    AlertRule rule2 = buildAlertRule(CounterName.MEMORY);
    AlertRule rule3 = mergeAlertRule(rule1, rule2, LogicOperator.AND);
    MonitorObjectEnum monitorObjectEnum2 = MonitorServerUtils
        .getMonitorObjectEnum(rule3, alertTemplate);
    Assert.assertEquals(MonitorObjectEnum.NODE, monitorObjectEnum2);
    Assert.assertEquals(counterName.getMonitorObjectEnum(), monitorObjectEnum2);

    AlertRule rule4 = buildAlertRule(CounterName.DATANODE_IO_DELAY);
    AlertRule rule5 = mergeAlertRule(rule3, rule4, LogicOperator.OR);
    MonitorObjectEnum monitorObjectEnum3 = MonitorServerUtils
        .getMonitorObjectEnum(rule5, alertTemplate);
    Assert.assertEquals(MonitorObjectEnum.NODE, monitorObjectEnum3);
    Assert.assertEquals(counterName.getMonitorObjectEnum(), monitorObjectEnum3);
  }

  private AlertMessage buildAlertMessage(AlertRule alertRule) {
    AlertMessage alertMessage = new AlertMessage();
    alertMessage.setFirstAlertTime(System.currentTimeMillis());
    alertMessage.setSourceId("A");
    alertMessage.setCounterKey(alertRule.getCounterKey());
    return alertMessage;
  }

  private AlertRule buildAlertRule(CounterName counterName) {
    AlertRule alertRule = new AlertRule();
    alertRule.setId(String.valueOf(RequestIdBuilder.get()));
    alertRule.setAlertTemplate(alertTemplate);
    alertRule.setCounterKey(counterName.toString());
    alertTemplate.getAlertRuleMap().put(alertRule.getId(), alertRule);
    return alertRule;
  }

  private AlertRule buildAlertRule() {
    AlertRule alertRule = new AlertRule();
    alertRule.setId(String.valueOf(RequestIdBuilder.get()));
    alertRule.setAlertTemplate(alertTemplate);
    alertRule.setCounterKey(String.valueOf(new Random().nextLong()));
    alertTemplate.getAlertRuleMap().put(alertRule.getId(), alertRule);
    return alertRule;
  }

  private AlertRule mergeAlertRule(AlertRule left, AlertRule right, LogicOperator logicOperator) {
    AlertRule parent = new AlertRule();
    parent.setId(String.valueOf(RequestIdBuilder.get()));
    parent.setAlertTemplate(alertTemplate);
    left.setParentId(parent.getId());
    parent.setLeftId(left.getId());
    parent.setRightId(right.getId());
    parent.setCounterKey(
        left.getCounterKey() + MonitorServerUtils.MIX_ALERT_RULE_DELIMITER + right.getCounterKey());
    parent.setLogicOperator(logicOperator.toString());
    alertTemplate.getAlertRuleMap().put(parent.getId(), parent);
    return parent;
  }
}
