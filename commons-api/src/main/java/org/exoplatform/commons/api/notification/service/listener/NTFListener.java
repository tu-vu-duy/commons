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

import org.exoplatform.commons.api.event.EventListener;
import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.services.listener.Event;

public abstract class NTFListener implements EventListener<String, NTFInforkey> {
  
  public NTFListener() {
  }
  
  @Override
  public void create(Event<String, NTFInforkey> event) {
    addEvent(event);
  }

  @Override
  public void update(Event<String, NTFInforkey> event) {
  }

  @Override
  public void remove(Event<String, NTFInforkey> event) {
  }

  public abstract void addEvent(Event<String, NTFInforkey> event);
  public abstract void processEvents(NTFEvent.NAME eventType);

}
