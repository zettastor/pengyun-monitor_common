
package py.monitorcommon.manager;

import java.util.List;
import py.monitorcommon.processor.EventLogProcessor;

public interface EventLogProcessorFactory {

  public EventLogProcessor createEventLogProcessor(String counterKey);

  public List<EventLogProcessor> getEventLogProcessorList();
}
