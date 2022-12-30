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

package py.monitorcommon.task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import net.sf.json.JSONArray;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivDES;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import py.app.context.AppContext;
import py.app.context.AppContextImpl;
import py.informationcenter.Utils;
import py.instance.InstanceStatus;
import py.monitor.common.AlertLevel;
import py.monitor.common.AlertMessage;
import py.monitor.common.AlertMessageKey;
import py.monitor.common.AlertRule;
import py.monitor.common.AlertType;
import py.monitor.common.CounterName;
import py.monitor.common.DtoApiReponse;
import py.monitor.common.DtoSenderLog;
import py.monitor.common.DtoUser;
import py.monitor.common.MonitorPlatformDataDto;
import py.monitor.common.QueueManager;
import py.monitor.common.SendNoticeDto;
import py.monitor.common.SendNoticeJobNumDto;
import py.monitor.common.SnmpForwardItem;
import py.monitor.utils.HttpClientUtils;
import py.monitorcommon.dao.AlertMessageDao;
import py.monitorcommon.dao.AlertRuleDao;
import py.monitorcommon.dao.DtoSendLogDao;
import py.monitorcommon.dao.DtoUserDao;
import py.monitorcommon.dao.EventLogInfoDao;
import py.monitorcommon.dao.SnmpForwardItemDao;
import py.monitorcommon.manager.MemoryAlertMessageManager;
import py.monitorcommon.smtp.SmtpSendEmail;
import py.monitorcommon.snmp.AlertMoTable;
import py.monitorcommon.snmp.SnmpAgentTrapV2;
import py.monitorcommon.snmp.SnmpAgentTrapV3;
import py.monitorcommon.websocketserver.WebSocketServer;
import py.thrift.monitorserver.service.AuthProtocol;
import py.thrift.monitorserver.service.PrivProtocol;
import py.thrift.monitorserver.service.SecurityLevel;
import py.thrift.monitorserver.service.SnmpVersion;

public class AlertMessageFilterByTimeTask extends BaseTask {

  private static final Logger logger = LoggerFactory.getLogger(AlertMessageFilterByTimeTask.class);

  private AlertMessageDao alertMessageDao;
  private WebSocketServer webSocketServer;
  private SnmpForwardItemDao snmpForwardItemDao;
  private EventLogInfoDao eventLogInfoDao;
  private SmtpSendEmail smtpSendEmail;
  private int maxEventLogCount;
  private int repeatAlertTimeMs;
  private boolean needSendDto;
  private String jobToken;
  private String jobUrl;
  private String signalToken;
  private String signalUrl;
  private String appId;
  private String system;
  private String extension;
  private String systemType;
  private int socketTimeout;
  private DtoUserDao dtoUserDao;
  private AlertRuleDao alertRuleDao;
  private DtoSendLogDao dtoSendLogDao;

  private MemoryAlertMessageManager memoryAlertMessageManager;
  private AppContextImpl appContext;
  private Map<String, String> counterKeyLock;

  public static final String SMS = "SMS";
  public static final String EMAIL = "EMAIL";
  public static final String LINK = "LINK";
  public static final String MESSAGE_FOOT = "告警业务系统：zettastor分布式数据服务";

  public AlertMessageFilterByTimeTask(QueueManager queueManager, AlertMessageDao alertMessageDao,
      WebSocketServer webSocketServer, MemoryAlertMessageManager memoryAlertMessageManager,
      AppContext appContext) {
    super(queueManager);
    this.alertMessageDao = alertMessageDao;
    this.webSocketServer = webSocketServer;
    this.memoryAlertMessageManager = memoryAlertMessageManager;
    this.appContext = (AppContextImpl) appContext;
    this.counterKeyLock = new HashMap<>();
    for (CounterName counterKey : CounterName.values()) {
      counterKeyLock.put(counterKey.name(), counterKey.name());
    }
  }

