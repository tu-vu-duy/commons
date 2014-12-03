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
  protected ChannelTemplateEngine engine    = null;
  private int index = 0;
  

  public AbstractChannelTemplateHandler() {
    LOG = ExoLogger.getLogger(this.getClass());
    engine = new ChannelTemplateEngine();
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
      engine.setPath(tConfigs[i].pluginId(), tConfigs[i].path());
    }
  }

  public ChannelTemplateEngine getChannelTemplateEngine() {
    return engine;
  }

  public void setTemplateHandler(ChannelTemplateEngine templateHandler) {
    this.engine = templateHandler;
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
