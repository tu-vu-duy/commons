package org.exoplatform.webui.form;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.portlet.MimeResponse;
import javax.portlet.ResourceURL;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.webui.application.WebuiRequestContext;

public class UIPermissionSelectorInput extends UIFormInputBase<String> {
  protected static Log LOG = ExoLogger.getLogger(UIPermissionSelectorInput.class);
  private static ResourceBundle res;
  private static  OrganizationService organizationService;
  private static List<String> listMemberhip;
  private static Map<String, String> membershipData;
  private static Locale locale;
  
  private String requestURL = "";

  private static OrganizationService getOrganizationService() {
    if (organizationService == null) {
      organizationService = (OrganizationService) PortalContainer.getInstance()
                                                                 .getComponentInstanceOfType(OrganizationService.class);
    }
    return organizationService;
  }
  
  private static Locale getLocale() {
    return locale;
  }

  public String getRequestURL() {
    return requestURL;
  }

  public void setRequestURL(String requestURL) {
    this.requestURL = requestURL;
  }

  public static List<String> getMembershipTypes() {
    if (listMemberhip == null) {
      try {
        Collection<?> ms = getOrganizationService().getMembershipTypeHandler().findMembershipTypes();
        List<MembershipType> memberships = (List<MembershipType>) ms;
        Collections.sort(memberships, new Comparator<MembershipType>() {
          @Override
          public int compare(MembershipType o1, MembershipType o2) {
            return o1.getName().compareTo(o2.getName());
          }
        });
        listMemberhip = new LinkedList<String>();
        boolean containWildcard = false;
        for (MembershipType mt : memberships) {
          listMemberhip.add(mt.getName());
          if ("*".equals(mt.getName())) {
            containWildcard = true;
          }
        }
        if (!containWildcard) {
          ((LinkedList<String>) listMemberhip).addFirst("*");
        }
      } catch (Exception e) {
        LOG.warn("Get memberships type unsuccessfully.");
      }
    }
    return listMemberhip;
  }

  private static String getMembershipType(String membershipType) {
    return (membershipType.equals("*")) ? "any" : membershipType;
  }

  public static Map<String, String> buildMembershipData() {
    if (membershipData == null) {
      membershipData = new HashMap<String, String>();
      for (String membershipType : UIPermissionSelectorInput.getMembershipTypes()) {
        membershipType = getMembershipType(membershipType);
        membershipData.put(membershipType, getCommonLabel("UIPermissionSelector.membership." + membershipType));
      }
    }
    //
    return membershipData;
  }  

  private String buildMembershipObject() {
    StringBuilder builder = new StringBuilder();
    Map<String, String> membershipData = buildMembershipData();
    for (String membershipType : UIPermissionSelectorInput.getMembershipTypes()) {
      if(builder.length() > 0) {
        builder.append(",\n");
      }
      membershipType = getMembershipType(membershipType);
      builder.append("\"").append(membershipType).append("\"").append(" : \"").append(membershipData.get(membershipType)).append("\"");
    }
    //
    return builder.toString();
  }
  
  private static ResourceBundle getResourceBundle() {
    if (res == null) {
      ResourceBundleService bundleService = (ResourceBundleService) PortalContainer.getInstance()
                                                                                   .getComponentInstanceOfType(ResourceBundleService.class);

      res = bundleService.getResourceBundle("locale.commons.Commons", getLocale());
    }
    return res;
  }
  
  public static String getCommonLabel(String key) {
    try {
      return getResourceBundle().getString(key);
    } catch (Exception e) {
      LOG.warn("Could not find key for " + key + " in " + res + " for locale " + res.getLocale());
      return (key.indexOf(".") > 0) ? key.substring(key.lastIndexOf(".") + 1) : key;
    }
  }
  
  public UIPermissionSelectorInput(String name, String bindingExpression) {
    super(name, bindingExpression, String.class);
  }

