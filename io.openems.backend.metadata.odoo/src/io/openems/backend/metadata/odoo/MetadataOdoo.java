package io.openems.backend.metadata.odoo;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonArray;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.alerting.OfflineEdgeAlertingSetting;
import io.openems.backend.common.alerting.SumStateAlertingSetting;
import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.edge.jsonrpc.UpdateMetadataCache;
import io.openems.backend.common.metadata.AbstractMetadata;
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
import io.openems.common.event.EventBuilder;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.oem.OpenemsBackendOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.DebugMode;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfigDiff;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

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
		AppCenterMetadata.UiData, Metadata, Mailer, EventHandler, DebugLoggable {

	private final Logger log = LoggerFactory.getLogger(MetadataOdoo.class);
	private final EdgeCache edgeCache;
	private final OdooEdgeHandler edgeHandler = new OdooEdgeHandler(this);
	/** Maps User-ID to {@link User}. */
	private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

	private DebugExecutor eventExecutor;
	private final ConcurrentHashMap<String, Boolean> pendingEdgeConfigIds = new ConcurrentHashMap<>();

	private DebugExecutor requestExecutor;

	@Reference
	private EventAdmin eventAdmin;

	@Reference
	private OpenemsBackendOem oem;

	protected OdooHandler odooHandler = null;
	protected PostgresHandler postgresHandler = null;
	private DebugMode debugMode = DebugMode.OFF;

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

		this.debugMode = config.debugMode();

		this.eventExecutor = new DebugExecutor((ThreadPoolExecutor) Executors.newFixedThreadPool(config.eventPoolSize(),
				new ThreadFactoryBuilder().setNameFormat("Metadata.Odoo.Event-%d").build()));
		this.requestExecutor = new DebugExecutor((ThreadPoolExecutor) Executors.newFixedThreadPool(
				config.requestPoolSize(), Thread.ofVirtual().name("Metadata.Odoo.Request-", 0).factory()));

		this.odooHandler = new OdooHandler(this, this.edgeCache, config);
		this.postgresHandler = new PostgresHandler(this, this.edgeCache, config, () -> {
			this.setInitialized();
		});
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
		shutdownAndAwaitTermination(this.eventExecutor, 5);
		shutdownAndAwaitTermination(this.requestExecutor, 5);
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
		var jUser = getAsJsonObject(result, "user");
		var odooUserId = getAsInt(jUser, "id");
		var login = getAsString(jUser, "login");
		var name = getAsString(jUser, "name");
		var language = Language.from(getAsString(jUser, "language"));
		var globalRole = Role.getRole(getAsString(jUser, "global_role"));
		var hasMultipleEdges = getAsBoolean(jUser, "has_multiple_edges");

		final var settings = getAsOptionalString(jUser, "settings") //
				.flatMap(JsonUtils::parseOptional) //
				.flatMap(JsonUtils::getAsOptionalJsonObject) //
				.orElse(new JsonObject());

		var user = new MyUser(odooUserId, login, name, sessionId, language, globalRole, new TreeMap<>(),
				hasMultipleEdges, settings);
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
	public void createSerialNumberExtensionProtocol(String edgeId, Map<String, Map<String, String>> serialNumbers,
			List<SetupProtocolItem> items) {
		try {
			this.odooHandler.createSerialNumberProtocol(((MyEdge) this.getEdgeOrError(edgeId)).getOdooId(),
					serialNumbers, items);
		} catch (OpenemsException e) {
			this.log.warn("Unable to create serialnumber protocol", e);
		}
	}

	@Override
	public void registerUser(JsonObject jsonObject, String oem) throws OpenemsNamedException {
		final OdooUserRole role;

		var roleOpt = getAsOptionalString(jsonObject, "role");
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
		case Edge.Events.ON_SET_ONLINE -> {
			var edgeId = reader.getString(Edge.Events.OnSetOnline.EDGE_ID);
			var isOnline = reader.getBoolean(Edge.Events.OnSetOnline.IS_ONLINE);

			this.getEdge(edgeId).ifPresent(edge -> {
				if (edge instanceof MyEdge myEdge) {
					// Set OpenEMS Is Connected in Odoo/Postgres
					this.postgresHandler.getPeriodicWriteWorker().onSetOnline(myEdge, isOnline);
				}
			});
		}

		case Edge.Events.ON_SET_CONFIG //
			-> this.onSetConfigEvent(reader);

		case Edge.Events.ON_SET_VERSION -> {
			var edge = (MyEdge) reader.getProperty(Edge.Events.OnSetVersion.EDGE);
			var version = (SemanticVersion) reader.getProperty(Edge.Events.OnSetVersion.VERSION);

			// Set Version in Odoo
			this.odooHandler.writeEdge(edge, new FieldValue<>(Field.EdgeDevice.OPENEMS_VERSION, version.toString()));
		}

		case Edge.Events.ON_SET_LASTMESSAGE -> {
			var edge = (MyEdge) reader.getProperty(Edge.Events.OnSetLastmessage.EDGE);
			// Set LastMessage timestamp in Odoo/Postgres
			this.postgresHandler.getPeriodicWriteWorker().onLastMessage(edge);
		}

		case Edge.Events.ON_SET_SUM_STATE -> {
			var edgeId = reader.getString(Edge.Events.OnSetSumState.EDGE_ID);
			var sumState = (Level) reader.getProperty(Edge.Events.OnSetSumState.SUM_STATE);

			var edge = this.edgeCache.getEdgeFromEdgeId(edgeId);
			// Set Sum-State in Odoo/Postgres
			this.postgresHandler.getPeriodicWriteWorker().onSetSumState(edge, sumState);
		}

		case Edge.Events.ON_SET_PRODUCTTYPE -> {
			var edge = (MyEdge) reader.getProperty(Edge.Events.OnSetProducttype.EDGE);
			var producttype = reader.getString(Edge.Events.OnSetProducttype.PRODUCTTYPE);
			// Set Producttype in Odoo/Postgres
			this.eventExecutor.submit("OnSetProducttype", () -> {
				this.postgresHandler.edge.updateProductType(edge.getOdooId(), producttype);
			}).whenComplete((r, t) -> {
				if (t != null) {
					this.logWarn(this.log, "Edge [" + edge.getId() + "] " //
							+ "Unable to insert update Product Type: " + t.getMessage());
				}
			});
		}
		}
	}

	@Override
	public void logGenericSystemLog(GenericSystemLog systemLog) {
		this.eventExecutor.submit("LogGenericSystemLog", () -> {
			final var edge = (MyEdge) this.getEdgeOrError(systemLog.edgeId());
			this.postgresHandler.edge.insertGenericSystemLog(edge.getOdooId(), systemLog);
		}).whenComplete((r, t) -> {
			if (t != null) {
				this.logWarn(this.log, "Unable to insert " + t.getMessage());
			}
		});
	}

	private void onSetConfigEvent(EventReader reader) {
		final var edge = (MyEdge) reader.getProperty(Edge.Events.OnSetConfig.EDGE);
		if (this.pendingEdgeConfigIds.putIfAbsent(edge.getId(), Boolean.TRUE) != null) {
			// A task for this Edge-ID is already scheduled
			// TODO it would be better to drop the old task and not the new one
			this.logWarn(this.log,
					"Edge [" + edge.getId() + "]. Update config ignored: another task is already scheduled");
			return;
		}

		this.eventExecutor.submit("OnSetConfig", () -> {
			final EdgeConfig newConfig = reader.getProperty(Edge.Events.OnSetConfig.CONFIG);

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

			EventBuilder.from(this.eventAdmin, Edge.Events.ON_UPDATE_CONFIG) //
					.addArg(Edge.Events.OnUpdateConfig.EDGE_ID, edge.getId()) //
					.addArg(Edge.Events.OnUpdateConfig.OLD_CONFIG, oldConfig) //
					.addArg(Edge.Events.OnUpdateConfig.NEW_CONFIG, newConfig) //
					.send();
		}).whenComplete((r, t) -> {
			this.pendingEdgeConfigIds.remove(edge.getId());
			if (t != null) {
				this.logWarn(this.log, "Unable to SetConfig " + t.getMessage());
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
	public Optional<String> getEmsTypeForEdge(String edgeId) {
		return this.odooHandler.getEmsTypeForEdge(edgeId);
	}

	@Override
	public void sendMail(ZonedDateTime sendAt, String template, JsonElement params) {
		this.odooHandler.sendNotificationMailAsync(sendAt, template, params).whenComplete((result, throwable) -> {
			if (throwable != null) {
				this.log.error("sendMail failed: {}", throwable.getMessage(), throwable);
			}
		});
	}

	@Override
	public CompletableFuture<JsonObject> sendIsKeyApplicable(String key, String edgeId, String appId) {
		return this.requestExecutor.submit("sendIsKeyApplicable", () -> {
			return this.odooHandler.getIsKeyApplicable(key, edgeId, appId);
		});
	}

	@Override
	public CompletableFuture<Void> sendAddInstallAppInstanceHistory(String key, String edgeId, String appId,
			UUID instanceId, String userId) {
		return this.requestExecutor.submit("sendAddInstallAppInstanceHistory", () -> {
			this.odooHandler.getAddInstallAppInstanceHistory(key, edgeId, appId, instanceId, userId);
		});
	}

	@Override
	public CompletableFuture<Void> sendAddDeinstallAppInstanceHistory(String edgeId, String appId, UUID instanceId,
			String userId) {
		return this.requestExecutor.submit("sendAddDeinstallAppInstanceHistory", () -> {
			this.odooHandler.getAddDeinstallAppInstanceHistory(edgeId, appId, instanceId, userId);
		});
	}

	@Override
	public CompletableFuture<Void> sendAddRegisterKeyHistory(String edgeId, String appId, String key, User user) {
		return this.requestExecutor.submit("sendAddRegisterKeyHistory", () -> {
			this.odooHandler.getAddRegisterKeyHistory(edgeId, appId, key, (MyUser) user);
		});
	}

	@Override
	public CompletableFuture<Void> sendAddUnregisterKeyHistory(String edgeId, String appId, String key, User user) {
		return this.requestExecutor.submit("sendAddUnregisterKeyHistory", () -> {
			this.odooHandler.getAddUnregisterKeyHistory(edgeId, appId, key, (MyUser) user);
		});
	}

	@Override
	public CompletableFuture<JsonArray> sendGetRegisteredKeys(String edgeId, String appId) {
		return this.requestExecutor.submit("sendGetRegisteredKeys", () -> {
			var response = this.odooHandler.getRegisteredKeys(edgeId, appId);
			return getAsOptionalJsonArray(response, "keys") //
					.orElse(new JsonArray());
		});
	}

	@Override
	public CompletableFuture<JsonArray> sendGetPossibleApps(String key, String edgeId) {
		return this.requestExecutor.submit("sendGetPossibleApps", () -> {
			var response = this.odooHandler.getPossibleApps(key, edgeId);
			return getAsJsonArray(response, "bundles");
		});
	}

	@Override
	public CompletableFuture<JsonObject> sendGetInstalledApps(String edgeId) {
		return this.requestExecutor.submit("sendGetInstalledApps", () -> {
			return this.odooHandler.getInstalledApps(edgeId);
		});
	}

	@Override
	public CompletableFuture<String> getSuppliableKey(//
			final User user, //
			final String edgeId, //
			final String appId //
	) {
		return this.isAppFree(user, appId).thenCompose(isAppFree -> {
			// context change, but for better request tracking
			return this.requestExecutor.submit("getSuppliableKey", () -> {
				if (isAppFree) {
					return this.oem.getAppCenterMasterKey();
				}
				// TODO better only for certain employees/admins
				if (!user.getRole(edgeId).map(r -> r.isAtLeast(Role.INSTALLER)).orElse(false)) {
					throw new CompletionException(new OpenemsException("Unable to supply key"));
				}
				return this.oem.getAppCenterMasterKey();
			});
		});
	}

	@Override
	public CompletableFuture<Boolean> isAppFree(//
			final User user, //
			final String appId //
	) {
		return this.requestExecutor.submit("isAppFree", () -> {
			return Sets.newHashSet(//
					"App.Hardware.KMtronic8Channel" //
			).contains(appId);
		});
	}

	@Override
	public UserAlertingSettings getUserAlertingSettings(String edgeId, String userId) throws OpenemsException {
		return this.odooHandler.getUserAlertingSettings(edgeId, userId);
	}

	@Override
	public List<UserAlertingSettings> getUserAlertingSettings(String edgeId) throws OpenemsException {
		return this.odooHandler.getUserAlertingSettings(edgeId);
	}

	@Override
	public List<OfflineEdgeAlertingSetting> getEdgeOfflineAlertingSettings(String edgeId) throws OpenemsException {
		return this.odooHandler.getOfflineAlertingSettings(edgeId);
	}

	@Override
	public SetupProtocolCoreInfo getLatestSetupProtocolCoreInfo(String edgeId) throws OpenemsNamedException {
		return this.odooHandler.getLatestSetupProtocolCoreInfo(edgeId);
	}

	@Override
	public List<SetupProtocolCoreInfo> getProtocolsCoreInfo(String edgeId) throws OpenemsNamedException {
		return this.odooHandler.getProtocolsCoreInfo(edgeId);
	}

	@Override
	public List<SumStateAlertingSetting> getSumStateAlertingSettings(String edgeId) throws OpenemsException {
		return this.odooHandler.getSumStateAlertingSettings(edgeId);
	}

	@Override
	public void setUserAlertingSettings(User user, String edgeId, List<UserAlertingSettings> settings)
			throws OpenemsException {
		switch (user) {
		case MyUser odooUser //
			-> this.odooHandler.setUserAlertingSettings(odooUser, edgeId, settings);
		default //
			-> throw new OpenemsException("User information is from foreign source!!");
		}
	}

	@Override
	public List<EdgeMetadata> getPageDevice(//
			final User user, //
			final PaginationOptions paginationOptions //
	) throws OpenemsNamedException {
		var result = this.odooHandler.getEdges((MyUser) user, paginationOptions);
		final var jsonArray = getAsJsonArray(result, "devices");
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
		final var edgeId = getAsString(jDevice, "name");

		// TODO remove cached edge
		final var cachedEdge = this.getEdge(edgeId).orElse(null);
		if (cachedEdge == null) {
			throw new OpenemsException("Unable to find edge with id [" + edgeId + "]");
		}

		final var role = Role.getRole(getAsString(jDevice, "role"));
		user.setRole(edgeId, role);

		final var sumState = getAsOptionalString(jDevice, "openems_sum_state_level") //
				.map(String::toUpperCase) //
				.map(Level::valueOf) //
				.orElse(Level.OK);

		final var commment = this.oem.anonymizeEdgeComment(user, //
				getAsOptionalString(jDevice, "comment").orElse(""), //
				edgeId);

		final var producttype = getAsOptionalString(jDevice, "producttype").orElse("");
		final var firstSetupProtocol = getAsOptionalString(jDevice, "first_setup_protocol_date")
				.map(DateTime::stringToDateTime) //
				.orElse(null);
		final var lastmessage = getAsOptionalString(jDevice, "lastmessage") //
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
	public Optional<Level> getSumState(String edgeId) {
		try {
			return Optional.of(this.odooHandler.getSumState(edgeId));
		} catch (Exception e) {
			this.log.warn(e.getMessage());
			return Optional.empty();
		}
	}

	@Override
	public void updateUserSettings(User user, JsonObject settings) throws OpenemsNamedException {
		this.odooHandler.updateUserSettings(user, settings);
	}

	@Override
	public String debugLog() {
		return new StringBuilder("[").append(this.getName()).append("] [monitor] ") //
				.append("Event-Executor: ") //
				.append(this.eventExecutor.debugLog(this.debugMode)) //
				.append(", Request-Executor: ") //
				.append(this.requestExecutor.debugLog(this.debugMode)) //
				.toString();
	}

	@Override
	public Map<String, JsonElement> debugMetrics() {

		final var eventExecutorMetrics = this.eventExecutor.debugMetrics();
		final var requestExecutorMetrics = this.requestExecutor.debugMetrics();

		// TODO create detailed debug metrics for each pool
		final var executorMetrics = new HashMap<>(eventExecutorMetrics);
		requestExecutorMetrics.forEach((key, value) -> {
			executorMetrics.compute(key, (t, u) -> u == null ? value : value + u);
		});

		return executorMetrics.entrySet().stream() //
				.collect(toUnmodifiableMap(//
						// TODO implement getId()
						e -> "metadata0/" + e.getKey(), //
						e -> new JsonPrimitive(e.getValue())));
	}

	@Override
	public UpdateMetadataCache.Notification generateUpdateMetadataCacheNotification() {
		return this.edgeCache.generateUpdateMetadataCacheNotification();
	}

}
