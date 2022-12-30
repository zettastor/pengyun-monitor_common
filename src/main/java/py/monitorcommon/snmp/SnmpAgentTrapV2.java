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

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpAgentTrapV2 implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(SnmpAgentTrapV2.class);

  private final String trapServerAddr;
  private final Snmp snmp;

  private final String trapSysUpTime = "0";
  private String community;
  private int timeoutMs;

  public SnmpAgentTrapV2(String trapServerIp, int trapServerPort) {
    this.trapServerAddr = String.format("%s/%d", trapServerIp, trapServerPort);

    logger.warn("new snmp v2c trap agent. ip:{} port:{}", trapServerIp, trapServerPort);

    TransportMapping<?> transport = null;
    try {
      transport = new DefaultUdpTransportMapping();
      transport.listen();
    } catch (IOException e) {
      logger.warn("snmpAgentTrapV2 constructing failed.", e);
    }

    this.snmp = new Snmp(transport);
  }

  public static Map<OID, Variable> buildCommonTrapHeader(OID trapOid, String trapDesc) {
    Map<OID, Variable> trapMap = new HashMap<>();
    trapMap.put(SnmpConstants.snmpTrapOID, trapOid);
    //trapMap.put(SnmpConstants.sysDescr, new OctetString(trapDesc));
    return trapMap;
  }

  /**
   * This methods sends the V1 trap to the Localhost in port 162.
   */
  public void trap(OID oid, Variable value) throws IOException {
    Target target = getTarget();

    PDU pdu = createPdu();
    pdu.add(new VariableBinding(oid, value));

    logger.warn("Sending V2 Trap... ");
    snmp.send(pdu, target);
  }

  public void trap(Map<OID, Variable> variableBinds) throws IOException {
    logger.warn("prepare sending V2 Traps info:{} ", variableBinds);
    Target target = getTarget();

    PDU pdu = createPdu();

    for (Map.Entry<OID, Variable> entry : variableBinds.entrySet()) {
      OID oid = entry.getKey();
      Variable value = entry.getValue();
      pdu.add(new VariableBinding(oid, value));
    }

    // Send the PDU
    logger.warn("Sending V2 Traps... ");
    snmp.send(pdu, target);
  }

  @Override
  public void close() throws IOException {
    snmp.close();
  }

  private PDU createPdu() {
    // Create PDU for V2
    PDU pdu = new PDU();

    // need to specify the system up time
    pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(trapSysUpTime)));
    pdu.setType(PDU.TRAP);

    return pdu;
  }

  private Target getTarget() {
    Address targetAddress = GenericAddress.parse(trapServerAddr);

    CommunityTarget target = new CommunityTarget();
    target.setCommunity(new OctetString(community));
    target.setAddress(targetAddress);
    target.setRetries(2);
    target.setTimeout(timeoutMs);
    target.setVersion(SnmpConstants.version2c);
    target.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv2c);
    return target;
  }

  public void setCommunity(String community) {
    this.community = community;
  }

  public void setTimeoutMs(int timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  public String getTrapServerAddr() {
    return trapServerAddr;
  }
}
