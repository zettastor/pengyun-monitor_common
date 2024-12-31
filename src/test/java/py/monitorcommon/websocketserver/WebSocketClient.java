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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import java.net.URI;

/**
 * this class is used to test.
 */
public class WebSocketClient {

  private int port;
  private String url;
  private NioEventLoopGroup group;
  private Channel channel = null;

  /**
   * xx.
   */
  public WebSocketClient(int port) {
    this.port = port;
    this.url = System
        .getProperty("url", "ws://localhost:" + port + WebSocketServer.suffix);
  }

  /**
   * xx.
   */
  public void start() throws Exception {
    URI uri = new URI(url);
    final int port = uri.getPort();
    group = new NioEventLoopGroup();
    Bootstrap b = new Bootstrap();
    b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        p.addLast("EncodeAndDecodeToFromByte", new HttpClientCodec());
        p.addLast("OnlyFullHttpResponse", new HttpObjectAggregator(65536));
        p.addLast(new WebSocketClientProtocolHandler(WebSocketClientHandshakerFactory
            .newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders())));
        p.addLast("WebSocketClientFrameHandler", new WebSocketClientFrameHandler());
      }
    });
    channel = b.connect(uri.getHost(), port).sync().channel();
  }

  public void write(String msg) {
    WebSocketFrame frame = new TextWebSocketFrame(msg);
    channel.writeAndFlush(frame);
  }

  public void ping() {
    WebSocketFrame frame = new PingWebSocketFrame();
    channel.writeAndFlush(frame);
  }

  /**
   * xx.
   */
  public void close() throws Exception {
    channel.writeAndFlush(new CloseWebSocketFrame());
    channel.closeFuture().sync();
    group.shutdownGracefully();
    channel.closeFuture().sync();
  }

  public Channel getChannel() {
    return channel;
  }
}
