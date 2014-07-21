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
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.service.NotificationCompletionService;
import org.exoplatform.commons.api.notification.service.setting.PluginContainer;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.api.notification.service.storage.NotificationDataStorage;
import org.exoplatform.commons.api.notification.service.storage.NotificationService;
import org.exoplatform.commons.notification.impl.AbstractService;
import org.exoplatform.commons.notification.plugin.PluginTest;

public class NotificationServiceTest extends AsbtractBaseNotificationTestCase {
  
  private NotificationService       notificationService;
  private NotificationDataStorage   notificationDataStorage;
  private PluginContainer container;
  private NotificationConfiguration configuration;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    notificationService = getService(NotificationService.class);
    assertNotNull(notificationService);

    notificationDataStorage = getService(NotificationDataStorage.class);
    assertNotNull(notificationDataStorage);

    configuration = getService(NotificationConfiguration.class);
    assertNotNull(configuration);
    configuration.setJobCurrentDay(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
    
    container = getService(PluginContainer.class);
    //
    initDefaultHomeNode();
    getService(NotificationCompletionService.class);
  }
  
  @Override
  public void tearDown() throws Exception {
    // remove all notification node
    removeChildNodes("/eXoNotification/messageHome/" + PluginTest.ID);
    removeChildNodes("/eXoNotification/messageInfoHome");
    super.tearDown();
  }

  private void removeChildNodes(String path) throws Exception {
    Node homeNode = (Node) session.getItem(path);
    NodeIterator iterator = homeNode.getNodes();
    while (iterator.hasNext()) {
      Node node = (iterator.nextNode());
      node.remove();
    }
    session.save();
  }
  
  private NotificationInfo saveNotification(String userDaily, String userWeekly) throws Exception {
    return saveNotification(userDaily, userWeekly, null);
  }

  private NotificationInfo saveNotification(String userDaily, String userWeekly, Calendar createdDate) throws Exception {
    AbstractNotificationPlugin pluginTest = container.getPlugin(new NotificationKey(PluginTest.ID));
    NotificationInfo notification = pluginTest.buildNotification(null);
    if (createdDate != null) {
      notification.setDateCreated(createdDate);
    }
    notification.setSendToDaily(userDaily)
                .setSendToWeekly(userWeekly);
    notificationDataStorage.save(notification);
    addMixin(notification.getId());
    return notification;
  }
  
  public void testServiceNotNull() throws Exception {
    assertNotNull(notificationService);
    assertNotNull(configuration);
    assertNotNull(notificationDataStorage);
    saveNotification("root", "demo");
  }

  /**
   * @throws Exception
   */
  public void testSave() throws Exception {
    NotificationInfo notification = saveNotification("root", "demo");
    
    NotificationInfo notification2 = getNotificationInfoByKeyIdAndParam(PluginTest.ID, "objectId=idofobject");
    assertNotNull(notification2);
    
    assertTrue(notification2.equals(notification));
    
  }
  
  public void testNormalGetByUserAndRemoveMessagesSent() throws Exception {
    NotificationInfo notification = saveNotification("root", "demo");
    UserSetting userSetting = UserSetting.getInstance();
    userSetting.setUserId("root").addProvider(PluginTest.ID, FREQUENCY.DAILY);
    userSetting.setActive(true);
    
    Map<NotificationKey, List<NotificationInfo>> map = notificationDataStorage.getByUser(userSetting, false);
    
    List<NotificationInfo> list = map.get(new NotificationKey(PluginTest.ID));
    assertEquals(1, list.size());
    
    assertTrue(list.get(0).equals(notification));
    // after sent, user demo will auto remove from property daily
    NotificationInfo notification2 = getNotificationInfoByKeyIdAndParam(PluginTest.ID, "objectId=idofobject");
    assertNotNull(notification2);
    
    assertEquals(0, notification2.getSendToDaily().length);
    
    userSetting.setUserId("demo").addProvider(PluginTest.ID, FREQUENCY.WEEKLY);
    map = notificationDataStorage.getByUser(userSetting, true);
    list = map.get(new NotificationKey(PluginTest.ID));
    assertEquals(1, list.size());
    
    
    notificationDataStorage.removeMessageAfterSent(true);
    
    notification2 = getNotificationInfoByKeyIdAndParam(PluginTest.ID, "objectId=idofobject");
    assertNull(notification2);
  }

