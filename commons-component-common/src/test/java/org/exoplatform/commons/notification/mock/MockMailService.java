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
package org.exoplatform.commons.notification.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;

public class MockMailService implements MailService {
  private static final Log LOG = ExoLogger.getExoLogger(MockMailService.class);
  private List<String> sentUser = new ArrayList<String>();

  @Override
  public Session getMailSession() {
    return null;
  }

  @Override
  public String getOutgoingMailServer() {
    return null;
  }

  @Override
  public void sendMessage(String from, String to, String subject, String body) throws Exception {
  }

  @Override
  public void sendMessage(Message message) throws Exception {
    String sendTo = message.getTo();
    sentUser.add(sendTo);
    if (NotificationUtils.isValidEmailAddresses(sendTo)) {
      LOG.info("Sent mail notification to email: " + sendTo);
    } else {
      LOG.warn("Failed to sending mail notification to email: " + sendTo);
    }
  }
  
  public List<String> getAndClearSentsUser() {
    List<String> list = new ArrayList<String>(sentUser);
    sentUser.clear();
    return list;
  }

  @Override
  public void sendMessage(MimeMessage message) throws Exception {

  }

  @Override
  public Future<Boolean> sendMessageInFuture(String from, String to, String subject, String body) {
    return null;
  }

  @Override
  public Future<Boolean> sendMessageInFuture(Message message) {
    return null;
  }

  @Override
  public Future<Boolean> sendMessageInFuture(MimeMessage message) {
    return null;
  }

}
