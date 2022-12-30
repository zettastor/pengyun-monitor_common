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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * xx.
 */
public class WebSocketClientFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

  private static Logger logger = LoggerFactory.getLogger(WebSocketClientFrameHandler.class);
  private Channel ch;

  public WebSocketClientFrameHandler() {
  }

  @Override
  public void channelRead0(ChannelHandlerContext context, WebSocketFrame frame) {
    /*
     * Ping and close has been processed automatically by the WebSocketClientProtocolHandler
     */
    if (frame instanceof TextWebSocketFrame) {
      TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
      logger.info("Client {} received: {}", context.channel().localAddress(), textFrame.text());
      //            context.channel().writeAndFlush(new PingWebSocketFrame());
      frame.retain();
    } else {
      String message = "unsupport frame type = " + frame.getClass().getName();
      throw new UnsupportedOperationException(message);
    }

  }

  @Override
  public void userEventTriggered(ChannelHandlerContext context, Object evt) throws Exception {
    if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
      ch = context.channel();
      //            logger.info("client has been connected");
    } else {
      super.userEventTriggered(context, evt);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable t) {
    context.close();
    logger.error("Caught an exception: " + t);
  }
}
