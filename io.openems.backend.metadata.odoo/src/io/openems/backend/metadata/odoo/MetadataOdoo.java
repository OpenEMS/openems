package io.openems.backend.metadata.odoo;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
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

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.UserAlertingSettings;
import io.openems.backend.common.metadata.AppCenterMetadata;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeHandler;
import io.openems.backend.common.metadata.Mailer;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.metadata.odoo.odoo.FieldValue;
import io.openems.backend.metadata.odoo.odoo.OdooHandler;
import io.openems.backend.metadata.odoo.odoo.OdooUserRole;
import io.openems.backend.metadata.odoo.odoo.OdooUtils.DateTime;
import io.openems.backend.metadata.odoo.postgres.PostgresHandler;
import io.openems.common.channel.Level;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
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
public class MetadataOdoo extends AbstractMetadata implements AppCenterMetadata, AppCenterMetadata.EdgeData,
		AppCenterMetadata.UiData, Metadata, Mailer, EventHandler {

	private static final int EXECUTOR_MIN_THREADS = 1;
	private static final int EXECUTOR_MAX_THREADS = 50;

	private final Logger log = LoggerFactory.getLogger(MetadataOdoo.class);
	private final EdgeCache edgeCache;
	private final OdooEdgeHandler edgeHandler = new OdooEdgeHandler(this);
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(EXECUTOR_MIN_THREADS, EXECUTOR_MAX_THREADS, 60L,
			TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
			new ThreadFactoryBuilder().setNameFormat("Metadata.Odoo.Worker-%d").build());
	/** Maps User-ID to {@link User}. */
	private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

	@Reference
	private EventAdmin eventAdmin;

	protected OdooHandler odooHandler = null;
	protected PostgresHandler postgresHandler = null;

	public MetadataOdoo() {
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
		var jUser = JsonUtils.getAsJsonObject(result, "user");
		var odooUserId = JsonUtils.getAsInt(jUser, "id");
		var login = JsonUtils.getAsString(jUser, "login");
		var name = JsonUtils.getAsString(jUser, "name");
		var language = Language.from(JsonUtils.getAsString(jUser, "language"));
		var globalRole = Role.getRole(JsonUtils.getAsString(jUser, "global_role"));
		var hasMultipleEdges = JsonUtils.getAsBoolean(jUser, "has_multiple_edges");

		final var settings = JsonUtils.getAsOptionalString(jUser, "settings") //
				.flatMap(JsonUtils::parseOptional) //
				.flatMap(JsonUtils::getAsOptionalJsonObject) //
				.orElse(new JsonObject());

		var jDevices = JsonUtils.getAsJsonArray(result, "devices");
		NavigableMap<String, Role> roles = new TreeMap<>();
		for (JsonElement device : jDevices) {
			var edgeId = JsonUtils.getAsString(device, "name");
			var role = Role.getRole(JsonUtils.getAsString(device, "role"));
			roles.put(edgeId, role);
		}

		var user = new MyUser(odooUserId, login, name, sessionId, language, globalRole, roles, hasMultipleEdges,
				settings);
		var oldUser = this.users.put(login, user);
		if (oldUser != null) {
			oldUser.getEdgeRoles().forEach((edgeId, role) -> {
				user.setRole(edgeId, role);
			});
		}
		return user;
	}

	@Override
	public void logout(User user) {
		this.users.remove(user.getId());
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
		user.setRole(edge.getId(), Role.INSTALLER);
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
	public void registerUser(JsonObject jsonObject, String oem) throws OpenemsNamedException {
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

	@Override
	public void logGenericSystemLog(GenericSystemLog systemLog) {
		this.executor.execute(() -> {
			try {
				final var edge = (MyEdge) this.getEdgeOrError(systemLog.edgeId());
				this.postgresHandler.edge.insertGenericSystemLog(edge.getOdooId(), systemLog);
			} catch (SQLException | OpenemsNamedException e) {
				this.logWarn(this.log, "Unable to insert ");
			}
		});
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
				var diffString = diff.toString();
				if (!diffString.isBlank()) {
					this.logInfo(this.log, "Edge [" + edge.getId() + "]. Update config: " + diff.toString());
				}

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
	public JsonObject sendIsKeyApplicable(String key, String edgeId, String appId) throws OpenemsNamedException {
		return this.odooHandler.getIsKeyApplicable(key, edgeId, appId);
	}

	@Override
	public void sendAddInstallAppInstanceHistory(String key, String edgeId, String appId, UUID instanceId,
			String userId) throws OpenemsNamedException {
		this.odooHandler.getAddInstallAppInstanceHistory(key, edgeId, appId, instanceId, userId);
	}

	@Override
	public void sendAddDeinstallAppInstanceHistory(String edgeId, String appId, UUID instanceId, String userId)
			throws OpenemsNamedException {
		this.odooHandler.getAddDeinstallAppInstanceHistory(edgeId, appId, instanceId, userId);
	}

	@Override
	public void sendAddRegisterKeyHistory(String edgeId, String appId, String key, User user)
			throws OpenemsNamedException {
		this.odooHandler.getAddRegisterKeyHistory(edgeId, appId, key, (MyUser) user);
	}

	@Override
	public void sendAddUnregisterKeyHistory(String edgeId, String appId, String key, User user)
			throws OpenemsNamedException {
		this.odooHandler.getAddUnregisterKeyHistory(edgeId, appId, key, (MyUser) user);
	}

	@Override
	public JsonArray sendGetRegisteredKeys(String edgeId, String appId) throws OpenemsNamedException {
		var response = this.odooHandler.getRegisteredKeys(edgeId, appId);
		return JsonUtils.getAsOptionalJsonArray(response, "keys") //
				.orElse(new JsonArray()) //
		;
	}

	@Override
	public JsonArray sendGetPossibleApps(String key, String edgeId) throws OpenemsNamedException {
		var response = this.odooHandler.getPossibleApps(key, edgeId);
		return JsonUtils.getAsJsonArray(response, "bundles");
	}

	@Override
	public JsonObject sendGetInstalledApps(String edgeId) throws OpenemsNamedException {
		return this.odooHandler.getInstalledApps(edgeId);
	}

	@Override
	public String getSuppliableKey(//
			final User user, //
			final String edgeId, //
			final String appId //
	) throws OpenemsNamedException {
		if (this.isAppFree(user, appId)) {
			return "";
		}
		if (!user.getRole(edgeId).map(r -> r.isAtLeast(Role.INSTALLER)).orElse(false)) {
			return null;
		}
		return "";
	}

	@Override
	public boolean isAppFree(//
			final User user, //
			final String appId //
	) throws OpenemsNamedException {
		return Sets.newHashSet(//
				"App.Hardware.KMtronic8Channel" //
		).contains(appId);
	}

	@Override
	public List<UserAlertingSettings> getUserAlertingSettings(String edgeId) throws OpenemsException {
		return this.odooHandler.getUserAlertingSettings(edgeId);
	}

	@Override
	public UserAlertingSettings getUserAlertingSettings(String edgeId, String userId) throws OpenemsException {
		return this.odooHandler.getUserAlertingSettings(edgeId, userId);
	}

	@Override
	public void setUserAlertingSettings(User user, String edgeId, List<UserAlertingSettings> users) throws OpenemsException {
		this.odooHandler.setUserAlertingSettings((MyUser) user, edgeId, users);
	}

	@Override
	public List<EdgeMetadata> getPageDevice(//
			final User user, //
			final PaginationOptions paginationOptions //
	) throws OpenemsNamedException {
		var result = this.odooHandler.getEdges((MyUser) user, paginationOptions);
		final var jsonArray = JsonUtils.getAsJsonArray(result, "devices");
		final var resultMetadata = new ArrayList<EdgeMetadata>(jsonArray.size());
		for (var jElement : jsonArray) {
			resultMetadata.add(this.convertToEdgeMetadata(user, jElement));
		}
		return resultMetadata;
	}

	@Override
	public EdgeMetadata getEdgeMetadataForUser(User user, String edgeId) throws OpenemsNamedException {
		return this.convertToEdgeMetadata(user, this.odooHandler.getEdgeWithRole(user, edgeId));
	}

	private EdgeMetadata convertToEdgeMetadata(User user, JsonElement jDevice) throws OpenemsNamedException {
		final var edgeId = JsonUtils.getAsString(jDevice, "name");

		// TODO remove cached edge
		final var cachedEdge = this.getEdge(edgeId).orElse(null);
		if (cachedEdge == null) {
			throw new OpenemsException("Unable to find edge with id [" + edgeId + "]");
		}

		final var role = Role.getRole(JsonUtils.getAsString(jDevice, "role"));
		user.setRole(edgeId, role);

		final var sumState = JsonUtils.getAsOptionalString(jDevice, "openems_sum_state_level") //
				.map(String::toUpperCase) //
				.map(Level::valueOf) //
				.orElse(Level.OK);
		final var commment = JsonUtils.getAsOptionalString(jDevice, "comment").orElse("");
		final var producttype = JsonUtils.getAsOptionalString(jDevice, "producttype").orElse("");
		final var firstSetupProtocol = JsonUtils.getAsOptionalString(jDevice, "first_setup_protocol_date")
				.map(DateTime::stringToDateTime) //
				.orElse(null);
		final var lastmessage = JsonUtils.getAsOptionalString(jDevice, "lastmessage") //
				.map(DateTime::stringToDateTime) //
				.orElse(null);

		return new EdgeMetadata(//
				edgeId, //
				commment, //
				producttype, //
				cachedEdge.getVersion(), //
				role, //
				// TODO isOnline should also come from odoo and in the ui there should be a
				// subscribe to maybe "edgeState" if any of these properties change
				cachedEdge.isOnline(), //
				lastmessage, //
				firstSetupProtocol, //
				sumState //
		);
	}

	@Override
	public void updateUserSettings(User user, JsonObject settings)throws OpenemsNamedException {
		this.odooHandler.updateUserSettings(user, settings);
	}

}
