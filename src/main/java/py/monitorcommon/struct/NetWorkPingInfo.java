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
