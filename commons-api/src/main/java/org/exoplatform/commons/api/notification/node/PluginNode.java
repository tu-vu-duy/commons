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
package org.exoplatform.commons.api.notification.node;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;

public class PluginNode {

  private NotificationKey        key;
  private List<NotificationInfo> notificationInfos;

  public PluginNode(NotificationKey key) {
    this.key = key;
    this.notificationInfos = new ArrayList<NotificationInfo>();
  }

  public NotificationKey getKey() {
    return key;
  }

  public void add(NotificationInfo info) {
    this.notificationInfos.add(info);
  }

  public List<NotificationInfo> getNotificationInfos() {
    return this.notificationInfos;
  }

}
