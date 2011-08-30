package org.ow2.chameleon.rose.pubsubhubbub.webconsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.constants.RoseRSSConstants;

@Component(immediate = true)
@Provides
@Instantiate
public class PubsubhubbubWebConsole extends AbstractWebConsolePlugin implements
		EventHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4556092591635863191L;

	@ServiceProperty(name = "felix.webconsole.label")
	private String label = "Rose";

	@SuppressWarnings("unused")
	@ServiceProperty(name = "felix.webconsole.title")
	@Requires
	private JSONService json;


	private Set<String> localEndpoints;
	private BundleContext context;
	private ServiceRegistration eventService;
	private HttpClient client;
	private HttpPost postMethod;

	public PubsubhubbubWebConsole(BundleContext context) {
		super();
		this.context = context;
	}

	@Validate
	void start() {

		localEndpoints = new HashSet<String>();
		registerEventHandler();
	}

	@Invalidate
	void stop() {
		eventService.unregister();
	}

	private void registerEventHandler() {
		String eventTitleFilter = "(|(entry.title=" + RoseRSSConstants.FEED_TITLE_NEW
				+ ")(entry.title=" + RoseRSSConstants.FEED_TITLE_REMOVE + "))";
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(EventConstants.EVENT_TOPIC, RoseRSSConstants.RSS_EVENT_TOPIC);
		props.put(EventConstants.EVENT_FILTER, eventTitleFilter);
		eventService = context.registerService(EventHandler.class.getName(),
				this, props);

	}

	@Override
	public String getLabel() {

		return label;
	}

	@Override
	public String getTitle() {
		return "Rose";
	}

	@Override
	protected void renderContent(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		Integer number = 1;
		res.getWriter().append("Local endpoints:<br><br>");
		for (String endpoint : localEndpoints) {
			res.getWriter().append(number.toString() + ":<br>");
			number++;
			res.getWriter().append(endpoint + "<br><br>");

		}

		if (req.getParameter("hub.url") != null) {
			client = new DefaultHttpClient();

			postMethod = new HttpPost(req.getParameter("hub.url"));
			postMethod.setHeader("Content-Type",
					"application/x-www-form-urlencoded");

			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(
					RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE,
					"getAllEndpoints"));
			postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpResponse response = client.execute(postMethod);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED) {
				response.getEntity().getContent().close();
				throw new ClientProtocolException(
						"Error in get all endpoints from hub server");
			}

			res.getWriter().append(
					"Remote endpoints registered on hub:<br><br>");
			res.getWriter().append(asString(response.getEntity().getContent()));
		}
		StringBuffer sb = new StringBuffer();
		sb.append("<form>Hub url: <input type=\"text\" name=\"hub.url\" /><br /><input type=\"submit\" value=\"Submit\" /></form>");
		res.getWriter().append(sb);

	}

	public void handleEvent(Event event) {

		if (event.getProperty("entry.title").equals(RoseRSSConstants.FEED_TITLE_NEW)) {
			localEndpoints.add((String) event.getProperty("entry.content"));

		} else if (event.getProperty("entry.title").equals(
				RoseRSSConstants.FEED_TITLE_REMOVE)) {
			localEndpoints.remove((String) event.getProperty("entry.content"));
		}

	}

	private String asString(InputStream ists) throws IOException {
		if (ists != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader r1 = new BufferedReader(new InputStreamReader(
						ists, "UTF-8"));
				while ((line = r1.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} finally {
				ists.close();
			}
			return sb.toString();
		} else {
			return "";
		}
	}

}
