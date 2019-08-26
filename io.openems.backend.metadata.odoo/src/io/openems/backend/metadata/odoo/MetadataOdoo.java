package io.openems.backend.metadata.odoo;

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

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.metadata.odoo.odoo.OdooHandler;
import io.openems.backend.metadata.odoo.odoo.jsonrpc.AuthenticateWithSessionIdResponse;
import io.openems.backend.metadata.odoo.postgres.PostgresHandler;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Metadata.Odoo", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MetadataOdoo extends AbstractOpenemsBackendComponent implements Metadata {

	public final static String ODOO_MODEL = "edge.device";
	public static final String ODOO_DEVICE_TABLE = ODOO_MODEL.replace(".", "_");

	private final Logger log = LoggerFactory.getLogger(MetadataOdoo.class);
	private final EdgeCache edgeCache;

	protected OdooHandler odooHandler = null;
	protected PostgresHandler postgresHandler = null;

	/**
	 * Maps User-ID to User.
	 */
	private ConcurrentHashMap<String, BackendUser> users = new ConcurrentHashMap<>();

	public MetadataOdoo() {
		super("Metadata.Odoo");

		this.edgeCache = new EdgeCache(this);
	}

	@Activate
	void activate(Config config) {
		this.logInfo(this.log, "Activate. " //
				+ "Odoo [" + config.odooHost() + ":" + config.odooPort() + ";PW "
				+ (config.odooPassword() != null ? "ok" : "NOT_SET") + "] " //
				+ "Postgres [" + config.pgHost() + ":" + config.pgPort() + ";PW "
				+ (config.pgPassword() != null ? "ok" : "NOT_SET") + "] " //
				+ "Database [" + config.database() + "]");

		this.odooHandler = new OdooHandler(this, config);
		this.postgresHandler = new PostgresHandler(this, edgeCache, config);
	}

	@Deactivate
	void deactivate() {
		this.logInfo(this.log, "Deactivate");
		if (this.postgresHandler != null) {
			this.postgresHandler.deactivate();
		}
	}

	@Override
	public BackendUser authenticate(String username, String password) throws OpenemsNamedException {
		String sessionId = this.odooHandler.authenticate(username, password);
		return this.authenticate(sessionId);
	}

	/**
	 * Tries to authenticate at the Odoo server using a sessionId from a cookie.
	 *
	 * @param sessionId the Session-ID
	 * @return the BackendUser
	 * @throws OpenemsException on error
	 */
	@Override
	public BackendUser authenticate(String sessionId) throws OpenemsNamedException {
		JsonrpcResponseSuccess origResponse = this.odooHandler.authenticateSession(sessionId);
		AuthenticateWithSessionIdResponse response = AuthenticateWithSessionIdResponse.from(origResponse, sessionId,
				this.edgeCache);
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
	public Optional<BackendUser> getUser(String userId) {
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
