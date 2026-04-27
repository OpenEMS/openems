package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

public enum ReferenceCycleTarget {
	DISCHARGE_TO_ZERO {
		@Override
		public boolean isReached(int soc) {
			return soc <= 0;
		}
	},
	CHARGE_TO_HUNDRED {
		@Override
		public boolean isReached(int soc) {
			return soc >= 100;
		}
	};

	/**
	 * is the target SoC reached.
	 * @param soc state of charge
	 * @return true if is reached otherwise false
	 */
	public abstract boolean isReached(int soc);
}
