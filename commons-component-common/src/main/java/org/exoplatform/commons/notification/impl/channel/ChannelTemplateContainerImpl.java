package org.exoplatform.commons.notification.impl.channel;

import groovy.text.GStringTemplateEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.api.notification.channel.AbstractChannel;
import org.exoplatform.commons.api.notification.channel.AbstractChannelTemplateHandler;
import org.exoplatform.commons.api.notification.channel.ChannelTemplateEngine;
import org.exoplatform.commons.api.notification.service.setting.ChannelManager;
import org.exoplatform.commons.api.notification.service.setting.ChannelTemplateContainer;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

public class ChannelTemplateContainerImpl implements ChannelTemplateContainer, Startable {
  private static final Log      LOG = ExoLogger.getLogger(ChannelTemplateContainerImpl.class);
  private GStringTemplateEngine gTemplateEngine;
  private ChannelManager        channelManager;
  private Map<String, List<AbstractChannelTemplateHandler>> templateComponents = new HashMap<String, List<AbstractChannelTemplateHandler>>();

  public ChannelTemplateContainerImpl(ChannelManager channelManager) {
    this.channelManager = channelManager;
    this.gTemplateEngine = new GStringTemplateEngine();
  }

  @Override
  public void registerTemplateHandler(AbstractChannelTemplateHandler templateHandler) {
    List<AbstractChannelTemplateHandler> templateHandlers = templateComponents.get(templateHandler.getName());
    if (templateHandlers == null) {
      templateHandlers = new ArrayList<AbstractChannelTemplateHandler>();
      templateComponents.put(templateHandler.getName(), templateHandlers);
    }
    templateHandler.setIndex(templateHandlers.size());
    templateHandlers.add(templateHandler);
  }

  @Override
  public void start() {
    for (String channelId : channelManager.getChannelIds()) {
      List<AbstractChannelTemplateHandler> templateComponents_ = templateComponents.get(channelId);
      if (templateComponents_ == null) {
        continue;
      }
      Collections.sort(templateComponents_, new ComparatorDESC());
      List<String> pluginAdded = new ArrayList<String>();
      for (AbstractChannelTemplateHandler templateComponent : templateComponents_) {
        ChannelTemplateEngine templateHandler = templateComponent.getChannelTemplateEngine();
        for (String pluginId : templateHandler.getPluginIds()) {
          if (pluginAdded.contains(pluginId)) {
            continue;
          }
          String templatePath = templateHandler.getPath(pluginId);
          if (templatePath != null && templatePath.length() > 0) {
            try {
              String template = TemplateUtils.loadGroovyTemplate(templatePath);
              templateHandler.addTemplateEngine(pluginId, gTemplateEngine.createTemplate(template));
              pluginAdded.add(pluginId);
            } catch (Exception e) {
              LOG.warn("Failed to build groovy template engine for: " + templateComponent.getName(), e);
            }
          }
        }
      }
      //
      AbstractChannel channelPlugin = channelManager.get(channelId);
      if (channelPlugin != null) {
        channelPlugin.setTemplateHandler(templateComponents_);
      }
    }
  }

  @Override
  public void stop() {
    templateComponents = null;
  }

  private class ComparatorDESC implements Comparator<AbstractChannelTemplateHandler> {
    @Override
    public int compare(AbstractChannelTemplateHandler o1, AbstractChannelTemplateHandler o2) {
      Integer order1 = o1.getIndex();
      Integer order2 = o2.getIndex();
      return order2.compareTo(order1);
    }
  }

}
