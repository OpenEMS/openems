package io.openems.edge.battery.protection;

import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class DischargeMaxCurrentHandler extends AbstractMaxCurrentHandler {

	/**
	 * Holds parameters for 'Force Charge' mode.
	 */
	public static class ForceChargeParams {
		private final int startChargeBelowCellVoltage;
		private final int chargeBelowCellVoltage;
		private final int blockDischargeBelowCellVoltage;

		public ForceChargeParams(int startChargeBelowCellVoltage, int chargeBelowCellVoltage,
				int blockDischargeBelowCellVoltage) {
			if (blockDischargeBelowCellVoltage < chargeBelowCellVoltage
					|| chargeBelowCellVoltage < startChargeBelowCellVoltage) {
				throw new IllegalArgumentException(
						"Make sure that startChargeBelowCellVoltage < chargeBelowCellVoltage < blockDischargeBelowCellVoltage.");
			}

			this.startChargeBelowCellVoltage = startChargeBelowCellVoltage;
			this.chargeBelowCellVoltage = chargeBelowCellVoltage;
			this.blockDischargeBelowCellVoltage = blockDischargeBelowCellVoltage;
		}
	}

	public static class Builder extends AbstractMaxCurrentHandler.Builder<Builder> {

		private ForceChargeParams forceChargeParams = null;

		protected Builder(ClockProvider clockProvider, int initialBmsMaxEverChargeCurrent) {
			super(clockProvider, initialBmsMaxEverChargeCurrent);
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
			this.forceChargeParams = new ForceChargeParams(startChargeBelowCellVoltage, chargeBelowCellVoltage,
					blockDischargeBelowCellVoltage);
			return this;
		}

		public Builder setForceCharge(ForceChargeParams forceChargeParams) {
			this.forceChargeParams = forceChargeParams;
			return this;
		}

		public DischargeMaxCurrentHandler build() {
			return new DischargeMaxCurrentHandler(this.clockProvider, this.initialBmsMaxEverCurrent,
					this.voltageToPercent, this.temperatureToPercent, this.maxIncreasePerSecond,
					this.forceChargeParams);
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
	 *                                          then one.
	 * @return a {@link Builder}
	 */
	public static Builder create(ClockProvider clockProvider, int initialBmsMaxEverChargeCurrent) {
		return new Builder(clockProvider, initialBmsMaxEverChargeCurrent);
	}

	protected DischargeMaxCurrentHandler(ClockProvider clockProvider, int initialBmsMaxEverDischargeCurrent,
			PolyLine voltageToPercent, PolyLine temperatureToPercent, double maxIncreasePerSecond,
			ForceChargeParams forceChargeParams) {
		super(clockProvider, initialBmsMaxEverDischargeCurrent, voltageToPercent, temperatureToPercent,
				maxIncreasePerSecond);

		this.forceChargeParams = forceChargeParams;
	}

	// used by 'getForceCurrent()'
	private final ForceChargeParams forceChargeParams;
	private boolean forceChargeActive = false;

	/**
	 * Calculates the Ampere limit for force charge mode. Returns:
	 * 
	 * <ul>
	 * <li>-1 -> in force charge mode
	 * <li>0 -> in block discharge mode
	 * <li>null -> otherwise
	 * </ul>
	 * 
	 * @return the limit or null
	 */
	protected Double getForceCurrent(int minCellVoltage, int maxCellVoltage) {
		if (this.forceChargeParams == null) {
			// Force Charge is not configured
			return null;
		}

		final Double result;
		if (minCellVoltage <= this.forceChargeParams.startChargeBelowCellVoltage) {
			// Activate Force-Charge mode
			result = -1.;
			this.forceChargeActive = true;

		} else if (this.forceChargeActive) {
			if (minCellVoltage <= this.forceChargeParams.chargeBelowCellVoltage) {
				// Below 'startChargeBelowCellVoltage', but 'forceChargeActive' and above
				// 'chargeBelowCellVoltage', so keep force-charging
				result = -1.;

			} else if (minCellVoltage <= this.forceChargeParams.blockDischargeBelowCellVoltage) {
				// Finished Force-Charge mode - now in Block-Discharge mode
				result = 0.;

			} else {
				// Neither 'Force-Charge' nor 'Block-Discharge'. No limit.
				result = null;
				this.forceChargeActive = false;
			}

		} else {
			// Neither 'Force-Charge' nor 'Block-Discharge'. No limit.
			result = null;
		}

		return result;
	}
}
