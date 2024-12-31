

package py.monitorcommon.dao;

import py.monitor.common.SmtpItem;

public interface SmtpItemDao {

  void clearDb();

  void saveOrUpdateSmtpItem(SmtpItem smtpItem);

  SmtpItem getSmtpItem();
}
