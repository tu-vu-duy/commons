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
package org.exoplatform.commons.notification.impl.log;

import org.exoplatform.services.mail.Message;

public abstract class AuditEmailMessageLog<T extends FormatLog> {
  private StringBuilder logDatas = new StringBuilder();

  protected T format;

  public AuditEmailMessageLog() {
  }

  /**
   * Gets the result of email message log.
   * 
   * @return
   */
  public String log() {
    return toString();
  }

  /**
   * Clear the result of email message log
   */
  public void clear() {
    logDatas = new StringBuilder();
  }

  /**
   * Put the email message to log.
   * 
   * @param pluginId: the pluginId
   * @param message the email message
   */
  public void put(String pluginId, Message message) {
    logDatas.append(format.with(pluginId, message).build());
  }

  @Override
  public String toString() {
    return logDatas.toString();
  }
}
