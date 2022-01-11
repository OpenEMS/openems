package io.openems.edge.battery.protection.currenthandler;

import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.protection.BatteryProtection.ChannelId;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class ChargeMaxCurrentHandler extends AbstractMaxCurrentHandler {

	public static class Builder extends AbstractMaxCurrentHandler.Builder<Builder> {

		private ForceDischarge.Params forceDischargeParams = null;

		/**
		 * Creates a {@link Builder} for {@link ChargeMaxCurrentHandler}.
		 *
		 * @param clockProvider                         a {@link ClockProvider}, mainly
		 *                                              for JUnit tests
		 * @param initialBmsMaxEverAllowedChargeCurrent the (estimated) maximum allowed
		 *                                              charge current. This is used as
		 *                                              a reference for percentage
		 *                                              values. If during runtime a
		 *                                              higher value is provided, that
		 *                                              one is taken from then on.
		 */
		protected Builder(ClockProvider clockProvider, int initialBmsMaxEverAllowedChargeCurrent) {
			super(clockProvider, initialBmsMaxEverAllowedChargeCurrent);
		}

		/**
		 * Configure 'Force Discharge' parameters.
		 *
		 * @param startDischargeAboveCellVoltage start force discharge if maxCellVoltage
		 *                                       is above this value, e.g. 3660
		 * @param dischargeAboveCellVoltage      force discharge as long as
		 *                                       maxCellVoltage is above this value,
		 *                                       e.g. 3640
		 * @param blockChargeAboveCellVoltage    after 'force discharge', block charging
		 *                                       as long as maxCellVoltage is above this
		 *                                       value, e.g. 3450
		 * @return {@link Builder}
		 */
		public Builder setForceDischarge(int startDischargeAboveCellVoltage, int dischargeAboveCellVoltage,
				int blockChargeAboveCellVoltage) {
			this.forceDischargeParams = new ForceDischarge.Params(startDischargeAboveCellVoltage,
					dischargeAboveCellVoltage, blockChargeAboveCellVoltage);
			return this;
		}

		/**
		 * Sets the {@link ForceDischarge.Params} parameters.
		 *
		 * @param forceDischargeParams the {@link ForceDischarge.Params}
		 * @return a {@link Builder}
		 */
		public Builder setForceDischarge(ForceDischarge.Params forceDischargeParams) {
			this.forceDischargeParams = forceDischargeParams;
			return this;
		}

		/**
		 * Builds the {@link ChargeMaxCurrentHandler} instance.
		 *
		 * @return a {@link ChargeMaxCurrentHandler}
		 */
		public ChargeMaxCurrentHandler build() {
			return new ChargeMaxCurrentHandler(this.clockProvider, this.initialBmsMaxEverCurrent, this.voltageToPercent,
					this.temperatureToPercent, this.maxIncreasePerSecond, this.forceDischargeParams);
		}

		@Override
		protected Builder self() {
			return this;
		}
	}

	/**
	 * Create a {@link ChargeMaxCurrentHandler} builder.
	 *
	 * @param clockProvider                         a {@link ClockProvider}
	 * @param initialBmsMaxEverAllowedChargeCurrent the (estimated) maximum allowed
	 *                                              charge current. This is used as
	 *                                              a reference for percentage
	 *                                              values. If during runtime a
	 *                                              higher value is provided, that
	 *                                              one is taken from then on.
	 * @return a {@link Builder}
	 */
	public static Builder create(ClockProvider clockProvider, int initialBmsMaxEverAllowedChargeCurrent) {
		return new Builder(clockProvider, initialBmsMaxEverAllowedChargeCurrent);
	}

	protected ChargeMaxCurrentHandler(ClockProvider clockProvider, int initialBmsMaxEverAllowedChargeCurrent,
			PolyLine voltageToPercent, PolyLine temperatureToPercent, Double maxIncreasePerSecond,
			ForceDischarge.Params forceDischargeParams) {
		super(clockProvider, initialBmsMaxEverAllowedChargeCurrent, voltageToPercent, temperatureToPercent,
				maxIncreasePerSecond, ForceDischarge.from(forceDischargeParams));
	}

	@Override
	protected ChannelId getBpBmsChannelId() {
		return BatteryProtection.ChannelId.BP_CHARGE_BMS;
	}

	@Override
	protected ChannelId getBpMinVoltageChannelId() {
		return BatteryProtection.ChannelId.BP_CHARGE_MIN_VOLTAGE;
	}

	@Override
	protected ChannelId getBpMaxVoltageChannelId() {
		return BatteryProtection.ChannelId.BP_CHARGE_MAX_VOLTAGE;
	}

	@Override
	protected ChannelId getBpMinTemperatureChannelId() {
		return BatteryProtection.ChannelId.BP_CHARGE_MIN_TEMPERATURE;
	}

	@Override
	protected ChannelId getBpMaxTemperatureChannelId() {
		return BatteryProtection.ChannelId.BP_CHARGE_MAX_TEMPERATURE;
	}

	@Override
	protected ChannelId getBpMaxIncreaseAmpereChannelId() {
		return BatteryProtection.ChannelId.BP_CHARGE_INCREASE;
	}

	@Override
	protected ChannelId getBpForceCurrentChannelId() {
		return BatteryProtection.ChannelId.BP_FORCE_DISCHARGE;
	}

}
