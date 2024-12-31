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
