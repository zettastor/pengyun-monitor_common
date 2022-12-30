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

import java.util.Date;
import java.util.List;
import java.util.Set;
import py.monitor.common.PerformanceMessageHistoryByDay;

public interface EventLogMessageByDayDao {

  void clearDb();

  void saveEventLogMessage(PerformanceMessageHistoryByDay eventLogMessage);

  void deleteEventLogMessageByObject(Object performanceMessageHistoryByDayProto);

  void deleteEventLogMessageById(String id);

  void deleteEventLogMessageByTime(Date time);

  void deleteEventLogMessageByTime(String counterName, Date time);

  void deleteEventLogMessageByIds(Set<String> ids);

  int getTotalCount();

  PerformanceMessageHistoryByDay getEventLogMessageById(String id);

  Date getEventLogMessageAsMaxTime(String counterKey);

  List<PerformanceMessageHistoryByDay> getAllEventLogMessage();

  int getFilterCount(Long startDay, Long endDay, String operation, String sourceId);

  List getEventLogMessage(Integer firstResult, Integer maxResult, String sortFeild,
      String sortDirection,
      Long startDay, Long endDay, String counterKey, String sourceId);
}
