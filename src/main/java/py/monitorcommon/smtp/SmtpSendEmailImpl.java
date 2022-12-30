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

package py.monitorcommon.smtp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import py.monitor.common.AlertLevel;
import py.monitor.common.AlertMessage;
import py.monitor.common.EmailForwardItem;
import py.monitor.common.SmtpItem;
import py.monitorcommon.dao.EmailForwardItemDao;
import py.monitorcommon.dao.SmtpItemDao;
import py.thrift.monitorserver.service.SmtpEncryptTypeThrift;

@Transactional
public class SmtpSendEmailImpl implements SmtpSendEmail {

  private static final Logger logger = LoggerFactory.getLogger(SmtpSendEmailImpl.class);
  private final String alertObject = "告警对象";
  private final String alertDesc = "告警描述";
  private final String alertLevel = "告警等级";
  private final String alertAck = "告警确认";
  private final String alertType = "告警类型";
  private final String alertRulename = "规则名";
  private final String alertFirsttime = "告警产生时间";
  private final String alertCleartime = "告警关闭时间";
  private final String alertClear = "告警关闭";
  private SmtpItemDao smtpItemDao;
  private EmailForwardItemDao emailForwardItemDao;

  public void setSmtpItemDao(SmtpItemDao smtpItemDao) {
    this.smtpItemDao = smtpItemDao;
  }

  public void setEmailForwardItemDao(EmailForwardItemDao emailForwardItemDao) {
    this.emailForwardItemDao = emailForwardItemDao;
  }

  private String getContentMessages(Map<String, String> map, String contentType) {
    String contentMessage;
    if (map == null) {
      contentMessage = "<html></html>";
      return contentMessage;
    }

    if (contentType.equals("HTML")) {
      contentMessage = "<html><h4>告警列表</h4><table border=\"1\">";
    } else {
      contentMessage = "告警列表\r\n";
    }
    Iterator<Map.Entry<String, String>> entries = map.entrySet().iterator();

    while (entries.hasNext()) {
      Map.Entry<String, String> entry = entries.next();
      if (contentType.equals("HTML")) {
        contentMessage += "<tr>";
        contentMessage += "<td>" + entry.getKey() + "</td>";
        contentMessage += "<td>" + entry.getValue() + "</td>";
        contentMessage += "</tr>";
      } else {
        contentMessage += entry.getKey() + " : " + entry.getValue() + "\r\n";
      }
    }
    if (contentType.equals("HTML")) {
      contentMessage += "</table></html>";
    }

    logger.warn("contenMessage:{}", contentMessage);
    return contentMessage;
  }

  @Override
  public void checksumSmtp(String smtpHost, String userName, String password, int smtpPort,
      String encryptType,
      String contentType, String subject) throws Exception {
    logger.warn("host:{} user:{} password:{} port:{} encrytType:{} contentType:{} subject:{}",
        smtpHost, userName,
        password, smtpPort, encryptType, contentType, subject);

    Properties props = new Properties();
    props.put("mail.smtp.timeout", 3000);
    props.put("mail.smtp.connectiontimeout", 3000);
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.host", smtpHost);
    props.put("mail.smtp.port", smtpPort);
    //props.setProperty("mail.transport.protocol", "smtp");
    if (SmtpEncryptTypeThrift.valueOf(encryptType).equals(SmtpEncryptTypeThrift.SSL)) {
      /* Auth in SSL */
      props.put("mail.smtp.ssl.enable", "true");
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      props.setProperty("mail.transport.protocol", "smtps");
      logger.warn("SSL");
    } else if (SmtpEncryptTypeThrift.valueOf(encryptType).equals(SmtpEncryptTypeThrift.TLS)) {
      /* Auth in TLS */
      props.put("mail.smtp.starttls.enable", "true");
      props.setProperty("mail.transport.protocol", "smtps");
      logger.warn("TLS");
    } else {
      props.setProperty("mail.transport.protocol", "smtp");
      logger.warn("Auth");
    }

    Session session = Session.getInstance(props, new Authenticator() {
      public PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(userName, password);
      }
    });

    session.setDebug(true);

