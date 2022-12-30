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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.app.NetworkConfiguration;
import py.app.context.AppContext;
import py.common.PyService;
import py.common.struct.EndPoint;
import py.instance.Instance;
import py.instance.InstanceStatus;
import py.instance.InstanceStore;
import py.instance.PortType;
import py.monitor.common.AlertRule;
import py.monitor.common.AlertTemplate;
import py.monitor.common.CounterName;
import py.monitor.common.EventDataInfo;
import py.monitor.common.OperationName;
import py.monitor.common.UserDefineName;
import py.monitorcommon.manager.MonitorServerHelperShared;
import py.monitorcommon.struct.NetWorkPingInfo;
import py.periodic.Worker;
import py.querylog.eventdatautil.EventDataWorker;

public abstract class SimulateNetWorkPingWorkerShared implements Worker {

  protected static final Logger logger = LoggerFactory
      .getLogger(SimulateNetWorkPingWorkerShared.class);
  protected InstanceStore instanceStore;
  protected NetworkConfiguration networkConfiguration;

  protected long minJudgedNetworkCongestionNodeCount = 1;

  protected Map<String, AlertTemplate> alertTemplateMap;
  protected Map<String, EventDataWorker> ip2EventDataWorker;
  protected Map<String, EventDataWorker> ip2StatusEventDataWorker;

  protected PyService pingService;
  protected AppContext appContext;

  protected long startTime;

  //all record about net work congestion information, <datanode ip, map<coordinate ip, record>>
  protected ConcurrentHashMap<String, ConcurrentHashMap<String, NetWorkPingInfo>>
      netWorkCongestionInfoMap;

  //all record about net work ping information, <ping destIp, map<ping srcIp, record>>
  protected ConcurrentHashMap<String, ConcurrentHashMap<String, NetWorkPingInfo>>
      netWorkPingInfoMap;

  public void setAppContext(AppContext appContext) {
    this.appContext = appContext;
  }

  public void setPingService(PyService pingService) {
    this.pingService = pingService;
  }

  /**
   * update ping result, contains drop and delay.
   *
   * @param srcIp         srcIp
   * @param destIp        destIp
   * @param value         value
   * @param counterName   counterName
   * @param eventDataInfo eventDataInfo
   */
  public synchronized void updateRecord(String srcIp, String destIp, long value,
      CounterName counterName, EventDataInfo eventDataInfo) {
    Validate.notNull(srcIp);
    Validate.notNull(destIp);
    Validate.notNull(counterName);
    Validate.notNull(eventDataInfo);

    ConcurrentHashMap<String, NetWorkPingInfo> pingInfoMap =
        netWorkPingInfoMap.computeIfAbsent(srcIp, v -> new ConcurrentHashMap<>());

    long lastOperationTime = eventDataInfo.getStartTime();
    NetWorkPingInfo netWorkPingInfo = pingInfoMap.computeIfAbsent(destIp,
        v -> new NetWorkPingInfo(srcIp, destIp, lastOperationTime, counterName, eventDataInfo));

    if (counterName.equals(CounterName.NETCARD_DELAY)) {
      netWorkPingInfo.setDelay(value);
    } else if (counterName.equals(CounterName.NETWORK_DROPPED)) {
      netWorkPingInfo.setDropped(value);
    } else {
      logger.error("counter name:{} is not net sub health counter name", counterName);
      return;
    }
    netWorkPingInfo.setTimeStamp(lastOperationTime);
    logger.debug("update record to network ping info. srcip:{} destip:{} value:{} countername:{}",
        srcIp, destIp, value, counterName);
  }

  /**
   * update status alert result, contains network congestion.
   *
   * @param srcIp         srcIp
   * @param destIp        destIp
   * @param factValue     factValue
   * @param overThreshold overThreshold
   * @param counterName   counterName
   * @param eventDataInfo eventDataInfo
   */
  public synchronized void updateStatusAlertRecord(String srcIp, String destIp, long factValue,
      boolean overThreshold,
      long minJudgedNetworkCongestionNodeCount,
      CounterName counterName, EventDataInfo eventDataInfo) {
    Validate.notNull(counterName);
    Validate.notNull(eventDataInfo);

    this.minJudgedNetworkCongestionNodeCount = minJudgedNetworkCongestionNodeCount;

    ConcurrentHashMap<String, NetWorkPingInfo> pingInfoMap =
        netWorkCongestionInfoMap.computeIfAbsent(destIp, v -> new ConcurrentHashMap<>());

    long lastOperationTime = eventDataInfo.getStartTime();
    NetWorkPingInfo netWorkPingInfo = pingInfoMap.computeIfAbsent(srcIp,
        v -> new NetWorkPingInfo(srcIp, destIp, lastOperationTime, counterName, eventDataInfo));

    if (counterName.equals(CounterName.NETWORK_CONGESTION)) {
      netWorkPingInfo.setOverThreshold(overThreshold);
      netWorkPingInfo.setDelay(factValue);
    } else {
      logger.error("counter name:{} is not net sub health counter name", counterName);
      return;
    }
  }

