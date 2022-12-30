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

package py.monitorcommon.worker;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.app.context.AppContext;
import py.common.RequestIdBuilder;
import py.instance.InstanceStatus;
import py.monitor.common.CounterName;
import py.monitor.common.PerformanceMessageHistory;
import py.monitor.common.PerformanceMessageHistoryByDay;
import py.monitor.common.PerformanceMessageHistoryByHour;
import py.monitor.common.PerformanceMessageHistoryByMonth;
import py.monitorcommon.dao.EventLogMessageByDayDao;
import py.monitorcommon.dao.EventLogMessageByHourDao;
import py.monitorcommon.dao.EventLogMessageByMonthDao;
import py.monitorcommon.dao.EventLogMessageDao;
import py.periodic.Worker;

public abstract class CompressPerformanceDataTableWorkerShared implements Worker {

  protected static final Logger logger = LoggerFactory
      .getLogger(CompressPerformanceDataTableWorkerShared.class);
  protected static final long HOUR_TO_MILLISECOND = 3600000L;
  protected static final long DAY_TO_MILLISECOND = 3600000L * 24;

  protected AppContext appContext;
  protected EventLogMessageDao eventLogMessageDao;
  protected EventLogMessageByHourDao eventLogMessageByHourDao;
  protected EventLogMessageByDayDao eventLogMessageByDayDao;
  protected EventLogMessageByMonthDao eventLogMessageByMonthDao;

  protected abstract List<CounterName> addCounterKeys();

  @Override
  public void doWork() {
    if (appContext.getStatus() == InstanceStatus.SUSPEND) {
      logger.info("This MonitorServer is suspend.");
      //Todo: delete memory data
      return;
    }
    logger.info("compress performance data table periodically.");
    Calendar currentHourCalendar = Calendar.getInstance();
    currentHourCalendar.set(Calendar.MINUTE, 0);
    currentHourCalendar.set(Calendar.SECOND, 0);
    currentHourCalendar.set(Calendar.MILLISECOND, 0);

    List<CounterName> counterKeys = addCounterKeys();

    // compress 5 minutes table to hour table
    for (CounterName counterName : counterKeys) {
      List<PerformanceMessageHistory> eventLogMessageInMinuteTable =
          listEventLogMessageInMinuteTable(
              counterName, currentHourCalendar);
      logger.debug("compress minute table, counterName is: {}, count is: {}.", counterName,
          eventLogMessageInMinuteTable == null ? 0 : eventLogMessageInMinuteTable.size());
      compressMinuteTableToHourTable(eventLogMessageInMinuteTable);
    }

    // compress hour table to day table
    for (CounterName counterName : counterKeys) {
      List<PerformanceMessageHistoryByHour> eventLogMessageInHourTable =
          listEventLogMessageInHourTable(
              counterName, currentHourCalendar);
      logger.debug("compress hour table, counterName is: {}, count is: {}.", counterName,
          eventLogMessageInHourTable == null ? 0 : eventLogMessageInHourTable.size());
      compressHourTableToDayTable(eventLogMessageInHourTable);
    }

    // compress day table to month table
    for (CounterName counterName : counterKeys) {
      List<PerformanceMessageHistoryByDay> eventLogMessageInDayTable =
          listEventLogMessageInDayTable(
              counterName,
              currentHourCalendar);
      logger.debug("compress hour table, counterName is: {}, count is: {}.", counterName,
          eventLogMessageInDayTable == null ? 0 : eventLogMessageInDayTable.size());
      compressDayTableToMonthTable(eventLogMessageInDayTable);
    }
  }

  @SuppressWarnings("unchecked")
  private List<PerformanceMessageHistory> listEventLogMessageInMinuteTable(CounterName counterKey,
      Calendar currentHourCalendar) {
    Date maxTimeInHourTable = eventLogMessageByHourDao
        .getEventLogMessageAsMaxTime(counterKey.name());
    Long startTime = null;

    if (maxTimeInHourTable != null) {
      if (currentHourCalendar.getTimeInMillis() - maxTimeInHourTable.getTime()
          < HOUR_TO_MILLISECOND) {
        return null;
      }
      startTime = maxTimeInHourTable.getTime() + HOUR_TO_MILLISECOND;
    }

    return eventLogMessageDao.getEventLogMessage(null, null, null, null, startTime,
        currentHourCalendar.getTimeInMillis() - 1, counterKey.name(), null);
  }

  @SuppressWarnings("unchecked")
  private List<PerformanceMessageHistoryByHour> listEventLogMessageInHourTable(
      CounterName counterKey,
      Calendar currentHourCalendar) {
    currentHourCalendar.set(Calendar.HOUR_OF_DAY, 0);
    Date maxTimeInDayTable = eventLogMessageByDayDao.getEventLogMessageAsMaxTime(counterKey.name());
    Long startTime = null;

    if (maxTimeInDayTable != null) {
      if (currentHourCalendar.getTimeInMillis() - maxTimeInDayTable.getTime()
          < DAY_TO_MILLISECOND) {
        return null;
      }
      startTime = maxTimeInDayTable.getTime() + DAY_TO_MILLISECOND;
    }

    return eventLogMessageByHourDao.getEventLogMessage(null, null, null, null, startTime,
        currentHourCalendar.getTimeInMillis() - 1, counterKey.name(), null);
  }

