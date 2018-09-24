package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.PivotSelectionRule;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.PowerException.Type;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class Solver {

	private final Logger log = LoggerFactory.getLogger(Solver.class);

	private final Data data;

	private boolean debugMode = PowerComponent.DEFAULT_DEBUG_MODE;

	private Consumer<Boolean> onSolved = (wasSolved) -> {
	};

	public Solver(Data data) {
		this.data = data;

	}

	public void onSolved(Consumer<Boolean> onSolved) {
		this.onSolved = onSolved;
	}

	/**
	 * Tests wheter the Problem is solvable under the current Constraints.
	 * 
	 * @return
	 * @throws PowerException
	 * 
	 */
	public void isSolvableOrError() throws PowerException {
		try {
			this.solveWithAllConstraints();
		} catch (NoFeasibleSolutionException e) {
			throw new PowerException(Type.NO_FEASIBLE_SOLUTION);
		} catch (UnboundedSolutionException e) {
			throw new PowerException(Type.UNBOUNDED_SOLUTION);
		}
	}

	/**
	 * Tests wheter the Problem is solvable under the current Constraints.
	 * 
	 * @return
	 */
	public boolean isSolvable() {
		try {
			this.solveWithAllConstraints();
			return true;
		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
			return false;
		}
	}

	public int getActivePowerExtrema(ManagedSymmetricEss ess, Phase phase, Pwr pwr, GoalType goal) {
		// prepare objective function
		int index = this.data.getCoefficient(ess, phase, pwr).getIndex();
		double[] cos = Solver.getEmptyCoefficients(data);
		cos[index] = 1;
		LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(cos, 0);

		// get Constraints
		List<Constraint> allConstraints = this.data.getConstraintsForAllInverters();
		LinearConstraintSet constraints = new LinearConstraintSet(
				Solver.convertToLinearConstraints(this.data, allConstraints));

		SimplexSolver solver = new SimplexSolver();
		try {
			PointValuePair solution = solver.optimize( //
					objectiveFunction, //
					constraints, //
					goal, //
					PivotSelectionRule.BLAND);
			double result = solution.getPoint()[index];
			return (int) result;
		} catch (UnboundedSolutionException e) {
			if (this.debugMode) {
				PowerComponent.debugLogConstraints(this.log, "No Constraints for " + goal.name() + " Active Power.",
						allConstraints);
			} else {
				this.log.warn("No Constraints for " + goal.name() + " Active Power.");
			}

			if (goal == GoalType.MAXIMIZE) {
				return Integer.MAX_VALUE;
			} else {
				return Integer.MIN_VALUE;
			}
		} catch (NoFeasibleSolutionException e) {
			if (this.debugMode) {
				PowerComponent.debugLogConstraints(this.log,
						"Unable to " + goal.name() + " Active Power. Setting it to zero.", allConstraints);
			} else {
				this.log.warn("Unable to " + goal.name() + " Active Power. Setting it to zero.");
			}

			return 0;
		}
	}

	public void solve() {
		// No Inverters -> nothing to do
		if (this.data.getInverters().isEmpty()) {
			return;
		}

		// Add Strict constraints if required
		// this.addConstraintsForNotStrictlyDefinedCoefficients();

		// Check if the Problem is solvable at all.
		List<Constraint> allConstraints = this.data.getConstraintsForAllInverters();

		Map<Inverter, PowerTuple> finalSolution;
		List<Inverter> allInverters = data.getInverters();
		try {
			PointValuePair solution = this.solveWithConstraints(allConstraints);
			// announce success
			this.onSolved.accept(true);

			// Evaluates whether it is a CHARGE or DISCHARGE problem.
			TargetDirection targetDirection = this.getTargetDirection();

			// Gets the target-Inverters, i.e. the Inverters that are minimally required to
			// solve the Problem.
			List<Inverter> targetInverters = this.getTargetInverters(data.getInverters(), targetDirection);

			// Applies weights to move slowly towards only using Target-Inverters.
			solution = this.applyWeights(targetDirection, allInverters, targetInverters, solution, allConstraints);

			// Apply precisions of Inverters
			finalSolution = this.applyInverterPrecisions(allInverters, solution, targetDirection);

		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
			// announce failure
			this.onSolved.accept(false);

			if (this.debugMode) {
				PowerComponent.debugLogConstraints(this.log,
						"[" + e.getMessage() + "] Unable to solve under the following constraints:", allConstraints);
			} else {
				this.log.warn("Power-Solver: Unable to solve under constraints!");
			}

			// get Zero-Solution as fallback
			finalSolution = this.getZeroSolution(allInverters);
		}

		// Apply final Solution to Inverters
		this.applySolution(finalSolution);
	}

	/**
	 * TODO Adds Constraints for not strictly defined Coefficients, e.g. if only a P <= X
	 * is defined, but no P = X.
	 */
//	private void addConstraintsForNotStrictlyDefinedCoefficients() {
//		int maxP = this.getActivePowerExtrema(GoalType.MAXIMIZE);
//		int minP = this.getActivePowerExtrema(GoalType.MINIMIZE);
//		if (minP == maxP) {
//			// Already strictly defined.
//			return;
//		}
//
//		int targetP;
//		if (0 < maxP && 0 > minP) {
//			targetP = 0;
//		} else if (Math.abs(maxP) < Math.abs(minP)) {
//			targetP = maxP;
//		} else {
//			targetP = minP;
//		}
//		List<LinearCoefficient> cos = new ArrayList<>();
//		this.data.getCoefficients().getAll().forEach(c -> {
//			if (!(c.getEss() instanceof MetaEss) // no MetaEss
//					&& c.getPhase() != Phase.ALL // only ALL phase
//					&& c.getPwr() == Pwr.ACTIVE) { // only ACTIVE power
//				cos.add(new LinearCoefficient(c, 1));
//			}
//		});
//		this.data.addConstraint(new Constraint("Strictly define Active Power", cos, Relationship.EQUALS, targetP));
//	}

	/**
	 * Tries to adjust the weights used in last applyPower() towards the target
	 * weights using a learning rate. If this fails it tries to start from the
	 * target weights towards a given existing solution. If everything fails it will
	 * eventually return the existing solution.
	 * 
	 * @param allInverters
	 * @param targetInverters
	 * @param existingSolution
	 * @param allConstraints
	 * @return
	 */
	private PointValuePair applyWeights(TargetDirection targetDirection, List<Inverter> allInverters,
			List<Inverter> targetInverters, PointValuePair existingSolution, List<Constraint> allConstraints) {
		// find weight with max distance from zero
		int maxDistance = 0;
		for (Inverter inverter : allInverters) {
			maxDistance = Math.max(Math.abs(inverter.lastP), maxDistance);
		}

		// create map with normalized last weights
		Map<Inverter, Double> lastWeights = new HashMap<>();
		if (maxDistance == 0) {
			// all lastP are zero -> put weights on targetInverters
			for (Inverter inverter : allInverters) {
				if (targetInverters.contains(inverter)) {
					lastWeights.put(inverter, Double.valueOf(100));
				} else {
					lastWeights.put(inverter, Double.valueOf(0));
				}
			}
		} else {
			// at least one weight is != zero -> start normal weighting
			double normalizeFactor = 100d / maxDistance;
			for (Inverter inverter : allInverters) {
				lastWeights.put(inverter, inverter.lastP * normalizeFactor);
			}
		}

		// create map with target weights
		Map<Inverter, Integer> targetWeights = new HashMap<>();
		for (Inverter inverter : allInverters) {
			if (targetInverters.contains(inverter)) {
				switch (targetDirection) {
				case CHARGE:
					// Invert weights for CHARGE, i.e. give higher weight to low state-of-charge
					// inverters
					targetWeights.put(inverter, 100 - inverter.weight);
					break;
				case DISCHARGE:
					targetWeights.put(inverter, inverter.weight);
					break;
				}
			} else {
				targetWeights.put(inverter, 0);
			}
		}

		// create map with learning rates
		Map<Inverter, Double> learningRates = new HashMap<>();
		for (Inverter inverter : allInverters) {
			learningRates.put(inverter, (targetWeights.get(inverter) - lastWeights.get(inverter)) * LEARNING_RATE);
		}

		// create map with next weights (= last weights + learningRates)
		Map<Inverter, Double> nextWeights = new HashMap<>();
		for (Inverter inverter : allInverters) {
			nextWeights.put(inverter, lastWeights.get(inverter) + learningRates.get(inverter));
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
					Constraint c = this.data.createSimpleConstraint(inv.toString() + ": next weight = 0", inv.getEss(),
							inv.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, 0);
					constraints.add(c);
					inverters.remove(inv);
				}
			}

			// no inverters left? -> nothing to optimize
			if (inverters.isEmpty()) {
				return existingSolution;
			}

			// Create weighted Constraint between first inverter and every other inverter
			Inverter invA = inverters.get(0);
			for (int j = 1; j < inverters.size(); j++) {
				Inverter invB = inverters.get(j);
				Constraint c = new Constraint(invA.toString() + "|" + invB.toString() + ": Weight",
						new LinearCoefficient[] {
								new LinearCoefficient(
										this.data.getCoefficient(invA.getEss(), invA.getPhase(), Pwr.ACTIVE),
										nextWeights.get(invB)),
								new LinearCoefficient(
										this.data.getCoefficient(invB.getEss(), invB.getPhase(), Pwr.ACTIVE),
										nextWeights.get(invA) * -1) },
						Relationship.EQUALS, 0);
				constraints.add(c);
			}

			try {
				return this.solveWithConstraints(constraints);
			} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
				// log.warn("[" + Math.round(i / LEARNING_RATE) + "] Unable to solve with next
				// weights. Next try!");

				// Adjust next weights
				for (Entry<Inverter, Double> entry : nextWeights.entrySet()) {
					entry.setValue(entry.getValue() + learningRates.get(entry.getKey()));
				}
			}
		}

		// TODO if we reached here, we should try to approach existingWeights in the
		// same way as above. This could still improve existingSolution.

		return existingSolution;
	}

	private final static double LEARNING_RATE = 0.1;

	/**
	 * Rounds each solution value to the Inverter precision; following this logic:
	 *
	 * On Discharge (Power > 0)
	 *
	 * <ul>
	 * <li>if SoC > 50 %: round up (more discharge)
	 * <li>if SoC < 50 %: round down (less discharge)
	 * </ul>
	 *
	 * On Charge (Power < 0)
	 *
	 * <ul>
	 * <li>if SoC > 50 %: round down (less charge)
	 * <li>if SoC < 50 %: round up (more discharge)
	 * </ul>
	 *
	 * @param allInverters
	 * @param solution
	 * @param targetDirection
	 * @return
	 */
	// TODO: round value of one inverter, apply constraint, repeat... to further
	// optimize this
	private Map<Inverter, PowerTuple> applyInverterPrecisions(List<Inverter> allInverters, PointValuePair solution,
			TargetDirection targetDirection) {
		Map<Inverter, PowerTuple> result = new HashMap<>();
		double[] point = solution.getPoint();
		for (Inverter inv : allInverters) {
			Round round = Round.TOWARDS_ZERO;
			ManagedSymmetricEss ess = inv.getEss();
			int soc = ess.getSoc().value().orElse(0);
			int precision = inv.getEss().getPowerPrecision();
			PowerTuple powerTuple = new PowerTuple();
			for (Pwr pwr : Pwr.values()) {
				Coefficient c = this.data.getCoefficient(inv.getEss(), inv.getPhase(), pwr);
				double value = point[c.getIndex()];
				if (value > 0 && soc > 50 || value < 0 && soc < 50) {
					round = Round.AWAY_FROM_ZERO;
				}
				int roundedValue = IntUtils.roundToPrecision((float) value, round, precision);
				if (roundedValue == -1 || roundedValue == 1) {
					roundedValue = 0; // avoid unnecessary power settings on rounding 0.xxx to 1
				}
				powerTuple.setValue(pwr, roundedValue);
			}
			result.put(inv, powerTuple);
		}
		return result;
	}

	private Map<Inverter, PowerTuple> getZeroSolution(List<Inverter> allInverters) {
		Map<Inverter, PowerTuple> result = new HashMap<>();
		for (Inverter inv : allInverters) {
			PowerTuple powerTuple = new PowerTuple();
			for (Pwr pwr : Pwr.values()) {
				powerTuple.setValue(pwr, 0);
			}
			result.put(inv, powerTuple);
		}
		return result;
	}

	/**
	 * Finds the target Inverters, i.e. the Inverters that are minimally required to
	 * fulfill all Constraints.
	 * 
	 * This method therefore tries to remove inverters in order until there is no
	 * solution anymore. It than re-adds that inverter and returns the solution.
	 * 
	 * @param allInverters
	 * @param targetDirection
	 * @return
	 */
	private List<Inverter> getTargetInverters(List<Inverter> allInverters, TargetDirection targetDirection) {
		List<Inverter> disabledInverters = new ArrayList<>();

		// For CHARGE take list as it is; for DISCHARGE reverse it. This prefers
		// high-weight inverters (e.g. high state-of-charge) on DISCHARGE and low-weight
		// inverters (e.g. low state-of-charge) on CHARGE.
		List<Inverter> allInvertersTargetDirection;
		if (targetDirection == TargetDirection.DISCHARGE) {
			allInvertersTargetDirection = Lists.reverse(allInverters);
		} else {
			allInvertersTargetDirection = allInverters;
		}

		for (Inverter inverter : allInvertersTargetDirection) {
			disabledInverters.add(inverter);
			try {
				this.solveWithDisabledInverters(disabledInverters);
			} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
				disabledInverters.remove(inverter);
				break;
			}
		}
		// build result
		List<Inverter> result = new ArrayList<>(allInverters);
		for (Inverter disabledInverter : disabledInverters) {
			result.remove(disabledInverter);
		}
		return result;
	}

	/**
	 * Solves the problem, while setting all DisabledInverters to EQUALS zero.
	 * 
	 * @param disabledInverters
	 * @return
	 * @throws NoFeasibleSolutionException
	 * @throws UnboundedSolutionException
	 */
	private PointValuePair solveWithDisabledInverters(List<Inverter> disabledInverters)
			throws NoFeasibleSolutionException, UnboundedSolutionException {
		List<Constraint> constraints = this.data.getConstraintsWithoutDisabledInverters(disabledInverters);
		return this.solveWithConstraints(constraints);
	}

	/**
	 * Solves the problem with the given list of Constraints.
	 * 
	 * @param constraints
	 * @return
	 * @throws NoFeasibleSolutionException
	 * @throws UnboundedSolutionException
	 */
	private PointValuePair solveWithConstraints(List<Constraint> constraints)
			throws NoFeasibleSolutionException, UnboundedSolutionException {
		List<LinearConstraint> linearConstraints = Solver.convertToLinearConstraints(this.data, constraints);
		return this.solveWithLinearConstraints(linearConstraints);
	}

	/**
	 * Solves the problem with the given list of LinearConstraints.
	 * 
	 * @param constraints
	 * @return
	 * @throws NoFeasibleSolutionException
	 * @throws UnboundedSolutionException
	 */
	private PointValuePair solveWithLinearConstraints(List<LinearConstraint> constraints)
			throws NoFeasibleSolutionException, UnboundedSolutionException {
		LinearObjectiveFunction objectiveFunction = Solver.getDefaultObjectiveFunction(this.data);

		SimplexSolver solver = new SimplexSolver();
		return solver.optimize( //
				objectiveFunction, //
				new LinearConstraintSet(constraints), //
				GoalType.MINIMIZE, //
				PivotSelectionRule.BLAND);
	}

	/**
	 * Solves the problem. Applies all set Constraints.
	 * 
	 * @return
	 * @throws NoFeasibleSolutionException
	 * @throws UnboundedSolutionException
	 */
	private PointValuePair solveWithAllConstraints() throws NoFeasibleSolutionException, UnboundedSolutionException {
		List<Constraint> allConstraints = this.data.getConstraintsForAllInverters();
		return this.solveWithConstraints(allConstraints);
	}

	public enum TargetDirection {
		CHARGE, DISCHARGE;
	}

	/**
	 * Gets the TargetDirection of the Problem, i.e. whether it is a DISCHARGE or
	 * CHARGE problem.
	 * 
	 * @return
	 */
	public TargetDirection getTargetDirection() {
		List<Constraint> constraints = this.data.getConstraintsForAllInverters();
		constraints.addAll(this.data.createPGreaterThan0Constraints());
		try {
			this.solveWithConstraints(constraints);
			return TargetDirection.DISCHARGE;
		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
			return TargetDirection.CHARGE;
		}
	}

	/**
	 * Send the final solution to each Inverter
	 * 
	 * @param finalSolution
	 */
	private void applySolution(Map<Inverter, PowerTuple> finalSolution) {
		// store last value inside Inverter
		finalSolution.forEach((inv, powerTuple) -> {
			inv.storeLastPower(powerTuple.getActivePower(), powerTuple.getReactivePower());
		});

		for (ManagedSymmetricEss ess : this.data.getEsss()) {
			if (ess instanceof MetaEss) {
				// ignore MetaEss
				continue;
			}
			PowerTuple inv = null;
			PowerTuple invL1 = null;
			PowerTuple invL2 = null;
			PowerTuple invL3 = null;
			for (Entry<Inverter, PowerTuple> entry : finalSolution.entrySet()) {
				Inverter i = entry.getKey();
				if (Objects.equals(ess, i.getEss())) {
					PowerTuple pt = entry.getValue();
					switch (i.getPhase()) {
					case ALL:
						inv = pt;
						break;
					case L1:
						invL1 = pt;
						break;
					case L2:
						invL2 = pt;
						break;
					case L3:
						invL3 = pt;
						break;
					}
				}
			}
			if (ess instanceof ManagedAsymmetricEss && (invL1 != null || invL2 != null || invL3 != null)) {
				if (invL1 == null) {
					invL1 = new PowerTuple();
				}
				if (invL2 == null) {
					invL2 = new PowerTuple();
				}
				if (invL3 == null) {
					invL3 = new PowerTuple();
				}
				// set debug channels on Ess
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER)
						.setNextValue(invL1.getActivePower() + invL2.getActivePower() + invL3.getActivePower());
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER)
						.setNextValue(invL1.getReactivePower() + invL2.getReactivePower() + invL3.getReactivePower());
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L1)
						.setNextValue(invL1.getActivePower());
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L1)
						.setNextValue(invL1.getReactivePower());
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L2)
						.setNextValue(invL2.getActivePower());
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L2)
						.setNextValue(invL2.getReactivePower());
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L3)
						.setNextValue(invL3.getActivePower());
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L3)
						.setNextValue(invL3.getReactivePower());
				// apply Power
				((ManagedAsymmetricEss) ess).applyPower( //
						invL1.getActivePower(), invL1.getReactivePower(), //
						invL2.getActivePower(), invL2.getReactivePower(), //
						invL3.getActivePower(), invL3.getReactivePower());

			} else if (inv != null) {
				// set debug channels on Ess
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER).setNextValue(inv.getActivePower());
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER)
						.setNextValue(inv.getReactivePower());
				// apply Power
				ess.applyPower(inv.getActivePower(), inv.getReactivePower());

			} else {
				log.error("No Solution for [" + ess.id() + "] available!");
			}
		}
	}

	/**
	 * Gets all Constraints converted to Linear Constraints
	 * 
	 * @param data: the Data object
	 * @param disabledInverters: for those inverters P/Q are set to zero
	 * @return
	 */
	public static List<LinearConstraint> convertToLinearConstraints(Data data, List<Constraint> constraints) {
		List<LinearConstraint> result = new ArrayList<>();
		for (Constraint c : constraints) {
			if (c.getValue().isPresent()) {
				double[] cos = Solver.getEmptyCoefficients(data);
				for (LinearCoefficient co : c.getCoefficients()) {
					cos[co.getCoefficient().getIndex()] = co.getValue();
				}
				org.apache.commons.math3.optim.linear.Relationship relationship = null;
				switch (c.getRelationship()) {
				case EQUALS:
					relationship = org.apache.commons.math3.optim.linear.Relationship.EQ;
					break;
				case GREATER_OR_EQUALS:
					relationship = org.apache.commons.math3.optim.linear.Relationship.GEQ;
					break;
				case LESS_OR_EQUALS:
					relationship = org.apache.commons.math3.optim.linear.Relationship.LEQ;
					break;
				}
				result.add(new LinearConstraint(cos, relationship, c.getValue().get()));
			}
		}
		return result;
	}

	/**
	 * Gets the linear objective function in the form 1*a + 1*b + 1*c + ...
	 * 
	 * @param data: the Data object
	 * @return
	 */
	public static LinearObjectiveFunction getDefaultObjectiveFunction(Data data) {
		double[] cos = Solver.getEmptyCoefficients(data);
		for (int i = 0; i < cos.length; i++) {
			cos[i] = 1;
		}
		return new LinearObjectiveFunction(cos, 0);
	}

	/**
	 * Gets an empty coefficients array required for linear solver.
	 * 
	 * @param data: the Data object
	 * @return
	 */
	public static double[] getEmptyCoefficients(Data data) {
		return new double[data.getCoefficients().getNoOfCoefficients()];
	}

	/**
	 * Activates/deactivates the Debug Mode
	 * 
	 * @param debugMode
	 */
	protected void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

}
