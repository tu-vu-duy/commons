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

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.commons.api.notification.node.PluginNode;
import org.exoplatform.commons.api.notification.node.TreeNode;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.notification.service.QueueMessage;
import org.exoplatform.commons.api.notification.service.listener.NTFEvent;
import org.exoplatform.commons.api.notification.service.setting.PluginContainer;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.api.notification.service.storage.NotificationDataStorage;
import org.exoplatform.commons.api.notification.service.template.DigestorService;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.NotificationConfiguration;
import org.exoplatform.commons.notification.NotificationContextFactory;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.impl.service.storage.ExtendedDataStorageImpl;
import org.exoplatform.commons.notification.impl.service.storage.NotificationDataStorageImpl;
import org.exoplatform.commons.notification.impl.service.template.DigestInfo;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;

public class ExtendedNotificationService extends NotificationServiceImpl {
  private static final Log         LOG              = ExoLogger.getLogger(ExtendedNotificationService.class);
  private static PluginContainer containerService_;
  public ExtendedNotificationService(NotificationConfiguration configuration,
                                     PluginContainer containerService,
                                     NotificationDataStorage storage,
                                     PluginSettingService settingService,
                                     UserSettingService userService,
                                     MailService mailService) {
    super(configuration, storage, settingService, userService, mailService);
    containerService_ = containerService;
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
      if(inforKeys.size() == 0) {
        return;
      }

      Map<String, List<NTFInforkey>> inforKeyMap = new HashMap<String, List<NTFInforkey>>(); 
      List<NTFInforkey> infoByPlugin;
      for (NTFInforkey ntfInforkey : inforKeys) {
        if ((infoByPlugin = inforKeyMap.get(ntfInforkey.getPluginId())) == null) {
          infoByPlugin = new ArrayList<NTFInforkey>();
          inforKeyMap.put(ntfInforkey.getPluginId(), infoByPlugin);
        }
        infoByPlugin.add(ntfInforkey);
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
      LOG.debug("Time to run process users used default settings: " + (System.currentTimeMillis() - startTime) + "ms.");
    } finally {
      //
      dataStorage.resetParentNodeMap();
      processEvents();
    }

  }
  
