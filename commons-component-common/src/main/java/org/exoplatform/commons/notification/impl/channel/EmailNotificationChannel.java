package org.exoplatform.commons.notification.impl.channel;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.AbstractChannel;
import org.exoplatform.commons.api.notification.channel.AbstractChannelTemplateHandler;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.notification.NotificationContextFactory;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.impl.service.NotificationServiceImpl;
import org.exoplatform.commons.notification.impl.service.QueueMessageImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class EmailNotificationChannel extends AbstractChannel {
  private static final Log LOG = ExoLogger.getLogger(NotificationServiceImpl.class);
  public EmailNotificationChannel(InitParams initParams) {
    super(initParams);
  }

  @Override
  public void sendNotification(NotificationInfo notification) throws Exception {
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    NotificationContext nCtx = NotificationContextImpl.cloneInstance().setNotificationInfo(notification);
    //
    AbstractChannelTemplateHandler templateHandler = getTemplateHandler(notification.getKey().getId());
    if (templateHandler != null) {
      MessageInfo info = templateHandler.makeMessage(nCtx);
      if (info != null) {
        info.pluginId(getId()).from(NotificationPluginUtils.getFrom(notification.getFrom()))
            .to(NotificationPluginUtils.getTo(notification.getTo())).end();
        if (NotificationUtils.isValidEmailAddresses(info.getTo()) == true) {
          CommonsUtils.getService(QueueMessageImpl.class).sendMessage(info.makeEmailNotification());
        } else {
          LOG.warn(String.format("The email %s is not valid for sending notification", info.getTo()));
        }
        if (stats) {
          NotificationContextFactory.getInstance().getStatisticsCollector().createMessageInfoCount(info.getPluginId());
        }
      }
    }
  }
  
}