  @Override
  public void startJob() {
    long lastTime = System.currentTimeMillis();
    long currentTime = 0;
    LinkedBlockingQueue<AlertMessage> alertQueue = queueManager.getAlertQueueFilterByTime();
    while (!isThreadPoolExecutorStop) {
      final AlertMessage alertMessage;
      try {
        if (logger.isInfoEnabled()) {
          currentTime = System.currentTimeMillis();
          if (currentTime - lastTime > 60000) {
            lastTime = currentTime;
            logger.warn("queue length:filter by time task has message:{}", alertQueue.size());
            logger.warn("queue length:filter by time task pool queue size:{}. active count:{}",
                poolExecutor.getQueue().size(), poolExecutor.getActiveCount());
            logger.warn("memory last alert message length:{}",
                memoryAlertMessageManager.memoryAlertLength());
          }
        }
        alertMessage = alertQueue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      poolExecutor.execute(() -> {
        doWorkWithClear(alertMessage);
        logger.debug("runnableQueue size in timeFilterTask is: {}", runnableQueue.size());
      });
    }
  }

  public void doWorkWithClear(AlertMessage alertMessage) {
    logger.debug("alertMessage filter by time task : {}", alertMessage);
    Validate.notNull(alertMessage);

    synchronized (counterKeyLock.get(alertMessage.getCounterKey())) {
      AlertMessageKey alertMessageKey = alertMessage.getAlertMessageKey();
      AlertMessage lastAlertMessage = memoryAlertMessageManager.getLastAlertMessage(
          alertMessageKey.getFuzzyAlertMessageKey());

      logger.debug("lastAlertMessage is: {}", lastAlertMessage);

      if (lastAlertMessage == null) {
        if (appContext.getStatus() == InstanceStatus.SUSPEND) {
          // send to OK monitor, and insert it into ok monitor server's memory
          // todo
          logger.warn("now i am suspend. but found alert message:{}", alertMessage);
        }

        sendAndSaveAlertMessage(alertMessage, alertMessageKey, appContext.getStatus());

        // put alert message into memory
        memoryAlertMessageManager.putMemoryAlert(alertMessage);

        if (alertMessage.isNetSubHealthAlert()) {
          logger.warn("node:{} net card is sub health alert ,"
                  + " now put it into net sub health queue. event data:{}",
              alertMessage.getSourceName(), alertMessage.getLatestEventDataInfo());
          queueManager.getNetSubHealthQueue().offer(alertMessage);
        }
      } else if (alertMessage.getLastAlertTime() - lastAlertMessage.getLastAlertTime()
          > repeatAlertTimeMs) {
        if (!updateLastAlertMessage(alertMessageKey.getFuzzyAlertMessageKey(), alertMessage,
            lastAlertMessage)) {
          logger.warn("updateLastAlertMessage failed. so we need re-save a new alert:{}",
              alertMessage);
          removeLastAlertMessageKey(alertMessageKey);
          sendAndSaveAlertMessage(alertMessage, alertMessageKey, appContext.getStatus());
        }

        if (alertMessage.isNetSubHealthAlert()) {
          logger.warn("node:{} net card is sub health alert, "
                  + "now put it into net sub health queue. event data:{}",
              alertMessage.getSourceName(), alertMessage.getLatestEventDataInfo());
          queueManager.getNetSubHealthQueue().offer(alertMessage);
        }
      } else {
        logger.info("repeat alert message, alert message is: {}", alertMessage);
      }
    }
  }

  public boolean updateLastAlertMessage(AlertMessageKey key, AlertMessage alertMessage,
      AlertMessage lastAlertMessage) {
    logger.debug("will update alert message:{}:{}", alertMessage.getSourceId(),
        alertMessage.getCounterKey());
    lastAlertMessage.incAlertFrequency();

    AlertMessage lastAlertMessageInDb = alertMessageDao
        .getAlertMessageById(lastAlertMessage.getId());
    if (lastAlertMessageInDb == null) {
      logger.warn("cannot get alert message from db:{}. will new alert message:{}",
          lastAlertMessage, alertMessage);
      return false;
    }

    int frequency = lastAlertMessage.getAlertFrequency();
    long lastAlertTime = Math
        .max(alertMessage.getLastAlertTime(), lastAlertMessage.getLastAlertTime());
    Set<Long> removeEventLogInfoIdSet = alertMessageDao
        .updateLastAlertById(lastAlertMessage.getId(), lastAlertTime, frequency,
            alertMessage.getEventLogInfoSet(), maxEventLogCount);

    //update last alert time
    memoryAlertMessageManager.updateLastAlertMessage(key, alertMessage);

    //if it is net sub health alert, and this alert is full source key alert
    if (alertMessage.isNetSubHealthAlert() && !alertMessage.getAlertMessageKey()
        .isFuzzySourceKey()) {
      logger.info("net sub health, update fuzzy alert message to full alert message:{}",
          alertMessage);
      alertMessageDao.updateAlertFromFuzzyToFullById(lastAlertMessage.getId(), alertMessage);
    }

    logger.info("remove eventLogInfoID set is: {}", removeEventLogInfoIdSet);
    for (Long id : removeEventLogInfoIdSet) {
      try {
        eventLogInfoDao.deleteById(id);
      } catch (Exception e) {
        logger.info("this event may be used by other alert, eventId is: {}", id);
      }
    }
    return true;
  }

  public void sendAndSaveAlertMessage(AlertMessage alertMessage, AlertMessageKey alertMessageKey,
      InstanceStatus instanceStatus) {
    logger.warn("generate alertMessage: {}", alertMessage);

    if (memoryAlertMessageManager.putIfAbsentLastAlertMessage(
        alertMessageKey.getFuzzyAlertMessageKey(), alertMessage) != null) {
      logger.warn("alertMessage has existed, alertMessage is: {}", alertMessage);
      return;
    }

    if (instanceStatus == InstanceStatus.HEALTHY) {
      alertMessageDao.saveOrUpdateAlertMessage(alertMessage);
    } else {
      // if suspend, cannot alert directly, need check alert time
      alertMessageDao.saveAlertMessageWithCheck(alertMessage);
    }

    try {
      webSocketServer.write(alertMessage);
    } catch (Exception e) {
      logger.warn("caught an exception when send alertMessage by websocket: ", e);
    }

    try {
      sendToNms(alertMessage);
    } catch (Exception e) {
      logger.warn("caught an exception when trap alertMessage by snmp : ", e);
    }

    if (needSendDto) {
      try {
        sendToDto(alertMessage, "0");
      } catch (Exception e) {
        logger.warn("caught an exception when trap alertMessage when send to DTO : ", e);
      }
    }

    try {
      sendToSmtp(alertMessage);
    } catch (Exception e) {
      logger.warn("caught an exception when send alertMessage by email: ", e);
    }
  }

  public void sendToNms(AlertMessage alertMessage) {
    logger.warn("begin send to SNMP, alertMessage: {}", alertMessage);
    List<SnmpForwardItem> snmpForwardItems;
    try {
      snmpForwardItems = snmpForwardItemDao.listEnabledForwardSnmp();
    } catch (Exception e) {
      logger.warn("caught an exception: ", e);
      return;
    }
    Validate.isTrue(snmpForwardItems.size() <= 1);
    if (snmpForwardItems.size() == 0) {
      logger.warn("no snmp NMS is set.");
      return;
    }

    SnmpForwardItem snmpForwardItem = snmpForwardItems.get(0);
    logger.warn("snmp forward item is: {}", snmpForwardItem);

    SnmpVersion snmpVersion = SnmpVersion.valueOf(snmpForwardItem.getSnmpVersion());
    Map<OID, Variable> trapMap = AlertMoTable.buildTrapMap(alertMessage, SnmpVersion.SNMPV2C);
    switch (snmpVersion) {
      case SNMPV2C:
        SnmpAgentTrapV2 trapV2 = new SnmpAgentTrapV2(snmpForwardItem.getTrapServerip(),
            snmpForwardItem.getTrapServerport());
        trapV2.setCommunity(snmpForwardItem.getCommunity());
        trapV2.setTimeoutMs(snmpForwardItem.getTimeoutMs());
        try {
          trapV2.trap(trapMap);
          logger.warn("trapV2 to NMS success, trapServerAddr:{}, alertMessage:{}",
              trapV2.getTrapServerAddr(),
              alertMessage);
        } catch (IOException e) {
          logger.warn("caught an exception when trapV2 to NMS.", e);
        }
        break;
      case SNMPV3:
        int securityLevel = 0;
        OID authenticationProtocol = null;
        OID privacyProtocol = null;

        switch (SecurityLevel.valueOf(snmpForwardItem.getSecurityLevel())) {
          case NoAuthNoPriv:
            securityLevel = org.snmp4j.security.SecurityLevel.NOAUTH_NOPRIV;
            break;
          case AuthNoPriv:
            securityLevel = org.snmp4j.security.SecurityLevel.AUTH_NOPRIV;
            break;
          case AuthPriv:
            securityLevel = org.snmp4j.security.SecurityLevel.AUTH_PRIV;
            break;
          default:
            break;
        }

        if (snmpForwardItem.getAuthProtocol() != null) {
          try {
            AuthProtocol authProtocol = AuthProtocol.valueOf(snmpForwardItem.getAuthProtocol());
            switch (authProtocol) {
              case MD5:
                authenticationProtocol = AuthMD5.ID;
                break;
              case SHA:
                authenticationProtocol = AuthSHA.ID;
                break;
              default:
                break;
            }
          } catch (Exception e) {
            logger.warn("caught an exception:", e);
          }
        }

        if (snmpForwardItem.getPrivProtocol() != null) {
          try {
            PrivProtocol privProtocol = PrivProtocol.valueOf(snmpForwardItem.getPrivProtocol());
            switch (privProtocol) {
              case DES:
                privacyProtocol = PrivDES.ID;
                break;
              case AES:
                privacyProtocol = PrivAES128.ID;
                break;
              default:
                break;
            }
          } catch (Exception e) {
            logger.warn("caught an exception:", e);
          }
        }

        SnmpAgentTrapV3 trapV3 = new SnmpAgentTrapV3(snmpForwardItem.getTrapServerip(),
            snmpForwardItem.getTrapServerport(), snmpForwardItem.getSecurityName(),
            authenticationProtocol,
            snmpForwardItem.getAuthKey(), privacyProtocol, snmpForwardItem.getPrivKey(),
            securityLevel);
        trapV3.setTimeoutMs(snmpForwardItem.getTimeoutMs());

        try {
          trapV3.trap(trapMap);
          logger.warn("trapV3 to NMS, trapV3Info:{}, alertMessage:{}", trapV3, alertMessage);
        } catch (IOException e) {
          logger.warn("caught an exception when trapV3 to NMS.", e);
        }

        break;
      default:
        logger.warn("snmpforward info is error.");
    }
  }

  public void sendToDto(AlertMessage alertMessage, String eventType) throws Exception {
    logger.warn("begin send to DTO, alertMessage: {}", alertMessage);
    HttpClientUtils httpClientUtils = new HttpClientUtils(socketTimeout);

    String systemTypeNew = new String(systemType.getBytes(), StandardCharsets.UTF_8);
    logger.info(
        "dto systemType : {}, systemTypeEncode :{} , systemTypeNew: {}",
        systemType, Utils.getEncoding(systemType), systemTypeNew);
    SendNoticeJobNumDto sendNoticeJob = new SendNoticeJobNumDto();
    sendNoticeJob.setAppId(appId);
    sendNoticeJob.setNoticeLevel(getNoticeLevel(alertMessage.getAlertLevel()));
    sendNoticeJob.setEmailHtmlFlag(false);
    MonitorPlatformDataDto mpd = new MonitorPlatformDataDto();
    mpd.setSendMessage(false);
    mpd.setAlarmSource(alertMessage.getSourceName());
    mpd.setAlarmMetric(alertMessage.getCounterKey());
    mpd.setAlarmTag("monitor_server");
    mpd.setAlarmLevel(getAlarmLevel(alertMessage.getAlertLevel()));
    mpd.setEventType(eventType);
    mpd.setAlarmTime(alertMessage.getLastAlertTime() + "");
    mpd.setSystem(system);
    if (null != alertMessage.getLatestEventDataInfo()) {
      mpd.setAlarmSysCode(alertMessage.getLatestEventDataInfo().getProgram());
    }
    mpd.setAlarmName(alertMessage.getAlertRuleName());
    mpd.setAlarmValue(alertMessage.getLastActualValue());
    List<AlertRule> rule = alertRuleDao.getAlertRuleByName(alertMessage.getAlertRuleName());

    if (rule.size() == 1) {
      if (rule.get(0).getAlertLevelOne().equals(alertMessage.getAlertLevel())) {
        mpd.setAlarmThreshold(rule.get(0).getAlertLevelOneThreshold() + "");
      } else if (rule.get(0).getAlertLevelTwo().equals(alertMessage.getAlertLevel())) {
        mpd.setAlarmThreshold(rule.get(0).getAlertLevelTwoThreshold() + "");
      }
    } else if (rule.size() > 1) {
      throw new Exception("exist same rule: " + alertMessage.getAlertRuleName());
    } else {
      throw new Exception("not found the rule " + alertMessage.getAlertRuleName());
    }
    String message = buildMessage(alertMessage, eventType, rule.get(0), mpd);
    mpd.setAlarmInfo(
        alertMessage.getSourceName() + " " + rule.get(0).getDescription() + "，请关注");

    Map<String, Object> extensionMap = new HashMap<>();
    String eventTypeCn = eventType.equals("0") ? "告警" : "恢复";
    String ruleObject = CounterName.valueOf(rule.get(0).getCounterKey()).getCounterNameCn();
    String subject =
        eventTypeCn + "-【" + systemTypeNew + "】" + alertMessage.getSourceName() + " " + ruleObject
            + "异常";
    String encode = Utils.getEncoding(extension);
    String extensionNew = new String(extension.getBytes(encode), StandardCharsets.UTF_8);
    extensionMap.put(extensionNew.split(":")[0], subject);
    try {
      List<DtoUser> dtoUsers = dtoUserDao.listUsers();
      for (DtoUser user : dtoUsers) {
        if (!user.isFlag()) {
          continue;
        }
        long sendTime = 0;
        List<String> jobNumReceiver = new ArrayList<>();
        jobNumReceiver.add(user.getJobNum());
        Map<String, Object> jobHeaders = new HashMap<>();
        jobHeaders.put("token", jobToken);
        Map<String, Object> signalHeaders = new HashMap<>();
        signalHeaders.put("token", signalToken);
        List<String> receiver = new ArrayList<>();

        boolean jobNumExist = !StringUtils.isEmpty(user.getJobNum());
        StringBuilder response = new StringBuilder();
        if (user.isEnableSms()) {
          sendNoticeJob.setType(SMS);
          sendNoticeJob.setMessage(message);
          mpd.setSms(user.getSms());
          sendNoticeJob.setMonitorPlatformDataDto(mpd);
          List<SendNoticeDto> sendNoticeList = new ArrayList<>();
          boolean sendResult = false;
          if (!StringUtils.isEmpty(user.getSms())) {
            receiver.add(user.getSms());
            sendNoticeJob.setReceiverList(receiver);
            sendNoticeList.add(sendNoticeJob);
            sendTime = System.currentTimeMillis();
            httpClientUtils.doPostParam(signalUrl, signalHeaders,
                JSONArray.fromObject(sendNoticeList).toString(), response);
            sendResult = getSendToDtoStatus(response);
          }
          if (!sendResult && jobNumExist) {
            sendNoticeJob.setReceiverList(jobNumReceiver);
            sendNoticeList.clear();
            sendNoticeList.add(sendNoticeJob);
            sendTime = System.currentTimeMillis();
            httpClientUtils.doPostParam(jobUrl, jobHeaders,
                JSONArray.fromObject(sendNoticeList).toString(), response);
            sendResult = getSendToDtoStatus(response);
          }
          dtoReponseForLog(sendResult, sendNoticeJob, user, sendTime);
        }
        if (user.isEnableEmail()) {
          sendNoticeJob.setExtension(extensionMap);
          sendNoticeJob.setType(EMAIL);
          sendNoticeJob.setMessage(message);
          mpd.setEmail(user.getEmail());
          sendNoticeJob.setMonitorPlatformDataDto(mpd);
          List<SendNoticeJobNumDto> sendNoticeList = new ArrayList<>();
          boolean sendResult = false;
          if (!StringUtils.isEmpty(user.getEmail())) {
            receiver.clear();
            receiver.add(user.getEmail());
            sendNoticeJob.setReceiverList(receiver);
            sendNoticeList.add(sendNoticeJob);
            sendTime = System.currentTimeMillis();
            httpClientUtils.doPostParam(signalUrl, signalHeaders,
                JSONArray.fromObject(sendNoticeList).toString(), response);
            sendResult = getSendToDtoStatus(response);
          }
          if (!sendResult && jobNumExist) {
            sendNoticeList.clear();
            sendNoticeJob.setReceiverList(jobNumReceiver);
            sendNoticeList.add(sendNoticeJob);
            sendTime = System.currentTimeMillis();
            httpClientUtils.doPostParam(jobUrl, jobHeaders,
                JSONArray.fromObject(sendNoticeList).toString(), response);
            sendResult = getSendToDtoStatus(response);
          }
          dtoReponseForLog(sendResult, sendNoticeJob, user, sendTime);
        }
        if (user.isEnableLink()) {
          sendNoticeJob.setExtension(extensionMap);
          sendNoticeJob.setType(LINK);
          sendNoticeJob.setMessage(message);
          mpd.setLink(user.getLink());
          sendNoticeJob.setMonitorPlatformDataDto(mpd);
          List<SendNoticeJobNumDto> sendNoticeList = new ArrayList<>();
          boolean sendResult = false;
          if (!StringUtils.isEmpty(user.getLink())) {
            receiver.clear();
            receiver.add(user.getLink());
            sendNoticeJob.setReceiverList(receiver);
            sendNoticeList.add(sendNoticeJob);
            sendTime = System.currentTimeMillis();
            httpClientUtils.doPostParam(signalUrl, signalHeaders,
                JSONArray.fromObject(sendNoticeList).toString(), response);
            sendResult = getSendToDtoStatus(response);
          }
          if (!sendResult && jobNumExist) {
            sendNoticeList.clear();
            sendNoticeJob.setReceiverList(jobNumReceiver);
            sendNoticeList.add(sendNoticeJob);
            sendTime = System.currentTimeMillis();
            httpClientUtils.doPostParam(jobUrl, jobHeaders,
                JSONArray.fromObject(sendNoticeList).toString(), response);
            sendResult = getSendToDtoStatus(response);
          }
          dtoReponseForLog(sendResult, sendNoticeJob, user, sendTime);
        }
      }
    } catch (Exception e) {
      logger.warn("caught an exception when send to DTO.", e);
    }
  }

  private String getNoticeLevel(String level) {
    switch (level) {
      case "CRITICAL":
      case "MAJOR":
        return "URGENT";
      case "MINOR":
      case "WARNING":
        return "COMMON";
      default:
        logger.warn("AlarmLevel error");
        return null;
    }
  }

  private String getAlarmLevel(String level) {
    switch (level) {
      case "CRITICAL":
        return "0";
      case "MAJOR":
        return "1";
      case "MINOR":
        return "2";
      case "WARNING":
        return "6";
      default:
        logger.warn("AlarmLevel error");
        return null;
    }
  }

  public void sendToSmtp(AlertMessage alertMessage) {
    logger.debug("begin send to Email, alertMessage: {}", alertMessage);
    try {
      AlertMessage convertAlertMessage = convertAlertMessage(alertMessage);
      smtpSendEmail.sendEmail(convertAlertMessage);
    } catch (Exception e) {
      logger.warn("caught an exception when send email.", e);
    }
  }

  private void dtoReponseForLog(boolean result, SendNoticeDto sendNoticeList,
      DtoUser user, long sendTime) {
    DtoSenderLog dtoSenderLog = new DtoSenderLog();
    dtoSenderLog.setResult(result);
    dtoSenderLog.setUserName(user.getUserName());
    dtoSenderLog.setJobNum(user.getJobNum());
    dtoSenderLog.setType(sendNoticeList.getType());
    dtoSenderLog.setSendTime(sendTime);
    MonitorPlatformDataDto mpd = sendNoticeList.getMonitorPlatformDataDto();
    if (null != mpd) {
      dtoSenderLog.setEventType(mpd.getEventType().equals("0") ? "告警" : "恢复");
    }
    dtoSenderLog.setSendNotice(sendNoticeList.toString().replace("\n", ","));
    dtoSendLogDao.saveDtoSendLog(dtoSenderLog);
  }

  private boolean getSendToDtoStatus(StringBuilder response) throws Exception {
    logger.warn("response:{} from DTO API", response);
    DtoApiReponse dtoApiReponse = Utils.jsonToObj(response.toString(), DtoApiReponse.class);
    if (null != dtoApiReponse) {
      if (dtoApiReponse.getCode() == 10000) {
        logger.warn("send to DTO API success");
        return true;
      } else if (dtoApiReponse.getCode() == 10001) {
        logger.warn("send to DTO error, No personnel information found.\nmsg: {}",
            dtoApiReponse.getMsg());
        return false;
      } else if (dtoApiReponse.getCode() == 10002) {
        logger.warn(
            "send to DTO error, The system is abnormal. Please try again later.\nmsg: {}",
            dtoApiReponse.getMsg());
        return false;
      } else {
        logger.error("send to DTO user failed, unknown error code:{}, message:{}",
            dtoApiReponse.getCode(), dtoApiReponse.getMsg());
        return false;
      }
    } else {
      throw new Exception("response from DTO API is illegal");
    }
  }

  private AlertMessage convertAlertMessage(AlertMessage alertMessage) {
    AlertMessage alertMessageCn = new AlertMessage();
    alertMessageCn.setSourceId(alertMessage.getSourceId());
    alertMessageCn.setSourceName(alertMessage.getSourceName());
    alertMessageCn.setAlertRuleName(alertMessage.getAlertRuleName());
    alertMessageCn.setAlertDescription(alertMessage.getAlertDescription());
    alertMessageCn.setAlertAcknowledge(alertMessage.isAlertAcknowledge());
    alertMessageCn.setFirstAlertTime(alertMessage.getFirstAlertTime());
    alertMessageCn.setLastAlertTime(alertMessage.getLastAlertTime());
    alertMessageCn.setAlertType(AlertType.valueOf(alertMessage.getAlertType()).getCnName());
    alertMessageCn.setAlertLevel(AlertLevel.valueOf(alertMessage.getAlertLevel()).getCnName());
    alertMessageCn.setClearTime(alertMessage.getClearTime());
    alertMessageCn.setAlertClear(alertMessage.isAlertClear());
    return alertMessageCn;
  }

  private String buildMessage(AlertMessage alertMessage, String eventType, AlertRule rule,
      MonitorPlatformDataDto mpd) {
    if (eventType.equals("0")) {
      eventType = "告警";
    }
    if (eventType.equals("1")) {
      eventType = "已恢复";
    }
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date lastDate = new Date(alertMessage.getLastAlertTime());
    String alertTime = simpleDateFormat.format(lastDate);
    String sourceName = alertMessage.getSourceName();
    String alarmMetric = alertMessage.getCounterKey();
    String alarmInfo = sourceName + " " + rule.getDescription() + "，请关注";
    String alertLevel = AlertLevel.valueOf(alertMessage.getAlertLevel()).getCnName();
    String alarmValue = (mpd.getAlarmValue() == null || mpd.getAlarmValue().equals("null"))
        ? "0" : mpd.getAlarmValue();
    String alarmThreshold = mpd.getAlarmThreshold();
    return "状态:" + eventType + "\n"
        + "告警时间:" + alertTime + "\n"
        + "告警源:" + sourceName + "\n"
        + "告警指标:" + alarmMetric + "\n"
        + "告警信息:" + alarmInfo + "\n"
        + "告警级别:" + alertLevel + "\n"
        + "当前值:" + alarmValue + "\n"
        + "阈值:" + alarmThreshold + "\n"
        + MESSAGE_FOOT;
  }

  public AlertMessage removeLastAlertMessageKey(AlertMessageKey alertMessageKey) {
    logger.warn("alertMessageKey:{} removed", alertMessageKey);
    return memoryAlertMessageManager
        .removeLastAlertMessageKey(alertMessageKey.getFuzzyAlertMessageKey());
  }

  public void setSnmpForwardItemDao(SnmpForwardItemDao snmpForwardItemDao) {
    this.snmpForwardItemDao = snmpForwardItemDao;
  }

  public void setSmtpSendEmail(SmtpSendEmail smtpSendEmail) {
    this.smtpSendEmail = smtpSendEmail;
  }

  public void setMaxEventLogCount(int maxEventLogCount) {
    this.maxEventLogCount = maxEventLogCount;
  }

  public void setEventLogInfoDao(EventLogInfoDao eventLogInfoDao) {
    this.eventLogInfoDao = eventLogInfoDao;
  }

  public void setRepeatAlertTimeMs(int repeatAlertTimeMs) {
    this.repeatAlertTimeMs = repeatAlertTimeMs;
  }

  public void setNeedSendDto(boolean needSendDto) {
    this.needSendDto = needSendDto;
  }

  public void setDtoUserDao(py.monitorcommon.dao.DtoUserDao dtoUserDao) {
    this.dtoUserDao = dtoUserDao;
  }

  public void setAlertRuleDao(py.monitorcommon.dao.AlertRuleDao alertRuleDao) {
    this.alertRuleDao = alertRuleDao;
  }

  public void setDtoSendLogDao(DtoSendLogDao dtoSendLogDao) {
    this.dtoSendLogDao = dtoSendLogDao;
  }

  public void setJobToken(String jobToken) {
    this.jobToken = jobToken;
  }

  public void setJobUrl(String jobUrl) {
    this.jobUrl = jobUrl;
  }

  public void setSignalToken(String signalToken) {
    this.signalToken = signalToken;
  }

  public void setSignalUrl(String signalUrl) {
    this.signalUrl = signalUrl;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public void setSystem(String system) {
    this.system = system;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public void setSystemType(String systemType) {
    this.systemType = systemType;
  }

  public void setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }
}
