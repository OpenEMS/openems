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
public class WriteWorker {

	private static final int UPDATE_INTERVAL_IN_SECONDS = 60;

	private final Logger log = LoggerFactory.getLogger(WriteWorker.class);
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

	public WriteWorker(PostgresHandler parent, MyConnection connection) {
		this.parent = parent;
		this.connection = connection;
	}

	public synchronized void start() {
		this.future = this.executor.scheduleWithFixedDelay(//
				() -> task.accept(this.connection), //
				0, UPDATE_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
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
				this.parent.parent.logWarn(this.log, "tasks interrupted");
			} finally {
				if (!executor.isTerminated()) {
					this.parent.parent.logWarn(this.log, "cancel non-finished tasks");
				}
				executor.shutdownNow();
			}
		}
	}

	private Consumer<MyConnection> task = (connection) -> {
		try {
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
		} catch (SQLException e) {
			this.log.error("Unable to execute WriteWorker task: " + e.getMessage());
		}
	};

	private final Set<Integer> lastMessageOdooIds = new HashSet<>();
	private final Set<Integer> lastUpdateOdooIds = new HashSet<>();

	public void onLastMessage(MyEdge edge) {
		synchronized (this.lastMessageOdooIds) {
			this.lastMessageOdooIds.add(edge.getOdooId());
		}
	}

	public void onLastUpdate(MyEdge edge) {
		synchronized (this.lastUpdateOdooIds) {
			this.lastUpdateOdooIds.add(edge.getOdooId());
		}
	}

}
