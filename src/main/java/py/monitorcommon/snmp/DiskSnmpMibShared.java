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

public class DiskSnmpMibShared implements MOGroup {

  protected static final Logger logger = LoggerFactory.getLogger(DiskSnmpMibShared.class);
  @SuppressWarnings("rawtypes")
  protected MOTable ifaceTable;

  @Override
  public void registerMOs(MOServer server, OctetString context)
      throws DuplicateRegistrationException {
    logger.warn("DiskSnmpMib registerMOs");
    server.register(this.ifaceTable, context);
  }

  @Override
  public void unregisterMOs(MOServer server, OctetString context) {
    logger.warn("DiskSnmpMib unregisterMOs");
    server.unregister(this.ifaceTable, context);
  }

  protected MOTableIndex buildIndex() {
    MOFactory moFactory = DefaultMOFactory.getInstance();
    MOTableSubIndex[] subIndex = new MOTableSubIndex[]{
        moFactory.createSubIndex(null, SMIConstants.SYNTAX_INTEGER, 1, 100)};
    MOTableIndex index = moFactory.createIndex(subIndex, false, new MOTableIndexValidator() {
      public boolean isValidIndex(OID index) {
        boolean isValidIndex = true;
        return isValidIndex;
      }
    });
    return index;
  }

  protected MOColumn<?>[] buildColumn() {
    MOColumn<?>[] columns = new MOColumn[DiskMoTableShared.TableMember.values().length];

    int colIndex = DiskMoTableShared.TableMember.Id.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.Name.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.ServerNodeId.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.ServerNodeName.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.Size.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    //        colIndex = DiskMOTable.TableMember.LogicCapacity.getIndex();
    //        columns[colIndex] = new MOMutableColumn<>(colIndex+1, 
    //        SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY);
    //
    //        colIndex = DiskMOTable.TableMember.FreeSize.getIndex();
    //        columns[colIndex] = new MOMutableColumn<>(colIndex+1, 
    //        SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY);
    //
    //        colIndex = DiskMOTable.TableMember.Status.getIndex();
    //        columns[colIndex] = new MOMutableColumn<>(colIndex+1, 
    //        SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY);
    //
    //        colIndex = DiskMOTable.TableMember.DiskType.getIndex();
    //        columns[colIndex] = new MOMutableColumn<>(colIndex+1, 
    //        SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.StorageType.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.Vendor.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.Model.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.LUN.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.WWN.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.ControllerId.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.EnclosureId.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.SerialNumber.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.Rate.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.SlotNo.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_OCTET_STRING,
        MOAccessImpl.ACCESS_READ_ONLY);

    colIndex = DiskMoTableShared.TableMember.Switch.getIndex();
    columns[colIndex] = new MOMutableColumn<>(colIndex + 1, SMIConstants.SYNTAX_INTEGER32,
        MOAccessImpl.ACCESS_READ_WRITE);
    return columns;
  }
}
