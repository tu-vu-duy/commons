package org.exoplatform.commons.api.notification.service.setting;

import java.util.List;

import org.exoplatform.services.organization.User;

public interface NotificationOrganizationUser {
  List<User> getAllUsers();
}
