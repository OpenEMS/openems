package io.openems.backend.metadata.wordpress;

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
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;

import org.mariadb.jdbc.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import io.openems.common.exceptions.OpenemsException;

public class DbUtils {
	private String password;
	private Connection conn;
	private String url;
	private String wpurl;
	private String dbuser;
	private String dbname;
	private String dburl;

	private final Logger log = LoggerFactory.getLogger(DbUtils.class);

	public DbUtils(String dbuser, String p, String dbname, String dburl, String wpurl) {
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
			this.log.error("Cannot connect to Database: " + e.getMessage());
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
			this.log.error("Failed to get Edges from Database: " + e.getMessage());
		}

		return null;

	}

	public ResultSet getWpEdges() {
		try {
			Connection conn = DriverManager
					.getConnection(this.dburl + "/wordpress" + "?user=" + this.dbuser + "&password=" + this.password);
			Statement stmt = conn.createStatement();
			String sql = "SELECT * FROM wp_participants_database";
			ResultSet result = stmt.executeQuery(sql);
			conn.close();
			return result;

		} catch (SQLException e) {

			this.log.error("Failed to get Edges from Wordpress: " + e.getMessage());
		}

		return null;
	}

	public boolean addEdge(String apikey, String mac, String version) {
		Connection conn;
		try {
			conn = DriverManager
					.getConnection(this.dburl + "/wordpress" + "?user=" + this.dbuser + "&password=" + this.password);
			Statement stmt = conn.createStatement();
			String sql = "INSERT INTO wp_participants_database (apikey,mac,producttype,edge_name,edge_comment,serial) VALUES('"
					+ apikey + "', '" + mac + "', 'blueplanet hybrid 10.0 TL3', '" + version + "', '" + apikey + "', '"
					+ apikey + "')";
			int result = stmt.executeUpdate(sql);
			if (result == 1) {
				return true;
			}
		} catch (SQLException e) {
			return false;
		}

		return false;

	}

	public void writeEdge(String apikey, String name, String comment, String producttype, int id) {
		try {
			reconnect();
			Statement stmt = this.conn.createStatement();

			String sql = "INSERT INTO Edges (apikey,name,comment,producttype,id) VALUES ('" + apikey + "', '" + name
					+ "', '" + comment + "', '" + producttype + "', '" + id + "')";
			stmt.executeUpdate(sql);

		} catch (SQLException e) {

			this.log.error("Failed to write Edge to Database: " + e.getMessage());
		}
	}

	public MyUser getUserFromDB(String login, String sessionId) throws OpenemsException {

		MyUser user = createUser(sessionId);

		return user;

	}

	private MyUser createUser(String sessionId) throws OpenemsException {

		if (sessionId == null) {

			return new MyUser("0", "Gast", new ArrayList<String>(Arrays.asList("2", "4")), "guest");

		}

		JsonObject j = getWpResponse("/user/get_user_meta/?cookie=" + sessionId);
		if (j == null) {
			throw new OpenemsException("no response from Wordpress");
		}

		String nick = j.get("nickname").getAsString();
		JsonElement hasrole = j.get("primusrole");
		String role = null;
		String primus;
		ArrayList<String> edges = null;
		if (hasrole != null) {
			role = hasrole.getAsString();
			primus = j.get("primus").getAsString();
			edges = new ArrayList<String>(Arrays.asList(primus.split(",")));
		}

		JsonObject j2 = getWpResponse("/user/get_currentuserinfo/?cookie=" + sessionId);
		if (j2 == null) {
			throw new OpenemsException("no response from Wordpress");
		}
		JsonObject userinfo = j2.getAsJsonObject("user");
		String userid = userinfo.get("id").getAsString();

		if (edges == null) {
			String serial = j.get("bp_serial").getAsString();
			String mac = j.get("bp_mac").getAsString();
			edges = checkNewUser(serial, mac);
			role = "owner";
			updateNewUser(userid, edges, role);
		}

		String name = userinfo.get("firstname").getAsString() + " " + userinfo.get("lastname").getAsString();
		if (!name.matches(".*\\w.*")) {
			name = nick;
		}

		MyUser user = new MyUser(userid, name, edges, role);

		return user;
	}

	private boolean updateNewUser(String userid, ArrayList<String> edges, String role) throws OpenemsException {
		Connection conn;
		try {
			conn = DriverManager
					.getConnection(this.dburl + "/wordpress" + "?user=" + this.dbuser + "&password=" + this.password);
			Statement stmt = conn.createStatement();
			String sql = "INSERT INTO wp_usermeta (user_id,meta_key,meta_value) VALUES('" + userid + "', 'primus', '"
					+ edges.get(0) + "')";
			int result = stmt.executeUpdate(sql);
			if (result == 1) {
				sql = "INSERT INTO wp_usermeta (user_id,meta_key,meta_value) VALUES('" + userid + "', 'primusrole', '"
						+ role + "')";
				result = stmt.executeUpdate(sql);
				if (result == 1) {
					return true;
				}
			}
		} catch (SQLException e) {
			throw new OpenemsException("Couldn't update user: " + e.getMessage());
		}
		return false;

	}

	private ArrayList<String> checkNewUser(String serial, String mac) throws OpenemsException {

		if (serial == null || mac == null) {
			throw new OpenemsException("No valid User");
		}

		String id = getEdgeIdFromDB(serial, mac);
		if (id != null) {

			ArrayList<String> edges = new ArrayList<String>();
			edges.add(id);
			return edges;
		}

		return null;
	}

	private String getEdgeIdFromDB(String serial, String mac) throws OpenemsException {
		try {
			Connection conn = DriverManager
					.getConnection(this.dburl + "/wordpress" + "?user=" + this.dbuser + "&password=" + this.password);
			Statement stmt = conn.createStatement();
			String sql = "SELECT * FROM wp_participants_database WHERE apikey = " + serial + " AND mac = '" + mac + "'";
			ResultSet result = stmt.executeQuery(sql);
			conn.close();
			if (result.first()) {
				return result.getString("id");
			}

			throw new OpenemsException("No valid Edge in Database found!");

		} catch (SQLException e) {

			this.log.error("Failed to get Edges from Wordpress: " + e.getMessage());
		}
		return null;
	}

	public JsonObject getWpResponse(String urlparams) throws OpenemsException {
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

			this.log.error("Failed to get response from Wordpress: " + e.getMessage());
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		return null;

	}

}
