package org.exoplatform.webui.container;

import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIPermissionSelectorInput;
import org.exoplatform.webui.organization.UIGroupMembershipSelector;
import org.exoplatform.webui.organization.account.UIGroupSelector;
import org.exoplatform.webui.organization.account.UIUserSelector;


@ComponentConfig(
   template = "classpath:groovy/webui/commons/UIPermissionContainer.gtmpl",
   events = { 
      @EventConfig(listeners = UIPermissionContainer.OpenUserPopupActionListener.class),
      @EventConfig(listeners = UIPermissionContainer.OpenRoleAndGroupPopupActionListener.class),
      @EventConfig(listeners = UIPermissionContainer.AddPermissionActionListener.class)
   }
)
public class UIPermissionContainer extends UIContainer {
  protected static Log        LOG                = ExoLogger.getLogger(UIPermissionContainer.class);

  private static final String PERMISSION_INPUT   = "permission_";

  private static final String POPUP_WINDOW_ID    = "UIPermissionPopupWindow";

  private static final String POPUP_CONTAINER_ID = "UIPermissionPopupContainer";

  private static final String USER_SELECTOR_ID   = "UIUserSelector";

  private static final String GROUP_SELECTOR_ID  = "UIMemberShipSelector";

  final private static String TREE_GROUP_ID      = "UITreeGroupSelector";

  final private static String BREADCUMB_GROUP_ID = "BreadcumbGroupSelector";
  
  private String limitedGroup = "";
  private boolean isEditMode = false;

  public UIPermissionContainer() throws Exception {
  }

  public UIPermissionContainer initContainer(String id, String requestURL, String defaultValue) {
    super.setId(id);
    //
    if (getUIPermissionSelectorInput() == null) {
      addChild(new UIPermissionSelectorInput(PERMISSION_INPUT + id, PERMISSION_INPUT + id, defaultValue));
    }
    //
    return setRequestURL(requestURL);
  }

  private UIPermissionPopupContainer getPopupContainer(WebuiRequestContext context) {
    UIForm uiForm = getAncestorOfType(UIForm.class);
    UIContainer uiContainer = null;
    if (uiForm.getAncestorOfType(UIPopupWindow.class) != null) {
      uiContainer = uiForm.getAncestorOfType(UIPopupWindow.class).getParent();
    } else {
      uiContainer = uiForm.getParent();
    }
    try {
      UIPermissionPopupContainer pContainer = uiContainer.getChildById(POPUP_CONTAINER_ID);
      if (pContainer == null) {
        pContainer = uiContainer.addChild(UIPermissionPopupContainer.class, null, POPUP_CONTAINER_ID);
        pContainer.getChild(UIPopupWindow.class).setId(POPUP_WINDOW_ID);
        context.addUIComponentToUpdateByAjax(uiContainer);
      }
      return pContainer;
    } catch (Exception e) {
      LOG.error("Failed to add UIPopupContainer ", e);
    }
    return null;
  }

  public UIPermissionContainer setValue(String value) {
    getUIPermissionSelectorInput().setValue(value);
    return this;
  }

  public UIPermissionContainer setRequestURL(String requestURL) {
    if (requestURL != null && requestURL.length() > 0) {
      getUIPermissionSelectorInput().setRequestURL(requestURL);
    }
    return this;
  }

  @Override
  public UIPermissionContainer setId(String id) {
    return initContainer(id, null, null);
  }

  public UIPermissionContainer setLimitGroupId(String limitedGroup) {
    this.limitedGroup = limitedGroup;
    return this;
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

  protected String getAddActionUrl() throws Exception {
    String action = event("AddPermission");
    return action.replace("javascript:ajaxGet('", "").replace("')", "&" + OBJECTID + "=");
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
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      LOG.debug("objectId " + objectId);
      panel.setValue(objectId);
      //
      event.getRequestContext().addUIComponentToUpdateByAjax(panel);
    }
  }

  public static class OpenUserPopupActionListener extends EventListener<UIPermissionContainer> {
    @Override
    public void execute(Event<UIPermissionContainer> event) throws Exception {
      UIPermissionContainer permissionPanel = event.getSource();

      UIPermissionPopupContainer uiPopupContainer = permissionPanel.getPopupContainer(event.getRequestContext());
      uiPopupContainer.setCurrentContainerId(permissionPanel.getId());
      UIPopupWindow uiPopupWindow = uiPopupContainer.getChildById(POPUP_WINDOW_ID);
      UIGroupSelector uiGroupSelector = uiPopupContainer.findFirstComponentOfType(UIGroupSelector.class);
      if (uiGroupSelector != null) {
        closePopup(uiPopupWindow);
      }
      //
      UIUserSelector uiUserSelector = uiPopupContainer.createUIComponent(UIUserSelector.class, null, USER_SELECTOR_ID);
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

      UIPermissionPopupContainer uiPopupContainer = permissionPanel.getPopupContainer(event.getRequestContext());
      uiPopupContainer.setCurrentContainerId(permissionPanel.getId());
      UIPopupWindow uiPopupWindow = uiPopupContainer.getChildById(POPUP_WINDOW_ID);
      
      UIUserSelector user = uiPopupContainer.findFirstComponentOfType(UIUserSelector.class);
      if (user != null) {
        closePopup(uiPopupWindow);
      }
      //
      UIGroupMembershipSelector group = uiPopupContainer.createUIComponent(UIGroupMembershipSelector.class, null,
                                                                           GROUP_SELECTOR_ID + permissionPanel.getId());
      group.getChild(UITree.class).setId(TREE_GROUP_ID);
      group.getChild(org.exoplatform.webui.core.UIBreadcumbs.class).setId(BREADCUMB_GROUP_ID);
      //
      uiPopupWindow.setUIComponent(group);
      uiPopupWindow.setShow(true);
      uiPopupWindow.setWindowSize(600, 0);
      uiPopupWindow.setRendered(true);
      uiPopupContainer.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  public UIPermissionContainer addValue(WebuiRequestContext context, String values) {
    getUIPermissionSelectorInput().addValue(context, values);
    return this;
  }

  
}
