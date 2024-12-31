

package py.monitorcommon.smtp;

import java.util.Map;
import py.monitor.common.AlertMessage;

public interface SmtpSendEmail {

  void checksumSmtp(String smtpHost, String userName, String password, int smtpPort,
      String encryptType,
      String contentType, String subject) throws Exception;

  void sendEmail(Map<String, String> map) throws Exception;

  void sendEmail(AlertMessage alertMessage) throws Exception;
}
