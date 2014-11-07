/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.webui.form.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.services.organization.User;
import org.exoplatform.web.application.AbstractApplicationMessage;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.common.utils.UserListAccess;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.container.UIPageIterator;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.user.UserHelper.FilterType;
import org.exoplatform.webui.form.user.UserHelper.UserFilter;
import org.exoplatform.webui.organization.account.UIGroupSelector;

@ComponentConfig(
  lifecycle = UIFormLifecycle.class,
  template = "system:/groovy/webui/organization/account/UIUserSelector.gtmpl",
  events = {
    @EventConfig(listeners = UIUserSelector.AddActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelector.AddUserActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelector.SearchActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelector.SearchGroupActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelector.SelectGroupActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelector.FindGroupActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelector.ShowPageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIUserSelector.CloseActionListener.class, phase = Phase.DECODE)
  }
)
  
public class UIUserSelector extends UIForm implements UIPopupComponent  {

  public static final String   ID_GROUP_SELECTOR = "UIPopupGroupSelector";
  public static final String   FIELD_KEYWORD     = "QuickSearch";
  public static final String   FIELD_FILTER      = "filter";
  public static final String   FIELD_GROUP       = "group";
  public static final String   USER_NAME         = FilterType.USER_NAME.getName();
  public static final String   LAST_NAME         = FilterType.LAST_NAME.getName();
  public static final String   FIRST_NAME        = FilterType.FIRST_NAME.getName();
  public static final String   EMAIL             = FilterType.EMAIL.getName();

  protected Map<String, User>  userData_          = new HashMap<String, User>();

  private boolean              isShowSearch_     = false;
  private boolean              isShowSearchGroup = false;
  private boolean              isShowSearchUser  = true;
  private boolean              isMultiSelect     = true;

  private String               selectedUsers;
  private String               permisionType;
  private String               selectedGroupId    = null;
  private int                  numberUserDisplay  = 10; 
  private UISelector<String>   uiComponent;

  private List<User>           itemOfCurrentPage            = new ArrayList<User>();

  public UIPageIterator<User>  uiIterator_;
  public UIUserSelector() throws Exception {
    addUIFormInput(new UIFormStringInput(FIELD_KEYWORD, FIELD_KEYWORD, null));
    addUIFormInput(new UIFormSelectBox(FIELD_FILTER, FIELD_FILTER, getFilters()));
    addUIFormInput(new UIFormStringInput(FIELD_GROUP, FIELD_GROUP, null));
    isShowSearch_ = true;
    //
    uiIterator_ = new UIPageIterator<User>();
    uiIterator_.setId("UISelectUserPage");
    //
    defaultUserList();
    //
    addChild(UIPopupWindow.class, null, ID_GROUP_SELECTOR).setWindowSize(540, 0);
    
    setActions(new String[] { "Add", "Close" });
  }

  @Override
  public String getLabel(String labelID)  {
    ResourceBundle res = null;
    try {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      res = context.getApplicationResourceBundle();
      return super.getLabel(res, labelID);
    } catch (Exception e) {
      return labelID;
    }
  }

  @Override
  public void activate() {
  }

  @Override
  public void deActivate() {
  }
  
  public UISelector<String> getReturnComponent() {
    return uiComponent;
  }

  public void setComponent(UISelector<String> uicomponent) {
    uiComponent = uicomponent;
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    if (UserHelper.isEmpty(getKeyword()) && !UserHelper.isEmpty(selectedGroupId) && uiIterator_ != null) {
      ListAccess<User> listAccess = UserHelper.searchUser(new UserFilter("", null).setGroupId(selectedGroupId));
      uiIterator_.setListAccess(new UserListAccess(listAccess), numberUserDisplay);
    }
    //
    initResult();
    super.processRender(context);
  }

  private void defaultUserList() throws Exception {
    ListAccess<User> listAccess = UserHelper.searchUser(new UserFilter("", null));
    uiIterator_.setListAccess(new UserListAccess(listAccess), numberUserDisplay);
  }

