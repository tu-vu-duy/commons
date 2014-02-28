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
package org.exoplatform.commons.notification.impl.service.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.commons.api.notification.node.TreeNode;
import org.exoplatform.commons.api.notification.service.NotificationService;
import org.exoplatform.commons.api.notification.service.listener.NTFEvent;
import org.exoplatform.commons.api.notification.service.storage.NotificationDataStorage;
import org.exoplatform.commons.notification.NotificationConfiguration;
import org.exoplatform.commons.notification.NotificationContextFactory;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.impl.AbstractService;
import org.exoplatform.commons.notification.impl.NotificationSessionManager;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NotificationDataStorageImpl extends AbstractService implements NotificationDataStorage {
  private static final Log        LOG              = ExoLogger.getLogger(NotificationDataStorageImpl.class);
  private String                    workspace;
  private NotificationConfiguration configuration    = null;
  private ThreadLocal<Map<String, Node>> parentNodeMap = new ThreadLocal<Map<String,Node>>();
  
  private final ReentrantLock lock = new ReentrantLock();

  public NotificationDataStorageImpl(NotificationConfiguration configuration) {
    this.workspace = configuration.getWorkspace();
    this.configuration = configuration;
  }
  
  public void resetParentNodeMap() {
    if (this.parentNodeMap.get() == null) {
      this.parentNodeMap.set(new HashMap<String, Node>());
    } else {
      this.parentNodeMap.get().clear();
    }
  }

  public void save(NotificationInfo message) throws Exception {
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    SessionProvider sProvider = CommonsUtils.getSystemSessionProvider();
    final ReentrantLock localLock = lock;
    try {
      localLock.lock();
      Node messageHomeNode = getOrCreateMessageParent(sProvider, workspace, message.getKey().getId());
      Node messageNode = null;
      if (messageHomeNode.hasNode(message.getName())) {
        messageNode = messageHomeNode.getNode(message.getName());
        // record entity update here
        if (stats) {
          NotificationContextFactory.getInstance().getStatisticsCollector().updateEntity(NTF_MESSAGE);
        }
      } else {
        messageNode = messageHomeNode.addNode(message.getName(), NTF_MESSAGE);
        messageHomeNode.addMixin("mix:referenceable");
        message.setId(messageNode.getUUID());
        // record statistics insert entity
        if (stats) {
          NotificationContextFactory.getInstance().getStatisticsCollector().insertEntity(NTF_MESSAGE);
        }
      }
      messageNode.setProperty(NTF_FROM, message.getFrom());
      messageNode.setProperty(NTF_ORDER, message.getOrder());
      messageNode.setProperty(NTF_PROVIDER_TYPE, message.getKey().getId());
      messageNode.setProperty(NTF_OWNER_PARAMETER, message.getArrayOwnerParameter());
      messageNode.setProperty(NTF_SEND_TO_DAILY, message.getSendToDaily());
      messageNode.setProperty(NTF_SEND_TO_WEEKLY, message.getSendToWeekly());
      //
      messageHomeNode.getSession().save();
    } catch (Exception e) {
      LOG.error("Failed to save the NotificationMessage", e);
    } finally {
      localLock.unlock();
    }
  }
  
  private NotificationInfo fillModel(Node node) throws Exception {
    if(node == null) return null;
    NotificationInfo message = NotificationInfo.instance()
      .setFrom(node.getProperty(NTF_FROM).getString())
      .setOrder(Integer.valueOf(node.getProperty(NTF_ORDER).getString()))
      .key(node.getProperty(NTF_PROVIDER_TYPE).getString())
      .setOwnerParameter(NotificationUtils.valuesToArray(node.getProperty(NTF_OWNER_PARAMETER).getValues()))
      .setSendToDaily(NotificationUtils.valuesToArray(node.getProperty(NTF_SEND_TO_DAILY).getValues()))
      .setSendToWeekly(NotificationUtils.valuesToArray(node.getProperty(NTF_SEND_TO_WEEKLY).getValues()))
      .setId(node.getUUID())
      .setName(node.getName());

    return message;
  }

  public NotificationInfo get(String id) throws Exception {
    SessionProvider sProvider = NotificationSessionManager.createSystemProvider();
    try {
      Node notificationNode = getSession(sProvider, workspace).getNodeByUUID(id);
      return fillModel(notificationNode);
    } catch (Exception e) {
      LOG.error("Failed to get the NotificationInfo", e);
    }
    return null;
  }

  public void remove(String id) throws Exception {
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    SessionProvider sProvider = NotificationSessionManager.createSystemProvider();
    try {
      Session session = getSession(sProvider, workspace);
      Node notificationNode = session.getNodeByUUID(id);
      notificationNode.remove();
      session.save();

      // record entity update here
      if (stats) {
        NotificationContextFactory.getInstance().getStatisticsCollector().deleteEntity(NTF_MESSAGE);
      }
    } catch (Exception e) {
      LOG.error("Failed to get the NotificationInfo", e);
    }
  }

  public TreeNode getByUser(UserSetting setting) {
    SessionProvider sProvider = NotificationSessionManager.createSystemProvider();
    
    TreeNode treeNode = new TreeNode(setting.getUserId());
    try {

      boolean isWeekly = configuration.isSendWeekly();

      List<String> plugins = (isWeekly) ? setting.getWeeklyProviders() : setting.getDailyProviders();

      treeNode.intPluginNodes(plugins);

      for (String pluginId : plugins) {
        addNotifications(sProvider, treeNode, pluginId, isWeekly);
      }

    } catch (Exception e) {
      LOG.error("Failed to get the NotificationMessage by user: " + setting.getUserId(), e);
    }
    return treeNode;
  }
  
  private Node getParentNodeByThreadLocal(String pluginId) {
    if (parentNodeMap.get() == null) {
      resetParentNodeMap();
      return null;
    }
    return parentNodeMap.get().get(pluginId);
  }
  
  public Node getDaily(SessionProvider sProvider, String pluginId) throws Exception {
    Node node = getParentNodeByThreadLocal(pluginId);
    if (node == null || node.getSession() == null || !node.getSession().isLive()) {
      // In the case session what hold by Node has been NULL or isLive is FALSE
      // must re-load Node from JCR
      node = getOrCreateMessageParent(sProvider, workspace, pluginId);
      parentNodeMap.get().put(pluginId, node);
    }

    return node;
  }

  public Node getWeekly(SessionProvider sProvider, String pluginId) throws Exception {
    Node node = getParentNodeByThreadLocal(pluginId);
    // In the case session what hold by Node has been NULL or isLive is FALSE
    // must re-load Node from JCR
    if (node == null || node.getSession() == null || !node.getSession().isLive()) {
      node = getMessageNodeByPluginId(sProvider, workspace, pluginId);
      parentNodeMap.get().put(pluginId, node);
    }

    return node;
  }
  
  private void addNotifications(SessionProvider sProvider, TreeNode treeNode, String pluginId, boolean isWeekly) throws Exception {
    Node messageHomeNode = (isWeekly) ? getWeekly(sProvider, pluginId) : getDaily(sProvider, pluginId);
    NotificationService notificationService = CommonsUtils.getService(NotificationService.class);
    //
    NodeIterator iter = getNotifications(messageHomeNode, isWeekly, treeNode.getUserName());
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      treeNode.add(new NotificationKey(pluginId), new NTFInforkey(node.getUUID()));
      //
      notificationService.addEvent(NTFEvent.createNTFEvent(treeNode.getUserName(), new NTFInforkey(node.getUUID()), isWeekly));
    }
  }

  private NodeIterator getNotifications(Node messageHomeNode, boolean isWeekly, String userName) throws Exception {
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    long startTime = 0;
    if (stats) {
      startTime = System.currentTimeMillis();
    }

    String strQuery = NotificationUtils.buildQueryNotification(messageHomeNode.getPath(), userName, isWeekly);

    QueryManager qm = messageHomeNode.getSession().getWorkspace().getQueryManager();
    Query query = qm.createQuery(strQuery.toString(), Query.SQL);
    NodeIterator it = query.execute().getNodes();

    // record statistics insert entity
    if (stats) {
      NotificationContextFactory.getInstance().getStatisticsCollector().queryExecuted(strQuery.toString(), it.getSize(), System.currentTimeMillis() - startTime);
    }
    return it;
  }

  private NodeIterator getNotificationsByQuery(Node messageHomeNode, boolean isWeekly, String userName) throws Exception {
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    long startTime = 0;
    if (stats) {
      startTime = System.currentTimeMillis();
    }
    
    String strQuery = NotificationUtils.buildQueryNotification(messageHomeNode.getPath(), userName, isWeekly);
    
    QueryManager qm = messageHomeNode.getSession().getWorkspace().getQueryManager();
    Query query = qm.createQuery(strQuery.toString(), Query.SQL);
    NodeIterator it = query.execute().getNodes();
    
    // record statistics insert entity
    if (stats) {
      NotificationContextFactory.getInstance().getStatisticsCollector().queryExecuted(strQuery.toString(), it.getSize(), System.currentTimeMillis() - startTime);
    }
    return it;
  }
  

}
