package io.openems.backend.metadata.odoo;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.AlertingSetting;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeHandler;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.metadata.odoo.odoo.FieldValue;
import io.openems.backend.metadata.odoo.odoo.OdooHandler;
import io.openems.backend.metadata.odoo.odoo.OdooUserRole;
import io.openems.backend.metadata.odoo.postgres.PostgresHandler;
import io.openems.common.OpenemsOEM;
import io.openems.common.channel.Level;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfigDiff;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ThreadPoolUtils;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Metadata.Odoo", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
@EventTopics({ //
		Edge.Events.ALL_EVENTS //
})
public class OdooMetadata extends AbstractMetadata implements Metadata, Mailer, EventHandler {

	private static final int EXECUTOR_MIN_THREADS = 1;
	private static final int EXECUTOR_MAX_THREADS = 50;

	private final Logger log = LoggerFactory.getLogger(OdooMetadata.class);

	private final EdgeCache edgeCache;
	private final OdooEdgeHandler edgeHandler = new OdooEdgeHandler(this);

	protected OdooHandler odooHandler = null;
	protected PostgresHandler postgresHandler = null;

	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(EXECUTOR_MIN_THREADS, EXECUTOR_MAX_THREADS, 60L,
			TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
			new ThreadFactoryBuilder().setNameFormat("Metadata.Odoo.Worker-%d").build());

	@Reference
	private EventAdmin eventAdmin;

	/**
	 * Maps User-ID to {@link User}.
	 */
	private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

	public OdooMetadata() {
		super("Metadata.Odoo");

		this.edgeCache = new EdgeCache(this);
	}

	@Activate
	private void activate(Config config) throws SQLException {
		this.logInfo(this.log, "Activate. " //
				+ "Odoo [" + config.odooHost() + ":" + config.odooPort() + ";PW "
				+ (config.odooPassword() != null ? "ok" : "NOT_SET") + "] " //
				+ "Postgres [" + config.pgHost() + ":" + config.pgPort() + ";PW "
				+ (config.pgPassword() != null ? "ok" : "NOT_SET") + "] " //
				+ "Database [" + config.database() + "]");

		this.odooHandler = new OdooHandler(this, this.edgeCache, config);
		this.postgresHandler = new PostgresHandler(this, this.edgeCache, config, () -> {
			this.setInitialized();
		});
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
		if (this.postgresHandler != null) {
			this.postgresHandler.deactivate();
		}
	}

	@Override
	public User authenticate(String username, String password) throws OpenemsNamedException {
		return this.authenticate(this.odooHandler.authenticate(username, password));
	}

	/**
	 * Tries to authenticate at the Odoo server using a sessionId from a cookie.
	 *
	 * @param sessionId the Session-ID
	 * @return the {@link User}
	 * @throws OpenemsException on error
	 */
	@Override
	public User authenticate(String sessionId) throws OpenemsNamedException {
		var result = this.odooHandler.authenticateSession(sessionId);

		// Parse Result
		var jDevices = JsonUtils.getAsJsonArray(result, "devices");
		NavigableMap<String, Role> roles = new TreeMap<>();
		for (JsonElement device : jDevices) {
			var edgeId = JsonUtils.getAsString(device, "name");
			var role = Role.getRole(JsonUtils.getAsString(device, "role"));
			roles.put(edgeId, role);
		}
		var jUser = JsonUtils.getAsJsonObject(result, "user");
		var odooUserId = JsonUtils.getAsInt(jUser, "id");

		var user = new MyUser(//
				odooUserId, //
				JsonUtils.getAsString(jUser, "login"), //
				JsonUtils.getAsString(jUser, "name"), //
				sessionId, //
				Language.from(JsonUtils.getAsString(jUser, "language")), //
				Role.getRole(JsonUtils.getAsString(jUser, "global_role")), //
				roles);

		this.users.put(user.getId(), user);
		return user;
	}

	@Override
	public void logout(User user) {
		this.odooHandler.logout(user.getToken());
	}

	@Override
	public Optional<String> getEdgeIdForApikey(String apikey) {
		var edgeOpt = this.postgresHandler.getEdgeForApikey(apikey);
		if (edgeOpt.isPresent()) {
			return Optional.of(edgeOpt.get().getId());
		}
		return Optional.empty();
	}

