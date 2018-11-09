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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.edgewebsocket.api.EdgeWebsocketService;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Edge.State;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Metadata.Odoo", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Odoo implements MetadataService {

	private final Logger log = LoggerFactory.getLogger(Odoo.class);
	private final int READ_BATCH_SIZE = 100;
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final ExecutorService readEdgeExecutor = Executors.newSingleThreadExecutor();
	private Future<?> readEdgeFuture = null;

	protected String url;
	protected String database;
	protected int uid;
	protected String password;

	private Map<Integer, User> users = new HashMap<>();
	private Map<Integer, Edge> edges = new HashMap<>();
	private OdooWriteWorker writeWorker;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	private volatile EdgeWebsocketService edgeWebsocketService;

	@Activate
	void activate(Config config) {
		log.info("Activate Metadata.Odoo [url=" + config.url() + ";database=" + config.database() + ";uid="
				+ config.uid() + ";password=" + (config.password() != null ? "ok" : "NOT_SET") + "]");
		this.url = config.url();
		this.database = config.database();
		this.uid = config.uid();
		this.password = config.password();
		this.writeWorker = new OdooWriteWorker(this);
		this.readEdgeFuture = this.readEdgeExecutor.submit((Runnable) () -> {
			/*
			 * Initialize Edge map
			 */
			int[] edgeIds;
			try {
				edgeIds = OdooUtils.search(this.url, this.database, this.uid, this.password, "fems.device");
			} catch (OpenemsException e) {
				log.error("Unable to search Edges from Odoo: " + e.getMessage());
				e.printStackTrace();
				return;
			}
			for (int firstIndex = 0; firstIndex < edgeIds.length; firstIndex += READ_BATCH_SIZE) {
				/*
				 * read batches of 100 Edges
				 */
				int lastIndex = firstIndex + READ_BATCH_SIZE - 1 > edgeIds.length ? edgeIds.length
						: firstIndex + READ_BATCH_SIZE - 1;
				Integer[] batchEdgeIds = new Integer[lastIndex - firstIndex + 1];
				for (int i = 0; i <= lastIndex - firstIndex; i++) {
					batchEdgeIds[i] = edgeIds[firstIndex + i];
				}
				Map<String, Object>[] edgeMaps;
				try {
					edgeMaps = OdooUtils.readMany(this.url, this.database, this.uid, this.password, "fems.device", //
							batchEdgeIds, //
							new Field[] { Field.FemsDevice.ID, Field.FemsDevice.APIKEY, Field.FemsDevice.NAME,
									Field.FemsDevice.COMMENT, Field.FemsDevice.OPENEMS_VERSION,
									Field.FemsDevice.PRODUCT_TYPE, Field.FemsDevice.OPENEMS_CONFIG,
									Field.FemsDevice.SOC, Field.FemsDevice.IPV4, Field.FemsDevice.STATE });
				} catch (OpenemsException e) {
					log.error("Unable to read Edges from Odoo: " + e.getMessage());
					e.printStackTrace();
					continue;
				}
				/*
				 * parse fields from Odoo
				 */
				for (Map<String, Object> edgeMap : edgeMaps) {
					Integer edgeId = OdooUtils.getAsInteger(edgeMap.get(Field.FemsDevice.ID.n()));
					try {
						String openemsConfig = OdooUtils.getAsString(edgeMap.get(Field.FemsDevice.OPENEMS_CONFIG.n()));
						JsonObject jOpenemsConfig;
						if (openemsConfig.isEmpty()) {
							jOpenemsConfig = new JsonObject();
						} else {
							jOpenemsConfig = JsonUtils.getAsJsonObject(JsonUtils.parse(openemsConfig));
						}
						String apikey = OdooUtils.getAsString(edgeMap.get(Field.FemsDevice.APIKEY.n()));
						String name = OdooUtils.getAsString(edgeMap.get(Field.FemsDevice.NAME.n()));
						String comment = OdooUtils.getAsString(edgeMap.get(Field.FemsDevice.COMMENT.n()));
						String openemsVersion = OdooUtils
								.getAsString(edgeMap.get(Field.FemsDevice.OPENEMS_VERSION.n()));
						String productType = OdooUtils.getAsString(edgeMap.get(Field.FemsDevice.PRODUCT_TYPE.n()));
						String initialIpv4 = OdooUtils.getAsString(edgeMap.get(Field.FemsDevice.IPV4.n()));
						Integer initialSoc = OdooUtils.getAsInteger(edgeMap.get(Field.FemsDevice.SOC.n()));
						// parse State
						String stateString = OdooUtils.getAsString(edgeMap.get(Field.FemsDevice.STATE.n()));
						State state;
						try {
							state = State.valueOf(stateString.toUpperCase());
						} catch (IllegalArgumentException e) {
							log.warn("Edge [" + name + "]. Unable to get State from [" + stateString + "]: "
									+ e.getMessage());
							state = State.INACTIVE; // Default
						}
						/*
						 * Create instance of Edge and register listeners
						 */
						Edge edge = new Edge( //
								edgeId, //
								apikey, //
								name, //
								comment, //
								state, //
								openemsVersion, //
								productType, //
								jOpenemsConfig, //
								initialSoc, //
								initialIpv4);
						edge.onSetOnline(isOnline -> {
							if (isOnline && edge.getState().equals(State.INACTIVE)) {
								// Update Edge state to active
								log.info("Mark Edge [" + edge.getId() + "] as ACTIVE. It was [" + edge.getState().name()
										+ "]");
								edge.setState(State.ACTIVE);
								this.write(edge, new FieldValue(Field.FemsDevice.STATE, "active"));
							}
						});
						edge.onSetConfig(jConfig -> {
							// Update Edge config in Odoo
							String conf = new GsonBuilder().setPrettyPrinting().create().toJson(jConfig);
							this.write(edge, new FieldValue(Field.FemsDevice.OPENEMS_CONFIG, conf));
						});
						edge.onSetLastMessage(() -> {
							// Set LastMessage timestamp in Odoo
							this.writeWorker.onLastMessage(edgeId);
						});
						edge.onSetLastUpdate(() -> {
							// Set LastUpdate timestamp in Odoo
							this.writeWorker.onLastUpdate(edgeId);
						});
						edge.onSetVersion(version -> {
							// Set Version in Odoo
							this.write(edge, new FieldValue(Field.FemsDevice.OPENEMS_VERSION, version));
						});
						edge.onSetSoc(soc -> {
							// Set SoC in Odoo
							this.write(edge, new FieldValue(Field.FemsDevice.SOC, String.valueOf(soc)));
						});
						edge.onSetIpv4(ipv4 -> {
							// Set IPv4 in Odoo
							this.write(edge, new FieldValue(Field.FemsDevice.IPV4, String.valueOf(ipv4)));
						});
						edge.setOnline(this.edgeWebsocketService.isOnline(edge.getId()));

						// store in cache
						synchronized (this.edges) {
							this.edges.put(edgeId, edge);
						}
					} catch (OpenemsException e) {
						log.error("Unable to read Edge [ID:" + edgeId + "]: " + e.getMessage());
					}
				}
				log.info("Odoo: add batch from [" + firstIndex + "] to [" + lastIndex + "] to cache");
			}
			this.isInitialized.set(true);
		});
	}

	@Deactivate
	void deactivate() {
		log.info("Deactivate Metadata.Odoo");
		this.writeWorker.dispose();
		this.readEdgeFuture.cancel(true);
		this.readEdgeExecutor.shutdown();
		this.isInitialized.set(false);
	}

	/**
	 * Tries to authenticate at the Odoo server WITHOUT a sessionId. This is always
	 * denied.
	 *
	 * @return
	 * @throws OpenemsException
	 */
	public User authenticate() throws OpenemsException {
		throw new OpenemsException("Session-ID is missing. Authentication to Odoo denied.");
	}

	/**
	 * Tries to authenticate at the Odoo server using a sessionId from a cookie.
	 *
	 * @param sessionId
	 * @return
	 * @throws OpenemsException
	 */
	@Override
	public User authenticate(String sessionId) throws OpenemsException {
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
			throw new OpenemsException("No matching user found");
		} catch (IOException e) {
			throw new OpenemsException("IOException while reading from Odoo: " + e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	@Override
	public int[] getEdgeIdsForApikey(String apikey) {
		synchronized (this.edges) {
			return this.edges.values().stream().filter(edge -> apikey.equals(edge.getApikey()))
					.mapToInt(edge -> edge.getId()).toArray();
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
		if (this.isInitialized.get()) {
			log.error("Edge [" + edgeId + "] is not in Cache!");
		}
		return Optional.empty();
	}

	@Override
	public Optional<User> getUser(int userId) {
		// try to read from cache
		synchronized (this.users) {
			return Optional.ofNullable(this.users.get(userId));
		}
	}

	private void write(Edge edge, FieldValue fieldValue) {
		try {
			OdooUtils.write(this.url, this.database, this.uid, this.password, "fems.device",
					new Integer[] { edge.getId() }, fieldValue);
		} catch (OpenemsException e) {
			log.error("Unable to update Edge [ID:" + edge.getName() + "] field [" + fieldValue.getField().n() + "] : "
					+ e.getMessage());
		}
	}
}
