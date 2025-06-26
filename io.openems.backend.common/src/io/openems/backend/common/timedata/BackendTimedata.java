package io.openems.backend.common.timedata;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.SortedMap;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.timedata.CommonTimedataService;
import io.openems.common.types.ChannelAddress;

@ProviderType
public interface BackendTimedata extends CommonTimedataService {

	/**
	 * Sends the data points to the Timedata service.
	 *
	 * @param edgeId The unique Edge-ID
	 * @param data   Table of timestamp (epoch in milliseconds), Channel-Address and
	 *               the Channel value as JsonElement. Sorted by timestamp.
	 */
	public void write(String edgeId, TimestampedDataNotification data);

	/**
	 * Sends the data points to the Timedata service.
	 *
	 * @param edgeId The unique Edge-ID
	 * @param data   Table of timestamp (epoch in milliseconds), Channel-Address and
	 *               the Channel value as AggregatedData. Sorted by timestamp.
	 */
	public void write(String edgeId, AggregatedDataNotification data);

	/**
	 * Sends the data points to the Timedata service.
	 * 
	 * @param edgeId The unique Edge-ID
	 * @param data   Table of timestamp (epoch in milliseconds), Channel-Address and
	 *               the Channel value as ResendData. Sorted by timestamp.
	 */
	public void write(String edgeId, ResendDataNotification data);

	/**
	 * Queries the latest values which are before the given {@link ZonedDateTime}.
	 * 
	 * @param edgeId   the id of the edge
	 * @param date     the bounding date exclusive
	 * @param channels the channels
	 * @return the channel values
	 * @throws OpenemsNamedException on error
	 */
	public default SortedMap<ChannelAddress, JsonElement> queryFirstValueBefore(//
			final String edgeId, //
			final ZonedDateTime date, //
			final Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		return null;
	}

}
