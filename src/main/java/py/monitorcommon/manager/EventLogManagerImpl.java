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

package py.monitorcommon.manager;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.monitor.common.CounterName;
import py.monitor.common.EventDataInfo;
import py.monitor.common.OperationName;
import py.monitor.common.UserDefineName;
import py.monitorcommon.processor.EventLogProcessor;

public class EventLogManagerImpl implements EventLogManager {

  private static final Logger logger = LoggerFactory.getLogger(EventLogManagerImpl.class);
  private final long timeDelay;
  private final long timeWindow;
  private volatile long startTime;
  private volatile long endTime;
  private Map<String, EventDataInfo> volumeEventDataMap;
  private Map<String, EventDataInfo> storagePoolEventDataMap;
  private AtomicReference<EventDataInfo> systemEventData;
  private EventLogProcessorFactory eventLogProcessorFactory;

  public EventLogManagerImpl(EventLogProcessorFactory eventLogProcessorFactory,
      long timeDelaySecond, long timeWindowSecond) {
    this.eventLogProcessorFactory = eventLogProcessorFactory;
    this.volumeEventDataMap = new ConcurrentHashMap<>();
    this.storagePoolEventDataMap = new ConcurrentHashMap<>();
    this.systemEventData = new AtomicReference<>();
    this.timeDelay = timeDelaySecond;
    this.timeWindow = timeWindowSecond;

    this.startTime = System.currentTimeMillis();
    this.endTime = startTime + timeWindow * 1000;
  }

  @Override
  public void processEventDataInfoList(List<EventDataInfo> eventDataList) {
    if (eventDataList == null || eventDataList.isEmpty()) {
      logger.error("event data list is empty, may caught something exception");
      return;
    }
    for (EventDataInfo eventDataInfo : eventDataList) {
      logger.debug("eventData is : {}", eventDataInfo);
      if (Objects.equals(eventDataInfo.getOperation(), OperationName.Volume.name())
          || Objects.equals(eventDataInfo.getOperation(), OperationName.Container.name())) {
        logger.debug("volume eventData is: {}", eventDataInfo);
        processVolumeEventDataInfo(eventDataInfo);
      } else {
        processSingleEventDataInfo(eventDataInfo);
      }
    }
  }

  @Override
  public void processSingleEventDataInfo(EventDataInfo eventDataInfo) {
    if (eventDataInfo == null) {
      logger.debug("event data is null, may caught something exception");
      return;
    }

    long dataStartTime = eventDataInfo.getStartTime() + timeDelay * 1000;
    logger.debug("dataStartTime is: {}, startTime: {}, endTime: {}, TIME_WINDOW: {}", dataStartTime,
        startTime,
        endTime, timeWindow);

    if (dataStartTime < startTime) { // older data, discard it
      logger
          .warn("it's older data, discard it, dataStartTime: {}, startTime: {}, eventDataInfo: {}",
              dataStartTime, startTime, eventDataInfo);
    } else if (dataStartTime < endTime) { // data in current time window which we concerned
      logger.debug("data in current time window, dataStartTime : {}, startTime : {}", dataStartTime,
          startTime);
      eventDataHandler(eventDataInfo);
    } else { // data of the next timeWindow
      logger
          .info("data of the next timeWindow, dataStartTime: {}, startTime: {}, eventDataInfo: {}",
              dataStartTime, startTime, eventDataInfo);
      long now = System.currentTimeMillis();
      if (now >= endTime) {
        synchronized (EventLogManagerImpl.class) {
          if (now >= endTime) {
            startTime = endTime;
            endTime += timeWindow * 1000;
            saveToEventLogQueue();
          }
        }
      }
      eventDataHandler(eventDataInfo);
    }
  }

  /**
   * data in current time window, do the following things : 1. save to eventLogCompressedMap 2. save
   * to performanceQueue 3. save to alertQueue
   */
  public void eventDataHandler(EventDataInfo eventDataInfo) {
    logger.debug("begin eventDataHandler, eventDataInfo: {}", eventDataInfo);

    Set<String> counterKeys = eventDataInfo.getCounters().keySet();
    if (counterKeys.isEmpty()) {
      logger.error("counters is empty.");
      return;
    }

    for (String counterKey : counterKeys) {
      EventLogProcessor eventLogProcessor = eventLogProcessorFactory
          .createEventLogProcessor(counterKey);

      if (CounterName.valueOf(counterKey).isPerformanceItem()) {
        eventLogProcessor.saveToEventLogCompressdMap(eventDataInfo, counterKey);
        eventLogProcessor.saveToPerformanceQueue(eventDataInfo, counterKey);
      }
      if (CounterName.valueOf(counterKey).isAlertItem()) {
        eventLogProcessor.saveToAlertQueue(eventDataInfo, counterKey);
      }
    }
  }

