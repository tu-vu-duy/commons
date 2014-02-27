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
package org.exoplatform.commons.api.notification.node;

public class NTFInforkey {
  private String UUID;
  private String pluginId;

  public NTFInforkey(String UUID) {
    this.UUID = UUID;
  }

  public String getUUID() {
    return this.UUID;
  }

  public String getPluginId() {
    return pluginId;
  }

  public NTFInforkey setPluginId(String pluginId) {
    this.pluginId = pluginId;
    return this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NTFInforkey)) {
      return false;
    }

    NTFInforkey that = (NTFInforkey) o;

    if (UUID != null ? !UUID.equals(that.UUID) : that.UUID != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result += (UUID != null ? UUID.hashCode() : 0);
    return result;
  }
}
