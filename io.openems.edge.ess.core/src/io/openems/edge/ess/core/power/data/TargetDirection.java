package io.openems.edge.ess.core.power.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.exception.MathIllegalStateException;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.core.power.solver.ConstraintSolver;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public enum TargetDirection {

	KEEP_ZERO, CHARGE, DISCHARGE;

	/**
	 * Gets the TargetDirection of the Problem, i.e. whether it is a DISCHARGE or
	 * CHARGE problem.
	 *
	 * @param inverters                  list of {@link Inverter}s
	 * @param coefficients               the {@link Coefficients}
	 * @param constraintsForAllInverters {@link Constraint}s for all
	 *                                   {@link Inverter}s
	 * @return the {@link TargetDirection}
	 * @throws OpenemsException on error
	 */
	public static TargetDirection from(List<Inverter> inverters, Coefficients coefficients,
			List<Constraint> constraintsForAllInverters) throws OpenemsException {
		var constraints = constraintsForAllInverters;
		var equals0 = createSumOfPConstraint(inverters, coefficients, Relationship.EQUALS, 0);
		constraints.add(equals0);
		try {
			ConstraintSolver.solve(coefficients, constraints);
			return TargetDirection.KEEP_ZERO;
		} catch (MathIllegalStateException e) {
			constraints.remove(equals0);
			var greaterOrEquals0 = createSumOfPConstraint(inverters, coefficients, Relationship.GREATER_OR_EQUALS, 0);
			constraints.add(greaterOrEquals0);
			try {
				ConstraintSolver.solve(coefficients, constraints);
				return TargetDirection.DISCHARGE;
			} catch (MathIllegalStateException e2) {
				constraints.remove(greaterOrEquals0);
				var lessOrEquals0 = createSumOfPConstraint(inverters, coefficients, Relationship.LESS_OR_EQUALS, 0);
				constraints.add(lessOrEquals0);
				ConstraintSolver.solve(coefficients, constraints);
				return TargetDirection.CHARGE;
			}
		}
	}

	/**
	 * Creates Constraints for Sum of P.
	 *
	 * @param coefficients the {@link Coefficients}
	 * @param inverters    list of {@link Inverter}s
	 * @param relationship the {@link Relationship} between P and value
	 * @param value        the value
	 * @return the {@link Constraint}
	 * @throws OpenemsException on error
	 */
	private static Constraint createSumOfPConstraint(List<Inverter> inverters, Coefficients coefficients,
			Relationship relationship, int value) throws OpenemsException {
		List<LinearCoefficient> cos = new ArrayList<>();
		for (Inverter inverter : inverters) {
			cos.add(new LinearCoefficient(coefficients.of(inverter.getEssId(), inverter.getPhase(), Pwr.ACTIVE), 1));
		}
		return new Constraint("Sum of P = 0", cos, relationship, value);
	}
}