  /**
   * data in the next time window, save eventLogCompressedMap to eventlogQueue.
   */
  public void saveToEventLogQueue() {
    Collection<EventLogProcessor> eventLogProcessorCollection = eventLogProcessorFactory
        .getEventLogProcessorList();
    logger.debug("event log processor collection is : {}", eventLogProcessorCollection);
    if (eventLogProcessorCollection == null || eventLogProcessorCollection.isEmpty()) {
      return;
    }
    eventLogProcessorCollection.forEach(EventLogProcessor::saveToEventLogQueue);

  }

  /**
   * One volume can be writen and read by more than one Coordinate. For example, when two Coordinate
   * generate volume event data at the same time, we should statistic the sum of two event data. We
   * use the xxxEventDataMap to save the volume, storagePool and system event data in one second. We
   * don't care how many event data came in one second, only add them to the xxxEventDataMap.
   *
   * @param eventDataInfo eventDataInfo
   */
  public void processVolumeEventDataInfo(EventDataInfo eventDataInfo) {
    String volumeId = eventDataInfo.getNameValues().get(UserDefineName.VolumeID.toString());
    String storagePoolId = eventDataInfo.getNameValues()
        .get(UserDefineName.StoragePoolID.toString());

    EventDataInfo lastVolumeEventData = volumeEventDataMap.get(volumeId);
    EventDataInfo lastStoragePoolEventData = storagePoolEventDataMap.get(storagePoolId);
    boolean existIo;
    if (eventDataInfo.getCounter(CounterName.VOLUME_IO_BLOCK_SIZE.toString()) == 0) {
      existIo = false;
    } else {
      existIo = true;
      eventDataInfo.incValidEventDataCount();
    }

    if (lastVolumeEventData == null) {
      lastVolumeEventData = volumeEventDataMap.putIfAbsent(volumeId, eventDataInfo.deepClone());
    }
    if (lastStoragePoolEventData == null) {
      EventDataInfo storagePoolEventDataInfo = buildStoragePoolEventDataInfo(eventDataInfo,
          existIo);
      lastStoragePoolEventData = storagePoolEventDataMap
          .putIfAbsent(storagePoolId, storagePoolEventDataInfo);
    }
    EventDataInfo lastSystemEventData = systemEventData.get();
    if (lastSystemEventData == null) {
      EventDataInfo systemEventDataInfo = buildSystemEventDataInfo(eventDataInfo, existIo);
      if (!systemEventData.compareAndSet(null, systemEventDataInfo)) {
        lastSystemEventData = systemEventData.get();
      }
    }

    /*
    one volume can be drived by multi driver, we should not add the 0 data to the lastXxxEventData;
    if the driver did not read and write, and we added the times, the counter value will be 
    incorrect.
     */
    if (!existIo) {
      return;
    }

    for (String volumeCounterKey : eventDataInfo.getCounters().keySet()) {
      Long incrementCounterValue = eventDataInfo.getCounter(volumeCounterKey);
      if (lastVolumeEventData != null) {
        lastVolumeEventData.addAndGetCounter(volumeCounterKey, incrementCounterValue);
      }
      if (lastStoragePoolEventData != null) {
        String storagePoolCounterKey = buildStoragePoolCounterKey(volumeCounterKey);
        lastStoragePoolEventData.addAndGetCounter(storagePoolCounterKey, incrementCounterValue);
      }
      if (lastSystemEventData != null) {
        String systemCounterKey = buildSystemCounterKey(volumeCounterKey);
        lastSystemEventData.addAndGetCounter(systemCounterKey, incrementCounterValue);
      }
    }

    if (lastVolumeEventData != null) {
      lastVolumeEventData.incValidEventDataCount();
    }
    if (lastStoragePoolEventData != null) {
      lastStoragePoolEventData.incValidEventDataCount();
    }
    if (lastSystemEventData != null) {
      lastSystemEventData.incValidEventDataCount();
    }
  }

