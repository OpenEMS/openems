package io.openems.backend.metadata.energydepot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.HttpsURLConnection;

import org.mariadb.jdbc.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import io.openems.backend.metadata.api.Edge.State;
import io.openems.common.OpenemsConstants;
import io.openems.common.session.Role;
import io.openems.common.utils.StringUtils;

public class DBUtils {
	private String password;
	private Connection conn;
	private final Logger log = LoggerFactory.getLogger(DBUtils.class);
	private String url = "jdbc:mariadb://localhost:3306/primus?user=root2&password=";

	public DBUtils(String p) {
		this.password = p;
		try {
			DriverManager.registerDriver(new Driver());
			this.conn = DriverManager.getConnection(this.url + this.password);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void reconnect() throws SQLException {
		if (this.conn.isClosed()) {
			this.conn = DriverManager.getConnection(this.url + this.password);
		}
	}

	public Map<Integer, MyEdge> getEdges() {

		Map<Integer, MyEdge> edges = new HashMap<>();

		try {
			reconnect();
			Statement stmt = this.conn.createStatement();
			String sql = "SELECT * FROM Edges";
			ResultSet result = stmt.executeQuery(sql);

			while (result.next()) {
				int id = result.getInt("Edges_id");
				String name = result.getString("name");
				String comment = result.getString("comment");
				String apikey = result.getString("apikey");
				String producttype = result.getString("producttype");

				Role role = Role.getRole("ADMIN");
				MyEdge edge = new MyEdge(id, apikey, name, comment, State.ACTIVE, OpenemsConstants.OPENEMS_VERSION,
						producttype, new JsonObject(), role);

				edge.onSetConfig(jConfig -> {
					log.debug("Edge [" + id + "]. Update config: " + StringUtils.toShortString(jConfig, 100));
				});
				edge.onSetSoc(soc -> {
					log.debug("Edge [" + id + "]. Set SoC: " + soc);
				});
				edge.onSetIpv4(ipv4 -> {
					log.debug("Edge [" + id + "]. Set IPv4: " + ipv4);
				});
				log.debug("Adding Edge from DB: " + name + ", " + comment + ", " + apikey);
				edges.put(id, edge);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return edges;

	}

	public MyUser getUserFromDB(String login, String sessionId) {

		MyUser user = null;
		
		createUser(sessionId);
		
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
				int i = 0;
				while (edges.next()) {
					edge_ids_list.add(edges.getInt("edge_id"));
					i++;
				}

				user = new MyUser(result.getInt("user_id"), result.getString("name"), edge_ids_list,
						result.getString("role"));
			}
			if (user == null) {

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return user;

	}

	private boolean createUser(String sessionId) {

		HttpsURLConnection connection = null;

		try {
			connection = (HttpsURLConnection) new URL(
					"https://www.energydepot.de/api/user/get_user_meta/?cookie=" + sessionId).openConnection();
			connection.setConnectTimeout(5000);// 5 secs
			connection.setReadTimeout(5000);// 5 secs
			// connection.setRequestProperty("Accept-Charset", charset);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-length", "0");
			// connection.setDoOutput(true);
			connection.setRequestProperty("Accept", "application/json");
			// connection.setRequestProperty("Content-Type", "application/json");

			// OutputStreamWriter out = new
			// OutputStreamWriter(connection.getOutputStream());
			// out.write("");
			// out.flush();
			// out.close();
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
					log.info(line);
					if (line.isEmpty()) {
						continue;
					}

					JsonObject j = (new JsonParser()).parse(line).getAsJsonObject();

					if (j.has("status")) {
						// parse the result
						status = j.get("status").getAsString();

						if (status.equals("ok")) {
							String nick = j.get("nickname").getAsString();
							String role = j.get("primusrole").getAsString();
							String primus = j.get("primus").getAsString();
							String[] edges = primus.split(",");

							reconnect();
							Statement stmt;
							stmt = this.conn.createStatement();

							String sql = "INSERT INTO users (role,edge_id,name,login) VALUES ('" + role + "', '"
									+ edges[0] + "', '" + nick + "', '" + nick + "')";
							stmt.executeUpdate(sql);
							sql = "SELECT * FROM users WHERE login = '" + nick + "'";

							ResultSet result = stmt.executeQuery(sql);
							while(result.next()) {
								int id = result.getInt("user_id");
								if (id == 0) {
									return false;
								}
								for (String edge_id : edges) {
									sql = "INSERT INTO user_edges (user_id, edge_id) VALUES (" + id + ", " + edge_id + ")";
									stmt.executeUpdate(sql);
								}
							}
							

							return true;

						}
					}
				}
			}
		} catch (JsonSyntaxException | IOException e) {

			e.printStackTrace();
			return false;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return false;

	}

}
