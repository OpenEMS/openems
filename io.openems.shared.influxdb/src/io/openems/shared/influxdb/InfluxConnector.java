package io.openems.shared.influxdb;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;

import com.google.gson.JsonElement;
import com.influxdb.client.write.Point;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

public interface InfluxConnector {
	public static final String MEASUREMENT = "data";

	/**
	 * deactivates the influx connector.
	 */
	public void deactivate();

	/**
	 * Queries historic energy.
	 *
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @return a map between ChannelAddress and value
	 * @throws OpenemsException on error
	 */
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(Optional<Integer> influxEdgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException;

	/**
	 * Queries historic energy per period.
	 *
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @param resolution   the resolution in seconds
	 * @return the historic data as Map
	 * @throws OpenemsException on error
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(
			Optional<Integer> influxEdgeId, ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels,
			Resolution resolution) throws OpenemsNamedException;

	/**
	 * Queries historic data.
	 * 
	 * @param influxEdgeId the unique, numeric Edge-ID; or Empty to query all Edges
	 * @param fromDate     the From-Date
	 * @param toDate       the To-Date
	 * @param channels     the Channels to query
	 * @param resolution   the resolution in seconds
	 * @return the historic data as Map
	 * @throws OpenemsException on error
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(
			Optional<Integer> influxEdgeId, ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels,
			Resolution resolution) throws OpenemsNamedException;

	/**
	 * Queries the latest available channel values.
	 *
	 * @param influxEdgeId     the unique, numeric Edge-ID; or Empty to query all
	 *                         Edges
	 * @param channelAddresses the {@link ChannelAddress}es
	 * @return a map of {@link ChannelAddress}es and values
	 * @throws OpenemsException on error
	 */
	public SortedMap<ChannelAddress, JsonElement> queryChannelValues(Optional<Integer> influxEdgeId,
			Set<ChannelAddress> channelAddresses);

	/**
	 * Actually write the Point to InfluxDB.
	 *
	 * @param point the InfluxDB Point
	 * @throws OpenemsException on error
	 */
	public void write(Point point);
}
