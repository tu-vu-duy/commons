package org.exoplatform.commons.notification.impl.channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.api.notification.channel.AbstractChannel;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.service.setting.ChannelManager;

public class ChannelManagerImpl implements ChannelManager {
  private Map<String, AbstractChannel> channelPlugins = new HashMap<String, AbstractChannel>();
  private List<String> channelIds;
  
  public ChannelManagerImpl() {
  }

  @Override
  public void registerChannel(AbstractChannel channelPlugin) {
    channelPlugins.put(channelPlugin.getId(), channelPlugin);
  }

  @Override
  public List<AbstractChannel> gets() {
    return Collections.unmodifiableList(new ArrayList<AbstractChannel>(channelPlugins.values()));
  }
  
  public List<String> getChannelIds() {
    if (channelIds == null) {
      channelIds = new ArrayList<String>();
      channelIds.add(UserSetting.EMAIL_CHANNEL);
      for (String channelId : channelPlugins.keySet()) {
        if (!UserSetting.EMAIL_CHANNEL.equals(channelId)) {
          channelIds.add(channelId);
        }
      }
    }
    return channelIds;
  }
  
  @Override
  public AbstractChannel get(String id) {
    if(channelPlugins.containsKey(id)) {
      return channelPlugins.get(id);
    }
    return null;
  }

}
