

package py.monitorcommon.dao;

import java.util.List;
import py.monitor.common.PerformanceSearchTemplate;

public interface PerformanceSearchTemplateDao {

  void clearDb();

  void saveOrUpdate(PerformanceSearchTemplate performanceSearchTemplate);

  void deleteById(long id);

  PerformanceSearchTemplate getPerformanceSearchTemplateByName(String name);

  PerformanceSearchTemplate getPerformanceSearchTemplateById(long id);

  List<PerformanceSearchTemplate> listPerformanceSearchTemplate();

}
