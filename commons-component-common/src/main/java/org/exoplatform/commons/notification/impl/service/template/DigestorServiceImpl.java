/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.commons.notification.impl.service.template;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.commons.api.notification.node.TreeNode;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.template.DigestorService;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.NotificationConfiguration;
import org.exoplatform.commons.notification.NotificationContextFactory;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.impl.setting.NotificationPluginContainer;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class DigestorServiceImpl implements DigestorService {
  
  private static final Log LOG = ExoLogger.getLogger(DigestorServiceImpl.class);


  public DigestorServiceImpl() {
  }
  
  
  public MessageInfo buildMessage(TreeNode treeNode, UserSetting userSetting) {
    MessageInfo messageInfo = null;

    long startTime = System.currentTimeMillis();
    try {
      messageInfo = new MessageInfo();
      PluginSettingService pluginService = CommonsUtils.getService(PluginSettingService.class);
      NotificationPluginContainer containerService = CommonsUtils.getService(NotificationPluginContainer.class);
      NotificationConfiguration configuration = CommonsUtils.getService(NotificationConfiguration.class);
      
      List<String> activePlugins = pluginService.getActivePluginIds();
      NotificationContext nCtx = NotificationContextImpl.cloneInstance();
      nCtx.append(NotificationPluginUtils.SENDTO, userSetting.getUserId());
      
      int totalDigestMsg = treeNode.getSize();
      
      Writer writer = new StringWriter();
      if (totalDigestMsg < 1) {
        return null;
      } else if (totalDigestMsg == 1) {
        writer.append("<ul style=\"margin: 0 0  40px -13px; list-style-type: none; padding-left: 0; color: #2F5E92; \">");
      } else {
        writer.append("<ul style=\"margin: 0 0  40px; padding-left: 0; color: #2F5E92; list-style-position: outside;  list-style: disc; \">");
      }
      for (String pluginId : activePlugins) {
        
        List<NTFInforkey> messages = treeNode.getNFTInforkeys(NotificationKey.key(pluginId));
        if (messages == null || messages.size() == 0){
          continue;
        }
        
        AbstractNotificationPlugin plugin = containerService.getPlugin(NotificationKey.key(pluginId));
        nCtx.setNotificationInfos(messages);
        plugin.buildDigest(nCtx, writer);
      }
      writer.append("</ul>");

      StringBuffer sb = ((StringWriter) writer).getBuffer();
      if (sb.length() == 0) {
        return null;
      }

      DigestInfo digestInfo = new DigestInfo(configuration, userSetting);

      TemplateContext ctx = new TemplateContext(digestInfo.getPluginId(), digestInfo.getLocale().getLanguage());

      ctx.put("FIRSTNAME", digestInfo.getFirstName());
      ctx.put("PORTAL_NAME", digestInfo.getPortalName());
      ctx.put("PORTAL_HOME", digestInfo.getPortalHome());
      ctx.put("PERIOD", digestInfo.getPeriodType());
      ctx.put("FROM_TO", digestInfo.getFromTo());
      String subject = TemplateUtils.processSubject(ctx);
      
      ctx.put("FOOTER_LINK", digestInfo.getFooterLink());
      ctx.put("DIGEST_MESSAGES_LIST", sb.toString());

      String body = TemplateUtils.processGroovy(ctx);

      messageInfo.from(NotificationPluginUtils.getFrom(null)).subject(subject)
                 .body(body).to(digestInfo.getSendTo());
    } catch (Exception e) {
      LOG.error("Can not build template of DigestorProviderImpl ", e);
      return null;
    }
    
    LOG.info(String.format("End build template email for user %s of DigestorProviderImpl ... " + (System.currentTimeMillis() - startTime) + " ms", userSetting.getUserId()));
    
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    if (stats) {
      NotificationContextFactory.getInstance().getStatisticsCollector().createDigestCount(messageInfo.getPluginId());
    }
    
    return messageInfo;
  }
  
}
