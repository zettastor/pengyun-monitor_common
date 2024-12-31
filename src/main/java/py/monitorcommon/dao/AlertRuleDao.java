

package py.monitorcommon.dao;

import java.util.List;
import java.util.Set;
import py.monitor.common.AlertRule;

public interface AlertRuleDao {

  void clearDb();

  void saveOrUpdateAlertRule(AlertRule alertRule);

  //in dos
  void saveOrUpdateAlertRuleWithTemplate(AlertRule alertRule);

  void deleteAlertRuleById(String id);

  void deleteAlertRuleByIds(Set<String> ids);

  AlertRule getAlertRuleById(String id);

  List<AlertRule> getAlertRuleByName(String name);

  List<AlertRule> getAllAlertRule();

}
