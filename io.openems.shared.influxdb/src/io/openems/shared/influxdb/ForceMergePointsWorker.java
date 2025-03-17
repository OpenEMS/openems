package io.openems.shared.influxdb;

import java.util.List;
import java.util.function.Predicate;

import com.influxdb.client.write.Point;
import com.influxdb.client.write.WriteParameters;
import com.influxdb.exceptions.BadRequestException;

public class ForceMergePointsWorker extends AbstractMergePointsWorker<Point> implements MergePointsWorker {

	public ForceMergePointsWorker(InfluxConnector parent, String name, WriteParameters writeParameters,
			Predicate<BadRequestException> onWriteError) {
		super(parent, name, writeParameters, onWriteError);
	}

	@Override
	protected List<Point> writePoints(List<Point> points) {
		return points;
	}

}
