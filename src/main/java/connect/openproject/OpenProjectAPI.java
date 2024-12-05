package connect.openproject;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.google.gson.Gson;

import model.openproject.OPResult;
import model.openproject.OPWorkPackage;
import rest.RESTInvoker;

import org.apache.http.client.utils.URIBuilder;

public class OpenProjectAPI {


	private static TimeZone tzUTC = TimeZone.getTimeZone("UTC");
	private static DateFormat dfZULU = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

	static {
		dfZULU.setTimeZone(tzUTC);
	}
	
	public static OPResult getProjects(String openPeojectUrl, String username, String password) {		
		String json = null;
		Gson gson = new Gson();	
		try {
			URIBuilder uriBuilder = new URIBuilder(openPeojectUrl + "/api/v3/projects");
			RESTInvoker ri = new RESTInvoker(uriBuilder.toString(), username, password);	
			json = ri.getDataFromServer("");
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getProjects from OpenProjectAPI from openproject.", e);
		}
		return gson.fromJson(json, OPResult.class);
	}
	
	
	public static OPResult getWorkPackageByProject(String openPeojectUrl,int projectId, String username, String password, Date lastExecution) {		
		
		Gson gson = new Gson();
		String json; 
		try {
			URIBuilder uriBuilder = new URIBuilder(openPeojectUrl + "/api/v3/projects/" + projectId + "/work_packages/");
			if (lastExecution != null) {
				String fromDate = dfZULU.format(lastExecution);
				String toDate = dfZULU.format(new Date());
				String filters = "[{\"updatedAt\":{\"operator\":\"<>d\",\"values\":[\"" + fromDate + "\",\"" + toDate + "\"]}}]";
				uriBuilder.addParameter("filters", filters);
			} else {
				uriBuilder.addParameter("filters", "[]");
			}
			uriBuilder.addParameter("pageSize", "500");
			
			RESTInvoker ri = new RESTInvoker(uriBuilder.toString(), username, password);
			json = ri.getDataFromServer("");
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getWorkPackageByProject from OpenProjectAPI from openproject.", e);
		}
		return gson.fromJson(json, OPResult.class);
	}
	
	
	
	public static OPWorkPackage getWorkPackage(String openPeojectUrl,int workPackageId, String username, String password) {		
		String json = null;
		try {
			URIBuilder uriBuilder = new URIBuilder(openPeojectUrl + "/api/v3/work_packages/" + workPackageId);
			RESTInvoker ri = new RESTInvoker(uriBuilder.toString(), username, password);
			json = ri.getDataFromServer("");
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getWorkPackage from OpenProjectAPI from openproject.", e);
		}
		Gson  gson = new Gson();	
		return gson.fromJson(json, OPWorkPackage.class);
	}
}

