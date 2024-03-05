package io.openems.backend.metadata.odoo.postgres;

import java.sql.Connection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
	private boolean isMarkAllEdgesAsOfflineCalled = false;

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
		this.executor.execute(() -> {
			try (var con = this.dataSource.getConnection()) {
				// First Execution on Start
				this.runCachingEdgesTask(con);

				// Plan scheduled Execution
				this.scheduledExecutor.scheduleAtFixedRate(() -> {
					try (Connection newCon = this.dataSource.getConnection()) {
						this.runCachingEdgesTask(newCon);
					} catch (SQLException e) {
						this.logError("Error trying to Connect to Postgres: ", e);
					}
				}, 30, 30, TimeUnit.MINUTES);
			} catch (SQLException e) {
				this.logError("Unable to connect do Postgres ", e);
			}
			this.onFinished.run();
		});
	}

	private void runCachingEdgesTask(Connection con) {
		this.parent.logInfo(this.log, "Caching Edges from Postgres [started]");
		// Check if markAllEdgesAsOffline has already been called
		if (!this.isMarkAllEdgesAsOfflineCalled) {
			this.markAllEdgesAsOffline(con);
			this.parent.logInfo(this.log, "Calling Mark-Edges Offline on first run");
			this.isMarkAllEdgesAsOfflineCalled = true;
		}
		this.readAllEdgesFromPostgres(con);
		this.parent.logInfo(this.log, "Caching Edges from Postgres [finished]");
	}

	/**
	 * Stops the {@link InitializeEdgesWorker}.
	 */
	public synchronized void stop() {
		// Shutdown executor
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
		ThreadPoolUtils.shutdownAndAwaitTermination(this.scheduledExecutor, 5);
	}

	private void markAllEdgesAsOffline(Connection con) {
		try (var pst = this.psUpdateAllEdgesOffline(con)) {
			pst.execute();
		} catch (SQLException e) {
			this.logError("Unable to mark Edges offline. ", e);
		}
	}

	private void readAllEdgesFromPostgres(Connection con) {
		try (var pst = this.psQueryAllEdges(con); //
				var rs = pst.executeQuery();) {
			var counter = 0;
			while (rs.next()) {
				this.logCachingProgress(counter, 1000);
				try {
					this.parent.edgeCache.addOrUpdate(rs);
				} catch (Exception e) {
					this.logError("Unable to read Edge: ", e);
				}
				counter++;
			}
			this.logCachingProgress(counter, 1);
		} catch (SQLException e) {
			this.logError("Unable to initialize Edges: ", e);
		}
	}

	private void logCachingProgress(int count, int interval) {
		if (count % interval == 0 && count > 0) {
			this.parent.logInfo(this.log, String.format("Caching Edges from Postgres [%1$6s]", count));
		}
	}

	private void logError(String msg, Throwable error) {
		this.parent.logError(this.log, new StringBuilder(msg) //
				.append(error.getClass().getSimpleName()) //
				.append(": ").append(error.getMessage()) //
				.toString());
		error.printStackTrace();
	}

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
