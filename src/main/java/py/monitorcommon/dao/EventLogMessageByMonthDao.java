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

import java.util.Date;
import java.util.List;
import java.util.Set;
import py.monitor.common.PerformanceMessageHistoryByMonth;

public interface EventLogMessageByMonthDao {

  void clearDb();

  void saveEventLogMessage(PerformanceMessageHistoryByMonth eventLogMessage);

  void deleteEventLogMessageByObject(Object performanceMessageHistoryByMonthProto);

  void deleteEventLogMessageById(String id);

  void deleteEventLogMessageByIds(Set<String> ids);

  void deleteEventLogMessageByTime(String counterName, Date time);

  int getTotalCount();

  PerformanceMessageHistoryByMonth getEventLogMessageById(String id);

  Date getEventLogMessageAsMaxTime(String counterKey);

  List<PerformanceMessageHistoryByMonth> getAllEventLogMessage();

  int getFilterCount(Long startDay, Long endDay, String operation, String sourceId);

  List getEventLogMessage(Integer firstResult, Integer maxResult, String sortFeild,
      String sortDirection,
      Long startDay, Long endDay, String counterKey, String sourceId);
}
