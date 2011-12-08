package org.ow2.chameleon.rose.demo.chat.api;

public interface ChatServer {

	public void sendMessage(String sendMessage, String login);
	
	public boolean loginUser(String login);
	
	public boolean logoffUser(String login);
}
