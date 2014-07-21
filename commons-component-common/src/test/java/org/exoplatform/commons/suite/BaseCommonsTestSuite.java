package org.exoplatform.commons.suite;

import org.exoplatform.commons.notification.AsbtractBaseNotificationTestCase;
import org.exoplatform.commons.notification.NotificationServiceTest;
import org.exoplatform.commons.notification.PluginContainerTest;
import org.exoplatform.commons.notification.user.UserSettingServiceTest;
import org.exoplatform.commons.testing.BaseExoContainerTestSuite;
import org.exoplatform.commons.testing.ConfigTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
  UserSettingServiceTest.class,
  NotificationServiceTest.class,
  PluginContainerTest.class
})
@ConfigTestCase(AsbtractBaseNotificationTestCase.class)
public class BaseCommonsTestSuite extends BaseExoContainerTestSuite {
  @BeforeClass
  public static void setUp() throws Exception {
    initConfiguration(BaseCommonsTestSuite.class);
    beforeSetup();
  }

  @AfterClass
  public static void tearDown() {
    afterTearDown();
  }
}
