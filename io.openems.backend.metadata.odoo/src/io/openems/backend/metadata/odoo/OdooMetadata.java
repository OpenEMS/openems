package io.openems.backend.metadata.odoo;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.metadata.odoo.odoo.OdooHandler;
import io.openems.backend.metadata.odoo.odoo.jsonrpc.AuthenticateWithSessionIdResponse;
import io.openems.backend.metadata.odoo.postgres.PostgresHandler;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

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
		this.postgresHandler = new PostgresHandler(this, edgeCache, config, () -> {
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
		String sessionId = this.odooHandler.authenticate(username, password);
		return this.authenticate(sessionId);
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
		JsonrpcResponseSuccess origResponse = this.odooHandler.authenticateSession(sessionId);
		AuthenticateWithSessionIdResponse response = AuthenticateWithSessionIdResponse.from(origResponse, sessionId,
				this.edgeCache, this.isInitialized());
		MyUser user = response.getUser();
		this.users.put(user.getId(), user);
		return user;
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

	public OdooHandler getOdooHandler() {
		return odooHandler;
	}

	public PostgresHandler getPostgresHandler() {
		return postgresHandler;
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
}
