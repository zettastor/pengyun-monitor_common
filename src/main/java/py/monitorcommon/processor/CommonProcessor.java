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