  public void testSpecialGetByUserAndRemoveMessagesSent() throws Exception {
    NotificationInfo notification = NotificationInfo.instance();
    Map<String, String> params = new HashMap<String, String>();
    params.put("objectId", "idofobject");
    notification.key(PluginTest.ID).setSendAll(true).setOwnerParameter(params).setOrder(1);
    notificationDataStorage.save(notification);
    
    UserSetting userSetting = UserSetting.getInstance();
    userSetting.setUserId("root").addProvider(PluginTest.ID, FREQUENCY.DAILY);
    userSetting.setActive(true);
    // Test send to daily
    Map<NotificationKey, List<NotificationInfo>> map = notificationDataStorage.getByUser(userSetting, false);
    
    List<NotificationInfo> list = map.get(new NotificationKey(PluginTest.ID));
    assertEquals(1, list.size());
    
    assertTrue(list.get(0).equals(notification));
    // check value from node
    NotificationInfo notification2 = getNotificationInfoByKeyIdAndParam(PluginTest.ID, "objectId=idofobject");
    assertNotNull(notification2);

    assertEquals(NotificationInfo.FOR_ALL_USER, notification2.getSendToDaily()[0]);
    // remove value on property sendToDaily
    notificationDataStorage.removeMessageAfterSent(false);

    // after sent, the value on on property sendToDaily will auto removed
    notification2 = getNotificationInfoByKeyIdAndParam(PluginTest.ID, "objectId=idofobject");
    assertEquals(0, notification2.getSendToDaily().length);
    
    // Test send to weekly
    userSetting.setUserId("demo").addProvider(PluginTest.ID, FREQUENCY.WEEKLY);
    map = notificationDataStorage.getByUser(userSetting, true);
    list = map.get(new NotificationKey(PluginTest.ID));
    assertEquals(1, list.size());
    
    notificationDataStorage.removeMessageAfterSent(true);
    
    notification2 = getNotificationInfoByKeyIdAndParam(PluginTest.ID, "objectId=idofobject");
    assertNull(notification2);
  }

  public void testWithUserNameContainSpecialCharacter() throws Exception {
    String userNameSpecial = "Rabe'e \"AbdelWahabô";
    NotificationInfo notification = saveNotification(userNameSpecial, "demo");
    //
    UserSetting userSetting = UserSetting.getInstance();
    userSetting.setUserId(userNameSpecial).addProvider(PluginTest.ID, FREQUENCY.DAILY);
    userSetting.setActive(true);
    //
    Map<NotificationKey, List<NotificationInfo>> map = notificationDataStorage.getByUser(userSetting, false);
    List<NotificationInfo> list = map.get(new NotificationKey(PluginTest.ID));
    //
    assertEquals(1, list.size());
    assertTrue(list.get(0).equals(notification));
  }
  
