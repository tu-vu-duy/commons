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
package org.exoplatform.commons.notification;

import java.io.Writer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.plugin.config.PluginConfig;
import org.exoplatform.commons.api.notification.plugin.config.TemplateConfig;
import org.exoplatform.commons.api.notification.service.storage.NotificationService;
import org.exoplatform.commons.notification.impl.setting.NotificationPluginContainer;
import org.exoplatform.commons.notification.impl.setting.PluginSettingServiceImpl;
import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.configuration.ConfigurationManagerImpl;
import org.exoplatform.container.util.ContainerUtil;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.test.MockConfigurationManagerImpl;

public abstract class AsbtractBaseNotificationTestCase extends BaseCommonsTestCase {

  protected void registerConfiguration(String configurationPath) {
    try {
      if (getService(NotificationService.class) == null) {
        ConfigurationManagerImpl manager = new MockConfigurationManagerImpl(container.getPortalContext());
        manager.addConfiguration(ContainerUtil.getConfigurationURL(configurationPath));
        //
        ContainerUtil.addContainerLifecyclePlugin(container, manager);
        ContainerUtil.addComponentLifecyclePlugin(container, manager);
        ContainerUtil.addComponents(container, manager);
        container.start(false);
        //
        getService(NotificationConfiguration.class).setWorkspace("portal-test");
        //
        addTestPlugin();
      }
    } catch (Exception e) {e.printStackTrace();}
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    //
    registerConfiguration("conf/standalone/test-notification-configuration.xml");
    //
    addInfoForUser();
  }
  
  protected void addLastUpdateTime(String userId) throws Exception {
    Node rootNode = session.getRootNode().getNode("settings").getNode("user").getNode(userId);
    rootNode.addMixin("exo:datetime");
    rootNode.setProperty("exo:lastModifiedDate", Calendar.getInstance());
    session.save();
  }
  
  private void addTestPlugin() {
    InitParams initParams = new InitParams();
    AbstractNotificationPlugin plugin = new AbstractNotificationPlugin(initParams) {
      
      @Override
      public List<PluginConfig> getPluginConfigs() {
        PluginConfig pluginConfig = new PluginConfig();
        pluginConfig.setPluginId(getId());
        pluginConfig.setTemplateConfig(new TemplateConfig(getId()));
        return Arrays.asList(pluginConfig);
      }
      
      @Override
      protected NotificationInfo makeNotification(NotificationContext ctx) {
        return new NotificationInfo().key(getKey());
      }

      @Override
      protected MessageInfo makeMessage(NotificationContext ctx) {
        return new MessageInfo().body("the body").pluginId(getId()).subject("subject").to("demo@plf.com").from("root").end();
      }

      @Override
      protected boolean makeDigest(NotificationContext ctx, Writer writer) {
        try {
          writer.write("The content of test plugin");
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }

      @Override
      public boolean isValid(NotificationContext ctx) {
        return true;
      }

      @Override
      public String getId() {
        return "TestPlugin";
      }
    };
    //
    CommonsUtils.getService(NotificationPluginContainer.class).addPlugin(plugin);
    PluginSettingServiceImpl pluginService = CommonsUtils.getService(PluginSettingServiceImpl.class);
    pluginService.registerPluginConfig(plugin.getPluginConfigs().get(0));
    pluginService.savePlugin(plugin.getId(), true);
    PluginConfig pluginConfig = new PluginConfig();
    pluginConfig.setPluginId("DigestDailyPlugin");
    pluginConfig.setTemplateConfig(new TemplateConfig("DigestDailyPlugin"));
    pluginService.registerPluginConfig(pluginConfig);
  }
  
  protected void addInfoForUser() throws Exception {
    OrganizationService organizationService = CommonsUtils.getService(OrganizationService.class);
    ListAccess<User> list = organizationService.getUserHandler().findAllUsers();
    //
    User[] users = list.load(0, list.getSize());
    for (int i = 0; i < users.length; i++) {
      if (users[i] != null && users[i].getUserName() != null) {
        users[i].setCreatedDate(Calendar.getInstance().getTime());
        users[i].setEmail(users[i].getUserName() + "@plf.com");
      }
    }
  }
}
