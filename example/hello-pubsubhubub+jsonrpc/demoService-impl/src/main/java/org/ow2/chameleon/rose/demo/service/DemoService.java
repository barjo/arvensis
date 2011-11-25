package org.ow2.chameleon.rose.demo.service;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.ow2.chameleon.rose.demo.api.DemoServiceAPI;


@Component
@Instantiate(name="demo")
@Provides
public class DemoService implements DemoServiceAPI{

	private static String GREETING= "Hello Rose!";
	
	@Override
	public String getGreetings() {
		return GREETING;
	}

}
