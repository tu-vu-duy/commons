package org.exoplatform.webui.form;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Created by The eXo Platform SAS
 * Author : Ha Quang Tan
 *          tanhq@exoplatform.com
 * July 15, 2013
 */
public class UIFormRichtextInput extends UIFormInputBase<String> {

  public static final String FULL_TOOLBAR = "CompleteWCM";
  public static final String BASIC_TOOLBAR = "Basic";
  public static final String SUPER_BASIC_TOOLBAR = "SuperBasicWCM";
  public static final String INLINE_TOOLBAR = "InlineEdit";
  public static final String FORUM_TOOLBAR = "Forum";
  public static final String FAQ_TOOLBAR = "FAQ";
  
  public static final String ENTER_P = "1";
  public static final String ENTER_BR = "2";
  public static final String ENTER_DIV = "3";

  private String width;

  private String height;

  private String toolbar;
  
  private String enterMode;
  
  private String css;
  
  private boolean isPasteAsPlainText = false;

  private boolean isIgnoreParserHTML = false;

  public UIFormRichtextInput(String name, String bindingField, String value) {
    super(name, bindingField, String.class);
    this.value_ = value;
  }

  public UIFormRichtextInput(String name, String bindingField, String value, String enterMode) {
    super(name, bindingField, String.class);
    this.value_ = value;
    this.enterMode = enterMode;
  }
  

  public UIFormRichtextInput(String name, String bindingField, String value, String enterMode, String toolbar) {
    super(name, bindingField, String.class);
    this.value_ = value;
    this.enterMode = enterMode;
    this.toolbar = toolbar;
  }
  
  
  public UIFormRichtextInput(String name, String bindingField, String value, String enterMode, String toolbar, String css) {
    super(name, bindingField, String.class);
	this.value_ = value;
	this.enterMode = enterMode;
	this.toolbar = toolbar;
	this.css = css;
  }
  
  public String getWidth() {
    return width;
  }

  public void setWidth(String width) {
    this.width = width;
  }

  public String getHeight() {
    return height;
  }

  public void setHeight(String height) {
    this.height = height;
  }

  public String getToolbar() {
    return toolbar;
  }
  
  public String getEnterMode() {
  	return enterMode;
  }

  public void setToolbar(String toolbar) {
    this.toolbar = toolbar;
  }

  public void setEnterMode(String enterMode) {
    this.enterMode = enterMode;
  }

  public UIFormRichtextInput setIsPasteAsPlainText(boolean isPasteAsPlainText) {
    this.isPasteAsPlainText = isPasteAsPlainText;
    return this;
  }

  public boolean getIsPasteAsPlainText() {
    return this.isPasteAsPlainText;
  }

  public boolean isIgnoreParserHTML() {
    return isIgnoreParserHTML;
  }

  public UIFormRichtextInput setIgnoreParserHTML(boolean isIgnoreParserHTML) {
    this.isIgnoreParserHTML = isIgnoreParserHTML;
    return this;
  }

  public void setCss(String css) {
	  this.css = css;
  }
  
  public String getCss() {
	  return css;
  }
  
  private String buildEditorLayout() throws Exception {
    if (toolbar == null) toolbar = BASIC_TOOLBAR;
    if (width == null) width = "98%";
    if (height == null) height = "'200px'";
    if (enterMode == null) enterMode = "1";
    if (css == null) css = "\"/CommonsResources/ckeditor/contents.css\"";

    StringBuffer buffer = new StringBuffer();
    buffer.append("<div class=\"clearfix\">");
    buffer.append("  <span style=\"float:left; width:").append(width).append(";\">");
    //
    String initValue = "";
    if (value_ != null) {
      initValue = (isIgnoreParserHTML()) ? value_.replaceAll("&lt;", "&_LT;") : value_;
      
      buffer.append("<div style=\"display:none\" id=\"content").append(name).append("\">")
            .append(URLEncoder.encode(value_, "UTF-8")).append("</div>");
    }

    buffer.append("  <textarea id=\"").append(name).append("\" name=\"").append(name).append("\">")
          .append(initValue).append("</textarea>\n");

    buffer.append("<script type=\"text/javascript\">\n")
          .append("    require(['/CommonsResources/ckeditor/ckeditor.js'], function() {")
          .append("  //<![CDATA[\n")
          .append("    var instance = CKEDITOR.instances['").append(name).append("'];")
          .append("    if (instance) { CKEDITOR.remove(instance); instance = null;}\n");

    buffer.append("    CKEDITOR.replace('").append(name).append("', {toolbar:'").append(toolbar).append("', height:")
          .append(height).append(", contentsCss:").append(css).append(", enterMode:").append(enterMode)
          .append((isPasteAsPlainText) ? ", forcePasteAsPlainText: true" : "")
          .append(", shiftEnterMode:").append(enterMode + "});\n");

    buffer.append((isIgnoreParserHTML()) ? "CKEDITOR.ignoreParserHTML=true;" : "");

    buffer.append("    instance = CKEDITOR.instances['" + name + "'];")
          .append("    instance.on( 'change', function(e) { document.getElementById('").append(name).append("').value = instance.getData(); });\n")
          .append("  //]]>\n")
          .append("});");

    buffer.append("</script>\n");

    buffer.append("  </span>");

    if (isMandatory()) {
      buffer.append("  <span style=\"float:left\"> &nbsp;*</span>");
    }
    buffer.append("</div>");
    //
    return buffer.toString();
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    //
    context.getWriter().write(buildEditorLayout());
  }

  public void decode(Object input, WebuiRequestContext context) {
    value_ = (String) input;
    if (value_ != null && value_.length() == 0) {
      value_ = null;
    }
    value_ = (value_ != null && isIgnoreParserHTML()) ? value_.replaceAll("&lt;", "&_LT;").replaceAll("&nbsp;", " ") : value_;
  }

}

