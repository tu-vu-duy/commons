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
package org.exoplatform.commons.notification.impl.log;

import java.util.Calendar;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.mail.Message;
import org.exoplatform.webui.utils.TimeConvertUtils;
import org.json.JSONObject;

public abstract class FormatLog {
  protected String     format;
  protected String     pluginId = "";
  protected JSONObject jsonObject;

  public FormatLog() {
    format = "";
  }

  public String getFormat() {
    return this.format;
  }

  public FormatLog with(String pluginId, Message message) {
    this.jsonObject = new JSONObject(message);
    this.pluginId = pluginId;
    return this;
  }

  public String build() {
    String time = TimeConvertUtils.getFormatDate(Calendar.getInstance().getTime(), "MM/dd/yyyy hh:mm");
    StringBuilder builder = new StringBuilder(time).append(" - ");
    builder.append(pluginId).append("::Thread: ").append(Thread.currentThread().getName())
           .append(", Repo: ").append(CommonsUtils.getRepository().getConfiguration().getName());
    return builder.toString();
  }
}
