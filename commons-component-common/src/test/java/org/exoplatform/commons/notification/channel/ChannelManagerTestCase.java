package org.exoplatform.commons.notification.channel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.api.notification.channel.AbstractChannelTemplateHandler;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.service.setting.ChannelManager;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.notification.BaseNotificationTestCase;
import org.exoplatform.commons.notification.channel.mock.MockChannalTemplateHandler;
import org.exoplatform.commons.notification.channel.mock.MockTestChannel;
import org.exoplatform.commons.notification.channel.mock.MockTestTemplateHandler;
import org.exoplatform.commons.notification.impl.DigestDailyPlugin;
import org.exoplatform.commons.notification.impl.DigestWeeklyPlugin;
import org.exoplatform.commons.notification.mock.MockEventListener;
import org.exoplatform.commons.notification.plugin.PluginTest;

public class ChannelManagerTestCase extends BaseNotificationTestCase {
  private ChannelManager channelManager;
  private UserSettingService settingService;
  private PluginSettingService adminSettingService;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    channelManager = getService(ChannelManager.class);
    settingService = getService(UserSettingService.class);
    adminSettingService = getService(PluginSettingService.class);
    assertNotNull(channelManager);
    //
    adminSettingService.saveActivePlugin(MockTestTemplateHandler.CHANNEL_TEST, PluginTest.ID, true);
  }
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  public void testChannelRegister() throws Exception {
    assertEquals(3, channelManager.getChannelIds().size());
    assertTrue(channelManager.getChannelIds().contains("email"));
    assertTrue(channelManager.getChannelIds().contains("intranet"));
  }
  
  public void testGetChannel() throws Exception {
    AbstractChannelTemplateHandler handler = channelManager.get("email").getTemplateHandlers().get(0);
    assertTrue(handler instanceof MockChannalTemplateHandler); 
    List<String> plugins = handler.getChannelTemplateEngine().getPluginIds();
    assertCollection(plugins, Arrays.asList(DigestDailyPlugin.ID, DigestWeeklyPlugin.ID, PluginTest.ID), Collections.reverseOrder());
  }
  
  public void testFullProcess() throws Exception {
    UserSetting setting = settingService.get("demo");
    setting.setChannelActive(MockTestTemplateHandler.CHANNEL_TEST);
    setting.addChannelPlugin(MockTestTemplateHandler.CHANNEL_TEST, PluginTest.ID);
    settingService.save(setting);
    //
    new MockEventListener().eventActive();
    //
    List<MessageInfo> keepDatas = ((MockTestChannel) channelManager.get(MockTestTemplateHandler.CHANNEL_TEST)).getKeepDatas();
    assertEquals(1, keepDatas.size());
    assertTrue(keepDatas.get(0).getSubject().contains("test channel"));
    assertTrue(keepDatas.get(0).getTo().equals("demo"));
    //
    for (int i = 0; i < 9; i++) {
      new MockEventListener().eventActive();
    }
    //
    keepDatas = ((MockTestChannel) channelManager.get(MockTestTemplateHandler.CHANNEL_TEST)).getKeepDatas();
    assertEquals(10, keepDatas.size());
  }
  
}