  private void send(Map<String, List<NTFInforkey>> inforKeyMap, DigestorService digest,
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
        List<NTFInforkey> ntfInforkeys = inforKeyMap.get(pluginId);
        if (ntfInforkeys != null) {
          for (NTFInforkey ntfInforkey : ntfInforkeys) {
            if (isAdd(ntfInforkey, isWeekly, userSetting.getUserId())) {
              treeNode.add(new NotificationKey(pluginId), ntfInforkey);
              //
              addEvent(NTFEvent.createNTFEvent(treeNode.getUserName(), new NTFInforkey(ntfInforkey.getUUID()), isWeekly));
            }
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
  
  private boolean isAdd(NTFInforkey inforkey, boolean isWeekly, String userName) throws Exception {
    NotificationInfo info = storage.get(inforkey.getUUID());
    List<String> users = Arrays.asList((isWeekly) ? info.getSendToWeekly() : info.getSendToDaily());
    if (users.contains(userName) || users.contains(NotificationInfo.FOR_ALL_USER)) {
      return true;
    }
    return false;
  }
  

/**
* New APIs 
**/
  
  /**
   * @throws Exception
   */
  public void processDigests() throws Exception {
    Map<String, ResultDigests> results = new HashMap<String, ResultDigests>();
    ExtendedDataStorageImpl extDataStorage = CommonsUtils.getService(ExtendedDataStorageImpl.class);
    UserSetting defaultSetting = getDefaultUserSetting(settingService.getActivePluginIds());
    for (String pluginId : settingService.getActivePluginIds()) {
      try {
        PluginNode pluginNode = extDataStorage.getPluginNotificationInfos(pluginId);
        // process for users used setting
        long startTime = System.currentTimeMillis();
        processListUser(results, pluginNode, null);
        LOG.debug("Time to run process users have settings: " + (System.currentTimeMillis() - startTime) + "ms.");

        // process for users used default setting
        startTime = System.currentTimeMillis();
        processListUser(results, pluginNode, defaultSetting);
        LOG.debug("Time to run process users used default settings: " + (System.currentTimeMillis() - startTime) + "ms.");

      } catch (Exception e) {
        LOG.warn("Failed to process notification of plugin " + pluginId);
        LOG.debug(e.getMessage(), e);
      } finally {
        processEvents();
      }
    }
    //
    for (String userId : results.keySet()) {
      MessageInfo messageInfo = buildMessageInfo(results.get(userId));
      if (messageInfo != null) {
        CommonsUtils.getService(QueueMessage.class).put(messageInfo);
      }
    }
    
  }
  
  private MessageInfo buildMessageInfo(ResultDigests digests) throws Exception {
    Writer writer = new StringWriter();
    if (digests.getDigestSize() == 0) {
      return null;
    } else if (digests.getDigestSize() == 1) {
      writer.append("<ul style=\"margin: 0 0  40px -13px; list-style-type: none; padding-left: 0; color: #2F5E92; \">");
    } else {
      writer.append("<ul style=\"margin: 0 0  40px; padding-left: 0; color: #2F5E92; list-style-position: outside;  list-style: disc; \">");
    }
    //
    writer.append(digests.toString());
    
    writer.append("</ul>");

    UserSetting userSetting = userService.get(digests.getUserId());
    DigestInfo digestInfo = new DigestInfo(configuration, userSetting);

    TemplateContext ctx = new TemplateContext(digestInfo.getPluginId(), digestInfo.getLocale().getLanguage());

    ctx.put("FIRSTNAME", digestInfo.getFirstName());
    ctx.put("PORTAL_NAME", digestInfo.getPortalName());
    ctx.put("PORTAL_HOME", digestInfo.getPortalHome());
    ctx.put("PERIOD", digestInfo.getPeriodType());
    ctx.put("FROM_TO", digestInfo.getFromTo());
    String subject = TemplateUtils.processSubject(ctx);
    
    ctx.put("FOOTER_LINK", digestInfo.getFooterLink());
    ctx.put("DIGEST_MESSAGES_LIST", writer.toString());

    String body = TemplateUtils.processGroovy(ctx);
    //
    MessageInfo messageInfo = new MessageInfo();
    return messageInfo.from(NotificationPluginUtils.getFrom(null)).subject(subject)
               .body(body).to(digestInfo.getSendTo()).end();
  }
  
  private void processListUser(Map<String, ResultDigests> results, PluginNode pluginNode, UserSetting defaultSetting) throws Exception {
    int limit = 100;
    int offset = 0;
    List<UserSetting> userSettings;
    boolean isDefaultSetting = (defaultSetting != null);
    while (true) {
      if(isDefaultSetting) {
        userSettings = userService.getDefaultDaily(offset, limit);
      } else {
        userSettings = userService.getDaily(offset, limit);
      }
      if (userSettings.size() == 0) {
        break;
      }
      //
      for (UserSetting userSetting : userSettings) {
        if (isDefaultSetting) {
          userSetting = defaultSetting.clone()
                                      .setUserId(userSetting.getUserId())
                                      .setLastUpdateTime(userSetting.getLastUpdateTime());
        }
        ResultDigests resultDigests = results.get(userSetting.getUserId());
        if (resultDigests == null) {
          resultDigests = new ResultDigests(userSetting.getUserId());
          results.put(userSetting.getUserId(), resultDigests);
        }
        processDigestsByUser(pluginNode, userSetting, resultDigests);
      }
      offset += limit;
    }
  }
  
  private void processDigestsByUser(PluginNode pluginNode, UserSetting userSetting, ResultDigests resultDigests) throws Exception {
    List<NTFInforkey> inforkeys = getByUser(pluginNode, userSetting);
    if (inforkeys.size() == 0) {
      return;
    }
    NotificationContext nCtx = NotificationContextImpl.cloneInstance();
    nCtx.append(NotificationPluginUtils.SENDTO, userSetting.getUserId());

    AbstractNotificationPlugin plugin = containerService_.getPlugin(pluginNode.getKey());
    nCtx.setNotificationInfos(inforkeys);
    plugin.buildDigest(nCtx, resultDigests.getWriter());
    //
    processRemoveItem(pluginNode);
  }
  
  private boolean isValid(PluginNode pluginNode, UserSetting userSetting) {
    List<String> activePlugins = (configuration.isSendWeekly()) ? userSetting.getWeeklyProviders() : 
                                                                  userSetting.getDailyProviders();
    return activePlugins.contains(pluginNode.getKey().getId());
  }
  
  public List<NTFInforkey> getByUser(PluginNode pluginNode, UserSetting userSetting) throws Exception {
    List<NTFInforkey> inforkeys = new ArrayList<NTFInforkey>();
    if (isValid(pluginNode, userSetting)) {
      List<NTFInforkey> messages = pluginNode.getNotificationInfos();
      for (NTFInforkey inforkey : messages) {
        if (isAdd(inforkey, pluginNode.isWeekly(), userSetting.getUserId())) {
          inforkeys.add(inforkey);
        }
      }
    }
    //
    return inforkeys;
  }
  
  
  
  private void processRemoveItem(PluginNode pluginNode) throws Exception {
    List<NTFInforkey> messages = pluginNode.getNotificationInfos();
    boolean isWeekly = configuration.isSendWeekly();
    for (NTFInforkey ntfInforkey : messages) {
      NotificationInfo info = storage.get(ntfInforkey.getUUID());
      String[] sendTos = (isWeekly) ? info.getSendToWeekly() : info.getSendToDaily();
      if (sendTos.length == 1 && !sendTos[0].equals(NotificationInfo.FOR_ALL_USER)) {
        pluginNode.remove(ntfInforkey);
      }
    }
  }
  
  
  public class ResultDigests {
    private Writer       writer;
    private int size = 0;
    private final String userId;

    public ResultDigests(String userId) {
      writer = new StringWriter();
      this.userId = userId;
    }

    public Writer getWriter() {
      ++size;
      return writer;
    }

    public String getUserId() {
      return userId;
    }

    public int getDigestSize() {
      return size;
    }

    @Override
    public boolean equals(Object o) {
      if (super.equals(o)) {
        return true;
      }

      if (userId.equals(((ResultDigests) o).getUserId())) {
        return true;
      }
      return false;
    }
    @Override
    public int hashCode() {
      int c = super.hashCode();
      return c + 21 * userId.hashCode();
    }
    
    @Override
    public String toString() {
      return writer.toString();
    }
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

}
