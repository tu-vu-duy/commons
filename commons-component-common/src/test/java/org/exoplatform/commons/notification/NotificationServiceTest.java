package org.exoplatform.commons.notification;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.model.UserSetting.FREQUENCY;
import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.commons.api.notification.node.TreeNode;
import org.exoplatform.commons.api.notification.service.NotificationService;
import org.exoplatform.commons.api.notification.service.QueueMessage;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.api.notification.service.storage.NotificationDataStorage;
import org.exoplatform.commons.notification.impl.service.ExtendedNotificationService;
import org.exoplatform.commons.notification.mock.MockMailService;
import org.exoplatform.commons.testing.BaseCommonsTestCase;

public class NotificationServiceTest extends BaseCommonsTestCase {
  
  private NotificationService notificationService;
  
  private NotificationDataStorage notificationDataStorage;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    notificationService = getService(NotificationService.class);
    assertNotNull(notificationService);
    
    notificationDataStorage = getService(NotificationDataStorage.class);
    assertNotNull(notificationDataStorage);
  }
  
  @Override
  public void tearDown() throws Exception {
    // remove all notification node
    Node homeNode = (Node) session.getItem("/eXoNotification/messageHome");
    NodeIterator iterator = homeNode.getNodes();
    while (iterator.hasNext()) {
      Node node = (iterator.nextNode());
      node.remove();
    }
    session.save();
    super.tearDown();
  }
  
  private NotificationInfo saveNotification() throws Exception {
    NotificationInfo notification = NotificationInfo.instance();
    Map<String, String> params = new HashMap<String, String>();
    params.put("objectId", "idofobject");
    notification.key("TestPlugin").setSendToDaily("root")
                .setSendToWeekly("demo").setOwnerParameter(params).setOrder(1);
    notificationDataStorage.save(notification);
    addMixin(notification);
    return notification;
  }

  public void testSave() throws Exception {
    NotificationInfo notification = saveNotification();
    //
    Node node = getMessageNodeByKeyIdAndParam("TestPlugin", "objectId=idofobject");
    assertNotNull(node);
    
    NotificationInfo notification2 = fillModel(node);
    
    assertTrue(notification2.equals(notification));
    
  }
  
  public void testNormalGetByUserAndRemoveMessagesSent() throws Exception {
    NotificationConfiguration configuration = getService(NotificationConfiguration.class);
    configuration.setSendWeekly(false);
    NotificationInfo notification = saveNotification();
    UserSetting userSetting = UserSetting.getInstance();
    userSetting.setUserId("root")
               .addProvider("TestPlugin", FREQUENCY.DAILY);
    userSetting.setActive(true);
    
    TreeNode treeNode = notificationDataStorage.getByUser(userSetting);
    
    List<NTFInforkey> list = treeNode.getNFTInforkeys(new NotificationKey("TestPlugin"));
    assertEquals(1, list.size());
    
    assertEquals(list.get(0), new NTFInforkey(notification.getId()) );
    
    // after sent, user demo will auto remove from property daily
    notificationService.processEvents();
    
    Node node = getMessageNodeByKeyIdAndParam("TestPlugin", "objectId=idofobject");
    assertNotNull(node);
    
    NotificationInfo notification2 = fillModel(node);
    
    assertEquals(0, notification2.getSendToDaily().length);
    
    configuration.setSendWeekly(true);
    userSetting.setUserId("demo").addProvider("TestPlugin", FREQUENCY.WEEKLY);
    treeNode = notificationDataStorage.getByUser(userSetting);
    list = treeNode.getNFTInforkeys(new NotificationKey("TestPlugin"));
    assertEquals(1, list.size());

    // after sent weekly, the node removed
    notificationService.processEvents();
    node = getMessageNodeByKeyIdAndParam("TestPlugin", "objectId=idofobject");
    assertNull(node);
  }

  public void testNormalExtendedDataStorageImpl() throws Exception {
    NotificationConfiguration configuration = getService(NotificationConfiguration.class);
    saveNotification();
    UserSetting userSetting = UserSetting.getInstance();
    userSetting.setUserId("root")
    .addProvider("TestPlugin", FREQUENCY.DAILY);
    userSetting.setActive(true);
    
    UserSettingService settingService = getService(UserSettingService.class);
    settingService.save(userSetting);
    
    ExtendedNotificationService extendService = getService(ExtendedNotificationService.class);
    
    configuration.setSendWeekly(false);
    extendService.processDigest();
    // send mails
    getService(QueueMessage.class).send();
    
    // result sent.
    System.out.println(getService(MockMailService.class).getSentUser());
    
    
    Node node = getMessageNodeByKeyIdAndParam("TestPlugin", "objectId=idofobject");
    assertNotNull(node);
    
    NotificationInfo notification = fillModel(node);
    assertEquals(0, notification.getSendToDaily().length);

    userSetting.setUserId("demo").addProvider("TestPlugin", FREQUENCY.WEEKLY);
    settingService.save(userSetting);

    // after sent weekly, the node removed
    configuration.setSendWeekly(true);
    extendService.processDigest();

    //
    node = getMessageNodeByKeyIdAndParam("TestPlugin", "objectId=idofobject");
    assertNull(node);
  }

  
  
  public void testSpecialGetByUserAndRemoveMessagesSent() throws Exception {
    NotificationConfiguration configuration = getService(NotificationConfiguration.class);
    NotificationInfo notification = NotificationInfo.instance();
    Map<String, String> params = new HashMap<String, String>();
    params.put("objectId", "idofobject");
    notification.key("TestPlugin").setSendAll(true)
                .setOwnerParameter(params).setOrder(1);
    notificationDataStorage.save(notification);
    
    UserSetting userSetting = UserSetting.getInstance();
    userSetting.setUserId("root").addProvider("TestPlugin", FREQUENCY.DAILY);
    userSetting.setActive(true);
    // Test send to daily
    configuration.setSendWeekly(false);
    TreeNode treeNode = notificationDataStorage.getByUser(userSetting);
    
    List<NTFInforkey> list = treeNode.getNFTInforkeys(new NotificationKey("TestPlugin"));
    assertEquals(1, list.size());
    
    assertEquals(list.get(0), new NTFInforkey(notification.getId()));
    // check value from node
    Node node = getMessageNodeByKeyIdAndParam("TestPlugin", "objectId=idofobject");
    assertNotNull(node);
    
    assertEquals(NotificationInfo.FOR_ALL_USER, fillModel(node).getSendToDaily()[0]);
    
    // after sent, user demo will auto remove from property daily
    notificationService.processEvents();

    // after sent, the value on on property sendToDaily will auto removed
    node = getMessageNodeByKeyIdAndParam("TestPlugin", "objectId=idofobject");
    assertEquals(0, fillModel(node).getSendToDaily().length);
    
    // Test send to weekly
    configuration.setSendWeekly(true);
    userSetting.setUserId("demo").addProvider("TestPlugin", FREQUENCY.WEEKLY);
    treeNode = notificationDataStorage.getByUser(userSetting);
    list = treeNode.getNFTInforkeys(new NotificationKey("TestPlugin"));
    assertEquals(1, list.size());
    
    // after sent, user demo will auto remove from property daily
    notificationService.processEvents();
    
    node = getMessageNodeByKeyIdAndParam("TestPlugin", "objectId=idofobject");
    assertNull(node);
  }
  
  
  private void addMixin(NotificationInfo notification) throws Exception {
    Node msgNode = getMessageNodeById(notification.getName());
    if (msgNode != null) {
      msgNode.addMixin("exo:datetime");
      msgNode.setProperty("exo:dateCreated", Calendar.getInstance());
      session.save();
      notification.setId(msgNode.getUUID());
      System.out.println(msgNode.getUUID());
    }
  }
  
  private NotificationInfo fillModel(Node node) throws Exception {
    if(node == null) return null;
    NotificationInfo message = NotificationInfo.instance()
      .setFrom(node.getProperty("ntf:from").getString())
      .setOrder(Integer.valueOf(node.getProperty("ntf:order").getString()))
      .key(node.getProperty("ntf:providerType").getString())
      .setOwnerParameter(NotificationUtils.valuesToArray(node.getProperty("ntf:ownerParameter").getValues()))
      .setSendToDaily(NotificationUtils.valuesToArray(node.getProperty("ntf:sendToDaily").getValues()))
      .setSendToWeekly(NotificationUtils.valuesToArray(node.getProperty("ntf:sendToWeekly").getValues()))
      .setName(node.getName())
      .setId(node.getUUID());
    
    return message;
  }
  
  private Node getMessageNodeById(String msgId) throws Exception {
    try {
      return session.getNodeByUUID(msgId);
    } catch (Exception e) {
      return getMessageNode(new StringBuffer("(fn:name() = '").append(msgId).append("')").toString(), "");
    }
  }

  private Node getMessageNodeByKeyIdAndParam(String key, String param) throws Exception {
    key = "/" + key;
    return getMessageNode(new StringBuilder("(ntf:ownerParameter LIKE '%").append(param).append("%')").toString(), key);
  }
  
  private Node getMessageNode(String statement, String key) throws Exception {
    StringBuilder jcrQuery = new StringBuilder("SELECT * FROM ntf:message WHERE jcr:path LIKE '");
    jcrQuery.append("/eXoNotification/messageHome").append(key).append("/%' AND ").append(statement);

    QueryManager qm = session.getWorkspace().getQueryManager();
    Query query = qm.createQuery(jcrQuery.toString(), Query.SQL);
    NodeIterator iter = query.execute().getNodes();
    return (iter.getSize() > 0) ? iter.nextNode() : null;
  }

}