    try {
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(userName));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(userName));
      message.setSubject(subject);
      if (contentType.equals("HTML")) {
        message.setContent("这是一封测试邮件", "text/html; charset=utf-8");
      } else {
        message.setContent("这是一封测试邮件", "text/plain; charset=utf-8");
      }
      Transport.send(message);
    } catch (Exception e) {
      logger.error("sendmail exception.", e);
      throw e;
    }
  }

  private SmtpItem getSmtpSettingFromDb() {
    /*  Get SMTP setting Info from SmtpItemDao */
    try {
      SmtpItem smtpItem = smtpItemDao.getSmtpItem();
      if (smtpItem != null) {
        logger.debug(
            "SMTP info: host:{} port:{} user:{} password:{} encryt:{} contentType:{} subject:{}"
                + " enable:{}",
            smtpItem.getSmtpServer(), smtpItem.getSmtpPort(), smtpItem.getUserName(),
            smtpItem.getPassword(),
            smtpItem.getEncryptType(), smtpItem.getContentType(), smtpItem.getSubject(),
            smtpItem.isEnable());
        return smtpItem;
      } else {
        logger.warn("Can't get smtp info");
        return null;
      }
    } catch (Exception e) {
      logger.warn("getSmtpItme error.", e);
      return null;
    }
  }

  private List<InternetAddress> getSmtpToList() {
    List<InternetAddress> addressesList = new ArrayList<InternetAddress>();
    List<EmailForwardItem> list = emailForwardItemDao.listEmailForwardItemEnable();
    if (list.size() <= 0) {
      logger.warn("no Receive Mail address.");
      return addressesList;
    }

    for (int i = 0; i < list.size(); i++) {
      try {
        InternetAddress addresses = new InternetAddress(list.get(i).getEmail());
        addressesList.add(addresses);
      } catch (AddressException e) {
        logger.error("InternetAddress:{} error.", list.get(i));
      }
    }
    return addressesList;
  }

  @Override
  public void sendEmail(Map<String, String> map) throws Exception {
    SmtpItem smtpItem = getSmtpSettingFromDb();
    List<InternetAddress> internetAddressList = getSmtpToList();

    /*  SMTP Info had not been set*/
    if (smtpItem == null) {
      logger.warn("SMTP: no set");
      return;
    }

    if (internetAddressList.size() <= 0) {
      logger.warn("To address not set");
      return;
    }

    if (!smtpItem.isEnable()) {
      logger.warn("SMTP:smtpEnable is false");
      return;
    }

    InternetAddress[] toAddresses = new InternetAddress[internetAddressList.size()];
    toAddresses = internetAddressList.toArray(toAddresses);
    String contentMessage;
    contentMessage = getContentMessages(map, smtpItem.getContentType());
    if (toAddresses.length <= 0) {
      logger.warn("Have't toAddress");
      return;
    }

    //Properties props = System.getProperties();
    Properties props = new Properties();
    props.put("mail.smtp.timeout", 3000);
    props.put("mail.smtp.connectiontimeout", 3000);
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.host", smtpItem.getSmtpServer());
    props.put("mail.smtp.port", smtpItem.getSmtpPort());
    //props.setProperty("mail.transport.protocol", "smtp");
    if (SmtpEncryptTypeThrift.valueOf(smtpItem.getEncryptType())
        .equals(SmtpEncryptTypeThrift.SSL)) {
      /* Auth in SSL */
      props.put("mail.smtp.ssl.enable", "true");
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      props.setProperty("mail.transport.protocol", "smtps");
    } else if (SmtpEncryptTypeThrift.valueOf(smtpItem.getEncryptType())
        .equals(SmtpEncryptTypeThrift.TLS)) {
      /* Auth in TLS */
      props.put("mail.smtp.starttls.enable", "true");
      props.setProperty("mail.transport.protocol", "smtps");
    } else {
      props.setProperty("mail.transport.protocol", "smtp");
    }

    /* Don't use Session.getDefaultInstance */
    Session session = Session.getInstance(props, new Authenticator() {
      public PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(smtpItem.getUserName(), smtpItem.getPassword());
      }
    });

    session.setDebug(false);

    try {
      AlertLevel alertLevel = null;
      //get alert level
      if (map.containsKey(this.alertLevel)) {
        alertLevel = AlertLevel.getLevleByCnName(map.get(this.alertLevel));
      } else {
        logger.warn("cannot found alert level. alert:{}", map);
      }

      String subject;
      if (alertLevel != null) {
        subject = String.format("【%s%s】%s", alertLevel.getCnName(), alertLevel.getSymbol(),
            smtpItem.getSubject());
      } else {
        subject = smtpItem.getSubject();
      }

      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(smtpItem.getUserName()));
      message.setRecipients(Message.RecipientType.TO, toAddresses);
      message.setSubject(MimeUtility.encodeWord(subject, "UTF-8", "Q"));
      if (smtpItem.getContentType().equals("HTML")) {
        message.setContent(contentMessage, "text/html; charset=utf-8");
      } else {
        message.setContent(contentMessage, "text/plain; charset=utf-8");
      }
      Transport.send(message);
    } catch (Exception e) {
      logger.error("sendEmail exception. smtpItem:{} toAddresses:{}", smtpItem, toAddresses, e);
      throw e;
    }
  }

  @Override
  public void sendEmail(AlertMessage alertMessage) throws Exception {
    logger.debug("send email alertMessage is {}", alertMessage);
    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put(alertObject, alertMessage.getSourceName());
    map.put(alertDesc, alertMessage.getAlertDescription());
    map.put(alertLevel, alertMessage.getAlertLevel());

    if (alertMessage.isAlertAcknowledge()) {
      map.put(alertAck, "已确认");
    } else {
      map.put(alertAck, "未确认");
    }

    map.put(alertType, alertMessage.getAlertType());
    map.put(alertRulename, alertMessage.getAlertRuleName());
    Date dateFirst = new Date(alertMessage.getFirstAlertTime());
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    map.put(alertFirsttime, format.format(dateFirst));

    if (alertMessage.isAlertClear()) {
      map.put(alertClear, "已关闭");
    } else {
      map.put(alertClear, "未关闭");
    }
    if (alertMessage.getClearTime() == 0) {
      map.put(alertCleartime, "----");
    } else {
      Date dateClear = new Date(alertMessage.getClearTime());
      SimpleDateFormat formatClear = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      map.put(alertCleartime, formatClear.format(dateClear));
    }

    logger.debug("send email map is {}", map);
    logger.debug("send email ALERT_OBJECT is {}", map.get(alertObject));
    logger.debug("send email ALERT_LEVEL is {}", map.get(alertLevel));
    logger.debug("send email ALERT_TYPE is {}", map.get(alertType));
    logger.debug("send email ALERT_RULENAME is {}", map.get(alertRulename));
    try {
      sendEmail(map);
    } catch (Exception e) {
      logger.error("sendEmail exception.", e);
      throw e;
    }
  }
}
