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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.commons.notification.job.mbeans;

import java.util.Calendar;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.service.storage.NotificationService;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.impl.service.QueueMessageImpl;
import org.exoplatform.commons.notification.job.NotificationJob;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.groovyscript.GroovyTemplate;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

@PersistJobDataAfterExecution
public class NotificationDigestJob implements Job {
  
  private final Log LOG = ExoLogger.getLogger(NotificationDigestJob.class);
  
  public NotificationDigestJob() {
    try {
      new GroovyTemplate("");
    } catch (Exception e) {}
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      long startTime = System.currentTimeMillis();
      //
      JobDataMap data = context.getJobDetail().getJobDataMap();
      String digestType = data.getString(AbstractNotificationJobManager.DIGEST_TYPE);
      NotificationContext notifContext = NotificationContextImpl.cloneInstance();
      if ("daily".equals(digestType)) {
        LOG.info("Starting run DailyJob to send daily email notification ... ");
        notifContext.append(NotificationJob.JOB_DAILY, true);
        String dayName = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        notifContext.append(NotificationJob.DAY_OF_JOB, dayName);
      } else if ("weekly".equals(digestType)) {
        LOG.info("Starting run WeeklyJob to send weekly email notification ... ");
        notifContext.append(NotificationJob.JOB_WEEKLY, true);
      }
      CommonsUtils.getService(QueueMessageImpl.class).clearLogs();
      CommonsUtils.getService(NotificationService.class).digest(notifContext);
      long endTime = System.currentTimeMillis();
      //last execution duration
      data.put(AbstractNotificationJobManager.LAST_EXECUTION_DURATION, (endTime - startTime)/1000);
      //counter
      String countKey = AbstractNotificationJobManager.EXECUTION_COUNT;
      if (data.containsKey(countKey)) {
        data.put(countKey, data.getInt(countKey)+1);
      } else {
        data.put(countKey, 1);
      }
    } catch (Exception e) {
      LOG.error("DailyJob exception: ", e);
    }
  }
  
}
