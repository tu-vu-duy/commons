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
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.commons.api.notification.node.PluginNode;
import org.exoplatform.commons.api.notification.service.NotificationService;
import org.exoplatform.commons.api.notification.service.listener.NTFEvent;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.notification.NotificationConfiguration;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.impl.AbstractService;
import org.exoplatform.commons.notification.impl.NotificationSessionManager;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

public class ExtendedDataStorageImpl extends AbstractService {
  private NotificationConfiguration configuration;
  private PluginSettingService pluginService;
  private NotificationDataStorageImpl dataStorage;
  
  public ExtendedDataStorageImpl(NotificationConfiguration configuration, PluginSettingService pluginService, NotificationDataStorageImpl dataStorage) {
    this.configuration = configuration;
    this.pluginService = pluginService;
    this.dataStorage = dataStorage;
  }
  
  public List<NTFInforkey> getNotificationInfos(String pluginId) throws Exception {
    SessionProvider sProvider = NotificationSessionManager.getOrCreateSessionProvider();
    List<NTFInforkey> inforkeys = new ArrayList<NTFInforkey>();
    boolean isWeekly = configuration.isSendWeekly();
    if (isWeekly) {
      addWeeklyNotification(sProvider, inforkeys, pluginId);
    } else {
      addDailyNotification(sProvider, inforkeys, pluginId);
    }
    return inforkeys;
  }

  public PluginNode getPluginNotificationInfos(String pluginId) throws Exception {
    NotificationService notificationService = CommonsUtils.getService(NotificationService.class);
    List<NTFInforkey> inforkeys = getNotificationInfos(pluginId);
    PluginNode pluginNode = new PluginNode(new NotificationKey(pluginId));
    pluginNode.setWeekly(configuration.isSendWeekly());
    for (NTFInforkey ntfInforkey : inforkeys) {
      pluginNode.add(ntfInforkey);
      //
      notificationService.addEvent(NTFEvent.createNTFEvent(pluginId, ntfInforkey, pluginNode.isWeekly()));
    }
    return pluginNode;
  }
  
  public List<NTFInforkey> getNotificationInfos() throws Exception {
    List<NTFInforkey> inforkeys = new ArrayList<NTFInforkey>();
    for (String pluginId : pluginService.getActivePluginIds()) {
      inforkeys.addAll(getNotificationInfos(pluginId));
    }
    return inforkeys;
  }

  private void addDailyNotification(SessionProvider sProvider, List<NTFInforkey> inforkeys, String pluginId) throws Exception {
    Node parentNode = dataStorage.getDaily(sProvider, pluginId);
    NodeIterator iter = parentNode.getNodes();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      if (NotificationUtils.valuesToList(node.getProperty(NTF_SEND_TO_DAILY).getValues()).size() > 0) {
        inforkeys.add(new NTFInforkey(node.getUUID()).setPluginId(pluginId));
      }
    }
  }

  private void addWeeklyNotification(SessionProvider sProvider, List<NTFInforkey> inforkeys, String pluginId) throws Exception {
    Node parentNode = dataStorage.getWeekly(sProvider, pluginId);
    NodeIterator iter = parentNode.getNodes();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      NodeIterator iter2 = node.getNodes();
      while (iter2.hasNext()) {
        //
        Node infoNode = iter2.nextNode();
        if (NotificationUtils.valuesToList(infoNode.getProperty(NTF_SEND_TO_WEEKLY).getValues()).size() > 0) {
          inforkeys.add(new NTFInforkey(infoNode.getUUID()).setPluginId(pluginId));
        }
      }
    }
  }
}
