package org.exoplatform.commons.api.notification.channel;

import groovy.text.Template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelTemplateEngine {

  private String channelId;
  private Map<String, String> paths = new HashMap<String, String>();
  private Map<String, Template> templateEngines = new HashMap<String, Template>();
  public ChannelTemplateEngine() {
  }

  /**
   * Get TemplateEngine of plugin
   * @return the TemplateEngine
   */
  public Template getTemplateEngine(String pluginId) {
    if (templateEngines.containsKey(pluginId)) {
      return templateEngines.get(pluginId);
    }
    return null;
  }

  /**
   * Set TemplateEngine for plugin
   * @param engine the TemplateEngine to set
   */
  public void addTemplateEngine(String pluginId, Template engine) {
    templateEngines.put(pluginId, engine);
  }

  /**
   * @return
   */
  public String getPath(String pluginId) {
    if (paths.containsKey(pluginId)) {
      return paths.get(pluginId);
    }
    return null;
  }

  /**
   * @param path
   */
  public ChannelTemplateEngine setPath(String pluginId, String path) {
    paths.put(pluginId, path);
    return this;
  }

  public List<String> getPluginIds() {
    return Collections.unmodifiableList(new ArrayList<String>(paths.keySet()));
  }

  /**
   * @return
   */
  public String getChannelId() {
    return channelId;
  }

  /**
   * @param channelId
   */
  public ChannelTemplateEngine setChannelId(String channelId) {
    this.channelId = channelId;
    return this;
  }
}
