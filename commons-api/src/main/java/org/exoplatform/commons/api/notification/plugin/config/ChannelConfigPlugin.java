package org.exoplatform.commons.api.notification.plugin.config;

public class ChannelConfigPlugin {
  private String channelId;
  private String resourceBundleKey = "";  
  private String order;
  public String getChannelId() {
    return channelId;
  }
  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }
  public String getResourceBundleKey() {
    return resourceBundleKey;
  }
  public void setResourceBundleKey(String resourceBundleKey) {
    this.resourceBundleKey = resourceBundleKey;
  }
  public String getOrder() {
    return order;
  }
  public void setOrder(String order) {
    this.order = order;
  }
}
