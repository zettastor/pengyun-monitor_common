/**
* Copyright (C) 2013-2024 Nanjing Pengyun Network Technology Co., Ltd.
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
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