  public void testRunDigestOfYesterdayOnToday() throws Exception {
    Calendar cal = Calendar.getInstance();
    int today = cal.get(Calendar.DAY_OF_MONTH);
    cal.setTimeInMillis(cal.getTimeInMillis() - 86400000);
    int yesterday = cal.get(Calendar.DAY_OF_MONTH);
    // create some notifications has createdDate is today
    int numberToday = 5;
    for (int i = 0; i < numberToday; i++) {
      saveNotification("demo", "");
    }
    int numberYesterday = 10;
    // create some notifications has createdDate is yesterday
    for (int i = 0; i < numberYesterday; i++) {
      saveNotification("demo", "", cal);
    }
    String pluginPath = "/eXoNotification/messageHome/" + PluginTest.ID;
    assertEquals(numberToday, ((Node) session.getItem(pluginPath + "/d" + today)).getNodes().getSize());
    assertEquals(numberYesterday, ((Node) session.getItem(pluginPath + "/d" + yesterday)).getNodes().getSize());
    // Save user setting
    UserSetting userSetting = UserSetting.getInstance();
    userSetting.setUserId("demo").addProvider(PluginTest.ID, FREQUENCY.DAILY);
    userSetting.setActive(true);
    getService(UserSettingService.class).save(userSetting);
    addLastUpdateTime("demo");
    // run daily digest
    configuration.setJobCurrentDay(null);
    notificationService.processDigest(yesterday, false);
    // check data after run digest
    assertEquals(1, ((Node) session.getItem("/eXoNotification/messageInfoHome")).getNodes().getSize());
    //
    assertEquals(0, ((Node) session.getItem(pluginPath + "/d" + yesterday)).getNodes().getSize());
  }
  
  
  private void addMixin(String msgId) throws Exception {
    Node msgNode = getMessageNodeById(msgId);
    if (msgNode != null) {
      msgNode.addMixin("exo:datetime");
      msgNode.setProperty("exo:dateCreated", Calendar.getInstance());
      session.save();
    }
  }

  private NotificationInfo fillModel(Node node) throws Exception {
    if(node == null) return null;
    NotificationInfo message = NotificationInfo.instance()
      .setFrom(node.getProperty("ntf:from").getString())
      .setOrder(Integer.valueOf(node.getProperty("ntf:order").getString()))
      .key(node.getProperty("ntf:providerType").getString())
      .setOwnerParameter(node.getProperty("ntf:ownerParameter").getValues())
      .setSendToDaily(NotificationUtils.valuesToArray(node.getProperty("ntf:sendToDaily").getValues()))
      .setSendToWeekly(NotificationUtils.valuesToArray(node.getProperty("ntf:sendToWeekly").getValues()))
      .setId(node.getName());
    
    return message;
  }
  
  private Node getMessageNodeById(String msgId) throws Exception {
    return getMessageNode(new StringBuffer("exo:name = '").append(msgId).append("'").toString(), "");
  }

  private NotificationInfo getNotificationInfoByKeyIdAndParam(String key, String param) throws Exception {
    Node node = getMessageNode(new StringBuffer("ntf:ownerParameter LIKE '%").append(param).append("%'").toString(), key);
    return fillModel(node);
  }
  
  private Node getMessageNode(String strQuery, String key) throws Exception {
    StringBuilder sqlQuery = new StringBuilder("SELECT * FROM ntf:message WHERE ");
    if (key != null && key.length() > 0) {
      sqlQuery.append(" jcr:path LIKE '").append("/eXoNotification/messageHome/").append(key).append("/%' AND ");
    }
    sqlQuery.append(strQuery);

    QueryManager qm = session.getWorkspace().getQueryManager();
    Query query = qm.createQuery(sqlQuery.toString(), Query.SQL);
    NodeIterator iter = query.execute().getNodes();
    return (iter.getSize() > 0) ? iter.nextNode() : null;
  }
  
  private void initDefaultHomeNode() throws Exception {
    // User
    Node setting = addNode(root, "settings", "stg:settings");
    addNode(setting, "user", "stg:subcontext");
    //
    Node ntfHome = addNode(root, AbstractService.NOTIFICATION_HOME_NODE, AbstractService.NTF_NOTIFICATION);
    Node msgHome = addNode(ntfHome, AbstractService.MESSAGE_HOME_NODE, AbstractService.NTF_MESSAGE_HOME);
    msgHome.addMixin(AbstractService.MIX_SUB_MESSAGE_HOME);
    addNode(ntfHome, AbstractService.MESSAGE_INFO_HOME_NODE, AbstractService.NTF_MESSAGE_INFO_HOME);
    //
    session.save();
  }

  private Node addNode(Node parent, String name, String type) throws Exception {
    if (parent.hasNode(name)) {
      return parent.getNode(name);
    }
    return parent.addNode(name, type);
  }

}
