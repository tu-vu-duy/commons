package org.exoplatform.commons.notification.storage;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.WebNotificationFilter;
import org.exoplatform.commons.api.notification.service.storage.WebNotificationStorage;
import org.exoplatform.commons.notification.BaseNotificationTestCase;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

public class CachedWebNotificationStorageTest extends BaseNotificationTestCase {
  private WebNotificationStorage cachedStorage;
  private final static String WEB_NOTIFICATION_CACHING_NAME = "WebNotificationCaching";
  private final static String LIST_WEB_NOTIFICATION_CACHING_NAME = "WebNotificationsCaching";
  //
  @Override
  public void setUp() throws Exception {
    initCollaborationWorkspace();
    super.setUp();
    cachedStorage = getService(WebNotificationStorage.class);
  }
  
  @Override
  public void tearDown() throws Exception {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    for (String userId : userIds) {
      Node userNodeApp = nodeHierarchyCreator.getUserApplicationNode(sessionProvider, userId);
      if (userNodeApp.hasNode(NOTIFICATIONS)) {
        userNodeApp.getNode(NOTIFICATIONS).remove();
        userNodeApp.save();
      }
    }
    CacheService cacheService = getService(CacheService.class);
    cacheService.getCacheInstance(WEB_NOTIFICATION_CACHING_NAME).clearCache();
    cacheService.getCacheInstance(LIST_WEB_NOTIFICATION_CACHING_NAME).clearCache();
    
    super.tearDown();
  }
  
  public void testSave() throws Exception {
    String userId = "demo";
    userIds.add(userId);
    NotificationInfo info = makeWebNotificationInfo(userId);
    cachedStorage.save(info);
    //
    NotificationInfo notifInfo = cachedStorage.get(info.getId());
    assertNotNull(notifInfo);
    assertEquals(1, cachedStorage.get(new WebNotificationFilter(userId, false), 0, 10).size());
  }
  
  public void testRemove() throws Exception {
    String userId = "demo";
    userIds.add(userId);
    NotificationInfo info = makeWebNotificationInfo(userId);
    cachedStorage.save(info);
    //
    NotificationInfo notifInfo = cachedStorage.get(info.getId());
    assertEquals(1, cachedStorage.get(new WebNotificationFilter(userId, false), 0 , 10).size());
    
    NotificationInfo info1 = makeWebNotificationInfo(userId);
    cachedStorage.save(info1);
    //
    notifInfo = cachedStorage.get(info1.getId());
    assertNotNull(notifInfo);
    assertEquals(2, cachedStorage.get(new WebNotificationFilter(userId, false), 0 , 10).size());
    assertEquals(2, cachedStorage.get(new WebNotificationFilter(userId, true), 0 , 10).size());
    
    cachedStorage.remove(info1.getId());
    
    assertNull(cachedStorage.get(info1.getId()));
    assertEquals(1, cachedStorage.get(new WebNotificationFilter(userId, false), 0 , 10).size());
    assertEquals(1, cachedStorage.get(new WebNotificationFilter(userId, true), 0 , 10).size());
  }
  
