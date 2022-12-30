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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

public class SnmpAgentShared extends BaseAgent {

  protected static final Logger logger = LoggerFactory.getLogger(SnmpAgentShared.class);

  protected String community;

  protected SnmpAgentShared(File bootCounterFile, File configFile,
      CommandProcessor commandProcessor) {
    super(bootCounterFile, configFile, commandProcessor);
  }

  /**
   * Adds community to security name mappings needed for SNMPv1 and SNMPv2c.
   */
  @Override
  @SuppressWarnings("rawtypes")
  protected void addCommunities(SnmpCommunityMIB communityMib) {
    logger.warn("addCommunities for snmp v2c");
    Variable[] com2sec = new Variable[]{new OctetString(community), new OctetString("cpublic"),
        // security name
        getAgent().getContextEngineID(), // local engine ID
        new OctetString("public"), // default context name
        new OctetString(), // transport tag
        new Integer32(StorageType.nonVolatile), // storage type
        new Integer32(RowStatus.active) // row status
    };
    MOTableRow row = communityMib.getSnmpCommunityEntry()
        .createRow(new OctetString("public2public").toSubIndex(true), com2sec);
    communityMib.getSnmpCommunityEntry().addRow((SnmpCommunityMIB.SnmpCommunityEntryRow) row);
  }

  /**
   * Adds initial VACM configuration.
   */
  @Override
  protected void addViews(VacmMIB vacm) {
    logger.warn("addViews for snmp v2c");
    vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString("cpublic"),
        new OctetString("v1v2group"),
        StorageType.nonVolatile);

    vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"),
        SecurityModel.SECURITY_MODEL_ANY,
        SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
        new OctetString("fullWriteView"), new OctetString("fullNotifyView"),
        StorageType.nonVolatile);

    vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"), new OctetString(),
        VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

    vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID("1.3"), new OctetString(),
        VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

    // for snmpV3
    //把用户加入安全组,并说明用户是V3用户
    //        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("enocsnmpv3"), 
    //        new OctetString("v3group"),
    //                StorageType.permanent);
    //
    //        vacm.addAccess(new OctetString("v3group"), new OctetString(),//ghu add public
    //                SecurityModel.SECURITY_MODEL_USM, SecurityLevel.AUTH_PRIV,
    //                //设置组的安全级别
    //                MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"), 
    //                new OctetString("fullWriteView"),
    //                new OctetString("fullNotifyView"), StorageType.nonVolatile);
    //
    //        vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"), 
    //        new OctetString(),
    //                VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
    //
    //        vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID("1.3"), 
    //        new OctetString(),
    //                VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
    //
    //        vacm.addViewTreeFamily(new OctetString("fullNotifyView"), new OID("1.3"), 
    //        new OctetString(),
    //                VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
  }

  /**
   * Adds initial notification targets and filters.
   */
  @Override
  protected void addNotificationTargets(SnmpTargetMIB arg0, SnmpNotificationMIB arg1) {
    logger.warn("addNotificationTargets for snmp v2c");
    // TODO Auto-generated method stub
  }

  /**
   * Adds all the necessary initial users to the USM.
   */
  @Override
  protected void addUsmUser(USM arg0) {
    logger.warn("addUsmUser for snmp v2c");
    //        UsmUser user = new UsmUser(new OctetString("enocsnmpv3"), //账户名
    //                AuthMD5.ID,                    //加密协议
    //                new OctetString("enocsnmpv3pw"), //密码
    ////                PrivAES128.ID,
    //                PrivDES.ID,                      //私有协议类型
    //                new OctetString("enocsnmpv3pk")      //私有密码
    //        );
    //        usm.addUser(user.getSecurityName(), null, user);
    //        usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
  }

  /**
   * Unregister the basic MIB modules from the agent's MOServer.
   */
  @Override
  protected void unregisterManagedObjects() {
    logger.warn("unregisterManagedObjects for snmp v2c");
    // TODO Auto-generated method stub
  }

  /**
   * Register additional managed objects at the agent's server.
   */
  @Override
  protected void registerManagedObjects() {
    logger.warn("registerManagedObjects for snmp v2c");
    // TODO Auto-generated method stub
  }

  /**
   * Clients can register the MO they need.
   */
  public void registerManagedObject(ManagedObject mo) {
    logger.warn("registerManagedObject for snmp v2c");
    try {
      server.register(mo, null);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void unregisterManagedObject(MOGroup moGroup) {
    logger.warn("unregisterManagedObject for snmp v2c");
    moGroup.unregisterMOs(server, getContext(moGroup));
  }
}
