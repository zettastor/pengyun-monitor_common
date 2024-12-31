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

package py.monitorcommon.dao;

import java.util.List;
import py.monitor.common.DtoUser;

public interface DtoUserDao {

  void saveUser(DtoUser user);

  void updateUser(DtoUser user);

  List<DtoUser> listUsers();

  List<DtoUser> getUserByNameAndJobNum(String userName, String jobNum);

  DtoUser getUserById(long id);

  void deleteUsersByIds(List<Long> ids);

  void updateUserFlagByIds(boolean flag, List<Long> ids);
}
