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
package org.exoplatform.commons.suite;

import org.exoplatform.commons.notification.TemplateTestCase;
import org.exoplatform.commons.notification.TestNotificationUtils;
import org.exoplatform.commons.notification.template.TemplateUtilsTestCase;
import org.exoplatform.services.bench.TestDataInjector;
import org.exoplatform.services.bench.TestDataInjectorService;
import org.exoplatform.services.deployment.ContentInitializerServiceTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
  ContentInitializerServiceTest.class,
  TestDataInjector.class,
  TestDataInjectorService.class,
  TestNotificationUtils.class,
  TemplateUtilsTestCase.class,
  TemplateTestCase.class
})
public class BaseUnitTestSuite {
  @BeforeClass
  public static void setUp() throws Exception {
  }

  @AfterClass
  public static void tearDown() {
  }
}
