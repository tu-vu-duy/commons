/**
 * 
 */
package org.exoplatform.commons.api.notification.channel;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.config.ChannelConfigPlugin;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;

public abstract class AbstractChannel extends BaseComponentPlugin {
  protected ChannelConfigPlugin configPlugin = null;
  protected String channelId;
  protected List<AbstractChannelTemplateHandler> templateHandlers = new ArrayList<AbstractChannelTemplateHandler>();

  public AbstractChannel(InitParams initParams) {
    List<ChannelConfigPlugin> chanels = initParams.getObjectParamValues(ChannelConfigPlugin.class);
    if (chanels.size() > 0) {
      configPlugin = chanels.get(0);
      this.channelId = configPlugin.getChannelId();
    } else {
      channelId = getName();
    }
    //
  }

  public ChannelConfigPlugin getChannelConfigPlugin() {
    return configPlugin;
  }

  public String getId() {
    return channelId;
  }

  public List<AbstractChannelTemplateHandler> getTemplateHandlers() {
    return templateHandlers;
  }

  public AbstractChannelTemplateHandler getTemplateHandler(String pluginId) {
    for (AbstractChannelTemplateHandler templateHandler : templateHandlers) {
      if (templateHandler.getChannelTemplateEngine().getPluginIds().contains(pluginId)) {
        return templateHandler;
      }
    }
    return null;
  }

  public void setTemplateHandler(List<AbstractChannelTemplateHandler> templateHandler) {
    this.templateHandlers = templateHandler;
  }

  public abstract void sendNotification(NotificationInfo notification) throws Exception;
}
