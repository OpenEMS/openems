package io.openems.edge.ess.core.power.optimizers;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.PivotSelectionRule;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.core.power.data.LinearSolverUtil;
import io.openems.edge.ess.core.power.solver.ConstraintSolver;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class AddConstraintsForNotStrictlyDefinedCoefficients {

	/**
	 * Adds Constraints for not strictly defined Coefficients, e.g. if only a P <= X
	 * is defined, but no P = X.
	 *
	 * @param allInverters   a list of all {@link Inverter}s
	 * @param coefficients   the {@link Coefficients}
	 * @param allConstraints a list of all {@link Constraint}s
	 * @throws OpenemsException on error
	 */
	public static void apply(List<Inverter> allInverters, Coefficients coefficients, List<Constraint> allConstraints)
			throws OpenemsException {
		var constraints = new LinearConstraintSet(
				LinearSolverUtil.convertToLinearConstraints(coefficients, allConstraints));

		for (Pwr pwr : Pwr.values()) {
			// prepare objective function
			var cos = LinearSolverUtil.generateEmptyCoefficientsArray(coefficients.getNoOfCoefficients());
			for (Inverter inv : allInverters) {
				var c = coefficients.of(inv.getEssId(), inv.getPhase(), pwr);
				cos[c.getIndex()] = 1;
			}
			var objectiveFunction = new LinearObjectiveFunction(cos, 0);

			// get Max value over all relevant Coefficients
			double max;
			try {
				var solver = new SimplexSolver();
				var solution = solver.optimize(//
						objectiveFunction, //
						constraints, //
						GoalType.MAXIMIZE, //
						PivotSelectionRule.BLAND);
				max = 0d;
				for (Inverter inv : allInverters) {
					var c = coefficients.of(inv.getEssId(), inv.getPhase(), pwr);
					max += solution.getPoint()[c.getIndex()];
				}
			} catch (Exception e) {
				max = Double.MAX_VALUE;
			}
			// get Min value over all relevant Coefficients
			double min;
			try {
				var solver = new SimplexSolver();
				var solution = solver.optimize(//
						objectiveFunction, //
						constraints, //
						GoalType.MINIMIZE, //
						PivotSelectionRule.BLAND);
				min = 0d;
				for (Inverter inv : allInverters) {
					var c = coefficients.of(inv.getEssId(), inv.getPhase(), pwr);
					min += solution.getPoint()[c.getIndex()];
				}
			} catch (Exception e) {
				min = Double.MIN_VALUE;
			}

			if (min == max) {
				// Already strictly defined.
				continue;
			}

			// find the best value to set for EQUALS constraint
			double target;
			if (0 < max && 0 > min) {
				// set to zero
				target = 0;
			} else if (Math.abs(max) < Math.abs(min)) {
				// set to smallest distance from zero -> max
				target = max;
			} else {
				// set to smallest distance from zero -> min
				target = min;
			}

			// Constraint for sum of inverter power
			{
				var lcs = new LinearCoefficient[allInverters.size()];
				for (var i = 0; i < allInverters.size(); i++) {
					var inv = allInverters.get(i);
					var c = coefficients.of(inv.getEssId(), inv.getPhase(), pwr);
					lcs[i] = new LinearCoefficient(c, 1);
				}
				allConstraints.add(new Constraint("Strictly define " + pwr.name(), lcs, Relationship.EQUALS, target));
			}

			// Constraint for each inverter
			var newConstraints = new LinkedList<Constraint>();
			for (Inverter inv : allInverters) {
				var c = coefficients.of(inv.getEssId(), inv.getPhase(), pwr);
				newConstraints.add(new Constraint("Strictly define " + inv.toString() + " " + pwr.name(), //
						new LinearCoefficient[] { //
								new LinearCoefficient(c, 1) },
						Relationship.EQUALS, target / allInverters.size()));
			}

			// No new strictly defined constraints? Stop early
			if (newConstraints.isEmpty()) {
				return;
			}

			// Add all individual Constraints; and remove one after the other if solving
			// fails
			allConstraints.addAll(newConstraints);
			for (Constraint constraint : newConstraints) {
				try {
					ConstraintSolver.solve(coefficients, allConstraints);
					break;
				} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
					// Unable to add Constraint
					allConstraints.remove(constraint);
				}
			}
		}
	}

}
