/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.commons.notification.impl.setting;

import groovy.text.GStringTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationChildPlugin;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.plugin.config.PluginConfig;
import org.exoplatform.commons.api.notification.service.setting.PluginContainer;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.notification.template.ResourceBundleConfigDeployer;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.gatein.wci.ServletContainerFactory;
import org.picocontainer.Startable;

public class NotificationPluginContainer implements PluginContainer, Startable {

  private final Map<PluginKey, AbstractNotificationPlugin> pluginMap;
  
  //parent key and list child key
  private final Map<PluginKey, List<PluginKey>>      parentChildrenKeysMap;

  private PluginSettingService                                   pSettingService;
  private ResourceBundleConfigDeployer                           deployer;
  private GStringTemplateEngine gTemplateEngine;
  
  public NotificationPluginContainer() {
    pluginMap = new HashMap<PluginKey, AbstractNotificationPlugin>();
    parentChildrenKeysMap = new HashMap<PluginKey, List<PluginKey>>();
    pSettingService = CommonsUtils.getService(PluginSettingService.class);
    deployer = new ResourceBundleConfigDeployer();
    gTemplateEngine = new GStringTemplateEngine();
  }

  @Override
  public void start() {
    Set<String> datas = new HashSet<String>();
    // register plugin
    
    for (AbstractNotificationPlugin plugin : pluginMap.values()) {
      boolean isChild = (plugin instanceof AbstractNotificationChildPlugin);
      for (PluginConfig pluginConfig : plugin.getPluginConfigs()) {
        pSettingService.registerPluginConfig(pluginConfig.isChildPlugin(isChild));
        datas.add(pluginConfig.getBundlePath());
      }
    }
    //
    if (ServletContainerFactory.getServletContainer().addWebAppListener(deployer)) {
      deployer.initBundlePath(datas);
    }
  }

  @Override
  public void stop() {
    ServletContainerFactory.getServletContainer().removeWebAppListener(deployer);
  }

  @Override
  public AbstractNotificationPlugin getPlugin(PluginKey key) {
    return pluginMap.get(key);
  }

  @Override
  public List<PluginKey> getChildPluginKeys(PluginKey parentKey) {
    List<PluginKey> keys = parentChildrenKeysMap.get(parentKey);
    if (keys != null) {
      return keys;
    }
    return new ArrayList<PluginKey>();
  }

  @Override
  public void addChildPlugin(AbstractNotificationChildPlugin plugin) {
    addPlugin(plugin);
    //
    List<String> parentIds = plugin.getParentPluginIds();
    PluginKey parentKey;
    List<PluginKey> childrenKeys;
    for (String parentId : parentIds) {
      parentKey = new PluginKey(parentId);
      if (parentChildrenKeysMap.containsKey(parentKey)) {
        childrenKeys = parentChildrenKeysMap.get(parentKey);
      } else {
        childrenKeys = new ArrayList<PluginKey>();
      }
      //
      childrenKeys.add(plugin.getKey());
      parentChildrenKeysMap.put(parentKey, childrenKeys);
    }
    //
    String templatePath = plugin.getTemplatePath();
    if (templatePath != null && templatePath.length() > 0) {
      try {
        String template = TemplateUtils.loadGroovyTemplate(templatePath);
        plugin.setTemplateEngine(gTemplateEngine.createTemplate(template));
      } catch (Exception e) {
        ExoLogger.getExoLogger(getClass()).warn("Failed to build groovy template engine for: " + plugin.getId(), e);
      }
    }
  }

  @Override
  public void addPlugin(AbstractNotificationPlugin plugin) {
    pluginMap.put(plugin.getKey(), plugin);
  }

  @Override
  public boolean remove(PluginKey key) {
    pluginMap.remove(key);
    return true;
  }

}
