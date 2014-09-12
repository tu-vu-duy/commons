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
package org.exoplatform.commons.notification.impl.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.notification.NotificationConfiguration;
import org.exoplatform.commons.notification.NotificationContextFactory;
import org.exoplatform.commons.notification.impl.AbstractService;
import org.exoplatform.commons.notification.impl.NotificationSessionManager;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.json.JSONObject;

public class DefaultSendMessageListener extends InMemorySendMessageListener {
  private static final Log LOG = ExoLogger.getExoLogger(DefaultSendMessageListener.class);
  private static final int BUFFER_SIZE = 32;
  private static long       sinceTime   = 0;

  /** .. */
  private NotificationConfiguration configuration;
  /** The lock protecting all mutators */
  transient final ReentrantLock lock = new ReentrantLock();
  /** using the set to keep the messages. */
  protected Set<MessageInfo> messages = Collections.synchronizedSet(new HashSet<MessageInfo>());
  private ThreadLocal<Set<String>> idsRemovingLocal = new ThreadLocal<Set<String>>();

  public DefaultSendMessageListener(InitParams params) {
    super(params);
    this.configuration = CommonsUtils.getService(NotificationConfiguration.class);
  }

  @Override
  public boolean put(MessageInfo message) {
    return saveMessageInfo(message);
  }

