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
package org.exoplatform.commons.notification.impl.service;

import java.util.GregorianCalendar;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.service.AbstractSendMessageListener;
import org.exoplatform.commons.notification.NotificationContextFactory;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.job.SendEmailNotificationJob;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.JobInfo;
import org.exoplatform.services.scheduler.JobSchedulerService;
import org.exoplatform.services.scheduler.PeriodInfo;
import org.quartz.JobDataMap;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

public class InMemorySendMessageListener extends AbstractSendMessageListener {
  private static final Log LOG = ExoLogger.getExoLogger(InMemorySendMessageListener.class);
  private static final String CACHE_REPO_NAME        = "repositoryName";
  public static final String MAX_TO_SEND_SYS_KEY     = "exo.notification.service.QueueMessage.numberOfMailPerBatch";
  public static final String MAX_TO_SEND_KEY         = "numberOfMailPerBatch";
  public static final String PERIOD_INTERVAL_SYS_KEY = "exo.notification.service.QueueMessage.period";
  public static final String PERIOD_INTERVAL_KEY     = "period";

  private int  MAX_TO_SEND;
  private long PERIOD_INTERVAL;

  protected int limit;

  /** using the set to keep the messages. */
  private final Queue<MessageInfo> messageQueue = new ConcurrentLinkedQueue<MessageInfo>();

  public InMemorySendMessageListener(InitParams params) {
    MAX_TO_SEND = NotificationUtils.getSystemValue(params, MAX_TO_SEND_SYS_KEY, MAX_TO_SEND_KEY, 50);
    PERIOD_INTERVAL = NotificationUtils.getSystemValue(params, PERIOD_INTERVAL_SYS_KEY, PERIOD_INTERVAL_KEY, 120) * 1000;
    this.limit = MAX_TO_SEND;
  }
  
  @Override
  public boolean put(MessageInfo message) {
    //
    messageQueue.add(message);
    return false;
  }

  @Override
  public void send() {
    LOG.debug("Starting to send email notification by InMemorySendMailMessage...");
    try {
      //
      final boolean stats = NotificationContextFactory.getInstance().getStatistics().isStatisticsEnabled();
      for (int i = 0; i < limit; i++) {
        if (messageQueue.isEmpty() == false) {
          MessageInfo messageInfo = messageQueue.peek();
          if (getQueueMessageImpl().sendMessage(messageInfo.makeEmailNotification()) == true) {
            messageQueue.remove(messageInfo);
            if (stats) {
              NotificationContextFactory.getInstance().getStatisticsCollector().pollQueue(messageInfo.getPluginId());
            }
          }
        } else {
          break;
        }
      }
    } catch (Exception e) {
      LOG.warn("Failed to send message.");
      LOG.debug(e.getMessage(), e);
    }
  }

  @Override
  public void start() {
    createJob();
  }
  
  private void createJob() {
    try {
      String jobName = "SendEmailNotificationJob";
      String groupName = "Notification";
      JobSchedulerService schedulerService = CommonsUtils.getService(JobSchedulerService.class);
      JobInfo info = new JobInfo(jobName, groupName, SendEmailNotificationJob.class);
      info.setDescription("Send email notification job.");
      schedulerService.removeJob(info);
      JobDataMap jdatamap = new JobDataMap();
      jdatamap.put(CACHE_REPO_NAME, CommonsUtils.getRepository().getConfiguration().getName());
      //
      PeriodInfo periodInfo = new PeriodInfo(new GregorianCalendar().getTime(), null, -1, PERIOD_INTERVAL);
      schedulerService.addPeriodJob(info, periodInfo, jdatamap);
    } catch (Exception e) {
      LOG.warn("Failed at createJob()");
      LOG.debug(e.getMessage(), e);
    }
  }

  @Override
  public void rescheduleJob(int limit, long interval) {
    try {
      JobSchedulerService schedulerService = CommonsUtils.getService(JobSchedulerService.class);
      String jobName = "SendEmailNotificationJob";
      String groupName = "Notification";
      if (interval == -1) {
        interval = PERIOD_INTERVAL;
      }
      Trigger[] triggers = schedulerService.getTriggersOfJob(jobName, groupName);
      if(triggers != null && triggers.length > 0) {
        Trigger newTrigger = TriggerBuilder.newTrigger().withIdentity(jobName, groupName)
                                           .forJob(jobName, groupName)
                                           .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                                                              .withRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY)
                                                                              .withIntervalInMilliseconds(interval))
                                           .build();
        //
        schedulerService.rescheduleJob(jobName, groupName, newTrigger);
      } else {
        createJob();
      }
      //
      if (limit == 0) {
        limit = MAX_TO_SEND;
      }
      this.limit = limit;
      LOG.debug(String.format("Reschedule Job executes interval: %s limit: %s ", interval, limit));
    } catch (Exception e) {
      LOG.warn("Failed at rescheduleJob().");
      LOG.debug(e.getMessage(), e);
    }
  }
}
