package io.openems.backend.metadata.odoo.postgres;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.backend.metadata.odoo.postgres.task.UpdateEdgeStatesSum;

public class InitializeEdgesWorker {

	private final Logger log = LoggerFactory.getLogger(InitializeEdgesWorker.class);
	protected final PostgresHandler parent;
	private final MyConnection connection;

	/**
	 * Executor for subscriptions task.
	 */
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public InitializeEdgesWorker(PostgresHandler parent, Credentials credentials) {
		this.parent = parent;
		this.connection = new MyConnection(credentials);
	}

	public synchronized void start() {
		this.executor.execute(() -> task.accept(this.parent, this.connection));
	}

	public synchronized void stop() {
		this.connection.deactivate();
		// Shutdown executor
		if (this.executor != null) {
			try {
				this.executor.shutdown();
				this.executor.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				this.parent.logWarn(this.log, "tasks interrupted");
			} finally {
				if (!this.executor.isTerminated()) {
					this.parent.logWarn(this.log, "cancel non-finished tasks");
				}
				this.executor.shutdownNow();
			}
		}
	}

	private BiConsumer<PostgresHandler, MyConnection> task = (parent, connection) -> {
		// First: mark all Edges as offline
		try {
			this.psUpdateAllEdgesOffline(connection).execute();
		} catch (SQLException e) {
			parent.logError(this.log,
					"Unable to mark Edges offline. " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}

		/**
		 * Reads all Edges from Postgres and puts them in a local Cache.
		 */
		try {
			parent.logInfo(this.log, "Caching Edges from Postgres");
			ResultSet rs = this.psQueryAllEdges(connection).executeQuery();
			for (int i = 0; rs.next(); i++) {
				if (i % 100 == 0) {
					parent.logInfo(this.log, "Caching Edges from Postgres. Finished [" + i + "]");
				}
				try {
					MyEdge edge = parent.edgeCache.addOrUpate(rs);

					// Trigger update to Edge States Sum
					parent.getQueueWriteWorker().addTask(new UpdateEdgeStatesSum(edge.getOdooId()));
				} catch (Exception e) {
					parent.logError(this.log,
							"Unable to read Edge: " + e.getClass().getSimpleName() + ". " + e.getMessage());
					e.printStackTrace();
				}
			}

		} catch (Exception e) {
			parent.logError(this.log,
					"Unable to initialize Edges: " + e.getClass().getSimpleName() + ". " + e.getMessage());
			e.printStackTrace();
		}

		parent.logInfo(this.log, "Caching Edges from Postgres finished");
	};

	/**
	 * SELECT {} FROM {edge.device};
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psQueryAllEdges(MyConnection connection) throws SQLException {
		return connection.get().prepareStatement(//
				"SELECT " + Field.getSqlQueryFields(EdgeDevice.values()) //
						+ " FROM " + EdgeDevice.ODOO_TABLE //
						+ ";");
	}

	/**
	 * UPDATE {} SET openems_is_connected = FALSE;
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psUpdateAllEdgesOffline(MyConnection connection) throws SQLException {
		return connection.get().prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET " + Field.EdgeDevice.OPENEMS_IS_CONNECTED.id() + " = FALSE" //
						+ ";");
	}

}
