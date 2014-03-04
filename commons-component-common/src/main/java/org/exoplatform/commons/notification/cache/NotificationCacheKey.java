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
package org.exoplatform.commons.notification.cache;

import org.exoplatform.commons.api.notification.node.NTFInforkey;

public class NotificationCacheKey extends ScopeCacheKey {
  private static final long     serialVersionUID = 1L;

  private final NTFInforkey key;

  public NotificationCacheKey(NTFInforkey key) {
    this.key = key;
  }

  public NTFInforkey getInfoKey() {
    return key;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof NotificationCacheKey))
      return false;
    if (!super.equals(o))
      return false;

    NotificationCacheKey that = (NotificationCacheKey) o;
    return that.getInfoKey().equals(this.getInfoKey());
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (key != null ? key.hashCode() : 0);
    return result;
  }

}
