package org.exoplatform.commons.notification.impl.channel;

import java.util.Calendar;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.AbstractChannel;
import org.exoplatform.commons.api.notification.channel.AbstractChannelTemplateHandler;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.service.storage.IntranetNotificationDataStorage;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.impl.service.NotificationServiceImpl;
import org.exoplatform.commons.notification.impl.service.storage.IntranetNotificationDataStorageImpl;
import org.exoplatform.commons.notification.net.WebSocketBootstrap;
import org.exoplatform.commons.notification.net.WebSocketServer;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.vertx.java.core.json.JsonObject;

public class IntranetNotificationChannel extends AbstractChannel {
  private static final Log LOG = ExoLogger.getLogger(NotificationServiceImpl.class);
  private final IntranetNotificationDataStorage dataStorage;
  public IntranetNotificationChannel(InitParams initParams) {
    super(initParams);
    dataStorage = CommonsUtils.getService(IntranetNotificationDataStorage.class);
    ((IntranetNotificationDataStorageImpl) dataStorage).setChannelId(channelId);
  }

  @Override
  public void sendNotification(NotificationInfo notification) throws Exception {
    NotificationContext nCtx = NotificationContextImpl.cloneInstance();
    try {
      notification.setLastModifiedDate(Calendar.getInstance());
      notification.setId(new NotificationInfo().getId());
      nCtx.setNotificationInfo(notification);
      AbstractChannelTemplateHandler templateHandler = getTemplateHandler(notification.getKey().getId());
      if (templateHandler != null) {
        MessageInfo info = templateHandler.makeMessage(nCtx);
        if (info != null) {
          String message = info.getBody();
          WebSocketBootstrap.sendMessage(WebSocketServer.NOTIFICATION_WEB_IDENTIFIER, notification.getTo(),
                                         new JsonObject().putString("message", message).encode());
          //
          dataStorage.save(notification);
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to connect with server : " + e, e.getMessage());
    }
  }

}
