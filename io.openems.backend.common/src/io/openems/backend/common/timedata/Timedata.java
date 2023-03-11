package io.openems.backend.common.timedata;

import org.osgi.annotation.versioning.ProviderType;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.CommonTimedataService;

@ProviderType
public interface Timedata extends CommonTimedataService {

	/**
	 * Returns a unique ID for this OpenEMS component.
	 *
	 * @return the unique ID
	 */
	public String id();

	/**
	 * Sends the data points to the Timedata service.
	 *
	 * @param edgeId The unique Edge-ID
	 * @param data   Table of timestamp (epoch in milliseconds), Channel-Address and
	 *               the Channel value as JsonElement. Sorted by timestamp.
	 * @throws OpenemsException on error
	 */
	public void write(String edgeId, TreeBasedTable<Long, String, JsonElement> data) throws OpenemsException;

}
