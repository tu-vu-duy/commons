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

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-commons-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/notification/exo.notification.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/notification/exo.notification.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/notification/exo.notification.test.portal-configuration.xml")
})
public abstract class AsbtractBaseNotificationTestCase extends BaseExoTestCase {

  protected Session session;
  protected Node root;

  @Override
  public void setUp() throws Exception {
    //
    begin();
    //
    session = getSession();
    root = session.getRootNode();
    System.setProperty(CommonsUtils.CONFIGURED_DOMAIN_URL_KEY, "http://exoplatform.com");
  }

  @Override
  public void tearDown() throws Exception {
    root = null;
    session.logout();
    //
    end();
  }

  @SuppressWarnings("unchecked")
  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }
  
  public Session getSession() {
    Session session = null;
    try {
      ManageableRepository repository = getService(RepositoryService.class).getCurrentRepository();
      session = repository.getSystemSession("portal-test");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return session;
  }
  
  protected void addLastUpdateTime(String userId) throws Exception {
    Node rootNode = session.getRootNode().getNode("settings").getNode("user").getNode(userId);
    rootNode.addMixin("exo:datetime");
    rootNode.setProperty("exo:lastModifiedDate", Calendar.getInstance());
    session.save();
  }
}
