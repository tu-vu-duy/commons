/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.commons.api.notification.service;

import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;

public abstract class AbstractSendMessageListener extends BaseComponentPlugin {
  private static final Log LOG = ExoLogger.getExoLogger(AbstractSendMessageListener.class);
  private QueueMessage queueMessageImpl;

  /**
   * Set the QueueMessage implement
   * 
   * @param queueMessageImpl
   */
  public void setQueueMessageImpl(QueueMessage queueMessageImpl) {
    this.queueMessageImpl = queueMessageImpl;
  }

  /**
   * Get the QueueMessage implement
   * 
   * @return
   */
  public QueueMessage getQueueMessageImpl() {
    return this.queueMessageImpl;
  }

  /**
   * Send the email message by MailService of kernel.
   * 
   * @param message
   * @return
   */
  public boolean sendMessage(Message message) {
    MailService mailService = (MailService) PortalContainer.getInstance()
                                                           .getComponentInstanceOfType(MailService.class);
    try {
      mailService.sendMessage(message);
      return true;
    } catch (Exception e) {
      LOG.warn("Failed at sendMessage().");
      LOG.debug(e.getMessage(), e);
      return false;
    }
  }

  /**
   * Puts the message into the queue
   * @param message
   * @return
   */
  public abstract boolean put(MessageInfo message);
  
  /**
   * Peek the message from queue and send
   */
  public abstract void send();

  /**
   * Start job send message notification
   * 
   * @throws Exception
   */
  public void start() {}

  /**
   * End job send message notification
   * 
   * @throws Exception
   */
  public void stop() {}
  
  /**
   * Create the rescheduleJob to send email notification messages. 
   * @param limit The max number of emails send each time.
   * @param interval The time interval between round run job (period job).
   */
  public void rescheduleJob(int limit, long interval) {}
}
