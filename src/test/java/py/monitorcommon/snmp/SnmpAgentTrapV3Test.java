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
