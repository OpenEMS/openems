package io.openems.backend.metadata.odoo.postgres;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.common.utils.ThreadPoolUtils;

/**
 * This worker combines writes to lastMessage and lastUpdate fields, to avoid
 * DDOSing Odoo/Postgres by writing too often.
 */
public class PeriodicWriteWorker {

	/**
	 * DEBUG_MODE activates printing of reqular statistics about queued tasks.
	 */
	private static final boolean DEBUG_MODE = true;

	private static final int UPDATE_INTERVAL_IN_SECONDS = 120;

	private final Logger log = LoggerFactory.getLogger(PeriodicWriteWorker.class);
	private final PostgresHandler parent;
	private final HikariDataSource dataSource;

	/**
	 * Holds the scheduled task.
	 */
	private ScheduledFuture<?> future = null;

	/**
	 * Executor for subscriptions task.
	 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10,
			new ThreadFactoryBuilder().setNameFormat("Metadata.Odoo.PGPeriodic-%d").build());

	public PeriodicWriteWorker(PostgresHandler parent, HikariDataSource dataSource) {
		this.parent = parent;
		this.dataSource = dataSource;
	}

	/**
	 * Starts the {@link PeriodicWriteWorker}.
	 */
	public synchronized void start() {
		this.future = this.executor.scheduleWithFixedDelay(//
				() -> this.task.accept(this.dataSource), //
				PeriodicWriteWorker.UPDATE_INTERVAL_IN_SECONDS, PeriodicWriteWorker.UPDATE_INTERVAL_IN_SECONDS,
				TimeUnit.SECONDS);
	}

	/**
	 * Stops the {@link PeriodicWriteWorker}.
	 */
	public synchronized void stop() {
		// unsubscribe regular task
		if (this.future != null) {
			this.future.cancel(true);
		}
		// Shutdown executor
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
	}

	private final Consumer<HikariDataSource> task = dataSource -> {
		try {
			if (PeriodicWriteWorker.DEBUG_MODE) {
				this.debugLog();
			}

			this.writeLastMessage(dataSource);
			this.writeLastUpdate(dataSource);
			this.writeIsOnline(dataSource);
			this.writeIsOffline(dataSource);

		} catch (SQLException e) {
			this.log.error("Unable to execute WriteWorker task: " + e.getMessage());
		}
	};

	private final Set<Integer> lastMessageOdooIds = new HashSet<>();
	private final Set<Integer> lastUpdateOdooIds = new HashSet<>();
	private final Set<Integer> isOnlineOdooIds = new HashSet<>();
	private final Set<Integer> isOfflineOdooIds = new HashSet<>();

	/**
	 * Called on Edge onSetLastMessage event.
	 *
	 * @param edge the {@link MyEdge}.
	 */
	public void onLastMessage(MyEdge edge) {
		synchronized (this.lastMessageOdooIds) {
			this.lastMessageOdooIds.add(edge.getOdooId());
		}
	}

	/**
	 * Called on Edge onSetLastUpdate event.
	 *
	 * @param edge the {@link MyEdge}.
	 */
	public void onLastUpdate(MyEdge edge) {
		synchronized (this.lastUpdateOdooIds) {
			this.lastUpdateOdooIds.add(edge.getOdooId());
		}
	}

	/**
	 * Called on Edge isOnline event.
	 *
	 * @param edge the {@link MyEdge}.
	 */
	public void isOnline(MyEdge edge) {
		synchronized (this.isOnlineOdooIds) {
			synchronized (this.isOfflineOdooIds) {
				var odooId = edge.getOdooId();
				this.isOfflineOdooIds.remove(odooId);
				this.isOnlineOdooIds.add(edge.getOdooId());
			}
		}
	}

