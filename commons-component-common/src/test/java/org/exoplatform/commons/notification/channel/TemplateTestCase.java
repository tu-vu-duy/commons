package org.exoplatform.commons.notification.channel;

import junit.framework.TestCase;

import org.exoplatform.commons.api.notification.channel.ChannelTemplateEngine;
import org.exoplatform.commons.notification.channel.mock.MockChannalTemplateHandler;
import org.exoplatform.services.idgenerator.impl.IDGeneratorServiceImpl;
import org.exoplatform.services.jcr.util.IdGenerator;

public class TemplateTestCase extends TestCase {

  public void testInitTemplateChannel() throws Exception {
    new IdGenerator(new IDGeneratorServiceImpl());
     //
    MockChannalTemplateHandler channelHandler = new MockChannalTemplateHandler();
    
    ChannelTemplateEngine handler = channelHandler.getChannelTemplateEngine();
    
    assertEquals(3, handler.getPluginIds().size());
    assertTrue(handler.getPluginIds().contains("TestPlugin"));
  }
}
