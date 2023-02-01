package io.openems.backend.common.timedata;

import java.util.Map;
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.CommonTimedataService;
import io.openems.common.types.ChannelAddress;

@ProviderType
public interface Timedata extends CommonTimedataService {

	/**
	 * Sends the data points to the Timedata service.
	 *
	 * @param edgeId The unique Edge-ID
	 * @param data   Table of timestamp (epoch in milliseconds), Channel-Address and
	 *               the Channel value as JsonElement. Sorted by timestamp.
	 * @throws OpenemsException on error
	 */
	public void write(String edgeId, TreeBasedTable<Long, String, JsonElement> data) throws OpenemsException;

	/**
	 * Gets the latest values for the given ChannelAddresses.
	 *
	 * @param edgeId           The unique Edge-ID
	 * @param channelAddresses The {@link ChannelAddress}es
	 * @return the values
	 */
	public Map<ChannelAddress, JsonElement> getChannelValues(String edgeId, Set<ChannelAddress> channelAddresses);

}
