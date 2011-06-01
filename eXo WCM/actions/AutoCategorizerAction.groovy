 
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;

public class AutoCategorizerAction implements CmsScript {


	private RepositoryService repositoryService;
	private TaxonomyService taxonomyService;

	public AutoCategorizerAction(TaxonomyService taxonomyService, RepositoryService repositoryService) {
		this.repositoryService = repositoryService;  
		this.taxonomyService = taxonomyService;
	}


	public void execute(Object context) throws Exception {
		Map<String, Object> variables = (Map<String, Object>) context;
		
		String nodePath = (String) variables.get("nodePath") ;
		String workspace = (String) variables.get("srcWorkspace") ;
//		String preamble = (String) variables.get("exo:preamble"); // TODO: seria interessante!
		String categoryPath = (String) variables.get("exo:categoryPath");
		String fallbackCategory = (String) variables.get("exo:fallbackCategory");
		
		// get the new node
		Session session = repositoryService.getCurrentRepository().getSystemSession(workspace);
		Node node = (Node) session.getItem(nodePath);

		// parse expression and make up path
		try {
			categoryPath = computeProperties(null, categoryPath, node);
			String[] path = splitCategory(categoryPath);
			if (path == null || path[0].isEmpty()) {
				throw new RuntimeException("Category path is invalid!!!");
			}
			taxonomyService.addCategory(node, path[0], path[1]);
		} catch (Exception e) {
			String[] path = splitCategory(fallbackCategory);
			taxonomyService.addCategory(node, path[0], path[1]);

			e.printStackTrace();
		}

		session.logout();
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
			
			String value = engine.eval(expression).toString();
			
			System.out.println(" >>>> " + expression +  " = "+ value);
			
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

	/**
	 * Splits category path into taxonomyName (index 0) and categoryPath (index 1).
	 * It handles leading and ending slashs. If there's no taxonomyName, returns "".
	 * If there's no categoryPath returns "/". Will return null is str is null.
	 * 
	 * @param str
	 * @return String array with [taxonomyName, categoryPath] 
	 */
	private String[] splitCategory(String str) {
		if (str == null) return null;
		
		if (str.startsWith("/")) {
			str = str.substring(1);
		}
		if (str.endsWith("/")) {
			str = str.substring(0, str.length()-1);
		}
		
		String[] result = str.split("/", 2);
		if (result.length < 2) {
			result = [result[0], "/"];
		}
		
		return result;
	}

	private String stripNamespace(String str) {
		return str.substring(str.indexOf(":")+1);
	}
}
