package io.openems.edge.battery.protection.force;

import java.time.Duration;

public class ForceDischarge extends AbstractForceChargeDischarge {

	/**
	 * Holds parameters for 'Force Discharge' mode.
	 */
	public static class Params {
		private final int startDischargeAboveCellVoltage;
		private final int dischargeAboveCellVoltage;
		private final int blockChargeAboveCellVoltage;

		public Params(int startDischargeAboveCellVoltage, int dischargeAboveCellVoltage,
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

	private final Params params;

	/**
	 * Builds a {@link ForceDischarge} instance from {@link ForceDischarge.Params}.
	 *
	 * @param params the parameter object
	 * @return a {@link ForceDischarge} instance
	 */
	public static ForceDischarge from(Params params) {
		if (params == null) {
			return null;
		}
		return new ForceDischarge(params);
	}

	private ForceDischarge(Params params) {
		this.params = params;
	}

	@Override
	protected State handleUndefinedState(int minCellVoltage, int maxCellVoltage) {
		if (maxCellVoltage >= this.params.startDischargeAboveCellVoltage) {
			return State.WAIT_FOR_FORCE_MODE;

		} else {
			return State.UNDEFINED;
		}
	}

	@Override
	protected State handleWaitForForceModeState(int minCellVoltage, int maxCellVoltage, Duration durationSinceStart) {
		if (maxCellVoltage < this.params.startDischargeAboveCellVoltage) {
			return State.UNDEFINED;

		} else if (durationSinceStart.getSeconds() < WAIT_FOR_FORCE_MODE_SECONDS) {
			return State.WAIT_FOR_FORCE_MODE;

		} else {
			return State.FORCE_MODE;
		}
	}

	@Override
	protected State handleForceModeState(int minCellVoltage, int maxCellVoltage) {
		if (maxCellVoltage >= this.params.dischargeAboveCellVoltage) {
			return State.FORCE_MODE;

		} else {
			return State.BLOCK_MODE;
		}
	}

	@Override
	protected State handleBlockModeState(int minCellVoltage, int maxCellVoltage) {
		if (maxCellVoltage >= this.params.startDischargeAboveCellVoltage) {
			return State.FORCE_MODE;

		} else if (maxCellVoltage >= this.params.blockChargeAboveCellVoltage) {
			return State.BLOCK_MODE;

		} else {
			return State.UNDEFINED;
		}
	}
}
