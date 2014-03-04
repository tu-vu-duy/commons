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

import java.io.Serializable;
import java.util.List;

import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.notification.cache.CacheLoader;
import org.exoplatform.commons.notification.cache.ListSettingCacheKey;
import org.exoplatform.commons.notification.cache.ScopeCacheKey;
import org.exoplatform.commons.notification.cache.ServiceContext;
import org.exoplatform.commons.notification.impl.setting.UserSettingServiceImpl;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.future.FutureExoCache;
import org.exoplatform.services.organization.User;
import org.picocontainer.Startable;

public class UserSettingCacheService implements UserSettingService, Startable {

  private UserSettingServiceImpl serviceImpl;
  private CacheService cacheService;
  private ExoCache<ScopeCacheKey, UserSetting> settingData;
  private ExoCache<ScopeCacheKey, ListUserSetting> settingList;
  private FutureExoCache<ScopeCacheKey, UserSetting, ServiceContext<UserSetting>> settingDataFuture;
  private FutureExoCache<ScopeCacheKey, ListUserSetting, ServiceContext<ListUserSetting>> settingListFuture;

  
  public UserSettingCacheService(CacheService service, UserSettingServiceImpl serviceImpl) {
    this.serviceImpl = serviceImpl;
    this.cacheService = service;
  }

  public enum CacheType {
    USER_SETTING_DATA("commons.notification.user.SettingData"),
    USER_SETTING_LIST("commons.notification.user.SettingList");

    private final String name;

    private CacheType(final String name) {
      this.name = name;
    }

    public <K extends ScopeCacheKey, V extends Serializable> ExoCache<K, V> getFromService(CacheService service) {
      return service.getCacheInstance(name);
    }

    public <K extends ScopeCacheKey, V extends Serializable> FutureExoCache<K, V, ServiceContext<V>> createFutureCache(ExoCache<K, V> cache) {
      return new FutureExoCache<K, V, ServiceContext<V>>(new CacheLoader<K, V>(), cache);
    }
  }
  
  @Override
  public void start() {
    this.settingData = CacheType.USER_SETTING_DATA.getFromService(cacheService);
    this.settingList = CacheType.USER_SETTING_LIST.getFromService(cacheService);

    this.settingDataFuture = CacheType.USER_SETTING_DATA.createFutureCache(settingData);
    this.settingListFuture = CacheType.USER_SETTING_LIST.createFutureCache(settingList);
  }

  @Override
  public void stop() {
  }

  @Override
  public void save(UserSetting model) {
    serviceImpl.save(model);
    //
    settingData.clearCache();
    settingList.clearCache();
  }

  @Override
  public UserSetting get(final String userId) {
    ScopeCacheKey key = new ScopeCacheKey(userId);
    return settingDataFuture.get(
      new ServiceContext<UserSetting>() {
        @Override
        public UserSetting execute() {
          return serviceImpl.get(userId);
        }
    }, key);
  }

  @Override
  public List<UserSetting> getDaily(final int offset, final int limit) {
    ListSettingCacheKey key = new ListSettingCacheKey("SETTING", offset, limit);
    return settingListFuture.get(new ServiceContext<ListUserSetting>() {
      @Override
      public ListUserSetting execute() {
        return new ListUserSetting(serviceImpl.getDaily(offset, limit), false);
      }
    }, key).build();
  }

  @Override
  public long getNumberOfDaily() {
    return serviceImpl.getNumberOfDaily();
  }

  @Override
  public List<UserSetting> getDefaultDaily(final int offset, final int limit) {
    ListSettingCacheKey key = new ListSettingCacheKey("DEFAULT_SETTING", offset, limit);
    return settingListFuture.get(new ServiceContext<ListUserSetting>() {
      @Override
      public ListUserSetting execute() {
        return new ListUserSetting(serviceImpl.getDefaultDaily(offset, limit), true);
      }
    }, key).build();
  }

  @Override
  public long getNumberOfDefaultDaily() {

    return serviceImpl.getNumberOfDefaultDaily();
  }

  @Override
  public List<String> getUserSettingByPlugin(final String pluginId) {
    ListSettingCacheKey key = new ListSettingCacheKey(pluginId, 0, 0);
    return settingListFuture.get(new ServiceContext<ListUserSetting>() {
      @Override
      public ListUserSetting execute() {
        ListUserSetting listUserSetting = new ListUserSetting();
        listUserSetting.setData(serviceImpl.getUserSettingByPlugin(pluginId));
        return listUserSetting;
      }
    }, key).getData();
  }

  @Override
  public void addMixin(String userId) {
    serviceImpl.addMixin(userId);
  }

  @Override
  public void addMixin(User[] users) {
    serviceImpl.addMixin(users);
  }

}
