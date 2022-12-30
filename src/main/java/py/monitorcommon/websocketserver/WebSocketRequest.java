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

import py.monitor.common.AlertMessage;
import py.monitor.common.PerformanceMessage;

public class WebSocketRequest {

  private AlertMessage alertMessage = null;
  private PerformanceMessage performanceMessage = null;

  public AlertMessage getAlertMessage() {
    return alertMessage;
  }

  public void setAlertMessage(AlertMessage alertMessage) {
    this.alertMessage = alertMessage;
  }

  public PerformanceMessage getPerformanceMessage() {
    return performanceMessage;
  }

  public void setPerformanceMessage(PerformanceMessage performanceMessage) {
    this.performanceMessage = performanceMessage;
  }

  @Override
  public String toString() {
    return "WebSocketRequest{" + "alertMessage=" + alertMessage + ", performanceMessage="
        + performanceMessage
        + '}';
  }
}
