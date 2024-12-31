
package py.monitorcommon.dao;

import java.util.Set;

public interface EventLogInfoDao {

  void deleteById(long id);

  void deleteByIds(Set<Long> ids);
}
