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

package py.monitorcommon.processor;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.monitor.common.AlertTemplate;
import py.monitor.common.EventDataInfo;
import py.monitor.common.QueueManager;

public class CommonProcessor extends BaseProcessor {

  private static final Logger logger = LoggerFactory.getLogger(CommonProcessor.class);

  public CommonProcessor(Map<String, AlertTemplate> alertTemplateMap, QueueManager queueManager,
      int webSocketServerPort) {
    super(alertTemplateMap, queueManager, webSocketServerPort);
  }

  @Override
  public void saveToAlertQueue(EventDataInfo eventDataInfo, String counterKey) {
    logger.warn("commonProcessor not generating AlertMessage!");
  }
}
