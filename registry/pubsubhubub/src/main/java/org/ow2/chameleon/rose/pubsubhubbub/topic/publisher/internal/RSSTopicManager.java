package org.ow2.chameleon.rose.pubsubhubbub.topic.publisher.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

public class RSSTopicManager {

	private String alias;
	private HttpService httpService;
	private SyndFeed feed;

	public RSSTopicManager(String host, String topicURL,
			HttpService httpService, final BundleContext context)
			throws ServletException, NamespaceException {
		this.alias = topicURL;
		this.httpService = httpService;

		ClassLoader bundle = this.getClass().getClassLoader();
		ClassLoader thread = Thread.currentThread().getContextClassLoader();
		// Switch
		Thread.currentThread().setContextClassLoader(bundle);
		feed = new SyndFeedImpl();
		feed.setTitle("Publisher for Pubsubhubbub");
		feed.setDescription("Publisher for Pubsubhubbub");
		feed.setLink(host + topicURL);
		feed.setFeedType("rss_2.0");
		// Restore
		Thread.currentThread().setContextClassLoader(thread);

		httpService.registerServlet(alias, new FeedServlet(), null,
				new HttpContext() {

					public String getMimeType(String name) {
						return null;
					}

					public URL getResource(String name) {
						URL url = context.getBundle().getResource(name);
						return url;
					}

					public boolean handleSecurity(HttpServletRequest request,
							HttpServletResponse response) throws IOException {
						return true;
					}

				});
	}

	void stop() {
		httpService.unregister(alias);
	}

	@SuppressWarnings("unchecked")
	synchronized void addEntry(String title, String content) {
		SyndEntry theEntry = new SyndEntryImpl();
		SyndContent theContent = new SyndContentImpl();
		theEntry.setTitle(title);
		theContent.setValue(content);

		theEntry.setPublishedDate(new Date());
		theEntry.setDescription(theContent);

		feed.getEntries().add(0, theEntry);

	}

	private class FeedServlet extends HttpServlet {
		/**
		 * 
		 */
		private static final long serialVersionUID = -3454917792460332803L;

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			ClassLoader bundle = this.getClass().getClassLoader();
			ClassLoader thread = Thread.currentThread().getContextClassLoader();

			try {
				// Switch
				Thread.currentThread().setContextClassLoader(bundle);
				SyndFeedOutput output = new SyndFeedOutput();
				output.output(feed, resp.getWriter(), true);
				resp.getWriter().flush();
				resp.getWriter().close();
			} catch (FeedException e) {
				throw new ServletException("Cannot render the feed", e);
			} finally {
				// Restore
				Thread.currentThread().setContextClassLoader(thread);
			}

		}

	}
}
