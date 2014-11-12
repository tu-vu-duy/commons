package org.exoplatform.webui.form;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.portlet.MimeResponse;
import javax.portlet.ResourceURL;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.form.CompletionBean.DefaultCompletionBean;
import org.json.JSONObject;

public class UIAutoCompletionInput<T extends CompletionBean> extends UIFormInputBase<String> {

  protected static Log LOG = ExoLogger.getLogger(UIAutoCompletionInput.class);
  private static ResourceBundle res;
  private static Locale locale;
  private JSONObject settings = new JSONObject();
  private String requestURL = "";
  private T completion;

  public static Locale getLocale() {
    if (locale == null) {
      locale = WebuiRequestContext.getCurrentInstance().getLocale();
    }
    return locale;
  }

  public CompletionBean getCompletionBean() {
    if (completion == null) {
      completion = (T) DefaultCompletionBean.getInstance();
    }
    return completion;
  }

  public UIAutoCompletionInput<T> setCompletionBean(T completion) {
    this.completion = completion;
    return this;
  }

  public String getRequestURL() {
    return requestURL;
  }
  
  public UIAutoCompletionInput<T> setRequestURL(String requestURL) {
    this.requestURL = requestURL;
    return this;
  }

  public JSONObject getSettings() {
    return settings;
  }

  public UIAutoCompletionInput<T> setSettings(JSONObject settings) {
    this.settings = settings;
    return this;
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
      LOG.warn("Could not find key for: " + key);
      return (key.indexOf(".") > 0) ? key.substring(key.lastIndexOf(".") + 1) : key;
    }
  }
  
  public UIAutoCompletionInput(String name, String bindingExpression) {
    super(name, bindingExpression, String.class);
  }

  public UIAutoCompletionInput(String name, String bindingExpression, String defaultValue) {
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
        result.add(getCompletionBean().parse(token).getDisplay());
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
        datas.append(getCompletionBean().parse(token).getJSObject());
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
          .append("  var defaultSetting = {\n")
          .append("   url : \"\",\n")
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
           .append("var settings = jq.extend(true, {}, window.eXo.webui.groupSelector.defaultSetting, ").append(settings.toString()).append(");")
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
}
