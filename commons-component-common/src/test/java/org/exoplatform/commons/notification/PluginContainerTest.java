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
package org.exoplatform.commons.notification;

import java.util.List;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.service.setting.PluginContainer;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.plugin.PluginTest;
import org.exoplatform.commons.notification.template.TemplateUtils;

public class PluginContainerTest extends AsbtractBaseNotificationTestCase {
  
  private PluginContainer container;
  public PluginContainerTest() {
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    container = getService(PluginContainer.class);
    assertNotNull(container);
    
  }
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  public void testPlugin() {
    // check existing plugin
    NotificationKey pluginKey = new NotificationKey(PluginTest.ID);
    AbstractNotificationPlugin plugin = container.getPlugin(pluginKey);
    assertNotNull(plugin);
    // get child
    List<NotificationKey> chikdKeys = container.getChildPluginKeys(pluginKey);
    assertEquals(1, chikdKeys.size());
    assertEquals("Child_Plugin", chikdKeys.get(0).getId());
    
    //
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    NotificationInfo notificationInfo = plugin.buildNotification(ctx);
    assertNotNull(notificationInfo);
    assertEquals("demo", notificationInfo.getSendToUserIds().get(0));
    assertEquals(PluginTest.ID, notificationInfo.getKey().getId());
    //
    ctx.setNotificationInfo(notificationInfo);
    MessageInfo messageInfo = plugin.buildMessage(ctx);

    // check subject
    assertEquals("The subject Test plugin notification", messageInfo.getSubject());
    // check content
    assertTrue(messageInfo.getBody().indexOf("root") > 0);
    assertTrue(messageInfo.getBody().indexOf("Test value") > 0);

    // check process resource-bundle on plugin
    assertTrue(messageInfo.getBody().indexOf("The test plugin") > 0);
    // check child plugin content
    assertTrue(messageInfo.getBody().indexOf("The content of child plugin") > 0);
    // check process resource-bundle on plugin
    assertTrue(messageInfo.getBody().indexOf("The test child plugin") > 0);
  }

  public void testRenderPlugin() throws Exception {
    TemplateContext ctx = new TemplateContext("DigestDailyPlugin", "en");
    ctx.put("FIRSTNAME", "Root");
    ctx.put("PORTAL_NAME", "portal");
    ctx.put("PORTAL_HOME", "portalHome");
    ctx.put("PERIOD", "Daily");
    ctx.put("FROM_TO", "yesterday - today");
    String subject = TemplateUtils.processSubject(ctx);
    
    ctx.put("FOOTER_LINK", "http://plf.com");
    ctx.put("DIGEST_MESSAGES_LIST", "The digest content");
    ctx.put("HAS_ONE_MESSAGE", true);
    String s = TemplateUtils.processGroovy(ctx);
    // check process resource-bundle
    assertTrue(s.indexOf("Test resource bundle") > 0);
    // check process Groovy
    assertTrue(s.indexOf("The digest content") > 0);
    // check subject
    assertTrue(subject.indexOf("Root") > 0);
  }
}