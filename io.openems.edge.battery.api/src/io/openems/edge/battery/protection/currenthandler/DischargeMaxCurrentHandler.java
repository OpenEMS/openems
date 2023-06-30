package io.openems.edge.battery.protection.currenthandler;

import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.protection.BatteryProtection.ChannelId;
import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class DischargeMaxCurrentHandler extends AbstractMaxCurrentHandler {

	public static class Builder extends AbstractMaxCurrentHandler.Builder<Builder> {

		private ForceCharge.Params forceChargeParams = null;

		/**
		 * Creates a {@link Builder} for {@link DischargeMaxCurrentHandler}.
		 *
		 * @param clockProvider                     a {@link ClockProvider}, mainly for
		 *                                          JUnit tests
		 * @param initialBmsMaxEverDischargeCurrent the (estimated) maximum allowed
		 *                                          discharge current. This is used as a
		 *                                          reference for percentage values. If
		 *                                          during runtime a higher value is
		 *                                          provided, that one is taken from
		 *                                          then on.
		 */
		protected Builder(ClockProvider clockProvider, int initialBmsMaxEverDischargeCurrent) {
			super(clockProvider, initialBmsMaxEverDischargeCurrent);
		}

		/**
		 * Configure 'Force Charge' parameters.
		 *
		 * @param startChargeBelowCellVoltage    start force charge if minCellVoltage is
		 *                                       below this value, e.g. 2850
		 * @param chargeBelowCellVoltage         force charge as long as minCellVoltage
		 *                                       is below this value, e.g. 2910
		 * @param blockDischargeBelowCellVoltage after 'force charge', block discharging
		 *                                       as long as minCellVoltage is below this
		 *                                       value, e.g. 3000
		 * @return {@link Builder}
		 */
		public Builder setForceCharge(int startChargeBelowCellVoltage, int chargeBelowCellVoltage,
				int blockDischargeBelowCellVoltage) {
			this.forceChargeParams = new ForceCharge.Params(startChargeBelowCellVoltage, chargeBelowCellVoltage,
					blockDischargeBelowCellVoltage);
			return this;
		}

		/**
		 * Sets the {@link ForceCharge.Params} parameters.
		 *
		 * @param forceChargeParams the {@link ForceCharge.Params}
		 * @return a {@link Builder}
		 */
		public Builder setForceCharge(ForceCharge.Params forceChargeParams) {
			this.forceChargeParams = forceChargeParams;
			return this;
		}

		/**
		 * Builds the {@link DischargeMaxCurrentHandler} instance.
		 *
		 * @return a {@link DischargeMaxCurrentHandler}
		 */
		public DischargeMaxCurrentHandler build() {
			return new DischargeMaxCurrentHandler(this.clockProvider, this.initialBmsMaxEverCurrent,
					this.voltageToPercent, this.temperatureToPercent, this.maxIncreasePerSecond,
					ForceCharge.from(this.forceChargeParams));
		}

		@Override
		protected Builder self() {
			return this;
		}
	}

	/**
	 * Create a {@link DischargeMaxCurrentHandler} builder.
	 *
	 * @param clockProvider                     a {@link ClockProvider}
	 * @param initialBmsMaxEverDischargeCurrent the (estimated) maximum allowed
	 *                                          discharge current. This is used as a
	 *                                          reference for percentage values. If
	 *                                          during runtime a higher value is
	 *                                          provided, that one is taken from
	 *                                          then on.
	 * @return a {@link Builder}
	 */
	public static Builder create(ClockProvider clockProvider, int initialBmsMaxEverDischargeCurrent) {
		return new Builder(clockProvider, initialBmsMaxEverDischargeCurrent);
	}

	protected DischargeMaxCurrentHandler(ClockProvider clockProvider, int initialBmsMaxEverDischargeCurrent,
			PolyLine voltageToPercent, PolyLine temperatureToPercent, Double maxIncreasePerSecond,
			ForceCharge forceCharge) {
		super(clockProvider, initialBmsMaxEverDischargeCurrent, voltageToPercent, temperatureToPercent,
				maxIncreasePerSecond, forceCharge);
	}

	@Override
	protected ChannelId getBpBmsChannelId() {
		return BatteryProtection.ChannelId.BP_DISCHARGE_BMS;
	}

	@Override
	protected ChannelId getBpMinVoltageChannelId() {
		return BatteryProtection.ChannelId.BP_DISCHARGE_MIN_VOLTAGE;
	}

	@Override
	protected ChannelId getBpMaxVoltageChannelId() {
		return BatteryProtection.ChannelId.BP_DISCHARGE_MAX_VOLTAGE;
	}

	@Override
	protected ChannelId getBpMinTemperatureChannelId() {
		return BatteryProtection.ChannelId.BP_DISCHARGE_MIN_TEMPERATURE;
	}

	@Override
	protected ChannelId getBpMaxTemperatureChannelId() {
		return BatteryProtection.ChannelId.BP_DISCHARGE_MAX_TEMPERATURE;
	}

	@Override
	protected ChannelId getBpMaxIncreaseAmpereChannelId() {
		return BatteryProtection.ChannelId.BP_DISCHARGE_INCREASE;
	}

	@Override
	protected ChannelId getBpForceCurrentChannelId() {
		return BatteryProtection.ChannelId.BP_FORCE_CHARGE;
	}

}
