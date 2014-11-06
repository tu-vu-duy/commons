package org.exoplatform.webui.container;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.organization.UIGroupMembershipSelector;
import org.exoplatform.webui.organization.account.UIUserSelector;

@ComponentConfigs({
  @ComponentConfig(lifecycle = Lifecycle.class),
  @ComponentConfig(
     id = "UIPermissionPopupWindow",
     type = UIPopupWindow.class,
     template = "system:/groovy/webui/core/UIPopupWindow.gtmpl",
     events = {
       @EventConfig(listeners = UIPermissionPopupContainer.ClosePopupActionListener.class, name = "ClosePopup"),
       @EventConfig(listeners = UIPermissionPopupContainer.AddActionListener.class, name = "Add", phase = Phase.DECODE),
       @EventConfig(listeners = UIPermissionPopupContainer.SelectMembershipActionListener.class, name = "SelectMembership", phase = Phase.DECODE),
       @EventConfig(listeners = UIPermissionPopupContainer.ClosePopupActionListener.class, name = "Close")
     }
  )
})
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

  public static class SelectMembershipActionListener extends EventListener<UIGroupMembershipSelector> {
    @Override
    public void execute(Event<UIGroupMembershipSelector> event) throws Exception {
      UIGroupMembershipSelector uiSelector = event.getSource();
      String group = uiSelector.getCurrentGroup().getId();
      System.out.println("\n selected: " + group);

      String membershipType = event.getRequestContext().getRequestParameter(OBJECTID);

      System.out.println("\n membershipType: " + group);

      UIPortletApplication portlet = uiSelector.getAncestorOfType(UIPortletApplication.class);
      UIPermissionPopupContainer popupContainer = uiSelector.getAncestorOfType(UIPermissionPopupContainer.class);
      String containerId = popupContainer.getCurrentContainerId();
      UIPermissionContainer uiPermission = portlet.findComponentById(containerId);
      //
      uiPermission.addValue(event.getRequestContext(), membershipType + ":" + group);
      //
      popupContainer.cancelPopupAction();
    }
  }

  public static class AddActionListener extends EventListener<UIUserSelector> {
    @Override
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiSelector = event.getSource();
      String values = uiSelector.getSelectedUsers();
      System.out.println("\n selected: " + values);
      UIPortletApplication portlet = uiSelector.getAncestorOfType(UIPortletApplication.class);
      UIPermissionPopupContainer popupContainer = uiSelector.getAncestorOfType(UIPermissionPopupContainer.class);
      String containerId = popupContainer.getCurrentContainerId();
      UIPermissionContainer uiPermission = portlet.findComponentById(containerId);
      //
      uiPermission.addValue(event.getRequestContext(), values);
      //
      popupContainer.cancelPopupAction();
    }
  }

  public static class ClosePopupActionListener extends EventListener<UIPopupWindow> {
    public void execute(Event<UIPopupWindow> event) throws Exception {
      UIPopupWindow popupWindow = event.getSource();
      popupWindow.getAncestorOfType(UIPermissionPopupContainer.class).cancelPopupAction();
    }
  }
}
