package org.exoplatform.commons.notification;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.model.UserSetting.FREQUENCY;
import org.exoplatform.commons.api.notification.node.NTFInforkey;
import org.exoplatform.commons.api.notification.node.TreeNode;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.plugin.config.PluginConfig;
import org.exoplatform.commons.api.notification.plugin.config.TemplateConfig;
import org.exoplatform.commons.api.notification.service.NotificationService;
import org.exoplatform.commons.api.notification.service.QueueMessage;
import org.exoplatform.commons.api.notification.service.setting.PluginContainer;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.api.notification.service.storage.NotificationDataStorage;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.impl.service.ExtendedNotificationService;
import org.exoplatform.commons.notification.impl.service.QueueMessageImpl;
import org.exoplatform.commons.notification.impl.setting.PluginSettingServiceImpl;
import org.exoplatform.commons.notification.mock.MockMailService;
import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.settings.impl.SettingServiceImpl;

public class NotificationServiceTest extends BaseCommonsTestCase {

  public final ArgumentLiteral<List> SENDTOS = new ArgumentLiteral<List>(List.class, "sendTos");
  private NotificationService notificationService;
  
  private NotificationDataStorage notificationDataStorage;
  private OrganizationService organizationService;
  private UserSettingService userSettingService;
  private UserHandler userHandler;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.organizationService = getService(OrganizationService.class);
    //
    this.userHandler = organizationService.getUserHandler();
    
    getService(SettingServiceImpl.class);
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    
    userSettingService = getService(UserSettingService.class);
    
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
  
  private void addPlugin(String pluginId) {
    InitParams initParams = new InitParams();
    PluginConfig config = new PluginConfig();
    config.setPluginId(pluginId);
    config.setGroupId("");
    config.setOrder("1");
    config.setResourceBundleKey("resourceBundleKey");
    TemplateConfig template = new TemplateConfig(pluginId);
    template.setBundlePath("locale.notification.template.Notification");
    template.setTemplatePath("classpath:/groovy/notification/template/PluginTest.gtmpl");
    //
    config.setTemplateConfig(template);
    
    ObjectParameter object = new ObjectParameter();
    object.setName("template." + pluginId);
    object.setDescription("description");
    object.setObject(config);
    initParams.addParam(object);
    ValueParam param = new ValueParam();
    param.setName("pluginId");
    param.setValue(pluginId);
    param.setDescription("");
    initParams.addParameter(param);
    
    AbstractNotificationPlugin plugin = new TestNotificationPlugin(initParams);
    getService(PluginContainer.class).add(plugin);
    getService(PluginSettingServiceImpl.class).registerPluginConfig(plugin.getPluginConfigs().get(0));
  }

