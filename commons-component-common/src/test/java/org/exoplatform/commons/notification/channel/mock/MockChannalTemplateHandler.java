package org.exoplatform.commons.notification.channel.mock;

import java.util.List;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.AbstractChannelTemplateHandler;
import org.exoplatform.commons.api.notification.channel.ChannelConfigs;
import org.exoplatform.commons.api.notification.channel.TemplateConfig;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationChildPlugin;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.notification.service.setting.PluginContainer;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;

@ChannelConfigs (
   id="email",
   templates = {
       @TemplateConfig( pluginId="DigestDailyPlugin", path="classpath:/groovy/notification/template/provider1.gtmpl"),
       @TemplateConfig( pluginId="DigestWeeklyPlugin", path="classpath:/groovy/notification/template/provider1.gtmpl"),
       @TemplateConfig( pluginId="TestPlugin", path="classpath:/groovy/notification/template/TestPlugin.gtmpl")
   }
)
public class MockChannalTemplateHandler extends AbstractChannelTemplateHandler {

  @Override
  public MessageInfo makeMessage(NotificationContext ctx) {
    NotificationInfo notification = ctx.getNotificationInfo();
    MakeMessageInfo builder = MakeMessageInfo.get(notification.getKey().getId());
    if(builder != null) {
      return builder.buildMessage(ctx);
    }
    return null;
  }

  private enum MakeMessageInfo {
    DigestDailyPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx) {
        MessageInfo info = new MessageInfo().pluginId(ctx.getNotificationInfo().getKey().getId());
        return info;
      }
    },
    DigestWeeklyPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx) {
        MessageInfo info = new MessageInfo().pluginId(ctx.getNotificationInfo().getKey().getId());
        return info;
      }
    },
    TestPlugin() {
      @Override
      protected MessageInfo buildMessage(NotificationContext ctx) {
        NotificationInfo notification = ctx.getNotificationInfo();
        String language = NotificationPluginUtils.getLanguage(notification.getTo());
        TemplateContext templateContext = new TemplateContext("email", notification.getKey().getId(), language);
        
        
        templateContext.put("USER", notification.getValueOwnerParameter("USER"));
        templateContext.put("SUBJECT", "Test plugin notification");
        String subject = TemplateUtils.processSubject(templateContext);

        String value = notification.getValueOwnerParameter("TEST_VALUE");
        templateContext.put("VALUE", value);
        StringBuilder childContent = new StringBuilder();
        
        PluginContainer pluginContainer = CommonsUtils.getService(PluginContainer.class);
        List<NotificationKey> childKeys = pluginContainer.getChildPluginKeys(notification.getKey());
        for (NotificationKey notificationKey : childKeys) {
          AbstractNotificationPlugin child = pluginContainer.getPlugin(notificationKey);
          childContent.append("<br>").append(((AbstractNotificationChildPlugin) child).makeContent(ctx));
        }
        templateContext.put("CHILD_CONTENT", childContent.toString());
        
        return new MessageInfo().subject(subject).body(TemplateUtils.processGroovy(templateContext)).end();
      }
    };
    
    public static MakeMessageInfo get(String id) {
      for (MakeMessageInfo item : values()) {
        if(item.name().equalsIgnoreCase(id)) {
          return item;
        }
      }
      return null;
    }
    protected abstract MessageInfo buildMessage(NotificationContext ctx);
  }
  
}
