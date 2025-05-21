package io.openems.shared.influxdb;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.influxdb.client.write.Point;
import com.influxdb.client.write.WriteParameters;
import com.influxdb.exceptions.BadRequestException;

import io.openems.common.worker.AbstractImmediateWorker;

public abstract class AbstractMergePointsWorker<T> extends AbstractImmediateWorker implements MergePointsWorker {

	private static final int MAX_POINTS_PER_WRITE = 1_000;
	private static final int MAX_AGGREGATE_WAIT = 10; // [s]
	private static final int POINTS_QUEUE_SIZE = 1_000_000;

	private final Logger log = LoggerFactory.getLogger(MergePointsWorker.class);

	private final String name;
	protected final InfluxConnector parent;
	protected final WriteParameters writeParameters;
	private final Consumer<BadRequestException> onWriteError;

	private final BlockingQueue<T> pointsQueue = new LinkedBlockingQueue<>(POINTS_QUEUE_SIZE);

	public AbstractMergePointsWorker(InfluxConnector parent, String name, WriteParameters writeParameters,
			Consumer<BadRequestException> onWriteError) {
		this.parent = parent;
		this.name = name;
		this.writeParameters = writeParameters;
		this.onWriteError = onWriteError;
	}

	@Override
	public void activate() {
		this.activate("TimescaleDB-MergePoints" + this.name);
	}

	@Override
	protected void forever() throws InterruptedException {
		var points = this.pollPoints();

		if (points.isEmpty()) {
			return;
		}

		/*
		 * Write points async.
		 */
		this.parent.executor.execute(() -> {
			if (this.parent.queryProxy.isLimitReached()) {
				return;
			}
			try {
				this.parent.getInfluxConnection().writeApi.writePoints(this.writePoints(points), this.writeParameters);
				this.parent.queryProxy.queryLimit.decrease();
			} catch (Throwable t) {
				this.parent.queryProxy.queryLimit.increase();
				this.onWriteError(t, points);
			}
		});
	}

	private List<T> pollPoints() throws InterruptedException {
		final Instant maxWait = Instant.now().plusSeconds(MAX_AGGREGATE_WAIT);
		var points = new ArrayList<T>(MAX_POINTS_PER_WRITE);
		for (int i = 0; i < MAX_POINTS_PER_WRITE; i++) {
			var point = this.pointsQueue.poll(MAX_AGGREGATE_WAIT, TimeUnit.SECONDS);
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

	protected abstract List<Point> writePoints(List<T> points);

	protected void onWriteError(Throwable t, List<T> points) {
		this.log.warn("Unable to write to InfluxDB. " + t.getClass().getSimpleName() + ": " + t.getMessage());
		if (t instanceof BadRequestException) {
			this.onWriteError.accept((BadRequestException) t);
		}
	}

	/**
	 * Inserts the specified element into this queue if it is possible to do so
	 * immediately without violating capacity restrictions, returning true upon
	 * success and false if no space is currently available.
	 * 
	 * @param point the point to add
	 * @return true if the point was added to this queue, else false
	 */
	public boolean offer(T point) {
		return this.pointsQueue.offer(point);
	}

	@Override
	public String debugLog() {
		final var pointsQueueSize = this.pointsQueue.size();
		return new StringBuilder() //
				.append(this.name) //
				.append(": ") //
				.append(pointsQueueSize) //
				.append("/") //
				.append(POINTS_QUEUE_SIZE) //
				.append((pointsQueueSize == POINTS_QUEUE_SIZE) ? " !!!POINTS BACKPRESSURE!!!" : "") //
				.toString();
	}

}
