package io.openems.backend.metadata.odoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Edge.State;
import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Metadata.Odoo", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Odoo extends AbstractOpenemsBackendComponent implements Metadata {

	public final static String ODOO_MODEL = "edge.device";

	private final static int READ_BATCH_SIZE = 300;
	private final static int MAX_TRIES = 10;

	private final Logger log = LoggerFactory.getLogger(Odoo.class);
	private final OdooWriteWorker writeWorker;

	private OdooCredentials odooCredentials;

	/**
	 * Maps User-ID to User
	 */
	private ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
	/**
	 * Caches Edges
	 */
	private EdgeCache edges = new EdgeCache();

	private CompletableFuture<Void> initializeEdgesTask = null;

	public Odoo() {
		super("Metadata.Odoo");
		this.writeWorker = new OdooWriteWorker(this);
	}

	@Activate
	void activate(Config config) {
		this.logInfo(this.log, "Activate [url=" + config.url() + ";database=" + config.database() + ";uid="
				+ config.uid() + ";password=" + (config.password() != null ? "ok" : "NOT_SET") + "]");
		this.odooCredentials = OdooCredentials.fromConfig(config);
		this.writeWorker.start(odooCredentials);
		this.initializeEdgesTask = CompletableFuture.runAsync(this.initializeEdges);
	}

	@Deactivate
	void deactivate() {
		this.logInfo(this.log, "Deactivate");
		this.writeWorker.stop();
		if (this.initializeEdgesTask != null) {
			this.initializeEdgesTask.cancel(true);
		}
	}

	/**
	 * Reads all Edges from Odoo and puts them in a local Cache.
	 */
	private Runnable initializeEdges = () -> {
		// get the Odoo-IDs for each Edge
		int[] edgeIds;
		try {
			edgeIds = OdooUtils.search(this.odooCredentials, ODOO_MODEL);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to search Edges from Odoo: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		// read Edge records from Odoo in batches
		for (int firstIndex = 0; firstIndex < edgeIds.length; firstIndex += READ_BATCH_SIZE) {
			this.logInfo(this.log, "Reading batch of [" + READ_BATCH_SIZE + "] starting from [" + firstIndex + "]");

			// collect Odoo-IDs for batch
			int lastIndex = firstIndex + READ_BATCH_SIZE - 1 > edgeIds.length ? edgeIds.length
					: firstIndex + READ_BATCH_SIZE - 1;
			Integer[] batchEdgeIds = new Integer[lastIndex - firstIndex + 1];
			// note: Odoo explicitly needs Integer[] and not int[]
			for (int i = 0; i <= lastIndex - firstIndex; i++) {
				batchEdgeIds[i] = edgeIds[firstIndex + i];
			}

			// read data from Odoo
			Map<String, Object>[] edgeMaps = null;
			boolean retry = true;
			int tries = MAX_TRIES;
			while (tries-- > 0 && retry) {
				try {
					edgeMaps = OdooUtils.readMany(Odoo.this.odooCredentials, ODOO_MODEL, //
							batchEdgeIds, //
							new Field[] { Field.EdgeDevice.ID, Field.EdgeDevice.APIKEY, Field.EdgeDevice.NAME,
									Field.EdgeDevice.COMMENT, Field.EdgeDevice.OPENEMS_VERSION,
									Field.EdgeDevice.PRODUCT_TYPE, Field.EdgeDevice.OPENEMS_CONFIG,
									Field.EdgeDevice.SOC, Field.EdgeDevice.IPV4, Field.EdgeDevice.STATE });
					retry = false;
				} catch (OpenemsException e) {
					this.logError(this.log, "Unable to read Edges from Odoo: " + e.getMessage());
					e.printStackTrace();
				}
			}
			if (tries == 0 || edgeMaps == null) {
				this.logError(this.log,
						"Unable to read read batch of [" + READ_BATCH_SIZE + "] from [" + firstIndex + "]");
				continue;
			}

			// parse fields from Odoo
			for (Map<String, Object> edgeMap : edgeMaps) {
				// simple fields
				Integer odooId = OdooUtils.getAsInteger(edgeMap.get(Field.EdgeDevice.ID.n()));
				String edgeId = OdooUtils.getAsString(edgeMap.get(Field.EdgeDevice.NAME.n()));
				String apikey = OdooUtils.getAsString(edgeMap.get(Field.EdgeDevice.APIKEY.n()));
				String comment = OdooUtils.getAsString(edgeMap.get(Field.EdgeDevice.COMMENT.n()));
				String version = OdooUtils.getAsString(edgeMap.get(Field.EdgeDevice.OPENEMS_VERSION.n()));
				String productType = OdooUtils.getAsString(edgeMap.get(Field.EdgeDevice.PRODUCT_TYPE.n()));
				String initialIpv4 = OdooUtils.getAsString(edgeMap.get(Field.EdgeDevice.IPV4.n()));
				Integer initialSoc = OdooUtils.getAsInteger(edgeMap.get(Field.EdgeDevice.SOC.n()));

				// Config
				EdgeConfig config;
				String configString = OdooUtils.getAsString(edgeMap.get(Field.EdgeDevice.OPENEMS_CONFIG.n()));
				if (configString.isEmpty()) {
					config = new EdgeConfig();
				}
				try {
					config = EdgeConfig.fromJson(//
							JsonUtils.getAsJsonObject(//
									JsonUtils.parse(configString)));
				} catch (OpenemsNamedException e) {
					this.logDebug(this.log, "Unable to read Edge Config for Odoo-ID [" + odooId + "] Edge-ID [" + edgeId
							+ "]: " + e.getMessage());
					config = new EdgeConfig();
				}

				// State
				String stateString = OdooUtils.getAsString(edgeMap.get(Field.EdgeDevice.STATE.n()));
				State state;
				try {
					state = State.valueOf(stateString.toUpperCase().replaceAll("-", "_"));
				} catch (IllegalArgumentException e) {
					this.logWarn(this.log,
							"Edge [" + edgeId + "]. Unable to get State from [" + stateString + "]: " + e.getMessage());
					state = State.INACTIVE; // Default
				}

				// Create instance of Edge and register listeners
				MyEdge edge = new MyEdge(//
						odooId, //
						edgeId, apikey, comment, state, version, productType, config, initialSoc, initialIpv4);
				this.addListeners(edge);

				// store in cache
				this.edges.add(edge);
			}
		}
		this.logInfo(this.log, "Reading batches finished");
	};

	/**
	 * Adds Listeners to act on changes to Edge.
	 * 
	 * @param edge the Edge
	 */
	private void addListeners(MyEdge edge) {
		edge.onSetOnline(isOnline -> {
			if (isOnline && edge.getState().equals(State.INACTIVE)) {
				// Update Edge state to active
				this.logInfo(this.log,
						"Mark Edge [" + edge.getId() + "] as ACTIVE. It was [" + edge.getState().name() + "]");
				edge.setState(State.ACTIVE);
				this.write(edge, new FieldValue(Field.EdgeDevice.STATE, "active"));
			}
		});
		edge.onSetConfig(config -> {
			// Update Edge config in Odoo
			this.logDebug(this.log,
					"Edge [" + edge.getId() + "]. Update config: " + StringUtils.toShortString(config.toJson(), 100));
			JsonObject jConfig = config.toJson();
			String conf = new GsonBuilder().setPrettyPrinting().create().toJson(jConfig);
			this.write(edge, new FieldValue(Field.EdgeDevice.OPENEMS_CONFIG, conf));
		});
		edge.onSetLastMessage(() -> {
			// Set LastMessage timestamp in Odoo
			this.writeWorker.onLastMessage(edge);
		});
		edge.onSetLastUpdate(() -> {
			// Set LastUpdate timestamp in Odoo
			this.writeWorker.onLastUpdate(edge);
		});
		edge.onSetVersion(version -> {
			// Set Version in Odoo
			this.logInfo(this.log, "Edge [" + edge.getId() + "]: Update OpenEMS Edge version to [" + version
					+ "]. It was [" + edge.getVersion() + "]");
			this.write(edge, new FieldValue(Field.EdgeDevice.OPENEMS_VERSION, version));
		});
		edge.onSetSoc(soc -> {
			// Set SoC in Odoo
			this.write(edge, new FieldValue(Field.EdgeDevice.SOC, String.valueOf(soc)));
		});
		edge.onSetIpv4(ipv4 -> {
			// Set IPv4 in Odoo
			this.write(edge, new FieldValue(Field.EdgeDevice.IPV4, String.valueOf(ipv4)));
		});
	}

	/**
	 * Tries to authenticate at the Odoo server using a sessionId from a cookie.
	 *
	 * @param sessionId
	 * @return
	 * @throws OpenemsException
	 */
	@Override
	public User authenticate(String sessionId) throws OpenemsNamedException {
		JsonObject jsonrpcResponse;
		HttpURLConnection connection = null;
		try {
			// send request to Odoo
			String charset = "US-ASCII";
			String query = String.format("session_id=%s", URLEncoder.encode(sessionId, charset));
			connection = (HttpURLConnection) new URL(this.odooCredentials.getUrl() + "/openems_backend/info?" + query)
					.openConnection();
			connection.setConnectTimeout(5000);// 5 secs
			connection.setReadTimeout(5000);// 5 secs
			connection.setRequestProperty("Accept-Charset", charset);
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");

			try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream())) {
				out.write("{}");
				out.flush();
			}

			// read JSON-RPC response
			StringBuilder sb = new StringBuilder();
			String line = null;
			try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
			}
			jsonrpcResponse = JsonUtils.getAsJsonObject(JsonUtils.parse(sb.toString()));
		} catch (IOException e) {
			throw new OpenemsException("IOException while reading from Odoo: " + e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

		// JSON-RPC Error
		if (jsonrpcResponse.has("error")) {

			JsonObject error = JsonUtils.getAsJsonObject(jsonrpcResponse, "error");
			String errorMessage = JsonUtils.getAsString(error, "message");
			throw new OpenemsException("Odoo replied with error: " + errorMessage);
		}

		if (!jsonrpcResponse.has("result")) {
			throw new OpenemsException("Odoo replied invalid JSON-RPC: " + jsonrpcResponse);
		} else {

			// JSON-RPC Success
			JsonObject result = JsonUtils.getAsJsonObject(jsonrpcResponse, "result");
			JsonObject jUser = JsonUtils.getAsJsonObject(result, "user");
			MyUser user = new MyUser(//
					JsonUtils.getAsInt(jUser, "id"), //
					JsonUtils.getAsString(jUser, "name"));
			JsonArray jDevices = JsonUtils.getAsJsonArray(result, "devices");
			List<String> notAvailableEdges = new ArrayList<>();
			for (JsonElement jDevice : jDevices) {
				int odooId = JsonUtils.getAsInt(jDevice, "id");
				MyEdge edge = this.edges.getEdgeFromOdooId(odooId);
				if (edge == null) {
					notAvailableEdges.add(String.valueOf(odooId));
				} else {
					user.addEdgeRole(edge.getId(), Role.getRole(JsonUtils.getAsString(jDevice, "role")));
				}
			}
			if (!notAvailableEdges.isEmpty()) {
				this.logWarn(this.log, "For User [" + user.getId() + "] following Edges are not available: "
						+ String.join(",", notAvailableEdges));
			}
			this.users.put(user.getId(), user);
			return user;
		}
	}

	/**
	 * Writes one field to Odoo.
	 * 
	 * @param edge       the Edge
	 * @param fieldValue the FieldValue
	 */
	private void write(MyEdge edge, FieldValue fieldValue) {
		try {
			OdooUtils.write(this.odooCredentials, ODOO_MODEL, new Integer[] { edge.getOdooId() }, fieldValue);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to update Edge [" + edge.getId() + "] Odoo-ID [" + edge.getOdooId()
					+ "] Field [" + fieldValue.getField().n() + "] : " + e.getMessage());
		}
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	public Optional<String> getEdgeIdForApikey(String apikey) {
		return this.edges.getEdgeIdFromApikey(apikey);
	}

	@Override
	public Optional<Edge> getEdge(String edgeId) {
		return Optional.ofNullable(this.edges.getEdgeFromEdgeId(edgeId));
	}

	@Override
	public Optional<User> getUser(String userId) {
		return Optional.ofNullable(this.users.get(userId));
	}

	@Override
	public Collection<Edge> getAllEdges() {
		return this.edges.getAllEdges();
	}
}
