package org.exoplatform.webui.container;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

@ComponentConfig(lifecycle = Lifecycle.class)
public class UIPermissionPopupContainer extends UIPopupContainer {

  private String currentContainerId = "";
  
  public UIPermissionPopupContainer() throws Exception {
    super();
  }
  
  public String getCurrentContainerId() {
    return currentContainerId;
  }

  public void setCurrentContainerId(String currentContainerId) {
    this.currentContainerId = currentContainerId;
  }
}
