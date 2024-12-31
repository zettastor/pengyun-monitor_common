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

package py.monitorcommon.websocketserver;

import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.Utils;
import py.monitor.common.AlertLevel;
import py.monitor.common.AlertMessage;
import py.monitor.common.AlertType;
import py.monitor.common.CounterName;
import py.monitor.common.PerformanceMessage;

/**
 * xx.
 */
public class WebSocketServerTest {

  private static final int port = 8083;
  private static final WebSocketServer server = WebSocketServer.getInstance(port);
  private static final Logger logger = LoggerFactory.getLogger(WebSocketServerTest.class);

  @Before
  public void init() {
    server.stop();
  }

  /**
   * xx.
   */
  public void startClient(int clientCount) {
    try {
      for (int i = 0; i < clientCount; i++) {
        WebSocketClient client = new WebSocketClient(port);
        client.start();
        Thread.sleep(200);
        client.write("I am a client: " + client.getChannel().localAddress());
        Thread.sleep(200);
      }
    } catch (Exception e) {
      logger.error("Caught an exception: " + e);
    }

  }

  //    @Test
  //    public void testStart() throws Exception {
  //        int count = 5;
  //        server.start();
  //        startClient(count);
  //        assertTrue(server.getChannelGroup().size() >= count);
  //    }
  //
  //    @Test
  //    public void testWrite() throws InterruptedException {
  //        server.start();
  //        startClient(5);
  //        server.write("hello, I am server! ");
  //        Thread.sleep(1000 * 10);
  //    }

  @Test
  public void testWriteObject() throws InterruptedException {
    server.start();
    PerformanceMessage performanceMessage = generatePerformanceMessage();
    AlertMessage alertMessage = generateAlertMessage();
    for (int i = 0; i < 10; i++) {
      if (i % 2 == 0) {
        server.write(performanceMessage);
      } else {
        server.write(alertMessage);
      }
      int seconds = Utils.generateRandomNum(0, 5);
      Thread.sleep(1000 * seconds);
    }
  }

  /**
   * xx.
   */
  public PerformanceMessage generatePerformanceMessage() {
    PerformanceMessage performanceMessage = new PerformanceMessage();
    performanceMessage.setStartTime(new Date().getTime());
    performanceMessage.setSourceId("10.0.1.110");
    performanceMessage.setCounterKey("CPU");
    performanceMessage.setCounterValue(79);
    return performanceMessage;
  }

  /**
   * xx.
   */
  public AlertMessage generateAlertMessage() {
    AlertMessage alertMessage = new AlertMessage();
    alertMessage.setSourceId("10.0.1.100");
    alertMessage.setSourceName("turtle");
    alertMessage.setAlertDescription("CPU generated alertMessage");
    alertMessage.setAlertLevel(AlertLevel.MINOR.toString());
    alertMessage.setAlertType(AlertType.EQUIPMENT.toString());
    alertMessage.setAlertRuleName(CounterName.CPU.toString());
    alertMessage.setAlertAcknowledge(false);
    alertMessage.setAlertAcknowledgeTime(0L);
    alertMessage.setFirstAlertTime(new Date().getTime());
    alertMessage.setLastAlertTime(new Date().getTime());
    alertMessage.setAlertFrequency(1);
    return alertMessage;
  }
}
