package io.openems.backend.timedata.timescaledb;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

// TODO replace with Timedata
public interface Timescaledb {

	/**
	 * Sends the data points to the Timedata service. Will be replaced with Timedata
	 * eventually.
	 *
	 * @param edgeId The unique Edge-ID
	 * @param data   Table of timestamp (epoch in milliseconds), Channel-Address and
	 *               the Channel value as JsonElement. Sorted by timestamp.
	 * @throws OpenemsException on error
	 */
	public void write(String edgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data) throws OpenemsException;

	/**
	 * Gets the latest value for the given ChannelAddress.
	 *
	 * @param edgeId         The unique Edge-ID
	 * @param channelAddress The Channel-Address
	 * @return the value
	 */
	public Optional<JsonElement> getChannelValue(String edgeId, ChannelAddress channelAddress);

	/**
	 * Queries historic data.
	 *
	 * @param edgeId     the Edge-ID; or null query all
	 * @param fromDate   the From-Date
	 * @param toDate     the To-Date
	 * @param channels   the Channels
	 * @param resolution the {@link Resolution}
	 * @return the query result
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException;

	/**
	 * Queries historic energy.
	 *
	 * @param edgeId   the Edge-ID; or null query all
	 * @param fromDate the From-Date
	 * @param toDate   the To-Date
	 * @param channels the Channels
	 * @return the query result
	 */
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException;

	/**
	 * Queries historic energy per period.
	 *
	 * <p>
	 * This is for use-cases where you want to get the energy for each period (with
	 * {@link Resolution}) per Channel, e.g. to visualize energy in a histogram
	 * chart. For each period the energy is calculated by subtracting first value of
	 * the period from the last value of the period.
	 *
	 * @param edgeId     the Edge-ID; or null query all
	 * @param fromDate   the From-Date
	 * @param toDate     the To-Date
	 * @param channels   the Channels
	 * @param resolution the {@link Resolution}
	 * @return the query result
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException;
}
