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

package py.monitorcommon.struct;

import java.util.Objects;
import py.monitor.common.AlertMessage;

public class MemoryAlertMessage {

  private String id;
  private String sourceId;
  private String counterKey;

  public MemoryAlertMessage(AlertMessage alertMessage) {
    this.id = alertMessage.getId();
    this.sourceId = alertMessage.getSourceId();
    this.counterKey = alertMessage.getCounterKey();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public String getCounterKey() {
    return counterKey;
  }

  public void setCounterKey(String counterKey) {
    this.counterKey = counterKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MemoryAlertMessage)) {
      return false;
    }
    MemoryAlertMessage that = (MemoryAlertMessage) o;
    return Objects.equals(getId(), that.getId())
        && Objects.equals(getSourceId(), that.getSourceId())
        && Objects.equals(getCounterKey(), that.getCounterKey());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getSourceId(), getCounterKey());
  }

  @Override
  public String toString() {
    return "MemoryAlertMessage{"
        + "id='" + id + '\''
        + ", sourceId='" + sourceId + '\''
        + ", counterKey='" + counterKey + '\''
        + '}';
  }
}
