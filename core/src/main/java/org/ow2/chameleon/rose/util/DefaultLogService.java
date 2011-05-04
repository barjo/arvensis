package org.ow2.chameleon.rose.util;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class DefaultLogService implements LogService {

	public void log(int level, String message) {
		System.out.println("["+getLevel(level)+"] "+ message);

	}

	public void log(int level, String message, Throwable exception) {
		log(level, message);
		System.out.println("\n\t(Exception)");
		exception.printStackTrace();
		System.out.println("\n\t(^Exception)");

	}

	public void log(ServiceReference sr, int level, String message) {
		log(level, message);
		System.out.println("\n\t(ServiceReference)\n\t"+sr.toString());
		System.out.println("\n\t(^ServiceReference)");
	}

	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		log(sr, level, message);
		System.out.println("\n\t(Exception)");
		exception.printStackTrace();
		System.out.println("\n\t(^Exception)");
	}
	
	private static String getLevel(int level) {
		final String slevel;
		switch (level) {
		case LOG_DEBUG:
			slevel = "DEBUG";
			break;
		case LOG_ERROR:
			slevel = "ERROR";
			break;
		case LOG_INFO:
			slevel = "INFO";
			break;
		case LOG_WARNING:
			slevel = "WARNING";
			break;
		default:
			slevel = "BAD LEVEL";
			break;
		}

		return slevel;
	}

}
