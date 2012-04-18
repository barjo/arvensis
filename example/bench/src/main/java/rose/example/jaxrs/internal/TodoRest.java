package rose.example.jaxrs.internal;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.status;

import java.text.ParseException;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.ow2.chameleon.json.JSONService;

import rose.example.jaxrs.contract.Todo;
import rose.example.jaxrs.contract.TodoList;

@Component(name="todo_rest")
@Instantiate
@Provides(specifications=TodoRest.class)
@Path(value="/task")
public class TodoRest {
	@Requires
	private JSONService json;
	
	@Requires
	private TodoList tlist;

	@GET
	@Path("{id}")
	@Produces(APPLICATION_JSON+"; charset=UTF-8")
	public String getTodo(@PathParam("id") String id) {
			Todo todo = tlist.getTodo(id);
			if (todo != null)
				return json.toJSON(tlist.getTodo(id).toMap());
			else 
				return "{}";
	}
	
	@GET
	@Produces(APPLICATION_JSON+"; charset=UTF-8")
	public String getAll(){
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (Todo todo : tlist.getAllTodo()) {
			sb.append(json.toJSON(todo.toMap()));
			sb.append(",");
		}
		sb.delete(sb.length() -1 , sb.length());
		sb.append(']');
		
		return sb.toString();
	}
	
	@PUT
	@Consumes(APPLICATION_JSON+"; charset=UTF-8")
	public Response putTodo(String content){
		try{	
			@SuppressWarnings("rawtypes")
			Map todo = json.fromJSON(content);
			tlist.putTodo(new Todo((String)todo.get("id"), (String) todo.get("content")));
			return ok().build();
		}
		catch (ParseException e) {
			return status(400).build();
		} catch (RuntimeException e) {
			return serverError().build();
		}
	}
}
