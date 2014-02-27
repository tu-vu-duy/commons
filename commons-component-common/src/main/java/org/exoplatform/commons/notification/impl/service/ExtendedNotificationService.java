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
package org.exoplatform.commons.notification.impl.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.commons.api.notification.node.TreeNode;
import org.exoplatform.commons.api.notification.service.QueueMessage;
import org.exoplatform.commons.api.notification.service.listener.NTFEvent;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.api.notification.service.storage.NotificationDataStorage;
import org.exoplatform.commons.api.notification.service.template.DigestorService;
import org.exoplatform.commons.notification.NotificationConfiguration;
import org.exoplatform.commons.notification.NotificationContextFactory;
import org.exoplatform.commons.notification.impl.service.storage.ExtendedDataStorageImpl;
import org.exoplatform.commons.notification.impl.service.storage.NotificationDataStorageImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;

public class ExtendedNotificationService extends NotificationServiceImpl {
  private static final Log         LOG              = ExoLogger.getLogger(ExtendedNotificationService.class);
  public ExtendedNotificationService(NotificationConfiguration configuration,
                                     NotificationDataStorage storage,
                                     PluginSettingService settingService,
                                     UserSettingService userService,
                                     MailService mailService) {
    super(configuration, storage, settingService, userService, mailService);
  }
  
  @Override
  public void processDigest() throws Exception {
    
    
    DigestorService digest = CommonsUtils.getService(DigestorService.class);
    //
    NotificationDataStorageImpl dataStorage = CommonsUtils.getService(NotificationDataStorageImpl.class);
    long startTime = System.currentTimeMillis();
    try {
      dataStorage.resetParentNodeMap();
      List<NTFInforkey> inforKeys = CommonsUtils.getService(ExtendedDataStorageImpl.class).getNotificationInfos();
      Map<String, NTFInforkey> inforKeyMap = new HashMap<String, NTFInforkey>(); 
      for (NTFInforkey ntfInforkey : inforKeys) {
        inforKeyMap.put(ntfInforkey.getPluginId(), ntfInforkey);
      }
      
      // process for users used setting
      int limit = 100;
      int offset = 0;
      while (true) {
        List<UserSetting> userSettings = userService.getDaily(offset, limit);
        if (userSettings.size() == 0) {
          break;
        }
        send(inforKeyMap, digest, userSettings, null);
        offset += limit;
      }
      LOG.debug("Time to run process users have settings: " + (System.currentTimeMillis() - startTime) + "ms.");

      // process for users used default setting
      UserSetting defaultSetting = getDefaultUserSetting(settingService.getActivePluginIds());
      startTime = System.currentTimeMillis();
      offset = 0;
      while (true) {
        List<UserSetting> usersDefaultSettings = userService.getDefaultDaily(offset, limit);
        if (usersDefaultSettings.size() == 0) {
          break;
        }
        send(inforKeyMap, digest, usersDefaultSettings, defaultSetting);
        offset += limit;
      }
    } finally {
      //
      dataStorage.resetParentNodeMap();
      processEvents();
    }

    LOG.debug("Time to run process users used default settings: " + (System.currentTimeMillis() - startTime) + "ms.");
  }
  
  private void send(Map<String, NTFInforkey> inforKeyMap, DigestorService digest,
                      List<UserSetting> userSettings, UserSetting defaultSetting) throws Exception {
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();

    for (UserSetting userSetting : userSettings) {
      if (defaultSetting != null) {
        userSetting = defaultSetting.clone()
                                    .setUserId(userSetting.getUserId())
                                    .setLastUpdateTime(userSetting.getLastUpdateTime());
      }
      boolean isWeekly = configuration.isSendWeekly();
      TreeNode treeNode = new TreeNode(userSetting.getUserId());
      List<String> pluginIds = (isWeekly) ? userSetting.getWeeklyProviders() : userSetting.getDailyProviders();
      treeNode.intPluginNodes(pluginIds);
      //
      for (String pluginId : pluginIds) {
        NTFInforkey infoUUID = inforKeyMap.get(pluginId);
        if(infoUUID != null) {
          if(isAdd(infoUUID, isWeekly, userSetting.getUserId())) {
            treeNode.add(new NotificationKey(pluginId), infoUUID);
            //
            addEvent(NTFEvent.createNTFEvent(treeNode.getUserName(), new NTFInforkey(infoUUID.getUUID()), isWeekly));
          }
        }
      }
      
      if (treeNode.getSize() > 0) {
        MessageInfo messageInfo = digest.buildMessage(treeNode, userSetting);
        if (messageInfo != null) {
          //
          CommonsUtils.getService(QueueMessage.class).put(messageInfo);
          
          if (stats) {
            NotificationContextFactory.getInstance().getStatisticsCollector().createMessageInfoCount(messageInfo.getPluginId());
            NotificationContextFactory.getInstance().getStatisticsCollector().putQueue(messageInfo.getPluginId());
          }
        }
      }
    }
  }
  
  private boolean isAdd(NTFInforkey infoUUID, boolean isWeekly, String userName) throws Exception {
    NotificationInfo info = storage.get(infoUUID.getUUID());
    List<String> users = Arrays.asList((isWeekly) ? info.getSendToWeekly() : info.getSendToDaily());
    if (users.contains(userName) || users.contains(NotificationInfo.FOR_ALL_USER)) {
      return true;
    }
    return false;
  }

}
