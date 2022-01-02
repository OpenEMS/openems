package io.openems.backend.metadata.odoo.postgres;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.metadata.odoo.postgres.task.DatabaseTask;
import io.openems.backend.metadata.odoo.postgres.task.InsertEdgeConfigUpdate;
import io.openems.backend.metadata.odoo.postgres.task.InsertOrUpdateDeviceStates;
import io.openems.backend.metadata.odoo.postgres.task.UpdateEdgeConfig;
import io.openems.backend.metadata.odoo.postgres.task.UpdateEdgeProducttype;
import io.openems.common.utils.ThreadPoolUtils;

/**
 * This worker writes all Statements in a queue.
 */
public class QueueWriteWorker {

	/**
	 * DEBUG_MODE activates printing of reqular statistics about queued tasks.
	 */
	private static final boolean DEBUG_MODE = false;

	private final Logger log = LoggerFactory.getLogger(QueueWriteWorker.class);
	private final PostgresHandler parent;
	private final HikariDataSource dataSource;

	// Executor for subscriptions task. Like a CachedThreadPool, but properly typed
	// for DEBUG_MODE.
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>(),
			new ThreadFactoryBuilder().setNameFormat("Metadata.Odoo.PGQueue-%d").build());

	private final ScheduledExecutorService debugLogExecutor;

	public QueueWriteWorker(PostgresHandler parent, HikariDataSource dataSource) {
		this.parent = parent;
		this.dataSource = dataSource;

		if (QueueWriteWorker.DEBUG_MODE) {
			this.debugLogExecutor = Executors.newSingleThreadScheduledExecutor();
		} else {
			this.debugLogExecutor = null;
		}
	}

	/**
	 * Starts the {@link QueueWriteWorker}.
	 */
	public synchronized void start() {
		if (QueueWriteWorker.DEBUG_MODE) {
			this.initializeDebugLog();
		}
	}

	/**
	 * Stops the {@link QueueWriteWorker}.
	 */
	public synchronized void stop() {
		// Shutdown executors
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
		ThreadPoolUtils.shutdownAndAwaitTermination(this.debugLogExecutor, 5);
	}

	/**
	 * Adds a {@link DatabaseTask} to the queue.
	 *
	 * @param task the {@link DatabaseTask}
	 */
	public void addTask(DatabaseTask task) {
		if (QueueWriteWorker.DEBUG_MODE) {
			this.count(task, true);
		}

		this.executor.execute(() -> {
			if (QueueWriteWorker.DEBUG_MODE) {
				this.count(task, false);
			}

			try {
				task.execute(this.dataSource);
			} catch (SQLException e) {
				this.parent.logWarn(this.log,
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
			var totalTasks = this.executor.getTaskCount();
			var completedTasks = this.executor.getCompletedTaskCount();
			var countInsertEdgeConfigUpdateUp = this.countInsertEdgeConfigUpdateUp.get();
			var countInsertEdgeConfigUpdateDown = this.countInsertEdgeConfigUpdateDown.get();
			var countInsertOrUpdateDeviceStateUp = this.countInsertOrUpdateDeviceStateUp.get();
			var countInsertOrUpdateDeviceStateDown = this.countInsertOrUpdateDeviceStateDown.get();
			var countUpdateEdgeConfigUp = this.countUpdateEdgeConfigUp.get();
			var countUpdateEdgeConfigDown = this.countUpdateEdgeConfigDown.get();
			var countUpdateEdgeProducttypeUp = this.countUpdateEdgeProducttypeUp.get();
			var countUpdateEdgeProducttypeDown = this.countUpdateEdgeProducttypeDown.get();

			this.parent.logInfo(this.log, "QueueWriteWorker. " //
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
			} else {
				System.out.println("Unknown Task " + task.getClass());
				return;
			}
			counter.getAndIncrement();
		}
	}

}
