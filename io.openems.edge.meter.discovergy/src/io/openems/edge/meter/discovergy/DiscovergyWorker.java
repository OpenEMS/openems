package io.openems.edge.meter.discovergy;

import java.util.ArrayList;
import java.util.List;

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
import io.openems.edge.meter.discovergy.jsonrpc.DiscovergyMeter;
import io.openems.edge.meter.discovergy.jsonrpc.Field;

public class DiscovergyWorker extends AbstractCycleWorker {

	private static final int LAST_READING_TOO_OLD_SECONDS = 30;

	private final Logger log = LoggerFactory.getLogger(DiscovergyWorker.class);
	private final MeterDiscovergy parent;
	private final DiscovergyApiClient apiClient;
	private final Config config;

	/**
	 * Holds the internal Discovergy meterId.
	 */
	private String meterId = null;

	public DiscovergyWorker(MeterDiscovergy parent, DiscovergyApiClient apiClient, Config config) {
		this.parent = parent;
		this.apiClient = apiClient;
		this.config = config;
		if (!config.meterId().trim().isEmpty()) {
			this.meterId = config.meterId();
		}
	}

	@Override
	protected void forever() throws OpenemsNamedException {
		Integer activePower = null;
		Integer activePowerL1 = null;
		Integer activePowerL2 = null;
		Integer activePowerL3 = null;
		Integer voltageL1 = null;
		Integer voltageL2 = null;
		Integer voltageL3 = null;
		Long productionEnergy = null;
		Long consumptionEnergy = null;
		boolean restApiFailed = true;
		boolean lastReadingTooOld = false;

		try {
			// Asserts that we have a valid MeterId
			this.assertMeterId();

			// Retrieve Channel-Values
			JsonObject reading = this.apiClient.getLastReading(this.meterId, //
					Field.POWER, Field.POWER_L1, Field.POWER_L2, Field.POWER_L3, //
					Field.VOLTAGE_L1, Field.VOLTAGE_L2, Field.VOLTAGE_L3, //
					Field.ENERGY_IN, Field.ENERGY_OUT);
			Long time = JsonUtils.getAsLong(reading, "time");
			if (System.currentTimeMillis() / 1000 - LAST_READING_TOO_OLD_SECONDS < time) {
				// Values are valid
				JsonObject values = JsonUtils.getAsJsonObject(reading, "values");
				activePower = JsonUtils.getAsInt(values, "power") / 1000;
				activePowerL1 = JsonUtils.getAsInt(values, "power1") / 1000;
				activePowerL2 = JsonUtils.getAsInt(values, "power2") / 1000;
				activePowerL3 = JsonUtils.getAsInt(values, "power3") / 1000;
				voltageL1 = JsonUtils.getAsInt(values, "voltage1");
				voltageL2 = JsonUtils.getAsInt(values, "voltage2");
				voltageL3 = JsonUtils.getAsInt(values, "voltage3");
				productionEnergy = JsonUtils.getAsLong(values, "energy") / 10_000_000;
				consumptionEnergy = JsonUtils.getAsLong(values, "energyOut") / 10_000_000;
				lastReadingTooOld = false;

			} else {
				// Values are too old
				lastReadingTooOld = true;
			}

			restApiFailed = false;

		} catch (OpenemsException e) {
			this.parent.logError(this.log, "REST-Api failed: " + e.getMessage());

		} finally {
			this.parent.getActivePower().setNextValue(activePower);
			this.parent.getActivePowerL1().setNextValue(activePowerL1);
			this.parent.getActivePowerL2().setNextValue(activePowerL2);
			this.parent.getActivePowerL3().setNextValue(activePowerL3);
			this.parent.getVoltage().setNextValue(voltageL1);
			this.parent.getVoltageL1().setNextValue(voltageL1);
			this.parent.getVoltageL2().setNextValue(voltageL2);
			this.parent.getVoltageL3().setNextValue(voltageL3);
			this.parent.getActiveProductionEnergy().setNextValue(productionEnergy);
			this.parent.getActiveConsumptionEnergy().setNextValue(consumptionEnergy);
			this.parent.channel(ChannelId.REST_API_FAILED).setNextValue(restApiFailed);
			this.parent.channel(ChannelId.LAST_READING_TOO_OLD).setNextValue(lastReadingTooOld);
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
		JsonArray jMeters = this.apiClient.getMeters();
		if (jMeters.size() == 0) {
			// too few
			throw new OpenemsException("No Meters available.");
		}

		List<DiscovergyMeter> meters = new ArrayList<>();
		for (JsonElement j : jMeters) {
			DiscovergyMeter meter = DiscovergyMeter.fromJson(j);
			meters.add(meter);
		}

		if (!this.config.fullSerialNumber().trim().isEmpty()) {
			// search for given fullSerialNumber
			for (DiscovergyMeter meter : meters) {
				if (meter.getFullSerialNumber().equalsIgnoreCase(this.config.fullSerialNumber())) {
					this.meterId = meter.getMeterId();
					return;
				}
			}
			throw new OpenemsException(
					"Unable to find meter with full serial number [" + this.config.fullSerialNumber() + "]");
		}

		if (!this.config.serialNumber().trim().isEmpty()) {
			// search for given serialNumber
			for (DiscovergyMeter meter : meters) {
				if (meter.getFullSerialNumber().equalsIgnoreCase(this.config.serialNumber())) {
					this.meterId = meter.getMeterId();
					return;
				}
			}
			throw new OpenemsException("Unable to find meter with serial number [" + this.config.serialNumber() + "]");
		}

		if (meters.size() > 1) {
			// too many
			StringBuilder b = new StringBuilder("Unable to identify meter, because there are multiple:");
			for (DiscovergyMeter meter : meters) {
				b.append(" ");
				b.append(meter.toString());
			}
			throw new OpenemsException(b.toString());
		}

		// exactly one
		DiscovergyMeter meter = meters.get(0);
		this.meterId = meter.getMeterId();
		this.parent.logInfo(this.log, "Updated Discovergy MeterId [" + this.meterId + "]");
	}

}
