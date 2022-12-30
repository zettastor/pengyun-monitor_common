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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketServerInitializer.class);

  private ChannelGroup channelGroup = null;
  private Map<SocketAddress, List<String>> remoteConnectionMap;
  private long webSocketHeartbeatTimeoutMs = 20000;


  public WebSocketServerInitializer(ChannelGroup group) {
    this.channelGroup = group;
  }

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    logger.warn("Channel start init");

    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("EncodeAndDecodeToFromByte", new HttpServerCodec());
    pipeline.addLast("OnlyFullHttPRequest", new HttpObjectAggregator(65536));
    pipeline.addLast("DistinctBetweenHttpWithWebSocket",
        new HttpRequestHandler(WebSocketServer.suffix));
    pipeline.addLast("WebSocket",
        new WebSocketServerProtocolHandler(WebSocketServer.suffix, null, true));
    pipeline
        .addLast(new IdleStateHandler(webSocketHeartbeatTimeoutMs, 0, 0, TimeUnit.MILLISECONDS));
    WebSocketServerFrameHandler webSocketServerFrameHandler = new WebSocketServerFrameHandler(
        channelGroup);
    webSocketServerFrameHandler.setRemoteConnectionMap(remoteConnectionMap);
    pipeline.addLast("WebSocketServerFrameHandler", webSocketServerFrameHandler);
  }

  public void setRemoteConnectionMap(Map<SocketAddress, List<String>> remoteConnectionMap) {
    this.remoteConnectionMap = remoteConnectionMap;
  }

  public void setWebSocketHeartbeatTimeoutMs(long webSocketHeartbeatTimeoutMs) {
    if (webSocketHeartbeatTimeoutMs != 0) {
      this.webSocketHeartbeatTimeoutMs = webSocketHeartbeatTimeoutMs;
    }
  }
}