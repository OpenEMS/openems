package io.openems.backend.metadata.energydepot;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.mariadb.jdbc.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge.State;
import io.openems.common.OpenemsConstants;
import io.openems.common.session.Role;
import io.openems.common.utils.StringUtils;

public class DBUtils {
	private String password;
	private Connection conn;
	private final Logger log = LoggerFactory.getLogger(DBUtils.class);
	public DBUtils(String p) {
		this.password = p;
		try {
			DriverManager.registerDriver(new Driver());
			this.conn = DriverManager.getConnection("jdbc:mariadb://localhost:3306/primus?user=root2&password=" + this.password);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Map<Integer, MyEdge> getEdges() {
		
		Map<Integer, MyEdge> edges =  new HashMap<>();
		
		try {
			Statement stmt = this.conn.createStatement();
			String sql = "SELECT * FROM Edges";
			ResultSet result = stmt.executeQuery(sql);
			
			
			while(result.next()) {
				int id = result.getInt("Edges_id");
				String name = result.getString("name");
				String comment = result.getString("comment");
				String apikey = result.getString("apikey");
				String producttype = result.getString("producttype");
				
				Role role = Role.getRole("ADMIN");
				MyEdge edge = new MyEdge(id, apikey, name, comment, State.ACTIVE,
						OpenemsConstants.OPENEMS_VERSION, producttype, new JsonObject(), role );
				
				edge.onSetConfig(jConfig -> {
					log.debug(
							"Edge [" + id + "]. Update config: " + StringUtils.toShortString(jConfig, 100));
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
	
	public MyUser getUserFromDB(String pwdString){
		
		byte[] res = hashPassword(pwdString.toCharArray(), Base64.getDecoder().decode("YWRtaW4="));
		String pwd = Base64.getEncoder().encodeToString(res);
		
		MyUser user = null;
		
		Statement stmt;
		try {
			stmt = this.conn.createStatement();
			String sql = "SELECT * FROM users WHERE pwd = " + pwd;
			ResultSet result = stmt.executeQuery(sql);
			while(result.next()) {
				user = new MyUser(result.getInt("user_id"), result.getString("name"), result.getInt("edge_id"));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return user;
		
	}
	
	
	
	/**
	 * Source: https://www.owasp.org/index.php/Hashing_Java
	 *
	 * @param password
	 * @param salt
	 * @param iterations
	 * @param keyLength
	 * @return
	 */
	private static byte[] hashPassword(final char[] password, final byte[] salt) {
		try {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			PBEKeySpec spec = new PBEKeySpec(password, salt, 10, 256);
			SecretKey key = skf.generateSecret(spec);
			byte[] res = key.getEncoded();
			return res;

		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}
}
