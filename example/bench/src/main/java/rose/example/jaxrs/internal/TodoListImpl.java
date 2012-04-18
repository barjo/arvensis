package rose.example.jaxrs.internal;

import static org.osgi.service.jdbc.DataSourceFactory.JDBC_URL;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.jdbc.DataSourceFactory;

import rose.example.jaxrs.contract.Todo;
import rose.example.jaxrs.contract.TodoList;

@Component(name="bench_todolist")
@Instantiate(name="bench_todolist-1")
@Provides
public class TodoListImpl implements TodoList{
	@Requires
	private DataSourceFactory dsf;
	
	private Connection con;
	
	public void putTodo(Todo todo) {
		try{
			
			Statement stmt = con.createStatement();
			stmt.execute("INSERT into todolist (id,content) values " +
					"('"+todo.id()+"', " +
					"'"+todo.content()+"')");
			stmt.close();
			}catch(SQLException e){
				throw new RuntimeException(e);
			}
	}
	
	public Todo getTodo(String id) {
		try{
			Statement stmt = con.createStatement();
			ResultSet set;
			
			set = stmt.executeQuery("SELECT * from todolist where id = '"+id+"'");
			
			if (set.next())
				return new Todo(id, set.getString("content"));
			else 
				return null;
			
		}catch(SQLException e){
			throw new RuntimeException(e);
		}
	}
	
	public List<Todo> getAllTodo() {
		try{
			Statement stmt = con.createStatement();
			ResultSet set;
			
			set = stmt.executeQuery("SELECT * from todolist");
			
			List<Todo> todos = new ArrayList<Todo>();

			while (set.next()){
				todos.add(new Todo(set.getString("id"),  set.getString("content")));
			}

			return todos;
		}catch(SQLException e){
			throw new RuntimeException(e);
		}
	}


	@Validate
	private void start() throws Exception {
		Properties props = new Properties();
		props.put(JDBC_URL, "jdbc:sqlite::memory:");
		DataSource ds = dsf.createDataSource(props);
		con = ds.getConnection();
		Statement stmt = con.createStatement();
		
		//Create the table and fill it, for test purpose
		stmt.execute("CREATE TABLE todolist ( id primary key, content TEXT) ");
		stmt.execute("INSERT into todolist (id, content) values ('course', 'faire les courses')");
		stmt.execute("INSERT into todolist (id, content) values ('a', 'thèse a')");
		stmt.execute("INSERT into todolist (id, content) values ('b', 'thèse b')");
		stmt.execute("INSERT into todolist (id, content) values ('c', 'Mi')");
		stmt.close();
	}

	@Invalidate
	private void stop() throws Exception{
		con.close();
	}
}


