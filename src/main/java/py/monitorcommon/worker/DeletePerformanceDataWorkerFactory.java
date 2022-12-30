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

import py.app.context.AppContext;
import py.monitorcommon.dao.AlertMessageDao;
import py.monitorcommon.dao.EventLogInfoDao;
import py.monitorcommon.dao.EventLogMessageByDayDao;
import py.monitorcommon.dao.EventLogMessageByHourDao;
import py.monitorcommon.dao.EventLogMessageDao;
import py.periodic.Worker;
import py.periodic.WorkerFactory;

public class DeletePerformanceDataWorkerFactory implements WorkerFactory {

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

  public DeletePerformanceDataWorkerFactory(EventLogMessageDao eventLogMessageDao,
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
  public Worker createWorker() {
    DeletePerformanceDataWorker worker = new DeletePerformanceDataWorker(eventLogMessageDao,
        eventLogMessageByHourDao, eventLogMessageByDayDao, performanceDataMinuteTableSaveDays,
        performanceDataHourTableSaveDays, performanceDataDayTableSaveYears);
    worker.setAppContext(appContext);
    worker.setAlertMessageDao(alertMessageDao);
    worker.setEventLogInfoDao(eventLogInfoDao);
    worker.setDeletedAlertMessageExpiredMinutes(deletedAlertMessageExpiredMinutes);
    return worker;
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
