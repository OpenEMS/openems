package io.openems.edge.ess.core.power.optimizers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.core.power.solver.ConstraintSolver;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class KeepAllEqual {

	/**
	 * Tries to distribute power equally between inverters.
	 *
	 * @param coefficients   the {@link Coefficients}
	 * @param allInverters   all {@link Inverter}s
	 * @param allConstraints all active {@link Constraint}s
	 * @return a solution or null
	 */
	public static PointValuePair apply(Coefficients coefficients, List<Inverter> allInverters,
			List<Constraint> allConstraints) {
		try {
			List<Constraint> constraints = new ArrayList<>(allConstraints);
			// Create weighted Constraint between first inverter and every other inverter
			var invA = allInverters.get(0);
			for (var j = 1; j < allInverters.size(); j++) {
				var invB = allInverters.get(j);
				constraints.add(new Constraint(
						invA.toString() + "|" + invB.toString() + ": distribute ActivePower equally",
						new LinearCoefficient[] {
								new LinearCoefficient(coefficients.of(invA.getEssId(), invA.getPhase(), Pwr.ACTIVE), 1),
								new LinearCoefficient(coefficients.of(invB.getEssId(), invB.getPhase(), Pwr.ACTIVE),
										-1) },
						Relationship.EQUALS, 0));
				constraints.add(new Constraint(
						invA.toString() + "|" + invB.toString() + ": distribute ReactivePower equally",
						new LinearCoefficient[] {
								new LinearCoefficient(coefficients.of(invA.getEssId(), invA.getPhase(), Pwr.REACTIVE),
										1),
								new LinearCoefficient(coefficients.of(invB.getEssId(), invB.getPhase(), Pwr.REACTIVE),
										-1) },
						Relationship.EQUALS, 0));
			}
			return ConstraintSolver.solve(coefficients, constraints);

		} catch (OpenemsException | NoFeasibleSolutionException | UnboundedSolutionException e) {
			return null;
		}
	}
}
