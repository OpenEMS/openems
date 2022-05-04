package io.openems.edge.ess.core.power.optimizers;

import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;

import io.openems.edge.ess.core.power.data.ConstraintUtil;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class KeepMaximumInvertersAtMaxEfficiency {

//	// Sorting inverters AND deciding CHARGE || DISCHARGE direction
//
//	// Inverters are by default sorted by weight descending. For DISCHARGE take list
//	// as it is; for CHARGE reverse it. This prefers high-weight inverters (e.g.
//	// high state-of-charge) on DISCHARGE and low-weight
//	// inverters (e.g. low state-of-charge) on CHARGE.
//	List<Inverter> sortedInverters;
//	private TargetDirection activeTargetDirection;
//	private List allInverters;
//	{
//		if (this.activeTargetDirection == TargetDirection.DISCHARGE) {
//			List<Inverter> allInverters = null;
//			sortedInverters = allInverters;
//		} else {
//			sortedInverters = Lists.reverse(allInverters);
//		}
//
//	}

	public static PointValuePair apply(Coefficients coefficients, List<Inverter> allInverters,
			List<Constraint> allConstraints) {
		for (var constraint : allConstraints) {
			System.out.println(constraint);
		}


			
		return null;
		// Only zero or one inverters available? No need to optimize.
//		if (coefficients.size() < 2) {
//			return coefficients;
//		}

		//-----------------------------------ReducedNumberOfUsedInverters.java----------------------------------------
		
		// Inverters are by default sorted by weight descending. For DISCHARGE take list
		// as it is; for CHARGE reverse it. This prefers high-weight inverters (e.g.
		// high state-of-charge) on DISCHARGE and low-weight
		// inverters (e.g. low state-of-charge) on CHARGE.
//		List<Inverter> sortedInverters;
//		if (this.activeTargetDirection == TargetDirection.DISCHARGE) {
//			sortedInverters = coefficients;
//		} else {
//			sortedInverters = Lists.reverse(coefficients);
//		}
//		return sortedInverters;
	}

//	public static PointValuePair apply(Coefficients coefficients, List<Inverter> allInverters,
//			List<Inverter> targetInverters, List<Constraint> allConstraints, TargetDirection targetDirection)
//			throws OpenemsException {
//		for (var constraint : allConstraints) {
//			for (var coefficient : constraint.getCoefficients()) {
//				coefficient.getCoefficient().
//			}
//		}
//
//		List<Constraint> constraints = new ArrayList<>(allConstraints);
//
//		// Add Zero-Constraint for all Inverters that are not Target
//		for (Inverter inv : allInverters) {
//			if (!targetInverters.contains(inv)) {
//				constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//						inv.toString() + ": is not a target inverter", //
//						inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, 0));
//				constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//						inv.toString() + ": is not a target inverter", //
//						inv.getEssId(), inv.getPhase(), Pwr.REACTIVE, Relationship.EQUALS, 0));
//			}
//		}
//
//		var result = ConstraintSolver.solve(coefficients, constraints);
//
//		var relationship = Relationship.EQUALS;
//		switch (targetDirection) {
//		case CHARGE:
//			relationship = Relationship.LESS_OR_EQUALS;
//			break;
//		case DISCHARGE:
//			relationship = Relationship.GREATER_OR_EQUALS;
//			break;
//		case KEEP_ZERO:
//			relationship = Relationship.EQUALS;
//			break;
//		}
//
//		for (Inverter inv : targetInverters) {
//			// Create Constraint to force Ess positive/negative/zero according to
//			// targetDirection
//			result = addContraintIfProblemStillSolves(result, constraints, coefficients,
//					ConstraintUtil.createSimpleConstraint(coefficients, //
//							inv.toString() + ": Force ActivePower " + targetDirection.name(), //
//							inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, relationship, 0));
//			result = addContraintIfProblemStillSolves(result, constraints, coefficients,
//					ConstraintUtil.createSimpleConstraint(coefficients, //
//							inv.toString() + ": Force ReactivePower " + targetDirection.name(), //
//							inv.getEssId(), inv.getPhase(), Pwr.REACTIVE, relationship, 0));
//		}
//
//		if (targetDirection == TargetDirection.KEEP_ZERO && result != null) {
//			return result;
//		}
//		return result;
//
///////////////////////////--------------------- Next part add here !
//
//	}
//
//	private static PointValuePair addContraintIfProblemStillSolves(PointValuePair result, List<Constraint> constraints,
//			Coefficients coefficients, Constraint createSimpleConstraint) {
//		// TODO Auto-generated method stub
//		return null;
//	}

}