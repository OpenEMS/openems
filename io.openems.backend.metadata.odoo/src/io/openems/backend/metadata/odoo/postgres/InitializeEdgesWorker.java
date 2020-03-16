package io.openems.backend.metadata.odoo.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.backend.metadata.odoo.postgres.task.UpdateEdgeStatesSum;

public class InitializeEdgesWorker {

	private final Logger log = LoggerFactory.getLogger(InitializeEdgesWorker.class);
	protected final PostgresHandler parent;
	private final HikariDataSource dataSource;
	private final Runnable onFinished;

	/**
	 * Executor for subscriptions task.
	 */
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public InitializeEdgesWorker(PostgresHandler parent, HikariDataSource dataSource, Runnable onFinished) {
		this.parent = parent;
		this.dataSource = dataSource;
		this.onFinished = onFinished;
	}

	public synchronized void start() {
		this.executor.execute(() -> task.accept(this));
	}

	public synchronized void stop() {
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

	private Consumer<InitializeEdgesWorker> task = (self) -> {
		/*
		 * First: mark all Edges as offline
		 */
		try (Connection con = self.dataSource.getConnection(); //
				PreparedStatement pst = self.psUpdateAllEdgesOffline(con); //
		) {
			pst.execute();
		} catch (SQLException e) {
			self.parent.logError(this.log,
					"Unable to mark Edges offline. " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}

		/**
		 * Reads all Edges from Postgres and puts them in a local Cache.
		 */
		try (Connection con = self.dataSource.getConnection(); //
				PreparedStatement pst = self.psQueryAllEdges(con); //
				ResultSet rs = pst.executeQuery(); //
		) {
			self.parent.logInfo(this.log, "Caching Edges from Postgres");
			for (int i = 0; rs.next(); i++) {
				if (i % 100 == 0) {
					self.parent.logInfo(this.log, "Caching Edges from Postgres. Finished [" + i + "]");
				}
				try {
					MyEdge edge = self.parent.edgeCache.addOrUpate(rs);

					// Trigger update to Edge States Sum
					self.parent.getQueueWriteWorker().addTask(new UpdateEdgeStatesSum(edge.getOdooId()));
				} catch (Exception e) {
					self.parent.logError(this.log,
							"Unable to read Edge: " + e.getClass().getSimpleName() + ". " + e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (SQLException e) {
			self.parent.logError(this.log,
					"Unable to initialize Edges: " + e.getClass().getSimpleName() + ". " + e.getMessage());
			e.printStackTrace();
		}

		self.parent.logInfo(this.log, "Caching Edges from Postgres finished");
		self.onFinished.run();
	};

	/**
	 * SELECT {} FROM {edge.device};
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psQueryAllEdges(Connection connection) throws SQLException {
		return connection.prepareStatement(//
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
	private PreparedStatement psUpdateAllEdgesOffline(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET " + Field.EdgeDevice.OPENEMS_IS_CONNECTED.id() + " = FALSE" //
						+ ";");
	}

}
