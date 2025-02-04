package io.openems.edge.evcs.api;

public enum PhaseRotation {
	/**
	 * EVCS uses standard wiring.
	 * 
	 * <ul>
	 * <li>EVCS L1 is connected to Grid L1
	 * <li>EVCS L2 is connected to Grid L2
	 * <li>EVCS L3 is connected to Grid L3
	 * </ul>
	 */
	L1_L2_L3,

	/**
	 * EVCS uses rotated wiring.
	 * 
	 * <ul>
	 * <li>EVCS L1 is connected to Grid L2
	 * <li>EVCS L2 is connected to Grid L3
	 * <li>EVCS L3 is connected to Grid L1
	 * </ul>
	 */
	L2_L3_L1,

	/**
	 * EVCS uses rotated wiring.
	 * 
	 * <ul>
	 * <li>EVCS L1 is connected to Grid L3
	 * <li>EVCS L2 is connected to Grid L1
	 * <li>EVCS L3 is connected to Grid L2
	 * </ul>
	 */
	L3_L1_L2;

	public record RotatedPhases(//
			Integer voltageL1, int currentL1, Integer activePowerL1, //
			Integer voltageL2, int currentL2, Integer activePowerL2, //
			Integer voltageL3, int currentL3, Integer activePowerL3) {

		/**
		 * Rotate phases for voltage, current and active power.
		 * 
		 * @param phaseRotation the {@link PhaseRotation}
		 * @param voltageL1     the voltage on L1
		 * @param currentL1     the current on L1
		 * @param activePowerL1 the active power on L1
		 * @param voltageL2     the voltage on L2
		 * @param currentL2     the current on L2
		 * @param activePowerL2 the active power on L2
		 * @param voltageL3     the voltage on L3
		 * @param currentL3     the current on L3
		 * @param activePowerL3 the active power on L3
		 * @return {@link RotatedPhases}
		 */
		public static RotatedPhases from(PhaseRotation phaseRotation, //
				Integer voltageL1, int currentL1, Integer activePowerL1, //
				Integer voltageL2, int currentL2, Integer activePowerL2, //
				Integer voltageL3, int currentL3, Integer activePowerL3) {
			return switch (phaseRotation) {
			case L1_L2_L3 -> new RotatedPhases(//
					voltageL1, currentL1, activePowerL1, //
					voltageL2, currentL2, activePowerL2, //
					voltageL3, currentL3, activePowerL3);
			case L2_L3_L1 -> new RotatedPhases(//
					voltageL3, currentL3, activePowerL3, //
					voltageL1, currentL1, activePowerL1, //
					voltageL2, currentL2, activePowerL2);
			case L3_L1_L2 -> new RotatedPhases(//
					voltageL2, currentL2, activePowerL2, //
					voltageL3, currentL3, activePowerL3, //
					voltageL1, currentL1, activePowerL1);
			};
		}
	}
}
