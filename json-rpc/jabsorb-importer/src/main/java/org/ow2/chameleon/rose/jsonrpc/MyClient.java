package org.ow2.chameleon.rose.jsonrpc;

import org.jabsorb.client.Client;

public class MyClient extends Client{
	private final MyHttpSession session;
	
	public MyClient(MyHttpSession pSession) {
		super(pSession);
		session=pSession;
	}
	
	@Override
	public void closeProxy(Object proxy) {
		super.closeProxy(proxy);
		session.close();
	}
}
