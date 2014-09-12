/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.commons.notification.impl.service;

import org.exoplatform.commons.notification.impl.log.FullAuditEmailMessageLog;
import org.exoplatform.commons.notification.impl.log.SimpleAuditEmailMessageLog;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.ManagementAware;
import org.exoplatform.management.ManagementContext;
import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;

@Managed
@ManagedDescription("Mock mail service")
@NameTemplate({ 
  @Property(key = "service", value = "notification"), 
  @Property(key = "view", value = "mockmail")
})
public class SendEmailService implements ManagementAware {
  private boolean     isOn           = false;

  private long        sentCounter     = 0;

  private long        currentCapacity = 0;

  private int         emailPerSend  = 0;

  private int         interval  = 0;
  
  private ManagementContext context;
  
  private QueueMessageImpl queueMessage;

  public SendEmailService(QueueMessageImpl queueMessage) {
    this.queueMessage = queueMessage;
    this.queueMessage.setManagementView(this);
  }
  
  public void registerManager(Object o) {
    if (context != null) {
      context.register(o);
    }
  }

  @Override
  public void setContext(ManagementContext context) {
    this.context = context;
  }

  public void counter() {
    ++sentCounter;
  }

  public void addCurrentCapacity() {
    ++currentCapacity;
  }

  public void removeCurrentCapacity() {
    if (currentCapacity > 0) {
      --currentCapacity;
    }
  }

  @Managed
  @ManagedDescription("Current mail service capacity should be available.")
  @Impact(ImpactType.READ)
  public long getCurrentCapacity() {
    return currentCapacity;
  }

  @Managed
  @ManagedDescription("Turn on the mail service.")
  @Impact(ImpactType.READ)
  public void on() {
    resetCounter();
    isOn = true;
    emailPerSend = 120;
    interval = 120;
    rescheduleJob();
  }

  @Managed
  @ManagedDescription("Status of mail service. (true/false)")
  @Impact(ImpactType.READ)
  public boolean isOn() {
    return isOn;
  }

  @Managed
  @ManagedDescription("Turn off the mail service.")
  @Impact(ImpactType.READ)
  public void off() {
    resetCounter();
    this.queueMessage.resetDefaultConfigJob();
    isOn = false;
  }

  @Managed
  @ManagedDescription("Number emails sent")
  @Impact(ImpactType.READ)
  public long getSentCounter() {
    return sentCounter;
  }

  @Managed
  @ManagedDescription("Reset email countet.")
  @Impact(ImpactType.READ)
  public void resetCounter() {
    sentCounter = 0;
  }

  @Managed
  @ManagedDescription("Set number emails send per one time.")
  @Impact(ImpactType.READ)
  public void setNumberEmailPerSend(int emailPerSend) {
    this.emailPerSend = emailPerSend;
    rescheduleJob();
  }

  @Managed
  @ManagedDescription("Number emails send per one time.")
  @Impact(ImpactType.READ)
  public int getNumberEmailPerSend() {
    return this.emailPerSend;
  }

  @Managed
  @ManagedDescription("Set period of time (in seconds) for each sending notification execution.")
  @Impact(ImpactType.READ)
  public void setInterval(int interval) {
    this.interval = interval;
    rescheduleJob();
  }
  
  @Managed
  @ManagedDescription("Period of time (in seconds) between each sending notification execution.")
  @Impact(ImpactType.READ)
  public int getInterval() {
    return this.interval;
  }

  @Managed
  @ManagedDescription("Gets the log sent emails notification.")
  @Impact(ImpactType.READ)
  public String getLogs() {
    return queueMessage.getLogs();
  }

  @Managed
  @ManagedDescription("Start DefaultSendMessageListener.")
  @Impact(ImpactType.READ)
  public String startDefaultSendMessage() {
    queueMessage.addPlugin(new DefaultSendMessageListener(createInitParams()));
    return "DefaultSendMessageListener started";
  }

  @Managed
  @ManagedDescription("Start InMemorySendMessageListener.")
  @Impact(ImpactType.READ)
  public String startInMemorySendMessage() {
    queueMessage.addPlugin(new InMemorySendMessageListener(createInitParams()));
    return "InMemorySendMessageListener started";
  }

  @Managed
  @ManagedDescription("Start SimpleSendMessageListener.")
  @Impact(ImpactType.READ)
  public String startSimpleSendMessage() {
    queueMessage.addPlugin(new SimpleSendMessageListener());
    return "SimpleSendMessageListener started";
  }

  @Managed
  @ManagedDescription("Reset to use default configuration SendMessageListener.")
  @Impact(ImpactType.READ)
  public String resetDefaultConfigSendMessage() {
    queueMessage.addPlugin(null);
    return "Reset to use default SendMessageListener";
  }

  @Managed
  @ManagedDescription("Start SimpleAuditEmailMessageLog.")
  @Impact(ImpactType.READ)
  public String startSimpleAuditMessageLog() {
    queueMessage.setAuditLog(new SimpleAuditEmailMessageLog());
    return "SimpleAuditEmailMessageLog started";
  }
  
  @Managed
  @ManagedDescription("Start FullAuditEmailMessageLog.")
  @Impact(ImpactType.READ)
  public String startFullAuditMessageLog() {
    queueMessage.setAuditLog(new FullAuditEmailMessageLog());
    return "FullAuditEmailMessageLog started";
  }

  @Managed
  @ManagedDescription("Clear AuditEmailMessageLog.")
  @Impact(ImpactType.READ)
  public String clearAuditMessageLog() {
    queueMessage.clearLogs();
    return "Clear AuditEmailMessageLog done!";
  }

  @Managed
  @ManagedDescription("Removes all notification data that stored in database.")
  @Impact(ImpactType.READ)
  public String resetTestMail() {
    currentCapacity = 0;
    resetCounter();
    isOn = true;
    emailPerSend = 120;
    interval = 120;
    return queueMessage.removeAll();
  }
  
  @Managed
  @ManagedDescription("Removes all users setting that stored in database.")
  @Impact(ImpactType.READ)
  public String removeUsersSetting() {
    return queueMessage.removeUsersSetting();
  }
  
  private void rescheduleJob() {
    if (isOn) {
      this.queueMessage.rescheduleJob(emailPerSend, interval * 1000);
    }
  }
  
  private InitParams createInitParams() {
    InitParams params = new InitParams();
    //
    ValueParam maxToSend = new ValueParam();
    maxToSend.setName(InMemorySendMessageListener.MAX_TO_SEND_KEY);
    maxToSend.setValue(String.valueOf(((emailPerSend > 0) ? emailPerSend : 50)));
    params.addParameter(maxToSend);

    ValueParam intervalParam = new ValueParam();
    intervalParam.setName(InMemorySendMessageListener.PERIOD_INTERVAL_KEY);
    intervalParam.setValue(String.valueOf(((interval > 0) ? interval : 120)));
    params.addParameter(intervalParam);
    return params;
  }
}