  protected abstract void resolveToMs(String eventDataBuf, String srcIp);

  @Override
  public void doWork() throws Exception {
    if (appContext.getStatus() == InstanceStatus.SUSPEND) {
      logger.info("This MonitorServer is suspend.");
      return;
    }

    logger.debug("simulate net work ping worker");

    //get all datanode endpoint as ping object
    Map<String, String> allDataNodeIoIp2ControlIpMap = new HashMap<>();
    Map<EndPoint, EndPoint> allDataNodeIoEndPoint2ControlEndPoint =
        getAllDataNodeIoEndPoint2ControlEndPoint();
    for (EndPoint ioEndPoint : allDataNodeIoEndPoint2ControlEndPoint.keySet()) {
      EndPoint controlEndPoint = allDataNodeIoEndPoint2ControlEndPoint.get(ioEndPoint);
      allDataNodeIoIp2ControlIpMap.put(ioEndPoint.getHostName(), controlEndPoint.getHostName());
    }

    if (allDataNodeIoIp2ControlIpMap.size() <= 2) {
      logger.warn(
          "there are less 3 datanode at this time, cannot do any check about net sub health. "
              + "all datanode:{}",
          allDataNodeIoEndPoint2ControlEndPoint);
      return;
    }

    //generate threshold alert event data(contains network delay, 
    // network dropped) and simulate send to monitor server
    for (String srcIp : netWorkPingInfoMap.keySet()) {
      String eventDataBuf = generateThresholdEventData(srcIp, allDataNodeIoIp2ControlIpMap);
      if (eventDataBuf == null || eventDataBuf.isEmpty()) {
        logger.warn(
            "generate srcIp:{} threshold event data is null or empty, "
                + "cannot do simulate ping worker now",
            srcIp);
        continue;
      }

      logger.info("simulate ping cmd start");
      resolveToMs(eventDataBuf, srcIp);
      logger.info("simulate ping cmd end");
    }

    //get all coordinate ip

    //generate status alert event data(contains network congestion) 
    // and simulate send to monitor server
    for (String destIp : netWorkCongestionInfoMap.keySet()) {
      ConcurrentHashMap<String, NetWorkPingInfo> srcIp2NetworkPingInfoMap = netWorkCongestionInfoMap
          .get(destIp);
      if (srcIp2NetworkPingInfoMap == null) {
        continue;
      }

      String eventDataBuf = generateStatusEventData(destIp, srcIp2NetworkPingInfoMap,
          allDataNodeIoIp2ControlIpMap);
      if (eventDataBuf == null || eventDataBuf.isEmpty()) {
        logger.warn(
            "generate destIp:{} status event data is null or empty, "
                + "cannot do simulate network congestion now",
            destIp);
        continue;
      }

      logger.info("simulate network congestion alert start");
      resolveToMs(eventDataBuf, destIp);
      logger.info("simulate network congestion alert end");
    }
  }

