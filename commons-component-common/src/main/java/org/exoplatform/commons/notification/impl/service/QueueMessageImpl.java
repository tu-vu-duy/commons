/*
 * Copyright (C) 2003-${year} eXo Platform SAS.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.commons.notification.impl.service;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.service.AbstractSendMessageListener;
import org.exoplatform.commons.api.notification.service.QueueMessage;
import org.exoplatform.commons.notification.NotificationConfiguration;
import org.exoplatform.commons.notification.NotificationContextFactory;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.impl.AbstractService;
import org.exoplatform.commons.notification.impl.log.AuditEmailMessageLog;
import org.exoplatform.commons.notification.impl.log.SimpleAuditEmailMessageLog;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.management.annotations.ManagedBy;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.Message;
import org.picocontainer.Startable;

@ManagedBy(SendEmailService.class)
public class QueueMessageImpl extends AbstractService implements QueueMessage, Startable {
  private static final Log LOG = ExoLogger.getExoLogger(QueueMessageImpl.class);
  /** .. */
  private SendEmailService sendEmailService;
  /** .. */
  private NotificationConfiguration configuration;
  
  private AbstractSendMessageListener sendMessageListener = null;
  private AbstractSendMessageListener defaultConfigMessageListener = null;

  private AuditEmailMessageLog<?>  messageLog;

  public QueueMessageImpl(NotificationConfiguration configuration) {
    this.configuration = configuration;
    this.messageLog = new SimpleAuditEmailMessageLog();
  }

  public void setManagementView(SendEmailService managementView) {
    this.sendEmailService = managementView;
  }
  
  public void setAuditLog(AuditEmailMessageLog<?> messageLog) {
    this.messageLog = messageLog;
  }

  @Override
  public void addPlugin(AbstractSendMessageListener baseComponent) {
    if (baseComponent == null) {
      this.sendMessageListener = defaultConfigMessageListener;
      this.sendMessageListener.setQueueMessageImpl(this);
      resetDefaultConfigJob();
    } else {
      this.sendMessageListener = baseComponent;
      this.sendMessageListener.setQueueMessageImpl(this);
    }
  }
  
  public void clearLogs() {
    LOG.debug("===> Clear the log message");
    messageLog.clear();
  }

  public String getLogs() {
    return messageLog.log();
  }
  
  public void resetDefaultConfigJob() {
    sendMessageListener.rescheduleJob(0, -1);
  }
  
  public void rescheduleJob(int limit, long interval) {
    sendMessageListener.rescheduleJob(limit, interval);
  }

  @Override
  public void start() {
    //
    sendMessageListener.start();
    //
    sendEmailService.registerManager(this);
    //
    defaultConfigMessageListener = sendMessageListener;
  }

  @Override
  public void stop() {
    sendMessageListener.stop();
  }

  @Override
  public boolean put(MessageInfo message) {
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    //
    if (message == null || message.getTo() == null || message.getTo().length() == 0) {
      return false;
    }
    //
    if (NotificationUtils.isValidEmailAddresses(message.getTo()) == false) {
      LOG.warn(String.format("The email %s is not valid for sending notification", message.getTo()));
      return false;
    }
    //
    if (stats) {
      LOG.info("Tenant Name:: " + CommonsUtils.getRepository().getConfiguration().getName());
      LOG.info("Message::From: " + message.getFrom() + " To: " + message.getTo() + " body: " + message.getBody());
    }
    //
    sendMessageListener.put(message);
    //
    messageLog.put(message.getPluginId(), message.makeEmailNotification());
    if (sendEmailService.isOn()) {
      sendEmailService.addCurrentCapacity();
    }
    return true;
  }

  @Override
  public void send() {
    sendMessageListener.send();
  }
  
  @Override
  public boolean sendMessage(Message message) {
    if (!sendEmailService.isOn()) {
      if (message.getFrom() == null) {
        return false;
      }
      boolean sent = sendMessageListener.sendMessage(message);
      final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
      if (sent && stats) {
        LOG.info("Sent email::From: " + message.getFrom() + " To: " + message.getTo() + " subject: " + message.getSubject());
      }
      messageLog.put("Sent", message);
      return sent;
    } else {
      sendEmailService.removeCurrentCapacity();
    }
    sendEmailService.counter();
    return true;
  }

  public String removeAll() {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    int t = 0, j = 0;
    String pli="";
    try {
      Session session = getSession(sProvider, configuration.getWorkspace());
      Node root = session.getRootNode();
      //
      LOG.trace("Removing messages: ");
      if (root.hasNode("eXoNotification/messageInfoHome")) {
        NodeIterator it = root.getNode("eXoNotification/messageInfoHome").getNodes();
        //
        removeNodes(session, it);
      }
      LOG.trace("Done to removed messages! ");
      //
      LOG.trace("Removing notification info... ");
      NodeIterator it = root.getNode("eXoNotification/messageHome").getNodes();
      List<String> pluginPaths = new ArrayList<String>();
      while (it.hasNext()) {
        pluginPaths.add(it.nextNode().getPath());
      }
      session.logout();
      for (String string : pluginPaths) {
        pli = string;
        LOG.trace("Remove notification info on plugin: " + pli);
        //
        session = getSession(sProvider, configuration.getWorkspace());
        it = ((Node) session.getItem(string)).getNodes();
        while (it.hasNext()) {
          NodeIterator hIter = it.nextNode().getNodes();
          j = removeNodes(session, hIter);
          t += j;
        }
        LOG.trace("Removed " + j + " nodes info on plugin: " + pli);
        session.logout();
      }

      return "Done to removed " + t + " nodes!";
    } catch (Exception e) {
      LOG.trace("Removed " + j + " nodes info on plugin: " + pli);
      LOG.trace("Removed all " + t + " nodes.");
      LOG.debug("Failed to remove all data of feature notification." + e.getMessage());
    } finally {
      sProvider.close();
    }
    return "Failed to remove all. Please, try again !";
  }
  
  private int removeNodes(Session session, NodeIterator it) throws Exception {
    int i = 0, size = Integer.valueOf(System.getProperty("sizePersiter", "200"));
    while (it.hasNext()) {
      it.nextNode().remove();
      ++i;
      if (i % size == 0) {
        session.save();
      }
      System.out.print(".");
    }
    session.save();
    return i;
  }
  
  public String removeUsersSetting() {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    int t = 0;
    try {
      Session session = getSession(sProvider, configuration.getWorkspace());
      Node root = session.getRootNode();
      LOG.trace("Removing all user settings: ");
      if (root.hasNode("settings/user")) {
        NodeIterator it = root.getNode("settings/user").getNodes();
        //
        t = removeNodes(session, it);
      }
      return "Done to removed " + t + " users!";
    } catch (Exception e) {
      LOG.trace("Removed all " + t + " nodes.");
      LOG.debug("Failed to remove all data of feature notification." + e.getMessage());
    } finally {
      sProvider.close();
    }
    return "Failed to remove all. Please, try again !";
  }
}
