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

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

public class SnmpAgentV3Shared extends BaseAgent {

  protected static final Logger logger = LoggerFactory.getLogger(SnmpAgentV3Shared.class);
  protected String username;
  protected OID authenticationProtocol; //AuthMD5.ID, AuthSHA.ID
  protected OctetString authenticationPassword;
  protected OID privacyProtocol;  //PrivDES.ID, PrivAES128.ID
  protected OctetString privacyPassword;
  protected int securityLevel;  //SecurityLevel.AUTH_PRIV

  protected SnmpAgentV3Shared(File bootCounterFile, File configFile,
      CommandProcessor commandProcessor) {
    super(bootCounterFile, configFile, commandProcessor);
  }

  /**
   * Adds community to security name mappings needed for SNMPv1 and SNMPv2c.
   */
  @Override
  @SuppressWarnings("rawtypes")
  protected void addCommunities(SnmpCommunityMIB communityMib) {
    logger.warn("addCommunities for snmp v3");
  }

  /**
   * Adds initial VACM configuration.
   */
  @Override
  protected void addViews(VacmMIB vacm) {
    logger.warn("addViews for snmp v3");

    // for snmpV3
    vacm.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString(username),
        new OctetString("v3group"),
        StorageType.permanent);
    //ghu add public
    vacm.addAccess(new OctetString("v3group"), new OctetString(),
        SecurityModel.SECURITY_MODEL_USM, securityLevel,
        MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
        new OctetString("fullWriteView"),
        new OctetString("fullNotifyView"), StorageType.nonVolatile);

    vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"), new OctetString(),
        VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

    vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID("1.3"), new OctetString(),
        VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

    vacm.addViewTreeFamily(new OctetString("fullNotifyView"), new OID("1.3"), new OctetString(),
        VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
  }

  /**
   * Adds initial notification targets and filters.
   */
  @Override
  protected void addNotificationTargets(SnmpTargetMIB arg0, SnmpNotificationMIB arg1) {
    logger.warn("addNotificationTargets for snmp v3");
    // TODO Auto-generated method stub
  }

  /**
   * Adds all the necessary initial users to the USM.
   */
  @Override
  protected void addUsmUser(USM arg0) {
    logger.warn("addUsmUser for snmp v3");
    UsmUser user = new UsmUser(new OctetString(username),
        authenticationProtocol,
        authenticationPassword,
        privacyProtocol,
        privacyPassword
    );
    //        usm.addUser(user.getSecurityName(), null, user);
    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
  }

  /**
   * Unregister the basic MIB modules from the agent's MOServer.
   */
  @Override
  protected void unregisterManagedObjects() {
    logger.warn("unregisterManagedObjects for snmp v3");
    // TODO Auto-generated method stub
  }

  /**
   * Register additional managed objects at the agent's server.
   */
  @Override
  protected void registerManagedObjects() {
    logger.warn("registerManagedObjects for snmp v3");
    // TODO Auto-generated method stub
  }

  /**
   * Start method invokes some initialization methods needed to start the agent.
   *
   * @throws IOException IOException
   */
  public void start() throws IOException {
    logger.warn("start snmp v3 agent");
    init();

    // This method reads some old config from a file and causes
    // unexpected behavior.
    // loadConfig(ImportModes.REPLACE_CREATE);
    addShutdownHook();
    getServer().addContext(new OctetString("public"));
    finishInit();
    run();
    sendColdStartNotification();
  }

  /**
   * Clients can register the MO they need.
   */
  public void registerManagedObject(ManagedObject mo) {
    logger.warn("registerManagedObject for snmp v3");
    try {
      server.register(mo, null);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void unregisterManagedObject(MOGroup moGroup) {
    logger.warn("unregisterManagedObject for snmp v3");
    moGroup.unregisterMOs(server, getContext(moGroup));
  }
}
