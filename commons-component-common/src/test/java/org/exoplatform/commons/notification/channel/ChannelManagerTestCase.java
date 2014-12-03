package org.exoplatform.commons.notification.channel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.api.notification.channel.AbstractChannelTemplateHandler;
import org.exoplatform.commons.api.notification.service.setting.ChannelManager;
import org.exoplatform.commons.notification.channel.mock.MockChannalTemplateHandler;
import org.exoplatform.commons.testing.BaseCommonsTestCase;

public class ChannelManagerTestCase extends BaseCommonsTestCase {
  private ChannelManager channelManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    channelManager = getService(ChannelManager.class);
    assertNotNull(channelManager);
  }
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  public void testChannelRegister() throws Exception {
    assertEquals(2, channelManager.getChannelIds().size());
    assertTrue(channelManager.getChannelIds().contains("email"));
    assertTrue(channelManager.getChannelIds().contains("intranet"));
  }
  

  public void testGetChannel() throws Exception {
    AbstractChannelTemplateHandler handler = channelManager.get("email").getTemplateHandlers().get(0);
    assertTrue(handler instanceof MockChannalTemplateHandler); 
    List<String> plugins = handler.getChannelTemplateEngine().getPluginIds();
    assertCollection(plugins, Arrays.asList("DigestDailyPlugin", "DigestWeeklyPlugin", "TestPlugin"), Collections.reverseOrder());
  }
  
  
  
}
