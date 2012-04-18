package rose.example.jaxrs.contract;

import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;

public class Todo {
	private final Map<String, String> todo = new HashMap<String, String>();
	
	public Todo(String id, String content) {
		todo.put("id", id);
		todo.put("content", content);
	}

	public String id(){
		return todo.get("id");
	}
	
	public String content(){
		return todo.get("content");
	}
	
	public Map<String,String> toMap(){
		return unmodifiableMap(todo);
	}
}
