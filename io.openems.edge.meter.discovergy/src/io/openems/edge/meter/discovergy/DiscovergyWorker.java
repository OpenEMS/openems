package io.openems.edge.meter.discovergy;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.discovergy.MeterDiscovergy.ChannelId;
import io.openems.edge.meter.discovergy.jsonrpc.DiscovergyMeter;
import io.openems.edge.meter.discovergy.jsonrpc.Field;

public class DiscovergyWorker extends AbstractCycleWorker {

	private static final int LAST_READING_TOO_OLD_SECONDS = 30;

	private final Logger log = LoggerFactory.getLogger(DiscovergyWorker.class);
	private final MeterDiscovergyImpl parent;
	private final DiscovergyApiClient apiClient;
	private final Config config;

	/** Holds the internal Discovergy meterId. */
	private String meterId = null;

	public DiscovergyWorker(MeterDiscovergyImpl parent, DiscovergyApiClient apiClient, Config config) {
		this.parent = parent;
		this.apiClient = apiClient;
		this.config = config;
		if (!config.meterId().trim().isEmpty()) {
			this.meterId = config.meterId();
		}
	}

	@Override
	protected void forever() throws OpenemsNamedException {
		Integer rawPower = null;
		Integer rawPower1 = null;
		Integer rawPower2 = null;
		Integer rawPower3 = null;
		Integer rawVoltage1 = null;
		Integer rawVoltage2 = null;
		Integer rawVoltage3 = null;
		Long rawEnergy = null;
		Long rawEnergy1 = null;
		Long rawEnergy2 = null;
		Long rawEnergyOut = null;
		Long rawEnergyOut1 = null;
		Long rawEnergyOut2 = null;

		var restApiFailed = true;
		var lastReadingTooOld = false;

		try {
			// Asserts that we have a valid MeterId
			this.assertMeterId();

			// Retrieve Channel-Values
			var reading = this.apiClient.getLastReading(this.meterId, //
					Field.POWER, Field.POWER1, Field.POWER2, Field.POWER3, //
					Field.VOLTAGE1, Field.VOLTAGE2, Field.VOLTAGE3, //
					Field.ENERGY, Field.ENERGY1, Field.ENERGY2, //
					Field.ENERGY_OUT, Field.ENERGY_OUT1, Field.ENERGY_OUT2);

			Long time = JsonUtils.getAsLong(reading, "time");
			if (System.currentTimeMillis() / 1000 - LAST_READING_TOO_OLD_SECONDS < time) {
				// Values are valid
				var values = JsonUtils.getAsJsonObject(reading, "values");
				rawPower = JsonUtils.getAsOptionalInt(values, Field.POWER.n()).orElse(null);
				rawPower1 = JsonUtils.getAsOptionalInt(values, Field.POWER1.n()).orElse(null);
				rawPower2 = JsonUtils.getAsOptionalInt(values, Field.POWER2.n()).orElse(null);
				rawPower3 = JsonUtils.getAsOptionalInt(values, Field.POWER3.n()).orElse(null);
				rawVoltage1 = JsonUtils.getAsOptionalInt(values, Field.VOLTAGE1.n()).orElse(null);
				rawVoltage2 = JsonUtils.getAsOptionalInt(values, Field.VOLTAGE2.n()).orElse(null);
				rawVoltage3 = JsonUtils.getAsOptionalInt(values, Field.VOLTAGE3.n()).orElse(null);
				rawEnergy = JsonUtils.getAsOptionalLong(values, Field.ENERGY.n()).orElse(null);
				rawEnergy1 = JsonUtils.getAsOptionalLong(values, Field.ENERGY1.n()).orElse(null);
				rawEnergy2 = JsonUtils.getAsOptionalLong(values, Field.ENERGY2.n()).orElse(null);
				rawEnergyOut = JsonUtils.getAsOptionalLong(values, Field.ENERGY_OUT.n()).orElse(null);
				rawEnergyOut1 = JsonUtils.getAsOptionalLong(values, Field.ENERGY_OUT1.n()).orElse(null);
				rawEnergyOut2 = JsonUtils.getAsOptionalLong(values, Field.ENERGY_OUT2.n()).orElse(null);

				lastReadingTooOld = false;

			} else {
				// Values are too old
				lastReadingTooOld = true;
			}

			restApiFailed = false;

		} catch (OpenemsException e) {
			this.parent.logError(this.log, "REST-Api failed: " + e.getMessage());

		} finally {
			// Raw Channels
			((IntegerReadChannel) this.parent.channel(ChannelId.RAW_POWER)).setNextValue(rawPower);
			((IntegerReadChannel) this.parent.channel(ChannelId.RAW_POWER1)).setNextValue(rawPower1);
			((IntegerReadChannel) this.parent.channel(ChannelId.RAW_POWER2)).setNextValue(rawPower2);
			((IntegerReadChannel) this.parent.channel(ChannelId.RAW_POWER3)).setNextValue(rawPower3);
			((IntegerReadChannel) this.parent.channel(ChannelId.RAW_VOLTAGE1)).setNextValue(rawVoltage1);
			((IntegerReadChannel) this.parent.channel(ChannelId.RAW_VOLTAGE2)).setNextValue(rawVoltage2);
			((IntegerReadChannel) this.parent.channel(ChannelId.RAW_VOLTAGE3)).setNextValue(rawVoltage3);
			((LongReadChannel) this.parent.channel(ChannelId.RAW_ENERGY)).setNextValue(rawEnergy);
			((LongReadChannel) this.parent.channel(ChannelId.RAW_ENERGY1)).setNextValue(rawEnergy1);
			((LongReadChannel) this.parent.channel(ChannelId.RAW_ENERGY2)).setNextValue(rawEnergy2);
			((LongReadChannel) this.parent.channel(ChannelId.RAW_ENERGY_OUT)).setNextValue(rawEnergyOut);
			((LongReadChannel) this.parent.channel(ChannelId.RAW_ENERGY_OUT1)).setNextValue(rawEnergyOut1);
			((LongReadChannel) this.parent.channel(ChannelId.RAW_ENERGY_OUT2)).setNextValue(rawEnergyOut2);

			// State Channels
			this.parent.channel(ChannelId.REST_API_FAILED).setNextValue(restApiFailed);
			this.parent.channel(ChannelId.LAST_READING_TOO_OLD).setNextValue(lastReadingTooOld);

			// Nature Channels
			var activePower = TypeUtils.divide(rawPower, 1_000);
			var activePowerL1 = TypeUtils.divide(rawPower1, 1_000);
			var activePowerL2 = TypeUtils.divide(rawPower2, 1_000);
			var activePowerL3 = TypeUtils.divide(rawPower3, 1_000);
			switch (this.config.type()) {
			case GRID:
				this.parent._setActivePower(activePower);
				this.parent._setActivePowerL1(activePowerL1);
				this.parent._setActivePowerL2(activePowerL2);
				this.parent._setActivePowerL3(activePowerL3);
				this.parent._setActiveProductionEnergy(TypeUtils.divide(rawEnergy, 10_000_000));
				this.parent._setActiveConsumptionEnergy(TypeUtils.divide(rawEnergyOut, 10_000_000));
				break;
			case CONSUMPTION_NOT_METERED: // to be validated
			case CONSUMPTION_METERED: // to be validated
			case PRODUCTION_AND_CONSUMPTION:
			case PRODUCTION:
				this.parent._setActivePower(TypeUtils.multiply(activePower, -1)); // invert
				this.parent._setActivePowerL1(TypeUtils.multiply(activePowerL1, -1)); // invert
				this.parent._setActivePowerL2(TypeUtils.multiply(activePowerL2, -1)); // invert
				this.parent._setActivePowerL3(TypeUtils.multiply(activePowerL3, -1)); // invert
				this.parent._setActiveProductionEnergy(TypeUtils.divide(rawEnergyOut, 10_000_000));
				this.parent._setActiveConsumptionEnergy(0 /* always zero for production-only meters */);
				break;
			}

			this.parent._setVoltage(TypeUtils.averageRounded(rawVoltage1, rawVoltage2, rawVoltage3));
			this.parent._setVoltageL1(rawVoltage1);
			this.parent._setVoltageL2(rawVoltage2);
			this.parent._setVoltageL3(rawVoltage3);
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
		var jMeters = this.apiClient.getMeters();
		if (jMeters.size() == 0) {
			// too few
			throw new OpenemsException("No Meters available.");
		}

		List<DiscovergyMeter> meters = new ArrayList<>();
		for (JsonElement j : jMeters) {
			var meter = DiscovergyMeter.fromJson(j);
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
			var b = new StringBuilder("Unable to identify meter, because there are multiple:");
			for (DiscovergyMeter meter : meters) {
				b.append(" ");
				b.append(meter.toString());
			}
			throw new OpenemsException(b.toString());
		}

		// exactly one
		var meter = meters.get(0);
		this.meterId = meter.getMeterId();
		this.parent.logInfo(this.log, "Updated Discovergy MeterId [" + this.meterId + "]");
	}

}
