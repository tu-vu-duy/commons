/**
 * 
 */
package org.exoplatform.commons.notification.impl.service;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.exoplatform.commons.notification.impl.service.Message.MessageDecoder;
import org.exoplatform.commons.notification.impl.service.Message.MessageEncoder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@ClientEndpoint(encoders = { MessageEncoder.class }, decoders = { MessageDecoder.class })
public class NotificationClientEndPoint {
  private static final Log LOG = ExoLogger.getLogger(NotificationClientEndPoint.class);

  @OnOpen
  public void onOpen(Session session) {
  }

  @OnMessage
  public void processMessage(Message message) {
  }

  @OnError
  public void processError(Throwable t) {
    LOG.error(t);
  }
}