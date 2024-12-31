
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
