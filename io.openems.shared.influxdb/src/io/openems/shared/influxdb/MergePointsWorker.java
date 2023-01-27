package io.openems.shared.influxdb;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.influxdb.client.write.Point;

import io.openems.common.worker.AbstractImmediateWorker;

public class MergePointsWorker extends AbstractImmediateWorker {

	private static final int MAX_POINTS_PER_WRITE = 1_000;
	private static final int MAX_AGGREGATE_WAIT = 10; // [s]

	private final Logger log = LoggerFactory.getLogger(MergePointsWorker.class);

	private final InfluxConnector parent;
	private final Consumer<Throwable> onWriteError;

	public MergePointsWorker(InfluxConnector parent, Consumer<Throwable> onWriteError) {
		this.parent = parent;
		this.onWriteError = onWriteError;
	}

	@Override
	protected void forever() throws InterruptedException {
		/*
		 * Merge Points. Wait max 10 seconds in total.
		 */
		final Instant maxWait = Instant.now().plusSeconds(MAX_AGGREGATE_WAIT);
		List<Point> points = new ArrayList<>(MAX_POINTS_PER_WRITE);
		for (int i = 0; i < MAX_POINTS_PER_WRITE; i++) {
			var point = this.parent.pointsQueue.poll(MAX_AGGREGATE_WAIT, TimeUnit.SECONDS);
			if (point == null) {
				break;
			}
			points.add(point);
			if (Instant.now().isAfter(maxWait)) {
				break;
			}
		}
		/*
		 * Write points async.
		 */
		if (!points.isEmpty()) {
			this.parent.executor.execute(() -> {
				try {
					this.parent.getInfluxConnection().writeApi.writePoints(points);
				} catch (Throwable t) {
					this.log.warn("Unable to write points. " + t.getMessage());
					this.onWriteError.accept(t);
				}
			});
		}
	}

}