  /**
   * generate simulate status event data buf.
   *
   * @param srcIp                    srcIp
   * @param srcIp2NetworkPingInfoMap srcIp2NetworkPingInfoMap
   */
  protected String generateStatusEventData(String srcIp,
      ConcurrentHashMap<String, NetWorkPingInfo> srcIp2NetworkPingInfoMap,
      Map<String, String> allDataNodeIoIp2ControlIpMap) {
    //judged is this srcIp has network congestion
    long totalDelayTimesMs = 0;
    int overThresholdCounters = 0;
    for (String coordinateIp : srcIp2NetworkPingInfoMap.keySet()) {
      NetWorkPingInfo netWorkPingInfo = srcIp2NetworkPingInfoMap.get(coordinateIp);
      if (netWorkPingInfo == null) {
        continue;
      }

      if (netWorkPingInfo.isOverThreshold()) {
        overThresholdCounters++;
        totalDelayTimesMs += netWorkPingInfo.getDelay();
      }
    }

    long alert = 1;
    if (overThresholdCounters >= minJudgedNetworkCongestionNodeCount) {
      alert = 0;
    }

    Map<String, Long> counters = new HashMap<>();
    counters.put(CounterName.NETWORK_CONGESTION.toString(), alert);

    EventDataWorker eventDataWorker = ip2StatusEventDataWorker.get(srcIp);
    if (eventDataWorker == null) {
      Map<String, String> userDefineParams = new HashMap<>();
      userDefineParams.put(UserDefineName.PingSourceIp.name(), srcIp);

      //get the remote addr
      String remoteAddr = allDataNodeIoIp2ControlIpMap.get(srcIp);
      /*eventDataWorker = new EventDataWorker(PyService.SYSTEMDAEMON, userDefineParams,
          MonitorServerHelperShared.Unknown, remoteAddr);*/

      ip2EventDataWorker.put(srcIp, eventDataWorker);
    }
    JSONArray jsonRet = new JSONArray();
    eventDataWorker.addToDefaultNameValues(UserDefineName.PingResult.name(), jsonRet.toString());
    eventDataWorker.addToDefaultNameValues(UserDefineName.CongestionDelay.name(),
        String.valueOf(totalDelayTimesMs / overThresholdCounters));

    String eventDataBuf = eventDataWorker.work(OperationName.NETWORK.name(), counters);
    logger.info("NetCard delay and drop info: \n{}", eventDataBuf);
    return eventDataBuf;
  }

  /**
   * generate simulate threshold event data buf.
   *
   * @param srcIp                        srcIp
   * @param allDataNodeIoIp2ControlIpMap allDataNodeIoIp2ControlIpMap
   */
  protected String generateThresholdEventData(String srcIp,
      Map<String, String> allDataNodeIoIp2ControlIpMap) {
    JSONArray pingResultJson = generatePingResult(srcIp, allDataNodeIoIp2ControlIpMap.keySet());
    if (pingResultJson == null || pingResultJson.isEmpty()) {
      logger.warn("generate srcIp:{} ping result is null", srcIp);
      return "";
    }

    Map<String, Long> counters = new HashMap<>();
    counters.put(CounterName.NETCARD_DELAY.toString(), 0L);
    counters.put(CounterName.NETWORK_DROPPED.toString(), 0L);

    //get iface name and active port if iface is team or bond
    String ifaceName = MonitorServerHelperShared.Unknown;
    String activePort = MonitorServerHelperShared.Unknown;
    String networkMacAddr = MonitorServerHelperShared.Unknown;
    String networkCardIp = srcIp;

    EventDataWorker eventDataWorker = ip2EventDataWorker.get(srcIp);
    if (eventDataWorker == null) {
      Map<String, String> userDefineParams = new HashMap<>();
      userDefineParams.put(UserDefineName.PingSourceIp.name(), srcIp);
      userDefineParams.put(UserDefineName.NetCardID.name(), networkMacAddr);
      userDefineParams.put(UserDefineName.NetCardIP.name(), srcIp);

      //get the remote addr
      String remoteAddr = allDataNodeIoIp2ControlIpMap.get(srcIp);
      /*eventDataWorker = new EventDataWorker(PyService.SYSTEMDAEMON, userDefineParams,
          MonitorServerHelperShared.Unknown, remoteAddr);*/

      ip2EventDataWorker.put(networkCardIp, eventDataWorker);
    }
    eventDataWorker.addToDefaultNameValues(UserDefineName.NetCardName.name(), ifaceName);
    eventDataWorker.addToDefaultNameValues(UserDefineName.NetCardActivePort.name(), activePort);
    eventDataWorker
        .addToDefaultNameValues(UserDefineName.PingResult.name(), pingResultJson.toString());

    String eventDataBuf = eventDataWorker.work(OperationName.NETWORK.name(), counters);
    logger.info("NetCard delay and drop info: \n{}", eventDataBuf);
    return eventDataBuf;
  }