  private void initResult() throws Exception {
    this.itemOfCurrentPage = uiIterator_.getCurrentPageData();
    if (isMultiSelect()) {
      UICheckBoxInput checkBox;
      for (User user : itemOfCurrentPage) {
        checkBox = getUICheckBoxInput(user.getUserName());
        if (checkBox == null) {
          checkBox = new UICheckBoxInput(user.getUserName(), user.getUserName(), false);
          addUIFormInput(checkBox);
        }
        //
        checkBox.setChecked(uiIterator_.isSelectedItem(user.getUserName()));
      }
    }
    //
    if (isShowSearchGroup) {
      UIPopupWindow uiPopup = getChild(UIPopupWindow.class);
      if (uiPopup.getUIComponent() == null) {
        UIGroupSelector uiGroup = createUIComponent(UIGroupSelector.class, null, null);
        uiPopup.setUIComponent(uiGroup);
        uiGroup.setId("GroupSelector");
        uiGroup.getChild(UITree.class).setId("TreeGroupSelector");
        uiGroup.getChild(UIBreadcumbs.class).setId("BreadcumbsGroupSelector");
      }
    }
  }

  protected List<User> getData() {
    return itemOfCurrentPage;
  }

  public String getSelectedUsers() {
    return selectedUsers;
  }

  public void setSelectedUsers(String selectedUsers) {
    this.selectedUsers = selectedUsers;
  }

  public void setMultiSelect(boolean isMultiSelect) {
    this.isMultiSelect = isMultiSelect;
  }

  public boolean isMultiSelect() {
    return isMultiSelect;
  }

  protected boolean getMulti() {
    return isMultiSelect;
  }

  public UIPageIterator<User> getUIPageIterator() {
    return uiIterator_;
  }

  public int getAvailablePage() throws Exception {
    return uiIterator_.getAvailablePage();
  }

  public int getCurrentPage() throws Exception {
    return uiIterator_.getCurrentPage();
  }
  
  public void setNumberUserDisplay(int numberUserDisplay) {
    this.numberUserDisplay = numberUserDisplay;
  }

  public String getLimitGroupId() {
    return selectedGroupId;
  }

  public void setLimitGroupId(String spaceGroupId) {
    this.selectedGroupId = spaceGroupId;
  }

  public String getPermisionType() {
    return permisionType;
  }

  public void setPermisionType(String permisionType) {
    this.permisionType = permisionType;
  }

