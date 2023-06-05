package io.openems.backend.metadata.odoo.postgres;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;

import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.metadata.odoo.Config;
import io.openems.backend.metadata.odoo.EdgeCache;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.backend.metadata.odoo.MetadataOdoo;

public class PostgresHandler {

	public final PgEdgeHandler edge;

	protected final EdgeCache edgeCache;

	private final MetadataOdoo parent;
	private final HikariDataSource dataSource;
	private final InitializeEdgesWorker initializeEdgesWorker;
	private final PeriodicWriteWorker periodicWriteWorker;

	public PostgresHandler(MetadataOdoo parent, EdgeCache edgeCache, Config config, Runnable onInitialized)
			throws SQLException {
		this.parent = parent;
		this.edgeCache = edgeCache;
		this.dataSource = this.getDataSource(config);
		this.edge = new PgEdgeHandler(this.dataSource);
		this.initializeEdgesWorker = new InitializeEdgesWorker(this, this.dataSource, () -> {
			onInitialized.run();
		});
		this.initializeEdgesWorker.start();
		this.periodicWriteWorker = new PeriodicWriteWorker(this);
		this.periodicWriteWorker.start();
	}

	/**
	 * Deactivates the {@link PostgresHandler}.
	 */
	public void deactivate() {
		this.initializeEdgesWorker.stop();
		this.periodicWriteWorker.stop();
	}

	/**
	 * Gets the Edge for an API-Key, i.e. authenticates the API-Key.
	 *
	 * @param apikey the API-Key
	 * @return the Edge or Empty
	 */
	public Optional<MyEdge> getEdgeForApikey(String apikey) {
		return Optional.ofNullable(this.edgeCache.getEdgeForApikey(apikey));
	}

	public PeriodicWriteWorker getPeriodicWriteWorker() {
		return this.periodicWriteWorker;
	}

	/**
	 * Creates a {@link HikariDataSource} connection pool.
	 *
	 * @param config the configuration
	 * @return the HikariDataSource
	 * @throws SQLException on error
	 */
	private HikariDataSource getDataSource(Config config) throws SQLException {
		if (!Driver.isRegistered()) {
			Driver.register();
		}
		var pgds = new PGSimpleDataSource();
		pgds.setServerNames(new String[] { config.pgHost() });
		pgds.setPortNumbers(new int[] { config.pgPort() });
		pgds.setDatabaseName(config.database());
		pgds.setUser(config.pgUser());
		if (config.pgPassword() != null) {
			pgds.setPassword(config.pgPassword());
		}
		var result = new HikariDataSource();
		result.setDataSource(pgds);
		return result;
	}

	protected Connection getConnection() throws SQLException {
		return this.dataSource.getConnection();
	}

	protected void logInfo(Logger log, String message) {
		this.parent.logInfo(log, message);
	}

	protected void logWarn(Logger log, String message) {
		this.parent.logWarn(log, message);
	}

	protected void logError(Logger log, String message) {
		this.parent.logError(log, message);
	}
}
