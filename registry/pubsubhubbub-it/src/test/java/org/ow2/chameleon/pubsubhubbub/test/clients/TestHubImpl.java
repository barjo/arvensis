package org.ow2.chameleon.pubsubhubbub.test.clients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.*;


/**
 * Testing Hub.
 * 
 * @author Bartek
 * 
 */

class TestHubImpl extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3950189218292421821L;
	private int responseStatus;
	private Map<String, Object> reqParams;
	private transient HttpService http;
	private transient HttpPost postMethod;
	private transient HttpClient client;

	public TestHubImpl(final HttpService pHttp, final int pResponseStatus) {
		this.http = pHttp;
		this.responseStatus = pResponseStatus;
	}

	/**
	 * Retrieve last POST request parameters.
	 * 
	 * @return map of parameters
	 */
	public Map<String, Object> getReqParams() {
		return reqParams;
	}

	public void changeResponseStatus(final int pResponseStatus) {
		this.responseStatus = pResponseStatus;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		reqParams = req.getParameterMap();
		resp.setStatus(responseStatus);
	}

	public void start() {
		try {
			http.registerServlet("/hub", this, null, null);
			client = new DefaultHttpClient(new ThreadSafeClientConnManager());
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (NamespaceException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		http.unregister("/hub");
	}

	public void sendUpdate(final String updateOption,
			final String publisherCallBackUrl, final EndpointDescription endp,
			final JSONService json) {
		HttpResponse response;

		postMethod = new HttpPost(publisherCallBackUrl);
		postMethod.setHeader("Content-Type", HTTP_POST_HEADER_TYPE);

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair(HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION,
				updateOption));
		nvps.add(new BasicNameValuePair(HTTP_POST_UPDATE_CONTENT, json
				.toJSON(endp.getProperties())));
		try {
			postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			response = client.execute(postMethod);
			response.getEntity().getContent().close();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
