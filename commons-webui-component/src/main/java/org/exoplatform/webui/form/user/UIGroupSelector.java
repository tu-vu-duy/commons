/**
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 **/
package org.exoplatform.webui.form.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIBreadcumbs;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.organization.UIGroupMembershipSelector;

@ComponentConfigs({ 
  @ComponentConfig(
      template = "classpath:groovy/webui/commons/UIGroupSelector.gtmpl",
      events = {
          @EventConfig(listeners = UIGroupSelector.ChangeNodeActionListener.class),
          @EventConfig(listeners = UIGroupSelector.SelectMembershipActionListener.class),
          @EventConfig(listeners = UIGroupSelector.SelectPathActionListener.class)  
      }  
  ),
  @ComponentConfig(
      type = UITree.class, id = "UITreeGroupSelector",
      template = "system:/groovy/webui/core/UITree.gtmpl",
      events = @EventConfig(listeners = UITree.ChangeNodeActionListener.class)
  ),
  @ComponentConfig(
      type = UIBreadcumbs.class, id = "BreadcumbGroupSelector",
      template = "system:/groovy/webui/core/UIBreadcumbs.gtmpl",
      events = @EventConfig(listeners = UIBreadcumbs.SelectPathActionListener.class)
  )
 })
public class UIGroupSelector extends UIGroupMembershipSelector implements UIPopupComponent {
  final public static String TYPE_USER = "0".intern() ;
  final public static String TYPE_MEMBERSHIP = "1".intern() ;
  final public static String TYPE_GROUP = "2".intern() ;
  final public static String TREE_GROUP_ID = "UITreeGroupSelector";
  final public static String BREADCUMB_GROUP_ID = "BreadcumbGroupSelector";

  private UISelector<String>         uiComponent;
  private String              type_           = "1";
  private List<Group>         selectedGroup_;
  private String              spaceGroupId    = null;
  private String              spaceParentId   = null;

  private OrganizationService service;

  public UIGroupSelector() throws Exception {
    service = UserHelper.getOrganizationService();
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    if(!UserHelper.isEmpty(spaceGroupId)) {
      UITree uiTree = getChild(UITree.class);
      Group parentGroup = (Group)uiTree.getParentSelected();
      if(parentGroup == null) {
        uiTree.setParentSelected(null);
      }
    }
    super.processRender(context);
  }
  
  public UISelector<String> getReturnComponent() {
    return uiComponent;
  }

  public void setComponent(UISelector<String> uicomponent) {
    uiComponent = uicomponent;
  }

  public List<Group> getChildGroup() throws Exception {
    return UserHelper.findGroups(getCurrentGroup());
  }

  public boolean isSelectGroup() {
    return TYPE_GROUP.equals(type_);
  }

  public boolean isSelectUser() {
    return TYPE_USER.equals(type_);
  }

  public boolean isSelectMemberShip() {
    return TYPE_MEMBERSHIP.equals(type_);
  }

  @SuppressWarnings("unchecked")
  public List<String> getList() throws Exception {
    List<String> children = new ArrayList<String>();
    if (isSelectUser()) {
      ListAccess<User> userPageList = service.getUserHandler().findUsersByGroupId(getCurrentGroup().getId());
      User users[] = userPageList.load(0, userPageList.getSize());
      for (int i = 0; i < userPageList.getSize(); i++) {
        children.add(users[i].getUserName());
      }
    } else if (isSelectMemberShip()) {
      for (String child : getListMemberhip()) {
        children.add(child);
      }
    } else if (isSelectGroup() && getCurrentGroup() != null) {
      if (!UserHelper.isEmpty(spaceGroupId) && getCurrentGroup().getId().equals(spaceParentId)) {
        Group group = service.getGroupHandler().findGroupById(spaceGroupId);
        children.add(group.getGroupName());
      } else {
        Collection<Group> groups = service.getGroupHandler().findGroups(getCurrentGroup());
        if (groups.size() > 0) {
          for (Group child : groups) {
            children.add(child.getGroupName());
          }
        } else {
          children.add("selectThisGroup");
        }
      }
    }
    return children;
  }

  public void setSelectedGroups(List<Group> groups) {
    if (groups != null) {
      selectedGroup_ = groups;
      getChild(UITree.class).setSibbling(selectedGroup_);
    }
  }

  public void setSpaceGroupId(String groupId) throws Exception {
    if (!UserHelper.isEmpty(groupId)) {
      Group group = service.getGroupHandler().findGroupById(groupId);
      if (group != null) {
        this.spaceGroupId = groupId;
        selectedGroup_ = new ArrayList<Group>();
        selectedGroup_.add(group);
        spaceParentId = group.getParentId();
        changeGroup(spaceParentId);
      }
    } else {
      setSelectedGroups(null);
    }
  }
  public void changeGroup(String groupId) throws Exception {
    super.changeGroup(groupId);
    if (selectedGroup_ != null) {
      UITree tree = getChild(UITree.class);
      tree.setSibbling(selectedGroup_);
      tree.setChildren(null);
    }
  }

  public void activate() {
  }

  public void deActivate() {
  }

  public void setType(String type) {
    this.type_ = type;
  }

  public String getType() {
    return type_;
  }

  static public class SelectMembershipActionListener extends EventListener<UIGroupSelector> {
    public void execute(Event<UIGroupSelector> event) throws Exception {
      String membership = event.getRequestContext().getRequestParameter(OBJECTID);
      UIGroupSelector uiGroupSelector = event.getSource();
      uiGroupSelector.getReturnComponent().updateSelect(membership);
      try {
        UIPopupContainer popupContainer = uiGroupSelector.getAncestorOfType(UIPopupContainer.class);
        popupContainer.cancelPopupAction();
        event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
      } catch (NullPointerException e) {
        UIPopupWindow uiPopup = uiGroupSelector.getAncestorOfType(UIPopupWindow.class);
        uiPopup.setShow(false);
        uiPopup.setUIComponent(null);
        uiPopup.setRendered(false);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
      }
    }
  }

  static public class ChangeNodeActionListener extends EventListener<UITree> {
    public void execute(Event<UITree> event) throws Exception {
      UIGroupSelector uiGroupSelector = event.getSource().getAncestorOfType(UIGroupSelector.class);
      String groupId = event.getRequestContext().getRequestParameter(OBJECTID);
      uiGroupSelector.changeGroup(groupId);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiGroupSelector);
    }
  }

  static public class SelectPathActionListener extends EventListener<UIBreadcumbs> {
    public void execute(Event<UIBreadcumbs> event) throws Exception {
      UIGroupSelector uiGroupSelector = event.getSource().getParent();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      uiGroupSelector.changeGroup(objectId);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiGroupSelector);
    }
  }
}