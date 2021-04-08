package io.openems.edge.goodwe.common;

import io.openems.edge.goodwe.common.enums.EmsPowerMode;

public class ApplyPowerHandler {

	public static class Result {

		public final EmsPowerMode emsPowerMode;
		public final int emsPowerSet;

		public Result(EmsPowerMode emsPowerMode, int emsPowerSet) {
			this.emsPowerMode = emsPowerMode;
			this.emsPowerSet = emsPowerSet;
		}
	}

	public static ApplyPowerHandler.Result calculate(boolean readOnlyMode, int activePowerSetPoint) {
		if (readOnlyMode) {
			// Read-Only
			return new Result(EmsPowerMode.AUTO, 0);

		} else if (activePowerSetPoint > 0) {
			// Export to AC
			return new Result(EmsPowerMode.EXPORT_AC, activePowerSetPoint);

		} else {
			// Import from AC
			return new Result(EmsPowerMode.IMPORT_AC, activePowerSetPoint * -1);
		}

//		if (activePowerSetPoint > 0) {
//			// Set-Point is positive
//			if (activePowerSetPoint > pvProduction) {
//				// Set-Point is bigger than PV-Production
//				return new Result(EmsPowerMode.DISCHARGE_PV, activePowerSetPoint - pvProduction);
//
//			}
//		}
//
//		/*
//		 * Standard
//		 */
//		if (context.activePowerSetPoint > 0) {
//			// Set-Point is positive
//			if (context.activePowerSetPoint > context.pvProduction) {
//				context.setMode(EmsPowerMode.DISCHARGE_PV, context.activePowerSetPoint - context.pvProduction);
//			} else {
//				context.setMode(EmsPowerMode.CHARGE_BAT, context.pvProduction - context.activePowerSetPoint);
//			}
//
//		} else {
//			// Set-Point is negative or zero
//			context.setMode(EmsPowerMode.CHARGE_BAT, context.pvProduction - context.activePowerSetPoint);
//		}
//
//		/*
//		 * Empty
//		 */
//		// Set-Point is negative or zero -> 'charge' from pv production and grid
//		context.setMode(EmsPowerMode.CHARGE_BAT, Math.max(context.pvProduction - context.activePowerSetPoint, 0));
//
//		/*
//		 * Full
//		 */
//		if (context.activePowerSetPoint > 0) {
//			// Set-Point is positive -> take power either from pv or battery
//			if (context.activePowerSetPoint > context.pvProduction) {
//				context.setMode(EmsPowerMode.DISCHARGE_BAT, context.activePowerSetPoint - context.pvProduction);
//			} else {
//				context.setMode(EmsPowerMode.EXPORT_AC, context.activePowerSetPoint);
//			}
//		} else {
//			// Set-Point is negative or zero
//			context.setMode(EmsPowerMode.EXPORT_AC, 0);
//		}
//
//		return State.ET;
	}

}
