package org.exoplatform.webui.form;

public abstract class CompletionBean {
//  public static DefaultCompletionBean defaultCompletionBean = new DefaultCompletionBean();
  protected String id;
  protected String label;
  public CompletionBean() {
  }

  public CompletionBean parse(String id, String label) {
    this.id = id.trim();
    this.label = label.trim();
    buildData();
    return this;
  }

  public CompletionBean parse(String id) {
    return this.parse(id, id);
  }

  public abstract void buildData();
  public abstract String getJSObject();
  public abstract String getDisplay();
  
  public static class DefaultCompletionBean extends CompletionBean {
    private static DefaultCompletionBean defaultCompletionBean;

    public static DefaultCompletionBean getInstance() {
      if (defaultCompletionBean == null) {
        defaultCompletionBean = new DefaultCompletionBean();
      }
      return defaultCompletionBean;
    }
    @Override
    public void buildData() {
    }
    @Override
    public String getJSObject() {
      StringBuilder builder = new StringBuilder("\"")
          .append(id).append("\" : \"").append(label).append("\"");
      return builder.toString();
    }

    @Override
    public String getDisplay() {
      StringBuilder builder = new StringBuilder("<strong>")
          .append(label).append("</strong> (").append(id).append(")");
      return builder.toString();
    }
  }
}
