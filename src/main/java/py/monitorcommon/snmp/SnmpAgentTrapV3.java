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
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * about snmp v3 trap.
 *
 **/
public class SnmpAgentTrapV3 implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(SnmpAgentTrapV3.class);
  private final String trapSysUpTime = "0";
  private Snmp snmp = null;
  private Address targetAddress = null;
  private String username;
  private OID authenticationProtocol; //AuthMD5.ID, AuthSHA.ID
  private String authenticationPassword;
  private OID privacyProtocol;  //PrivDES.ID, PrivAES128.ID
  private String privacyPassword;
  private int securityLevel;  //SecurityLevel.AUTH_PRIV
  private int timeoutMs;

  public SnmpAgentTrapV3(String trapServerIp, int trapServerPort, String username,
      OID authenticationProtocol,
      String authenticationPassword, OID privacyProtocol, String privacyPassword,
      int securityLevel) {
    targetAddress = GenericAddress.parse(String.format("udp:%s/%d", trapServerIp, trapServerPort));
    this.username = username;
    this.authenticationProtocol = authenticationProtocol;
    this.authenticationPassword = authenticationPassword;
    this.privacyProtocol = privacyProtocol;
    this.privacyPassword = privacyPassword;
    this.securityLevel = securityLevel;

    logger.warn(
        "new snmp v3 trap agent. ip:{}, port:{}, username:{}, authenticationProtocol:{}, "
            + "authenticationPassword:{} privacyProtocol:{}, privacyPassword:{} securityLevel:{}",
        trapServerIp, trapServerPort, username, authenticationProtocol, authenticationPassword,
        privacyProtocol, privacyPassword, securityLevel);

    TransportMapping<UdpAddress> transport = null;
    try {
      transport = new DefaultUdpTransportMapping();
      snmp = new Snmp(transport);
      createUsmUser(securityLevel);
      transport.listen();
    } catch (IOException e) {
      logger.warn("snmpAgentTrapV3 constructing failed.", e);
    }
  }

  public static Map<OID, Variable> buildCommonTrapHeader(OID trapOid, String trapDesc) {
    Map<OID, Variable> trapMap = new HashMap<>();
    trapMap.put(SnmpConstants.snmpTrapOID, trapOid);
    trapMap.put(SnmpConstants.sysDescr, new OctetString(trapDesc));
    return trapMap;
  }

  public void trap(OID oid, Variable value) throws IOException {
    Target userTarget = getTarget();

    PDU pdu = createPdu();
    pdu.add(new VariableBinding(oid, value));

    logger.warn("Sending V3 Trap... ");
    snmp.send(pdu, userTarget);
  }

  public void trap(Map<OID, Variable> variableBinds) throws IOException {
    Target userTarget = getTarget();

    PDU pdu = createPdu();

    for (Map.Entry<OID, Variable> entry : variableBinds.entrySet()) {
      OID oid = entry.getKey();
      Variable value = entry.getValue();
      pdu.add(new VariableBinding(oid, value));
    }

    // Send the PDU
    logger.warn("Sending V3 Traps... ");
    snmp.send(pdu, userTarget);
  }

  private void createUsmUser(int securityLevel) {
    USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()),
        0);
    SecurityModels.getInstance().addSecurityModel(usm);

    //Creating USM user according to Security level

    if (securityLevel == SecurityLevel.NOAUTH_NOPRIV) {
      snmp.getUSM()
          .addUser(new OctetString(username),
              new UsmUser(new OctetString(username), null, null, null, null));
    }

    if (securityLevel == SecurityLevel.AUTH_NOPRIV) {
      snmp.getUSM().addUser(new OctetString(username),
          new UsmUser(new OctetString(username), new OID(authenticationProtocol),
              new OctetString(authenticationPassword), null, null));
    }

    if (securityLevel == SecurityLevel.AUTH_PRIV) {
      snmp.getUSM().addUser(new OctetString(username),
          new UsmUser(new OctetString(username), new OID(authenticationProtocol),
              new OctetString(authenticationPassword), new OID(privacyProtocol),
              new OctetString(privacyPassword)));
    }
  }

  private PDU createPdu() {
    ScopedPDU pdu = new ScopedPDU();
    pdu.setType(PDU.TRAP);
    pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(trapSysUpTime)));
    return pdu;
  }

  private UserTarget getTarget() {
    UserTarget target = new UserTarget();
    target.setSecurityName(new OctetString(username));
    target.setVersion(SnmpConstants.version3);
    target.setSecurityLevel(securityLevel);
    target.setAddress(targetAddress);
    target.setTimeout(timeoutMs);
    return target;
  }

  @Override
  public void close() throws IOException {
    snmp.close();
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setAuthenticationProtocol(OID authenticationProtocol) {
    this.authenticationProtocol = authenticationProtocol;
  }

  public void setAuthenticationPassword(String authenticationPassword) {
    this.authenticationPassword = authenticationPassword;
  }

  public void setPrivacyProtocol(OID privacyProtocol) {
    this.privacyProtocol = privacyProtocol;
  }

  public void setPrivacyPassword(String privacyPassword) {
    this.privacyPassword = privacyPassword;
  }

  public void setSecurityLevel(int securityLevel) {
    this.securityLevel = securityLevel;
  }

  public void setTimeoutMs(int timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  @Override
  public String toString() {
    return "SnmpAgentTrapV3{" + "targetAddress=" + targetAddress + ", username='" + username + '\''
        + ", authenticationProtocol=" + authenticationProtocol + ", authenticationPassword='"
        + authenticationPassword + '\'' + ", privacyProtocol=" + privacyProtocol
        + ", privacyPassword='"
        + privacyPassword + '\'' + ", securityLevel=" + securityLevel + '}';
  }
}
