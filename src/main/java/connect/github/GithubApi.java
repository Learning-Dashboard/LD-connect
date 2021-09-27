package connect.github;

import com.google.gson.Gson;
import model.github.*;
import net.sf.json.JSONObject;
import rest.RESTInvoker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

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

	public static GithubIssues getIssues(String url, String secret, String updatedSince, State state, int offset) {

		String api = "/issues";
		String apiparams = "?since="+ updatedSince +"&state=" + state.getValue() + "&order_by=updated_at&sort=desc&page=" + offset;
		String urlCall = url + api + apiparams;
		RESTInvoker ri = new RESTInvoker(urlCall, secret);
		String json = ri.getDataFromServer("");
		model.github.Issue[] iss = gson.fromJson(json, model.github.Issue[].class);

		GithubIssues giss = new GithubIssues();
		giss.issues=iss;
		giss.total_count = new Long(iss.length);
		giss.offset = new Long(offset);
		return giss;
	}

	// Returns all users that contributed to the repo and their n# of contributions
	public static GithubContributor getContributors(String url, String secret, boolean anon, int offset) {

		String api = "/contributors";
		String apiparams = "?anon=" + anon + "&page=" + offset;
		String urlCall = url + api + apiparams;
		RESTInvoker ri = new RESTInvoker(urlCall, secret);
		String json = ri.getDataFromServer("");
		model.github.User[] con = gson.fromJson(json, model.github.User[].class);

		GithubContributor gcon = new GithubContributor();
		gcon.users=con;
		gcon.total_count = (long) con.length;
		gcon.offset = (long) offset;
		return gcon;
	}

	// Returns all repository labels
	public static GithubLabels getLabels(String url, String secret, int offset) {

		String api = "/labels";
		String apiparams = "?page=" + offset;
		String urlCall = url + api + apiparams;
		RESTInvoker ri = new RESTInvoker(urlCall, secret);
		String json = ri.getDataFromServer("");
		model.github.Label[] la = gson.fromJson(json, model.github.Label[].class);

		GithubLabels gla = new GithubLabels();
		gla.labels=la;
		gla.total_count = (long) la.length;
		gla.offset = (long) offset;
		return gla;
	}

	// Returns the repository specified by the url
	public static Repository getRepository(String url, String secret) {

		RESTInvoker ri = new RESTInvoker(url, secret);
		String json = ri.getDataFromServer("");
        return gson.fromJson(json, Repository.class);
	}

	// Returns the repository milestones
	public static GithubMilestones getMilestones(String url, String secret, State state, int offset) {

		String api = "/milestones";
		String apiparams = "?state=all&sort=desc&page=" + offset + "&state=" + state.getValue();
		String urlCall = url + api + apiparams;
		RESTInvoker ri = new RESTInvoker(urlCall, secret);
		String json = ri.getDataFromServer("");
		model.github.Milestone[] mile = gson.fromJson(json, model.github.Milestone[].class);

		GithubMilestones gmile = new GithubMilestones();
		gmile.milestones=mile;
		gmile.total_count = (long) mile.length;
		gmile.offset = (long) offset;
		return gmile;
	}

	public static void main(String[] args) {
		GithubIssues ri = getIssues("https://api.github.com/repos/q-rapids/qrapids-dashboard","HsdhNpJXdhpgpd7bkJtB", "2021-01-01", State.CLOSED,1);
		for (Issue a: ri.issues) {
			System.out.println(a.id + ' ' + a.state);
		}

	}
	
}
