package io.openems.backend.metadata.odoo;

import java.sql.SQLException;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.metadata.odoo.odoo.OdooHandler;
import io.openems.backend.metadata.odoo.postgres.PostgresHandler;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Metadata.Odoo", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class OdooMetadata extends AbstractMetadata implements Metadata {

	private final Logger log = LoggerFactory.getLogger(OdooMetadata.class);
	private final EdgeCache edgeCache;

	protected OdooHandler odooHandler = null;
	protected PostgresHandler postgresHandler = null;

	/**
	 * Maps User-ID to {@link User}.
	 */
	private ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

	public OdooMetadata() {
		super("Metadata.Odoo");

		this.edgeCache = new EdgeCache(this);
	}

	@Activate
	void activate(Config config) throws SQLException {
		this.logInfo(this.log, "Activate. " //
				+ "Odoo [" + config.odooHost() + ":" + config.odooPort() + ";PW "
				+ (config.odooPassword() != null ? "ok" : "NOT_SET") + "] " //
				+ "Postgres [" + config.pgHost() + ":" + config.pgPort() + ";PW "
				+ (config.pgPassword() != null ? "ok" : "NOT_SET") + "] " //
				+ "Database [" + config.database() + "]");

		this.odooHandler = new OdooHandler(this, config);
		this.postgresHandler = new PostgresHandler(this, this.edgeCache, config, () -> {
			this.setInitialized();
		});
	}

	@Deactivate
	void deactivate() {
		this.logInfo(this.log, "Deactivate");
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
		JsonObject result = this.odooHandler.authenticateSession(sessionId);

		// Parse Result
		JsonArray jDevices = JsonUtils.getAsJsonArray(result, "devices");
		NavigableMap<String, Role> roles = new TreeMap<>();
		for (JsonElement device : jDevices) {
			String edgeId = JsonUtils.getAsString(device, "name");
			Role role = Role.getRole(JsonUtils.getAsString(device, "role"));
			roles.put(edgeId, role);
		}
		JsonObject jUser = JsonUtils.getAsJsonObject(result, "user");
		MyUser user = new MyUser(//
				JsonUtils.getAsInt(jUser, "id"), //
				JsonUtils.getAsString(jUser, "login"), //
				JsonUtils.getAsString(jUser, "name"), //
				sessionId, //
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
		Optional<MyEdge> edge = this.postgresHandler.getEdgeForApikey(apikey);
		if (edge.isPresent()) {
			return Optional.of(edge.get().getId());
		} else {
			return Optional.empty();
		}
	}

	@Override
	public Optional<Edge> getEdgeBySetupPassword(String setupPassword) {
		Optional<String> optEdgeId = this.odooHandler.getEdgeIdBySetupPassword(setupPassword);
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
	public Collection<Edge> getAllEdges() {
		return this.edgeCache.getAllEdges();
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
	public void addEdgeToUser(User user, Edge edge) throws OpenemsException {
		this.odooHandler.assignEdgeToUser((MyUser) user, (MyEdge) edge);
	}

}
