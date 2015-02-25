package org.exoplatform.commons.notification.mock;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.plugin.PluginTest;

public class MockEventListener {
  public void eventActive() {
    NotificationContext ctx = NotificationContextImpl.cloneInstance();
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(PluginTest.ID)))
                                 .execute(ctx);
  }
}
