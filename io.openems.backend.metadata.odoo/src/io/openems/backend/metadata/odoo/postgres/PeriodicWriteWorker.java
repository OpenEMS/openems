package io.openems.backend.metadata.odoo.postgres;

import java.sql.SQLException;
import java.sql.Statement;
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

import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.MyEdge;

/**
 * This worker combines writes to lastMessage and lastUpdate fields, to avoid
 * DDOSing Odoo/Postgres by writing too often.
 */
public class PeriodicWriteWorker {

	private static final int UPDATE_INTERVAL_IN_SECONDS = 60;

	private final Logger log = LoggerFactory.getLogger(PeriodicWriteWorker.class);
	private final PostgresHandler parent;
	private final MyConnection connection;

	/**
	 * Holds the scheduled task.
	 */
	private ScheduledFuture<?> future = null;

	/**
	 * Executor for subscriptions task.
	 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	public PeriodicWriteWorker(PostgresHandler parent, Credentials credentials) {
		this.parent = parent;
		this.connection = new MyConnection(credentials);
	}

	public synchronized void start() {
		this.future = this.executor.scheduleWithFixedDelay(//
				() -> task.accept(this.connection), //
				UPDATE_INTERVAL_IN_SECONDS, UPDATE_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
	}

	public synchronized void stop() {
		// unsubscribe regular task
		if (this.future != null) {
			this.future.cancel(true);
		}
		// Shutdown executor
		if (this.executor != null) {
			try {
				executor.shutdown();
				executor.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				this.parent.logWarn(this.log, "tasks interrupted");
			} finally {
				if (!executor.isTerminated()) {
					this.parent.logWarn(this.log, "cancel non-finished tasks");
				}
				executor.shutdownNow();
			}
		}
	}

	private Consumer<MyConnection> task = (connection) -> {
		try {
			this.writeLastMessage(connection);
			this.writeLastUpdate(connection);
			this.writeIsOnline(connection);
			this.writeIsOffline(connection);

		} catch (SQLException e) {
			this.log.error("Unable to execute WriteWorker task: " + e.getMessage());
		}
	};

	private final Set<Integer> lastMessageOdooIds = new HashSet<>();
	private final Set<Integer> lastUpdateOdooIds = new HashSet<>();
	private final Set<Integer> isOnlineOdooIds = new HashSet<>();
	private final Set<Integer> isOfflineOdooIds = new HashSet<>();

	public void onLastMessage(MyEdge edge) {
		synchronized (this.lastMessageOdooIds) {
			this.lastMessageOdooIds.add(edge.getOdooId());
		}
	}

	private void writeIsOffline(MyConnection connection) throws SQLException {
		synchronized (this.isOfflineOdooIds) {
			if (!this.isOfflineOdooIds.isEmpty()) {
				StringBuilder sql = new StringBuilder(//
						"UPDATE " + EdgeDevice.ODOO_TABLE //
								+ " SET " + Field.EdgeDevice.OPENEMS_IS_CONNECTED.id() + " = FALSE" //
								+ " WHERE id IN (");
				sql.append(//
						this.isOfflineOdooIds.stream() //
								.map(String::valueOf) //
								.collect(Collectors.joining(",")));
				this.isOfflineOdooIds.clear();
				sql.append(")");
				Statement s = connection.get().createStatement();
				s.executeUpdate(sql.toString());
			}
		}
	}

	private void writeIsOnline(MyConnection connection) throws SQLException {
		synchronized (this.isOnlineOdooIds) {
			if (!this.isOnlineOdooIds.isEmpty()) {
				StringBuilder sql = new StringBuilder(//
						"UPDATE " + EdgeDevice.ODOO_TABLE //
								+ " SET " + Field.EdgeDevice.OPENEMS_IS_CONNECTED.id() + " = TRUE" //
								+ " WHERE id IN (");
				sql.append(//
						this.isOnlineOdooIds.stream() //
								.map(String::valueOf) //
								.collect(Collectors.joining(",")));
				this.isOnlineOdooIds.clear();
				sql.append(")");
				Statement s = connection.get().createStatement();
				s.executeUpdate(sql.toString());
			}
		}
	}

	private void writeLastUpdate(MyConnection connection) throws SQLException {
		synchronized (this.lastUpdateOdooIds) {
			if (!this.lastUpdateOdooIds.isEmpty()) {
				StringBuilder sql = new StringBuilder(//
						"UPDATE " + EdgeDevice.ODOO_TABLE //
								+ " SET " + Field.EdgeDevice.LAST_UPDATE.id() + " = (now() at time zone 'UTC')" //
								+ " WHERE id IN (");
				sql.append(//
						this.lastUpdateOdooIds.stream() //
								.map(String::valueOf) //
								.collect(Collectors.joining(",")));
				this.lastUpdateOdooIds.clear();
				sql.append(")");
				Statement s = connection.get().createStatement();
				s.executeUpdate(sql.toString());
			}
		}
	}

	private void writeLastMessage(MyConnection connection) throws SQLException {
		synchronized (this.lastMessageOdooIds) {
			if (!this.lastMessageOdooIds.isEmpty()) {
				StringBuilder sql = new StringBuilder(//
						"UPDATE " + EdgeDevice.ODOO_TABLE //
								+ " SET " + Field.EdgeDevice.LAST_MESSAGE.id() + " = (now() at time zone 'UTC')" //
								+ " WHERE id IN (");
				sql.append(//
						this.lastMessageOdooIds.stream() //
								.map(String::valueOf) //
								.collect(Collectors.joining(",")));
				this.lastMessageOdooIds.clear();
				sql.append(")");
				Statement s = connection.get().createStatement();
				s.executeUpdate(sql.toString());
			}
		}
	}

	public void onLastUpdate(MyEdge edge) {
		synchronized (this.lastUpdateOdooIds) {
			this.lastUpdateOdooIds.add(edge.getOdooId());
		}
	}

	public void isOnline(MyEdge edge) {
		synchronized (this.isOnlineOdooIds) {
			synchronized (this.isOfflineOdooIds) {
				int odooId = edge.getOdooId();
				this.isOfflineOdooIds.remove(odooId);
				this.isOnlineOdooIds.add(edge.getOdooId());
			}
		}
	}

	public void isOffline(MyEdge edge) {
		synchronized (this.isOnlineOdooIds) {
			synchronized (this.isOfflineOdooIds) {
				int odooId = edge.getOdooId();
				this.isOnlineOdooIds.remove(odooId);
				this.isOfflineOdooIds.add(edge.getOdooId());
			}
		}
	}

}
