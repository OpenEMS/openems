package io.openems.edge.goodwe.common;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.ess.api.ApplyPowerContext;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;

public class ApplyPowerHandler {

	/**
	 * Apply the desired Active-Power Set-Point by setting the appropriate
	 * EMS_POWER_SET and EMS_POWER_MODE settings.
	 * 
	 * @param goodWe         the GoodWe - either Battery-Inverter or ESS
	 * @param readOnlyMode   is Read-Only-Mode activated?
	 * @param setActivePower the Active-Power Set-Point
	 * @param context        the {@link ApplyPowerContext}
	 * @throws OpenemsNamedException on error
	 */
	public static void apply(AbstractGoodWe goodWe, boolean readOnlyMode, int setActivePower, ApplyPowerContext context)
			throws OpenemsNamedException {
		ApplyPowerHandler.Result apply = calculate(goodWe, readOnlyMode, setActivePower, context);


		IntegerWriteChannel emsPowerSetChannel = goodWe.channel(GoodWe.ChannelId.EMS_POWER_SET);
		emsPowerSetChannel.setNextWriteValue(apply.emsPowerSet);
		EnumWriteChannel emsPowerModeChannel = goodWe.channel(GoodWe.ChannelId.EMS_POWER_MODE);
		emsPowerModeChannel.setNextWriteValue(apply.emsPowerMode);
	}

	private static class Result {

		public final EmsPowerMode emsPowerMode;
		public final int emsPowerSet;

		public Result(EmsPowerMode emsPowerMode, int emsPowerSet) {
			this.emsPowerMode = emsPowerMode;
			this.emsPowerSet = emsPowerSet;
		}
	}

	private static ApplyPowerHandler.Result calculate(AbstractGoodWe goodWe, boolean readOnlyMode,
			int activePowerSetPoint, ApplyPowerContext context) {
		if (readOnlyMode) {
			// Read-Only
			return new Result(EmsPowerMode.AUTO, 0);
		}

		if (activePowerSetPoint > 0) {
			// Export to AC
//			Integer pvProduction = goodWe.calculatePvProduction();
//			if (pvProduction != null && pvProduction > 0 && soc > 98) {
//				System.out.println("DISCHARGE_PV [0] PV [" + goodWe.calculatePvProduction() + "] Set-Point ["
//						+ activePowerSetPoint + "]");
//				return new Result(EmsPowerMode.DISCHARGE_PV, 0);
//			} else {
			System.out.println("EXPORT_AC [" + activePowerSetPoint + "] PV [" + goodWe.calculatePvProduction()
					+ "] Set-Point [" + activePowerSetPoint + "]");
			return new Result(EmsPowerMode.EXPORT_AC, activePowerSetPoint);
//			}

//			// Is there any Max-Power Constraint? (e.g. curtail grid-feed-in power)

//			Integer maxPowerConstraint = null;
//			for (Constraint constraint : context.getConstraints()) {
//				if (constraint.getRelationship() == Relationship.LESS_OR_EQUALS && constraint.getValue().isPresent()) {
//					maxPowerConstraint = TypeUtils.min((int) Math.round(constraint.getValue().get()), maxPowerConstraint);
//					System.out.println("maxPowerConstraint [" + maxPowerConstraint + "] Constraint: " + constraint);
//				}
//			}
//			Integer maxPowerConstraint = null;
//			if (context.getConstraints().size() > 1) {
//				for (Constraint constraint : context.getConstraints()) {
//					if (constraint.getRelationship() == Relationship.LESS_OR_EQUALS && constraint.getValue().isPresent()
//					// Do not consider the default 'Allowed Discharge' Constraint
//							&& !constraint.getDescription().endsWith("Allowed Discharge")) {
//						maxPowerConstraint = TypeUtils.min((int) Math.round(constraint.getValue().get()),
//								maxPowerConstraint);
//						System.out.println("maxPowerConstraint [" + maxPowerConstraint + "] Constraint: " + constraint);
//					}
//				}
//			}

			// Szenarien:
			// Tagsueber: Überschusseinspeisung (Set-Point < PV)
			// Tagsueber: Überschusseinspeisung reicht nicht aus um Lasten zu versorgen
			// (Set-Point > PV)
			// Nachts: Entladung (Set-Point > PV)
//			if (maxPowerConstraint == null) {
//				// No Limit
//				if (activePowerSetPoint > goodWe.calculatePvProduction()) {
//					// Discharge
//					System.out.println("Discharge EXPORT_AC [" + activePowerSetPoint + "] PV ["
//							+ goodWe.calculatePvProduction() + "] Set-Point [" + activePowerSetPoint + "]");
//				} else {
//					//
//				}
//			}
//
//			if (maxPowerConstraint != null) {
//				if (maxPowerConstraint > goodWe.calculatePvProduction()) {
//					// Curtail
//					System.out.println("Curtail EXPORT_AC [" + Math.min(activePowerSetPoint, maxPowerConstraint)
//							+ "] PV [" + goodWe.calculatePvProduction() + "] Set-Point [" + activePowerSetPoint + "]");
//				}
//			}
//
//			if (maxPowerConstraint == null || goodWe.calculatePvProduction() < activePowerSetPoint) {
//				// Apply Constraint or Active-Power-SetPoint
//
//				return new Result(EmsPowerMode.EXPORT_AC, Math.min(activePowerSetPoint, maxPowerConstraint));
//			} else if (maxPowerConstraint != null) {
//				System.out.println("EXPORT_AC [" + Math.min(activePowerSetPoint, maxPowerConstraint) + "] PV ["
//						+ goodWe.calculatePvProduction() + "] Set-Poinet [" + activePowerSetPoint + "]");
//				return new Result(EmsPowerMode.EXPORT_AC, Math.min(activePowerSetPoint, maxPowerConstraint));
//
//			} else {
//				System.out.println("CHARGE_BAT [0]");
//				return new Result(EmsPowerMode.CHARGE_BAT, 0);
//			}

		} else {
			// Import from AC
			return new Result(EmsPowerMode.IMPORT_AC, activePowerSetPoint * -1);
		}
	}
}
