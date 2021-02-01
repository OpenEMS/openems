package io.openems.edge.battery.protection;

import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class ChargeMaxCurrentHandler extends AbstractMaxCurrentHandler {

	/**
	 * Holds parameters for 'Force Discharge' mode.
	 */
	public static class ForceDischargeParams {
		private final int startDischargeAboveCellVoltage;
		private final int dischargeAboveCellVoltage;
		private final int blockChargeAboveCellVoltage;

		public ForceDischargeParams(int startDischargeAboveCellVoltage, int dischargeAboveCellVoltage,
				int blockChargeAboveCellVoltage) {
			if (blockChargeAboveCellVoltage > dischargeAboveCellVoltage
					|| dischargeAboveCellVoltage > startDischargeAboveCellVoltage) {
				throw new IllegalArgumentException(
						"Make sure that startDischargeAboveCellVoltage > dischargeAboveCellVoltage > blockChargeAboveCellVoltage.");
			}

			this.startDischargeAboveCellVoltage = startDischargeAboveCellVoltage;
			this.dischargeAboveCellVoltage = dischargeAboveCellVoltage;
			this.blockChargeAboveCellVoltage = blockChargeAboveCellVoltage;
		}
	}

	public static class Builder extends AbstractMaxCurrentHandler.Builder<Builder> {

		private ForceDischargeParams forceDischargeParams = null;

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
			this.forceDischargeParams = new ForceDischargeParams(startDischargeAboveCellVoltage,
					dischargeAboveCellVoltage, blockChargeAboveCellVoltage);
			return this;
		}

		public Builder setForceDischarge(ForceDischargeParams forceDischargeParams) {
			this.forceDischargeParams = forceDischargeParams;
			return this;
		}

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
			ForceDischargeParams forceChargeParams) {
		super(clockProvider, initialBmsMaxEverAllowedChargeCurrent, voltageToPercent, temperatureToPercent,
				maxIncreasePerSecond);

		this.forceDischargeParams = forceChargeParams;
	}

	// used by 'getForceCurrent()'
	private final ForceDischargeParams forceDischargeParams;
	private boolean forceDischargeActive = false;

	/**
	 * Calculates the Ampere limit for force discharge mode. Returns:
	 * 
	 * <ul>
	 * <li>-1 -> in force discharge mode
	 * <li>0 -> in block charge mode
	 * <li>null -> otherwise
	 * </ul>
	 * 
	 * @return the limit or null
	 */
	protected Double getForceCurrent(Integer minCellVoltage, Integer maxCellVoltage) {
		if (this.forceDischargeParams == null || minCellVoltage == null || maxCellVoltage == null) {
			return null;
		}

		final Double result;
		if (maxCellVoltage >= this.forceDischargeParams.startDischargeAboveCellVoltage) {
			// Activate Force-Discharge mode
			result = -1.;
			this.forceDischargeActive = true;

		} else if (this.forceDischargeActive) {
			if (maxCellVoltage >= this.forceDischargeParams.dischargeAboveCellVoltage) {
				// Below 'startDischargeAboveCellVoltage', but 'forceDischargeActive' and above
				// 'dischargeAboveCellVoltage', so keep force-discharging
				result = -1.;

			} else if (maxCellVoltage >= this.forceDischargeParams.blockChargeAboveCellVoltage) {
				// Finished Force-Discharge mode - now in Block-Charge mode
				result = 0.;

			} else {
				// Neither 'Force-Discharge' nor 'Block-Charge'. No limit.
				result = null;
				this.forceDischargeActive = false;
			}

		} else {
			// Neither 'Force-Discharge' nor 'Block-Charge'. No limit.
			result = null;
		}

		return result;
	}
}
