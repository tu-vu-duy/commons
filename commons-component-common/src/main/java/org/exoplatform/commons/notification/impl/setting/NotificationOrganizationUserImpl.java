package org.exoplatform.commons.notification.impl.setting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.commons.api.notification.service.setting.NotificationOrganizationUser;
import org.exoplatform.commons.notification.impl.NotificationSessionManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.impl.UserImpl;

public class NotificationOrganizationUserImpl implements NotificationOrganizationUser {
  private static final Log         LOG              = ExoLogger.getLogger(NotificationOrganizationUserImpl.class);
  private static final String SOCIAL_WORKSPACE   = "social";
  private final RepositoryService repositoryService;
  private final UserHandler userHandler;
  private final String SOC_USER_PATH = "/production/soc:providers/soc:organization/";

  public NotificationOrganizationUserImpl(RepositoryService repositoryService, OrganizationService organizationService) {
    this.repositoryService = repositoryService;
    this.userHandler = organizationService.getUserHandler();
  }

  @Override
  public List<User> getAllUsers() {
    List<User> users = new ArrayList<User>();
    boolean created = NotificationSessionManager.createSystemProvider();
    SessionProvider sProvider = NotificationSessionManager.getSessionProvider();
    try {
      Node userRootNode = (Node) getSession(sProvider).getItem(SOC_USER_PATH);
      NodeIterator userNodes = userRootNode.getNodes();
      //
      while (userNodes.hasNext()) {
        Node userNode = userNodes.nextNode();
        String userId = userNode.getName();
        if (!userId.equals("userProfileDeleted")) {
          UserImpl user = (UserImpl) userHandler.createUserInstance(userId);
          user.setCreatedDate(calendar(userNode, "exo:dateCreated", Calendar.getInstance()).getTime());
//          user.setEnabled(bool(userNode, "exo:isDisabled", true));
          users.add(user);
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to get all users on system", e);
    } finally {
      NotificationSessionManager.closeSessionProvider(created);
    }
    return users;
  }

  public Session getSession(SessionProvider sessionProvider) {
    Session session = null;
    try {
      session = sessionProvider.getSession(SOCIAL_WORKSPACE, repositoryService.getCurrentRepository());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return session;
  }

  private Boolean bool(Node node, String name, boolean defaultValue) {
    try {
      return node.getProperty(name).getBoolean();
    } catch (Exception e) {
      return defaultValue;
    }
  }
  
  private Calendar calendar(Node node, String name, Calendar defaultValue) {
    try {
      return node.getProperty(name).getDate();
    } catch (Exception e) {
      return defaultValue;
    }
  }
}
