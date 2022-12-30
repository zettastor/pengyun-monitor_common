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

import org.snmp4j.smi.OID;

public class SnmpMibUtils {

  //[<alert object>:<alert rule>]<alert desc>
  public static final String trapDescFormat = "%s::%s::%s";

  public static final OID oidPy = new OID(".1.3.6.1.4.1.20229");

  // about alert mib
  public static final OID oidPyAlertTable = new OID(".1.3.6.1.4.1.20229.1");
  public static final OID oidPyAlertTableEntry = new OID(".1.3.6.1.4.1.20229.1.1");

  // about alert trap mib
  public static final OID oidPyAlert = new OID(".1.3.6.1.4.1.20229.10");
  public static final OID oidPyAlertItemAcknowledge = new OID(".1.3.6.1.4.1.20229.10.1");
  public static final OID oidPyAlertItemOjectName = new OID(".1.3.6.1.4.1.20229.10.2");
  public static final OID oidPyAlertItemDescription = new OID(".1.3.6.1.4.1.20229.10.3");
  public static final OID oidPyAlertItemLevel = new OID(".1.3.6.1.4.1.20229.10.4");
  public static final OID oidPyAlertItemType = new OID(".1.3.6.1.4.1.20229.10.5");
  public static final OID oidPyAlertItemRuleName = new OID(".1.3.6.1.4.1.20229.10.6");
  public static final OID oidPyAlertItemClear = new OID(".1.3.6.1.4.1.20229.10.7");

  //about capacity mib
  public static final OID oidPyCapacityTable = new OID(".1.3.6.1.4.1.20229.20");
  public static final OID oidPyCapacityTableEntry = new OID(".1.3.6.1.4.1.20229.20.1");

  //about server node mib
  public static final OID oidPyServerNodeTable = new OID(".1.3.6.1.4.1.20229.30");
  public static final OID oidPyServerNodeTableEntry = new OID(".1.3.6.1.4.1.20229.30.1");

  //about disk mib
  public static final OID oidPyDiskTable = new OID(".1.3.6.1.4.1.20229.40");
  public static final OID oidPyDiskTableEntry = new OID(".1.3.6.1.4.1.20229.40.1");


  public static String filterNullString(String src) {
    String value = src;
    if (value == null) {
      value = "";
    }
    return value;
  }
}
