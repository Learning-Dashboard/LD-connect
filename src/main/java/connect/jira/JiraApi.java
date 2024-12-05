package connect.jira;


import com.google.gson.Gson;

import model.jira.JQLResult;
import rest.RESTInvoker;

import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;

public class JiraApi {
	
	
	public static JQLResult getIssues(String jiraUrl, String username, String password, String jql, int startAt) {
		
		try {
			URIBuilder uriBuilder = new URIBuilder(jiraUrl + "/rest/api/2/search");
			uriBuilder.addParameter("jql", jql);
			uriBuilder.addParameter("startAt", String.valueOf(startAt));
			
			RESTInvoker ri = new RESTInvoker(uriBuilder.build().toString(), username, password);
			
			Gson gson = new Gson();
			
			String json = ri.getDataFromServer("");
			
			return gson.fromJson(json, JQLResult.class);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getIssues from JiraApi from jira.", e);
		}
		
		/**
		try {
			PrintWriter debug = new PrintWriter("jira.debug.txt");
			debug.print(json);
			debug.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}**/

	}

}
