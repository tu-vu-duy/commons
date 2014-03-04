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

import org.exoplatform.commons.api.notification.model.NotificationKey;

public class PluginNode {

  private boolean isWeekly = false;
  private NotificationKey   key;
  private List<NTFInforkey> inforkeys;

  public PluginNode(NotificationKey key) {
    this.key = key;
    this.inforkeys = new ArrayList<NTFInforkey>();
  }

  public NotificationKey getKey() {
    return key;
  }
  
  public boolean isWeekly() {
    return isWeekly;
  }

  public void setWeekly(boolean isWeekly) {
    this.isWeekly = isWeekly;
  }

  public void add(NTFInforkey infoUUIDs) {
    this.inforkeys.add(infoUUIDs);
  }

  public boolean remove(NTFInforkey infoUUIDs) {
    return this.inforkeys.remove(infoUUIDs);
  }

  public List<NTFInforkey> getNotificationInfos() {
    return this.inforkeys;
  }
}