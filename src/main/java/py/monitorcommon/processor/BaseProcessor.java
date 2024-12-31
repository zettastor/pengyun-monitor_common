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

package py.monitorcommon.processor;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.monitor.common.AlertTemplate;
import py.monitor.common.EventDataInfo;
import py.monitor.common.EventLogCompressed;
import py.monitor.common.PerformanceMessage;
import py.monitor.common.PerformanceMessageHistory;
import py.monitor.common.QueueManager;
import py.monitorcommon.manager.MonitorServerHelperShared;
import py.monitorcommon.websocketserver.WebSocketServer;

public abstract class BaseProcessor implements EventLogProcessor {

  private static final Logger logger = LoggerFactory.getLogger(BaseProcessor.class);

  protected Map<String, AlertTemplate> alertTemplateMap;
  protected QueueManager queueManager;
  // map< sourceId + counterKey, EventLogCompressed>
  protected Map<String, EventLogCompressed> eventLogCompressedMap;
  protected int webSocketServerPort;

  public BaseProcessor(Map<String, AlertTemplate> alertTemplateMap, QueueManager queueManager,
      int webSocketServerPort) {
    this.alertTemplateMap = alertTemplateMap;
    this.queueManager = queueManager;
    this.eventLogCompressedMap = new ConcurrentHashMap<>();
    this.webSocketServerPort = webSocketServerPort;
  }

  @Override
  public void saveToEventLogCompressdMap(EventDataInfo eventDataInfo, String counterKey) {
    String sourceId = MonitorServerHelperShared.getSourceKey(eventDataInfo, counterKey);
    if (sourceId == null) {
      logger.warn("sourceId is null, eventDataInfo is: {}", eventDataInfo);
      return;
    }
    String key = sourceId + counterKey;

    if (!eventLogCompressedMap.containsKey(key)) {
      EventLogCompressed logCompressed = new EventLogCompressed();
      EventLogCompressed eventLogCompressed = eventLogCompressedMap.putIfAbsent(key, logCompressed);
      if (eventLogCompressed == null) {
        logCompressed.setOperation(eventDataInfo.getOperation());
        logCompressed
            .setSourceId(MonitorServerHelperShared.getSourceKey(eventDataInfo, counterKey));
        logCompressed.setCounterKey(counterKey);
        logCompressed.getStartTime().set(eventDataInfo.getStartTime());
        logCompressed.getEndTime().set(eventDataInfo.getStartTime());
      }
    }
    EventLogCompressed logCompressed = eventLogCompressedMap.get(key);
    Validate.notNull(logCompressed);
    logCompressed.getEndTime().set(eventDataInfo.getStartTime());
    logCompressed.getCounterTotal().addAndGet(eventDataInfo.getCounter(counterKey));
    logCompressed.getFrequency().incrementAndGet();
  }

  @Override
  public void saveToPerformanceQueue(EventDataInfo eventDataInfo, String counterKey) {
    String sourceId = MonitorServerHelperShared.getSourceKey(eventDataInfo, counterKey);
    logger
        .debug("begin saveToPerformanceQueue: sourceId is:{}, counterKey:{}", sourceId, counterKey);

    Map<SocketAddress, List<String>> remoteConnectionMap = WebSocketServer
        .getInstance(webSocketServerPort).getRemoteConnectionMap();
    List<String> sourceIds = new ArrayList<>();
    for (Map.Entry<SocketAddress, List<String>> entry : remoteConnectionMap.entrySet()) {
      sourceIds.addAll(entry.getValue());
    }
    if (!sourceIds.contains(sourceId)) {
      logger.debug("no client is watching this performance, counterKey is: {}", counterKey);
      return;
    }

    logger.debug("started sourceIds is: {}", sourceIds);

    PerformanceMessage performanceMessage = MonitorServerHelperShared
        .buildPerformanceMessageFrom(eventDataInfo, counterKey);
    if (performanceMessage == null) {
      logger.warn("performance is null, eventDataInfo is: {}", eventDataInfo);
      return;
    }
    queueManager.getPerformanceQueue().offer(performanceMessage);
    logger.debug("performance queue size is: {}", queueManager.getPerformanceQueue().size());
  }

  @Override
  public abstract void saveToAlertQueue(EventDataInfo eventDataInfo, String counterKey);

  @Override
  public void saveToEventLogQueue() {
    logger.debug("begin saveToEventLogQueue, eventLogCompressedMap: {}", eventLogCompressedMap);
    Iterator<Map.Entry<String, EventLogCompressed>> iterator = eventLogCompressedMap.entrySet()
        .iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, EventLogCompressed> next = iterator.next();
      EventLogCompressed logCompressed = next.getValue();
      PerformanceMessageHistory performanceMessageHistory = MonitorServerHelperShared
          .buildPerformanceMessageHistoryFrom(logCompressed);
      iterator.remove();
      queueManager.getEventLogQueue().offer(performanceMessageHistory);
    }
  }

  @Override
  public Map<String, EventLogCompressed> getEventLogCompressedMap() {
    return eventLogCompressedMap;
  }

}
