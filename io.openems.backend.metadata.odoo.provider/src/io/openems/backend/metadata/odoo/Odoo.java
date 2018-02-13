package io.openems.backend.metadata.odoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.plaf.synth.SynthSpinnerUI;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.edgewebsocket.api.EdgeWebsocketService;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.metadata.api.Role;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

@Designate(ocd = Odoo.Config.class, factory = false)
@Component(name = "Odoo", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Odoo implements MetadataService {

	private final Logger log = LoggerFactory.getLogger(Odoo.class);

	@ObjectClassDefinition
	@interface Config {
		String database();
		String uid();
		String password();
		String url() default "https://www1.fenecon.de";
	}

	private String url;
	private String database;
	private String uid;
	private String password;

	private Map<Integer, User> users = new HashMap<>();
	private Map<Integer, Edge> edges = new HashMap<>();

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC) // avoid recursive dependency
	private volatile EdgeWebsocketService edgeWebsocketService;
	
	@Activate
	void activate(Config config) {
		log.debug("Activate Odoo [url=" + config.url() + ";database=" + config.database() + ";uid=" + config.uid()
				+ ";password=" + (config.password() != null ? "ok" : "NOT_SET") + "]");
		this.url = config.url();
		this.database = config.database();
		this.uid = config.uid();
		this.password = config.password();
	}

	@Deactivate
	void deactivate() {
		log.debug("Deactivate Odoo");
	}

	/**
	 * Tries to authenticate at the Odoo server using a sessionId from a cookie.
	 * Updates the Session object accordingly.
	 *
	 * @param sessionId
	 * @return
	 * @throws OpenemsException
	 */
	@Override
	public User getUserWithSession(String sessionId) throws OpenemsException {
		HttpURLConnection connection = null;
		try {
			// send request to Odoo
			String charset = "US-ASCII";
			String query = String.format("session_id=%s", URLEncoder.encode(sessionId, charset));
			connection = (HttpURLConnection) new URL(this.url + "/openems_backend/info?" + query).openConnection();
			connection.setConnectTimeout(5000);// 5 secs
			connection.setReadTimeout(5000);// 5 secs
			connection.setRequestProperty("Accept-Charset", charset);
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");

			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			out.write("{}");
			out.flush();
			out.close();

			InputStream is = connection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while ((line = br.readLine()) != null) {
				JsonObject j = (new JsonParser()).parse(line).getAsJsonObject();
				if (j.has("error")) {
					JsonObject jError = JsonUtils.getAsJsonObject(j, "error");
					String errorMessage = JsonUtils.getAsString(jError, "message");
					throw new OpenemsException("Odoo replied with error: " + errorMessage);
				}

				if (j.has("result")) {
					// parse the result
					JsonObject jResult = JsonUtils.getAsJsonObject(j, "result");
					JsonObject jUser = JsonUtils.getAsJsonObject(jResult, "user");
					User user = new User(//
							JsonUtils.getAsInt(jUser, "id"), //
							JsonUtils.getAsString(jUser, "name"));
					JsonArray jDevices = JsonUtils.getAsJsonArray(jResult, "devices");
					for (JsonElement jDevice : jDevices) {
						Edge edge = new Edge(//
								JsonUtils.getAsInt(jDevice, "id"), //
								JsonUtils.getAsString(jDevice, "name"), //
								JsonUtils.getAsString(jDevice, "comment"), //
								JsonUtils.getAsString(jDevice, "producttype"));
						edge.setOnline(this.edgeWebsocketService.isOnline(edge.getId()));
						synchronized (this.edges) {
							this.edges.putIfAbsent(edge.getId(), edge);	
						}
						user.addEdgeRole(edge.getId(), Role.getRole(JsonUtils.getAsString(jDevice, "role")));
					}
					synchronized (this.users) {
						this.users.put(user.getId(), user);	
					}
					return user;
				}
			}
		} catch (IOException e) {
			throw new OpenemsException("IOException while reading from Odoo: " + e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		throw new OpenemsException("No result from Odoo");
	}

	@Override
	public int[] getEdgeIdsForApikey(String apikey) {
		// TODO Auto-generated method stub
		return new int[] { -1 };
	}

	@Override
	public Optional<Edge> getEdge(int edgeId) {
		return Optional.ofNullable(this.edges.get(edgeId));
	}

	@Override
	public void updateEdgeConfig(int edgeId, JsonObject jConfig) {
		// TODO Auto-generated method stub
		log.info("TODO: updateEdgeConfig");
	}

	// public Optional<User> getUser(int id) {
	// return Optional.ofNullable(this.users.get(id));
	// }
	//
	// public Optional<Device> getDevice(int id) {
	// return Optional.ofNullable(this.devices.get(id));
	// }
	//
	// // private
	// protected final ConcurrentMap<Integer, Device> devices = new
	// ConcurrentHashMap<>();
	// protected final ConcurrentMap<Integer, User> users = new
	// ConcurrentHashMap<>();

}
