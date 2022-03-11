package io.openems.backend.metadata.odoo.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.common.utils.ThreadPoolUtils;

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

	/**
	 * Starts the {@link InitializeEdgesWorker}.
	 */
	public synchronized void start() {
		this.executor.execute(() -> this.task.accept(this));
	}

	/**
	 * Stops the {@link InitializeEdgesWorker}.
	 */
	public synchronized void stop() {
		// Shutdown executor
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
	}

	private final Consumer<InitializeEdgesWorker> task = self -> {
		/*
		 * First: mark all Edges as offline
		 */
		try (var con = self.dataSource.getConnection(); //
				var pst = self.psUpdateAllEdgesOffline(con); //
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
		try (var con = self.dataSource.getConnection(); //
				var pst = self.psQueryAllEdges(con); //
				var rs = pst.executeQuery(); //
		) {
			self.parent.logInfo(this.log, "Caching Edges from Postgres");
			for (var i = 0; rs.next(); i++) {
				if (i % 100 == 0) {
					self.parent.logInfo(this.log, "Caching Edges from Postgres. Finished [" + i + "]");
				}
				try {
					self.parent.edgeCache.addOrUpate(rs);

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
	 * SELECT {} FROM {edge.device};.
	 *
	 * @param connection the {@link Connection}
	 * @return the {@link PreparedStatement}
	 * @throws SQLException on error
	 */
	private PreparedStatement psQueryAllEdges(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"SELECT " + Field.getSqlQueryFields(EdgeDevice.values()) //
						+ " FROM " + EdgeDevice.ODOO_TABLE //
						+ ";");
	}

	/**
	 * UPDATE {} SET openems_is_connected = FALSE;.
	 *
	 * @param connection the {@link Connection}
	 * @return the {@link PreparedStatement}
	 * @throws SQLException on error
	 */
	private PreparedStatement psUpdateAllEdgesOffline(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET " + Field.EdgeDevice.OPENEMS_IS_CONNECTED.id() + " = FALSE" //
						+ ";");
	}

}
