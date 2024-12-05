/* Copyright (C) 2019 Fraunhofer IESE
 * You may use, distribute and modify this code under the
 * terms of the Apache License 2.0 license
 */

package connect.sonarqube;

import com.google.gson.Gson;

import model.sonarqube.issues.SonarcubeIssuesResult;
import model.sonarqube.measures.SonarcubeMeasuresResult;
import rest.RESTInvoker;

import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;

/**
 * REST calls for Sonarqube data collection
 * @author wickenkamp
 *
 */
public class SonarqubeApi {
	
	
	public static SonarcubeMeasuresResult getMeasures(String sonarURL, String username, String password, String metricKeys, String sonarBaseComponentKey, int pageIndex) {

		try {
			URIBuilder uriBuilder = new URIBuilder(sonarURL + "/api/measures/component_tree");
			uriBuilder.addParameter("metricKeys", metricKeys);
			uriBuilder.addParameter("baseComponentKey", sonarBaseComponentKey);
			uriBuilder.addParameter("pageIndex", String.valueOf(pageIndex));

			RESTInvoker ri = new RESTInvoker(uriBuilder.toString(), username, password);
			
			Gson  gson = new Gson();
			return gson.fromJson(ri.getDataFromServer(""), SonarcubeMeasuresResult.class);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getMeasures from SonarqubeApi from sonarqube.", e);
		}
	
	}
	
	public static SonarcubeIssuesResult getIssues(String sonarUrl, String username, String password, String projectKeys, int p) {
		
		try {
			URIBuilder uriBuilder = new URIBuilder(sonarUrl + "/api/issues/search");
			uriBuilder.addParameter("projectKeys", projectKeys);
			uriBuilder.addParameter("p", String.valueOf(p));

			RESTInvoker ri = new RESTInvoker(uriBuilder.toString(), username, password);
			
			Gson  gson = new Gson();
			return gson.fromJson(ri.getDataFromServer(""), SonarcubeIssuesResult.class);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getIssues from SonarqubeApi from sonarqube.", e);
		}
	}
	
}
