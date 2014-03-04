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
package org.exoplatform.commons.notification.impl.service.template;

import java.util.Calendar;
import java.util.Locale;

import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.notification.NotificationConfiguration;
import org.exoplatform.commons.notification.impl.DigestDailyPlugin;
import org.exoplatform.commons.notification.impl.DigestWeeklyPlugin;
import org.exoplatform.webui.utils.TimeConvertUtils;

public class DigestInfo {
  private String  firstName;

  private String  portalName;
  
  private String  portalHome;

  private String  sendTo;

  private String  footerLink;

  private String  fromTo     = "Today";

  private String  periodType = fromTo;

  private String  pluginId   = DigestDailyPlugin.ID;

  private Locale  locale;

  private boolean isWeekly;

  public DigestInfo(NotificationConfiguration configuration, UserSetting userSetting) {
    firstName = NotificationPluginUtils.getFirstName(userSetting.getUserId());
    sendTo = NotificationPluginUtils.getTo(userSetting.getUserId());
    portalName = NotificationPluginUtils.getBrandingPortalName();
    portalHome = NotificationPluginUtils.getPortalHome(portalName);
    footerLink = NotificationPluginUtils.getProfileUrl(userSetting.getUserId());
    String language = NotificationPluginUtils.getLanguage(userSetting.getUserId());
    locale = (language == null || language.length() == 0) ? Locale.ENGLISH : new Locale(language);
    
    isWeekly = (configuration.isSendWeekly() && userSetting.getWeeklyProviders().size() > 0);
    //
    if(isWeekly) {
      pluginId = DigestWeeklyPlugin.ID;
      periodType = "Weekly";
      //
      Calendar periodFrom = userSetting.getLastUpdateTime();
      long t = System.currentTimeMillis() - 604800000;
      if(t > periodFrom.getTimeInMillis()) {
        periodFrom.setTimeInMillis(t);
      }
      StringBuffer buffer = new StringBuffer();
      buffer.append(TimeConvertUtils.getFormatDate(periodFrom.getTime(), "mmmm dd", locale))
            .append(" - ")
            .append(TimeConvertUtils.getFormatDate(Calendar.getInstance().getTime(), "mmmm dd, yyyy", locale));
      fromTo = buffer.toString();
    }
  }

  public String getFromTo() {
    return fromTo;
  }

  public String getPeriodType() {
    return periodType;
  }

  public String getPluginId() {
    return pluginId;
  }

  public Locale getLocale() {
    return locale;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getPortalName() {
    return portalName;
  }

  public String getPortalHome() {
    return portalHome;
  }

  public String getSendTo() {
    return sendTo;
  }

  public String getFooterLink() {
    return footerLink;
  }

}
