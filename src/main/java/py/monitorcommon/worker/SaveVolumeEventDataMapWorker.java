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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.app.context.AppContext;
import py.instance.InstanceStatus;
import py.monitor.common.CounterName;
import py.monitor.common.EventDataInfo;
import py.monitor.common.EventLogInfo;
import py.monitorcommon.manager.EventLogManager;
import py.periodic.Worker;

public class SaveVolumeEventDataMapWorker implements Worker {

  private static final Logger logger = LoggerFactory.getLogger(SaveVolumeEventDataMapWorker.class);

  private AppContext appContext;
  private EventLogManager eventLogManager;

  public SaveVolumeEventDataMapWorker(EventLogManager eventLogManager) {
    this.eventLogManager = eventLogManager;
  }

  @Override
  public void doWork() {
    if (appContext.getStatus() == InstanceStatus.SUSPEND) {
      logger.info("This MonitorServer is suspend.");
      return;
    }

    processVolumeEventData();
  }

  public void processVolumeEventData() {
    Map<String, EventDataInfo> volumeEventDataMap = computeVolumeEventDataMap();
    Map<String, EventDataInfo> storagePoolEventDataMap = computeStoragePoolEventDataMap();
    EventDataInfo systemEventData = computeSystemEventData();

    // process the volume, storagePool and system eventData.
    for (Map.Entry<String, EventDataInfo> entry : volumeEventDataMap.entrySet()) {
      eventLogManager.processSingleEventDataInfo(entry.getValue());
    }
    for (Map.Entry<String, EventDataInfo> entry : storagePoolEventDataMap.entrySet()) {
      EventDataInfo eventDataInfo = entry.getValue();
      Set<String> userDefineKeys = new HashSet<>();
      userDefineKeys.add("StoragePoolID");
      userDefineKeys.add("StoragePoolName");
      EventLogInfo eventLogInfo = eventDataInfo
          .buildFromEventDataInfo(userDefineKeys, "StoragePool");

      eventDataInfo.setEventLogInfo(eventLogInfo);

      eventLogManager.processSingleEventDataInfo(eventDataInfo);

    }
    eventLogManager.processSingleEventDataInfo(systemEventData);
  }

  public Map<String, EventDataInfo> computeVolumeEventDataMap() {
    // 1, deep clone eventData in volumeEventDataMap, and clear the map in eventLogManager;
    Map<String, EventDataInfo> volumeEventDataMap = new HashMap<>();
    logger.debug("before compute, volumeEventDataMap is: {}",
        eventLogManager.getVolumeEventDataMap());
    Iterator<Map.Entry<String, EventDataInfo>> volumeIterator = eventLogManager
        .getVolumeEventDataMap().entrySet().iterator();
    while (volumeIterator.hasNext()) {
      Map.Entry<String, EventDataInfo> entry = volumeIterator.next();
      volumeEventDataMap.put(entry.getKey(), entry.getValue().deepClone());
      volumeIterator.remove();
    }

    // 2, compute counter value according to validEventDataCount
    for (Map.Entry<String, EventDataInfo> entry : volumeEventDataMap.entrySet()) {
      EventDataInfo eventDataInfo = entry.getValue();
      if (eventDataInfo.getValidEventDataCount().get() == 0) {
        continue;
      }
      Map<String, AtomicLong> counters = eventDataInfo.getCounters();
      for (Map.Entry<String, AtomicLong> entry1 : counters.entrySet()) {
        switch (CounterName.valueOf(entry1.getKey())) {
          case VOLUME_READ_LATENCY:
          case VOLUME_WRITE_LATENCY:
          case VOLUME_IO_BLOCK_SIZE:
            entry1.getValue()
                .set(entry1.getValue().get() / eventDataInfo.getValidEventDataCount().get());
            break;
          default:
        }
      }
    }
    logger.debug("after compute, volumeEventDataMap is: {}", volumeEventDataMap);
    return volumeEventDataMap;
  }

  public Map<String, EventDataInfo> computeStoragePoolEventDataMap() {
    // 1, deep clone eventData in storagePoolEventDataMap, and clear the map in eventLogManager;
    Map<String, EventDataInfo> storagePoolEventDataMap = new HashMap<>();
    logger.debug("before compute, storagePoolEventDataMap is: {}",
        eventLogManager.getStoragePoolEventDataMap());
    Iterator<Map.Entry<String, EventDataInfo>> storagePoolIterator = eventLogManager
        .getStoragePoolEventDataMap()
        .entrySet().iterator();
    while (storagePoolIterator.hasNext()) {
      Map.Entry<String, EventDataInfo> entry = storagePoolIterator.next();
      storagePoolEventDataMap.put(entry.getKey(), entry.getValue().deepClone());
      storagePoolIterator.remove();
    }

    // 2, compute counter value according to validEventDataCount
    for (Map.Entry<String, EventDataInfo> entry : storagePoolEventDataMap.entrySet()) {
      EventDataInfo eventDataInfo = entry.getValue();
      if (eventDataInfo.getValidEventDataCount().get() == 0) {
        continue;
      }
      Map<String, AtomicLong> counters = eventDataInfo.getCounters();
      for (Map.Entry<String, AtomicLong> entry1 : counters.entrySet()) {
        switch (CounterName.valueOf(entry1.getKey())) {
          case STORAGEPOOL_READ_LATENCY:
          case STORAGEPOOL_WRITE_LATENCY:
          case STORAGEPOOL_IO_BLOCK_SIZE:
            entry1.getValue()
                .set(entry1.getValue().get() / eventDataInfo.getValidEventDataCount().get());
            break;
          default:
        }
      }
    }
    logger.debug("after compute, storagePoolEventDataMap is: {}", storagePoolEventDataMap);
    return storagePoolEventDataMap;
  }

  public EventDataInfo computeSystemEventData() {
    // 1, deep clone systemEventData, and clear systemEventData in eventLogManager;
    EventDataInfo systemEventData = null;
    logger.debug("before compute, systemEventData is: {}",
        eventLogManager.getSystemEventData().get());
    if (eventLogManager.getSystemEventData().get() != null) {
      systemEventData = eventLogManager.getSystemEventData().get().deepClone();
      eventLogManager.getSystemEventData().set(null);
    }

    // 2, compute counter value according to validEventDataCount
    if (systemEventData != null && systemEventData.getValidEventDataCount().get() != 0) {
      Map<String, AtomicLong> systemCounters = systemEventData.getCounters();
      for (Map.Entry<String, AtomicLong> entry1 : systemCounters.entrySet()) {
        switch (CounterName.valueOf(entry1.getKey())) {
          case SYSTEM_READ_LATENCY:
          case SYSTEM_WRITE_LATENCY:
          case SYSTEM_IO_BLOCK_SIZE:
            entry1.getValue()
                .set(entry1.getValue().get() / systemEventData.getValidEventDataCount().get());
            break;
          default:
        }
      }
    }
    logger.debug("after compute, systemEventData is: {}", systemEventData);
    return systemEventData;
  }


  public void setAppContext(AppContext appContext) {
    this.appContext = appContext;
  }
}
