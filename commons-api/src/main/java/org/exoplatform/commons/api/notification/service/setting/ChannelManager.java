package org.exoplatform.commons.api.notification.service.setting;

import java.util.List;

import org.exoplatform.commons.api.notification.channel.AbstractChannel;
import org.exoplatform.commons.api.notification.channel.AbstractChannelTemplateHandler;

public interface ChannelManager {
  /**
   * @param notifChannelConfig
   */
  void registerChannel(AbstractChannel notifChannelConfig);
  
  /**
   * @param templateComponent
   */
  void registerTemplateHandler(AbstractChannelTemplateHandler templateComponent);
  /**
   * @return
   */
  List<AbstractChannel> gets();

  /**
   * @return
   */
  List<String> getChannelIds();
  /**
   * @param id
   * @return
   */
  AbstractChannel get(String id);

}