  private void addMixin(NotificationInfo notification) throws Exception {
    Node msgNode = getMessageNodeById(notification.getId());
    if (msgNode != null) {
      msgNode.addMixin("exo:datetime");
      msgNode.setProperty("exo:dateCreated", Calendar.getInstance());
      msgNode.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      session.save();
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
      return null;
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

  public void testNormalExtendedDataStorageImpl() throws Exception {
    NotificationConfiguration configuration = getService(NotificationConfiguration.class);
    // add plugin
    addPlugin("TestPlugin");
    // setting for user
    UserSettingService settingService = getService(UserSettingService.class);
    // for root
    UserSetting userSetting = UserSetting.getInstance();
    userSetting.setUserId("root").addProvider("TestPlugin", FREQUENCY.DAILY);
    userSetting.setActive(true);
    settingService.save(userSetting);
    // for demo
    userSetting = UserSetting.getInstance();
    userSetting.setUserId("demo").addProvider("TestPlugin", FREQUENCY.WEEKLY);
    userSetting.setActive(true);
    settingService.save(userSetting);

    // create notification information.
    TestNotificationPlugin plugin = (TestNotificationPlugin) getService(PluginContainer.class).getPlugin(new NotificationKey("TestPlugin"));
    // This process will storage notification via settings of users on system.
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.append(SENDTOS, Arrays.asList("demo", "root"));
    notificationService.process(plugin.buildNotification(ctx));

    // process send daily
    configuration.setSendWeekly(false);

    ExtendedNotificationService extendService = getService(ExtendedNotificationService.class);
    extendService.processDigest();
    // send mails
    getService(QueueMessage.class).send();
    // result sent.
    assertTrue(getService(MockMailService.class).getAndClearSentsUser().get(0).indexOf("root") >= 0);

    // check after sent daily
    Node node = getMessageNodeByKeyIdAndParam("TestPlugin", "PLUGIN=TestPlugin");
    assertNotNull(node);

    NotificationInfo notification = fillModel(node);
    // after sent daily, the value of property sendDaily will removed
    assertEquals(0, notification.getSendToDaily().length);

    // process send weekly
    configuration.setSendWeekly(true);
    extendService.processDigest();
    // send mails
    getService(QueueMessage.class).send();

    // result
    assertTrue(getService(MockMailService.class).getAndClearSentsUser().get(0).indexOf("demo") >= 0);

    // after sent weekly, the node removed
    node = getMessageNodeByKeyIdAndParam("TestPlugin", "PLUGIN=TestPlugin");
    assertNull(node);
  }

  public void testPerfomanceExtendNotification() throws Exception {
    PluginContainer container = getService(PluginContainer.class);
    NotificationConfiguration configuration = getService(NotificationConfiguration.class);
    QueueMessageImpl messageImpl = getService(QueueMessageImpl.class);
    messageImpl.makeJob(10000, 10000);
    // register plugins
    int pluginSize = 10;
    List<String> pluginIds = new ArrayList<String>();
    for (int i = 0; i < pluginSize; i++) {
      String plnId = "TestPlugin" + i;
      addPlugin(plnId);
      pluginIds.add(plnId);
    }
    // setting for 100 users and active all plugins for each user.
    List<String> users = createUser(100, pluginIds);
    Map<String, Long> results = new HashMap<String, Long>();
    { // Test for extend
      // create notification information and active for all users.
      createNotificationInfo(container, users, pluginSize);

      // process send daily
      configuration.setSendWeekly(false);
      ExtendedNotificationService extendService = getService(ExtendedNotificationService.class);
      long t = System.currentTimeMillis();
      extendService.processDigest();
      results.put("Extend_daily", (System.currentTimeMillis() - t));
      System.out.println("\n\n The time for extend process daily: " + results.get("Extend_daily") + "ms\n\n");

      // send mails
      messageImpl.send();
      // result
      System.out.println("\n\n The mails sent: " + getService(MockMailService.class).getAndClearSentsUser().size() + "\n");

      // process send weekly
      configuration.setSendWeekly(false);
      t = System.currentTimeMillis();
      extendService.processDigest();
      results.put("Extend_weekly", (System.currentTimeMillis() - t));
      System.out.println("\n\n The time for extend process weekly: " +  results.get("Extend_weekly") + "ms\n\n");

      // send mails
      messageImpl.send();
      // result
      System.out.println("\n\n The mails sent: " + getService(MockMailService.class).getAndClearSentsUser().size() + "\n");
    }

    { // test for down tree
      // create notification information and active for all users.
      createNotificationInfo(container, users, pluginSize);

      // process send daily
      configuration.setSendWeekly(false);
      long t = System.currentTimeMillis();
      notificationService.processDigest();
      results.put("Normal_daily", (System.currentTimeMillis() - t));
      System.out.println("\n\n The time for normal process daily: " + results.get("Normal_daily") + "ms\n\n");

      // send mails
      messageImpl.send();
      // result
      System.out.println("\n\n The mails sent: " + getService(MockMailService.class).getAndClearSentsUser().size() + "\n");

      // process send weekly
      configuration.setSendWeekly(false);
      t = System.currentTimeMillis();
      notificationService.processDigest();
      results.put("Normal_weekly", (System.currentTimeMillis() - t));
      System.out.println("\n\n The time for normal process weekly: " + results.get("Normal_weekly") + "ms\n\n");

      // send mails
      messageImpl.send();
      // result
      System.out.println("\n\n The mails sent: " + getService(MockMailService.class).getAndClearSentsUser().size() + "\n");
    }
    
    System.out.println("\n\n Results:\n" + results.toString().replace("{", "{\n  ").replace("}", "}\n").replace(",", ",\n  ") + "\n");
  }

  private void createNotificationInfo(PluginContainer container, List<String> users, int number) throws Exception {
    for (int i = 0; i < number; i++) {
      AbstractNotificationPlugin plugin = container.getPlugin(new NotificationKey("TestPlugin" + i));
      // This process will storage notification via settings of users on system.
      NotificationContext ctx = NotificationContextImpl.cloneInstance();
      ctx.append(SENDTOS, users);
      notificationService.process(plugin.buildNotification(ctx));
    }
  }

  private List<String> createUser(int number, List<String> pluginIds) throws Exception {
    List<String> users = new ArrayList<String>();
    for (int i = 0; i < number; ++i) {
      String userId = "user" + i;
      User user = userHandler.createUserInstance(userId);
      user.setEmail(userId + "@test.com");
      user.setFirstName(userId.toUpperCase());
      user.setLastName("Test");
      user.setPassword("exoexo");
      //
      userHandler.createUser(user, true);
      // addMixin
      userSettingService.addMixin(userId);
      //
      users.add(userId);
      UserSetting userSetting = UserSetting.getInstance();
      userSetting.setUserId(userId).setDailyProviders(pluginIds);
      userSetting.setWeeklyProviders(pluginIds);
      userSetting.setActive(true);
      userSettingService.save(userSetting);
    }
    return users;
  }

  public class TestNotificationPlugin extends AbstractNotificationPlugin {
    private String id = null;

    public TestNotificationPlugin(InitParams initParams) {
      super(initParams);
      this.id = initParams.getValueParam("pluginId").getValue();
    }

    @Override
    protected NotificationInfo makeNotification(NotificationContext ctx) {
      NotificationInfo notificationInfo = NotificationInfo.instance();
      return notificationInfo.to("demo")
                             .setTo("demo")
                             .with("USER", "root")
                             .with("TEST_VALUE", "Test value")
                             .with("PLUGIN", getId())
                             .key(getId())
                             .to(new ArrayList<String>(ctx.value(SENDTOS)))
                             .end();
    }

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      return new MessageInfo().subject("subject").body("body").end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      try {
        NotificationDataStorage dataStorage = CommonsUtils.getService(NotificationDataStorage.class);
        List<NTFInforkey> infoKeys = ctx.getNotificationInfos();

        for (NTFInforkey infoKey : infoKeys) {
          NotificationInfo message = dataStorage.get(infoKey.getUUID());
          String userId = message.getValueOwnerParameter("USER");
          //
          writer.append("The user ").append(userId).append(". The value ").append(message.getValueOwnerParameter("TEST_VALUE"));
        }
        writer.append(", The plugin ").append(getId());
      } catch (Exception e) {
        ctx.setException(e);
        return false;
      }
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
  }
}
