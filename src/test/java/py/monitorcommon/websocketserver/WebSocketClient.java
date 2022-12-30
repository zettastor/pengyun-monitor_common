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
