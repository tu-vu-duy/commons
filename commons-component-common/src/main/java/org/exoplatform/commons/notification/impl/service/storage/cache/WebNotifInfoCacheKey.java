/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.exoplatform.commons.notification.impl.service.storage.cache;

import java.io.Serializable;

public class WebNotifInfoCacheKey implements CacheKey, Serializable {
  private static final long serialVersionUID = 1L;
  private String id;

  private WebNotifInfoCacheKey(String id) {
    this.id = id;
  }
  
  public static WebNotifInfoCacheKey key(String id) {
    return new WebNotifInfoCacheKey(id);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    WebNotifInfoCacheKey that = (WebNotifInfoCacheKey) obj;
    if (id != null ? !id.equals(that.id) : that.id != null)
      return false;
    
    return true;
  }

}
