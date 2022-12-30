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
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.app.context.AppContext;
import py.instance.InstanceStatus;
import py.monitor.common.AlertMessage;
import py.monitor.common.EventLogInfo;
import py.monitorcommon.dao.AlertMessageDao;
import py.monitorcommon.dao.EventLogInfoDao;
import py.monitorcommon.dao.EventLogMessageByDayDao;
import py.monitorcommon.dao.EventLogMessageByHourDao;
import py.monitorcommon.dao.EventLogMessageDao;
import py.periodic.Worker;

public class DeletePerformanceDataWorker implements Worker {

  private static final Logger logger = LoggerFactory.getLogger(DeletePerformanceDataWorker.class);
  private static final long HOUR_TO_MILLISECOND = 3600000L;

  private AppContext appContext;
  private EventLogMessageDao eventLogMessageDao;
  private EventLogMessageByHourDao eventLogMessageByHourDao;
  private EventLogMessageByDayDao eventLogMessageByDayDao;
  private AlertMessageDao alertMessageDao;
  private EventLogInfoDao eventLogInfoDao;
  private int performanceDataMinuteTableSaveDays;
  private int performanceDataHourTableSaveDays;
  private int performanceDataDayTableSaveYears;
  private int deletedAlertMessageExpiredMinutes;

  public DeletePerformanceDataWorker(EventLogMessageDao eventLogMessageDao,
      EventLogMessageByHourDao eventLogMessageByHourDao,
      EventLogMessageByDayDao eventLogMessageByDayDao,
      int performanceDataMinuteTableSaveDays,
      int performanceDataHourTableSaveDays,
      int performanceDataDayTableSaveYears) {
    this.eventLogMessageDao = eventLogMessageDao;
    this.eventLogMessageByHourDao = eventLogMessageByHourDao;
    this.eventLogMessageByDayDao = eventLogMessageByDayDao;
    this.performanceDataMinuteTableSaveDays = performanceDataMinuteTableSaveDays;
    this.performanceDataHourTableSaveDays = performanceDataHourTableSaveDays;
    this.performanceDataDayTableSaveYears = performanceDataDayTableSaveYears;
  }

  @Override
  public void doWork() {
    if (appContext.getStatus() == InstanceStatus.SUSPEND) {
      logger.info("This MonitorServer is suspend.");
      //Todo: delete memory data
      return;
    }
    logger.info("delete performance message periodically.");
    long startTime = System.currentTimeMillis();
    deleteHistoryPerformanceMessage(performanceDataMinuteTableSaveDays,
        performanceDataHourTableSaveDays,
        performanceDataDayTableSaveYears);
    long endTime = System.currentTimeMillis();
    logger.warn("delete performance message time is: {}ms", endTime - startTime);
    deleteExpiredAlertMessages();
  }

  public void deleteExpiredAlertMessages() {
    List<AlertMessage> alertMessages = alertMessageDao.listDeletedAlertMessages();
    if (alertMessages == null || alertMessages.isEmpty()) {
      return;
    }
    long expiredTime = deletedAlertMessageExpiredMinutes * 60 * 1000;
    for (AlertMessage alertMessage : alertMessages) {
      if ((System.currentTimeMillis() - alertMessage.getDeleteTime()) < expiredTime) {
        continue;
      }
      String id = alertMessage.getId();
      logger.warn("delete deletedAlertMessage, alertMessage is: {}", alertMessage);
      alertMessageDao.deleteAlertMessageById(id);
    }
    for (AlertMessage alertMessage : alertMessages) {
      if ((System.currentTimeMillis() - alertMessage.getDeleteTime()) < expiredTime) {
        continue;
      }
      Set<EventLogInfo> eventLogInfoSet = alertMessage.getEventLogInfoSet();
      for (EventLogInfo eventLogInfo : eventLogInfoSet) {
        try {
          eventLogInfoDao.deleteById(eventLogInfo.getId());
        } catch (Exception e) {
          logger
              .info("this event may be used by other alert, eventId is: {}", eventLogInfo.getId());
        }
      }
    }
  }

  private void deleteHistoryPerformanceMessage(int minuteTableSaveDays, int hourTableSaveDays,
      int dayTableSaveYears) {
    Calendar startCalendar = Calendar.getInstance();
    startCalendar.set(Calendar.HOUR_OF_DAY, 0);
    startCalendar.set(Calendar.MINUTE, 0);
    startCalendar.set(Calendar.SECOND, 0);
    startCalendar.set(Calendar.MILLISECOND, 0);
    Date currentTime = startCalendar.getTime();

    //delete minute table
    if (minuteTableSaveDays >= 1) {
      Long beginTime = currentTime.getTime() - minuteTableSaveDays * HOUR_TO_MILLISECOND * 24;
      eventLogMessageDao.deleteEventLogMessageByTime(new Date(beginTime));
    }

    //delete hour table
    if (hourTableSaveDays >= 1) {
      Long beginTime = currentTime.getTime() - hourTableSaveDays * HOUR_TO_MILLISECOND * 24;
      eventLogMessageByHourDao.deleteEventLogMessageByTime(new Date(beginTime));
    }

    //delete year table
    if (dayTableSaveYears >= 1) {
      Long beginTime = currentTime.getTime() - dayTableSaveYears * 365 * HOUR_TO_MILLISECOND * 24;
      eventLogMessageByDayDao.deleteEventLogMessageByTime(new Date(beginTime));
    }
  }

  public void setAppContext(AppContext appContext) {
    this.appContext = appContext;
  }

  public void setAlertMessageDao(AlertMessageDao alertMessageDao) {
    this.alertMessageDao = alertMessageDao;
  }

  public void setEventLogInfoDao(EventLogInfoDao eventLogInfoDao) {
    this.eventLogInfoDao = eventLogInfoDao;
  }

  public void setDeletedAlertMessageExpiredMinutes(int deletedAlertMessageExpiredMinutes) {
    this.deletedAlertMessageExpiredMinutes = deletedAlertMessageExpiredMinutes;
  }
}
