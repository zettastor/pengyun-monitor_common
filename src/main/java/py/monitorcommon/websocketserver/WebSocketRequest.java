
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
