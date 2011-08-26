package org.ow2.chameleon.rss.test;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**Testing Hub
 * @author Bartek
 *
 */
class TestHub extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1780507086548421628L;
	private int responseStatus;
	private Map<String, Object> reqParams;
	private HttpService http;

	public TestHub(HttpService http, int responseStatus) {
		this.http = http;
		this.responseStatus = responseStatus;
	}

	/**
	 * Retrieve last POST request parameters
	 * 
	 * @return map of parameters
	 */
	public Map<String, Object> getReqParams() {
		return reqParams;
	}

	public void changeResponseStatus(int responseStatus) {
		this.responseStatus = responseStatus;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println("get something, sends: " + responseStatus);
		reqParams = req.getParameterMap();
		resp.setStatus(responseStatus);
	}

	public void start() {
		try {
			http.registerServlet("/hub", this, null, null);
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (NamespaceException e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		http.unregister("/hub");
	}
}
