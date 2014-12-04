package org.exoplatform.commons.notification.channel.mock;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.AbstractChannel;
import org.exoplatform.commons.api.notification.channel.AbstractChannelTemplateHandler;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.container.xml.InitParams;

public class MockTestChannel extends AbstractChannel {
  private List<MessageInfo> keepDatas = new ArrayList<MessageInfo>();
  public MockTestChannel(InitParams initParams) {
    super(initParams);
  }

  @Override
  public void sendNotification(NotificationInfo notification) throws Exception {
    NotificationContext nCtx = NotificationContextImpl.cloneInstance().setNotificationInfo(notification);
    //
    AbstractChannelTemplateHandler templateHandler = getTemplateHandler(notification.getKey().getId());
    if (templateHandler != null) {
      MessageInfo info = templateHandler.makeMessage(nCtx);
      if (info != null) {
        info.pluginId(getId()).from(NotificationPluginUtils.getFrom(notification.getFrom()))
            .to(notification.getTo()).end();
        keepDatas.add(info);
      }
    }
  }

  public List<MessageInfo> getKeepDatas() {
    return keepDatas;
  }

  public void setKeepDatas(List<MessageInfo> keepDatas) {
    this.keepDatas = keepDatas;
  }
}