  /**
   * generate ping result by record.
   *
   * @param srcIp              srcIp
   * @param allDataNodeIoIpSet allDataNodeIoIpSet
   */
  protected JSONArray generateNetworkCongestionResult(String srcIp,
      Set<String> allDataNodeIoIpSet) {
    JSONArray jsonRet = new JSONArray();

    ConcurrentHashMap<String, NetWorkPingInfo> pingInfoMap = netWorkPingInfoMap.get(srcIp);
    if (pingInfoMap == null) {
      logger.warn("srcIp:{} have no ping record", srcIp);
      return jsonRet;
    }

    long startTime = System.currentTimeMillis();

    EventDataInfo eventDataInfo = new EventDataInfo();
    eventDataInfo.setOperation(CounterName.NETCARD_DELAY.getOperationName().toString());
    eventDataInfo.addNameValue(UserDefineName.PingSourceIp.toString(), srcIp);
    eventDataInfo
        .addNameValue(UserDefineName.NetCardName.toString(), MonitorServerHelperShared.Unknown);
    String sourceKey = MonitorServerHelperShared
        .getSourceKey(eventDataInfo, CounterName.NETCARD_DELAY.toString());
    AlertRule dropAlertRule = MonitorServerHelperShared
        .getAlertRule(alertTemplateMap, sourceKey, CounterName.NETWORK_DROPPED.toString());
    AlertRule delayAlertRule = MonitorServerHelperShared
        .getAlertRule(alertTemplateMap, sourceKey, CounterName.NETCARD_DELAY.toString());

    int invalidCount = 0;
    for (String otherIp : allDataNodeIoIpSet) {
      if (otherIp.equals(srcIp)) {
        continue;
      }

      long droped = 0;   //dropped , unit: %
      long delayUs = 0;    //delay , unit: us
      NetWorkPingInfo record = pingInfoMap.get(otherIp);
      if (record == null) {
        logger.warn("src:{} to dest:{} ping info do not found. set this is over threshold",
            srcIp, otherIp);
        droped = dropAlertRule.getAlertLevelOneThreshold() + 1;
        delayUs = delayAlertRule.getAlertLevelOneThreshold() + 1;
        invalidCount++;
      } else if (startTime - record.getTimeStamp() > NetWorkPingInfo.MAX_RECORD_SAVE_TIME_SECOND) {
        logger.warn(
            "src:{} to dest:{} ping info is older then {}ms, "
                + "cannot used and set this is over threshold",
            srcIp, otherIp, NetWorkPingInfo.MAX_RECORD_SAVE_TIME_SECOND);
        droped = dropAlertRule.getAlertLevelOneThreshold() + 1;
        delayUs = delayAlertRule.getAlertLevelOneThreshold() + 1;
        invalidCount++;
      } else {
        droped = record.getDropped();
        delayUs = record.getDelay();
      }

      //construct a dest result
      JSONObject destJson = new JSONObject();
      destJson.put(UserDefineName.PingDestIp.toString(), otherIp);
      destJson.put(UserDefineName.PingDropped.toString(), droped);
      destJson.put(UserDefineName.PingDelay.toString(), delayUs);

      jsonRet.add(destJson);
    }

    if ((invalidCount * 2) > (allDataNodeIoIpSet.size() - 1)) {
      logger.warn(
          "cannot found ping dest node info count:{} is more than 50% "
              + "(all ping dest node info count:{}), cannot generate ping result",
          invalidCount, allDataNodeIoIpSet.size() - 1);
      return new JSONArray();
    }

    logger.info("generate srcIp:{} ping result:{}", srcIp, jsonRet);
    return jsonRet;
  }

