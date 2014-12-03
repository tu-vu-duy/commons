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
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

public class ChannelManagerImpl implements ChannelManager, Startable {
  private static final Log LOG = ExoLogger.getLogger(ChannelManagerImpl.class);
  private Map<String, AbstractChannel> channelPlugins = new HashMap<String, AbstractChannel>();
  private Map<String, List<AbstractChannelTemplateHandler>> templateComponents = new HashMap<String, List<AbstractChannelTemplateHandler>>();
  
  private GStringTemplateEngine gTemplateEngine;
  
  public ChannelManagerImpl() {
    gTemplateEngine = new GStringTemplateEngine();
  }

  @Override
  public void registerChannel(AbstractChannel channelPlugin) {
    channelPlugins.put(channelPlugin.getId(), channelPlugin);
  }

  @Override
  public void registerTemplateHandler(AbstractChannelTemplateHandler templateComponent) {
    List<AbstractChannelTemplateHandler> templateHandlers = templateComponents.get(templateComponent.getName());
    if(templateHandlers == null) {
      templateHandlers = new ArrayList<AbstractChannelTemplateHandler>();
      templateComponents.put(templateComponent.getName(), templateHandlers);
    }
    templateComponent.setIndex(templateHandlers.size());
    templateHandlers.add(templateComponent);
  }

  @Override
  public List<AbstractChannel> gets() {
    return Collections.unmodifiableList(new ArrayList<AbstractChannel>(channelPlugins.values()));
  }
  
  public List<String> getChannelIds() {
    return new ArrayList<String>(channelPlugins.keySet());
  }
  
  @Override
  public AbstractChannel get(String id) {
    if(channelPlugins.containsKey(id)) {
      return channelPlugins.get(id);
    }
    return null;
  }

  @Override
  public void start() {
    for (String channelId : getChannelIds()) {
      List<AbstractChannelTemplateHandler> templateComponents_ = templateComponents.get(channelId);
      if(templateComponents_ == null) {
        continue;
      }
      Collections.sort(templateComponents_, new ComparatorDESC());
      List<String> pluginAdded = new ArrayList<String>();
      for (AbstractChannelTemplateHandler templateComponent : templateComponents_) {
        ChannelTemplateEngine templateHandler = templateComponent.getChannelTemplateEngine();
        for (String pluginId : templateHandler.getPluginIds()) {
          if(pluginAdded.contains(pluginId)) {
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
      AbstractChannel channelPlugin = get(channelId);
      if(channelPlugin != null) {
        channelPlugin.setTemplateHandler(templateComponents_);
      }
    }
  }

  @Override
  public void stop() {
    channelPlugins = null;
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
