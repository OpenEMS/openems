package io.vev.backend.metadata.token;

import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.openems.common.types.Tuple;
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

import com.google.gson.JsonObject;

import io.openems.backend.common.alerting.OfflineEdgeAlertingSetting;
import io.openems.backend.common.alerting.SumStateAlertingSetting;
import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.backend.common.edge.jsonrpc.UpdateMetadataCache;
import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeHandler;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.MetadataUtils;
import io.openems.backend.common.metadata.SimpleEdgeHandler;
import io.openems.backend.common.metadata.User;
import io.openems.common.channel.Level;
import io.openems.common.event.EventBuilder;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.ThreadPoolUtils;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Metadata.Token", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		service = { Metadata.class, EventHandler.class, MetadataToken.class }, //
		immediate = true //
)
@EventTopics({ //
		Edge.Events.ON_SET_CONFIG //
})
public class MetadataToken extends AbstractMetadata implements Metadata, EventHandler {

	private static final Pattern NAME_NUMBER_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");

	private final Logger log = LoggerFactory.getLogger(MetadataToken.class);

	private static final String JWT_SECRET = "s8dtgTyyj5ksd6tls56YrdxgTHRTcxc5gT68hYid624Gehs4x5fjg6hc7sq3AS5Gfg678";

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final EventAdmin eventAdmin;

	private final SimpleEdgeHandler edgeHandler = new SimpleEdgeHandler();
	private final JWTVerifier jwtVerifier;
	private MongoRepository mongoRepository;

	private JsonObject settings = new JsonObject();

	@Activate
	public MetadataToken(@Reference EventAdmin eventadmin, Config config) {
		super("Metadata.Token");
		this.eventAdmin = eventadmin;
		var algorithm = Algorithm.HMAC256(MetadataToken.JWT_SECRET);
		this.jwtVerifier = JWT.require(algorithm).build();
		this.logInfo(this.log, "Activate");

		this.initializeMongoRepository(config);

		// Allow the services some time to settle
		this.executor.schedule(() -> {
			this.setInitialized();
		}, 10, TimeUnit.SECONDS);
	}

	@Deactivate
	private void deactivate() {
		if (this.mongoRepository != null) {
			try {
				this.mongoRepository.close();
			} catch (RuntimeException e) {
				this.logWarn(this.log, "Error while closing MongoDB connection: " + e.getMessage());
			} finally {
				this.mongoRepository = null;
			}
		}
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public User authenticate(String username, String password) throws OpenemsNamedException {
		// Use password as token
        return this.authenticate(password);
	}

	@Override
	public User authenticate(String token) throws OpenemsNamedException {
		if (token == null || token.isBlank()) {
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}

		final DecodedJWT jwt;
		try {
			jwt = this.jwtVerifier.verify(token);
		} catch (JWTVerificationException e) {
			this.log.warn("Failed to verify JWT token: {}", e.getMessage());
            this.log.warn(token);
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}

        final String tenantId = jwt.getClaim("tenantID").asString();
        final String userId = jwt.getClaim("id").asString();
        if (tenantId == null || tenantId.isBlank() || userId == null || userId.isBlank()) {
            this.log.warn("JWT token does not contain required claims");
            throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
        }
        log.info("Authenticating user {} for tenant {}", userId, tenantId);
        final var mongo = this.mongoRepository.forTenant(tenantId);
        log.info("Fetching user {} from tenant {}", userId, tenantId);
        var mongoUser = mongo.getUser(userId);
        log.info("Fetching edges for tenant {}", tenantId);
        var edges = mongo.getEdgeList();
        log.info("Found {} edges for tenant {}", edges.size(), tenantId);
        if (mongoUser.isEmpty()) {
            this.log.warn("User not found in tenant {}", tenantId);
            throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
        }
        log.info("User {} authenticated successfully", userId);

		final VevUser user = new VevUser(tenantId, mongoUser, edges, Optional.of(token));

        log.info("User {} has {} edges", user.getId(), edges.size());
		return user;
	}

	private User createUser(String userId, String name, String token, Language language, Role role,
			boolean hasMultipleEdges) {
		return new User(userId, name, token, language, role, hasMultipleEdges, this.settings);
	}

	private Language resolveLanguage(VevUser user) {
		return user.getLanguage();
	}

	private static Optional<String> firstNonBlank(String... values) {
		if (values == null) {
			return Optional.empty();
		}
		for (var value : values) {
			if (value == null) {
				continue;
			}
			var trimmed = value.trim();
			if (!trimmed.isEmpty()) {
				return Optional.of(trimmed);
			}
		}
		return Optional.empty();
	}

	private void initializeMongoRepository(Config config) {
		try {
			this.mongoRepository = new MongoRepository(config.mongoUri(), config.mongoDatabase());
			this.logInfo(this.log,
					"MongoDB repository enabled [database=" + config.mongoDatabase() + ", uri=" + config.mongoUri() + "]");
		} catch (RuntimeException e) {
			this.mongoRepository = null;
			this.logWarn(this.log, "MongoDB repository not available: " + e.getMessage());
		}
	}

	@Override
	public void logout(User user) {
		this.logInfo(this.log, "User " + user.getId() + " logged out");
	}

	@Override
	public Optional<String> getEdgeIdForApikey(String apikey) {
        var mongo = this.mongoRepository.forDefaultTenant();
        var mongoEdgeOpt = mongo.getEdgeByApiKey(apikey);
        if (mongoEdgeOpt.isEmpty()) {
            return Optional.empty();
        }
        var edge = new VevEdge(this, mongoEdgeOpt.get());
        return Optional.of(edge.getId());
	}

	@Override
	public Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
        var mongo = this.mongoRepository.forDefaultTenant();
        var mongoEdgeOpt = mongo.getEdgeBySetupPassword(setupPassword);
        if (mongoEdgeOpt.isEmpty()) {
            return Optional.empty();
        }
        var edge = new VevEdge(this, mongoEdgeOpt.get());
        return Optional.of(edge);
	}

