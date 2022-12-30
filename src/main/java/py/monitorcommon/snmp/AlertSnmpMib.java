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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.DefaultMOFactory;
import org.snmp4j.agent.mo.DefaultMOMutableRow2PCFactory;
import org.snmp4j.agent.mo.DefaultMOMutableTableModel;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOFactory;
import org.snmp4j.agent.mo.MOMutableColumn;
import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.mo.MOTableIndexValidator;
import org.snmp4j.agent.mo.MOTableSubIndex;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;
import py.monitorcommon.dao.AlertMessageDao;
import py.monitorcommon.task.AlertMessageFilterByTimeTask;

public class AlertSnmpMib implements MOGroup {

  private static final Logger logger = LoggerFactory.getLogger(AlertSnmpMib.class);

  @SuppressWarnings("rawtypes")
  private MOTable ifaceTable;

  public AlertSnmpMib(AlertMessageDao alertMessageDao,
      AlertMessageFilterByTimeTask alertMessageFilterByTimeTask) {
    this.ifaceTable = buildSimpleTable(alertMessageDao, alertMessageFilterByTimeTask);
  }

  @Override
  public void registerMOs(MOServer server, OctetString context)
      throws DuplicateRegistrationException {
    logger.warn("AlertSnmpMib registerMOs");
    server.register(this.ifaceTable, context);
  }

  @Override
  public void unregisterMOs(MOServer server, OctetString context) {
    logger.warn("AlertSnmpMib unregisterMOs");
    server.unregister(this.ifaceTable, context);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private MOTable buildSimpleTable(AlertMessageDao alertMessageDao,
      AlertMessageFilterByTimeTask alertMessageFilterByTimeTask) {
    logger.warn("AlertSnmpMib buildSimpleTable");

    MOColumn<?>[] columns = new MOColumn[AlertMoTable.TableMember.values().length];

    int colIndex = AlertMoTable.TableMember.AlertMsg_Id.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_WRITE);

    colIndex = AlertMoTable.TableMember.AlertMsg_SourceName.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_WRITE);

    colIndex = AlertMoTable.TableMember.AlertMsg_Description.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_WRITE);

    colIndex = AlertMoTable.TableMember.AlertMsg_Level.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_INTEGER32,
        MOAccessImpl.ACCESS_READ_WRITE);

    colIndex = AlertMoTable.TableMember.AlertMsg_IsAck.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_INTEGER32,
        MOAccessImpl.ACCESS_READ_WRITE);

    colIndex = AlertMoTable.TableMember.AlertMsg_Type.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_WRITE);

    colIndex = AlertMoTable.TableMember.AlertMsg_RuleName.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_WRITE);

    colIndex = AlertMoTable.TableMember.AlertMsg_FirstTime.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_WRITE);

    colIndex = AlertMoTable.TableMember.AlertMsg_LastTime.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_WRITE);

    colIndex = AlertMoTable.TableMember.AlertMsg_Frequency.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_WRITE);

    colIndex = AlertMoTable.TableMember.AlertMsg_IsClear.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_INTEGER32,
        MOAccessImpl.ACCESS_READ_WRITE);

    colIndex = AlertMoTable.TableMember.AlertMsg_ClearTime.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_WRITE);

    DefaultMOMutableTableModel tableModel = new DefaultMOMutableTableModel<>();
    tableModel.setRowFactory(new DefaultMOMutableRow2PCFactory());

    logger.debug("alertMessageDao in mib is: {}", alertMessageDao);
    MOFactory moFactory = DefaultMOFactory.getInstance();
    MOTableSubIndex[] subIndex = new MOTableSubIndex[]{
        moFactory.createSubIndex(null, SMIConstants.SYNTAX_INTEGER, 1, 100)};
    MOTableIndex index = moFactory.createIndex(subIndex, false, new MOTableIndexValidator() {
      public boolean isValidIndex(OID index) {
        boolean isValidIndex = true;
        return isValidIndex;
      }
    });
    DefaultMOTable table = new AlertMoTable(new OID(SnmpMibUtils.oidPyAlertTableEntry), index,
        columns,
        alertMessageDao, alertMessageFilterByTimeTask);
    table.setModel(tableModel);

    table.addNewRow(new OID("1"), new Variable[]{});

    table.setVolatile(true);
    return table;
  }
}
