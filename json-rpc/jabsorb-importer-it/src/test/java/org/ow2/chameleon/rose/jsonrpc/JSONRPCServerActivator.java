package org.ow2.chameleon.rose.jsonrpc;

import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.Hashtable;

import org.jabsorb.JSONRPCServlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class JSONRPCServerActivator implements BundleActivator, ServiceTrackerCustomizer{
		private static final String SERVLETNAME = "/JSONRPC"; 
		ServiceTracker httptracker;
		private BundleContext context;

		public void start(BundleContext pContext) throws Exception {
			context=pContext;
			httptracker = new ServiceTracker(context,HttpService.class.getName(),this);
			httptracker.open();
		}

		public void stop(BundleContext context) throws Exception {
			httptracker.close();
		}

		public Object addingService(ServiceReference reference) {
			HttpService httpservice = (HttpService) context.getService(reference);
			Dictionary<String, String> props = new Hashtable<String, String>();
			props.put("gzip.threshold", "200");
			try {
				httpservice.registerServlet(SERVLETNAME, new JSONRPCServlet(), props, null);
			} catch (Exception e) {
				fail(e.getMessage());
			}
			
			return httpservice;
		}

		public void modifiedService(ServiceReference reference, Object service) {
		}

		public void removedService(ServiceReference reference, Object service) {
			HttpService httpservice = (HttpService) service;
			httpservice.unregister(SERVLETNAME);

			context.ungetService(reference);
		}
		
	}