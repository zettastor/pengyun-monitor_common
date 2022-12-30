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

package py.monitorcommon.dao;

import java.util.List;
import java.util.Set;
import py.monitor.common.AlertMessage;
import py.monitor.common.EventLogInfo;

public interface AlertMessageDao {

  void clearDb();

  void saveAlertMessageWithCheck(AlertMessage alertMessage);

  void saveOrUpdateAlertMessage(AlertMessage alertMessage);

  Set<Long> updateLastAlertById(String id, long lastAlertTime, int frequency, Set<EventLogInfo>
      eventLogInfoSet, int maxEventLogCount);

  void updateAlertFromFuzzyToFullById(String id, AlertMessage fullAlertMessage);

  void updateAlertMessageReadFlag(String id, boolean readFlag);

  void deleteAlertMessageById(String alertMessageId);

  void deleteAlertMessageByIds(Set<String> alertMessageIdList);

  void setDeleteFlagById(String id);

  AlertMessage getAlertMessageById(String alertMessageId);

  AlertMessage getAlertMessageWithEventLogById(String alertMessageId);

  List<AlertMessage> getAllAlertMessage();

  List<AlertMessage> listDeletedAlertMessages();

  void clearAlertMessageById(String alertMessageId);

  void clearAlertMessageByIds(Set<String> alertMessageIdSet);

  void acknowledgeAlertMessageById(String alertMessageId);

  void acknowledgeAlertMessageByIds(Set<String> alertMessageIdList);

  void clearAcknowledgeAlertMessageById(String alertMessageId);

  void clearAcknowledgeAlertMessageByIds(Set<String> alertMessageIdList);

  int getTotalCount();

  int getFilterCount(Long startTime, Long endTime, String sourceId, String sourceName,
      String alertLevel,
      Boolean alertAcknowledge, Boolean alertClear, String alertType, String alertRuleName,
      boolean isCsi);

  List<AlertMessage> getFilterAlertMessage(Integer firstResult, Integer maxResult, String sortFeild,
      String sortDirection, Long startTime, Long endTime, String sourceId, String sourceName,
      String alertLevel,
      Boolean alertAcknowledge, Boolean alertClear, String alertType, String alertRuleName,
      boolean isCsi);

  List<AlertMessage> listAlertMessagesBySourceId(String sourceId);

  List<AlertMessage> getAlertMessageAboutNetSubHealth(String sourcename);
}
