package io.openems.backend.metadata.energydepot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import org.mariadb.jdbc.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import io.openems.common.exceptions.OpenemsException;

public class DBUtils {
	private String password;
	private Connection conn;
	private String url;
	private String wpurl;
	private String dbuser;
	private String dbname;
	private String dburl;
	
	private final Logger log = LoggerFactory.getLogger(DBUtils.class);

	public DBUtils(String dbuser, String p, String dbname, String dburl, String wpurl) {
		this.password = p;
		this.wpurl = wpurl;
		this.dbuser = dbuser;
		this.dbname = dbname;
		this.dburl = dburl;
		this.url = dburl + "/" + this.dbname + "?user=" + this.dbuser + "&password=" + this.password;

		try {
			DriverManager.registerDriver(new Driver());
			this.conn = DriverManager.getConnection(this.url);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void reconnect() throws SQLException {
		if (this.conn.isClosed()) {
			this.conn = DriverManager.getConnection(this.url);
		}
	}

	public ResultSet getEdges() {

		try {
			reconnect();
			Statement stmt = this.conn.createStatement();
			String sql = "SELECT * FROM Edges";
			ResultSet result = stmt.executeQuery(sql);

			return result;

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;

	}

	public ResultSet getWPEdges() {
		try {
			Connection conn = DriverManager
					.getConnection(this.dburl + "/wordpress" + "?user=" + this.dbuser + "&password=" + this.password);
			Statement stmt = conn.createStatement();
			String sql = "SELECT * FROM wp_participants_database";
			ResultSet result = stmt.executeQuery(sql);
			conn.close();
			return result;

		} catch (SQLException e) {

			e.printStackTrace();
		}

		return null;
	}

	public void writeEdge(String apikey, String name, String comment, String producttype, int id) {
		try {
			reconnect();
			Statement stmt = this.conn.createStatement();

			String sql = "INSERT INTO Edges (apikey,name,comment,producttype,id) VALUES ('" + apikey + "', '" + name
					+ "', '" + comment + "', '" + producttype + "', '" + id + "')";
			stmt.executeUpdate(sql);

		} catch (SQLException e) {

			e.printStackTrace();
		}
	}

	public MyUser getUserFromDB(String login, String sessionId) throws OpenemsException {

		MyUser user = null;
		
		if(createUser(sessionId) == false) {
			log.warn("Error in createUser");
		}

		Statement stmt;
		try {
			reconnect();
			stmt = this.conn.createStatement();
			String sql = "SELECT * FROM users WHERE login = '" + login + "'";
			ResultSet result = stmt.executeQuery(sql);

			ArrayList<Integer> edge_ids_list = new ArrayList<>();

			while (result.next()) {

				sql = "SELECT edge_id FROM user_edges WHERE user_id = " + result.getInt("user_id");

				ResultSet edges = stmt.executeQuery(sql);

				while (edges.next()) {
					edge_ids_list.add(edges.getInt("edge_id"));

				}

				user = new MyUser(result.getInt("user_id"), result.getString("name"), edge_ids_list,
						result.getString("role"));
			}
			if (user == null) {

			}

		} catch (SQLException e) {

			e.printStackTrace();
		}

		return user;

	}

	private boolean createUser(String sessionId) throws OpenemsException {

		if (sessionId == null) {
			return false;
			
		}

		JsonObject j = getWPResponse("/user/get_user_meta/?cookie=" + sessionId);
		if (j == null) {
			throw new OpenemsException("no response from Wordpress");
		}
		String nick = j.get("nickname").getAsString();
		String role = j.get("primusrole").getAsString();
		String primus = j.get("primus").getAsString();
		String[] edges = primus.split(",");

		j = getWPResponse("/user/get_currentuserinfo/?cookie=" + sessionId);
		if (j == null) {
			throw new OpenemsException("no response from Wordpress");
		}
		JsonObject userinfo = j.getAsJsonObject("user");

		String name = userinfo.get("firstname").getAsString() + " " + userinfo.get("lastname").getAsString();
		if (!name.matches(".*\\w.*")) {
			name = nick;
		}
		String email = userinfo.get("email").getAsString();

		try {
			reconnect();
			Statement stmt;
			stmt = this.conn.createStatement();

			String sql = "INSERT INTO users (role,edge_id,name,email,login) VALUES ('" + role + "', '" + edges[0]
					+ "', '" + name + "', '" + email + "', '" + nick + "')";
			stmt.executeUpdate(sql);
			sql = "SELECT * FROM users WHERE login = '" + nick + "'";

			ResultSet result = stmt.executeQuery(sql);
			while (result.next()) {
				int id = result.getInt("user_id");
				if (id == 0) {
					throw new OpenemsException("error creating new User ID");
				}
				for (String edge_id : edges) {
					sql = "INSERT INTO user_edges (user_id, edge_id) VALUES (" + id + ", " + edge_id + ")";
					stmt.executeUpdate(sql);
				}
			}
			return true;
		} catch (SQLException e) {

			log.warn(e.getMessage());
		}
			return false;
	}

	public JsonObject getWPResponse(String urlparams) throws OpenemsException {
		HttpsURLConnection connection = null;

		try {
			connection = (HttpsURLConnection) new URL(this.wpurl + "/api" + urlparams).openConnection();
			connection.setConnectTimeout(5000);// 5 secs
			connection.setReadTimeout(5000);// 5 secs

			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-length", "0");

			connection.setRequestProperty("Accept", "application/json");

			connection.connect();
			int responseCode = connection.getResponseCode();

			InputStream is = connection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;

			String status;

			switch (responseCode) {
			case 200:
			case 201:
				while ((line = br.readLine()) != null) {

					if (line.isEmpty()) {
						continue;
					}

					JsonObject j = (new JsonParser()).parse(line).getAsJsonObject();

					if (j.has("status")) {
						// parse the result
						status = j.get("status").getAsString();

						if (status.equals("ok")) {
							return j;

						}
						if (status.equals("error")) {
							throw new OpenemsException("Authentication Error: " + j.get("error").getAsString());
						}
					}
				}
			}
		} catch (JsonSyntaxException | IOException e) {

			e.printStackTrace();
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return null;

	}

}