  private EventDataInfo buildStoragePoolEventDataInfo(EventDataInfo volumeEventDataInfo,
      boolean existIo) {
    EventDataInfo storagePoolEventDataInfo = new EventDataInfo();
    storagePoolEventDataInfo.setStartTime(volumeEventDataInfo.getStartTime());
    storagePoolEventDataInfo.setRemoteAddr(volumeEventDataInfo.getRemoteAddr());
    storagePoolEventDataInfo.setHostName(volumeEventDataInfo.getHostName());
    storagePoolEventDataInfo.setProgram(volumeEventDataInfo.getProgram());
    storagePoolEventDataInfo.setOperation(OperationName.StoragePool.toString());
    storagePoolEventDataInfo.getNameValues().putAll(volumeEventDataInfo.getNameValues());
    for (Map.Entry<String, AtomicLong> entry : volumeEventDataInfo.getCounters().entrySet()) {
      String volumeCounterKey = entry.getKey();
      long volumeCounterValue = entry.getValue().get();
      storagePoolEventDataInfo
          .putCounter(buildStoragePoolCounterKey(volumeCounterKey), volumeCounterValue);
    }
    if (existIo) {
      storagePoolEventDataInfo.incValidEventDataCount();
    }
    return storagePoolEventDataInfo;
  }

  private EventDataInfo buildSystemEventDataInfo(EventDataInfo volumeEventDataInfo,
      boolean existIo) {
    EventDataInfo systemEventDataInfo = new EventDataInfo();
    systemEventDataInfo.setStartTime(volumeEventDataInfo.getStartTime());
    systemEventDataInfo.setRemoteAddr(null);
    systemEventDataInfo.setHostName(null);
    systemEventDataInfo.setProgram(volumeEventDataInfo.getProgram());
    systemEventDataInfo.setOperation(OperationName.SYSTEM.toString());
    for (Map.Entry<String, AtomicLong> entry : volumeEventDataInfo.getCounters().entrySet()) {
      String volumeCounterKey = entry.getKey();
      long volumeCounterValue = entry.getValue().get();
      systemEventDataInfo.putCounter(buildSystemCounterKey(volumeCounterKey), volumeCounterValue);
    }
    if (existIo) {
      systemEventDataInfo.incValidEventDataCount();
    }
    return systemEventDataInfo;
  }

  private String buildStoragePoolCounterKey(String volumeCounterKey) {
    switch (CounterName.valueOf(volumeCounterKey)) {
      case VOLUME_READ_IOPS:
        return CounterName.STORAGEPOOL_READ_IOPS.toString();
      case VOLUME_WRITE_IOPS:
        return CounterName.STORAGEPOOL_WRITE_IOPS.toString();
      case VOLUME_READ_THROUGHPUT:
        return CounterName.STORAGEPOOL_READ_THROUGHPUT.toString();
      case VOLUME_WRITE_THROUGHPUT:
        return CounterName.STORAGEPOOL_WRITE_THROUGHPUT.toString();
      case VOLUME_READ_LATENCY:
        return CounterName.STORAGEPOOL_READ_LATENCY.toString();
      case VOLUME_WRITE_LATENCY:
        return CounterName.STORAGEPOOL_WRITE_LATENCY.toString();
      case VOLUME_IO_BLOCK_SIZE:
        return CounterName.STORAGEPOOL_IO_BLOCK_SIZE.toString();
      default:
    }
    return null;
  }

  private String buildSystemCounterKey(String volumeCounterKey) {
    switch (CounterName.valueOf(volumeCounterKey)) {
      case VOLUME_READ_IOPS:
        return CounterName.SYSTEM_READ_IOPS.toString();
      case VOLUME_WRITE_IOPS:
        return CounterName.SYSTEM_WRITE_IOPS.toString();
      case VOLUME_READ_THROUGHPUT:
        return CounterName.SYSTEM_READ_THROUGHPUT.toString();
      case VOLUME_WRITE_THROUGHPUT:
        return CounterName.SYSTEM_WRITE_THROUGHPUT.toString();
      case VOLUME_READ_LATENCY:
        return CounterName.SYSTEM_READ_LATENCY.toString();
      case VOLUME_WRITE_LATENCY:
        return CounterName.SYSTEM_WRITE_LATENCY.toString();
      case VOLUME_IO_BLOCK_SIZE:
        return CounterName.SYSTEM_IO_BLOCK_SIZE.toString();
      default:
    }
    return null;
  }

  @Override
  public Map<String, EventDataInfo> getVolumeEventDataMap() {
    return volumeEventDataMap;
  }

  @Override
  public Map<String, EventDataInfo> getStoragePoolEventDataMap() {
    return storagePoolEventDataMap;
  }

  @Override
  public AtomicReference<EventDataInfo> getSystemEventData() {
    return systemEventData;
  }
}