  @Override
  public void send() {
    LOG.debug("Starting to send email notification by DegistSendMailMessage...");
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      //
      load(sProvider);
      if (idsRemovingLocal.get() == null) {
        idsRemovingLocal.set(new HashSet<String>());
      }
      //
      if (messages.size() > 0) {
        LOG.info(messages.size() + " message(s) will be sent.");
      }
      
      for (MessageInfo messageInfo : messages) {
        if (messageInfo != null && !idsRemovingLocal.get().contains(messageInfo.getId())
            && getQueueMessageImpl().sendMessage(messageInfo.makeEmailNotification())) {
          
          LOG.debug("Message sent to user: " + messageInfo.getTo());
          //
          idsRemovingLocal.get().add(messageInfo.getId());
          if (stats) {
            NotificationContextFactory.getInstance().getStatisticsCollector().pollQueue(messageInfo.getPluginId());
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Failed to send message.");
      LOG.debug(e.getMessage(), e);
    } finally {
      sProvider.close();
      removeMessageInfo();
    }
  }

  /**
   * Loading the messageInfo as buffer with Limit
   * and sinceTime
   * @param sProvider
   */
  private void load(SessionProvider sProvider) {
    try {
      NodeIterator iterator = getMessageInfoNodes(sProvider);
      while (iterator.hasNext()) {
        Node node = iterator.nextNode();
        long createdTime = Long.parseLong(node.getName());
        if ((sinceTime == 0 || sinceTime < createdTime)) {
          MessageInfo messageInfo = getMessageInfo(node);
          messageInfo.setId(node.getUUID());
          messages.add(messageInfo);

          sinceTime = createdTime;
        } else {
          sinceTime = 0;
          messages.clear();
          break;
        }
      }
    } catch (Exception e) {
      LOG.warn("Failed to load message.");
      LOG.debug(e.getMessage(), e);
    }
  }

  private boolean saveMessageInfo(MessageInfo message) {
    final ReentrantLock lock = this.lock;
    SessionProvider sProvider = NotificationSessionManager.createSystemProvider();
    try {
      lock.lock();
      message.setCreatedTime(System.currentTimeMillis());
      Node messageInfoHome = AbstractService.getMessageInfoHomeNode(sProvider, configuration.getWorkspace());
      Node messageInfoNode = messageInfoHome.addNode(String.valueOf(message.getCreatedTime()), AbstractService.NTF_MESSAGE_INFO);
      if (messageInfoNode.canAddMixin("mix:referenceable")) {
        messageInfoNode.addMixin("mix:referenceable");
      }
      //
      saveData(messageInfoNode, compress(message.toJSON()));
      messageInfoNode.getSession().save();
      return true;
    } catch (Exception e) {
      LOG.warn("Failed to save message.");
      LOG.debug(e.getMessage() + message.toJSON(), e);
    } finally {
      lock.unlock();
    }
    return false;
  }

  private void removeMessageInfo() {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    final ReentrantLock lock = this.lock;
    List<String> ids = new ArrayList<String>(idsRemovingLocal.get()) ;
    try {
      lock.lock();
      Session session = AbstractService.getSession(sProvider, configuration.getWorkspace());
      for (String messageId : ids) {
        session.getNodeByUUID(messageId).remove();
        //
        LOG.debug("Removing messageId: " + messageId);
      }
      session.save();
    } catch (Exception e) {
      LOG.warn("Failed to remove message.");
      LOG.debug(e.getMessage(), e);
    } finally {
      messages.clear();
      idsRemovingLocal.get().removeAll(ids);
      lock.unlock();
      sProvider.close();
    }
  }

  private NodeIterator getMessageInfoNodes(SessionProvider sProvider) {
    try {
      Node messageInfoHome = AbstractService.getMessageInfoHomeNode(sProvider, configuration.getWorkspace());
      QueryManager qm = messageInfoHome.getSession().getWorkspace().getQueryManager();
      StringBuilder sqlQuery = new StringBuilder();
      sqlQuery.append("SELECT * FROM ").append(AbstractService.NTF_MESSAGE_INFO)
              .append(" WHERE jcr:path LIKE '").append(messageInfoHome.getPath()).append("/%' AND NOT jcr:path LIKE '")
              .append(messageInfoHome.getPath()).append("/%/%'")
              .append(" ORDER BY exo:name");
      QueryImpl query = (QueryImpl) qm.createQuery(sqlQuery.toString(), Query.SQL);
      query.setOffset(0);
      query.setLimit(limit);
      QueryResult result = query.execute();
      return result.getNodes();
    } catch (Exception e) {
      LOG.warn("Failed to get message from node.");
      LOG.debug(e.getMessage(), e);
    }
    return null;
  }

  private MessageInfo getMessageInfo(Node messageInfoNode) {
    try {
      String messageJson = getDataJson(messageInfoNode);
      JSONObject object = new JSONObject(messageJson);
      MessageInfo info = new MessageInfo();
      info.pluginId(object.optString("pluginId"))
          .from(object.getString("from"))
          .to(object.getString("to"))
          .subject(object.getString("subject"))
          .body(object.getString("body"))
          .footer(object.optString("footer"))
          .setCreatedTime(object.getLong("createdTime"));
      //
      return info;
    } catch (Exception e) {
      LOG.warn("Failed to map message between node and model.");
      LOG.debug(e.getMessage(), e);
    }
    return null;
  }

  private void saveData(Node node, InputStream is) throws Exception {
    Node fileNode = node.addNode("datajson", "nt:file");
    Node nodeContent = fileNode.addNode("jcr:content", "nt:resource");
    //
    nodeContent.setProperty("jcr:mimeType", "application/x-gzip");
    nodeContent.setProperty("jcr:data", is);
    nodeContent.setProperty("jcr:lastModified", Calendar.getInstance().getTimeInMillis());
  }

  private String getDataJson(Node node) throws Exception {
    Node fileNode = node.getNode("datajson");
    Node nodeContent = fileNode.getNode("jcr:content");
    InputStream stream = nodeContent.getProperty("jcr:data").getStream();
    return decompress(stream);
  }

  private static InputStream compress(String string) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream(string.length());
    GZIPOutputStream gos = new GZIPOutputStream(os);
    gos.write(string.getBytes());
    gos.close();
    byte[] compressed = os.toByteArray();
    os.close();
    return new ByteArrayInputStream(compressed);
  }

  private static String decompress(InputStream is) throws IOException {
    GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
    StringBuilder string = new StringBuilder();
    byte[] data = new byte[BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = gis.read(data)) != -1) {
      string.append(new String(data, 0, bytesRead));
    }
    gis.close();
    is.close();
    return string.toString();
  }
}
