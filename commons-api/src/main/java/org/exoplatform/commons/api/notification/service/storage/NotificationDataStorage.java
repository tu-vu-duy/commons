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
package org.exoplatform.commons.api.notification.service.storage;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.node.TreeNode;


public interface NotificationDataStorage {

  /**
   * Saves information of a notification.
   * 
   * @param notification The notification to be saved.
   * @throws Exception
   */
  void save(NotificationInfo notification) throws Exception;

  /**
   * get information of a notification.
   * 
   * @param id The UUID of notification to be get.
   * @throws Exception
   */
  NotificationInfo get(String id) throws Exception;

  /**
   * Remove a notification.
   * 
   * @param id The UUID of notification to be remove.
   * @throws Exception
   */
  void remove(String id) throws Exception;
  
  TreeNode getByUser(UserSetting setting);

}
