package io.openems.edge.controller.api.backend;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collector;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.OpenemsType;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;

/**
 * Sends all aggregated channels which have at least the
 * {@link Config#aggregationPriority() aggregationPriority} as the
 * {@link PersistencePriority} every
 * {@link SendAggregatedChannelValuesWorker#AGGREGATION_MINUTES
 * AGGREGATION_MINUTES} minutes.
 * 
 * <p>
 * Note: the time stamp of the values is the beginning of the period. For
 * cumulated values the max value is sent and for the others the average value
 * of the last period is sent.
 * 
 * <pre>
 * e. G. 
 *   12:35       data       12:40
 *     |----------------------|
 * time stamp
 * </pre>
 */
public class SendAggregatedChannelValuesWorker extends AbstractWorker {

	private static final int AGGREGATION_MINUTES = 5;

	private final BackendApiImpl parent;

	public SendAggregatedChannelValuesWorker(BackendApiImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() throws Throwable {
		final var enabledComponents = this.parent.componentManager.getEnabledComponents();
		final var allValues = this.collectData(enabledComponents);

		if (allValues.isEmpty()) {
			return;
		}

		this.parent.websocket.sendMessage(new AggregatedDataNotification(allValues));
	}

	private TreeBasedTable<Long, String, JsonElement> collectData(List<OpenemsComponent> enabledComponents) {
		final var now = LocalDateTime.now(this.parent.componentManager.getClock());
		final var endTime = now.truncatedTo(DurationUnit.ofMinutes(AGGREGATION_MINUTES));
		final var startTime = endTime.minusMinutes(AGGREGATION_MINUTES);

		final var timestamp = Instant.now().truncatedTo(DurationUnit.ofMinutes(AGGREGATION_MINUTES)) //
				.minus(AGGREGATION_MINUTES, ChronoUnit.MINUTES);
		final var timestampMillis = timestamp.toEpochMilli();

		final var table = TreeBasedTable.<Long, String, JsonElement>create();
		enabledComponents.stream() //
				.flatMap(component -> component.channels().stream()) //
				.filter(channel -> // Ignore WRITE_ONLY Channels
				channel.channelDoc().getAccessMode() != AccessMode.WRITE_ONLY //
						// Ignore Low-Priority Channels
						&& channel.channelDoc().getPersistencePriority()
								.isAtLeast(this.parent.config.aggregationPriority()))
				.forEach(channel -> {
					try {
						var value = channel.getPastValues() //
								// TODO the value that was 'active' at `startTime` is dropped here. This might
								// cause a problem especially if data is coming in bigger distances.
								.tailMap(startTime, true) //
								.entrySet() //
								.stream() //
								.filter(e -> e.getKey().isBefore(endTime)) //
								.filter(e -> e.getValue().isDefined()).map(e -> e.getValue().get()) //
								.collect(aggregateCollector(channel.channelDoc().getUnit().isCumulated(), //
										channel.getType()));

						if (value == null) {
							return;
						}

						table.put(timestampMillis, channel.address().toString(), value);
					} catch (IllegalArgumentException e) {
						// unable to collect data because types are not matching the expected one
						e.printStackTrace();
					}
				});

		return table;
	}

	protected static Collector<Object, ?, JsonElement> aggregateCollector(//
			final boolean isCumulated, //
			final OpenemsType type //
	) {
		return Collector.of(ArrayList::new, //
				(a, b) -> a.add(b), //
				(a, b) -> {
					b.addAll(a);
					return b;
				}, //
				t -> aggregate(isCumulated, type, t) //
		);
	}

	protected static JsonElement aggregate(boolean isCumulated, OpenemsType type, Collection<Object> values)
			throws IllegalArgumentException {
		switch (type) {
		case DOUBLE:
		case FLOAT: {
			final var stream = values.stream() //
					.mapToDouble(item -> TypeUtils.getAsType(OpenemsType.DOUBLE, item));
			if (isCumulated) {
				final var maxOpt = stream.max();
				if (maxOpt.isPresent()) {
					return new JsonPrimitive(maxOpt.getAsDouble());
				}
			} else {
				final var avgOpt = stream.average();
				if (avgOpt.isPresent()) {
					return new JsonPrimitive(avgOpt.getAsDouble());
				}
			}
			break;
		}
		// round averages to their type
		case BOOLEAN:
		case LONG:
		case INTEGER:
		case SHORT:
			final var stream = values.stream() //
					.mapToLong(item -> TypeUtils.getAsType(OpenemsType.LONG, item));
			if (isCumulated) {
				final var maxOpt = stream.max();
				if (maxOpt.isPresent()) {
					return new JsonPrimitive(maxOpt.getAsLong());
				}
			} else {
				final var avgOpt = stream.average();
				if (avgOpt.isPresent()) {
					return new JsonPrimitive(Math.round(avgOpt.getAsDouble()));
				}
			}
			break;
		case STRING:
			// return first string for now
			for (var item : values) {
				return new JsonPrimitive(TypeUtils.<String>getAsType(type, item));
			}
		}
		return null;
	}

	@Override
	protected int getCycleTime() {
		// runs every full AGGREGATION_MINUTES minutes + buffer of 1 seconds
		return (int) Instant.now() //
				.until(Instant.now() //
						.truncatedTo(DurationUnit.ofMinutes(AGGREGATION_MINUTES)) //
						.plus(AGGREGATION_MINUTES, ChronoUnit.MINUTES), ChronoUnit.MILLIS)
				+ 1 * 1000;
	}

}
