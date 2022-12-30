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

package py.monitorcommon.snmp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.agent.mo.DefaultMOMutableTableModel;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.request.SubRequest;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import py.monitor.common.AlertLevel;
import py.monitor.common.AlertMessage;
import py.monitorcommon.dao.AlertMessageDao;
import py.monitorcommon.task.AlertMessageFilterByTimeTask;
import py.thrift.monitorserver.service.SnmpVersion;

public class AlertMoTable extends DefaultMOTable {

  private static final Logger logger = LoggerFactory.getLogger(AlertMoTable.class);
  private volatile boolean isTableLoaded = false;
  private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private AlertMessageDao alertMessageDao;
  private AlertMessageFilterByTimeTask alertMessageFilterByTimeTask;
  private Runnable runnable = new Runnable() {
    @Override
    public void run() {
      try {
        Thread.sleep(1000);
        isTableLoaded = false;
        logger.warn("run clear alert table load status! set isTableLoaded false! ");
        //                clearTable();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  };

  public AlertMoTable(OID oid, MOTableIndex indexDef, MOColumn[] columns,
      AlertMessageDao alertMessageDao, AlertMessageFilterByTimeTask alertMessageFilterByTimeTask) {
    super(oid, indexDef, columns, new DefaultMOMutableTableModel());
    this.alertMessageDao = alertMessageDao;
    this.alertMessageFilterByTimeTask = alertMessageFilterByTimeTask;
  }

  public static Map<OID, Variable> buildTrapMap(AlertMessage alertMessage,
      SnmpVersion snmpVersion) {
    Map<OID, Variable> map = new HashMap<>();
    String trapDesc = String.format(SnmpMibUtils.trapDescFormat, alertMessage.getSourceName(),
        alertMessage.getAlertRuleName(), alertMessage.getAlertDescription());
    if (snmpVersion == SnmpVersion.SNMPV2C) {
      map.putAll(SnmpAgentTrapV2
          .buildCommonTrapHeader(new OID(SnmpMibUtils.oidPyAlertItemRuleName), trapDesc));
    } else if (snmpVersion == SnmpVersion.SNMPV3) {
      map.putAll(SnmpAgentTrapV3
          .buildCommonTrapHeader(new OID(SnmpMibUtils.oidPyAlertItemRuleName), trapDesc));
    }

    map.put(SnmpMibUtils.oidPyAlertItemAcknowledge,
        new Integer32(alertMessage.isAlertAcknowledge() ? 1 : 2));
    map.put(SnmpMibUtils.oidPyAlertItemOjectName, new OctetString(alertMessage.getSourceName()));
    map.put(SnmpMibUtils.oidPyAlertItemDescription,
        new OctetString(alertMessage.getAlertDescription()));
    map.put(SnmpMibUtils.oidPyAlertItemLevel, new OctetString(alertMessage.getAlertLevel()));
    map.put(SnmpMibUtils.oidPyAlertItemType, new OctetString(alertMessage.getAlertType()));
    map.put(SnmpMibUtils.oidPyAlertItemRuleName, new OctetString(alertMessage.getAlertRuleName()));
    map.put(SnmpMibUtils.oidPyAlertItemClear, new Integer32(alertMessage.isAlertClear() ? 1 : 2));
    return map;
  }

  @Override
  public void get(SubRequest request) {
    logger.warn("AlertMOTable snmp get, OID:{}", request.getVariableBinding().getOid().toString());
    super.get(request);
  }

  @Override
  public boolean next(SubRequest request) {
    OID findOid = request.getVariableBinding().getOid();
    logger.warn("AlertMOTable snmp next, OID:{}", findOid);
    if (!findOid.startsWith(SnmpMibUtils.oidPyAlertTable)) {
      logger.warn("AlertMOTable oid:{} is not sub oid of current table oid:{}, cannot run!",
          findOid, SnmpMibUtils.oidPyAlertTable);
      return false;
    }

    if (!isTableLoaded) {
      logger.warn("load alert table from DB...");
      try {
        loadTableFromDb();
      } catch (Exception e) {
        logger.debug("caught an exception when load alert table from DB.", e);
        return false;
      }
      new Thread(runnable).start();
    }

    return super.next(request);
  }

  @Override
  public void prepare(SubRequest request) {
    logger.warn("AlertMOTable snmp set, OID:{}", request.getVariableBinding().getOid().toString());

    int[] value1 = request.getVariableBinding().getOid().getValue();
    logger.warn("value1 is: {}", Arrays.toString(value1));
    int colume = 0;
    int row = 0;
    String alertMessageId = null;
    try {
      colume = request.getVariableBinding().getOid().get(9);
      row = request.getVariableBinding().getOid().get(10);
      alertMessageId = this
          .getValue(new OID(String.valueOf(row)), TableMember.AlertMsg_Id.getIndex()).toString();
    } catch (Exception e) {
      logger.warn("snmp set, colume: {}, row: {}. caught an exception.", colume, row, e);
    }
    logger.warn("snmp set, colume: {}, row: {}", colume, row);

    colume = colume - 1;
    if (colume == TableMember.AlertMsg_Level.getIndex()) {
      Variable variable = request.getVariableBinding().getVariable();
      int value = variable.toInt();
      logger.warn("update alertLevel by alertMessageId, id:{}, value:{}", alertMessageId, value);
      if (value == AlertLevel.CLEARED.getLevel()) {
        AlertMessage alertMessageDb = alertMessageDao.getAlertMessageById(alertMessageId);
        if (!alertMessageDb.isAlertClear()) {
          AlertMessage remove = alertMessageFilterByTimeTask
              .removeLastAlertMessageKey(alertMessageDb
                  .getAlertMessageKey());
          logger.debug("alert message will be removed, alertmessage:{}", remove);
          if (remove == null) {
            request.setErrorStatus(11);
            logger.debug("clear alert error.");
          } else {
            alertMessageDao.clearAlertMessageById(alertMessageId);
          }
        }
      } else {
        request.setErrorStatus(11);
        logger.debug("alert level can only be cleared!");
      }
    } else if (colume == TableMember.AlertMsg_IsAck.getIndex()) {
      Variable variable = request.getVariableBinding().getVariable();
      int i = variable.toInt();
      boolean value = i == 1;
      logger.warn("update alertAcknowedge by alertMessageId...id:{}, value:{}", alertMessageId,
          value);
      if (value) {
        alertMessageDao.acknowledgeAlertMessageById(alertMessageId);
      } else {
        alertMessageDao.clearAcknowledgeAlertMessageById(alertMessageId);
      }
    } else {
      request.setErrorStatus(11);
      logger.warn("snmp colume not support modify, colume is {}", colume);
    }

    //        super.prepare(request);
    request.completed();
  }

  private void loadTableFromDb() {
    this.removeAll();
    logger.warn("AlertMOTable alertMessage size is: {}", alertMessageDao.getTotalCount());
    logger.debug("alertMessageDao is:{}", alertMessageDao);
    List<AlertMessage> alertMessageList = alertMessageDao
        .getFilterAlertMessage(0, 500, "lastAlertTime", "desc", null, null, null, null, null, null,
            null, null, null, true);

    for (int i = 0; i < alertMessageList.size(); i++) {
      Variable[] variables;
      try {
        variables = buildVariableArrayFrom(alertMessageList.get(i));
      } catch (Exception e) {
        logger
            .error("build variable from server node:{}, cause exception,", alertMessageList.get(i),
                e);
        continue;
      }
      this.addNewRow(new OID(String.valueOf(i)), variables);
    }
    isTableLoaded = true;
  }

  private void clearTable() {
    this.removeAll();
    this.addNewRow(new OID("1"), new Variable[]{});
  }

  private Variable[] buildVariableArrayFrom(AlertMessage alertMessage) {
    Variable[] variables = new Variable[TableMember.values().length];

    variables[TableMember.AlertMsg_Id.getIndex()] = new OctetString(alertMessage.getId());
    variables[TableMember.AlertMsg_SourceName.getIndex()] = new OctetString(
        alertMessage.getSourceName());
    variables[TableMember.AlertMsg_Description.getIndex()] = new OctetString(
        alertMessage.getAlertDescription());
    switch (AlertLevel.valueOf(alertMessage.getAlertLevel())) {
      case CRITICAL:
        variables[TableMember.AlertMsg_Level.getIndex()] = new Integer32(
            AlertLevel.CRITICAL.getLevel());
        break;
      case MAJOR:
        variables[TableMember.AlertMsg_Level.getIndex()] = new Integer32(
            AlertLevel.MAJOR.getLevel());
        break;
      case MINOR:
        variables[TableMember.AlertMsg_Level.getIndex()] = new Integer32(
            AlertLevel.MINOR.getLevel());
        break;
      case WARNING:
        variables[TableMember.AlertMsg_Level.getIndex()] = new Integer32(
            AlertLevel.WARNING.getLevel());
        break;
      case CLEARED:
        variables[TableMember.AlertMsg_Level.getIndex()] = new Integer32(
            AlertLevel.CLEARED.getLevel());
        break;
      default:
        logger.warn("alert message alert level is error, alert level: {}",
            alertMessage.getAlertLevel());
        return null;
    }
    variables[TableMember.AlertMsg_IsAck.getIndex()] = new Integer32(
        alertMessage.isAlertAcknowledge() ? 1 : 0);
    variables[TableMember.AlertMsg_Type.getIndex()] = new OctetString(alertMessage.getAlertType());
    variables[TableMember.AlertMsg_RuleName.getIndex()] = new OctetString(
        alertMessage.getAlertRuleName());
    variables[TableMember.AlertMsg_FirstTime.getIndex()] = new OctetString(
        dateFormat.format(new Date(alertMessage.getFirstAlertTime())));
    variables[TableMember.AlertMsg_LastTime.getIndex()] = new OctetString(
        dateFormat.format(new Date(alertMessage.getLastAlertTime())));
    variables[TableMember.AlertMsg_Frequency.getIndex()] = new OctetString(
        String.valueOf(alertMessage.getAlertFrequency()));
    variables[TableMember.AlertMsg_ClearTime.getIndex()] = new OctetString(
        dateFormat.format(new Date(alertMessage.getClearTime())));
    variables[TableMember.AlertMsg_IsClear.getIndex()] = new Integer32(
        alertMessage.isAlertClear() ? 1 : 0);
    return variables;
  }

  public enum TableMember {
    AlertMsg_Id(0),
    AlertMsg_SourceName(1),
    AlertMsg_Description(2),
    AlertMsg_Level(3),
    AlertMsg_IsAck(4),
    AlertMsg_Type(5),
    AlertMsg_RuleName(6),
    AlertMsg_FirstTime(7),
    AlertMsg_LastTime(8),
    AlertMsg_Frequency(9),
    AlertMsg_IsClear(10),
    AlertMsg_ClearTime(11);

    int index;

    TableMember(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }
  }

}

