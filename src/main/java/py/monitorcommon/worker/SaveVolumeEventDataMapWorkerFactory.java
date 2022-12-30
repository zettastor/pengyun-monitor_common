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
import py.monitorcommon.manager.EventLogManager;
import py.periodic.Worker;
import py.periodic.WorkerFactory;

public class SaveVolumeEventDataMapWorkerFactory implements WorkerFactory {

  private AppContext appContext;
  private EventLogManager eventLogManager;

  public SaveVolumeEventDataMapWorkerFactory(EventLogManager eventLogManager) {
    this.eventLogManager = eventLogManager;
  }

  @Override
  public Worker createWorker() {
    SaveVolumeEventDataMapWorker worker = new SaveVolumeEventDataMapWorker(eventLogManager);
    worker.setAppContext(appContext);
    return worker;
  }

  public void setAppContext(AppContext appContext) {
    this.appContext = appContext;
  }
}
