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
package org.exoplatform.commons.notification.impl.service.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.storage.NotificationDataStorage;
import org.exoplatform.commons.notification.NotificationConfiguration;
import org.exoplatform.commons.notification.impl.AbstractService;
import org.exoplatform.commons.notification.impl.NotificationSessionManager;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

public class ExtendedDataStorageImpl extends AbstractService {
  private String                    workspace;
  private NotificationConfiguration configuration;
  private PluginSettingService pluginService;
  private NotificationDataStorage dataStorage;
  private ThreadLocal<Map<String, Node>> parentNodeMap = new ThreadLocal<Map<String,Node>>();
  
  public ExtendedDataStorageImpl(NotificationConfiguration configuration, PluginSettingService pluginService, NotificationDataStorage dataStorage) {
    this.configuration = configuration;
    this.workspace = configuration.getWorkspace();
    this.pluginService = pluginService;
    this.dataStorage = dataStorage;
  }
  
  public void resetParentNodeMap() {
    if (this.parentNodeMap.get() == null) {
      this.parentNodeMap.set(new HashMap<String, Node>());
    } else {
      this.parentNodeMap.get().clear();
    }
  }
  
  private Node getParentNodeByThreadLocal(String pluginId) {
    if (parentNodeMap.get() == null) {
      resetParentNodeMap();
      return null;
    }
    return parentNodeMap.get().get(pluginId);
  }
  
  private Node getDaily(SessionProvider sProvider, String pluginId) throws Exception {
    Node node = getParentNodeByThreadLocal(pluginId);
    if (node == null || node.getSession() == null || !node.getSession().isLive()) {
      // In the case session what hold by Node has been NULL or isLive is FALSE
      // must re-load Node from JCR
      node = getOrCreateMessageParent(sProvider, workspace, pluginId);
      parentNodeMap.get().put(pluginId, node);
    }

    return node;
  }

  private Node getWeekly(SessionProvider sProvider, String pluginId) throws Exception {
    Node node = getParentNodeByThreadLocal(pluginId);
    // In the case session what hold by Node has been NULL or isLive is FALSE
    // must re-load Node from JCR
    if (node == null || node.getSession() == null || !node.getSession().isLive()) {
      node = getMessageNodeByPluginId(sProvider, workspace, pluginId);
      parentNodeMap.get().put(pluginId, node);
    }

    return node;
  }

  public List<NTFInforkey> getNotificationInfos() throws Exception {
    SessionProvider sProvider = NotificationSessionManager.createSystemProvider();
    List<NTFInforkey> inforkeys = new ArrayList<NTFInforkey>();
    boolean isWeekly = configuration.isSendWeekly();
    for (String pluginId : pluginService.getActivePluginIds()) {
      if (isWeekly) {
        addWeeklyNotification(sProvider, inforkeys, pluginId);
      } else {
        addDailyNotification(sProvider, inforkeys, pluginId);
      }
    }
    return inforkeys;
  }

  private void addDailyNotification(SessionProvider sProvider, List<NTFInforkey> inforkeys, String pluginId) throws Exception {
    Node parentNode = getDaily(sProvider, pluginId);
    NodeIterator iter = parentNode.getNodes();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      String daily = node.getProperty(NTF_SEND_TO_DAILY).getString();
      if (daily != null && daily.length() > 0) {
        inforkeys.add(new NTFInforkey(node.getUUID()));
      }
    }
  }

  private void addWeeklyNotification(SessionProvider sProvider, List<NTFInforkey> inforkeys, String pluginId) throws Exception {
    Node parentNode = getWeekly(sProvider, pluginId);
    NodeIterator iter = parentNode.getNodes();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      NodeIterator iter2 = node.getNodes();
      while (iter2.hasNext()) {
        Node infoNode = iter2.nextNode();
        String weekly = infoNode.getProperty(NTF_SEND_TO_WEEKLY).getString();
        if (weekly != null && weekly.length() > 0) {
          inforkeys.add(new NTFInforkey(infoNode.getUUID()).setPluginId(pluginId));
        }
      }
    }
  }
  
  
}
