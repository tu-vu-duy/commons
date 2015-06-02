package org.exoplatform.commons.notification.mock;

import java.util.List;

import org.exoplatform.commons.api.notification.service.setting.NotificationOrganizationUser;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

import com.google.caja.util.Lists;

public class MockNotificationOrganizationUser implements NotificationOrganizationUser {
  private UserHandler userHandler;

  public MockNotificationOrganizationUser(OrganizationService organizationService) {
    userHandler = organizationService.getUserHandler();
  }

  @Override
  public List<User> getAllUsers() {
    try {
      ListAccess<User> listAccess = userHandler.findAllUsers();
      return Lists.newArrayList(listAccess.load(0, listAccess.getSize()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Lists.newArrayList();
  }
}
