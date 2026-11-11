package io.openems.backend.edge.manager;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.notification.AbstractDataNotification;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

public class DummyTimedataManager implements TimedataManager {

	private final Map<String, List<AbstractDataNotification>> dataStore = new ConcurrentHashMap<>();
	private final Logger log = LoggerFactory.getLogger(DummyTimedataManager.class);

	/**
	 * Gets the list of notifications for the given edgeId.
	 *
	 * @param edgeId EdgeId
	 * @return List of notifications for the given edgeId, or null if none exist
	 */
	public List<AbstractDataNotification> get(String edgeId) {
		return this.dataStore.getOrDefault(edgeId, List.of());
	}

	@Override
	public void write(String edgeId, TimestampedDataNotification timestampedDataNotification) {
		this.log.info("Writing TimestampedDataNotification for edgeId: {}", edgeId);
		this.dataStore.computeIfAbsent(edgeId, k -> new ArrayList<>()).add(timestampedDataNotification);
	}

	@Override
	public void write(String edgeId, AggregatedDataNotification aggregatedDataNotification) {
		this.log.info("Writing AggregatedDataNotification for edgeId: {}", edgeId);
		this.dataStore.computeIfAbsent(edgeId, k -> new ArrayList<>()).add(aggregatedDataNotification);
	}

	@Override
	public void write(String edgeId, ResendDataNotification resendDataNotification) {
		this.log.info("Writing ResendDataNotification for edgeId: {}", edgeId);
		this.dataStore.computeIfAbsent(edgeId, k -> new ArrayList<>()).add(resendDataNotification);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String s,
			ZonedDateTime zonedDateTime, ZonedDateTime zonedDateTime1, Set<ChannelAddress> set, Resolution resolution)
			throws OpenemsError.OpenemsNamedException {
		return null;
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String s, ZonedDateTime zonedDateTime,
			ZonedDateTime zonedDateTime1, Set<ChannelAddress> set) throws OpenemsError.OpenemsNamedException {
		return null;
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String s,
			ZonedDateTime zonedDateTime, ZonedDateTime zonedDateTime1, Set<ChannelAddress> set, Resolution resolution)
			throws OpenemsError.OpenemsNamedException {
		return null;
	}
}
