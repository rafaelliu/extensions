import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.jcr.RepositoryService;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class MailAction implements CmsScript {

  private static Log LOG = ExoLogger.getLogger("ecm.MailAtion");
  
  private RepositoryService repositoryService;
  private MailService mailService;

  public MailAction(RepositoryService repositoryService, MailService mailService) {
    this.repositoryService = repositoryService;
    this.mailService = mailService;
  }
  
  public void execute(Object context) {
     Map variables = (Map) context;
     String to = variables.get("exo:to").toString();
     String from = variables.get("exo:from").toString();
     String subject = variables.get("exo:subject").toString();
     String message = variables.get("exo:message").toString();

     // get the new node
     String nodePath = (String) variables.get("nodePath") ;
     javax.jcr.Session session = repositoryService.getCurrentRepository().getSystemSession("collaboration");
     Node node = (Node) session.getItem(nodePath);

     try {
       subject = computeProperties(null, subject, node);
       message = computeProperties(null, message, node);

       javax.mail.Session mailSession = mailService.getMailSession();
       MimeMessage msg = new MimeMessage(mailSession);

       msg.setFrom(new InternetAddress(from));
       msg.setRecipient(RecipientType.TO, new InternetAddress(to));
     
       msg.setSubject(subject);
       msg.setContent(message, "text/html ; charset=ISO-8859-1");

       mailService.sendMessage(msg);
       LOG.info("Message was sent from " + from + " to " + to);
     } catch (Exception e) {
       LOG.error("Message sending failed", e);
     }
  }

  public void setParams(String[] params) {}

  
	private String computeProperties(String preamble, String str, Node node) throws ValueFormatException, IllegalStateException, RepositoryException, ScriptException {
		if (str == null) return null;

		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("groovy");

		// execute imports, function and variable definitions, etc
		if (preamble != null && !"".equals(preamble)) {
			engine.eval(preamble);
		}

		PropertyIterator it = node.getProperties();
		while (it.hasNext()) {
			Property prop = it.nextProperty();
			addVariable(engine, prop);
		}

		Matcher matcher = (str =~ /\$\{(.*?)\}/);
		while (matcher.find()) {
			String match = matcher.group(0);
			String expression = matcher.group(1);

			String value;
			try {
				value = engine.eval(expression).toString();
			} catch (Exception e) {
				value = expression;
			}

			str = str.replace(match, value);
		}
		return str;

	}

	private void addVariable(ScriptEngine engine, Property prop) throws ValueFormatException, IllegalStateException, RepositoryException {
		if (prop.getDefinition().isMultiple()) return;

		String name = stripNamespace(prop.getName());

		if (prop.getType() == PropertyType.STRING) {
			engine.put(name, prop.getValue().getString());
		} else if (prop.getType() == PropertyType.DATE) {
			engine.put(name, prop.getValue().getDate());
		} else if (prop.getType() == PropertyType.LONG) {
			engine.put(name, prop.getValue().getLong());
		} else if (prop.getType() == PropertyType.BOOLEAN) {
			engine.put(name, prop.getValue().getBoolean());
		}
	}

	private String stripNamespace(String str) {
		return str.substring(str.indexOf(":")+1);
	}

}
