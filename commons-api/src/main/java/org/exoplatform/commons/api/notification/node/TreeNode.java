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

public class TreeNode {
  
  private int size = 0;
  private UserNode node;
  private TreeNode rootNode;

  public TreeNode(String userName) {
    rootNode = this;
    node = new UserNode(userName);
  }

  public void intPluginNodes(List<String> pluginIds) {
    for (String string : pluginIds) {
      rootNode.addPluginNode(new PluginNode(new NotificationKey(string)));
    }
  }

  public void add(NotificationKey key, NTFInforkey infoUUID) {
    PluginNode node = rootNode.getUserNode().findPluginNode(key);
    if (node != null) {
      node.add(infoUUID);
      ++size;
    }
  }

  public boolean remove(NotificationKey key, NTFInforkey infoUUID) {
    if (rootNode.getUserNode().findPluginNode(key).remove(infoUUID)) {
      --size;
      return true;
    }
    return false;
  }

  public List<NTFInforkey> getNFTInforkeys(NotificationKey key) {
    PluginNode node = rootNode.getUserNode().findPluginNode(key);
    if (node != null) {
      return node.getNotificationInfos();
    }
    return new ArrayList<NTFInforkey>();
  }

  public String getUserName() {
    return rootNode.getUserNode().getUserName();
  }
  
  public int getSize() {
    return size;
  }

  private UserNode getUserNode() {
    return node;
  }

  private void addPluginNode(PluginNode plugin) {
    rootNode.getUserNode().addPluginNode(plugin);
  }
  
  private class UserNode {

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

    public PluginNode findPluginNode(NotificationKey key) {
      for (PluginNode node : pluginNodes) {
        if (node.getKey().equals(key)) {
          return node;
        }
      }

      return null;
    }

  }

  private class PluginNode {

    private NotificationKey   key;
    private List<NTFInforkey> inforkeys;

    public PluginNode(NotificationKey key) {
      this.key = key;
      this.inforkeys = new ArrayList<NTFInforkey>();
    }

    public NotificationKey getKey() {
      return key;
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
  
}
