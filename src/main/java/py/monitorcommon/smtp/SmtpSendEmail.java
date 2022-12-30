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

import java.util.Map;
import py.monitor.common.AlertMessage;

public interface SmtpSendEmail {

  void checksumSmtp(String smtpHost, String userName, String password, int smtpPort,
      String encryptType,
      String contentType, String subject) throws Exception;

  void sendEmail(Map<String, String> map) throws Exception;

  void sendEmail(AlertMessage alertMessage) throws Exception;
}
