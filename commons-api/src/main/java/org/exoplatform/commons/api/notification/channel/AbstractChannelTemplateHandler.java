package org.exoplatform.commons.api.notification.channel;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@ChannelConfigs (
   id="email",
   templates = {}
)
public abstract class AbstractChannelTemplateHandler extends BaseComponentPlugin {
  protected static Log            LOG;
  protected String                channelId = "";
  protected ChannelTemplateEngine templateEngine    = null;
  private int index = 0;
  

  public AbstractChannelTemplateHandler() {
    LOG = ExoLogger.getLogger(this.getClass());
    templateEngine = new ChannelTemplateEngine();
    channelId = getName();
    //
    ChannelConfigs config = this.getClass().getAnnotation(ChannelConfigs.class);
    if (config == null) {
      return;
    }
    if (!"".equals(config.id())) {
      channelId = config.id();
    }
    TemplateConfig[] tConfigs = config.templates();
    for (int i = 0; i < tConfigs.length; i++) {
      templateEngine.setPath(tConfigs[i].pluginId(), tConfigs[i].path());
    }
  }

  public ChannelTemplateEngine getChannelTemplateEngine() {
    return templateEngine;
  }

  public void setTemplateHandler(ChannelTemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }
  
  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }
  /**
   * Makes the MessageInfor from given NotificationMessage what keep inside NotificationContext
   * @param context
   * @return
   */
  public abstract MessageInfo makeMessage(NotificationContext ctx);

}
