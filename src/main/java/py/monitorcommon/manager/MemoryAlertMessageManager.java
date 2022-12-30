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

package py.monitorcommon.manager;

import java.util.Map;
import org.apache.commons.lang.Validate;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.monitor.common.AlertMessage;
import py.monitor.common.AlertMessageKey;
import py.monitorcommon.struct.MemoryAlertMessage;

public class MemoryAlertMessageManager {

  private static final Logger logger = LoggerFactory.getLogger(MemoryAlertMessageManager.class);

  //<fuzzy alert key, alert message>
  private Map<AlertMessageKey, AlertMessage> lastAlertMessageMap;
  // all alert message <sourceId, alert message>
  private ConcurrentHashMap<String, MemoryAlertMessage> memoryAlertMessageMap;

  public MemoryAlertMessageManager() {
    memoryAlertMessageMap = new ConcurrentHashMap<>();
    lastAlertMessageMap = new ConcurrentHashMap<>();
  }

  public AlertMessage getLastAlertMessage(AlertMessageKey alertMessageKey) {
    return lastAlertMessageMap.get(alertMessageKey);
  }

  public AlertMessage putIfAbsentLastAlertMessage(AlertMessageKey alertMessageKey,
      AlertMessage alertMessage) {
    return lastAlertMessageMap.putIfAbsent(alertMessageKey, alertMessage);
  }

  public void updateLastAlertMessage(AlertMessageKey key, AlertMessage alertMessage) {
    synchronized (lastAlertMessageMap) {
      AlertMessage oldAlertMessage = lastAlertMessageMap.get(key);
      if (oldAlertMessage.getLastAlertTime() < alertMessage.getLastAlertTime()) {
        oldAlertMessage.setLastAlertTime(alertMessage.getLastAlertTime());
      }
    }
  }

  public AlertMessage removeLastAlertMessageKey(AlertMessageKey alertMessageKey) {
    synchronized (lastAlertMessageMap) {
      return lastAlertMessageMap.remove(alertMessageKey);
    }
  }

  public long lastAlertMessageLength() {
    return lastAlertMessageMap.size();
  }

  public static String generateMemoryAlertKey(String sourceId, String counterKey) {
    return String.format("%s_%s", sourceId, counterKey);
  }

  public boolean containsMemoryAlertKey(String sourceId, String counterKey) {
    return memoryAlertMessageMap.containsKey(generateMemoryAlertKey(sourceId, counterKey));
  }

  public void putMemoryAlert(AlertMessage value) {
    Validate.notEmpty(value.getSourceId(), "put alert message failed. source id is null");
    Validate.notEmpty(value.getCounterKey(), "put alert message failed. counter key is null");

    memoryAlertMessageMap
        .putIfAbsent(generateMemoryAlertKey(value.getSourceId(), value.getCounterKey()),
            new MemoryAlertMessage(value));
  }

  public void removeMemoryAlert(String sourceId, String counterKey) {
    memoryAlertMessageMap.remove(generateMemoryAlertKey(sourceId, counterKey));
  }

  public long memoryAlertLength() {
    return memoryAlertMessageMap.size();
  }

  public void clearMemoryAlert() {
    memoryAlertMessageMap.clear();
  }

  public String printMemoryAlert() {
    return memoryAlertMessageMap.toString();
  }
}
