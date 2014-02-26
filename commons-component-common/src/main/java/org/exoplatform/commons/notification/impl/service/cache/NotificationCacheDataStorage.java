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
package org.exoplatform.commons.notification.impl.service.cache;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.commons.api.notification.node.TreeNode;
import org.exoplatform.commons.api.notification.service.storage.NotificationDataStorage;
import org.exoplatform.commons.notification.cache.NotificationCacheKey;
import org.exoplatform.commons.notification.impl.service.storage.NotificationDataStorageImpl;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.future.FutureExoCache;
import org.exoplatform.services.cache.future.Loader;

public class NotificationCacheDataStorage implements NotificationDataStorage {

  private NotificationDataStorageImpl dataStorage;
  private ExoCache<NotificationCacheKey, NotificationInfo> notificationCache;
  private FutureExoCache<NotificationCacheKey, NotificationInfo, NotificationDataStorageImpl> futureExoCache;
  private final static String CACHING_NAME = "commons.notification.NotificationDataStorage";

  public NotificationCacheDataStorage(CacheService cacheService, NotificationDataStorageImpl dataStorage) {
    this.dataStorage = dataStorage;

    notificationCache = cacheService.getCacheInstance(CACHING_NAME);
    //
    Loader<NotificationCacheKey, NotificationInfo, NotificationDataStorageImpl> loader = new Loader<NotificationCacheKey, NotificationInfo, NotificationDataStorageImpl>() {
      @Override
      public NotificationInfo retrieve(NotificationDataStorageImpl service, NotificationCacheKey key) throws Exception {
        return service.get(key.getKey().getUUID());
      }
    };
    futureExoCache = new FutureExoCache<NotificationCacheKey, NotificationInfo, NotificationDataStorageImpl>(loader, notificationCache);
  }
  
  @Override
  public void save(NotificationInfo notification) throws Exception {
    dataStorage.save(notification);
    //
    notificationCache.remove(new NotificationCacheKey(new NTFInforkey(notification.getId())));
  }

  @Override
  public NotificationInfo get(String id) throws Exception {
    return futureExoCache.get(dataStorage, new NotificationCacheKey(new NTFInforkey(id)));
  }

  @Override
  public void remove(String id) throws Exception {
    dataStorage.remove(id);
    //
    notificationCache.remove(new NotificationCacheKey(new NTFInforkey(id)));
  }

  @Override
  public TreeNode getByUser(UserSetting setting) {
    return dataStorage.getByUser(setting);
  }

}
