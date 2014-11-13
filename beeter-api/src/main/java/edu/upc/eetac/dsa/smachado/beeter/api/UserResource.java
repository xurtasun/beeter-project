package edu.upc.eetac.dsa.smachado.beeter.api;



import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;





import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.codec.digest.DigestUtils;

import edu.upc.eetac.dsa.smachado.beeter.api.model.User;

@Path("/users")
public class UserResource {
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();

	
	@Context
	private SecurityContext security;
	
	private final static String GET_USER_BY_USERNAME_QUERY = "select * from users where username=?";
	private final static String INSERT_USER_INTO_USERS = "insert into users values(?, MD5(?), ?, ?)";
	private final static String INSERT_USER_INTO_USER_ROLES = "insert into user_roles values (?, 'registered')";
	private final static String DELETE_USER_QUERY = "delete from users where username =?";
	private final static String UPDATE_USER_QUERY = "update users set email=ifnull(?, email) where username=?";
	
	@POST
	@Consumes(MediaType.BEETER_API_USER)
	@Produces(MediaType.BEETER_API_USER)
	public User createUser(User user) {
		validateUser(user);

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		PreparedStatement stmtGetUsername = null;
		PreparedStatement stmtInsertUserIntoUsers = null;
		PreparedStatement stmtInsertUserIntoUserRoles = null;
		try {
			stmtGetUsername = conn.prepareStatement(GET_USER_BY_USERNAME_QUERY);
			stmtGetUsername.setString(1, user.getUsername());

			ResultSet rs = stmtGetUsername.executeQuery();
			if (rs.next())
				throw new WebApplicationException(user.getUsername()
						+ " already exists.", Status.CONFLICT);
			rs.close();

			conn.setAutoCommit(false);
			stmtInsertUserIntoUsers = conn
					.prepareStatement(INSERT_USER_INTO_USERS);
			stmtInsertUserIntoUserRoles = conn
					.prepareStatement(INSERT_USER_INTO_USER_ROLES);

			stmtInsertUserIntoUsers.setString(1, user.getUsername());
			stmtInsertUserIntoUsers.setString(2, user.getPassword());
			stmtInsertUserIntoUsers.setString(3, user.getName());
			stmtInsertUserIntoUsers.setString(4, user.getEmail());
			stmtInsertUserIntoUsers.executeUpdate();

			stmtInsertUserIntoUserRoles.setString(1, user.getUsername());
			stmtInsertUserIntoUserRoles.executeUpdate();
			//Aun no esta en la base de datos 
			conn.commit();
			//Ahora si, ya sta en la base de datos
		} catch (SQLException e) {
			if (conn != null)
				try {
					conn.rollback();
				} catch (SQLException e1) {
				}
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmtGetUsername != null)
					stmtGetUsername.close();
				if (stmtInsertUserIntoUsers != null)
					stmtGetUsername.close();
				if (stmtInsertUserIntoUserRoles != null)
					stmtGetUsername.close();
				conn.setAutoCommit(true);
				conn.close();
			} catch (SQLException e) {
			}
		}
		user.setPassword(null);
		return user;
	}

	
	@PUT
	@Path("/{username}")
	@Consumes(MediaType.BEETER_API_USER)
	@Produces(MediaType.BEETER_API_USER)
	public User updateUser(@PathParam("username") String username, User user) { // UPDATE
		System.out.println("Actualizando User....");
		validateUserConnected(username);
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(UPDATE_USER_QUERY);
			stmt.setString(1, user.getEmail());
			stmt.setString(2, username);

			

			int rows = stmt.executeUpdate();
			if (rows == 1){
				user = getUserFromDatabase(username,true);
				System.out.println("User con username: "+ user.getUsername() +" ha sido actualizado.");
			}
			
			else {
				throw new NotFoundException("El usuario "+username);
			}

		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		return user;
	}
	
	@DELETE
	@Path("/{username}")
	public void deleteUser(@PathParam("username") String username) { // DELETE
		System.out.println("Borrando user....");
		validateUserConnected(username);
		System.out.println("username----2"+ username);
	
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		User user = new User();
		user = getUserFromDatabase(security.getUserPrincipal().getName(),true);
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(DELETE_USER_QUERY);
			stmt.setString(1, username);

			int rows = stmt.executeUpdate();
			
			System.out.println("User con el username: "+ user.getUsername()+" borrado.");
			
			if (rows == 0)
				;// Deleting inexistent sting
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
	}

	
	private void validateUser(User user) {
		if (user.getUsername() == null)
			throw new BadRequestException("username cannot be null.");
		if (user.getPassword() == null)
			throw new BadRequestException("password cannot be null.");
		if (user.getName() == null)
			throw new BadRequestException("name cannot be null.");
		if (user.getEmail() == null)
			throw new BadRequestException("email cannot be null.");
	}
	
	private void validateUserConnected(String username){
		System.out.println("comparo"+ security.getUserPrincipal().getName() +"yyy"+username);
		if(!security.getUserPrincipal().getName().equals(username))
			throw new ForbiddenException("No puedes modificar el usuario de otra persona");
		
	}

	@Path("/login")
	@POST
	@Produces(MediaType.BEETER_API_USER)
	@Consumes(MediaType.BEETER_API_USER)
	public User login(User user) {
		if (user.getUsername() == null || user.getPassword() == null)
			throw new BadRequestException(
					"username and password cannot be null.");

		String pwdDigest = DigestUtils.md5Hex(user.getPassword());
		String storedPwd = getUserFromDatabase(user.getUsername(), true)
				.getPassword();

		user.setLoginSuccessful(pwdDigest.equals(storedPwd));//compara los dos md5 (almacenado en la BDD y el que transforma del obtenido)
		user.setPassword(null);
		return user;
	}

	private User getUserFromDatabase(String username, boolean password) {
		User user = new User();
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(GET_USER_BY_USERNAME_QUERY);
			stmt.setString(1, username);

			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				user.setUsername(rs.getString("username"));
				if (password)
					user.setPassword(rs.getString("userpass"));
				user.setEmail(rs.getString("email"));
				user.setName(rs.getString("name"));
			} else
				throw new NotFoundException(username + " not found.");
		} catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}

		return user;
	}
}