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

import java.util.Arrays;
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

public abstract class ServerNodeMoTableShared extends DefaultMOTable {

  protected static final Logger logger = LoggerFactory.getLogger(ServerNodeMoTableShared.class);

  protected volatile boolean isTableLoaded = false;
  protected Runnable runnable = new Runnable() {
    @Override
    public void run() {
      try {
        Thread.sleep(1000);
        isTableLoaded = false;
        logger.warn("run clear server node table load status! set isTableLoaded false! ");
        //                clearTable();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  };

  public ServerNodeMoTableShared(OID oid, MOTableIndex indexDef,
      MOColumn[] columns, MOTableModel model) {
    super(oid, indexDef, columns, model);
  }

  @Override
  public void get(SubRequest request) {
    logger.warn("ServerNodeMOTable snmp get, OID:{}",
        request.getVariableBinding().getOid().toString());
    super.get(request);
  }

  @Override
  public boolean next(SubRequest request) {
    OID findOid = request.getVariableBinding().getOid();
    logger.warn("ServerNodeMOTable snmp next, OID:{}", findOid);
    if (!findOid.startsWith(SnmpMibUtils.oidPyServerNodeTable)) {
      logger.warn("ServerNodeMOTable oid:{} is not sub oid of current table oid:{}, cannot run!",
          findOid, SnmpMibUtils.oidPyServerNodeTable);
      return false;
    }

    if (!isTableLoaded) {
      logger.warn("ServerNodeMOTable load server node table from infocenter...");
      try {
        loadTable();
      } catch (Exception e) {
        logger.debug("caught an exception when load server node table from DB.", e);
        return false;
      }
      new Thread(runnable).start();
    }

    return super.next(request);
  }

  @Override
  public void prepare(SubRequest request) {
    logger.warn("ServerNodeMOTable snmp set, OID:{}",
        request.getVariableBinding().getOid().toString());

    int[] value1 = request.getVariableBinding().getOid().getValue();
    logger.warn("set value1 is: {}", Arrays.toString(value1));
    int colume = 0;
    int row = 0;
    String serverNodeId = null;
    try {
      colume = request.getVariableBinding().getOid().get(9);
      row = request.getVariableBinding().getOid().get(10);
      serverNodeId = this.getValue(new OID(String.valueOf(row)), TableMember.Node_Id.getIndex())
          .toString();
    } catch (Exception e) {
      logger.error("server node snmp set, colume: {}, row: {}. caught an exception.",
          colume, row, e);
      request.setErrorStatus(11);
      request.completed();
      return;
    }

    logger.warn("start server node snmp set, colume: {}, row: {}", colume, row);
    colume = colume - 1;
    updateServerNode(request, colume, serverNodeId);
    //        super.prepare(request);
    request.completed();
  }

  protected void clearTable() {
    this.removeAll();
    this.addNewRow(new OID("1"), new Variable[]{});
  }

  protected abstract void loadTable() throws TException;

  protected abstract void updateServerNode(SubRequest request, int colume, String serverNodeId);

  /**
   * xx.
   */
  public enum TableMember {
    Node_Id(0),
    Node_HostName(1),
    Node_OSInfo(2),
    Node_CpuInfo(3),
    Node_MemSize(4),
    Node_DiskSize(5),
    Node_NetworkCardInfo(6),
    Node_ManageIp(7),
    Node_GatewayIp(8),
    Node_CabinetName(9),
    Node_ChildFrameNo(10),
    Node_SlotNo(11),
    Node_Status(12);

    int index;

    TableMember(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }
  }
}
