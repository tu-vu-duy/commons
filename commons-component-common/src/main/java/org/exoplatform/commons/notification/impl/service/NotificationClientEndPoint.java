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

@ClientEndpoint(encoders = { MessageEncoder.class }, decoders = { MessageDecoder.class })
public class NotificationClientEndPoint {
    @OnOpen
    public void onOpen(Session session) {
      System.out.println("Open");
    }
 
    @OnMessage
    public void processMessage(Message message) {
      System.out.println("Received message in client: " + message);
    }
 
    @OnError
    public void processError(Throwable t) {
      t.printStackTrace();
    }
}