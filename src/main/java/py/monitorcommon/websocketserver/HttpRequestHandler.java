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
