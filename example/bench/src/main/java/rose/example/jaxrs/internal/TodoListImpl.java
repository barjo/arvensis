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
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.jdbc.DataSourceFactory;

import rose.example.jaxrs.contract.Todo;
import rose.example.jaxrs.contract.TodoList;

@Component(name = "bench_todolist")
@Provides
public class TodoListImpl implements TodoList {

	@Property(name = "jdbc.url", value = "jdbc:sqlite::memory:")
	private String jdbcurl;

	@Requires
	private DataSourceFactory dsf;

	private Connection con;

	public boolean delTodo(String id) {
		try {

			Statement stmt = con.createStatement();
			int res = stmt.executeUpdate("DELETE from todolist where id = \""
					+ id + "\"");
			stmt.close();

			return res == 1;

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void putTodo(Todo todo) {
		try {

			Statement stmt = con.createStatement();
			stmt.execute("INSERT into todolist (id,content) values " + "(\""
					+ todo.id() + "\", \"" + todo.content() + "\")");
			stmt.close();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Todo getTodo(String id) {
		try {
			Statement stmt = con.createStatement();
			ResultSet set;

			set = stmt.executeQuery("SELECT * from todolist where id = \"" + id
					+ "\"");

			if (set.next())
				return new Todo(id, set.getString("content"));
			else
				return null;

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Todo> getAllTodo() {
		try {
			Statement stmt = con.createStatement();
			ResultSet set;

			set = stmt.executeQuery("SELECT * from todolist");

			List<Todo> todos = new ArrayList<Todo>();

			while (set.next()) {
				todos.add(new Todo(set.getString("id"), set
						.getString("content")));
			}

			return todos;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Validate
	private void start() throws Exception {
		Properties props = new Properties();
		props.put(JDBC_URL, jdbcurl);
		DataSource ds = dsf.createDataSource(props);
		con = ds.getConnection();
	}

	@Invalidate
	private void stop() throws Exception {
		con.close();
	}
}
