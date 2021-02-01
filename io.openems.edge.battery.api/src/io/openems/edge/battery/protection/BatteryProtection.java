package io.openems.edge.battery.protection;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.type.TypeUtils;

// TODO bmsMaxEver ist immer mindestens 80 bei Soltaro

/**
 * This utility class provides algorithms to calculate maximum allowed charge
 * and discharge currents for batteries based on
 * 
 * <ul>
 * <li>Allowed Current Limit provided by Battery Management System
 * <li>Voltage-to-Percent characteristics based on Min- and Max-Cell-Voltage
 * <li>Temperature-to-Percent characteristics based on Min- and
 * Max-Cell-Temperature
 * <li>Linear max increase limit (e.g. 0.5 A per second)
 * <li>Force Charge/Discharge mode (e.g. -1 A to enforce charge/discharge)
 * </ul>
 */
public class BatteryProtection {

	public static class Builder {

		private final Battery battery;

		private ChargeMaxCurrentHandler chargeMaxCurrentHandler;
		private DischargeMaxCurrentHandler dischargeMaxCurrentHandler;

		private ChannelId bmsAllowedChargeCurrentChannelId;
		private ChannelId bmsAllowedDischargeCurrentChannelId;

		protected Builder(Battery battery) {
			this.battery = battery;
		}

		/**
		 * Sets the {@link ChargeMaxCurrentHandler}.
		 * 
		 * @return a {@link Builder}
		 */
		public Builder setChargeMaxCurrentHandler(ChargeMaxCurrentHandler chargeMaxCurrentHandler) {
			this.chargeMaxCurrentHandler = chargeMaxCurrentHandler;
			return this;
		}

		/**
		 * Sets the {@link DischargeMaxCurrentHandler}.
		 * 
		 * @return a {@link Builder}
		 */
		public Builder setDischargeMaxCurrentHandler(DischargeMaxCurrentHandler dischargeMaxCurrentHandler) {
			this.dischargeMaxCurrentHandler = dischargeMaxCurrentHandler;
			return this;
		}

		public Builder setBmsAllowedChargeCurrent(ChannelId bmsAllowedChargeCurrentChannelId) {
			this.bmsAllowedChargeCurrentChannelId = bmsAllowedChargeCurrentChannelId;
			return this;
		}

		public Builder setBmsAllowedDischargeCurrent(ChannelId bmsAllowedDischargeCurrentChannelId) {
			this.bmsAllowedDischargeCurrentChannelId = bmsAllowedDischargeCurrentChannelId;
			return this;
		}

		public BatteryProtection build() {
			return new BatteryProtection(this.battery, this.chargeMaxCurrentHandler, this.dischargeMaxCurrentHandler,
					this.bmsAllowedChargeCurrentChannelId, this.bmsAllowedDischargeCurrentChannelId);
		}
	}

	public static Builder create(Battery battery) {
		return new Builder(battery);
	}

	private final Battery battery;
	private final ChargeMaxCurrentHandler chargeMaxCurrentHandler;
	private final DischargeMaxCurrentHandler dischargeMaxCurrentHandler;
	private final ChannelId bmsChargeMaxCurrentChannelId;
	private final ChannelId bmsDischargeMaxCurrentChannelId;

	protected BatteryProtection(Battery battery, ChargeMaxCurrentHandler chargeMaxCurrentHandler,
			DischargeMaxCurrentHandler dischargeMaxCurrentHandler, ChannelId bmsChargeMaxCurrentChannelId,
			ChannelId bmsDischargeMaxCurrentChannelId) {
		TypeUtils.assertNull("BatteryProtection algorithm is missing data", battery, chargeMaxCurrentHandler,
				dischargeMaxCurrentHandler, bmsChargeMaxCurrentChannelId, bmsDischargeMaxCurrentChannelId);
		this.battery = battery;
		this.chargeMaxCurrentHandler = chargeMaxCurrentHandler;
		this.dischargeMaxCurrentHandler = dischargeMaxCurrentHandler;
		this.bmsChargeMaxCurrentChannelId = bmsChargeMaxCurrentChannelId;
		this.bmsDischargeMaxCurrentChannelId = bmsDischargeMaxCurrentChannelId;
	}

	public void apply() {
		// Read input parameters from Battery
		Integer minCellVoltage = this.battery.getMinCellVoltage().get();
		Integer maxCellVoltage = this.battery.getMaxCellVoltage().get();
		Integer minCellTemperature = this.battery.getMinCellTemperature().get();
		Integer maxCellTemperature = this.battery.getMaxCellTemperature().get();
		IntegerReadChannel bmsAllowedChargeCurrentChannel = this.battery.channel(this.bmsChargeMaxCurrentChannelId);
		Integer bmsAllowedChargeCurrent = bmsAllowedChargeCurrentChannel.value().get();
		IntegerReadChannel bmsAllowedDischargeCurrentChannel = this.battery
				.channel(this.bmsDischargeMaxCurrentChannelId);
		int bmsAllowedDischargeCurrent = bmsAllowedDischargeCurrentChannel.value().get();

		// Use MaxCurrentHandlers to calculate max charge and discharge currents
		int chargeMaxCurrent = this.chargeMaxCurrentHandler.calculateCurrentLimit(minCellVoltage, maxCellVoltage,
				minCellTemperature, maxCellTemperature, bmsAllowedChargeCurrent);
		int dischargeMaxCurrent = this.dischargeMaxCurrentHandler.calculateCurrentLimit(minCellVoltage, maxCellVoltage,
				minCellTemperature, maxCellTemperature, bmsAllowedDischargeCurrent);

		// Set max charge and discharge currents
		battery._setChargeMaxCurrent(chargeMaxCurrent);
		battery._setDischargeMaxCurrent(dischargeMaxCurrent);

		// Set State-Channels on 'Force Charge' or 'Force Discharge' mode
		battery._setForceDischargeActive(chargeMaxCurrent < 0);
		battery._setForceChargeActive(dischargeMaxCurrent < 0);
	}
}
