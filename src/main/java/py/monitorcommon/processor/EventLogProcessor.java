
package py.monitorcommon.processor;

import java.util.Map;
import py.monitor.common.EventDataInfo;
import py.monitor.common.EventLogCompressed;

public interface EventLogProcessor {

  void saveToEventLogCompressdMap(EventDataInfo eventDataInfo, String counterKey);

  void saveToPerformanceQueue(EventDataInfo eventDataInfo, String counterKey);

  void saveToAlertQueue(EventDataInfo eventDataInfo, String counterKey);

  void saveToEventLogQueue();

  Map<String, EventLogCompressed> getEventLogCompressedMap();
}
