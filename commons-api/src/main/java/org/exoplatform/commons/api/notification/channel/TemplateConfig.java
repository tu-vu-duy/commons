package org.exoplatform.commons.api.notification.channel;

public @interface TemplateConfig {
  String pluginId() default "";
  String path() default "";
}
