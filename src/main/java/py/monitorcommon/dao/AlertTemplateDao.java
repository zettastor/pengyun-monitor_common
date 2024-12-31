

package py.monitorcommon.dao;

import java.util.List;
import py.monitor.common.AlertTemplate;

public interface AlertTemplateDao {

  void clearDb();

  void saveOrUpdateAlertTemplate(AlertTemplate alertTemplate);

  //in dos
  void saveOrUpdateAlertTemplateWithAlertRule(AlertTemplate alertTemplate);

  void deleteAlertTemplateById(String id);

  AlertTemplate getAlertTemplateById(String id);

  List<AlertTemplate> getAllAlertTemplate();
}
