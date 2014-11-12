package org.exoplatform.webui.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.exoplatform.webui.form.CompletionBean;
import org.exoplatform.webui.form.UIAutoCompletionInput;
import org.exoplatform.webui.form.UIForm;
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
  
  private static ConcurrentHashMap<String, JSONArray> caches = new ConcurrentHashMap<>();
  
  private String limitedGroup = "";
  protected boolean isEditMode = false;
  private static Map<String, String> membershipData;

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
    String KEY = "@user_" + key;
    if(caches.containsKey(KEY)) {
      return caches.get(KEY);
    }
    
    if (limit == 0) {
      limit = 10;
    }
    FilterType[] types = new FilterType[] { FilterType.USER_NAME, FilterType.FIRST_NAME, FilterType.LAST_NAME };
    List<User> users = new ArrayList<>();
    for (int i = 0; i < types.length && users.size() < limit; i++) {
      User[] arr = filterUser(key, limit, types[i]);
      for (int j = 0; j < arr.length && users.size() < limit; j++) {
        if (!containUser(users, arr[j])) {
          users.add(arr[j]);
        }
      }
    }
    JSONArray jsChilds = new JSONArray();
    for (User user : users) {
      jsChilds.put(toJson(user));
    }
    //
    caches.put(KEY, jsChilds);
    //
    return jsChilds;
  }

  private User[] filterUser(String key, int limit, FilterType type) throws Exception {
    if (limit == 0) {
      limit = 10;
    }
    ListAccess<User> listAccess = UserHelper.searchUser(new UserFilter(key, type).setGroupId(getLimitGroupId()));
    int maxSize = listAccess.getSize();
    if (limit > maxSize) {
      limit = maxSize;
    }
    return listAccess.load(0, limit);
  }

  public JSONArray filterGroup(String key, int limit) throws Exception {
    String KEY = "@group_" + key;
    if(caches.containsKey(KEY)) {
      return caches.get(KEY);
    }
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
    //
    caches.put(KEY, jsChilds);
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

  private static boolean containUser(List<User> users, User user) {
    for (User user_ : users) {
      if (user_.getUserName().equals(user)) {
        return true;
      }
    }
    return false;
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

  public static String getMembershipType(String membershipType) {
    return (membershipType.equals("*")) ? "any" : membershipType;
  }

  public static Map<String, String> buildMembershipData() {
    if (membershipData == null
        || !(WebuiRequestContext.getCurrentInstance().getLocale().equals(UIAutoCompletionInput.getLocale()))) {
      membershipData = new HashMap<String, String>();
      for (String membershipType : UserHelper.getMembershipTypes()) {
        membershipType = getMembershipType(membershipType);
        membershipData.put(membershipType, UIAutoCompletionInput.getCommonLabel("UIPermissionSelector.membership." + membershipType));
      }
    }
    //
    return membershipData;
  }  

  private JSONObject buildMembershipJson() throws JSONException {
    JSONObject memberships = new JSONObject();
    Map<String, String> membershipData = buildMembershipData();
    for (String membershipType : UserHelper.getMembershipTypes()) {
      memberships.put(membershipType, membershipData.get(membershipType));
    }
    //
    return memberships;
  }

  @Override
  public void updateSelect(String value) {
    addValue((WebuiRequestContext) WebuiRequestContext.getCurrentInstance(), value);
  }
  
  public UIPermissionContainer initContainer(String id, String requestURL, String defaultValue) {
    super.setId(id);
    //
    if (getUIPermissionSelectorInput() == null) {
      UIAutoCompletionInput<GroupCompletionBean> input = new UIAutoCompletionInput<GroupCompletionBean>(PERMISSION_INPUT + id, PERMISSION_INPUT + id, defaultValue);
      input.setCompletionBean(new GroupCompletionBean());
      //
      addChild(input);
    }
    //
    caches.clear();
    //
    return setRequestURL(requestURL);
  }
  
  protected void updateSetting() {
    JSONObject settings = new JSONObject();
    try {
      settings.put("memberships", buildMembershipJson());
    } catch (JSONException e) {
      LOG.debug("Can not build membership json", e);
    }
    getUIPermissionSelectorInput().setSettings(settings);
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

  public JSONObject getSettings() {
    return getUIPermissionSelectorInput().getSettings();
  }

  public void setSettings(JSONObject settings) {
    getUIPermissionSelectorInput().setSettings(settings);
  }

  public UIPermissionContainer setValue(String value) {
    getUIPermissionSelectorInput().setValue(value);
    return this;
  }

  public String getValue() {
    return getUIPermissionSelectorInput().getValue();
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
    return UIAutoCompletionInput.getCommonLabel(key);
  }
  
  private UIAutoCompletionInput<?> getUIPermissionSelectorInput() {
    return getChild(UIAutoCompletionInput.class);
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
      permissionPanel.isEditMode = true;
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
      permissionPanel.isEditMode = true;
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

  private class GroupCompletionBean extends CompletionBean {
    private boolean isUser = true;
    private String membershipType;
    private String groupId;
    @Override
    public void buildData() {
      this.isUser = (id.indexOf("/") < 0);
      if (isUser) {
        try {
          User user = UserHelper.getUserHandler().findUserByName(this.id);
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
          Group group = UserHelper.getGroupHandler().findGroupById(groupId);
          label = group.getLabel();
        } catch (Exception e) {
          this.label = id;
        }
      }
    }

    @Override
    public String getJSObject() {
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

    @Override
    public String getDisplay() {
      StringBuilder builder = new StringBuilder("<strong>");
      if(isUser) {
        builder.append(label);
      } else {
        builder.append(membershipType).append(" ")
               .append(UIAutoCompletionInput.getCommonLabel("UIPermissionSelector.label.in"))
               .append(" ")
               .append(label);
      }
      return builder.append("</strong> (").append(id).append(")").toString();
    }
    
  }
}
