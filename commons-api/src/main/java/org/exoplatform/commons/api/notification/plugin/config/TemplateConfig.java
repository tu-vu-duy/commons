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
package org.exoplatform.commons.api.notification.plugin.config;


public class TemplateConfig {
  public static final String  DEFAULT_SRC_RESOURCE_BUNDLE_KEY = "locale.notification.template.Notification";
  


  private String              providerId;

  private String              bundlePath;

  public TemplateConfig() {
    bundlePath = DEFAULT_SRC_RESOURCE_BUNDLE_KEY;
  }

  public TemplateConfig(String providerId) {
    this();
    this.providerId = providerId;
  }

  /**
   * @return the providerId
   */
  public String getProviderId() {
    return providerId;
  }

  /**
   * @param providerId the providerId to set
   */
  public TemplateConfig setProviderId(String providerId) {
    this.providerId = providerId;
    return this;
  }

  /**
   * @return the bundlePath
   */
  public String getBundlePath() {
    return bundlePath;
  }

  /**
   * @param bundlePath the bundlePath to set
   */
  public void setBundlePath(String bundlePath) {
    this.bundlePath = bundlePath;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TemplateConfig) {
      return ((TemplateConfig) obj).getProviderId().equals(this.getProviderId());
    }
    return false;
  }

}
