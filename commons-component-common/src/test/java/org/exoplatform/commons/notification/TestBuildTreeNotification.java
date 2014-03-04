/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.commons.notification;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.commons.api.notification.node.TreeNode;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.idgenerator.impl.IDGeneratorServiceImpl;
import org.exoplatform.services.jcr.util.IdGenerator;

public class TestBuildTreeNotification extends TestCase {

  private static Map<String, NotificationInfo> infoDatas           = new HashMap<String, NotificationInfo>();
  private static List<String>                  pluginIds           = new ArrayList<String>();
  private static List<UserSetting>             userSettings        = new ArrayList<UserSetting>();
  private static List<NTFInforkey>             inforkeys           = new ArrayList<NTFInforkey>();

  private static int                           numberInfoPerPlugin = 1000;
  private static int                           numberUser          = 1000;
  private static int                           pluginSize          = 100;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    new IdGenerator(new IDGeneratorServiceImpl());
    
    if (pluginIds.size() == 0) {
      for (int i = 0; i < pluginSize; i++) {
        pluginIds.add("TestPlugin" + i);
      }

      // make users
      List<String> users = new ArrayList<String>();
      for (int i = 0; i < numberUser; i++) {
        int t = random(pluginSize, 10);
        users.add("user" + i);
        UserSetting setting = UserSetting.getInstance();
        setting.setUserId("user" + i);
        setting.setActive(true);
        setting.setDailyProviders(pluginIds.subList(t, t + 10));
        userSettings.add(setting);
      }
      //
      createNTFInforkeys(numberInfoPerPlugin, pluginSize, users);
    }
  }

  public void testBuildHorizontalTree() {

    // make notifications
    long t = System.currentTimeMillis();
    // process build tree
    Map<String, List<NTFInforkey>> inforKeyMap = new HashMap<String, List<NTFInforkey>>();
    List<NTFInforkey> infoByPlugin;
    for (NTFInforkey ntfInforkey : inforkeys) {
      if ((infoByPlugin = inforKeyMap.get(ntfInforkey.getPluginId())) == null) {
        infoByPlugin = new ArrayList<NTFInforkey>();
        inforKeyMap.put(ntfInforkey.getPluginId(), infoByPlugin);
      }
      infoByPlugin.add(ntfInforkey);
    }

    for (UserSetting userSetting : userSettings) {
      TreeNode treeNode = new TreeNode(userSetting.getUserId());
      List<String> pluginIds = userSetting.getDailyProviders();
      treeNode.intPluginNodes(pluginIds);
      //
      for (String pluginId : pluginIds) {
        List<NTFInforkey> ntfInforkeys = inforKeyMap.get(pluginId);
        if (ntfInforkeys != null) {
          for (NTFInforkey ntfInforkey : ntfInforkeys) {
            if (isAdd(ntfInforkey, userSetting.getUserId())) {
              treeNode.add(new NotificationKey(pluginId), ntfInforkey);
            }
          }
        }
      }
    }

    System.out.println("Time to build horizontal tree: " + (System.currentTimeMillis() - t) + "ms");

  }

  public void testBuildVerticalTree() {

    long t = System.currentTimeMillis();
    //
    for (UserSetting userSetting : userSettings) {
      TreeNode treeNode = new TreeNode(userSetting.getUserId());
      List<String> pluginIds = userSetting.getDailyProviders();
      treeNode.intPluginNodes(pluginIds);
      //
      for (String pluginId : pluginIds) {
        if (isActivePlugin(pluginId)) {
          for (NotificationInfo info : infoDatas.values()) {
            if (info.getKey().getId().equals(pluginId)) {
              treeNode.add(new NotificationKey(pluginId), new NTFInforkey(info.getId()));
            }
          }
        }
      }
    }
    System.out.println("Time to build vertical tree: " + (System.currentTimeMillis() - t) + "ms");
    
  }

  private NotificationInfo getNotificationInfoById(String id) {
    return infoDatas.get(id);
  }

  private void createNTFInforkeys(int size, int pluginSize, List<String> users) {
    for (int i = 0; i < pluginSize; i++) {
      String pluginId = "TestPlugin" + i;
      TestNotificationPlugin plugin = new TestNotificationPlugin(new InitParams());
      plugin.setId(pluginId);
      int t = random(numberUser, 20);
      String[] users_ = users.subList(t, t + 20).toArray(new String[] {});
      for (int j = 0; j < size; j++) {
        NotificationInfo info = plugin.buildNotification(null);
        info.setId("id" + j);
        info.setSendToDaily(users_);
        info.setSendToWeekly(users_);
        info.key(pluginId);

        //
        NTFInforkey inforkey = new NTFInforkey(info.getId());
        inforkeys.add(inforkey.setPluginId(pluginId));

        infoDatas.put(info.getId(), info);
      }
    }
  }

  private int random(int max, int size) {
    int i = new Random().nextInt(max);
    if (i + size > max) {
      i = max - size - 1;
    }
    return i;
  }

  private boolean isActivePlugin(String pluginId) {
    return pluginIds.contains(pluginId);
  }

  private boolean isAdd(NTFInforkey ntfInforkey, String user) {
    NotificationInfo info = getNotificationInfoById(ntfInforkey.getUUID());
    List<String> users = Arrays.asList(info.getSendToDaily());
    if (users.contains(user) || users.contains(NotificationInfo.FOR_ALL_USER)) {
      return true;
    }
    return false;
  }

  public class TestNotificationPlugin extends AbstractNotificationPlugin {
    private String id = null;

    public TestNotificationPlugin(InitParams initParams) {
      super(initParams);
    }

    @Override
    protected NotificationInfo makeNotification(NotificationContext ctx) {
      return NotificationInfo.instance().to("demo").end();
    }

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      return new MessageInfo().subject("subject").body("body").end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return true;
    }

    @Override
    public boolean isValid(NotificationContext ctx) {
      return true;
    }

    @Override
    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

}
