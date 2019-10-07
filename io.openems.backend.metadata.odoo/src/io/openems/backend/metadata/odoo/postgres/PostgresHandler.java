package io.openems.backend.metadata.odoo.postgres;

import java.sql.ResultSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metadata.odoo.Config;
import io.openems.backend.metadata.odoo.EdgeCache;
import io.openems.backend.metadata.odoo.MetadataOdoo;
import io.openems.backend.metadata.odoo.MyEdge;

public class PostgresHandler {

	private final Logger log = LoggerFactory.getLogger(PostgresHandler.class);
	private final MetadataOdoo parent;
	private final EdgeCache edgeCache;
	private final Credentials credentials;
	private final CompletableFuture<Void> initializeEdgesTask;
	private final MyConnection connection;

	public PostgresHandler(MetadataOdoo parent, EdgeCache edgeCache, Config config) {
		this.parent = parent;
		this.edgeCache = edgeCache;
		this.credentials = Credentials.fromConfig(config);
		this.connection = new MyConnection(credentials);

		// Initialize EdgeCache
		this.initializeEdgesTask = CompletableFuture.runAsync(() -> {

			/**
			 * Reads all Edges from Postgres and puts them in a local Cache.
			 */
			try {
				this.parent.logInfo(this.log, "Caching Edges from Postgres");
				ResultSet rs = this.connection.psQueryAllEdges().executeQuery();
				for (int i = 0; rs.next(); i++) {
					if (i % 100 == 0) {
						this.parent.logInfo(this.log, "Caching Edges from Postgres. Finished [" + i + "]");
					}
					try {
						this.edgeCache.addOrUpate(rs);
					} catch (Exception e) {
						this.parent.logError(this.log,
								"Unable to read Edge: " + e.getClass().getSimpleName() + ". " + e.getMessage());
						e.printStackTrace();
					}
				}

			} catch (Exception e) {
				this.parent.logError(this.log,
						"Unable to initialize Edges: " + e.getClass().getSimpleName() + ". " + e.getMessage());
				e.printStackTrace();
			}

			this.parent.logInfo(this.log, "Caching Edges from Postgres finished");
		});
	}

	public void deactivate() {
		this.initializeEdgesTask.cancel(true);
		this.connection.deactivate();
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
}
