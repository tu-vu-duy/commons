/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.commons.notification.impl.service.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.utils.CommonsUtils;

public class ListUserSetting implements Serializable {
  private static final long serialVersionUID = 1L;
  private List<String>      data = new ArrayList<String>();
  private List<UserSetting> storage = null;

  public ListUserSetting() {
  }

  public ListUserSetting(List<UserSetting> input, boolean isDefault) {
    if (!isDefault) {
      for (UserSetting setting : input) {
        data.add(setting.getUserId());
      }
    } else {
      storage = input;
    }
  }

  public List<UserSetting> build() {
    if (storage == null) {
      UserSettingService service = CommonsUtils.getService(UserSettingService.class);
      List<UserSetting> out = new ArrayList<UserSetting>();
      for (String userId : data) {
        out.add(service.get(userId));
      }
      return out;
    }
    return storage;
  }

  public void setData(List<String> data) {
    this.data = data;
  }

  public List<String> getData() {
    return this.data;
  }
}
