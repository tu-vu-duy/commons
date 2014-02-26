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
package org.exoplatform.commons.notification;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.jcr.Value;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.config.TemplateConfig;
import org.exoplatform.commons.api.notification.template.Element;
import org.exoplatform.commons.notification.impl.AbstractService;
import org.exoplatform.commons.notification.template.DigestTemplate;
import org.exoplatform.commons.notification.template.SimpleElement;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.xml.InitParams;


public class NotificationUtils {

  public static final String DEFAULT_SUBJECT_KEY       = "Notification.subject.{0}";

  public static final String DEFAULT_SIMPLE_DIGEST_KEY = "Notification.digest.{0}";

  public static final String DEFAULT_DIGEST_ONE_KEY    = "Notification.digest.one.{0}";

  public static final String DEFAULT_DIGEST_THREE_KEY  = "Notification.digest.three.{0}";
  
  public static final String DEFAULT_DIGEST_MORE_KEY   = "Notification.digest.more.{0}";

  public static final String FEATURE_NAME              = "notification";
  
  private static final String emailRegex = "[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[_A-Za-z0-9-.]+\\.[A-Za-z]{2,5}";
  private static final Pattern EMAIL_PATTERN = Pattern.compile(emailRegex);

  public static String getDefaultKey(String key, String providerId) {
    return MessageFormat.format(key, providerId);
  }
  
  /**
   * Gets the digest's resource bundle
   * 
   * @param templateConfig
   * @param pluginId
   * @param language
   * @return
   */
  public static DigestTemplate getDigest(TemplateConfig templateConfig, String pluginId, String language) {
    String srcResource = templateConfig.getBundlePath();
    String digestOneKey = templateConfig.getKeyValue(TemplateConfig.DIGEST_ONE_KEY, getDefaultKey(DEFAULT_DIGEST_ONE_KEY, pluginId));
    String digestThreeKey = templateConfig.getKeyValue(TemplateConfig.DIGEST_THREE_KEY, getDefaultKey(DEFAULT_DIGEST_THREE_KEY, pluginId));
    String digestMoreKey = templateConfig.getKeyValue(TemplateConfig.DIGEST_MORE_KEY, getDefaultKey(DEFAULT_DIGEST_MORE_KEY, pluginId));
    
    Locale locale = new Locale(language);
    
    return new DigestTemplate().digestOne(TemplateUtils.getResourceBundle(digestOneKey, locale, srcResource))
                               .digestThree(TemplateUtils.getResourceBundle(digestThreeKey, locale, srcResource))
                               .digestMore(TemplateUtils.getResourceBundle(digestMoreKey, locale, srcResource));
        
                                
  }
  
  /**
   * Gets the subject's resource bundle
   * 
   * @param templateConfig
   * @param pluginId
   * @param language
   * @return
   */
  public static Element getSubject(TemplateConfig templateConfig, String pluginId, String language) {
    String bundlePath = templateConfig.getBundlePath();
    String subjectKey = templateConfig.getKeyValue(TemplateConfig.SUBJECT_KEY, getDefaultKey(DEFAULT_SUBJECT_KEY, pluginId));
    
    Locale locale = new Locale(language);
    
    return new SimpleElement().language(locale.getLanguage()).template(TemplateUtils.getResourceBundle(subjectKey, locale, bundlePath));
  }
  
  
  public static String listToString(List<String> list) {
    if (list == null || list.size() == 0) {
      return "";
    }
    StringBuffer values = new StringBuffer();
    for (String str : list) {
      if (values.length() > 0) {
        values.append(",");
      }
      values.append(str);
    }
    return values.toString();
  }

  public static String[] valuesToArray(Value[] values) throws Exception {
    if (values.length < 1)
      return new String[] {};
    List<String> list = valuesToList(values);
    return list.toArray(new String[list.size()]);
  }

  public static List<String> valuesToList(Value[] values) throws Exception {
    List<String> list = new ArrayList<String>();
    if (values.length < 1)
      return list;
    String s;
    for (int i = 0; i < values.length; ++i) {
      s = values[i].getString();
      if (s != null && s.trim().length() > 0)
        list.add(s);
    }
    return list;
  }
  
  public static String getValueParam(InitParams params, String key, String defaultValue) {
    try {
      return params.getValueParam(key).getValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public static int getValueParam(InitParams params, String key, int defaultValue) {
    try {
      return Integer.valueOf(params.getValueParam(key).getValue());
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public static String getSystemValue(InitParams params, String systemKey, String paramKey, String defaultValue) {
    try {
      String vl = System.getProperty(systemKey);
      if (vl == null || vl.length() == 0) {
        vl = getValueParam(params, paramKey, defaultValue);
      }
      return vl.trim();
    } catch (Exception e) {
      return defaultValue;
    }
  }
  
  public static int getSystemValue(InitParams params, String systemKey, String paramKey, int defaultValue) {
    return Integer.valueOf(getSystemValue(params, systemKey, paramKey, String.valueOf(defaultValue)));
  }
  
  public static boolean isValidEmailAddresses(String addressList){
    if (addressList == null || addressList.length() < 0)
      return false;
    addressList = StringUtils.remove(addressList, " ");
    addressList = StringUtils.replace(addressList, ";", ",");
    try {
      InternetAddress[] iAdds = InternetAddress.parse(addressList, true);
      for (int i = 0; i < iAdds.length; i++) {
        if (!EMAIL_PATTERN.matcher(iAdds[i].getAddress()).matches()){
          return false;
        }
      }
    } catch (AddressException e) {
      return false;
    }
    return true;
  }
  
  public static String buildQueryNotification(String parentPath, String userName, boolean isWeekly) {
    StringBuilder strQuery = new StringBuilder("SELECT * FROM ").append(AbstractService.NTF_MESSAGE).append(" WHERE ");

    // ntf:sendToWeekly: ['&forAllUser'] OR ['demo', 'mary', 'john']
    // ntf:sendToDaily: ['&forAllUser'] OR ['demo', 'mary', 'john']
    // builds filter UserId to send daily or weekly
    // for example: in case daily >> ntf:sendToDaily='demo'
    // in case weekly >> ntf:sendToWeekly='demo'
    if (isWeekly) {
      strQuery.append(" jcr:path LIKE '").append(parentPath).append("/%'");
      strQuery.append(" AND (").append(AbstractService.NTF_SEND_TO_WEEKLY).append("='").append(userName).append("'");
    } else {
      strQuery.append(" (jcr:path LIKE '").append(parentPath).append("/%'").append(" AND NOT jcr:path LIKE '").append(parentPath).append("/%/%')");
      strQuery.append(" AND (").append(AbstractService.NTF_SEND_TO_DAILY).append("='").append(userName).append("'");
    }

    if (!NotificationInfo.FOR_ALL_USER.equals(userName)) {
      strQuery.append(" OR ")
              .append((isWeekly) ? AbstractService.NTF_SEND_TO_WEEKLY : AbstractService.NTF_SEND_TO_DAILY)
              .append("='")
              .append(NotificationInfo.FOR_ALL_USER)
              .append("') AND ")
              .append(AbstractService.NTF_FROM)
              .append("<>'")
              .append(userName)
              .append("'");
    } else {
      strQuery.append(")");
    }

    return strQuery.toString();
  }
  
  
  
}
