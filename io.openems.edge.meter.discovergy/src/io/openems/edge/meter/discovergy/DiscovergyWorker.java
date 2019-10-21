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

public class DiscovergyWorker extends AbstractCycleWorker {

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
				if (meter.fullSerialNumber.equalsIgnoreCase(this.config.fullSerialNumber())) {
					this.meterId = meter.meterId;
					return;
				}
			}
			throw new OpenemsException(
					"Unable to find meter with full serial number [" + this.config.fullSerialNumber() + "]");
		}

		if (!this.config.serialNumber().trim().isEmpty()) {
			// search for given serialNumber
			for (DiscovergyMeter meter : meters) {
				if (meter.fullSerialNumber.equalsIgnoreCase(this.config.serialNumber())) {
					this.meterId = meter.meterId;
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
		this.meterId = meter.meterId;
		this.parent.logInfo(this.log, "Updated Discovergy MeterId [" + this.meterId + "]");
	}

	private static class DiscovergyMeter {

		public static DiscovergyMeter fromJson(JsonElement j) throws OpenemsNamedException {
			String meterId = JsonUtils.getAsString(j, "meterId");
			String manufacturerId = JsonUtils.getAsOptionalString(j, "manufacturerId").orElse(""); // e.g. "ESY"
			String serialNumber = JsonUtils.getAsOptionalString(j, "serialNumber").orElse(""); // e.g. "12345678"
			String fullSerialNumber = JsonUtils.getAsOptionalString(j, "fullSerialNumber").orElse(""); // e.g.
																										// 1ESY1234567890
			String type = JsonUtils.getAsOptionalString(j, "type").orElse(""); // e.g. "EASYMETER"
			String measurementType = JsonUtils.getAsOptionalString(j, "measurementType").orElse(""); // e.g.
																										// "ELECTRICITY"
			return new DiscovergyMeter(meterId, manufacturerId, serialNumber, fullSerialNumber, type, measurementType);
		}

		private final String meterId;
		private final String manufacturerId;
		private final String serialNumber;
		private final String fullSerialNumber;
		private final String type;
		private final String measurementType;

		private DiscovergyMeter(String meterId, String manufacturerId, String serialNumber, String fullSerialNumber,
				String type, String measurementType) {
			this.meterId = meterId;
			this.manufacturerId = manufacturerId;
			this.serialNumber = serialNumber;
			this.fullSerialNumber = fullSerialNumber;
			this.type = type;
			this.measurementType = measurementType;
		}

		@Override
		public String toString() {
			return "[meterId=" + meterId + ", manufacturerId=" + manufacturerId + ", serialNumber=" + serialNumber
					+ ", fullSerialNumber=" + fullSerialNumber + ", type=" + type + ", measurementType="
					+ measurementType + "]";
		}

	}

}
