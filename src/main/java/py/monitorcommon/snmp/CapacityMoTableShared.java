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

public abstract class CapacityMoTableShared extends DefaultMOTable {

  protected static final Logger logger = LoggerFactory.getLogger(CapacityMoTableShared.class);
  protected volatile boolean isTableLoaded = false;
  protected Runnable runnable = new Runnable() {
    @Override
    public void run() {
      try {
        Thread.sleep(1000);
        isTableLoaded = false;
        logger.warn("run clear capacity table load status! set isTableLoaded false! ");
        //                clearTable();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  };

  public CapacityMoTableShared(OID oid, MOTableIndex indexDef,
      MOColumn[] columns, MOTableModel model) {
    super(oid, indexDef, columns, model);
  }

  @Override
  public void get(SubRequest request) {
    logger
        .warn("CapacityMOTable snmp get, OID:{}", request.getVariableBinding().getOid().toString());
    super.get(request);
  }

  @Override
  public boolean next(SubRequest request) {
    OID findOid = request.getVariableBinding().getOid();
    logger.warn("CapacityMOTable snmp next, OID:{}", findOid);
    if (!findOid.startsWith(SnmpMibUtils.oidPyCapacityTable)) {
      logger.warn("CapacityMOTable oid:{} is not sub oid of current table oid:{}, cannot run!",
          findOid, SnmpMibUtils.oidPyCapacityTable);
      return false;
    }

    if (!isTableLoaded) {
      logger.warn("CapacityMOTable load capacity table from DB...");
      try {
        loadTable();
      } catch (Exception e) {
        logger.debug("caught an exception when load capacity table from DB.", e);
        return false;
      }
      new Thread(runnable).start();
    }

    return super.next(request);
  }

  @Override
  public void prepare(SubRequest request) {
    logger.error("CapacityMOTable snmp not support set, OID:{}",
        request.getVariableBinding().getOid().toString());
    request.setErrorStatus(11);
    request.completed();
  }

  protected void clearTable() {
    this.removeAll();
    this.addNewRow(new OID("1"), new Variable[]{});
  }

  protected abstract void loadTable() throws TException;

  /**
   * xx.
   */
  public enum TableMember {
    Capacity_Total(0),
    Capacity_UsedInVolume(1),
    Capacity_UnusedInVolume(2),
    Capacity_Unallocated(3);

    int index;

    TableMember(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }
  }
}
