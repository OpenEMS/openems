package io.openems.backend.common.timedata;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.timedata.CommonTimedataService;

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

}
