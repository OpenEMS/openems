package io.openems.backend.timedata.timescaledb.internal.write;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;

import com.google.gson.JsonElement;
import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.timedata.timescaledb.internal.Priority;
import io.openems.backend.timedata.timescaledb.internal.Schema.ChannelRecord;
import io.openems.backend.timedata.timescaledb.internal.Type;
import io.openems.backend.timedata.timescaledb.internal.write.Point.FloatPoint;
import io.openems.backend.timedata.timescaledb.internal.write.Point.IntPoint;
import io.openems.backend.timedata.timescaledb.internal.write.Point.StringPoint;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;

public abstract class QueueHandler<T extends Point> {
	private final MergePointsWorker<T> mergePointsWorker;
	private final Class<T> pointClass;

	protected QueueHandler(MergePointsWorker<T> mergePointsWorker, Class<T> pointClass) {
		super();
		this.mergePointsWorker = mergePointsWorker;
		this.pointClass = pointClass;
	}

	/**
	 * Adds a point to the handler.
	 * 
	 * @param channel   the channel
	 * @param timestamp the timestamp
	 * @param json      the value
	 * @return true if the element was added to this queue, else false
	 * @throws OpenemsNamedException on error
	 */
	public boolean offer(ChannelRecord channel, long timestamp, JsonElement json) throws OpenemsNamedException {
		var time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
		var value = this.dataToPointConverter(channel, time, json);
		if (value == null) {
			return false;
		}
		return this.mergePointsWorker.getQueue().offer(value);
	}

	public MergePointsWorker<T> getMergePointsWorker() {
		return this.mergePointsWorker;
	}

	/**
	 * Returns a DebugLog String.
	 * 
	 * @return debug log
	 */
	public String debugLog() {
		var sb = new StringBuilder() //
				.append(this.pointClass.getSimpleName()) //
				.append(": ") //
				.append(this.mergePointsWorker.debugLog());
		return sb.toString();
	}

	protected abstract T dataToPointConverter(ChannelRecord channel, ZonedDateTime time, JsonElement json)
			throws OpenemsNamedException;

	/**
	 * Activates the {@link MergePointsWorker}.
	 */
	public void activate() {
		this.mergePointsWorker.activate("TimescaleDB-Merge-" + this.pointClass.getSimpleName());
	}

	/**
	 * Returns a new {@link QueueHandler} of the given type.
	 * 
	 * @param type       the type of the handler
	 * @param priority   the priority of the handler
	 * @param dataSource the dataSource to get database connections
	 * @param executor   the executor to execute writes
	 * @return the handler
	 */
	public static QueueHandler<?> of(Type type, Priority priority, HikariDataSource dataSource,
			ExecutorService executor) {
		switch (type) {
		case INTEGER:
			return new IntQueueHandler(dataSource, executor, type, priority);
		case FLOAT:
			return new FloatQueueHandler(dataSource, executor, type, priority);
		case STRING:
			return new StringQueueHandler(dataSource, executor, type, priority);
		}
		return null;
	}

	public static class IntQueueHandler extends QueueHandler<IntPoint> {

		public IntQueueHandler(HikariDataSource dataSource, ExecutorService executor, Type type, Priority priority) {
			super(new MergePointsWorker<IntPoint>(dataSource, executor, type, priority), IntPoint.class);
		}

		@Override
		protected IntPoint dataToPointConverter(ChannelRecord channel, ZonedDateTime time, JsonElement json)
				throws OpenemsNamedException {
			Long value = JsonUtils.getAsType(OpenemsType.LONG, json);
			if (value == null) {
				return null;
			}
			return new IntPoint(channel.id, time, value);
		}

	}

	public static class FloatQueueHandler extends QueueHandler<FloatPoint> {

		public FloatQueueHandler(HikariDataSource dataSource, ExecutorService executor, Type type, Priority priority) {
			super(new MergePointsWorker<FloatPoint>(dataSource, executor, type, priority), FloatPoint.class);
		}

		@Override
		protected FloatPoint dataToPointConverter(ChannelRecord channel, ZonedDateTime time, JsonElement json)
				throws OpenemsNamedException {
			Double value = JsonUtils.getAsType(OpenemsType.DOUBLE, json);
			if (value == null) {
				return null;
			}
			return new FloatPoint(channel.id, time, value);
		}

	}

	public static class StringQueueHandler extends QueueHandler<StringPoint> {

		public StringQueueHandler(HikariDataSource dataSource, ExecutorService executor, Type type, Priority priority) {
			super(new MergePointsWorker<StringPoint>(dataSource, executor, type, priority), StringPoint.class);
		}

		@Override
		protected StringPoint dataToPointConverter(ChannelRecord channel, ZonedDateTime time, JsonElement json)
				throws OpenemsNamedException {
			String value = JsonUtils.getAsType(OpenemsType.STRING, json);
			if (value == null) {
				return null;
			}
			return new StringPoint(channel.id, time, value);
		}

	}

}