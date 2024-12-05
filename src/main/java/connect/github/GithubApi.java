package connect.github;

import com.google.gson.Gson;
import model.github.*;
import model.github.branch.Branch;
import model.github.branch.GitHubBranches;
import model.github.commit.Commit;
import model.github.commit.CommitStats;
import rest.RESTInvoker;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.http.client.utils.URIBuilder;

public class GithubApi {

	public enum State {
		ALL("all"), OPEN("open"), CLOSED("closed");
		State(String v) {
			value = v;
		}
		private final String value;
		public String getValue() {
			return value;
		}
	}

	private static TimeZone tzUTC = TimeZone.getTimeZone("UTC");
	private static DateFormat dfZULU = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

	static {
		dfZULU.setTimeZone(tzUTC);
	}

	private static Gson gson = new Gson();

	public static GithubIssues getIssues(String url, String secret, String updatedSince, State state, int offset) throws RuntimeException{
		
		String api = "/issues";
		String json = null;
		try {
			URIBuilder builder = new URIBuilder(url + api);
			builder.addParameter("since", updatedSince);
			builder.addParameter("state", state.getValue());
			builder.addParameter("order_by", "updated_at");
			builder.addParameter("sort", "desc");
			builder.addParameter("per_page", "100");
			builder.addParameter("page", Integer.toString(offset));
			api = builder.build().toString();
			String urlCall = builder.build().toString();
			RESTInvoker ri = new RESTInvoker(urlCall, secret);
			json = ri.getDataFromServer("");

		} catch (URISyntaxException e) {
            throw new RuntimeException("Error building URI at getIssues from GithubApi from github.", e);
        }
		
		model.github.Issue[] iss = gson.fromJson(json, model.github.Issue[].class);

		GithubIssues giss = new GithubIssues();
		giss.issues=iss;
		giss.total_count = (long) iss.length;
		giss.offset = (long) offset;
		return giss;
	}

	// Returns all users that contributed to the repo and their n# of contributions
	public static GitHubBranches getBranches(String url, String secret, int offset) throws RuntimeException {

		String api = "/branches";
		String json = null;
		try {
			URIBuilder builder = new URIBuilder(url + api);
			builder.addParameter("per_page", "100");
			builder.addParameter("page", Integer.toString(offset));
			String urlCall = builder.build().toString();
			RESTInvoker ri = new RESTInvoker(urlCall, secret);
			json = ri.getDataFromServer("");

		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getBranches from GithubApi from github.", e);
		}

		Branch[] con = gson.fromJson(json, Branch[].class);

		GitHubBranches gbr = new GitHubBranches();
		gbr.branches = con;
		gbr.total_count = (long) con.length;
		gbr.offset = (long) offset;
		return gbr;
	}

	// Returns all users that contributed to the repo and their n# of contributions
	public static GithubUsers getCollaborators(String url, String secret, int offset) throws RuntimeException {

		try {
			String api = "/collaborators";
			URIBuilder builder = new URIBuilder(url + api);
			builder.addParameter("per_page", "100");
			builder.addParameter("page", Integer.toString(offset));
			String urlCall = builder.build().toString();
			RESTInvoker ri = new RESTInvoker(urlCall, secret);
			String json = ri.getDataFromServer("");
			User[] coll = gson.fromJson(json, User[].class);

			GithubUsers gcoll = new GithubUsers();
			gcoll.users = coll;
			gcoll.total_count = (long) coll.length;
			gcoll.offset = (long) offset;
			return gcoll;

		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getCollaborators from GithubApi from github.", e);
		} catch (RuntimeException e) {

			if(e.getMessage().equals(RESTInvoker.HTTP_STATUS_FORBIDDEN)) {
				throw new RuntimeException(e);
			}

			System.out.println("COLLABORATORS: Could not fetch the collaborators from " + url);
			System.out.println(e.getMessage());

			GithubUsers gcoll = new GithubUsers();
			gcoll.users = new User[]{};
			gcoll.total_count = (long) 0;
			gcoll.offset = (long) offset;
			return gcoll;
		}
	}

