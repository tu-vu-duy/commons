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
package org.exoplatform.commons.notification.impl;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public abstract class AbstractService {

  private static final Log LOG = ExoLogger.getLogger(AbstractService.class);

  public static final String STG_SCOPE                = "stg:scope";

  public static final String STG_CONTEXT              = "stg:context";

  public static final String STG_SUBCONTEXT           = "stg:subcontext";

  public static final String STG_SIMPLE_CONTEXT       = "stg:simplecontext";

  public static final String EXO_IS_ACTIVE            = "exo:isActive";

  public static final String EXO_INSTANTLY            = "exo:instantly";
  
  public static final String EXO_DAILY                = "exo:daily";

  public static final String EXO_WEEKLY               = "exo:weekly";

  public static final String NTF_TO                   = "ntf:to";

  public static final String NTF_FROM                 = "ntf:from";

  public static final String NTF_TEXT                 = "ntf:text";

  public static final String NTF_READ                 = "ntf:read";

  public static final String NTF_ORDER                = "ntf:order";

  public static final String NTF_SENDER               = "ntf:sender";

  public static final String NTF_OWNER                = "ntf:owner";

  public static final String NTF_CHANNEL              = "ntf:channel";

  public static final String NTF_MESSAGE              = "ntf:message";

  public static final String NTF_PARENT_ID            = "ntf:parentId";

  public static final String NTF_PLUGIN_ID            = "ntf:pluginId";

  public static final String NTF_NOTIF_USER           = "ntf:notifUser";

  public static final String NTF_NOTIF_DATE           = "ntf:notifDate";

  public static final String NTF_NOTIF_INFO           = "ntf:notifInfo";

  public static final String NTF_SHOW_POPOVER         = "ntf:showPopover";

  public static final String NTF_MESSAGE_INFO         = "ntf:messageInfo";

  public static final String NTF_SEND_TO_DAILY        = "ntf:sendToDaily";

  public static final String NTF_MESSAGE_HOME         = "ntf:messageHome";

  public static final String NTF_SEND_TO_WEEKLY       = "ntf:sendToWeekly";

  public static final String NTF_NOTIFICATION         = "ntf:notification";

  public static final String NTF_PROVIDER_TYPE        = "ntf:providerType";

  public static final String NTF_SEND_TO_MONTHLY      = "ntf:sendToMonthly";

  public static final String NTF_LAST_MODIFIED_DATE   = "ntf:lastModifiedDate";

  public static final String MIX_DEFAULT_SETTING      = "mix:defaultSetting";

  public static final String MIX_SUB_MESSAGE_HOME     = "mix:subMessageHome";

  public static final String NTF_OWNER_PARAMETER      = "ntf:ownerParameter";

  public static final String NTF_MESSAGE_INFO_HOME    = "ntf:messageInfoHome";

  public static final String EXO_DATE_CREATED         = "exo:dateCreated";

  public static final String EXO_LAST_MODIFIED_DATE   = "exo:lastModifiedDate";

  public static final String JCR_ROOT                 = "/jcr:root";

  public static final String ASCENDING                = " ASC";

  public static final String DESCENDING               = " DESC";

  public static final String WORKSPACE_PARAM          = "workspace";

  public static final String DEFAULT_WORKSPACE_NAME   = "portal-system";

  public static final String NOTIFICATION_HOME_NODE   = "eXoNotification";
  
  public static final String MESSAGE_HOME_NODE        = "messageHome";
  
  public static final String MESSAGE_INFO_HOME_NODE   = "messageInfoHome";

  public static final String WEB_CHANNEL              = "web";

  public static final String SETTING_NODE             = "settings";

  public static final String SETTING_USER_NODE        = "user";

  public static final String SETTING_USER_PATH        = "settings/user";

  public static final String NOTIFICATION_PARENT_PATH = "/";

  public static final String VALUE_PATTERN            = "{VALUE}";

  public static final String DATE_NODE_PATTERN        = "yyyyMMdd";

  public static final String NTF_NAME_SPACE           = "ntf:";

  /** Defines the prefix of the parent message node such as d20 */
  public static final String DAY                      = "d";

  protected static Node getNotificationHomeNode(SessionProvider sProvider, String workspace) throws Exception {
    Session session = getSession(sProvider, workspace);
    Node notificationHome, rootNode = session.getRootNode();
    if (rootNode.hasNode(NOTIFICATION_HOME_NODE)) {
      notificationHome = rootNode.getNode(NOTIFICATION_HOME_NODE);
    } else {
      notificationHome = rootNode.addNode(NOTIFICATION_HOME_NODE, NTF_NOTIFICATION);
      session.save();
    }
    return notificationHome;
  }

  /**
   * Get the home node of MessageInfo node
   * 
   * @param sProvider
   * @param workspace
   * @return
   * @throws Exception
   */
  protected Node getMessageInfoHomeNode(SessionProvider sProvider, String workspace) throws Exception {
    Node ntfHomeNode = getNotificationHomeNode(sProvider, workspace);
    if (ntfHomeNode.hasNode(MESSAGE_INFO_HOME_NODE) == false) {
      Node messageHome = ntfHomeNode.addNode(MESSAGE_INFO_HOME_NODE, NTF_MESSAGE_INFO_HOME);
      sessionSave(ntfHomeNode);
      return messageHome;
    }
    return ntfHomeNode.getNode(MESSAGE_INFO_HOME_NODE);
  }

  /**
   * Makes the node path for MessageHome node
   * "/eXoNotification/messageHome/<pluginId>/<DAY_OF_MONTH>/<HOUR_OF_DAY>/"
   * 
   * @param sProvider
   * @param pluginId
   * @return
   * @throws Exception
   */
  protected Node getOrCreateMessageParent(SessionProvider sProvider, String workspace, String pluginId) throws Exception {
    Node providerNode = getMessageNodeByPluginId(sProvider, workspace, pluginId);
    String dayName = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
    Node dayNode = getOrCreateMessageNode(providerNode, DAY + dayName);
    return dayNode;
  }
  
 

  /**
   * Makes the node path for MessageHome node
   * 
   * @param sProvider
   * @param workspace
   * @param pluginId
   * @return
   * @throws Exception
   */
  protected Node getMessageNodeByPluginId(SessionProvider sProvider, String workspace, String pluginId) throws Exception {
    Node root = getNotificationHomeNode(sProvider, workspace);
    // rootPath = "/eXoNotification/messageHome/"
    Node messageHome = getOrCreateMessageNode(root, MESSAGE_HOME_NODE);
    // pluginPath = /eXoNotification/messageHome/<pluginId>/
    return getOrCreateMessageNode(messageHome, pluginId);
  }
  
  /**
   * 
   * @param parent
   * @param nodeName
   * @return
   * @throws Exception
   */
  protected Node getOrCreateMessageNode(Node parent, String nodeName) throws Exception {
    if (parent.hasNode(nodeName) == false) {
      Node messageHome = parent.addNode(nodeName, NTF_MESSAGE_HOME);
      messageHome.addMixin(MIX_SUB_MESSAGE_HOME);
      sessionSave(messageHome);
      return messageHome;
    }
    return parent.getNode(nodeName);
  }

  public static Session getSession(SessionProvider sProvider, String workspace) {
    if (workspace == null || workspace.length() == 0) {
      workspace = DEFAULT_WORKSPACE_NAME;
    }
    try {
      return sProvider.getSession(workspace, CommonsUtils.getRepository());
    } catch (RepositoryException e) {
      LOG.error("Failed to get session." + e);
      try {
        return NotificationSessionManager.createSystemProvider().getSession(workspace, CommonsUtils.getRepository());
      } catch (Exception e2) {
        return null;
      }
    }
  }

  public static String getCurrentTenantName() {
    try {
      return CommonsUtils.getRepository().getConfiguration().getName();
    } catch (Exception e) {
      return "defaultTenantName";
    }
  }

  protected static void sessionSave(Node node) throws Exception {
    node.getSession().save();
  }
  
  protected static String getValue(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    return VALUE_PATTERN.replace("VALUE", value);
  }

  protected static String getValues(String values) {
    if (values == null || values.isEmpty()) {
      return "";
    }
    return values.replace("{", "").replace("}", "");
  }
  
}
