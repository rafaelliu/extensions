/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

import java.util.Map;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.jcr.RepositoryService;


public class SendMailScript implements CmsScript {

  private static Log LOG = ExoLogger.getLogger("ecm.SendMailScript");
  private static String DEFAULT_MAIL = "exosender@gmail.com";
  private static String DEFAULT_DESCRIPTION = "You have received a message from ";
  
  private RepositoryService repositoryService;

  public SendMailScript(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }
  
  public void execute(Object context) {               
     Map variables = (Map) context;                      
     String to = variables.get("exo:to").toString();     

     // get the new node
     String nodePath = (String) variables.get("nodePath") ;
     javax.jcr.Session session = repositoryService.getCurrentRepository().getSystemSession("collaboration");
     Node node = (Node) session.getItem(nodePath);

     try {
       String subject = node.getProperty("exo:fg_p_assunto").getString();              
       String message = node.getProperty("exo:fg_p_mensagem").getString();
       String from = node.getProperty("exo:fg_p_email").getString();
       ExoContainer myContainer = ExoContainerContext.getCurrentContainer();       
       MailService service = (MailService)myContainer.getComponentInstanceOfType(MailService.class);
       Session mailSession = service.getMailSession();
       MimeMessage msg = new MimeMessage(mailSession);
       if(from.equals("null") || from.isEmpty()) {
         from = DEFAULT_MAIL;
       }
     if(message.equals("null") || message.isEmpty()) {
       message = DEFAULT_DESCRIPTION + from;
     }
       msg.setFrom(new InternetAddress(from));
       msg.setRecipient(RecipientType.TO, new InternetAddress(to));
     
       msg.setSubject(subject);
       msg.setContent(message, "text/html ; charset=ISO-8859-1");
       service.sendMessage(msg);
       LOG.info("Message was sent from " + from + " to " + to);
     } catch (Exception e) {
       LOG.error("Message sending failed", e);
     }
  }

  public void setParams(String[] params) {}

}
