package org.exoplatform.webui.form.user;


public interface UISelector<T> {
  public abstract T currentSelected();
  public abstract void updateSelect(T values);
}
