package io.openems.backend.timedata.timescaledb.internal.write;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.timedata.timescaledb.internal.Priority;
import io.openems.backend.timedata.timescaledb.internal.Type;
import io.openems.common.worker.AbstractImmediateWorker;

public class MergePointsWorker<POINT extends Point> extends AbstractImmediateWorker {

	private final HikariDataSource dataSource;
	private final ExecutorService executor;
	private final Type type;
	private final Priority priority;
	// TODO queue: delete old entries if full; like an EvictingQueue;
	// https://github.com/google/guava/issues/3882
	private final BlockingQueue<POINT> queue = new ArrayBlockingQueue<>(TimescaledbWriteHandler.POINTS_QUEUE_SIZE);
	private long countPoints = 0;

	public MergePointsWorker(HikariDataSource dataSource, ExecutorService executor, Type type, Priority priority) {
		this.dataSource = dataSource;
		this.executor = executor;
		this.type = type;
		this.priority = priority;
	}

	public BlockingQueue<POINT> getQueue() {
		return this.queue;
	}

	@Override
	protected void forever() throws InterruptedException {
		/**
		 * This task merges single Points to Lists of Points, which are then sent to
		 * TimescaleDB. This approach improves speed as not every single Point gets sent
		 * via HTTP individually.
		 */
		// Poll and merge Points. Wait max 10 seconds in total.
		var points = pollAndMergePoints(this.queue);

		if (points.isEmpty()) {
			return;
		}

		this.countPoints += points.size();

		// Write points async.
		this.executor.execute(new WritePointsHandler(this.dataSource, this.type, this.priority, points));
	}

	/**
	 * Poll and merge Points. Wait max 10 seconds in total.
	 * 
	 * @param <POINT> the type of the Point
	 * @param queue   the Queue of Points
	 * @return a list of Points
	 * @throws InterruptedException on error
	 */
	private static <POINT extends Point> List<Point> pollAndMergePoints(BlockingQueue<POINT> queue)
			throws InterruptedException {
		final Instant maxWait = Instant.now().plusSeconds(TimescaledbWriteHandler.MAX_AGGREGATE_WAIT);
		List<Point> points = new ArrayList<>(TimescaledbWriteHandler.MAX_POINTS_PER_WRITE);
		for (int i = 0; i < TimescaledbWriteHandler.MAX_POINTS_PER_WRITE; i++) {
			var point = queue.poll(TimescaledbWriteHandler.MAX_AGGREGATE_WAIT, TimeUnit.SECONDS);
			if (point == null) {
				break;
			}
			points.add(point);
			if (Instant.now().isAfter(maxWait)) {
				break;
			}
		}
		return points;
	}

	/**
	 * Returns a DebugLog String.
	 * 
	 * @return debug log
	 */
	public String debugLog() {
		return new StringBuilder() //
				.append(this.queue.size()) //
				.append("/") //
				.append(TimescaledbWriteHandler.POINTS_QUEUE_SIZE) //
				.append("|Total:") //
				.append(this.countPoints) //
				.toString();
	}
}
