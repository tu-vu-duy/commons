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

public class UserNode {

  private String           userName;

  private List<PluginNode> pluginNodes;

  public UserNode(String userName) {
    this.userName = userName;
    pluginNodes = new ArrayList<PluginNode>();
  }

  public String getUserName() {
    return userName;
  }

  public void addPluginNode(PluginNode plugin) {
    pluginNodes.add(plugin);
  }
  
  public PluginNode findPluginNode(String pluginId) {
    for (PluginNode node : pluginNodes) {
      if (node.getKey().equals(new NotificationKey(pluginId))) {
        return node;
      }
    }

    return null;
  }

}
