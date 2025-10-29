package io.vev.backend.metadata.token;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
import com.google.gson.JsonParser;

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
	private static final String VEVIQ_LOGIN_PATH = "/v1/auth/signin";

	private final Logger log = LoggerFactory.getLogger(MetadataToken.class);

	private static final String JWT_SECRET = "s8dtgTyyj5ksd6tls56YrdxgTHRTcxc5gT68hYid624Gehs4x5fjg6hc7sq3AS5Gfg678";

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final EventAdmin eventAdmin;

	private final SimpleEdgeHandler edgeHandler = new SimpleEdgeHandler();
	private final JWTVerifier jwtVerifier;
	private MongoRepository mongoRepository;
	private final EdgeCache edgeCache;
	private final Config config;
	private final HttpClient httpClient;
	private final URI vevIqLoginUri;

	private JsonObject settings = new JsonObject();

	@Activate
	public MetadataToken(@Reference EventAdmin eventadmin, Config config) {
		super("Metadata.Token");
		this.eventAdmin = eventadmin;
		this.config = config;
		var algorithm = Algorithm.HMAC256(MetadataToken.JWT_SECRET);
		this.jwtVerifier = JWT.require(algorithm).build();
		this.logInfo(this.log, "Activate");
		this.edgeCache = new EdgeCache(this);
		this.vevIqLoginUri = MetadataToken.buildVevIqLoginUri(config.vevIqUrl());
		this.httpClient = HttpClient.newBuilder() //
				.connectTimeout(Duration.ofSeconds(5)) //
				.build();

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
		final var split = MetadataToken.parseId(username);
		final var tenantId = split.a();
		final var email = split.b();
		if (tenantId.isBlank() || email.isBlank() || password == null || password.isBlank()) {
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}
		if (this.mongoRepository == null) {
			throw OpenemsError.COMMON_SERVICE_NOT_AVAILABLE.exception();
		}

		var payload = new JsonObject();
		payload.addProperty("email", email);
		payload.addProperty("password", password);
		payload.addProperty("acceptEula", true);
		payload.addProperty("tenant", tenantId);

		var request = HttpRequest.newBuilder() //
				.uri(this.vevIqLoginUri) //
				.header("Content-Type", "application/json") //
				.header("Accept", "application/json") //
				.timeout(Duration.ofSeconds(10)) //
				.POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8)) //
				.build();

		final HttpResponse<String> response;
		try {
			response = this.httpClient.send(request,
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			this.logWarn(this.log, "Authentication interrupted for email [" + email + "]: " + e.getMessage());
			throw OpenemsError.COMMON_SERVICE_NOT_AVAILABLE.exception();
		} catch (IOException e) {
			this.logWarn(this.log, "Unable to reach vev-iq for email [" + email + "]: " + e.getMessage());
			throw OpenemsError.COMMON_SERVICE_NOT_AVAILABLE.exception();
		}

		if (response.statusCode() != 200) {
			this.logWarn(this.log,
					"vev-iq authentication failed for email [" + email + "] with HTTP status ["
							+ response.statusCode() + "]");
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}

		var body = response.body();
		if (body == null || body.isBlank()) {
			this.logWarn(this.log, "vev-iq authentication returned empty body for email [" + email + "]");
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}

		final JsonObject jsonResponse;
		try {
			jsonResponse = JsonParser.parseString(body).getAsJsonObject();
		} catch (RuntimeException e) {
			this.logWarn(this.log, "Failed to parse vev-iq response for email [" + email + "]: " + e.getMessage());
			throw OpenemsError.COMMON_SERVICE_NOT_AVAILABLE.exception();
		}

		var token = Optional.ofNullable(jsonResponse.get("token").getAsString());
		if (token.isEmpty() || token.get().isBlank()) {
			this.logWarn(this.log, "vev-iq authentication did not return a token for email [" + email + "]");
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}

		return this.authenticate(token.get());
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
        this.edgeCache.syncEdgesForTenant(tenantId, edges);
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
        if (this.mongoRepository == null) {
            return Optional.empty();
        }
        var mongo = this.mongoRepository.forDefaultTenant();
        var mongoEdgeOpt = mongo.getEdgeByApiKey(apikey);
        if (mongoEdgeOpt.isEmpty()) {
            this.edgeCache.removeApikey(apikey);
            return Optional.empty();
        }
        var edge = this.edgeCache.upsert(mongoEdgeOpt.get());
        return Optional.of(edge.getId());
	}

	@Override
	public Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
        if (this.mongoRepository == null) {
            return Optional.empty();
        }
        var mongo = this.mongoRepository.forDefaultTenant();
        var mongoEdgeOpt = mongo.getEdgeBySetupPassword(setupPassword);
        if (mongoEdgeOpt.isEmpty()) {
            this.edgeCache.removeSetupPassword(setupPassword);
            return Optional.empty();
        }
        var edge = this.edgeCache.upsert(mongoEdgeOpt.get());
        return Optional.of(edge);
	}

	private static Tuple<String, String> parseId(String fullId) throws IllegalArgumentException {
		var parts = fullId.split(":");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid full ID: " + fullId);
		}
		return new Tuple<>(parts[0], parts[1]);
	}

	@Override
	public Optional<Edge> getEdge(String fullEdgeId) {
        if (this.mongoRepository == null) {
            return this.edgeCache.getCachedEdge(fullEdgeId).map(edge -> edge);
        }
        var parsed = MetadataToken.parseId(fullEdgeId);
        var mongo = this.mongoRepository.forTenant(parsed.a());
        var edgeDocOpt = mongo.getEdgeById(parsed.b());
        if (edgeDocOpt.isEmpty()) {
            this.edgeCache.removeEdge(fullEdgeId);
            return Optional.empty();
        }
        var edgeDoc = edgeDocOpt.get();
        return Optional.of(this.edgeCache.upsert(edgeDoc));
	}

	@Override
	public Optional<User> getUser(String fullUserId) {
        var parsed = MetadataToken.parseId(fullUserId);
        return Optional.of(VevUser.fromFullId(this.mongoRepository, parsed));
	}

	@Override
	public Collection<Edge> getAllOfflineEdges() {
		return this.edgeCache.getOfflineEdges();
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
        var edges = this.edgeCache.syncEdgesForTenant(vevUser.getTenantId(), mongo.getEdgeList());
		return MetadataUtils.getPageDevice(user, edges, paginationOptions);
	}

	@Override
	public EdgeMetadata getEdgeMetadataForUser(User user, String edgeId) throws OpenemsNamedException {
		if (!(user instanceof VevUser vevUser)) {
            throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
        }
        final var mongo = this.mongoRepository.forTenant(vevUser.getTenantId());
        final var parsedEdgeId = MetadataToken.parseId(edgeId);
        if (!vevUser.getTenantId().equals(parsedEdgeId.a())) {
            throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
        }
        var edgeDocOpt = mongo.getEdgeById(parsedEdgeId.b());
        if (edgeDocOpt.isEmpty()) {
            this.edgeCache.removeEdge(parsedEdgeId.a() + ":" + parsedEdgeId.b());
            return null;
        }
        var edge = this.edgeCache.upsert(edgeDocOpt.get());
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
        return this.edgeCache.generateUpdateMetadataCacheNotification();
	}

	private static URI buildVevIqLoginUri(String baseUrl) {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new IllegalArgumentException("vevIqUrl must not be blank");
		}
		var normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return URI.create(normalized + VEVIQ_LOGIN_PATH);
	}

}
