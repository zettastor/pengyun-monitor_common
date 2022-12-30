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

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.mo.MOTableModel;
import org.snmp4j.agent.request.SubRequest;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

public abstract class DiskMoTableShared extends DefaultMOTable {

  protected static final Logger logger = LoggerFactory.getLogger(DiskMoTableShared.class);
  protected volatile boolean isTableLoaded = false;
  private Runnable runnable = new Runnable() {
    @Override
    public void run() {
      try {
        Thread.sleep(1000);
        isTableLoaded = false;
        logger.warn("run clear disk table load status! set isTableLoaded false! ");
        //                clearTable();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  };

  public DiskMoTableShared(OID oid, MOTableIndex indexDef,
      MOColumn[] columns, MOTableModel model) {
    super(oid, indexDef, columns, model);
  }

  @Override
  public void get(SubRequest request) {
    logger.warn("DiskMOTable snmp get, OID:{}", request.getVariableBinding().getOid().toString());
    super.get(request);
  }

  @Override
  public boolean next(SubRequest request) {
    OID findOid = request.getVariableBinding().getOid();
    logger.warn("DiskMOTable snmp next, OID:{}", findOid);
    if (!findOid.startsWith(SnmpMibUtils.oidPyDiskTable)) {
      logger.warn("DiskMOTable oid:{} is not sub oid of current table oid:{}, cannot run!",
          findOid, SnmpMibUtils.oidPyDiskTable);
      return false;
    }

    if (!isTableLoaded) {
      logger.warn("DiskMOTable load disk table from DB...");
      try {
        loadTable();
      } catch (Exception e) {
        logger.debug("caught an exception when load disk table from DB.", e);
        return false;
      }
      new Thread(runnable).start();
    }

    return super.next(request);
  }

  protected abstract void loadTable() throws TException;

  protected void clearTable() {
    this.removeAll();
    this.addNewRow(new OID("1"), new Variable[]{});
  }

  public enum TableMember {
    Id(0),
    Name(1),
    ServerNodeId(2),
    ServerNodeName(3),
    Size(4),
    //        LogicCapacity(5),
    //        FreeSize(6),
    //        Status(7),
    //        DiskType(8),
    StorageType(5),
    Vendor(6),
    Model(7),
    LUN(8),
    WWN(9),
    ControllerId(10),
    EnclosureId(11),
    SerialNumber(12),
    Rate(13),
    SlotNo(14),
    Switch(15);

    int index;

    TableMember(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }
  }
}
