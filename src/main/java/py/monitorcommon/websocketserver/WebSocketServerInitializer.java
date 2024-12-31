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