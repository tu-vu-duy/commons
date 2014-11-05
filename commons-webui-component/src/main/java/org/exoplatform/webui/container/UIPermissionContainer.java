package org.exoplatform.webui.container;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIPermissionSelectorInput;
import org.exoplatform.webui.form.UIPermissionSelectorInput.Bean;
import org.exoplatform.webui.organization.UIGroupMembershipSelector;
import org.exoplatform.webui.organization.account.UIGroupSelector;
import org.exoplatform.webui.organization.account.UIUserSelector;


@ComponentConfigs({
  @ComponentConfig(
     template = "classpath:groovy/forum/common/UIPermissionContainer.gtmpl",
     events = { 
        @EventConfig(listeners = UIPermissionContainer.OpenUserPopupActionListener.class),
        @EventConfig(listeners = UIPermissionContainer.OpenRoleAndGroupPopupActionListener.class),
        @EventConfig(listeners = UIPermissionContainer.AddPermissionActionListener.class)
     }
  ),
  
  @ComponentConfig(
     id = "UIPermissionPopupWindow",
     type = UIPopupWindow.class,
     template = "system:/groovy/webui/core/UIPopupWindow.gtmpl",
     events = {
       @EventConfig(listeners = UIPermissionContainer.ClosePopupActionListener.class, name = "ClosePopup"),
       @EventConfig(listeners = UIPermissionContainer.AddActionListener.class, name = "Add", phase = Phase.DECODE),
       @EventConfig(listeners = UIPermissionContainer.ClosePopupActionListener.class, name = "Close")
     }
  )
})
public class UIPermissionContainer extends UIContainer {

  private static final String PERMISSION_INPUT   = "permissionInput";

  private static final String POPUP_WINDOW_ID    = "UIPermissionPopupWindow";

  final private static String TREE_GROUP_ID      = "UITreeGroupSelector";

  final private static String BREADCUMB_GROUP_ID = "BreadcumbGroupSelector";
  
  private String limitedGroup = "";
  private boolean isEditMode = false;

  public UIPermissionContainer() {
    setId("UIPermissionContainer");
    UIPermissionSelectorInput input = new UIPermissionSelectorInput(PERMISSION_INPUT, PERMISSION_INPUT, null);
    addChild(input);
  }

  public String getLimitGroupId() {
    return limitedGroup;
  }
  
  protected String getResouceLabel(String key) {
    return UIPermissionSelectorInput.getCommonLabel(key);
  }
  
  private UIPermissionSelectorInput getUIPermissionSelectorInput() {
    return getChild(UIPermissionSelectorInput.class);
  }

  public String getValue() {
    return getUIPermissionSelectorInput().getValue();
  }

  public List<String> getDisplayValue() {
    return getUIPermissionSelectorInput().getDisplayValue();
  }

  public UIPermissionContainer setValue(String value) {
    getUIPermissionSelectorInput().setValue(value);
    return this;
  }

  private static void closePopup(UIPopupWindow popupWindow) {
    popupWindow.setUIComponent(null);
    popupWindow.setShow(false);
    popupWindow.setRendered(false);
    WebuiRequestContext context = RequestContext.getCurrentInstance();
    context.addUIComponentToUpdateByAjax(popupWindow.getParent());
  }

  public static class AddPermissionActionListener extends EventListener<UIPermissionContainer> {
    @Override
    public void execute(Event<UIPermissionContainer> event) throws Exception {
      UIPermissionContainer panel = event.getSource();
      panel.isEditMode = false;
      event.getRequestContext().addUIComponentToUpdateByAjax(panel);
    }
  }

  public static class OpenUserPopupActionListener extends EventListener<UIPermissionContainer> {
    @Override
    public void execute(Event<UIPermissionContainer> event) throws Exception {
      UIPermissionContainer permissionPanel = event.getSource();

      UIPopupContainer uiPopupContainer = permissionPanel.getAncestorOfType(UIPopupContainer.class);
      UIGroupSelector uiGroupSelector = uiPopupContainer.findFirstComponentOfType(UIGroupSelector.class);
      if (uiGroupSelector != null) {
        UIPopupWindow popupWindow = uiGroupSelector.getAncestorOfType(UIPopupWindow.class);
        closePopup(popupWindow);
      }

      UIPopupWindow uiPopupWindow = uiPopupContainer.getChildById(POPUP_WINDOW_ID);
      if (uiPopupWindow == null){
        uiPopupWindow = uiPopupContainer.addChild(UIPopupWindow.class, POPUP_WINDOW_ID, POPUP_WINDOW_ID);
      }
      //
      UIUserSelector uiUserSelector = uiPopupContainer.createUIComponent(UIUserSelector.class, null, "UIUserSelector");
      uiUserSelector.setShowSearch(true);
      uiUserSelector.setShowSearchUser(true);
      uiUserSelector.setShowSearchGroup(false);
      uiUserSelector.setSelectedGroup(permissionPanel.getLimitGroupId());
      uiPopupWindow.setUIComponent(uiUserSelector);
      uiPopupWindow.setShow(true);
      uiPopupWindow.setWindowSize(740, 400);
      uiPopupWindow.setRendered(true);
      uiPopupContainer.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  public static class OpenRoleAndGroupPopupActionListener extends EventListener<UIPermissionContainer> {
    @Override
    public void execute(Event<UIPermissionContainer> event) throws Exception {
      UIPermissionContainer permissionPanel = event.getSource();

      UIPopupContainer uiPopupContainer = permissionPanel.getAncestorOfType(UIPopupContainer.class);
      UIUserSelector user = uiPopupContainer.findFirstComponentOfType(UIUserSelector.class);
      if (user != null) {
        UIPopupWindow popupWindow = user.getAncestorOfType(UIPopupWindow.class);
        closePopup(popupWindow);
      }

      UIPopupWindow uiPopupWindow = uiPopupContainer.getChild(UIPopupWindow.class);

      //
      UIGroupMembershipSelector group = uiPopupContainer.createUIComponent(UIGroupMembershipSelector.class, null, "UIMemberShipSelector");
      group.getChild(UITree.class).setId(TREE_GROUP_ID);
      group.getChild(org.exoplatform.webui.core.UIBreadcumbs.class).setId(BREADCUMB_GROUP_ID);

      uiPopupWindow.setUIComponent(group);
      uiPopupWindow.setShow(true);
      uiPopupWindow.setWindowSize(600, 0);
      uiPopupWindow.setRendered(true);
      uiPopupContainer.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  private UIPermissionContainer addValue(WebuiRequestContext context, String values) {
    getUIPermissionSelectorInput().addValue(context, values);
    return this;
  }

  public static class AddActionListener extends EventListener<UIUserSelector> {
    @Override
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiUserSelector = event.getSource();
      String values = uiUserSelector.getSelectedUsers();
      UIPortletApplication portlet = uiUserSelector.getAncestorOfType(UIPortletApplication.class);
      UIPermissionContainer uiPermission = portlet.findFirstComponentOfType(UIPermissionContainer.class);
      uiPermission.addValue(event.getRequestContext(), values);
      //
      closePopup((UIPopupWindow) uiUserSelector.getParent());
    }
  }

  public static class ClosePopupActionListener extends EventListener<UIPopupWindow> {
    public void execute(Event<UIPopupWindow> event) throws Exception {
      UIPopupWindow popupWindow = event.getSource();
      closePopup(popupWindow);
    }
  }
}
