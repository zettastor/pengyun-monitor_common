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
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class is used to distinct between http and web socket.
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);
  private final String wsUri;

  public HttpRequestHandler(String wsUri) {
    super();
    this.wsUri = wsUri;
  }

  @Override
  public void channelRead0(ChannelHandlerContext context, FullHttpRequest request) {
    if (request.getUri().contains(WebSocketServer.suffix)) {
      /*
       * immediately invoke the next handle's channelRead0 or channelRead.
       */
      context.fireChannelRead(request.retain());
    } else {
      /*
       * http process
       */
      logger.warn("this is http request");
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable t) {
    logger.warn("caught exception,", t);
    context.close();
  }
}
