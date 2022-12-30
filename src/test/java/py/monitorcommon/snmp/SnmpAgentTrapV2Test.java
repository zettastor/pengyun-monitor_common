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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import py.monitor.common.AlertMessage;
import py.test.TestBase;
import py.thrift.monitorserver.service.SnmpVersion;

/**
 * xx.
 */
public class SnmpAgentTrapV2Test extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(SnmpAgentTrapV2Test.class);
  private final int trapServerPort = 162;
  private Level originalLevel;
  private String trapServerIp = "127.0.0.1";

  @Override
  public void init() throws Exception {
    super.init();

    originalLevel = getLogLevel();
    setLogLevel(Level.DEBUG);
  }

  @Test
  public void testSendTrap() throws Exception {
    String notification = "This is a demo trap";

    SnmpAgentTrapV2 trapV2 = new SnmpAgentTrapV2(trapServerIp, trapServerPort);
    trapV2.setCommunity("public");
    trapV2.trap(SnmpConstants.sysName, new OctetString(notification));
  }

  @Test
  public void testSendTraps() throws IOException {
    Map<OID, Variable> map = new HashMap<>();
    map.put(new OID(".1.2.3.4.5.1"), new OctetString("value1"));
    map.put(new OID(".1.2.3.4.5.2"), new OctetString("value2"));
    map.put(new OID(".1.2.3.4.5.3"), new OctetString("value3"));

    SnmpAgentTrapV2 trapV2 = new SnmpAgentTrapV2(trapServerIp, trapServerPort);
    trapV2.setCommunity("public");
    trapV2.trap(map);
  }

  @Test
  public void testSendAlertItemTrap() throws IOException {
    AlertMessage alertMessage = generateAlertMessage();
    Map<OID, Variable> map = AlertMoTable.buildTrapMap(alertMessage, SnmpVersion.SNMPV2C);

    SnmpAgentTrapV2 trapV2 = new SnmpAgentTrapV2(trapServerIp, trapServerPort);
    trapV2.setCommunity("public");
    trapV2.trap(map);
  }

  @Override
  public void cleanUp() throws Exception {
    setLogLevel(originalLevel);
  }

  private AlertMessage generateAlertMessage() {
    AlertMessage alertMessage = new AlertMessage();
    alertMessage.setAlertAcknowledge(false);
    alertMessage.setSourceId("10.0.3.33");
    alertMessage.setSourceName("vm33");
    alertMessage.setAlertDescription("cpu occures alert");
    alertMessage.setAlertLevel("level1");
    alertMessage.setAlertType("type1");
    alertMessage.setAlertRuleName("CPU");
    return alertMessage;
  }

}
