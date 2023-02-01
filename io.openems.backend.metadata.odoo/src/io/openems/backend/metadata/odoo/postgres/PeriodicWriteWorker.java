package io.openems.backend.metadata.odoo.postgres;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.backend.common.metadata.Edge;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.common.channel.Level;
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

	/**
	 * Holds the scheduled task.
	 */
	private ScheduledFuture<?> future = null;

	/**
	 * Executor for subscriptions task.
	 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10,
			new ThreadFactoryBuilder().setNameFormat("Metadata.Odoo.PGPeriodic-%d").build());

	public PeriodicWriteWorker(PostgresHandler parent) {
		this.parent = parent;
	}

	/**
	 * Starts the {@link PeriodicWriteWorker}.
	 */
	public synchronized void start() {
		this.future = this.executor.scheduleWithFixedDelay(//
				() -> this.task.accept(this.parent.edge), //
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

	private final LinkedBlockingQueue<Integer> lastMessageOdooIds = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<Integer> isOnlineOdooIds = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<Integer> isOfflineOdooIds = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<Integer> sumStateOk = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<Integer> sumStateInfo = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<Integer> sumStateWarning = new LinkedBlockingQueue<>();
	private final LinkedBlockingQueue<Integer> sumStateFault = new LinkedBlockingQueue<>();

	private final Consumer<PgEdgeHandler> task = edge -> {
		if (PeriodicWriteWorker.DEBUG_MODE) {
			this.debugLog();
		}

		try {
			// Last Message
			edge.updateLastMessage(drainToSet(this.lastMessageOdooIds));

			// Online/Offline
			edge.updateOpenemsIsConnected(drainToSet(this.isOfflineOdooIds), false);
			edge.updateOpenemsIsConnected(drainToSet(this.isOnlineOdooIds), true);

			// Sum-State
			edge.updateSumState(drainToSet(this.sumStateOk), Level.OK);
			edge.updateSumState(drainToSet(this.sumStateInfo), Level.INFO);
			edge.updateSumState(drainToSet(this.sumStateWarning), Level.WARNING);
			edge.updateSumState(drainToSet(this.sumStateFault), Level.FAULT);

		} catch (SQLException e) {
			this.log.error("Unable to execute WriteWorker task: " + e.getMessage());
		}
	};

	/**
	 * Called on {@link Edge.Events#ON_SET_LAST_MESSAGE_TIMESTAMP} event.
	 *
	 * @param edge the {@link MyEdge}.
	 */
	public void onLastMessage(MyEdge edge) {
		this.lastMessageOdooIds.add(edge.getOdooId());
	}

	/**
	 * Called on {@link Edge.Events#ON_SET_ONLINE} event.
	 *
	 * @param edge     the {@link MyEdge}.
	 * @param isOnline true if online, false if offline
	 */
	public void onSetOnline(MyEdge edge, boolean isOnline) {
		var odooId = edge.getOdooId();
		if (isOnline) {
			this.isOnlineOdooIds.add(odooId);
		} else {
			this.isOfflineOdooIds.add(odooId);
		}
	}

	/**
	 * Called on {@link Edge.Events#ON_SET_SUM_STATE} event.
	 *
	 * @param edge     the {@link MyEdge}.
	 * @param sumState Sum-State {@link Level}
	 */
	public void onSetSumState(MyEdge edge, Level sumState) {
		var odooId = edge.getOdooId();
		switch (sumState) {
		case OK:
			this.sumStateOk.add(odooId);
			break;
		case INFO:
			this.sumStateInfo.add(odooId);
			break;
		case WARNING:
			this.sumStateWarning.add(odooId);
			break;
		case FAULT:
			this.sumStateFault.add(odooId);
			break;
		}
	}

	/**
	 * Moves all entries of a {@link LinkedBlockingQueue} to a Set and clears the
	 * queue. This is thread-safe.
	 * 
	 * @param queue the {@link LinkedBlockingQueue}
	 * @return the {@link Set}
	 */
	protected static Set<Integer> drainToSet(LinkedBlockingQueue<Integer> queue) {
		Set<Integer> result = new HashSet<>(queue.size());
		queue.drainTo(result);
		return result;
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
