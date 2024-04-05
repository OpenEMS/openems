package io.openems.shared.influxdb;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.influxdb.client.write.Point;
import com.influxdb.client.write.WriteParameters;
import com.influxdb.exceptions.BadRequestException;

import io.openems.shared.influxdb.SafeMergePointsWorker.WritePoint;

public class SafeMergePointsWorker extends AbstractMergePointsWorker<WritePoint> implements MergePointsWorker {

	public SafeMergePointsWorker(InfluxConnector parent, String name, WriteParameters writeParameters,
			Consumer<BadRequestException> onWriteError) {
		super(parent, name, writeParameters, onWriteError);
	}

	public static class WritePoint {
		public final Point point;
		private int failedCountDown = 3;

		public WritePoint(Point point) {
			super();
			this.point = point;
		}
	}

	@Override
	public boolean offer(Point point) {
		return this.offer(new WritePoint(point));
	}

	@Override
	protected List<Point> writePoints(List<WritePoint> points) {
		return points.stream().map(t -> t.point) //
				.collect(Collectors.toList());
	}

	@Override
	protected void onWriteError(Throwable t, List<WritePoint> points) {
		super.onWriteError(t, points);
		points.stream() //
				.peek(w -> w.failedCountDown--) //
				.filter(w -> w.failedCountDown > 0) //
				.forEach(this::offer);
	}

}
