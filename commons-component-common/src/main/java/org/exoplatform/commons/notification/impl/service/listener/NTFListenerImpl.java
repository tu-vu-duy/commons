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
package org.exoplatform.commons.notification.impl.service.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.commons.api.notification.service.listener.NTFEvent;
import org.exoplatform.commons.api.notification.service.listener.NTFListener;
import org.exoplatform.commons.api.notification.service.storage.NotificationDataStorage;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NTFListenerImpl extends NTFListener {
  private static final Log           LOG = ExoLogger.getLogger(NTFListenerImpl.class);
  private final ReentrantLock lock = new ReentrantLock();
  private Map<String, List<NTFEvent>> events;

  public NTFListenerImpl() {
    events = new ConcurrentHashMap<String, List<NTFEvent>>();
    //
    events.put(NTFEvent.NAME.DAILY.name(), new ArrayList<NTFEvent>());
    events.put(NTFEvent.NAME.WEEKLY.name(), new ArrayList<NTFEvent>());
  }

  @Override
  public void addEvent(Event<String, NTFInforkey> event) {
    List<NTFEvent> evts = events.get(event.getEventName());
    evts.add((NTFEvent) event);
  }

  @Override
  public void processEvents(NTFEvent.NAME eventType) {
    NotificationDataStorage storage = CommonsUtils.getService(NotificationDataStorage.class);
    final ReentrantLock localLock = lock;
    try {
      localLock.lock();
      List<NTFEvent> evts = events.get(eventType.toString());
      for (NTFEvent event : evts) {
        String uuid = event.getData().getUUID();
        NotificationInfo info = storage.get(uuid);
        if (NTFEvent.NAME.DAILY.equals(eventType)) {
          if (info.getSendToWeekly().length == 0) {
            storage.remove(info.getId());
          } else {
            info.setSendToDaily(new String[] {});
            storage.save(info);
          }
        } else {
          if (info.getSendToDaily().length == 0) {
            storage.remove(info.getId());
          } else {
            info.setSendToWeekly(new String[] {});
            storage.save(info);
          }
        }
      }

    } catch (Exception e) {
      LOG.warn("Failed to update property/remove notifications after sent.");
      LOG.debug(e.getMessage(), e);
    } finally {
      events.get(eventType.toString()).clear();
      localLock.unlock();
    }
  }
  
}