  public UIPermissionSelectorInput(String name, String bindingExpression, String defaultValue) {
    super(name, bindingExpression, String.class);
    this.defaultValue_ = defaultValue;
  }
  
  @Override
  public void decode(Object input, WebuiRequestContext context) {
    String val = (String)input;
    if ((val == null || val.trim().length() == 0)) {
      value_ = "";
      return;
    }
    value_ = StringUtils.replace(val, "any:", "*:");
  }
  
  public String buildDataJson() {
    return "";
  }

  public void addValue(WebuiRequestContext context, String value) {
    if (value_ == null || value_.trim().length() == 0) {
      value_ = value;
    } else {
      value_ = value + "," + value_;
    }
    //
    StringBuilder scripts = new StringBuilder();
    scripts.append("(function(jq) {")
           .append("var datas = ").append(buildValueJsObject(value)).append(";")
           .append("var thizz = jq(\"#wrapper-").append(getId()).append("\");")
           .append("if(thizz.data('groupSelector')){ thizz.groupSelector('setVal', datas); }")
           .append("})(jQuery);");
    
    context.getJavascriptManager().getRequireJS().require("SHARED/jquery", "jQuery")
           .require("SHARED/commons-uipermission").addScripts(scripts.toString());
  }
  
  public List<String> getDisplayValue() {
    List<String> result = new ArrayList<String>();
    if (value_ != null && value_.length() > 0) {
      StringTokenizer tokenizer = new StringTokenizer(value_, ",");
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        result.add(new Bean(token).getDisplay());
      }
    }
    return result;
  }
  
  private String buildValueJsObject(String value) {
    StringBuilder datas = new StringBuilder("{");
    if(value.length() > 0) {
      StringTokenizer tokenizer = new StringTokenizer(value, ",");
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        datas.append(new Bean(token).toJSObject());
        if(tokenizer.hasMoreTokens()) {
          datas.append(",");
        }
      }
    }
    datas.append("}");
    return datas.toString();
  }
  
  public String createRerveResourceURL(WebuiRequestContext context) {
    try {
      MimeResponse res = context.getResponse();
      ResourceURL rsURL = res.createResourceURL();
      rsURL.setResourceID(getId());
      String url = rsURL.toString() + "&q=";
      //
      return url;
    } catch (Exception e) {
      return "";
    }
  }
  
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    Locale locale_ = context.getLocale();
    if (locale == null || locale_ != locale) {
      locale = locale_;
      res = null;
      membershipData = null;
    }
    String requestUrl = getRequestURL();
    if (requestUrl == null || requestUrl.length() == 0) {
      requestUrl = createRerveResourceURL(context);
    }
    //
    String value = value_;
    if (value == null || value.length() == 0) {
      value = (defaultValue_ != null) ? defaultValue_ : "";
    }
    value = StringUtils.replace(StringUtils.replace(value, ",,", ""), "*:", "any:");
    StringBuilder writer = new StringBuilder();
    //
    Writer w = new StringWriter();
    renderHTMLAttributes(w);
    //
    writer.append("<div class=\"groupSelector-container\"").append(w.toString()).append(">")
          .append("  <div class=\"uneditable-input groupSelector dropdown\" id=\"wrapper-" + getId() +"\">")
          .append("    <input type=\"hidden\" id=\"" + getId() +"\" name=\"" + getId() +"\" value=\"" + value +"\"/>")
          .append("    <span class=\"w-input\"></span>")
          .append("    <input type=\"text\" class=\"target-input\" name=\"target-input\"/>")
          .append("  </div>")
          .append("</div>");

    writer.append("<script type=\"text/javascript\">\n");
    writer.append("window.eXo = window.eXo || {};\n")
          .append("window.eXo.webui = window.eXo.webui || {};\n")
          .append("if(!window.eXo.webui.groupSelector) {\n")
          .append("  window.eXo.webui.groupSelector = {};\n")
          .append("  var memberships_ = {")
          .append(buildMembershipObject())
          .append("  };")
          .append("  var defaultSetting = {\n")
          .append("   url : \"\",\n")
          .append("   memberships : memberships_,\n")
          .append("   i18n : {\n")
          .append("     inLabel : \"").append(getCommonLabel("UIPermissionSelector.label.in")).append("\",\n")
          .append("     anyLabel : \"").append(getCommonLabel("UIPermissionSelector.membership.any")).append("\",\n")
          .append("     userLabel : \"").append(getCommonLabel("UIPermissionSelector.label.Users")).append("\",\n")
          .append("     groupLabel : \"").append(getCommonLabel("UIPermissionSelector.label.Groups")).append("\",\n")
          .append("     noMatchLabel : \"").append(getCommonLabel("UIPermissionSelector.label.noMatch")).append("\"\n")
          .append("   }\n")
          .append(" };\n")
          .append(" window.eXo.webui.groupSelector.defaultSetting = defaultSetting;\n")
          .append("}\n");
    writer.append("</script>\n");
    //
    context.getWriter().write(writer.toString());
    //
    StringBuilder scripts = new StringBuilder();
    scripts.append("(function(jq) {")
           .append("var settings = jq.extend(true, {}, {}, window.eXo.webui.groupSelector.defaultSetting);")
           .append("settings.url = \"").append(requestUrl).append("\";")
           .append("var datas = ").append(buildValueJsObject(value)).append(";")
           .append("jq(\"#wrapper-").append(getId()).append("\")")
           .append(".groupSelector(settings).groupSelector('setVal', datas);")
           .append("})(jQuery);");
    //
    context.getJavascriptManager()
           .getRequireJS().require("SHARED/jquery", "jQuery")
           .require("SHARED/commons-uipermission")
           .addScripts(scripts.toString());
  }
  
  public class Bean {
    private String id;
    private String label;
    private boolean isUser = true;
    private String membershipType;
    private String groupId;
    
    public Bean(String id) {
      this.id = id.trim();
      this.isUser = (id.indexOf("/") < 0);
      buildData();
    }
    
    private void buildData() {
      if (isUser) {
        try {
          User user = getOrganizationService().getUserHandler().findUserByName(this.id);
          label = user.getDisplayName();
          if (label == null || label.trim().length() == 0) {
            label = user.getFirstName() + " " + user.getLastName();
          }
        } catch (Exception e) {
          this.label = id;
        }
      } else {
        membershipType = getMembershipType(this.id.substring(0, this.id.indexOf(":")));
        groupId = this.id.substring(this.id.indexOf(":") + 1);
        Map<String, String> membershipData = buildMembershipData();
        if (membershipData.containsKey(membershipType)) {
          membershipType = membershipData.get(membershipType);
        }
        try {
          Group group = getOrganizationService().getGroupHandler().findGroupById(groupId);
          label = group.getLabel();
        } catch (Exception e) {
          this.label = id;
        }
      }
    }

    public String getGroupId() {
      return groupId;
    }

    public String toJSObject() {
      StringBuilder builder = new StringBuilder();
      if(isUser) {
        builder.append("\"").append(id).append("\" : \"").append(label).append("\"");
      } else {
        builder.append("\"").append(id).append("\" : {")
               .append("\"type\" : \"").append(membershipType).append("\", ")
               .append("\"group\" : \"").append(label).append("\"")
               .append("}");
      }
      return builder.toString();
    }

    public String getDisplay() {
      StringBuilder builder = new StringBuilder("<strong>");
      if(isUser) {
        builder.append(label);
      } else {
        builder.append(membershipType).append(" ")
               .append(getCommonLabel("UIPermissionSelector.label.in"))
               .append(" ")
               .append(label);
      }
      return builder.append("</strong> (").append(id).append(")").toString();
    }
  }
}
