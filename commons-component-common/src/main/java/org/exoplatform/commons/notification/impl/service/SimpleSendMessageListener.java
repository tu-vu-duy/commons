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

import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.service.AbstractSendMessageListener;
import org.exoplatform.commons.notification.NotificationContextFactory;

public class SimpleSendMessageListener extends AbstractSendMessageListener {

  @Override
  public boolean put(MessageInfo message) {
    boolean isSuccessful = getQueueMessageImpl().sendMessage(message.makeEmailNotification());
    final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
    if (stats && isSuccessful) {
      NotificationContextFactory.getInstance().getStatisticsCollector().pollQueue(message.getPluginId());
    }
    return isSuccessful;
  }

  @Override
  public void send() {
    // unsupported this method now
  }
}