  private List<SelectItemOption<String>> getFilters() {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(getLabel(USER_NAME), USER_NAME));
    options.add(new SelectItemOption<String>(getLabel(LAST_NAME), LAST_NAME));
    options.add(new SelectItemOption<String>(getLabel(FIRST_NAME), FIRST_NAME));
    options.add(new SelectItemOption<String>(getLabel(EMAIL), EMAIL));
    return options;
  }

  public void setShowSearch(boolean isShowSearch) {
    this.isShowSearch_ = isShowSearch;
  }

  public boolean isShowSearch() {
    return isShowSearch_;
  }

  public void setShowSearchGroup(boolean isShowSearchGroup) {
    this.isShowSearchGroup = isShowSearchGroup;
  }

  public boolean isShowSearchGroup() {
    return isShowSearchGroup;
  }

  public void setShowSearchUser(boolean isShowSearchUser) {
    this.isShowSearchUser = isShowSearchUser;
  }

  public boolean isShowSearchUser() {
    return isShowSearchUser;
  }

  public String getFilterGroup() {
    return getUIStringInput(FIELD_GROUP).getValue();
  }

  public void setFilterGroup(String finterGroupId) {
    getUIStringInput(FIELD_GROUP).setValue(finterGroupId);
  }

  public static class AddActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiUserSelector = event.getSource();
      uiUserSelector.setSelectedItem();
      // get item from selected item map
      Set<String> items = uiUserSelector.uiIterator_.getSelectedItems();
      if (items.size() == 0) {
        WebuiRequestContext context = event.getRequestContext();
        context.getUIApplication().addMessage(new ApplicationMessage("UIUserSelect.msg.user-required", new String[]{}, AbstractApplicationMessage.WARNING));
        ((PortalRequestContext) context.getParentAppRequestContext()).ignoreAJAXUpdateOnPortlets(true);
        return;
      }
      String result = items.toString().replace("[", "").replace("]", "").replaceAll(" ", "");

      uiUserSelector.setSelectedUsers(result);

      uiUserSelector.getReturnComponent().updateSelect(result);

      uiUserSelector.<UIComponent> getParent().broadcast(event, event.getExecutionPhase());
      //
      UIPopupContainer popupContainer = uiUserSelector.getAncestorOfType(UIPopupContainer.class);
      if (popupContainer != null) {
        uiUserSelector.getAncestorOfType(UIPopupContainer.class).cancelPopupAction();
      }
    }
  }

  public static class AddUserActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiForm = event.getSource();
      String userName = event.getRequestContext().getRequestParameter(OBJECTID);
      uiForm.setSelectedUsers(userName);
      uiForm.<UIComponent> getParent().broadcast(event, event.getExecutionPhase());
    }
  }

  protected void updateCurrentPage(int page) throws Exception {
    uiIterator_.setCurrentPage(page);
  }

  public void setKeyword(String value) {
    getUIStringInput(FIELD_KEYWORD).setValue(value);
  }

  public String getKeyword() {
    return getUIStringInput(FIELD_KEYWORD).getValue();
  }

  private void setSelectedItem() throws Exception {
    for (User user : itemOfCurrentPage) {
      UICheckBoxInput input = getUICheckBoxInput(user.getUserName());
      if (input != null) {
        uiIterator_.setSelectedItem(user.getUserName(), input.isChecked());
      }
    }
  }

  public static class SelectGroupActionListener extends EventListener<UIGroupSelector> {
    public void execute(Event<UIGroupSelector> event) throws Exception {
      UIGroupSelector uiSelectGroup = event.getSource();
      UIUserSelector uiSelectUserForm = uiSelectGroup.<UIComponent> getParent().getParent();
      uiSelectUserForm.selectedGroupId = event.getRequestContext().getRequestParameter(OBJECTID);
      uiSelectUserForm.setFilterGroup(uiSelectUserForm.selectedGroupId);
      uiSelectUserForm.setKeyword("");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSelectUserForm);
    }
  }

  public static class FindGroupActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiUserSelector = event.getSource();
      String groupId = uiUserSelector.getFilterGroup();
      if (!UserHelper.isEmpty(groupId)) {
        if (UserHelper.getOrganizationService().getGroupHandler().findGroupById(groupId) != null) {
          uiUserSelector.selectedGroupId = groupId;
        }
      } else {
        //
        uiUserSelector.defaultUserList();
      }

      uiUserSelector.setKeyword("");
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUserSelector);
    }
  }

  static public class SearchActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiForm = event.getSource();
      String type = uiForm.getUIFormSelectBox(FIELD_FILTER).getValue();
      FilterType filterType = FilterType.getType(type);
      //
      if (filterType == null) {
        return;
      }
      //
      String keyword = uiForm.getKeyword();
      String groupId = uiForm.getLimitGroupId();
      
      UserFilter userFilter = new UserFilter(keyword, filterType);
      uiForm.uiIterator_.setListAccess(new UserListAccess(UserHelper.searchUser(userFilter.setGroupId(groupId))), uiForm.numberUserDisplay);
      //
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }
  
  public static class CloseActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiUserSelector = event.getSource();
      uiUserSelector.<UIComponent> getParent().broadcast(event, event.getExecutionPhase());
      //
      UIPopupContainer popupContainer = uiUserSelector.getAncestorOfType(UIPopupContainer.class);
      if (popupContainer != null) {
        uiUserSelector.getAncestorOfType(UIPopupContainer.class).cancelPopupAction();
      }
    }
  }

  public static class SearchGroupActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiForm = event.getSource();
      uiForm.getChild(UIPopupWindow.class).setShow(true);
    }
  }

  public static class ShowPageActionListener extends EventListener<UIUserSelector> {
    public void execute(Event<UIUserSelector> event) throws Exception {
      UIUserSelector uiSelectUserForm = event.getSource();
      uiSelectUserForm.setSelectedItem();

      int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID));
      uiSelectUserForm.updateCurrentPage(page);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSelectUserForm);
    }
  }

}
