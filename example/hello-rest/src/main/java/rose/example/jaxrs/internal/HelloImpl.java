package rose.example.jaxrs.internal;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import rose.example.jaxrs.contract.HelloWorld;

@Component(name="example_helloworld")
@Instantiate(name="example_helloworld-1")
@Provides
@Path(value="/helloworld/{name}")
public class HelloImpl implements HelloWorld {

	@GET
	@Produces("text/plain")
	public String hello(@PathParam("name") String name) {
		return "Hello "+name+" !";
	}
	
	@GET
	@Produces("application/json")
	public String helloJson(@PathParam("name") String name){
		return "{hello : \""+hello(name)+"\"}";
	}

}
