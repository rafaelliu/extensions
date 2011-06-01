import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.jcr.RepositoryService;

public class ExpirationDateAction implements CmsScript {


	private static final String LIFECYCLE_PROP = "publication:lifecycleName";
	private static final String PUBLISH_DATE_PROP = "publication:endPublishedDate";
	private RepositoryService repositoryService;

	public ExpirationDateAction(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;  
	}


	public void execute(Object context) throws Exception {
		Map<String, Object> variables = (Map<String, Object>) context;
		
		String nodePath = (String) variables.get("nodePath") ;
		String workspace = (String) variables.get("srcWorkspace") ;
		String dateField = (String) variables.get("exo:dateField");
		
		// get the new node
		Session session = repositoryService.getCurrentRepository().getSystemSession(workspace);
		Property property = (Property) session.getItem(nodePath);
		Node node = property.getParent();
		
		if (nodePath.endsWith(LIFECYCLE_PROP)) {
			if (node.hasProperty(dateField)) {
				// exo is adding publication end date and we have set a publication field
				node.setProperty(PUBLISH_DATE_PROP, node.getProperty(dateField).getDate());
			}
		} else if (nodePath.endsWith(dateField)) {
			if (node.hasProperty(LIFECYCLE_PROP)) {
				// exo is adding de date field and we have the publication mixin already
				node.setProperty(PUBLISH_DATE_PROP, node.getProperty(dateField).getDate());
			}
		}
		
		session.save();
		session.logout();
	}

	public void setParams(String[] params) {}

}
 
