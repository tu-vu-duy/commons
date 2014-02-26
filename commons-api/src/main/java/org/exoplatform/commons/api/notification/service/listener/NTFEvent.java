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
package org.exoplatform.commons.api.notification.service.listener;

import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.services.listener.Event;

public class NTFEvent extends Event<String, NTFInforkey> {
  public enum NAME {
    DAILY, WEEKLY
  }

  public NTFEvent(String name, String userName, NTFInforkey data) {
    super(name, userName, data);
  }

  public static NTFEvent createNTFEvent(String userName, NTFInforkey data, boolean isWeekly) {
    String name = (isWeekly) ? NAME.WEEKLY.toString() : NAME.DAILY.toString();
    return new NTFEvent(name, userName, data);
  }
  
  public boolean isDaily() {
    return NAME.DAILY.toString().equals(getEventName());
  }

  public boolean isWeekly() {
    return NAME.WEEKLY.toString().equals(getEventName());
  }

}
