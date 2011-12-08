package org.ow2.chameleon.rose.demo.chat.client.internal;

import java.io.PrintStream;
import java.util.StringTokenizer;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.shell.Command;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.chameleon.rose.demo.chat.api.ChatClient;
import org.ow2.chameleon.rose.demo.chat.api.ChatServer;

@Component(name = "ChatClient")
@Provides(properties = @StaticServiceProperty(name = "RoseChat", value = "client", type = "String"), specifications = { org.ow2.chameleon.rose.demo.chat.api.ChatClient.class })
public class ChatClientImpl implements ChatClient, Command,
		ServiceTrackerCustomizer {

	private ChatServer chatServer;

	@ServiceProperty(name = "login", value = "client1")
	private String login;

	private transient BundleContext context;
	private String command;
	private String sendMessage;
	private boolean loginStatus;
	private ServiceTracker serverTracker;

	public ChatClientImpl(BundleContext pcontext) {
		this.context = pcontext;
	}

	@SuppressWarnings("unused")
	@Validate
	private void start() {
		context.registerService(Command.class.getName(), this, null);
		serverTracker = new ServiceTracker(context, ChatServer.class.getName(),
				this);
		serverTracker.open();
	}

	@SuppressWarnings("unused")
	@Invalidate
	private void stop() {
		if (loginStatus) {
			chatServer.logoffUser(login);
		}

	}

	public void receiveMessage(String message) {
		System.out.println("chat: " + message);
	}

	public void execute(String line, PrintStream arg1, PrintStream arg2) {
		StringTokenizer st = new StringTokenizer(line, " ");

		// Ignore the command name.
		st.nextToken();
		command = st.nextToken();
		if (command.equals("-m") && loginStatus == true) {
			// sends message
			sendMessage = st.nextToken();
			while (st.hasMoreTokens()) {
				sendMessage = sendMessage + " " + st.nextToken();
			}
			chatServer.sendMessage(sendMessage, login);
		} else if ((command.equals("-l"))) {
			// login
			if ((chatServer != null)
					&& (loginStatus = chatServer.loginUser(login))) {
				System.out.println("Successfully logon");
			} else {
				System.out.println("Unsuccessfully logon");
			}

		} else if (command.equals("-m") && loginStatus == false) {
			System.out.println("Not logon");
		} else {
			System.out.println("Wrong usage");
		}
	}

	public String getName() {
		return "chat";
	}

	public String getShortDescription() {
		return "login and sends message";
	}

	public String getUsage() {
		return "chat [-m message] [-l]";
	}

	public Object addingService(ServiceReference reference) {
		chatServer = (ChatServer) context.getService(reference);
		return chatServer;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public void removedService(ServiceReference reference, Object service) {
		chatServer = null;
		loginStatus = false;
		System.out.println("Server disappeared");

	}

}