  @SuppressWarnings("unchecked")
  private List<PerformanceMessageHistoryByDay> listEventLogMessageInDayTable(CounterName counterKey,
      Calendar currentMonthCalendar) {

    currentMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);
    Date maxTime = eventLogMessageByMonthDao.getEventLogMessageAsMaxTime(counterKey.name());
    Long startTime = null;

    if (maxTime != null) {
      if (maxTime.getTime() >= currentMonthCalendar.getTimeInMillis()) {
        return null;
      }
      Calendar maxCalendar = Calendar.getInstance();
      maxCalendar.setTime(maxTime);
      maxCalendar.set(Calendar.MONTH, maxCalendar.get(Calendar.MONTH) + 1);
      startTime = maxCalendar.getTimeInMillis();
    }

    return eventLogMessageByDayDao
        .getEventLogMessage(null, null, null, null, startTime,
            currentMonthCalendar.getTimeInMillis() - 1,
            counterKey.name(), null);
  }

  private void compressMinuteTableToHourTable(List<PerformanceMessageHistory> eventLogMessageList) {
    if (eventLogMessageList == null || eventLogMessageList.size() == 0) {
      logger.debug("no eventLogMessage in minute table need compressed.");
      return;
    }

    Map<String, PerformanceMessageHistoryByHour> hourPerformanceMessageMap = new HashMap<>();

    for (PerformanceMessageHistory minutePerformanceMessage : eventLogMessageList) {
      Calendar tempCalendar = Calendar.getInstance();
      tempCalendar.setTime(minutePerformanceMessage.getStartTime());
      tempCalendar.set(Calendar.MINUTE, 0);
      tempCalendar.set(Calendar.SECOND, 0);
      tempCalendar.set(Calendar.MILLISECOND, 0);
      Date minuteTableHourDate = tempCalendar.getTime();

      String key = minutePerformanceMessage.getSourceId() + minutePerformanceMessage.getCounterKey()
          + minuteTableHourDate;

      if (!hourPerformanceMessageMap.containsKey(key)) {
        PerformanceMessageHistoryByHour hourPerformanceMessage =
            new PerformanceMessageHistoryByHour();
        hourPerformanceMessage.setId(String.valueOf(RequestIdBuilder.get()));
        hourPerformanceMessage.setSourceId(minutePerformanceMessage.getSourceId());
        hourPerformanceMessage.setCounterKey(minutePerformanceMessage.getCounterKey());
        hourPerformanceMessage.setOperation(minutePerformanceMessage.getOperation());
        hourPerformanceMessage.setHour(minuteTableHourDate);
        hourPerformanceMessage.setCounterTotal(minutePerformanceMessage.getCounterTotal());
        hourPerformanceMessage.setFrequency(minutePerformanceMessage.getFrequency());
        hourPerformanceMessageMap.put(key, hourPerformanceMessage);
      } else {
        PerformanceMessageHistoryByHour hourPerformanceMessage = hourPerformanceMessageMap.get(key);
        long counterTotal =
            hourPerformanceMessage.getCounterTotal() + minutePerformanceMessage.getCounterTotal();
        int frequency =
            hourPerformanceMessage.getFrequency() + minutePerformanceMessage.getFrequency();

        hourPerformanceMessage.setCounterTotal(counterTotal);
        hourPerformanceMessage.setFrequency(frequency);
      }
    }

    for (Map.Entry<String, PerformanceMessageHistoryByHour> entry : hourPerformanceMessageMap
        .entrySet()) {
      PerformanceMessageHistoryByHour hourPerformanceMessage = entry.getValue();
      logger.info(
          "compress to hour table, counterKey is:{}, sourceId is:{}, "
              + "hour performance message is: {}",
          hourPerformanceMessage.getCounterKey(), hourPerformanceMessage.getSourceId(),
          hourPerformanceMessage);
      eventLogMessageByHourDao.saveEventLogMessage(entry.getValue());
    }
  }


  private void compressHourTableToDayTable(
      List<PerformanceMessageHistoryByHour> eventLogMessageList) {
    if (eventLogMessageList == null || eventLogMessageList.size() == 0) {
      logger.debug("no eventLogMessage in hour table need compressed.");
      return;
    }

    Map<String, PerformanceMessageHistoryByDay> dayPerformanceMessageMap = new HashMap<>();

    for (PerformanceMessageHistoryByHour hourPerformanceMessage : eventLogMessageList) {
      Calendar tempCalendar = Calendar.getInstance();
      tempCalendar.setTime(hourPerformanceMessage.getHour());
      tempCalendar.set(Calendar.HOUR_OF_DAY, 0);
      Date hourTableDayDate = tempCalendar.getTime();

      String key =
          hourPerformanceMessage.getSourceId() + hourPerformanceMessage.getCounterKey()
              + hourTableDayDate;

      if (!dayPerformanceMessageMap.containsKey(key)) {
        PerformanceMessageHistoryByDay dayPerformanceMessage = new PerformanceMessageHistoryByDay();
        dayPerformanceMessage.setId(String.valueOf(RequestIdBuilder.get()));
        dayPerformanceMessage.setSourceId(hourPerformanceMessage.getSourceId());
        dayPerformanceMessage.setCounterKey(hourPerformanceMessage.getCounterKey());
        dayPerformanceMessage.setOperation(hourPerformanceMessage.getOperation());
        dayPerformanceMessage.setDay(hourTableDayDate);
        dayPerformanceMessage.setCounterTotal(hourPerformanceMessage.getCounterTotal());
        dayPerformanceMessage.setFrequency(hourPerformanceMessage.getFrequency());
        dayPerformanceMessageMap.put(key, dayPerformanceMessage);
      } else {
        PerformanceMessageHistoryByDay dayPerformanceMessage = dayPerformanceMessageMap.get(key);
        long counterTotal =
            dayPerformanceMessage.getCounterTotal() + hourPerformanceMessage.getCounterTotal();
        int frequency =
            dayPerformanceMessage.getFrequency() + hourPerformanceMessage.getFrequency();
        dayPerformanceMessage.setCounterTotal(counterTotal);
        dayPerformanceMessage.setFrequency(frequency);
      }
    }

    for (Map.Entry<String, PerformanceMessageHistoryByDay> entry : dayPerformanceMessageMap
        .entrySet()) {
      PerformanceMessageHistoryByDay dayPerformanceMessage = entry.getValue();
      logger.info(
          "compress to day table, counterKey is:{}, sourceId is:{}, day performance message is: {}",
          dayPerformanceMessage.getCounterKey(), dayPerformanceMessage.getSourceId(),
          dayPerformanceMessage);
      eventLogMessageByDayDao.saveEventLogMessage(entry.getValue());
    }
  }

  private void compressDayTableToMonthTable(
      List<PerformanceMessageHistoryByDay> eventLogMessageList) {
    if (eventLogMessageList == null || eventLogMessageList.size() == 0) {
      logger.debug("no eventLogMessage in day table nedd compressed.");
      return;
    }

    Map<String, PerformanceMessageHistoryByMonth> monthPerformanceMessageMap = new HashMap<>();

    for (PerformanceMessageHistoryByDay dayPerformanceMessage : eventLogMessageList) {
      Calendar tempCalendar = Calendar.getInstance();
      tempCalendar.setTime(dayPerformanceMessage.getDay());
      tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
      Date dayTableMonthDate = tempCalendar.getTime();

      String key = dayPerformanceMessage.getSourceId() + dayPerformanceMessage.getCounterKey()
          + dayTableMonthDate;

      if (!monthPerformanceMessageMap.containsKey(key)) {
        PerformanceMessageHistoryByMonth monthPerformanceMessage =
            new PerformanceMessageHistoryByMonth();
        monthPerformanceMessage.setId(String.valueOf(RequestIdBuilder.get()));
        monthPerformanceMessage.setSourceId(dayPerformanceMessage.getSourceId());
        monthPerformanceMessage.setCounterKey(dayPerformanceMessage.getCounterKey());
        monthPerformanceMessage.setOperation(dayPerformanceMessage.getOperation());
        monthPerformanceMessage.setMonth(dayTableMonthDate);
        monthPerformanceMessage.setCounterTotal(dayPerformanceMessage.getCounterTotal());
        monthPerformanceMessage.setFrequency(dayPerformanceMessage.getFrequency());
        monthPerformanceMessageMap.put(key, monthPerformanceMessage);
      } else {
        PerformanceMessageHistoryByMonth monthPerformanceMessage = monthPerformanceMessageMap
            .get(key);
        long counterTotal =
            monthPerformanceMessage.getCounterTotal() + dayPerformanceMessage.getCounterTotal();
        int frequency =
            monthPerformanceMessage.getFrequency() + dayPerformanceMessage.getFrequency();
        monthPerformanceMessage.setCounterTotal(counterTotal);
        monthPerformanceMessage.setFrequency(frequency);
      }
    }

    for (Map.Entry<String, PerformanceMessageHistoryByMonth> entry : monthPerformanceMessageMap
        .entrySet()) {
      PerformanceMessageHistoryByMonth monthPerformanceMessage = entry.getValue();
      logger.warn(
          "compress to month table, counterKey is:{}, sourceId is:{}, "
              + "month performance message is: {}",
          monthPerformanceMessage.getCounterKey(), monthPerformanceMessage.getSourceId(),
          monthPerformanceMessage);
      eventLogMessageByMonthDao.saveEventLogMessage(entry.getValue());
    }
  }

  public void setAppContext(AppContext appContext) {
    this.appContext = appContext;
  }
}