	// Returns all repository labels
	public static GithubLabels getLabels(String url, String secret, int offset) throws RuntimeException {

		String api = "/labels";
		String json = null;
		try {
			URIBuilder builder = new URIBuilder(url + api);
			builder.addParameter("page", Integer.toString(offset));
			String urlCall = builder.build().toString();
			RESTInvoker ri = new RESTInvoker(urlCall, secret);
			json = ri.getDataFromServer("");
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getLabels from GithubApi from github.", e);
		}

		model.github.Label[] la = gson.fromJson(json, model.github.Label[].class);

		GithubLabels gla = new GithubLabels();
		gla.labels = la;
		gla.total_count = (long) la.length;
		gla.offset = (long) offset;
		return gla;
	}

	// Returns the repository specified by the url
	public static Repository getRepository(String url, String secret) throws RuntimeException{
		RESTInvoker ri = new RESTInvoker(url, secret);
		String json = ri.getDataFromServer("");
		return gson.fromJson(json, Repository.class);
	}

	// Returns the repository milestones
	public static GithubMilestones getMilestones(String url, String secret, State state, int offset) throws RuntimeException {

		String api = "/milestones";
		String json = null;
		try {
			URIBuilder builder = new URIBuilder(url + api);
			builder.addParameter("state", state.getValue());
			builder.addParameter("sort", "desc");
			builder.addParameter("page", Integer.toString(offset));
			String urlCall = builder.build().toString();
			RESTInvoker ri = new RESTInvoker(urlCall, secret);
			json = ri.getDataFromServer("");
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getMilestones from GithubApi from github.", e);
		}
		model.github.Milestone[] mile = gson.fromJson(json, model.github.Milestone[].class);

		GithubMilestones gmile = new GithubMilestones();
		gmile.milestones = mile;
		gmile.total_count = (long) mile.length;
		gmile.offset = (long) offset;
		return gmile;
	}

	// Returns all commits made by a user
	public static GitHubCommits getCommits(String url, String secret, String branch, int offset) throws RuntimeException {
		try {
			URIBuilder builder = new URIBuilder(url + "/commits");
			builder.addParameter("sha", branch);
			builder.addParameter("per_page", "100");
			builder.addParameter("page", String.valueOf(offset));
			String urlCall = builder.build().toString();
			RESTInvoker ri = new RESTInvoker(urlCall, secret);
			String json = ri.getDataFromServer("");
			Commit[] commits = gson.fromJson(json, Commit[].class);

			GitHubCommits gcommit = new GitHubCommits();
			gcommit.commits = commits;
			gcommit.total_count = (long) commits.length;
			gcommit.offset = (long) offset;
			return gcommit;

		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getCommits from GithubApi from github.", e);
		} catch (RuntimeException e) {
			if(e.getMessage().equals(RESTInvoker.HTTP_STATUS_FORBIDDEN)) {
				throw new RuntimeException(e);
			}

			System.out.println("COMMITS: Commit API error in branch " + branch);
			System.out.println(e.getMessage());

			GitHubCommits gcommit = new GitHubCommits();
			gcommit.commits = new Commit[]{};
			gcommit.total_count = (long) 0;
			gcommit.offset = (long) offset;
			return gcommit;
		}
	}

	public static CommitStats getCommitInfo(String url, String secret, String commitSha) throws RuntimeException {
		String json = null;
		try {
			URIBuilder builder = new URIBuilder(url + "/commits/" + commitSha);
			String urlCall = builder.build().toString();

			RESTInvoker ri = new RESTInvoker(urlCall, secret);
			json = ri.getDataFromServer("");
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getCommitInfo from GithubApi from github.", e);
		}
		return gson.fromJson(json, CommitStats.class);
	}

	public static Repository[] getReposFromOrganization(String orgName, String secret) {
		String api = "https://api.github.com/orgs/" + orgName + "/repos";
		String json = null;
		try {
			URIBuilder builder = new URIBuilder(api);
			String urlCall = builder.build().toString();
			RESTInvoker ri = new RESTInvoker(urlCall, secret);
			json = ri.getDataFromServer("");
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error building URI at getReposFromOrganization from GithubApi from github.", e);
		}
		return gson.fromJson(json, Repository[].class);
	}

	public static void main(String[] args) {

		String secret = "";


		GitHubCommits ri = null;
		for(int i = 1; i <= 80; ++i)
			ri = getCommits("https://api.github.com/repos/q-rapids/learning-dashboard", null, "master", i);


		//GitHubCommits ri = getCommits("https://api.github.com/repos/q-rapids/learning-dashboard", null, "master", 1);

		for(Commit c : ri.commits){
			System.out.println(c.parents.size());
			System.out.println(c.parents.get(0).sha);
		}
	}
	
}
