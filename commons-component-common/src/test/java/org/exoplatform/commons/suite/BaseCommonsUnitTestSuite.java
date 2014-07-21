package org.exoplatform.commons.suite;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.commons.embedder.EmbedderTest;
import org.exoplatform.commons.event.TestEventManager;
import org.exoplatform.commons.utils.CommonsUtilsTest;
import org.exoplatform.commons.utils.TestActivityTypeUtils;
import org.exoplatform.commons.utils.XPathUtilsTest;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.job.MultiTenancyJobTest;
import org.exoplatform.job.MultiTenancyTaskTest;
import org.exoplatform.services.deployment.TestContentInitializerService;
import org.exoplatform.services.deployment.UtilsTest;
import org.exoplatform.services.deployment.plugins.XMLDeploymentPluginTest;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.user.UserStateServiceTest;
import org.exoplatform.settings.impl.CacheSettingTest;
import org.exoplatform.settings.impl.FeatureServiceTest;
import org.exoplatform.settings.impl.SettingServiceEventTest;
import org.exoplatform.settings.impl.SettingServiceImplTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
  SettingServiceImplTest.class,
  CacheSettingTest.class,
  SettingServiceEventTest.class,
  FeatureServiceTest.class,
  MultiTenancyJobTest.class,
  MultiTenancyTaskTest.class,
  UserStateServiceTest.class,
  XMLDeploymentPluginTest.class,
  TestContentInitializerService.class,
  UtilsTest.class,
  EmbedderTest.class,
  TestEventManager.class,
  CommonsUtilsTest.class,
  XPathUtilsTest.class,
  TestActivityTypeUtils.class
})
public class BaseCommonsUnitTestSuite {
  protected static final String         REPO_NAME      = "repository";
  protected static final String         WORKSPACE_NAME = "portal-test";
  protected static PortalContainer      container;
  protected static RepositoryService    repositoryService;
  protected static ConfigurationManager configurationManager;
  protected static Session              session;
  protected static Node                 root;
  @BeforeClass
  public static void setUp() throws Exception {
    RootContainer.setInstance(null);
    container = PortalContainer.getInstance();
    repositoryService = getService(RepositoryService.class);
    configurationManager = getService(ConfigurationManager.class);
    
    ManageableRepository manageableRepository = repositoryService.getRepository(REPO_NAME);
    manageableRepository.getConfiguration().setDefaultWorkspaceName(WORKSPACE_NAME);
    session = manageableRepository.getSystemSession(WORKSPACE_NAME);
    root = session.getRootNode();
  }

  @AfterClass
  public static void tearDown() {
    container = null;
    repositoryService = null;
    configurationManager = null;
    root = null;
    if(session != null && session.isLive()) {
      session.logout();
    }
    session = null;
  }
  protected static <T> T getService(Class<T> clazz) {
    return clazz.cast(container.getComponentInstanceOfType(clazz));
  }
}
