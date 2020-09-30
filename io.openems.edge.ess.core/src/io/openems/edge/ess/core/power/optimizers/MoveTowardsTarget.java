package io.openems.edge.ess.core.power.optimizers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.core.power.data.ConstraintUtil;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.core.power.solver.ConstraintSolver;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class MoveTowardsTarget {

	private static final double LEARNING_RATE = 0.1;

	/**
	 * Tries to adjust the weights used in last applyPower() towards the target
	 * weights using a learning rate. If this fails it tries to start from the
	 * target weights towards a given existing solution.
	 *
	 * @param coefficients    the {@link Coefficients}
	 * @param allInverters    all {@link Inverter}s
	 * @param targetInverters the target {@link Inverter}s
	 * @param allConstraints  all active {@link Constraint}s
	 * @param targetDirection the {@link TargetDirection}
	 * @return a solution as {@link PointValuePair} or null
	 * @throws OpenemsException on error
	 */
	public static PointValuePair apply(Coefficients coefficients, TargetDirection targetDirection,
			List<Inverter> allInverters, List<Inverter> targetInverters, List<Constraint> allConstraints)
			throws OpenemsException {
		// find maxLastActive + maxWeight
		int maxLastActivePower = 0;
		int sumWeights = 0;
		for (Inverter inv : allInverters) {
			maxLastActivePower = Math.max(Math.abs(inv.getLastActivePower()), maxLastActivePower);
			sumWeights += Math.abs(inv.getWeight());
		}

		// create map with normalized last weights
		Map<Inverter, Double> lastWeights = new HashMap<>();
		if (maxLastActivePower == 0) {
			// all lastActivePower are zero -> put weights on targetInverters
			for (Inverter inv : allInverters) {
				if (targetInverters.contains(inv)) {
					lastWeights.put(inv, Double.valueOf(100));
				} else {
					lastWeights.put(inv, Double.valueOf(0));
				}
			}
		} else {
			// at least one weight is != zero -> start normal weighting
			double normalizeFactor = 100d / maxLastActivePower;
			for (Inverter inv : allInverters) {
				lastWeights.put(inv, Math.abs(inv.getLastActivePower() * normalizeFactor));
			}
		}

		// create map with target weights
		Map<Inverter, Integer> targetWeights = new HashMap<>();
		for (Inverter inv : allInverters) {
			if (targetInverters.contains(inv)) {
				switch (targetDirection) {
				case CHARGE:
				case KEEP_ZERO:
					// Invert weights for CHARGE, i.e. give higher weight to low state-of-charge
					// inverters
					targetWeights.put(inv, (100 - (inv.getWeight() * 100 / sumWeights)));
					break;
				case DISCHARGE:
					targetWeights.put(inv, inv.getWeight() * 100 / sumWeights);
					break;
				}
			} else {
				targetWeights.put(inv, 0);
			}
		}

		// create map with learning rates
		Map<Inverter, Double> learningRates = new HashMap<>();
		for (Inverter inv : allInverters) {
			learningRates.put(inv, (targetWeights.get(inv) - lastWeights.get(inv)) * LEARNING_RATE);
		}

		// create map with next weights (= last weights + learningRates)
		Map<Inverter, Double> nextWeights = new HashMap<>();
		for (Inverter inv : allInverters) {
			nextWeights.put(inv, lastWeights.get(inv) + learningRates.get(inv));
		}

		// adjust towards target weight till Problem solves
		for (double i = 0; i < 1 - LEARNING_RATE; i += LEARNING_RATE) {
			List<Constraint> constraints = new ArrayList<>(allConstraints);
			List<Inverter> inverters = new ArrayList<>(allInverters);

			// set EQUALS ZERO constraint if next weight is zero + remove Inverter from
			// inverters
			for (Entry<Inverter, Double> entry : nextWeights.entrySet()) {
				if (entry.getValue() == 0) { // might fail... compare double to zero
					Inverter inv = entry.getKey();
					Constraint c = ConstraintUtil.createSimpleConstraint(coefficients, //
							inv.toString() + ": next weight = 0", //
							inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, 0);
					constraints.add(c);
					inverters.remove(inv);
				}
			}

			// no inverters left? -> nothing to optimize
			if (inverters.isEmpty()) {
				return null;
			}

			// Create weighted Constraint between first inverter and every other inverter
			Inverter invA = inverters.get(0);
			for (int j = 1; j < inverters.size(); j++) {
				Inverter invB = inverters.get(j);
				Constraint c = new Constraint(invA.toString() + "|" + invB.toString() + ": Weight",
						new LinearCoefficient[] {
								new LinearCoefficient(coefficients.of(invA.getEssId(), invA.getPhase(), Pwr.ACTIVE),
										nextWeights.get(invB)),
								new LinearCoefficient(coefficients.of(invB.getEssId(), invB.getPhase(), Pwr.ACTIVE),
										nextWeights.get(invA) * -1) },
						Relationship.EQUALS, 0);
				constraints.add(c);
			}

			try {
				PointValuePair solution = ConstraintSolver.solve(coefficients, constraints);
				return solution;
			} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
				// Adjust next weights
				for (Entry<Inverter, Double> entry : nextWeights.entrySet()) {
					entry.setValue(entry.getValue() + learningRates.get(entry.getKey()));
				}
			}
		}

		// TODO if we reached here, we should try to approach existingWeights in the
		// same way as above. This could still improve existingSolution.
		return null;
	}
}
