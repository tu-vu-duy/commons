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
package org.exoplatform.commons.notification.cache;

public class ListSettingCacheKey extends ScopeCacheKey {
  private static final long serialVersionUID = 1L;

  private final int         limit;
  private final int         offset;

  public ListSettingCacheKey(String key, int offset, int limit) {
    super(key);
    this.limit = limit;
    this.offset = offset;
  }
  
  public int getLimit() {
    return limit;
  }
  public int getOffset() {
    return offset;
  }
  
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ListSettingCacheKey)) {
      return false;
    }

    ListSettingCacheKey that = (ListSettingCacheKey) o;

    if (limit != that.getLimit()) {
      return false;
    }

    if (offset != that.getOffset()) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int c = super.hashCode();
    c += 31 * c + limit;
    c += 31 * c + offset;
    return c;
  }
  
}
