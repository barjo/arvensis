package org.ow2.chameleon.rose.demo.client;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.ow2.chameleon.rose.demo.api.DemoServiceAPI;


@Component
@Instantiate
public class Client {

	@Requires
	DemoServiceAPI demoService;

	BundleContext context;

	@Validate
	public void start() {
		System.out.println("Service called: "+demoService.getGreetings());
	}

	public Client(BundleContext context) {
//		super();
//		this.context = context;
//
//		DemoServiceAPI obj = (DemoServiceAPI) context.getService(context
//				.getServiceReference(DemoServiceAPI.class.getName()));
//
//		try {
//			System.out.println(obj.getGreetings());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

	}
}