    private static Tuple<String, String> parseId(String fullId) throws IllegalArgumentException {
        var parts = fullId.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid full ID: " + fullId);
        }
        return new Tuple<String, String>(parts[0], parts[1]);
    }

	@Override
	public Optional<Edge> getEdge(String fullEdgeId) {
        var parsed = MetadataToken.parseId(fullEdgeId);
        var mongo = this.mongoRepository.forTenant(parsed.a());
        var edgeDocOpt = mongo.getEdgeById(parsed.b());
        if (edgeDocOpt.isEmpty()) {
            return Optional.empty();
        }
        var edgeDoc = edgeDocOpt.get();
        return Optional.of(new VevEdge(this, edgeDoc));
	}

	@Override
	public Optional<User> getUser(String fullUserId) {
        var parsed = MetadataToken.parseId(fullUserId);
        return Optional.of(VevUser.fromFullId(this.mongoRepository, parsed));
	}

	@Override
	public Collection<Edge> getAllOfflineEdges() {
		return List.of();
	}

	@Override
	public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException {
		throw new UnsupportedOperationException("TokenMetadata.addEdgeToUser() is not implemented");
	}

	@Override
	public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException {
        var info = new HashMap<String, Object>();
        info.put("id", user.getId());
        info.put("name", user.getName());
        info.put("language", user.getLanguage().toString());
        return info;
	}

	@Override
	public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException {
		throw new UnsupportedOperationException("TokenMetadata.setUserInformation() is not implemented");
	}

	@Override
	public byte[] getSetupProtocol(User user, int setupProtocolId) throws OpenemsNamedException {
		throw new UnsupportedOperationException("TokenMetadata.getSetupProtocol() is not implemented");
	}

	@Override
	public JsonObject getSetupProtocolData(User user, String edgeId) throws OpenemsNamedException {
		throw new UnsupportedOperationException("TokenMetadata.getSetupProtocolData() is not implemented");
	}

	@Override
	public SetupProtocolCoreInfo getLatestSetupProtocolCoreInfo(String edgeId) throws OpenemsNamedException {
		return null;
	}

	@Override
	public List<SetupProtocolCoreInfo> getProtocolsCoreInfo(String edgeId) throws OpenemsNamedException {
		return Collections.emptyList();
	}

	@Override
	public int submitSetupProtocol(User user, JsonObject jsonObject) {
		throw new UnsupportedOperationException("TokenMetadata.submitSetupProtocol() is not implemented");
	}

	@Override
	public void createSerialNumberExtensionProtocol(String edgeId, Map<String, Map<String, String>> serialNumbers,
			List<SetupProtocolItem> items) {
		this.log.info("SerialNumberProtocol[{}]: {}, {}", edgeId, serialNumbers, items);
	}

	@Override
	public void registerUser(JsonObject jsonObject, String oem) throws OpenemsNamedException {
		throw new UnsupportedOperationException("TokenMetadata.registerUser() is not implemented");
	}

	@Override
	public void updateUserLanguage(User user, Language language) throws OpenemsNamedException {
	}

	@Override
	public EventAdmin getEventAdmin() {
		return this.eventAdmin;
	}

	@Override
	public void handleEvent(Event event) {
		var reader = new EventReader(event);

		switch (event.getTopic()) {
		case Edge.Events.ON_SET_CONFIG -> {
			this.edgeHandler.setEdgeConfigFromEvent(reader, (edge, oldConfig, newConfig) -> {
				EventBuilder.from(this.eventAdmin, Edge.Events.ON_UPDATE_CONFIG) //
						.addArg(Edge.Events.OnUpdateConfig.EDGE_ID, edge.getId()) //
						.addArg(Edge.Events.OnUpdateConfig.OLD_CONFIG, oldConfig) //
						.addArg(Edge.Events.OnUpdateConfig.NEW_CONFIG, newConfig) //
						.send();
			});
		}
		}
	}

	@Override
	public EdgeHandler edge() {
		return this.edgeHandler;
	}

	@Override
	public Optional<String> getSerialNumberForEdge(Edge edge) {
		throw new UnsupportedOperationException("TokenMetadata.getSerialNumberForEdge() is not implemented");
	}

	@Override
	public Optional<String> getEmsTypeForEdge(String edgeId) {
		throw new UnsupportedOperationException("TokenMetadata.getEmsTypeForEdge() is not implemented");
	}

	@Override
	public UserAlertingSettings getUserAlertingSettings(String edgeId, String userId) throws OpenemsException {
		throw new UnsupportedOperationException("TokenMetadata.getUserAlertingSettings() is not implemented");
	}

	@Override
	public List<UserAlertingSettings> getUserAlertingSettings(String edgeId) {
		return List.of(new UserAlertingSettings("demo", 5, 10, 15));
	}

	@Override
	public List<OfflineEdgeAlertingSetting> getEdgeOfflineAlertingSettings(String edgeId) throws OpenemsException {
		return List.of(new OfflineEdgeAlertingSetting(edgeId, "demo", 5, null));
	}

	@Override
	public List<SumStateAlertingSetting> getSumStateAlertingSettings(String edgeId) throws OpenemsException {
		return List.of(new SumStateAlertingSetting(edgeId, "demo", 10, 15, null));
	}

	@Override
	public void setUserAlertingSettings(User user, String edgeId, List<UserAlertingSettings> settings) {
		throw new UnsupportedOperationException("TokenMetadata.setUserAlertingSettings() is not implemented");
	}

	@Override
	public List<EdgeMetadata> getPageDevice(User user, PaginationOptions paginationOptions)
			throws OpenemsNamedException {
        if (!(user instanceof VevUser vevUser)) {
            throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
        }
        final var mongo = this.mongoRepository.forTenant(vevUser.getTenantId());
		return MetadataUtils.getPageDevice(user, mongo.getEdgeList().stream().map(e -> new VevEdge(this, e)).collect(Collectors.toList()), paginationOptions);
	}

	@Override
	public EdgeMetadata getEdgeMetadataForUser(User user, String edgeId) throws OpenemsNamedException {
		if (!(user instanceof VevUser vevUser)) {
            throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
        }
        final var mongo = this.mongoRepository.forTenant(vevUser.getTenantId());
        var edgeDocOpt = mongo.getEdgeById(edgeId);
        if (edgeDocOpt.isEmpty()) {
            return null;
        }
        var edge = new VevEdge(this, edgeDocOpt.get());
		user.setRole(edgeId, Role.ADMIN);

		return new EdgeMetadata(//
				edge.getId(), //
				edge.getComment(), //
				edge.getProducttype(), //
				edge.getVersion(), //
				Role.ADMIN, //
				edge.isOnline(), //
				edge.getLastmessage(), //
				null, // firstSetupProtocol
				Level.OK //
		);
	}

	@Override
	public Optional<Level> getSumState(String edgeId) {
		throw new UnsupportedOperationException("TokenMetadata.getSumState() is not implemented");
	}

	@Override
	public void logGenericSystemLog(GenericSystemLog systemLog) {
		this.logInfo(this.log,
				"%s on %s executed %s [%s]".formatted(systemLog.user().getId(), systemLog.edgeId(), systemLog.teaser(),
						systemLog.getValues().entrySet().stream() //
								.map(t -> t.getKey() + "=" + t.getValue()) //
								.collect(joining(", "))));
	}

	@Override
	public void updateUserSettings(User user, JsonObject settings) {
		this.settings = settings == null ? new JsonObject() : settings;
	}

	@Override
	public UpdateMetadataCache.Notification generateUpdateMetadataCacheNotification() {
        throw new UnsupportedOperationException("TokenMetadata.generateUpdateMetadataCacheNotification() is not implemented");
	}

}
