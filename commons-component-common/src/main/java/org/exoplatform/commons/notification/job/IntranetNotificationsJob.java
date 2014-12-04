package org.exoplatform.commons.notification.job;

import org.exoplatform.commons.api.notification.service.storage.IntranetNotificationDataStorage;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class IntranetNotificationsJob implements Job {

  protected static final Log LOG = ExoLogger.getLogger(NotificationJob.class);
  
  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    
    IntranetNotificationDataStorage dataStorage = CommonsUtils.getService(IntranetNotificationDataStorage.class);
    try {
      dataStorage.remove(30);
    } catch (Exception e) {
      LOG.info("Error in cleaning web notifications in one month." + e);
    }
  }

}
