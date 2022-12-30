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

package py.monitorcommon.websocketserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.ImmediateEventExecutor;
import java.net.SocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.CycleDetectionStrategy;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.monitor.common.AlertLevel;
import py.monitor.common.AlertMessage;
import py.monitor.common.AlertType;
import py.monitor.common.CounterName;
import py.monitor.common.PerformanceMessage;

public class WebSocketServer {

  public static final String suffix = "/ws";
  private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);
  private static WebSocketServer webSocketServer = null;
  private EventLoopGroup bossGroup = null;
  private EventLoopGroup workerGroup = null;
  private ChannelGroup channelGroup = null;
  private Map<SocketAddress, List<String>> remoteConnectionMap;

  private Channel channel = null;
  private int port;
  private long webSocketHeartbeatTimeoutMs;

  private WebSocketServer(int port) {
    this.port = port;
  }

  public static WebSocketServer getInstance(int port) {
    if (webSocketServer == null) {
      synchronized (WebSocketServer.class) {
        if (webSocketServer == null) {
          webSocketServer = new WebSocketServer(port);
          webSocketServer.remoteConnectionMap = new ConcurrentHashMap<>();
        }
      }
    }
    return webSocketServer;
  }

  /**
   * just for test generate a web socket server.
   *
   * @param args args
   * @throws InterruptedException InterruptedException
   */
  public static void main(String[] args) throws InterruptedException {
    WebSocketServer server = WebSocketServer.getInstance(19890);
    server.start();

    //generator
    PerformanceMessage performanceMessage = new PerformanceMessage();
    performanceMessage.setStartTime(new Date().getTime());
    performanceMessage.setSourceId("10.0.1.110");
    performanceMessage.setCounterKey("CPU");
    performanceMessage.setCounterValue(79);

    AlertMessage alertMessage = new AlertMessage();
    alertMessage.setSourceId("10.0.1.100");
    alertMessage.setSourceName("turtle");
    alertMessage.setAlertDescription("CPU generated alertMessage");
    alertMessage.setAlertLevel(AlertLevel.MINOR.toString());
    alertMessage.setAlertType(AlertType.EQUIPMENT.toString());
    alertMessage.setAlertRuleName(CounterName.CPU.toString());
    alertMessage.setCounterKey(CounterName.CPU.toString());
    alertMessage.setAlertAcknowledge(false);
    alertMessage.setAlertAcknowledgeTime(0L);
    alertMessage.setFirstAlertTime(new Date().getTime());
    alertMessage.setLastAlertTime(new Date().getTime());
    alertMessage.setAlertFrequency(1);

    Map<SocketAddress, List<String>> msgFromClient = server.getRemoteConnectionMap();
    while (true) {
      if ((msgFromClient != null) && !msgFromClient.isEmpty()) {
        logger.warn("Start send message!");
        int i = 0;
        while (true) {
          if (msgFromClient.isEmpty()) {
            logger.warn("Stop send message!");
            //server.stop();
            //return;
            break;
          }

          if (i % 2 == 0) {
            server.write(performanceMessage);
          } else {
            server.write(alertMessage);
          }
          i++;

          //int seconds = Utils.generateRandomNum(0, 5);
          Thread.sleep(5 * 1000);
        }
      }
      Thread.sleep(1000);
    }
  }

  public void start() {
    logger.warn("WebSocket Server start!");
    bossGroup = new NioEventLoopGroup();
    workerGroup = new NioEventLoopGroup();
    channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    ServerBootstrap bootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class);
    //                .option(ChannelOption.SO_BACKLOG, 256)
    //                .childOption(ChannelOption.SO_KEEPALIVE, true)
    //                .childOption(ChannelOption.TCP_NODELAY, true)
    // .childOption(ChannelOption.SO_KEEPALIVE, true)
    //                .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024 * 10)
    //                .childOption(ChannelOption.WRITE_SPIN_COUNT, 50);

    WebSocketServerInitializer webSocketServerInitializer = new WebSocketServerInitializer(
        channelGroup);
    webSocketServerInitializer.setRemoteConnectionMap(remoteConnectionMap);
    webSocketServerInitializer.setWebSocketHeartbeatTimeoutMs(webSocketHeartbeatTimeoutMs);
    bootstrap.childHandler(webSocketServerInitializer);

    try {
      ChannelFuture channelFuture = bootstrap.bind(port).sync();
      channel = channelFuture.channel();
      //            channel.closeFuture().sync();
    } catch (InterruptedException e) {
      logger.error("caught an exception when start webSocket: " + e);
    }
    logger.info("WebSocket server started at port {}.", port);
  }

  public void stop() {
    if (channel != null) {
      channel.close();
    }
    if (channelGroup != null) {
      channelGroup.close();
    }
    if (bossGroup != null) {
      bossGroup.shutdownGracefully();
    }
    if (workerGroup != null) {
      workerGroup.shutdownGracefully();
    }
  }

  public void write(String info) {
    if (channelGroup != null && !channelGroup.isEmpty()) {
      TextWebSocketFrame text = new TextWebSocketFrame(info);
      channelGroup.writeAndFlush(text);
      logger.info("push info to all clients: " + info);
    }
  }

  public void write(Object object) {
    WebSocketRequest webSocketRequest = new WebSocketRequest();
    if (object instanceof AlertMessage) {
      AlertMessage message = (AlertMessage) object;
      webSocketRequest.setAlertMessage(message);
    } else {
      Validate.isTrue(object instanceof PerformanceMessage);
      PerformanceMessage message = (PerformanceMessage) object;
      webSocketRequest.setPerformanceMessage(message);
    }

    //fix bug that JSONObject.fromObject() cause exception: There is a cycle in the hierarchy
    JsonConfig jsonConfig = new JsonConfig();
    jsonConfig.setCycleDetectionStrategy(CycleDetectionStrategy.LENIENT);

    JSONObject jsonObject;
    try {
      jsonObject = JSONObject.fromObject(webSocketRequest, jsonConfig);
    } catch (Exception e) {
      logger.warn("caught exception when send web socket request:{}", webSocketRequest, e);
      return;
    }

    String request = jsonObject.toString();

    logger.info("send to console request is {}", request);
    if (channelGroup != null && !channelGroup.isEmpty()) {
      TextWebSocketFrame text = new TextWebSocketFrame(request);
      channelGroup.writeAndFlush(text);
      logger.info("websocket push info to all clients: " + request);
    } else {
      logger.warn("channelGroup is null or is Empty");
    }
  }

  public ChannelGroup getChannelGroup() {
    return channelGroup;
  }

  public Map<SocketAddress, List<String>> getRemoteConnectionMap() {
    return remoteConnectionMap;
  }

  public void setWebSocketHeartbeatTimeoutMs(long webSocketHeartbeatTimeoutMs) {
    this.webSocketHeartbeatTimeoutMs = webSocketHeartbeatTimeoutMs;
  }

  /**
   * just for test.
   *
   * @param port port
   */
  public void setPort(int port) {
    this.port = port;
  }
}