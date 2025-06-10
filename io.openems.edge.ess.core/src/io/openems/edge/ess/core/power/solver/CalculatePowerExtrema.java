package io.openems.edge.ess.core.power.solver;

import java.util.List;

import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.PivotSelectionRule;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.data.LinearSolverUtil;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class CalculatePowerExtrema {

	private static final Logger LOG = LoggerFactory.getLogger(CalculatePowerExtrema.class);

	/**
	 * Calculates the extrema under the current constraints for the given
	 * parameters.
	 *
	 * @param coefficients   the {@link Coefficients}
	 * @param allConstraints all active {@link Constraint}s
	 * @param essId          the ID of the {@link ManagedSymmetricEss}
	 * @param phase          the {@link Phase}
	 * @param pwr            the {@link Pwr}
	 * @param goal           the {@link GoalType}
	 * @return the extrema value; or 0 on error
	 */
	public static double from(Coefficients coefficients, List<Constraint> allConstraints, String essId, Phase phase,
			Pwr pwr, GoalType goal) {
		// prepare objective function
		int index;
		try {
			index = coefficients.of(essId, phase, pwr).getIndex();
		} catch (IllegalArgumentException | OpenemsException e) {
			LOG.error(e.getMessage());
			return 0d;
		}
		var cos = LinearSolverUtil.generateEmptyCoefficientsArray(coefficients.getNoOfCoefficients());
		cos[index] = 1;
		var objectiveFunction = new LinearObjectiveFunction(cos, 0);

		var constraints = new LinearConstraintSet(
				LinearSolverUtil.convertToLinearConstraints(coefficients, allConstraints));

		var solver = new SimplexSolver();
		try {
			var solution = solver.optimize(//
					objectiveFunction, //
					constraints, //
					goal, //
					PivotSelectionRule.BLAND);
			return solution.getPoint()[index];

		} catch (UnboundedSolutionException e) {
			LOG.warn("No Constraints for " + goal.name() + " [" + essId + "] phase [" + phase + "] pwr [" + pwr + "].");
			if (goal == GoalType.MAXIMIZE) {
				return Integer.MAX_VALUE;
			}
			return Integer.MIN_VALUE;

		} catch (NoFeasibleSolutionException e) {
			LOG.warn("Unable to " + goal.name() + " [" + essId + "] phase [" + phase + "] pwr [" + pwr
					+ "]. Setting it to zero.");
			return 0;
		}
	}
}