  /**
   * generate ping result by record.
   *
   * @param srcIp              srcIp
   * @param allDataNodeIoIpSet allDataNodeIoIpSet
   */
  protected JSONArray generatePingResult(String srcIp, Set<String> allDataNodeIoIpSet) {
    JSONArray jsonRet = new JSONArray();

    ConcurrentHashMap<String, NetWorkPingInfo> pingInfoMap = netWorkPingInfoMap.get(srcIp);
    if (pingInfoMap == null) {
      logger.warn("srcIp:{} have no ping record", srcIp);
      return jsonRet;
    }

    long startTime = System.currentTimeMillis();

    EventDataInfo eventDataInfo = new EventDataInfo();
    eventDataInfo.setOperation(CounterName.NETCARD_DELAY.getOperationName().toString());
    eventDataInfo.addNameValue(UserDefineName.PingSourceIp.toString(), srcIp);
    eventDataInfo
        .addNameValue(UserDefineName.NetCardName.toString(), MonitorServerHelperShared.Unknown);
    String sourceKey = MonitorServerHelperShared
        .getSourceKey(eventDataInfo, CounterName.NETCARD_DELAY.toString());
    AlertRule dropAlertRule = MonitorServerHelperShared
        .getAlertRule(alertTemplateMap, sourceKey, CounterName.NETWORK_DROPPED.toString());
    AlertRule delayAlertRule = MonitorServerHelperShared
        .getAlertRule(alertTemplateMap, sourceKey, CounterName.NETCARD_DELAY.toString());

    int invalidCount = 0;
    for (String otherIp : allDataNodeIoIpSet) {
      if (otherIp.equals(srcIp)) {
        continue;
      }

      long droped = 0;   //dropped , unit: %
      long delayUs = 0;    //delay , unit: us
      NetWorkPingInfo record = pingInfoMap.get(otherIp);
      if (record == null) {
        logger.warn("src:{} to dest:{} ping info do not found. set this is over threshold",
            srcIp, otherIp);
        droped = dropAlertRule.getAlertLevelOneThreshold() + 1;
        delayUs = delayAlertRule.getAlertLevelOneThreshold() + 1;
        invalidCount++;
      } else if (startTime - record.getTimeStamp() > NetWorkPingInfo.MAX_RECORD_SAVE_TIME_SECOND) {
        logger.warn(
            "src:{} to dest:{} ping info is older then {}ms, "
                + "cannot used and set this is over threshold",
            srcIp, otherIp, NetWorkPingInfo.MAX_RECORD_SAVE_TIME_SECOND);
        droped = dropAlertRule.getAlertLevelOneThreshold() + 1;
        delayUs = delayAlertRule.getAlertLevelOneThreshold() + 1;
        invalidCount++;
      } else {
        droped = record.getDropped();
        delayUs = record.getDelay();
      }

      //construct a dest result
      JSONObject destJson = new JSONObject();
      destJson.put(UserDefineName.PingDestIp.toString(), otherIp);
      destJson.put(UserDefineName.PingDropped.toString(), droped);
      destJson.put(UserDefineName.PingDelay.toString(), delayUs);

      jsonRet.add(destJson);
    }

    if ((invalidCount * 2) > (allDataNodeIoIpSet.size() - 1)) {
      logger.warn(
          "cannot found ping dest node info count:{} is more than 50% "
              + "(all ping dest node info count:{}), cannot generate ping result",
          invalidCount, allDataNodeIoIpSet.size() - 1);
      return new JSONArray();
    }

    logger.info("generate srcIp:{} ping result:{}", srcIp, jsonRet);
    return jsonRet;
  }

  protected Map<EndPoint, EndPoint> getAllDataNodeIoEndPoint2ControlEndPoint() {
    Set<Instance> datanodeInstances = instanceStore.getAll(pingService.getServiceName());

    Map<EndPoint, EndPoint> ioEndPoint2ControlEndPoint;
    if (networkConfiguration.isEnableDataDepartFromControl()) {
      ioEndPoint2ControlEndPoint = parseEndpointsInAnyFlow(datanodeInstances,
          networkConfiguration.getDataFlowSubnet());
    } else {
      ioEndPoint2ControlEndPoint = parseEndpointsInAnyFlow(datanodeInstances,
          networkConfiguration.getControlFlowSubnet());
    }
    return ioEndPoint2ControlEndPoint;
  }

  protected Map<EndPoint, EndPoint> parseEndpointsInAnyFlow(Set<Instance> datanodeInstances,
      String networkFlow) {
    Map<EndPoint, EndPoint> ioEndPoint2ControlEndPoint = new HashMap<>();

    for (Instance instance : datanodeInstances) {
      EndPoint ioEndPoint = instance.getEndPointByServiceName(PortType.IO);
      EndPoint controlEndPoint = instance.getEndPointByServiceName(PortType.CONTROL);
      ioEndPoint2ControlEndPoint.put(ioEndPoint, controlEndPoint);
    }

    logger.debug("get result of io endPoint 2 control endPoint {} in parseEndpoints.",
        ioEndPoint2ControlEndPoint);
    return ioEndPoint2ControlEndPoint;
  }
}
