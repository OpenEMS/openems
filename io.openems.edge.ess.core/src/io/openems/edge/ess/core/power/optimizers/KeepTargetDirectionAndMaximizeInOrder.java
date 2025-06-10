package io.openems.edge.ess.core.power.optimizers;

import static io.openems.edge.ess.core.power.data.ConstraintUtil.createSimpleConstraint;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;
import static io.openems.edge.ess.power.api.Relationship.LESS_OR_EQUALS;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MAXIMIZE;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MINIMIZE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.core.power.solver.CalculatePowerExtrema;
import io.openems.edge.ess.core.power.solver.ConstraintSolver;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.Pwr;

public class KeepTargetDirectionAndMaximizeInOrder {

	/**
	 * Tries to keep all Target Inverters in the right TargetDirection; then
	 * maximizes them in order.
	 *
	 * @param coefficients    the {@link Coefficients}
	 * @param allInverters    all {@link Inverter}s
	 * @param targetInverters the target {@link Inverter}s
	 * @param allConstraints  all active {@link Constraint}s
	 * @param targetDirection the {@link TargetDirection}
	 * @return a solution as {@link PointValuePair} or null
	 * @throws OpenemsException on error
	 */
	public static PointValuePair apply(Coefficients coefficients, List<Inverter> allInverters,
			List<Inverter> targetInverters, List<Constraint> allConstraints, TargetDirection targetDirection)
			throws OpenemsException {
		List<Constraint> constraints = new ArrayList<>(allConstraints);

		// Add Zero-Constraint for all Inverters that are not Target
		for (Inverter inv : allInverters) {
			if (!targetInverters.contains(inv)) {
				constraints.add(createSimpleConstraint(coefficients, //
						inv.toString() + ": is not a target inverter", //
						inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, EQUALS, 0));
				constraints.add(createSimpleConstraint(coefficients, //
						inv.toString() + ": is not a target inverter", //
						inv.getEssId(), inv.getPhase(), Pwr.REACTIVE, EQUALS, 0));
			}
		}

		var result = ConstraintSolver.solve(coefficients, constraints);

		var relationship = switch (targetDirection) {
		case CHARGE -> LESS_OR_EQUALS;
		case DISCHARGE -> GREATER_OR_EQUALS;
		case KEEP_ZERO -> EQUALS;
		};

		for (var inv : targetInverters) {
			// Create Constraint to force Ess positive/negative/zero according to
			// targetDirection
			result = addContraintIfProblemStillSolves(result, constraints, coefficients,
					createSimpleConstraint(coefficients, //
							inv.toString() + ": Force ActivePower " + targetDirection.name(), //
							inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, relationship, 0));
			result = addContraintIfProblemStillSolves(result, constraints, coefficients,
					createSimpleConstraint(coefficients, //
							inv.toString() + ": Force ReactivePower " + targetDirection.name(), //
							inv.getEssId(), inv.getPhase(), Pwr.REACTIVE, relationship, 0));
		}

		if (targetDirection == TargetDirection.KEEP_ZERO && result != null) {
			return result;
		}

		// Try maximizing all inverters in order in target direction
		for (var inv : targetInverters) {
			var goal = switch (targetDirection) {
			case CHARGE -> MINIMIZE;
			case DISCHARGE, KEEP_ZERO -> MAXIMIZE;
			};

			var activePowerTarget = CalculatePowerExtrema.from(coefficients, allConstraints, inv.getEssId(),
					inv.getPhase(), Pwr.ACTIVE, goal);
			result = addContraintIfProblemStillSolves(result, constraints, coefficients,
					createSimpleConstraint(coefficients, //
							inv.toString() + ": Set ActivePower " + goal.name() + " value", //
							inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, EQUALS, activePowerTarget));

			var reactivePowerTarget = CalculatePowerExtrema.from(coefficients, allConstraints, inv.getEssId(),
					inv.getPhase(), Pwr.REACTIVE, goal);
			result = addContraintIfProblemStillSolves(result, constraints, coefficients,
					createSimpleConstraint(coefficients, //
							inv.toString() + ": Set ReactivePower " + goal.name() + " value", //
							inv.getEssId(), inv.getPhase(), Pwr.REACTIVE, EQUALS, reactivePowerTarget));
		}

		return result;
	}

	/**
	 * Add Constraint only if the problem still solves with the Constraint.
	 *
	 * @param lastResult   the last result
	 * @param constraints  the list of {@link Constraint}s
	 * @param coefficients the {@link Coefficients}
	 * @param c            the {@link Constraint} to be added
	 * @return new solution on success; last result on error
	 */
	private static PointValuePair addContraintIfProblemStillSolves(PointValuePair lastResult,
			List<Constraint> constraints, Coefficients coefficients, Constraint c) {
		constraints.add(c);
		// Try to solve with Constraint
		try {
			return ConstraintSolver.solve(coefficients, constraints); // only if solving was successful
		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
			// solving failed
			constraints.remove(c);
			return lastResult;
		}
	}

}
