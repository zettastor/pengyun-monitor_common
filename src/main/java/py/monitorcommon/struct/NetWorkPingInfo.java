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

package py.monitorcommon.struct;

import py.monitor.common.CounterName;
import py.monitor.common.EventDataInfo;

/**
 * the info of ping.
 */
public class NetWorkPingInfo {

  public static final long MAX_RECORD_SAVE_TIME_SECOND = 60 * 1000;

  private String srcIp;       //fping dest ip
  private String destIp;      //fping src ip
  private boolean overThreshold; // is over threshold for status alert
  private long timeStamp;
  private long dropped;       //net work dropped percent
  private long delay;         //net work delay time
  private CounterName counterName;
  private EventDataInfo eventDataInfo;

  public NetWorkPingInfo(String srcIp, String destIp, long timeStamp,
      CounterName counterName, EventDataInfo eventDataInfo) {
    this.srcIp = srcIp;
    this.destIp = destIp;
    this.timeStamp = timeStamp;
    this.counterName = counterName;
    this.eventDataInfo = eventDataInfo;
  }

  public String getSrcIp() {
    return srcIp;
  }

  public void setSrcIp(String srcIp) {
    this.srcIp = srcIp;
  }

  public String getDestIp() {
    return destIp;
  }

  public void setDestIp(String destIp) {
    this.destIp = destIp;
  }

  public long getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(long timeStamp) {
    if (this.timeStamp < timeStamp) {
      this.timeStamp = timeStamp;
    }
  }

  public boolean isOverThreshold() {
    return overThreshold;
  }

  public void setOverThreshold(boolean overThreshold) {
    this.overThreshold = overThreshold;
  }

  public CounterName getCounterName() {
    return counterName;
  }

  public void setCounterName(CounterName counterName) {
    this.counterName = counterName;
  }

  public EventDataInfo getEventDataInfo() {
    return eventDataInfo;
  }

  public void setEventDataInfo(EventDataInfo eventDataInfo) {
    this.eventDataInfo = eventDataInfo;
  }

  public long getDropped() {
    return dropped;
  }

  public void setDropped(long dropped) {
    this.dropped = dropped;
  }

  public long getDelay() {
    return delay;
  }

  public void setDelay(long delay) {
    this.delay = delay;
  }
}
