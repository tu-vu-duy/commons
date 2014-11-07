package org.exoplatform.webui.container;

import java.util.ArrayList;
import java.util.List;

import javax.portlet.MimeResponse;
import javax.portlet.ResourceRequest;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.User;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIPermissionSelectorInput;
import org.exoplatform.webui.form.user.UIGroupSelector;
import org.exoplatform.webui.form.user.UISelector;
import org.exoplatform.webui.form.user.UIUserSelector;
import org.exoplatform.webui.form.user.UserHelper;
import org.exoplatform.webui.form.user.UserHelper.FilterType;
import org.exoplatform.webui.form.user.UserHelper.UserFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


@ComponentConfig(
   template = "classpath:groovy/webui/commons/UIPermissionContainer.gtmpl",
   events = { 
      @EventConfig(listeners = UIPermissionContainer.OpenUserPopupActionListener.class),
      @EventConfig(listeners = UIPermissionContainer.OpenRoleAndGroupPopupActionListener.class),
      @EventConfig(listeners = UIPermissionContainer.AddPermissionActionListener.class)
   }
)
public class UIPermissionContainer extends UIContainer implements UISelector<String> {
  protected static Log        LOG                = ExoLogger.getLogger(UIPermissionContainer.class);

  private static final String PERMISSION_INPUT   = "permission_";

  private static final String POPUP_WINDOW_ID    = "UIPermissionPopupWindow";

  private static final String POPUP_CONTAINER_ID = "UIPermissionPopupContainer";

  private static final String USER_SELECTOR_ID   = "UIUserSelector";

  private static final String GROUP_SELECTOR_ID  = "UIMemberShipSelector";

  final private static String TREE_GROUP_ID      = "UITreeGroupSelector";

  final private static String BREADCUMB_GROUP_ID = "BreadcumbGroupSelector";
  
  private String limitedGroup = "";
  protected boolean isEditMode = false;

  public UIPermissionContainer() throws Exception {
  }

  @Override
  public String currentSelected() {
    return getValue();
  }

  public static void buildServeResourceData(WebuiRequestContext context) throws Exception {
    ResourceRequest req = context.getRequest();
    String permissionID = req.getResourceID();
    String key = req.getParameter("q");
    //
    MimeResponse res = context.getResponse();
    res.setContentType("text/json");
    if (UserHelper.isEmpty(permissionID) || UserHelper.isEmpty(key)) {
      res.getWriter().write("[]");
      return;
    }
    UIPermissionContainer container = context.getUIApplication().findComponentById(permissionID).getParent();
    if (container != null) {
      JSONArray jsChilds = new JSONArray();
      jsChilds = concatArray(container.filterUser(key, 10), container.filterGroup(key, 10));
      res.getWriter().write(jsChilds.toString());
    }
  }
  
  public JSONArray filterUser(String key, int limit) throws Exception {
    if (limit == 0) {
      limit = 10;
    }
    ListAccess<User> listAccess = UserHelper.searchUser(new UserFilter(key, FilterType.ALL).setGroupId(getLimitGroupId()));
    int maxSize = listAccess.getSize();
    if (limit > maxSize) {
      limit = maxSize;
    }
    JSONArray jsChilds = new JSONArray();
    User[] users_ = listAccess.load(0, limit);
    for (int i = 0; i < users_.length; i++) {
      jsChilds.put(toJson(users_[i]));
    }
    return jsChilds;
  }

  public JSONArray filterGroup(String key, int limit) throws Exception {
    if (limit == 0) {
      limit = 10;
    }
    List<Group> groups = new ArrayList<Group>();
    if (!UserHelper.isEmpty(limitedGroup)) {
      Group pr = UserHelper.getGroupHandler().findGroupById(limitedGroup);
      groups.add(pr);
      groups.addAll(UserHelper.findGroups(pr));
    } else {
      groups = UserHelper.getAllGroup();
    }
    int i = 0;
    key = key.replace("/", "").toLowerCase();
    JSONArray jsChilds = new JSONArray();
    for (Group group : groups) {
      if (matchGroup(group, key)) {
        jsChilds.put(toJson(group));
      }
      if (i == limit) {
        break;
      }
      ++i;
    }
    return jsChilds;
  }
  
  private static JSONArray concatArray(JSONArray... arrs) throws JSONException {
    JSONArray result = new JSONArray();
    for (JSONArray arr : arrs) {
      for (int i = 0; i < arr.length(); i++) {
        result.put(arr.get(i));
      }
    }
    return result;
  }

  private boolean matchGroup(Group group, String key) {
    if (group.getId().replace("/", "").startsWith(key)) {
      return true;
    }
    if (group.getLabel().toLowerCase().startsWith(key)) {
      return true;
    }
    return false;
  }

  private JSONObject toJson(User user) throws JSONException {
    JSONObject object = new JSONObject();
    String name = user.getDisplayName();
    if (UserHelper.isEmpty(name)) {
      name = user.getFirstName() + " " + user.getLastName();
    }
    object.put("id", user.getUserName());
    object.put("name", name);
    object.put("avatar", "");
    //
    return object;
  }

  private JSONObject toJson(Group group) throws JSONException {
    JSONObject object = new JSONObject();
    object.put("id", group.getId());
    object.put("name", group.getLabel());
    object.put("avatar", "");
    //
    return object;
  }
  
  @Override
  public void updateSelect(String value) {
    addValue((WebuiRequestContext) WebuiRequestContext.getCurrentInstance(), value);
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
    if (!UserHelper.isEmpty(requestURL)) {
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
      uiUserSelector.setLimitGroupId(permissionPanel.getLimitGroupId());
      uiUserSelector.setComponent(permissionPanel);
      //
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
      UIGroupSelector group = uiPopupContainer.createUIComponent(UIGroupSelector.class, null, GROUP_SELECTOR_ID);
      group.setComponent(permissionPanel);
      group.getChild(UITree.class).setId(TREE_GROUP_ID);
      group.getChild(org.exoplatform.webui.core.UIBreadcumbs.class).setId(BREADCUMB_GROUP_ID);
      group.setType("1");
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
    this.isEditMode = true;
    return this;
  }

  
}
