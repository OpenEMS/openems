package io.openems.backend.metadata.odoo.postgres;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

	private static final int UPDATE_INTERVAL_IN_SECONDS = 30;

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
				this::applyChanges, //
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
	private ExchangableObject<Map<Integer, Boolean>> connectionStatesToUpdate = new ExchangableObject<>(
			new ConcurrentHashMap<>());
	private ExchangableObject<Map<Integer, Level>> sumStatesToUpdate = new ExchangableObject<>(
			new ConcurrentHashMap<>());

	private final void applyChanges() {
		final var edge = this.parent.edge;
		if (PeriodicWriteWorker.DEBUG_MODE) {
			this.debugLog();
		}

		try {
			// Last Message
			edge.updateLastMessage(drainToSet(this.lastMessageOdooIds));

			// Online/Offline
			final var connectionToUpdate = this.connectionStatesToUpdate.exchange(new ConcurrentHashMap<>());
			final var edgesByConnection = connectionToUpdate.entrySet().stream() //
					.collect(groupingBy(Entry::getValue, mapping(Entry::getKey, toSet())));
			edge.updateOpenemsIsConnected(edgesByConnection.getOrDefault(false, emptySet()), false);
			edge.updateOpenemsIsConnected(edgesByConnection.getOrDefault(true, emptySet()), true);
			if (PeriodicWriteWorker.DEBUG_MODE) {
				this.parent.logInfo(this.log,
						"Update Edge connection states online["
								+ edgesByConnection.getOrDefault(true, emptySet()).size() + "] offline["
								+ edgesByConnection.getOrDefault(false, emptySet()).size() + "]");
			}

			// Sum-State
			final var statesToUpdate = this.sumStatesToUpdate.exchange(new ConcurrentHashMap<>());
			final var edgesByState = statesToUpdate.entrySet().stream()
					.collect(groupingBy(Entry::getValue, mapping(Entry::getKey, toSet())));
			for (var stateEntry : edgesByState.entrySet()) {
				edge.updateSumState(stateEntry.getValue(), stateEntry.getKey());
			}
			if (PeriodicWriteWorker.DEBUG_MODE) {
				this.parent.logInfo(this.log, "Update Edge sum states " + Stream.of(Level.values()) //
						.map(level -> {
							final var itemsToUpdate = edgesByState.getOrDefault(level, emptySet());
							return level.getName() + "[" + itemsToUpdate.size() + "]";
						}).collect(joining(" ")));
			}

		} catch (SQLException e) {
			this.log.error("Unable to execute WriteWorker task: " + e.getMessage());
		}
	}

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
		final var odooId = edge.getOdooId();
		this.connectionStatesToUpdate.lockReading(t -> {
			t.put(odooId, isOnline);
		});
	}

	/**
	 * Called on {@link Edge.Events#ON_SET_SUM_STATE} event.
	 *
	 * @param edge     the {@link MyEdge}.
	 * @param sumState Sum-State {@link Level}
	 */
	public void onSetSumState(MyEdge edge, Level sumState) {
		final var odooId = edge.getOdooId();
		this.sumStatesToUpdate.lockReading(t -> {
			t.put(odooId, sumState);
		});
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

	private static class ExchangableObject<T> {
		private final ReadWriteLock lock = new ReentrantReadWriteLock();
		private volatile T currentObject;

		public ExchangableObject(T currentObject) {
			super();
			this.currentObject = currentObject;
		}

		public T exchange(T newObject) {
			this.lock.writeLock().lock();
			try {
				final var prev = this.currentObject;
				this.currentObject = newObject;
				return prev;
			} finally {
				this.lock.writeLock().unlock();
			}
		}

		public void lockReading(Consumer<T> block) {
			this.lock.readLock().lock();
			try {
				block.accept(this.currentObject);
			} finally {
				this.lock.readLock().unlock();
			}
		}

	}

}
