package io.openems.backend.metadata.odoo.postgres;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metadata.odoo.postgres.task.DatabaseTask;

/**
 * This worker writes all Statements in a queue.
 */
public class QueueWriteWorker {

	private final Logger log = LoggerFactory.getLogger(QueueWriteWorker.class);
	private final PostgresHandler parent;
	private final MyConnection connection;

	/**
	 * Executor for subscriptions task.
	 */
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public QueueWriteWorker(PostgresHandler parent, Credentials credentials) {
		this.parent = parent;
		this.connection = new MyConnection(credentials);
	}

	public synchronized void start() {
		// nothing to do
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

	public void addTask(DatabaseTask task) {
		this.executor.execute(() -> {
			try {
				task.execute(connection);
			} catch (SQLException e) {
				parent.logWarn(this.log,
						"Unable to execute Task. " + task.getClass().getSimpleName() + ": " + e.getMessage());
			}
		});
	}

}
