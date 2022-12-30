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
import org.apache.log4j.Level;
import org.junit.Test;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import py.test.TestBase;

/**
 * xx.
 **/
public class SnmpAgentTrapV3Test extends TestBase {

  private Level originalLevel;

  @Override
  public void init() throws Exception {
    super.init();

    originalLevel = getLogLevel();
    setLogLevel(Level.DEBUG);
  }

  @Override
  public void cleanUp() throws Exception {
    setLogLevel(originalLevel);
  }

  @Test
  public void test() {
    SnmpAgentTrapV3 snmpAgentTrapV3 = new SnmpAgentTrapV3("127.0.0.1", 162, "user", AuthMD5.ID,
        "password",
        PrivDES.ID, "password", SecurityLevel.NOAUTH_NOPRIV);
    try {
      snmpAgentTrapV3.trap(new OID(".1.2.3.1.2.2.2.2"), new OctetString("hello..."));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
