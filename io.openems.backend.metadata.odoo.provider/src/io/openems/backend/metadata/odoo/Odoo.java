package io.openems.backend.metadata.odoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

import com.google.gson.GsonBuilder;
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

		int uid();

		String password();

		String url() default "https://www1.fenecon.de";
	}

	private String url;
	private String database;
	private int uid;
	private String password;

	private Map<Integer, User> users = new HashMap<>();
	private Map<Integer, Edge> edges = new HashMap<>();

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
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
					switch (errorMessage) {
					case "Odoo Session Expired":
						// TODO handle exception
						throw new OpenemsException("Odoo Session Expired");
					default:
						throw new OpenemsException("Odoo replied with error: " + errorMessage);
					}
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
						int edgeId = JsonUtils.getAsInt(jDevice, "id");
						Optional<Edge> edgeOpt = this.getEdgeOpt(edgeId);
						if (edgeOpt.isPresent()) {
							Edge edge = edgeOpt.get();
							synchronized (this.edges) {
								this.edges.putIfAbsent(edge.getId(), edge);
							}
						}
						user.addEdgeRole(edgeId, Role.getRole(JsonUtils.getAsString(jDevice, "role")));
					}
					synchronized (this.users) {
						this.users.put(user.getId(), user);
					}
					return user;
				}
			}
			// TODO handle exception
			throw new OpenemsException("No matching user found");
		} catch (IOException e) {
			// TODO handle exception
			throw new OpenemsException("IOException while reading from Odoo: " + e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	@Override
	public int[] getEdgeIdsForApikey(String apikey) {
		try {
			// TODO use Odoo "search_read" method
			int[] edgeIds = OdooUtils.search(this.url, this.database, this.uid, this.password, "fems.device",
					new Domain("apikey", "=", apikey));
			// refresh Edge cache
			for (int edgeId : edgeIds) {
				this.getEdgeForceRefresh(edgeId);
			}
			return edgeIds;
		} catch (OpenemsException e) {
			log.error("Unable to get EdgeIds for Apikey: " + e.getMessage());
			return new int[] {};
		}
	}

	@Override
	public Optional<Edge> getEdgeOpt(int edgeId) {
		// try to read from cache
		synchronized (this.edges) {
			if (this.edges.containsKey(edgeId)) {
				return Optional.of(this.edges.get(edgeId));
			}
		}
		// if it was not in cache:
		return this.getEdgeForceRefresh(edgeId);
	}

	@Override
	public Optional<User> getUser(int userId) {
		// try to read from cache
		synchronized (this.users) {
			return Optional.ofNullable(this.users.get(userId));
		}
	}

	/**
	 * Reads the Edge object from Odoo and stores it in the cache
	 * 
	 * @param edgeId
	 * @return
	 */
	private Optional<Edge> getEdgeForceRefresh(int edgeId) {
		try {
			Map<String, Object> edgeMap = OdooUtils.readOne(this.url, this.database, this.uid, this.password,
					"fems.device", edgeId, Field.FemsDevice.NAME, Field.FemsDevice.COMMENT,
					Field.FemsDevice.PRODUCT_TYPE, Field.FemsDevice.OPENEMS_CONFIG);
			Edge edge = new Edge( //
					(Integer) edgeMap.get(Field.FemsDevice.ID.n()), //
					(String) edgeMap.get(Field.FemsDevice.NAME.n()), //
					(String) edgeMap.get(Field.FemsDevice.COMMENT.n()), //
					(String) edgeMap.get(Field.FemsDevice.PRODUCT_TYPE.n()), //
					JsonUtils.getAsJsonObject(
							JsonUtils.parse((String) edgeMap.get(Field.FemsDevice.OPENEMS_CONFIG.n()))));
			edge.onSetConfig(jConfig -> {
				// Update Edge config in Odoo
				String config = new GsonBuilder().setPrettyPrinting().create().toJson(jConfig);
				this.write(edge, new FieldValue(Field.FemsDevice.OPENEMS_CONFIG, config), false);
			});
			edge.onSetLastMessage(time -> {
				// Set LastMessage timestamp in Odoo
				this.write(edge,
						new FieldValue(Field.FemsDevice.LAST_MESSAGE, OdooUtils.DATETIME_FORMATTER.format(time)), true);
			});
			edge.onSetLastUpdate(time -> {
				// Set LastUpdate timestamp in Odoo
				this.write(edge,
						new FieldValue(Field.FemsDevice.LAST_UPDATE, OdooUtils.DATETIME_FORMATTER.format(time)), true);
			});
			edge.onSetSoc(soc -> {
				// Set SoC in Odoo
				this.write(edge, new FieldValue(Field.FemsDevice.SOC, String.valueOf(soc)), true);
			});
			edge.onSetIpv4(ipv4 -> {
				// Set IPv4 in Odoo
				this.write(edge, new FieldValue(Field.FemsDevice.IPV4, String.valueOf(ipv4)), true);
			});
			edge.setOnline(this.edgeWebsocketService.isOnline(edge.getId()));
			// store in cache
			synchronized (this.edges) {
				this.edges.put(edge.getId(), edge);
			}
			return Optional.ofNullable(edge);
		} catch (OpenemsException e) {
			log.error("Unable to read Edge [ID:" + edgeId + "]: " + e.getMessage());
			return Optional.empty();
		}
	}

	private final int DEBOUNCE_SECONDS = 60;

	private HashMap<String, Instant> lastWriteMap = new HashMap<>();

	private void write(Edge edge, FieldValue fieldValue, boolean debounce) {
		Instant now = Instant.now();
		boolean executeWrite = true;
		if (debounce) {
			// debounce = avoid writing too often
			synchronized (this.lastWriteMap) {
				Instant lastWrite = lastWriteMap.get(fieldValue.getField().n());
				if (lastWrite != null && now.minusSeconds(DEBOUNCE_SECONDS).isBefore(lastWrite)) {
					executeWrite = false;
				} else {
					this.lastWriteMap.put(fieldValue.getField().n(), now);
				}
			}
		}
		if (executeWrite) {
			try {
				OdooUtils.write(this.url, this.database, this.uid, this.password, "fems.device", edge.getId(),
						fieldValue);
			} catch (OpenemsException e) {
				log.error("Unable to update Edge [ID:" + edge.getName() + "] field [" + fieldValue.getField().n()
						+ "] : " + e.getMessage());
			}
		}
	}
}
