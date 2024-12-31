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
