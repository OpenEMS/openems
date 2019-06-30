package io.openems.edge.meter.discovergy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.meter.discovergy.MeterDiscovergy.ChannelId;

public class DiscovergyWorker extends AbstractCycleWorker {

	private final Logger log = LoggerFactory.getLogger(DiscovergyWorker.class);
	private final MeterDiscovergy parent;
	private final DiscovergyApiClient apiClient;

	/**
	 * Holds the internal Discovergy meterId.
	 */
	private String meterId = null;

	public DiscovergyWorker(MeterDiscovergy parent, DiscovergyApiClient apiClient, String meterId) {
		this.parent = parent;
		this.apiClient = apiClient;
		if (!meterId.trim().isEmpty()) {
			this.meterId = meterId;
		}
	}

	@Override
	protected void forever() throws OpenemsNamedException {
		Integer activePower = null;
		Integer activePowerL1 = null;
		Integer activePowerL2 = null;
		Integer activePowerL3 = null;
		Integer voltageL1 = null;
		Long energy = null;
		boolean restApiFailed = true;

		try {
			// Asserts that we have a valid MeterId
			this.assertMeterId();

			// Retrieve Channel-Values
			JsonObject reading = this.apiClient.getLastReading(this.meterId, "power", "power1", "power2", "power3",
					"voltage1", "energy");
			JsonObject values = JsonUtils.getAsJsonObject(reading, "values");
			activePower = JsonUtils.getAsInt(values, "power") / 1000;
			activePowerL1 = JsonUtils.getAsInt(values, "power1") / 1000;
			activePowerL2 = JsonUtils.getAsInt(values, "power2") / 1000;
			activePowerL3 = JsonUtils.getAsInt(values, "power3") / 1000;
			voltageL1 = JsonUtils.getAsInt(values, "voltage1");
			energy = JsonUtils.getAsLong(values, "energy") / 10_000_000;
			restApiFailed = false;

		} catch (OpenemsException e) {
			this.parent.logError(this.log, "REST-Api failed: " + e.getMessage());

		} finally {
			this.parent.getActivePower().setNextValue(activePower);
			this.parent.getActivePowerL1().setNextValue(activePowerL1);
			this.parent.getActivePowerL2().setNextValue(activePowerL2);
			this.parent.getActivePowerL3().setNextValue(activePowerL3);
			this.parent.getVoltage().setNextValue(voltageL1);
			this.parent.getActiveProductionEnergy().setNextValue(energy);
			this.parent.channel(ChannelId.REST_API_FAILED).setNextValue(restApiFailed);
		}
	}

	/**
	 * Validates that we have valid MeterId.
	 * 
	 * @throws OpenemsNamedException on invalid MeterId.
	 */
	private void assertMeterId() throws OpenemsNamedException {
		if (this.meterId != null) {
			return;
		}
		JsonArray meters = this.apiClient.getMeters();
		for (JsonElement meter : meters) {
			this.meterId = JsonUtils.getAsString(meter, "meterId");
			this.parent.logInfo(this.log, "Updated Discovergy MeterId [" + this.meterId + "]");
			return;
		}
		throw new OpenemsException("No Meters available.");
	}

}
