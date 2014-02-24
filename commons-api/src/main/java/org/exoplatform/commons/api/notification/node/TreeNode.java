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

import java.util.List;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;

public class TreeNode {
  public enum TREE_TYPE {DAILY, WEEKLY};

  private UserNode node;
  
  private TreeNode rootNode;
  
  public TreeNode(String userName) {
    rootNode = this;
    node = new UserNode(userName);
  }
  
  public void buildPluginNodes(List<String> pluginIds) {
    for (String string : pluginIds) {
      rootNode.addPluginNode(new PluginNode(new NotificationKey(string)));
    }
  }
  
  public void addPluginNode(PluginNode plugin) {
    rootNode.getUserNode().addPlugin(plugin);
  }
  
  
  public void addNotification(NotificationKey key, NotificationInfo info) {
    
  }
  
  private UserNode getUserNode() {
    return node;
  }
  
}
