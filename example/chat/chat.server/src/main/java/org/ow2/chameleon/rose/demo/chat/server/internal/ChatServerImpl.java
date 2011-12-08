package org.ow2.chameleon.rose.demo.chat.server.internal;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
//import org.ow2.chameleon.rose.demo.chat.client.ChatClient;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.rose.demo.chat.api.ChatClient;
import org.ow2.chameleon.rose.demo.chat.api.ChatServer;

@Component(name = "ChatServer")
@Instantiate
@Provides(properties = @StaticServiceProperty(name = "RoseChat", value = "server", type = "String"))
public class ChatServerImpl implements ChatServer {

	private ConcurrentHashMap<String, ChatClient> clients;
	private BundleContext context;

	public ChatServerImpl(BundleContext pContext) {
		super();
		this.context = pContext;
	}

	@SuppressWarnings("unused")
	@Validate
	private void start() {
		clients = new ConcurrentHashMap<String, ChatClient>();
	}

	public void sendMessage(String sendMessage, String login) {
		System.out.println("server got message: " + sendMessage);
		for (Entry<String, ChatClient> client : clients.entrySet()) {
			if (!(client.getKey().equals(login))) {
				// send message to others
				try {
					client.getValue().receiveMessage(login+": "+sendMessage);
				} catch (Exception e) {
					e.printStackTrace();
					clients.remove(client.getKey());
					// client not available
				}
			}
		}
	}

	public boolean loginUser(String login) {
		ServiceReference sref[] = null;
		String filter = "(login=" + login + ")";

		if (clients.containsKey(login)) {
			return false;
		}

		try {
			sref = context.getServiceReferences(ChatClient.class.getName(),
					filter);
			if (sref == null || sref.length > 1) {
				return false;
			}
			clients.put(login, (ChatClient) context.getService(sref[0]));
			// Send notification to others about new user
			this.sendMessage("User " + login + " logon to chat", login);
			return true;
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
			return false;
		}

	}

	public boolean logoffUser(String login) {
		clients.remove(login);
		this.sendMessage("User " + login + " logoff from chat", login);
		return true;
	}

}
