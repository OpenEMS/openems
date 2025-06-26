package io.openems.edge.ess.core.power.solver;

import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;

import io.openems.edge.ess.core.power.data.LinearSolverUtil;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;

public class ConstraintSolver {

	/**
	 * Solves the problem with the given list of Constraints.
	 *
	 * @param coefficients the {@link Coefficients}
	 * @param constraints  a list of Constraints
	 * @return a solution
	 * @throws NoFeasibleSolutionException if not solvable
	 * @throws UnboundedSolutionException  if not solvable
	 */
	public static PointValuePair solve(Coefficients coefficients, List<Constraint> constraints)
			throws NoFeasibleSolutionException, UnboundedSolutionException {
		var linearConstraints = LinearSolverUtil.convertToLinearConstraints(coefficients, constraints);
		return LinearConstraintsSolver.solve(coefficients, linearConstraints);
	}

}
