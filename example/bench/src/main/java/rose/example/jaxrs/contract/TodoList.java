package rose.example.jaxrs.contract; 

import java.util.List;


public interface TodoList {
	Todo getTodo(String id);
	boolean delTodo(String id);
	void putTodo(Todo todo);
	List<Todo> getAllTodo();
}