	/**
	 * Called on Edge isOffline event.
	 *
	 * @param edge the {@link MyEdge}.
	 */
	public void isOffline(MyEdge edge) {
		synchronized (this.isOnlineOdooIds) {
			synchronized (this.isOfflineOdooIds) {
				var odooId = edge.getOdooId();
				this.isOnlineOdooIds.remove(odooId);
				this.isOfflineOdooIds.add(edge.getOdooId());
			}
		}
	}

	private void writeIsOffline(HikariDataSource dataSource) throws SQLException {
		synchronized (this.isOfflineOdooIds) {
			if (!this.isOfflineOdooIds.isEmpty()) {
				var sql = new StringBuilder(//
						"UPDATE " + EdgeDevice.ODOO_TABLE //
								+ " SET " + Field.EdgeDevice.OPENEMS_IS_CONNECTED.id() + " = FALSE" //
								+ " WHERE id IN (");
				sql.append(//
						this.isOfflineOdooIds.stream() //
								.map(String::valueOf) //
								.collect(Collectors.joining(",")));
				this.isOfflineOdooIds.clear();
				sql.append(")");
				this.executeSql(dataSource, sql.toString());
			}
		}
	}

	private void writeIsOnline(HikariDataSource dataSource) throws SQLException {
		synchronized (this.isOnlineOdooIds) {
			if (!this.isOnlineOdooIds.isEmpty()) {
				var sql = new StringBuilder(//
						"UPDATE " + EdgeDevice.ODOO_TABLE //
								+ " SET " + Field.EdgeDevice.OPENEMS_IS_CONNECTED.id() + " = TRUE" //
								+ " WHERE id IN (");
				sql.append(//
						this.isOnlineOdooIds.stream() //
								.map(String::valueOf) //
								.collect(Collectors.joining(",")));
				this.isOnlineOdooIds.clear();
				sql.append(")");
				this.executeSql(dataSource, sql.toString());
			}
		}
	}

	private void writeLastUpdate(HikariDataSource dataSource) throws SQLException {
		synchronized (this.lastUpdateOdooIds) {
			if (!this.lastUpdateOdooIds.isEmpty()) {
				var sql = new StringBuilder(//
						"UPDATE " + EdgeDevice.ODOO_TABLE //
								+ " SET " + Field.EdgeDevice.LAST_UPDATE.id() + " = (now() at time zone 'UTC')" //
								+ " WHERE id IN (");
				sql.append(//
						this.lastUpdateOdooIds.stream() //
								.map(String::valueOf) //
								.collect(Collectors.joining(",")));
				this.lastUpdateOdooIds.clear();
				sql.append(")");
				this.executeSql(dataSource, sql.toString());
			}
		}
	}

	private void writeLastMessage(HikariDataSource dataSource) throws SQLException {
		synchronized (this.lastMessageOdooIds) {
			if (!this.lastMessageOdooIds.isEmpty()) {
				var sql = new StringBuilder(//
						"UPDATE " + EdgeDevice.ODOO_TABLE //
								+ " SET " + Field.EdgeDevice.LAST_MESSAGE.id() + " = (now() at time zone 'UTC')" //
								+ " WHERE id IN (");
				sql.append(//
						this.lastMessageOdooIds.stream() //
								.map(String::valueOf) //
								.collect(Collectors.joining(",")));
				this.lastMessageOdooIds.clear();
				sql.append(")");
				this.executeSql(dataSource, sql.toString());
			}
		}
	}

	private void executeSql(HikariDataSource dataSource, String sql) throws SQLException {
		try (var con = dataSource.getConnection()) {
			con.createStatement().executeUpdate(sql);
		}
	}

	/*
	 * From here required for DEBUG_MODE
	 */
	private LocalDateTime lastExecute = null;

	private synchronized void debugLog() {
		var now = LocalDateTime.now();
		if (this.lastExecute != null) {
			this.parent.logInfo(this.log, "PeriodicWriteWorker. " //
					+ "Time since last run: [" + ChronoUnit.SECONDS.between(this.lastExecute, now) + "s]" //
			);
		}
		this.lastExecute = now;
	}
}