	@Override
	public Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
		var optEdgeId = this.odooHandler.getEdgeIdBySetupPassword(setupPassword);
		if (!optEdgeId.isPresent()) {
			return Optional.empty();
		}
		return this.getEdge(optEdgeId.get());
	}

	@Override
	public Optional<Edge> getEdge(String edgeId) {
		return Optional.ofNullable(this.edgeCache.getEdgeFromEdgeId(edgeId));
	}

	@Override
	public Optional<User> getUser(String userId) {
		return Optional.ofNullable(this.users.get(userId));
	}

	@Override
	public Collection<Edge> getAllOfflineEdges() {
		return this.edgeCache.getAllEdges().stream().filter(Edge::isOffline).toList();
	}

	/**
	 * Gets the {@link OdooHandler}.
	 *
	 * @return the {@link OdooHandler}
	 */
	public OdooHandler getOdooHandler() {
		return this.odooHandler;
	}

	/**
	 * Gets the {@link PostgresHandler}.
	 *
	 * @return the {@link PostgresHandler}
	 */
	public PostgresHandler getPostgresHandler() {
		return this.postgresHandler;
	}

	@Override
	public void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	public void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException {
		this.odooHandler.assignEdgeToUser((MyUser) user, (MyEdge) edge, OdooUserRole.INSTALLER);
	}

	@Override
	public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException {
		return this.odooHandler.getUserInformation((MyUser) user);
	}

	@Override
	public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException {
		this.odooHandler.setUserInformation((MyUser) user, jsonObject);
	}

	@Override
	public byte[] getSetupProtocol(User user, int setupProtocolId) throws OpenemsNamedException {
		return this.odooHandler.getOdooSetupProtocolReport(setupProtocolId);
	}

	@Override
	public JsonObject getSetupProtocolData(User user, String edgeId) throws OpenemsNamedException {
		return this.odooHandler.getSetupProtocolData((MyUser) user, edgeId);
	}

	@Override
	public int submitSetupProtocol(User user, JsonObject jsonObject) throws OpenemsNamedException {
		return this.odooHandler.submitSetupProtocol((MyUser) user, jsonObject);
	}

	@Override
	public void registerUser(JsonObject jsonObject, OpenemsOEM.Manufacturer oem) throws OpenemsNamedException {
		final OdooUserRole role;

		var roleOpt = JsonUtils.getAsOptionalString(jsonObject, "role");
		if (roleOpt.isPresent()) {
			role = OdooUserRole.getRole(roleOpt.get());
		} else {
			role = OdooUserRole.OWNER;
		}

		this.odooHandler.registerUser(jsonObject, role, oem);
	}

	@Override
	public void updateUserLanguage(User user, Language language) throws OpenemsNamedException {
		this.odooHandler.updateUserLanguage((MyUser) user, language);
	}

	@Override
	public EventAdmin getEventAdmin() {
		return this.eventAdmin;
	}

	@Override
	public void handleEvent(Event event) {
		var reader = new EventReader(event);

		switch (event.getTopic()) {
		case Edge.Events.ON_SET_ONLINE: {
			var edgeId = reader.getString(Edge.Events.OnSetOnline.EDGE_ID);
			var isOnline = reader.getBoolean(Edge.Events.OnSetOnline.IS_ONLINE);

			this.getEdge(edgeId).ifPresent(edge -> {
				if (edge instanceof MyEdge) {
					// Set OpenEMS Is Connected in Odoo/Postgres
					this.postgresHandler.getPeriodicWriteWorker().onSetOnline((MyEdge) edge, isOnline);
				}
			});
		}
			break;

		case Edge.Events.ON_SET_CONFIG:
			this.onSetConfigEvent(reader);
			break;

		case Edge.Events.ON_SET_VERSION: {
			var edge = (MyEdge) reader.getProperty(Edge.Events.OnSetVersion.EDGE);
			var version = (SemanticVersion) reader.getProperty(Edge.Events.OnSetVersion.VERSION);

			// Set Version in Odoo
			this.odooHandler.writeEdge(edge, new FieldValue<>(Field.EdgeDevice.OPENEMS_VERSION, version.toString()));
		}
			break;

		case Edge.Events.ON_SET_LASTMESSAGE: {
			var edge = (MyEdge) reader.getProperty(Edge.Events.OnSetLastmessage.EDGE);
			// Set LastMessage timestamp in Odoo/Postgres
			this.postgresHandler.getPeriodicWriteWorker().onLastMessage(edge);
		}
			break;

		case Edge.Events.ON_SET_SUM_STATE: {
			var edge = (MyEdge) reader.getProperty(Edge.Events.OnSetSumState.EDGE);
			var sumState = (Level) reader.getProperty(Edge.Events.OnSetSumState.SUM_STATE);
			// Set Sum-State in Odoo/Postgres
			this.postgresHandler.getPeriodicWriteWorker().onSetSumState(edge, sumState);
		}
			break;

		case Edge.Events.ON_SET_PRODUCTTYPE: {
			var edge = (MyEdge) reader.getProperty(Edge.Events.OnSetProducttype.EDGE);
			var producttype = reader.getString(Edge.Events.OnSetProducttype.PRODUCTTYPE);
			// Set Producttype in Odoo/Postgres
			this.executor.execute(() -> {
				try {
					this.postgresHandler.edge.updateProductType(edge.getOdooId(), producttype);
				} catch (SQLException | OpenemsNamedException e) {
					this.logWarn(this.log, "Edge [" + edge.getId() + "] " //
							+ "Unable to insert update Product Type: " + e.getMessage());
				}
			});
		}
			break;

		}
	}

	private void onSetConfigEvent(EventReader reader) {
		this.executor.execute(() -> {
			var edge = (MyEdge) reader.getProperty(Edge.Events.OnSetConfig.EDGE);
			var newConfig = (EdgeConfig) reader.getProperty(Edge.Events.OnSetConfig.CONFIG);

			EdgeConfig oldConfig;
			try {
				oldConfig = this.edgeHandler.getEdgeConfig(edge.getId());

			} catch (OpenemsNamedException e) {
				oldConfig = EdgeConfig.empty();
				this.logWarn(this.log, "Edge [" + edge.getId() + "]. " + e.getMessage());
			}

			var diff = EdgeConfigDiff.diff(newConfig, oldConfig);
			if (diff.isDifferent()) {
				// Update "EdgeConfigUpdate"
				this.logInfo(this.log, "Edge [" + edge.getId() + "]. Update config: " + diff.toString());

				try {
					this.postgresHandler.edge.insertEdgeConfigUpdate(edge.getOdooId(), diff);
				} catch (SQLException | OpenemsNamedException e) {
					this.logWarn(this.log, "Edge [" + edge.getId() + "] " //
							+ "Unable to insert EdgeConfigUpdate: " + e.getMessage());
				}
			}

			// Always update EdgeConfig, because it also updates "openems_config_components"
			try {
				this.postgresHandler.edge.updateEdgeConfig(edge.getOdooId(), newConfig);
			} catch (SQLException | OpenemsNamedException e) {
				this.logWarn(this.log, "Edge [" + edge.getId() + "] " //
						+ "Unable to insert EdgeConfigUpdate: " + e.getMessage());
			}
		});
	}

	@Override
	public EdgeHandler edge() {
		return this.edgeHandler;
	}

	@Override
	public Optional<String> getSerialNumberForEdge(Edge edge) {
		return this.odooHandler.getSerialNumberForEdge(edge);
	}

	@Override
	public void sendMail(ZonedDateTime sendAt, String template, JsonElement params) {
		try {
			this.odooHandler.sendNotificationMailAsync(sendAt, template, params);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<AlertingSetting> getUserAlertingSettings(String edgeId) throws OpenemsException {
		return this.odooHandler.getUserAlertingSettings(edgeId);
	}

	@Override
	public AlertingSetting getUserAlertingSettings(String edgeId, String userId) throws OpenemsException {
		return this.odooHandler.getUserAlertingSettings(edgeId, userId);
	}

	@Override
	public void setUserAlertingSettings(User user, String edgeId, List<AlertingSetting> users) throws OpenemsException {
		this.odooHandler.setUserAlertingSettings((MyUser) user, edgeId, users);
	}

	@Override
	public Map<String, Role> getPageDevice(User user, PaginationOptions paginationOptions)
			throws OpenemsNamedException {
		var result = this.odooHandler.getEdges((MyUser) user, paginationOptions);

		Map<String, Role> devices = new LinkedHashMap<>();

		var jDevices = JsonUtils.getAsJsonArray(result, "devices");
		for (var jDevice : jDevices) {
			var edgeId = JsonUtils.getAsString(jDevice, "name");
			var role = Role.getRole(JsonUtils.getAsString(jDevice, "role"));
			user.setRole(edgeId, role);

			devices.put(edgeId, role);
		}

		return devices;
	}

	@Override
	public Role getRoleForEdge(User user, String edgeId) throws OpenemsNamedException {
		var result = this.odooHandler.getEdgeWithRole((MyUser) user, edgeId);
		var roleString = JsonUtils.getAsOptionalString(result, "role") //
				.orElseThrow(() -> OpenemsError.COMMON_ROLE_UNDEFINED.exception(edgeId, user.getId()));

		var role = Role.getRole(roleString);
		user.setRole(edgeId, role);
		return role;
	}

}
