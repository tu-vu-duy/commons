/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.model.UserSetting;
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
  private static final Log         LOG              = ExoLogger.getLogger(NotificationDataStorageImpl.class);

  public static final String       REMOVE_ALL       = "removeAll";

  private String                    workspace;

  private NotificationConfiguration configuration    = null;
  
  private final ReentrantLock lock = new ReentrantLock();

  private Map<String, Set<String>>  removeByCallBack = new ConcurrentHashMap<String, Set<String>>();
  
  private ThreadLocal<Map<String, Node>> parentNodeMap = new ThreadLocal<Map<String,Node>>();

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

  private Node getParentNodeByThreadLocal(String pluginId) {
    if (parentNodeMap.get() == null) {
      resetParentNodeMap();
      return null;
    }
    return parentNodeMap.get().get(pluginId);
  }
  
  @Override
  public void save(NotificationInfo message) throws Exception {
    SessionProvider sProvider = CommonsUtils.getSystemSessionProvider();
    final ReentrantLock localLock = lock;
    try {
      localLock.lock();
      Node messageHomeNode = getOrCreateMessageParent(sProvider, workspace, message.getKey().getId());
      Node messageNode = messageHomeNode.addNode(message.getId(), NTF_MESSAGE);
      messageNode.setProperty(NTF_FROM, message.getFrom());
      messageNode.setProperty(NTF_ORDER, message.getOrder());
      messageNode.setProperty(NTF_PROVIDER_TYPE, message.getKey().getId());
      messageNode.setProperty(NTF_OWNER_PARAMETER, message.getArrayOwnerParameter());
      messageNode.setProperty(NTF_SEND_TO_DAILY, message.getSendToDaily());
      messageNode.setProperty(NTF_SEND_TO_WEEKLY, message.getSendToWeekly());
      messageHomeNode.getSession().save();
      
      //record statistics insert entity
      if (NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled()) {
        NotificationContextFactory.getInstance().getStatisticsCollector().insertEntity(NTF_MESSAGE);
      }
      
    } catch (Exception e) {
      LOG.error("Failed to save the NotificationMessage", e);
    } finally {
      localLock.unlock();
    }
  }

  @Override
  public Map<NotificationKey, List<NotificationInfo>> getByUser(UserSetting setting) {
    SessionProvider sProvider = NotificationSessionManager.createSystemProvider();
    Map<NotificationKey, List<NotificationInfo>> notificationData = new LinkedHashMap<NotificationKey, List<NotificationInfo>>();
    try {

      if (configuration.isSendWeekly() == false) {
        // for daily
        for (String pluginId : setting.getDailyProviders()) {
          putMap(notificationData, NotificationKey.key(pluginId), getDailyNotification(sProvider, pluginId, setting.getUserId()));
        }
      } else {
        // for weekly
        for (String pluginId : setting.getWeeklyProviders()) {
          putMap(notificationData, NotificationKey.key(pluginId), getWeeklyNotification(sProvider, pluginId, setting.getUserId()));
        }
      }

    } catch (Exception e) {
      LOG.error("Failed to get the NotificationMessage by user: " + setting.getUserId(), e);
    }

    return notificationData;
  }

  private static void putMap(Map<NotificationKey, List<NotificationInfo>> notificationData, NotificationKey key, List<NotificationInfo> values) {
    if (notificationData.containsKey(key)) {
      List<NotificationInfo> messages = notificationData.get(key);
      for (NotificationInfo notificationMessage : values) {
        if (messages.size() == 0 || messages.contains(notificationMessage) == false) {
          messages.add(notificationMessage);
        }
      }
      //
      if(messages.size() > 0 ) {
        notificationData.put(key, messages);
      }
    } else if (values.size() > 0) {
      notificationData.put(key, values);
    }
  }

  private Node getDaily(SessionProvider sProvider, String pluginId) throws Exception {
    Node node = getParentNodeByThreadLocal(pluginId);
    if (node == null || node.getSession() == null || !node.getSession().isLive()) {
      // In the case session what hold by Node has been NULL or isLive is FALSE
      // must re-load Node from JCR
      node = getOrCreateMessageParent(sProvider, workspace, pluginId);
      parentNodeMap.get().put(pluginId, node);
    }

    return node;
  }

  private Node getWeekly(SessionProvider sProvider, String pluginId) throws Exception {
    Node node = getParentNodeByThreadLocal(pluginId);
    // In the case session what hold by Node has been NULL or isLive is FALSE
    // must re-load Node from JCR
    if (node == null || node.getSession() == null || !node.getSession().isLive()) {
      node = getMessageNodeByPluginId(sProvider, workspace, pluginId);
      parentNodeMap.get().put(pluginId, node);
    }

    return node;
  }

  private List<NotificationInfo> getNotificationMessages(Node messageHomeNode, String pluginId,
                                                            boolean isDaily, String userId) throws Exception {
    List<NotificationInfo> messages = new ArrayList<NotificationInfo>();
    NodeIterator iter = getNotifications(messageHomeNode, isDaily, userId);
    Session session = messageHomeNode.getSession();
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      NotificationInfo model = fillModel(node);
      messages.add(model.setTo(userId));
      processRemove(session, model, isDaily, node.getPath());
    }
    return messages;
  }
  
  
  private List<NotificationInfo> getDailyNotification(SessionProvider sProvider, String pluginId,
                                                          String userId) throws Exception {
    
    return getNotificationMessages(getDaily(sProvider, pluginId), pluginId, true, userId);
  }

  private List<NotificationInfo> getWeeklyNotification(SessionProvider sProvider, String pluginId,
                                                          String userId) throws Exception {
    
    return getNotificationMessages(getWeekly(sProvider, pluginId), pluginId, false, userId);
  }

  private NodeIterator getNotifications(Node messageHomeNode, boolean isDaily, String userId) throws Exception {
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    long startTime = 0;
    if ( stats ) startTime = System.currentTimeMillis();
    
    String parentPath = messageHomeNode.getPath();
    StringBuilder strQuery = new StringBuilder("SELECT * FROM ").append(NTF_MESSAGE).append(" WHERE ");
    
    //ntf:sendToWeekly: ['&forAllUser'] OR ['demo', 'mary', 'john'] 
    //ntf:sendToDaily: ['&forAllUser'] OR ['demo', 'mary', 'john'] 
    //builds filter UserId to send daily or weekly
    //for example: in case daily >> ntf:sendToDaily='demo'
    //             in case weekly >> ntf:sendToWeekly='demo'
    if (isDaily) {
      strQuery.append(" (jcr:path LIKE '").append(parentPath).append("/%'")
              .append(" AND NOT jcr:path LIKE '").append(parentPath).append("/%/%')");
      strQuery.append(" AND (").append(NTF_SEND_TO_DAILY).append("='").append(userId).append("'");
    } else {
      strQuery.append(" jcr:path LIKE '").append(parentPath).append("/%'");
      strQuery.append(" AND (").append(NTF_SEND_TO_WEEKLY).append("='").append(userId).append("'");
    }

    if(!NotificationInfo.FOR_ALL_USER.equals(userId)) {
      strQuery.append(" OR ")
              .append((isDaily) ? NTF_SEND_TO_DAILY : NTF_SEND_TO_WEEKLY)
              .append("='")
              .append(NotificationInfo.FOR_ALL_USER)
              .append("') AND ")
              .append(NTF_FROM).append("<>'").append(userId).append("'");
      strQuery.append(" order by ").append(NTF_ORDER).append(ASCENDING).append(", exo:dateCreated").append(DESCENDING);
    } else {
      strQuery.append(")");
    }

    QueryManager qm = messageHomeNode.getSession().getWorkspace().getQueryManager();
    Query query = qm.createQuery(strQuery.toString(), Query.SQL);
    NodeIterator it = query.execute().getNodes();
    
    //record statistics insert entity
    if (stats) {
      NotificationContextFactory.getInstance().getStatisticsCollector().queryExecuted(strQuery.toString(), it.getSize(), System.currentTimeMillis() - startTime);
    }
    return it;
  }

  private NodeIterator getNotificationsForWeeklySendAllUser(Node messageHomeNode) throws Exception {
    
    String parentPath = messageHomeNode.getPath();
    StringBuilder strQuery = new StringBuilder("SELECT * FROM ").append(NTF_MESSAGE).append(" WHERE ");

    strQuery.append(" (jcr:path LIKE '").append(parentPath).append("/%'")
            .append(" AND (").append(NTF_SEND_TO_DAILY).append("='' OR ").append(NTF_SEND_TO_WEEKLY).append("='").append(NotificationInfo.FOR_ALL_USER).append("')");
    
    QueryManager qm = messageHomeNode.getSession().getWorkspace().getQueryManager();
    Query query = qm.createQuery(strQuery.toString(), Query.SQL);
    NodeIterator it = query.execute().getNodes();

    return it;
  }

  private NodeIterator getNotificationsForDailySendAllUser(Node messageHomeNode) throws Exception {
    
    String parentPath = messageHomeNode.getPath();
    StringBuilder strQuery = new StringBuilder("SELECT * FROM ").append(NTF_MESSAGE).append(" WHERE ");
    
    strQuery.append(" (jcr:path LIKE '").append(parentPath).append("/%'")
            .append(" AND (").append(NTF_SEND_TO_DAILY).append("='").append(NotificationInfo.FOR_ALL_USER).append("')");
    
    QueryManager qm = messageHomeNode.getSession().getWorkspace().getQueryManager();
    Query query = qm.createQuery(strQuery.toString(), Query.SQL);
    NodeIterator it = query.execute().getNodes();
    
    return it;
  }

  private NotificationInfo fillModel(Node node) throws Exception {
    if(node == null) return null;
    NotificationInfo message = NotificationInfo.instance()
      .setFrom(node.getProperty(NTF_FROM).getString())
      .setOrder(Integer.valueOf(node.getProperty(NTF_ORDER).getString()))
      .key(node.getProperty(NTF_PROVIDER_TYPE).getString())
      .setOwnerParameter(node.getProperty(NTF_OWNER_PARAMETER).getValues())
      .setSendToDaily(NotificationUtils.valuesToArray(node.getProperty(NTF_SEND_TO_DAILY).getValues()))
      .setSendToWeekly(NotificationUtils.valuesToArray(node.getProperty(NTF_SEND_TO_WEEKLY).getValues()))
      .setId(node.getName());

    return message;
  }

  private Set<String> addValue(String key, String value) {
    Set<String> set = removeByCallBack.get(key);
    if (set == null) {
      set = new HashSet<String>();
    }
    set.add(value);
    return set;
  }

  private void processRemove(Session session, NotificationInfo message, 
                                boolean isDaily, String path) throws Exception {
    boolean isRemove = false;
    if (isDaily && message.getSendToDaily().length == 1) {
      isRemove = (message.getSendToWeekly().length == 0);
    }
    if (isRemove == false && !isDaily && message.getSendToWeekly().length == 1) {
      isRemove = (message.getSendToDaily().length == 0);
    }
    //
    if (isRemove) {
      removeByCallBack.put(REMOVE_ALL, addValue(REMOVE_ALL, path));
    } else {
      String removeRemoteId = message.getTo();
      removeUserInSendList(session, path, (isDaily) ? NTF_SEND_TO_DAILY : NTF_SEND_TO_WEEKLY, removeRemoteId);
    }
  }

  private void removeUserInSendList(Session session, String path, String property, String value) {
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    
    try {
      Node node = (Node) session.getItem(path);
      List<String> values = NotificationUtils.valuesToList(node.getProperty(property).getValues());
      if (values.contains(value)) {
        values.remove(value);
        if (values.isEmpty()) {
          values.add("");
        }
        node.setProperty(property, values.toArray(new String[values.size()]));
        node.save();
        
        //record entity update here
        if (stats) {
          NotificationContextFactory.getInstance().getStatisticsCollector().updateEntity(NTF_MESSAGE);
        }
      }
    } catch (Exception e) {
      LOG.warn(String.format("Failed to remove value %s on property %s of node", value, property));
      LOG.debug(e.getMessage(), e);
    }
  }

  @Override
  public void removeMessageAfterSent() throws Exception {
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    
    SessionProvider sProvider = NotificationSessionManager.createSystemProvider();
    try {
      Node notificationHome = getNotificationHomeNode(sProvider, workspace);
      Session session = notificationHome.getSession();
      // remove all
      Set<String> listPaths = removeByCallBack.get(REMOVE_ALL);
      removeByCallBack.remove(REMOVE_ALL);
      if (listPaths != null && listPaths.size() > 0) {
        for (String nodePath : listPaths) {
          try {
            session.getItem(nodePath).remove();
            
            //record entity delete here
            if (stats) {
              NotificationContextFactory.getInstance().getStatisticsCollector().deleteEntity(NTF_MESSAGE);
            }
            
            LOG.debug("Remove NotificationMessage " + nodePath);
          } catch (Exception e) {
            LOG.warn("Failed to remove node of NotificationMessage " + nodePath + "\n" + e.getMessage());
          }
        }
        session.save();
      }
      
      // remove node weekly for case send all.
      Node messageHomeNode = notificationHome.getNode(MESSAGE_HOME_NODE);
      if (configuration.isSendWeekly()) {
        NodeIterator iterator = getNotificationsForWeeklySendAllUser(messageHomeNode);
        String nodePath;
        while (iterator.hasNext()) {
          Node node = iterator.nextNode();
          nodePath = node.getPath();
          node.remove();

          // record entity delete here
          if (stats) {
            NotificationContextFactory.getInstance().getStatisticsCollector().deleteEntity(NTF_MESSAGE);
          }
          LOG.debug("Remove NotificationMessage " + nodePath);
        }
      } else {
        // remove send all for daily.
        NodeIterator iterator = getNotificationsForDailySendAllUser(messageHomeNode);
        String nodePath;
        while (iterator.hasNext()) {
          Node node = iterator.nextNode();
          nodePath = node.getPath();

          if (node.getProperty(NTF_SEND_TO_WEEKLY).getString().length() > 0) {
            removeUserInSendList(session, nodePath, NTF_SEND_TO_DAILY, NotificationInfo.FOR_ALL_USER);
          } else {
            node.remove();
            // record entity delete here
            if (stats) {
              NotificationContextFactory.getInstance().getStatisticsCollector().deleteEntity(NTF_MESSAGE);
            }
          }
        }
      }
      session.save();
    } catch (Exception e) {
      LOG.warn("Failed to remove message after sent email notification");
      LOG.debug(e.getMessage(), e);
    }
  }

}
