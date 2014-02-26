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

import org.exoplatform.commons.api.notification.service.NotificationService;
import org.exoplatform.commons.notification.NotificationConfiguration;
import org.exoplatform.commons.notification.job.NotificationJob;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

@PersistJobDataAfterExecution
public class NotificationDigestJob extends NotificationJob {
  private final Log LOG        = ExoLogger.getLogger(NotificationDigestJob.class);
  private String    digestType = "";

  public NotificationDigestJob() {
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    long startTime = System.currentTimeMillis();
    //
    JobDataMap data = context.getJobDetail().getJobDataMap();
    digestType = data.getString(AbstractNotificationJobManager.DIGEST_TYPE);
    //
    super.execute(context);

    long endTime = System.currentTimeMillis();
    // last execution duration
    data.put(AbstractNotificationJobManager.LAST_EXECUTION_DURATION, (endTime - startTime) / 1000);
    // counter
    String countKey = AbstractNotificationJobManager.EXECUTION_COUNT;
    if (data.containsKey(countKey)) {
      data.put(countKey, data.getInt(countKey) + 1);
    } else {
      data.put(countKey, 1);
    }
  }

  @Override
  protected void processSendNotification() throws Exception {
    try {
      if ("daily".equals(digestType)) {
        LOG.info("Starting run DailyJob to send daily email notification ... ");
        CommonsUtils.getService(NotificationConfiguration.class).setSendWeekly(false);
        CommonsUtils.getService(NotificationService.class).processDigest();
      } else if ("weekly".equals(digestType)) {
        LOG.info("Starting run WeeklyJob to send weekly email notification ... ");
        CommonsUtils.getService(NotificationConfiguration.class).setSendWeekly(true);
        CommonsUtils.getService(NotificationService.class).processDigest();
      }
    } catch (Exception e) {
      LOG.warn(String.format("Failed to send %s digest emails.", digestType));
      LOG.debug(e);
    }
  }

}