  public void testRead() throws Exception {
    String userId = "demo";
    userIds.add(userId);
    NotificationInfo info = makeWebNotificationInfo(userId);
    cachedStorage.save(info);
    //
    NotificationInfo notifInfo = cachedStorage.get(info.getId());
    assertFalse(Boolean.valueOf(notifInfo.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())));
    //
    cachedStorage.markRead(notifInfo.getId());
    //
    notifInfo = cachedStorage.get(info.getId());
    assertTrue(Boolean.valueOf(notifInfo.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())));
  }

  public void testMarkAllRead() throws Exception {
    String userId = "demo";
    userIds.add(userId);
    for (int i = 0; i < 10; i++) {
      cachedStorage.save(makeWebNotificationInfo(userId));
    }
    //
    List<NotificationInfo> onPopoverInfos = cachedStorage.get(new WebNotificationFilter(userId, true), 0 , 10);
    assertEquals(10, onPopoverInfos.size());
    for (NotificationInfo notifInfo : onPopoverInfos) {
      assertFalse(Boolean.valueOf(notifInfo.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())));
    }
    List<NotificationInfo> viewAllInfos = cachedStorage.get(new WebNotificationFilter(userId, false), 0 , 10);
    assertEquals(10, viewAllInfos.size());
    for (NotificationInfo notifInfo : viewAllInfos) {
      assertFalse(Boolean.valueOf(notifInfo.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())));
    }
    
    //
    cachedStorage.markAllRead(userId);
    //
    onPopoverInfos = cachedStorage.get(new WebNotificationFilter(userId, true), 0 , 10);
    assertEquals(10, onPopoverInfos.size());
    for (NotificationInfo notifInfo : onPopoverInfos) {
      assertTrue(Boolean.valueOf(notifInfo.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())));
    }
    viewAllInfos = cachedStorage.get(new WebNotificationFilter(userId, false), 0 , 10);
    assertEquals(10, viewAllInfos.size());
    for (NotificationInfo notifInfo : viewAllInfos) {
      assertTrue(Boolean.valueOf(notifInfo.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())));
    }
  }

  public void testHidePopover() {
    String userId = "demo";
    userIds.add(userId);
    NotificationInfo info = makeWebNotificationInfo(userId);
    cachedStorage.save(info);
    //
    NotificationInfo notifInfo = cachedStorage.get(info.getId());
    assertTrue(Boolean.valueOf(notifInfo.getValueOwnerParameter(NotificationMessageUtils.SHOW_POPOVER_PROPERTY.getKey())));
    //
    List<NotificationInfo> infos = cachedStorage.get(new WebNotificationFilter(userId, true), 0 , 10);
    assertEquals(infos.get(0), notifInfo);
    assertTrue(Boolean.valueOf(infos.get(0).getValueOwnerParameter(NotificationMessageUtils.SHOW_POPOVER_PROPERTY.getKey())));
    //
    cachedStorage.hidePopover(notifInfo.getId());
    //
    notifInfo = cachedStorage.get(info.getId());
    assertFalse(Boolean.valueOf(notifInfo.getValueOwnerParameter(NotificationMessageUtils.SHOW_POPOVER_PROPERTY.getKey())));
    //
    infos = cachedStorage.get(new WebNotificationFilter(userId, true), 0 , 10);
    assertEquals(0, infos.size());
  }
  
  
  public void testUpdate() throws Exception {
    String userId = "mary";
    userIds.add(userId);
    NotificationInfo info = makeWebNotificationInfo(userId);
    cachedStorage.save(info);
    //
    NotificationInfo createdFirstInfo = cachedStorage.get(info.getId());
    assertEquals(info.getTitle(), createdFirstInfo.getTitle());
    for (int i = 0; i < 5; i++) {
      cachedStorage.save(makeWebNotificationInfo(userId));
    }
    List<NotificationInfo> onPopoverInfos = cachedStorage.get(new WebNotificationFilter(userId, true), 0 , 10);
    assertEquals(6, onPopoverInfos.size());
    List<NotificationInfo> viewAllInfos = cachedStorage.get(new WebNotificationFilter(userId, false), 0 , 10);
    assertEquals(6, viewAllInfos.size());
    //
    NotificationInfo lastOnPopoverInfo = onPopoverInfos.get(onPopoverInfos.size() - 1);
    assertEquals(createdFirstInfo.getId(), lastOnPopoverInfo.getId());
    NotificationInfo lastViewAllInfos = onPopoverInfos.get(onPopoverInfos.size() - 1);
    assertEquals(createdFirstInfo.getId(), lastViewAllInfos.getId());
    //
    String newTitle = "The new title";
    createdFirstInfo.setTitle(newTitle);
    //
    cachedStorage.update(createdFirstInfo);
    //
    createdFirstInfo = cachedStorage.get(info.getId());
    //
    assertEquals(newTitle, createdFirstInfo.getTitle());
    //
    onPopoverInfos = cachedStorage.get(new WebNotificationFilter(userId, true), 0 , 10);
    viewAllInfos = cachedStorage.get(new WebNotificationFilter(userId, false), 0 , 10);
    //
    NotificationInfo firstOnPopoverInfo = onPopoverInfos.get(0);
    assertEquals(newTitle, firstOnPopoverInfo.getTitle());
    NotificationInfo firstViewAllInfos = viewAllInfos.get(0);
    assertEquals(newTitle, firstViewAllInfos.getTitle());
  }
}
