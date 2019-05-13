package io.openems.backend.metadata.odoo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import io.openems.common.access_control.RoleId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Edge.State;
import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.metadata.odoo.jsonrpc.AuthenticateWithSessionIdResponse;
import io.openems.backend.metadata.odoo.jsonrpc.AuthenticateWithUsernameAndPasswordRequest;
import io.openems.backend.metadata.odoo.jsonrpc.AuthenticateWithUsernameAndPasswordResponse;
import io.openems.backend.metadata.odoo.jsonrpc.EmptyRequest;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfigDiff;
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
	private ConcurrentHashMap<String, BackendUser> users = new ConcurrentHashMap<>();
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
			EdgeConfigDiff diff = EdgeConfigDiff.diff(config, edge.getConfig());
			if (!diff.isDifferent()) {
				return;
			}

			this.logDebug(this.log,
					"Edge [" + edge.getId() + "]. Update config: " + StringUtils.toShortString(diff.toString(), 100));
			String conf = new GsonBuilder().setPrettyPrinting().create().toJson(config.toJson());
			String components = new GsonBuilder().setPrettyPrinting().create().toJson(config.componentsToJson());
			this.write(edge, //
					new FieldValue(Field.EdgeDevice.OPENEMS_CONFIG, conf),
					new FieldValue(Field.EdgeDevice.OPENEMS_CONFIG_COMPONENTS, components));

			// write EdgeConfig-Diff to Odoo Chatter
			try {
				OdooUtils.addChatterMessage(this.odooCredentials, ODOO_MODEL, edge.getOdooId(), //
						"<p>Configuration Update:</p>" + diff.getAsHtml());
			} catch (OpenemsException e) {
				this.logError(this.log, "Unable to update Edge [" + edge.getId() + "] Odoo-ID [" + edge.getOdooId()
						+ "] write EdgeConfig Diff to Odoo Chatter: " + e.getMessage());
			}
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
			this.write(edge, new FieldValue(Field.EdgeDevice.OPENEMS_VERSION, version.toString()));
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

	@Override
	public BackendUser authenticate(String username, String password) throws OpenemsNamedException {
		AuthenticateWithUsernameAndPasswordRequest request = new AuthenticateWithUsernameAndPasswordRequest(
				this.odooCredentials.getDatabase(), username, password);
		JsonrpcResponseSuccess origResponse = OdooUtils
				.sendJsonrpcRequest(this.odooCredentials.getUrl() + "/web/session/authenticate", request);
		AuthenticateWithUsernameAndPasswordResponse response = AuthenticateWithUsernameAndPasswordResponse
				.from(origResponse);
		return this.authenticate(response.getSessionId());
	}

	@Override
	public RoleId authenticate2(String userName, String password, String roleId) throws OpenemsException {
		return null;
	}

	/**
	 * Tries to authenticate at the Odoo server using a sessionId from a cookie.
	 *
	 * @param sessionId
	 * @return
	 * @throws OpenemsException
	 */
	@Override
	public BackendUser authenticate(String sessionId) throws OpenemsNamedException {
		EmptyRequest request = new EmptyRequest();
		String charset = "US-ASCII";
		String query;
		try {
			query = String.format("session_id=%s", URLEncoder.encode(sessionId, charset));
		} catch (UnsupportedEncodingException e) {
			throw OpenemsError.GENERIC.exception(e.getMessage());
		}
		JsonrpcResponseSuccess origResponse = OdooUtils
				.sendJsonrpcRequest(this.odooCredentials.getUrl() + "/openems_backend/info?" + query, request);

		AuthenticateWithSessionIdResponse response = AuthenticateWithSessionIdResponse.from(origResponse, sessionId,
				this.edges);
		MyUser user = response.getUser();
		this.users.put(user.getId(), user);
		return user;
	}

	/**
	 * Writes one field to Odoo.
	 * 
	 * @param edge       the Edge
	 * @param fieldValue the FieldValue
	 */
	private void write(MyEdge edge, FieldValue... fieldValues) {
		try {
			OdooUtils.write(this.odooCredentials, ODOO_MODEL, new Integer[] { edge.getOdooId() }, fieldValues);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to update Edge [" + edge.getId() + "] Odoo-ID [" + edge.getOdooId()
					+ "] Fields [" + fieldValues.toString() + "]: " + e.getMessage());
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
	public Optional<BackendUser> getUser(String userId) {
		return Optional.ofNullable(this.users.get(userId));
	}

	@Override
	public Collection<Edge> getAllEdges() {
		return this.edges.getAllEdges();
	}

}
