package io.openems.backend.metadata.odoo.postgres;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.metadata.odoo.postgres.task.DatabaseTask;
import io.openems.backend.metadata.odoo.postgres.task.InsertEdgeConfigUpdate;
import io.openems.backend.metadata.odoo.postgres.task.InsertOrUpdateDeviceStates;
import io.openems.backend.metadata.odoo.postgres.task.UpdateEdgeConfig;
import io.openems.backend.metadata.odoo.postgres.task.UpdateEdgeProducttype;
import io.openems.backend.metadata.odoo.postgres.task.UpdateEdgeVersion;

/**
 * This worker writes all Statements in a queue.
 */
public class QueueWriteWorker {

	private final static int NUMBER_OF_THREADS = 5;

	/**
	 * DEBUG_MODE activates printing of reqular statistics about queued tasks.
	 */
	private final static boolean DEBUG_MODE = true;

	private final Logger log = LoggerFactory.getLogger(QueueWriteWorker.class);
	private final PostgresHandler parent;
	private final HikariDataSource dataSource;

	// Executor for subscriptions task.
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(NUMBER_OF_THREADS, NUMBER_OF_THREADS, 0L,
			TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

	private final ScheduledExecutorService debugLogExecutor;

	public QueueWriteWorker(PostgresHandler parent, HikariDataSource dataSource) {
		this.parent = parent;
		this.dataSource = dataSource;

		if (DEBUG_MODE) {
			this.debugLogExecutor = Executors.newSingleThreadScheduledExecutor();
		} else {
			this.debugLogExecutor = null;
		}
	}

	public synchronized void start() {
		if (DEBUG_MODE) {
			this.initializeDebugLog();
		}
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
		if (this.debugLogExecutor != null) {
			this.debugLogExecutor.shutdown();
		}
	}

	public void addTask(DatabaseTask task) {
		if (DEBUG_MODE) {
			this.count(task, true);
		}

		this.executor.execute(() -> {
			if (DEBUG_MODE) {
				this.count(task, false);
			}

			try {
				task.execute(this.dataSource);
			} catch (SQLException e) {
				parent.logWarn(this.log,
						"Unable to execute Task. " + task.getClass().getSimpleName() + ": " + e.getMessage());
				e.printStackTrace();
			}
		});
	}

	/*
	 * From here required for DEBUG_MODE
	 */

	private void initializeDebugLog() {
		this.debugLogExecutor.scheduleWithFixedDelay(() -> {
			long totalTasks = this.executor.getTaskCount();
			long completedTasks = this.executor.getCompletedTaskCount();
			int countInsertEdgeConfigUpdateUp = this.countInsertEdgeConfigUpdateUp.get();
			int countInsertEdgeConfigUpdateDown = this.countInsertEdgeConfigUpdateDown.get();
			int countInsertOrUpdateDeviceStateUp = this.countInsertOrUpdateDeviceStateUp.get();
			int countInsertOrUpdateDeviceStateDown = this.countInsertOrUpdateDeviceStateDown.get();
			int countUpdateEdgeConfigUp = this.countUpdateEdgeConfigUp.get();
			int countUpdateEdgeConfigDown = this.countUpdateEdgeConfigDown.get();
			int countUpdateEdgeProducttypeUp = this.countUpdateEdgeProducttypeUp.get();
			int countUpdateEdgeProducttypeDown = this.countUpdateEdgeProducttypeDown.get();
			int countUpdateEdgeVersionUp = this.countUpdateEdgeVersionUp.get();
			int countUpdateEdgeVersionDown = this.countUpdateEdgeVersionDown.get();

			parent.logInfo(this.log, "QueueWriteWorker. " //
					+ "Total tasks [" + totalTasks + "|" + completedTasks + "|" + (totalTasks - completedTasks) + "] " //
					+ "Threads [" + this.executor.getPoolSize() + "] " //
					+ "InsertEdgeConfigUpdate [" + countInsertEdgeConfigUpdateUp + "|" + countInsertEdgeConfigUpdateDown
					+ "|" + (countInsertEdgeConfigUpdateUp - countInsertEdgeConfigUpdateDown) + "] " //
					+ "InsertOrUpdateDeviceState [" + countInsertOrUpdateDeviceStateUp + "|"
					+ countInsertOrUpdateDeviceStateDown + "|"
					+ (countInsertOrUpdateDeviceStateUp - countInsertOrUpdateDeviceStateDown) + "] " //
					+ "UpdateEdgeConfig [" + countUpdateEdgeConfigUp + "|" + countUpdateEdgeConfigDown + "|"
					+ (countUpdateEdgeConfigUp - countUpdateEdgeConfigDown) + "] " //
					+ "UpdateEdgeProducttype [" + countUpdateEdgeProducttypeUp + "|" + countUpdateEdgeProducttypeDown
					+ "|" + (countUpdateEdgeProducttypeUp - countUpdateEdgeProducttypeDown) + "] " //
					+ "UpdateEdgeVersion [" + countUpdateEdgeVersionUp + "|" + countUpdateEdgeVersionDown + "|"
					+ (countUpdateEdgeVersionUp - countUpdateEdgeVersionDown) + "] " //
			);
		}, 10, 10, TimeUnit.SECONDS);
	}

	private final AtomicInteger countInsertEdgeConfigUpdateUp = new AtomicInteger(0);
	private final AtomicInteger countInsertEdgeConfigUpdateDown = new AtomicInteger(0);
	private final AtomicInteger countInsertOrUpdateDeviceStateUp = new AtomicInteger(0);
	private final AtomicInteger countInsertOrUpdateDeviceStateDown = new AtomicInteger(0);
	private final AtomicInteger countUpdateEdgeConfigUp = new AtomicInteger(0);
	private final AtomicInteger countUpdateEdgeConfigDown = new AtomicInteger(0);
	private final AtomicInteger countUpdateEdgeProducttypeUp = new AtomicInteger(0);
	private final AtomicInteger countUpdateEdgeProducttypeDown = new AtomicInteger(0);
	private final AtomicInteger countUpdateEdgeVersionUp = new AtomicInteger(0);
	private final AtomicInteger countUpdateEdgeVersionDown = new AtomicInteger(0);

	private void count(DatabaseTask task, boolean up) {
		if (up) {
			AtomicInteger counter;
			if (task instanceof InsertEdgeConfigUpdate) {
				counter = this.countInsertEdgeConfigUpdateUp;
			} else if (task instanceof InsertOrUpdateDeviceStates) {
				counter = this.countInsertOrUpdateDeviceStateUp;
			} else if (task instanceof UpdateEdgeConfig) {
				counter = this.countUpdateEdgeConfigUp;
			} else if (task instanceof UpdateEdgeProducttype) {
				counter = this.countUpdateEdgeProducttypeUp;
			} else if (task instanceof UpdateEdgeVersion) {
				counter = this.countUpdateEdgeVersionUp;
			} else {
				System.out.println("Unknown Task " + task.getClass());
				return;
			}
			counter.getAndIncrement();
		} else {
			AtomicInteger counter;
			if (task instanceof InsertEdgeConfigUpdate) {
				counter = this.countInsertEdgeConfigUpdateDown;
			} else if (task instanceof InsertOrUpdateDeviceStates) {
				counter = this.countInsertOrUpdateDeviceStateDown;
			} else if (task instanceof UpdateEdgeConfig) {
				counter = this.countUpdateEdgeConfigDown;
			} else if (task instanceof UpdateEdgeProducttype) {
				counter = this.countUpdateEdgeProducttypeDown;
			} else if (task instanceof UpdateEdgeVersion) {
				counter = this.countUpdateEdgeVersionDown;
			} else {
				System.out.println("Unknown Task " + task.getClass());
				return;
			}
			counter.getAndIncrement();
		}
	}

}
