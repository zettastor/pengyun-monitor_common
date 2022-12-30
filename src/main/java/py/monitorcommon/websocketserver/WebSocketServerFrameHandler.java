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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketServerFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

  private final String heartBeatBufPing = "1";
  private final String heartBeatBufPong = "2";
  private Logger logger = LoggerFactory.getLogger(WebSocketServerFrameHandler.class);
  private ChannelGroup channelGroup = null;
  private Map<SocketAddress, List<String>> remoteConnectionMap;

  public WebSocketServerFrameHandler(ChannelGroup group) {
    this.channelGroup = group;
  }

  @Override
  public void channelRead0(ChannelHandlerContext context, WebSocketFrame frame) {
    if (frame instanceof TextWebSocketFrame) {
      TextWebSocketFrame text = (TextWebSocketFrame) frame;
      SocketAddress socketAddress = context.channel().remoteAddress();
      String message = text.text();
      logger
          .info("receive from brower, socketAddress is:{}, message is:{}", socketAddress, message);

      synchronized (WebSocketServerFrameHandler.class) {
        if ("stopAll".equalsIgnoreCase(message)) {
          remoteConnectionMap.remove(socketAddress);
        } else if (heartBeatBufPing.equalsIgnoreCase(message)) {
          sendPongMsg(context);
        } else {
          List<String> sourceIdList = remoteConnectionMap.get(socketAddress);
          //10.0.0.51:/dev/sda##start
          //10.0.0.51:/dev/sda##stop
          String[] split = message.split("##");
          if (split.length != 2) {
            logger
                .warn("receive a cannot parsed message:{} from brower:{}", message, socketAddress);
          } else {
            String sourceId = split[0];
            String status = split[1];
            if ("start".equalsIgnoreCase(status)) {
              if (sourceIdList == null) {
                sourceIdList = new ArrayList<>();
                sourceIdList.add(sourceId);
              } else {
                sourceIdList.add(sourceId);
              }
            } else if ("stop".equalsIgnoreCase(status)) {
              if (sourceIdList != null) {
                sourceIdList.remove(sourceId);
              } else {
                logger.warn("sourceIdList is null, message is: {}", message);
              }
            }
          }
          if (sourceIdList != null) {
            remoteConnectionMap.put(context.channel().remoteAddress(), sourceIdList);
          }
        }
      }
      logger.info("after opening websocket, remote connection map is: {}", remoteConnectionMap);

      /*the following is used to send message to one client*/
      //            String responseInfo = "response-- " + text.text();
      //            context.channel().writeAndFlush(new TextWebSocketFrame(responseInfo));
      //            logger.info("push info to one client {} : {}", 
      //            context.channel().remoteAddress(), responseInfo);
    } else {
      String message = "unsupport frame type = " + frame.getClass().getName();
      logger.warn("the websocketframe is unsupported", new UnsupportedOperationException(message));
    }
  }

  private void sendPongMsg(ChannelHandlerContext ctx) {
    logger.debug("send pong msg of web socket to address:{}", ctx.channel().remoteAddress());
    TextWebSocketFrame buf = new TextWebSocketFrame(heartBeatBufPong);
    ctx.channel().writeAndFlush(buf);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    logger.warn("ChannelHandle added. client: {}. remain {} channel in group.",
        ctx.channel().remoteAddress(), channelGroup.size());
    super.handlerAdded(ctx);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    List<String> remove = remoteConnectionMap.remove(ctx.channel().remoteAddress());
    if (remove != null) {
      logger
          .warn("ChannelHandle removed. remain remote connection map is: {}", remoteConnectionMap);
    }

    logger.warn("ChannelHandle removed. client: {}. remain {} channel in group.",
        ctx.channel().remoteAddress(), channelGroup.size());
    super.handlerRemoved(ctx);
  }

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    logger.warn("ChannelHandle registered. client: {}. remain {} channel in group.",
        ctx.channel().remoteAddress(), channelGroup.size());
    super.channelRegistered(ctx);
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    channelGroup.remove(ctx.channel());

    List<String> remove = remoteConnectionMap.remove(ctx.channel().remoteAddress());
    if (remove != null) {
      logger.warn("ChannelHandle unregistered. remain remote connection map is: {}",
          remoteConnectionMap);
    }

    logger.warn("ChannelHandle unregistered. client: {}. remain {} channel in group.",
        ctx.channel().remoteAddress(), channelGroup.size());
    super.channelUnregistered(ctx);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    channelGroup.add(ctx.channel());

    logger.warn("ChannelHandle active. client: {}. remain {} channel in group.",
        ctx.channel().remoteAddress(), channelGroup.size());
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    channelGroup.remove(ctx.channel());

    List<String> remove = remoteConnectionMap.remove(ctx.channel().remoteAddress());
    if (remove != null) {
      logger
          .warn("ChannelHandle inactive. remain remote connection map is: {}", remoteConnectionMap);
    }

    logger.warn("ChannelHandle inactive. client: {}. remain {} channel in group.",
        ctx.channel().remoteAddress(), channelGroup.size());
    super.channelInactive(ctx);
  }


  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
    channelGroup.remove(ctx.channel());

    List<String> remove = remoteConnectionMap.remove(ctx.channel().remoteAddress());
    if (remove != null) {
      logger.error("ChannelHandle caught exception. remain remote connection map is: {}",
          remoteConnectionMap);
    }

    logger.error(
        "ChannelHandle caught exception. client: {}. remain {} channel in group. "
            + "Caught an exception",
        ctx.channel().remoteAddress(), channelGroup.size(), t);
    ctx.close();
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent event = (IdleStateEvent) evt;
      if (event.state() == IdleState.READER_IDLE) {
        synchronized (channelGroup) {
          logger.warn(
              "no heartbeat from client, close inactivity channel:{}. remain {} channel in group.",
              ctx.channel().remoteAddress(), channelGroup.size() - 1);

          ctx.channel().close();
          channelGroup.remove(ctx.channel());

          List<String> remove = remoteConnectionMap.remove(ctx.channel().remoteAddress());
          if (remove != null) {
            logger.warn("ChannelHandle unregistered. remain remote connection map is: {}",
                remoteConnectionMap);
          }
        }
      }
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

  public void setRemoteConnectionMap(Map<SocketAddress, List<String>> remoteConnectionMap) {
    this.remoteConnectionMap = remoteConnectionMap;
  }
}
