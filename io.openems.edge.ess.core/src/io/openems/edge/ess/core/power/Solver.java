package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.DummyInverter;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.OnSolved;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.PowerException.Type;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.power.api.SolverStrategy;

public class Solver {

	private static final double LEARNING_RATE = 0.1;

	private final Logger log = LoggerFactory.getLogger(Solver.class);

	private final Data data;

	private boolean debugMode = PowerComponent.DEFAULT_DEBUG_MODE;
	private SolverStrategy strategy = PowerComponent.DEFAULT_SOLVER_STRATEGY;
	private OnSolved onSolvedCallback = (isSolved, duration, strategy) -> {
	};

	public Solver(Data data) {
		this.data = data;
	}

	/**
	 * Adds a callback for onSolved event.
	 * 
	 * @param onSolvedCallback the Callback
	 */
	public void onSolved(OnSolved onSolvedCallback) {
		this.onSolvedCallback = onSolvedCallback;
	}

	/**
	 * Tests wheter the Problem is solvable under the current Constraints.
	 * 
	 * @throws PowerException if not solvable
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
	 * Tests whether the Problem is solvable under the current Constraints.
	 * 
	 * @return true if the problem is solvable
	 */
	public boolean isSolvable() {
		try {
			this.solveWithAllConstraints();
			return true;
		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
			return false;
		}
	}

	public double getActivePowerExtrema(String essId, Phase phase, Pwr pwr, GoalType goal) {
		// prepare objective function
		int index;
		try {
			index = this.data.getCoefficient(essId, phase, pwr).getIndex();
		} catch (IllegalArgumentException e) {
			this.log.error(e.getMessage());
			return 0d;
		}
		double[] cos = Solver.getEmptyCoefficients(data);
		cos[index] = 1;
		LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(cos, 0);

		// get Constraints
		List<Constraint> allConstraints = this.data.getConstraintsForAllInverters();
		LinearConstraintSet constraints = new LinearConstraintSet(
				Solver.convertToLinearConstraints(this.data, allConstraints));

		SimplexSolver solver = new SimplexSolver();
		try {
			PointValuePair solution = solver.optimize(//
					objectiveFunction, //
					constraints, //
					goal, //
					PivotSelectionRule.BLAND);
			return solution.getPoint()[index];
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
		// measure duration
		final long startTime = System.nanoTime();

		// No Inverters -> nothing to do
		if (this.data.getInverters().isEmpty()) {
			this.onSolvedCallback.accept(true, 0, SolverStrategy.NONE);
			return;
		}
		List<Inverter> allInverters = data.getInverters();

		// Check if the Problem is solvable at all.
		List<Constraint> allConstraints = this.data.getConstraintsForAllInverters();

		// Add Strict constraints if required
		this.addConstraintsForNotStrictlyDefinedCoefficients(allInverters, allConstraints);

		// Print log with currently active EQUALS != 0 Constraints
		if (this.debugMode) {
			this.log.info("Currently active EQUALS contraints");
			for (Constraint c : allConstraints) {
				if (c.getRelationship() == Relationship.EQUALS && c.getValue().orElse(0d) != 0d) {
					this.log.info("- " + c.toString());
				}
			}
		}

		SolveSolution solution = new SolveSolution(SolverStrategy.NONE, null);
		TargetDirection targetDirection = null;
		try {
			// Evaluates whether it is a CHARGE or DISCHARGE problem.
			targetDirection = this.getTargetDirection();

			// Gets the target-Inverters, i.e. the Inverters that are minimally required to
			// solve the Problem.
			List<Inverter> targetInverters = this.getTargetInverters(data.getInverters(), targetDirection);

			switch (this.strategy) {
			case UNDEFINED:
			case ALL_CONSTRAINTS:
			case NONE:
				solution = this.tryStrategies(targetDirection, allInverters, targetInverters, allConstraints);
				break;

			case OPTIMIZE_BY_MOVING_TOWARDS_TARGET:
				solution = this.tryStrategies(targetDirection, allInverters, targetInverters, allConstraints,
						SolverStrategy.OPTIMIZE_BY_MOVING_TOWARDS_TARGET,
						SolverStrategy.OPTIMIZE_BY_KEEPING_TARGET_DIRECTION_AND_MAXIMIZING_IN_ORDER);
				break;

			case OPTIMIZE_BY_KEEPING_TARGET_DIRECTION_AND_MAXIMIZING_IN_ORDER:
				solution = this.tryStrategies(targetDirection, allInverters, targetInverters, allConstraints,
						SolverStrategy.OPTIMIZE_BY_KEEPING_TARGET_DIRECTION_AND_MAXIMIZING_IN_ORDER,
						SolverStrategy.OPTIMIZE_BY_MOVING_TOWARDS_TARGET);
				break;
			}

		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
			if (this.debugMode) {
				PowerComponent.debugLogConstraints(this.log,
						"[" + e.getMessage() + "] Unable to solve under the following constraints:", allConstraints);
			} else {
				this.log.warn("Power-Solver: Unable to solve under constraints!");
			}
		}

		// finish time measure (in milliseconds)
		int duration = (int) (System.nanoTime() - startTime) / 1_000_000;

		// announce success/failure
		boolean isSolved = solution.getPoints() != null;
		this.onSolvedCallback.accept(isSolved, duration, solution.getSolvedBy());

		// Apply final Solution to Inverters
		if (isSolved) {
			this.applySolution(this.applyInverterPrecisions(allInverters, solution.getPoints(), targetDirection));
		} else {
			this.applySolution(this.getZeroSolution(allInverters));
		}
	}

	/**
	 * Tries different solving strategies in order. 'ALL_CONSTRAINTS' is always
	 * tried last if everything else failed. Returns as soon as a result is found.
	 * 
	 * @param targetDirection the target direction
	 * @param allInverters    a list of all inverters
	 * @param targetInverters a list of target inverters
	 * @param allConstraints  a list of all Constraints
	 * @param strategies      an array of SolverStrategies
	 * @return a Solution
	 */
	private SolveSolution tryStrategies(TargetDirection targetDirection, List<Inverter> allInverters,
			List<Inverter> targetInverters, List<Constraint> allConstraints, SolverStrategy... strategies) {
		PointValuePair solution = null;
		for (SolverStrategy strategy : strategies) {
			switch (strategy) {
			case UNDEFINED:
			case NONE:
				break;
			case ALL_CONSTRAINTS:
				solution = this.solveWithConstraints(allConstraints);
				break;
			case OPTIMIZE_BY_MOVING_TOWARDS_TARGET:
				solution = this.optimizeByMovingTowardsTarget(targetDirection, allInverters, targetInverters,
						allConstraints);
				break;
			case OPTIMIZE_BY_KEEPING_TARGET_DIRECTION_AND_MAXIMIZING_IN_ORDER:
				solution = this.optimizeByKeepingTargetDirectionAndMaximizingInOrder(allInverters, targetInverters,
						allConstraints, targetDirection);
				break;
			}

			if (solution != null) {
				return new SolveSolution(strategy, solution);
			}
		}
		// to strategy was successful -> try allConstraints
		solution = this.solveWithConstraints(allConstraints);
		if (solution != null) {
			return new SolveSolution(SolverStrategy.ALL_CONSTRAINTS, solution);
		} else {
			return new SolveSolution(SolverStrategy.NONE, null);
		}
	}

	/**
	 * Adds Constraints for not strictly defined Coefficients, e.g. if only a P <= X
	 * is defined, but no P = X.
	 * 
	 * @param allInverters   a list of all inverters
	 * @param allConstraints a list of all Constraints
	 */
	private void addConstraintsForNotStrictlyDefinedCoefficients(List<Inverter> allInverters,
			List<Constraint> allConstraints) {
		LinearConstraintSet constraints = new LinearConstraintSet(
				Solver.convertToLinearConstraints(this.data, allConstraints));

		for (Pwr pwr : Pwr.values()) {
			// prepare objective function
			double[] cos = Solver.getEmptyCoefficients(data);
			for (Inverter inv : allInverters) {
				Coefficient c = this.data.getCoefficient(inv.getEssId(), inv.getPhase(), pwr);
				cos[c.getIndex()] = 1;
			}
			LinearObjectiveFunction objectiveFunction = new LinearObjectiveFunction(cos, 0);

			// get Max value over all relevant Coefficients
			double max;
			try {
				SimplexSolver solver = new SimplexSolver();
				PointValuePair solution = solver.optimize(//
						objectiveFunction, //
						constraints, //
						GoalType.MAXIMIZE, //
						PivotSelectionRule.BLAND);
				max = 0d;
				for (Inverter inv : allInverters) {
					Coefficient c = this.data.getCoefficient(inv.getEssId(), inv.getPhase(), pwr);
					max += solution.getPoint()[c.getIndex()];
				}
			} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
				max = Double.MAX_VALUE;
			}
			// get Min value over all relevant Coefficients
			double min;
			try {
				SimplexSolver solver = new SimplexSolver();
				PointValuePair solution = solver.optimize(//
						objectiveFunction, //
						constraints, //
						GoalType.MINIMIZE, //
						PivotSelectionRule.BLAND);
				min = 0d;
				for (Inverter inv : allInverters) {
					Coefficient c = this.data.getCoefficient(inv.getEssId(), inv.getPhase(), pwr);
					min += solution.getPoint()[c.getIndex()];
				}
			} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
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

			LinearCoefficient[] lcs = new LinearCoefficient[allInverters.size()];
			for (int i = 0; i < allInverters.size(); i++) {
				Inverter inv = allInverters.get(i);
				Coefficient c = this.data.getCoefficient(inv.getEssId(), inv.getPhase(), pwr);
				lcs[i] = new LinearCoefficient(c, 1);
			}
			Constraint c = new Constraint("Strictly define " + pwr.name(), lcs, Relationship.EQUALS, target);
			allConstraints.add(c);
		}
	}

	/**
	 * Tries to distribute power equally between inverters
	 * 
	 * @param targetInverters
	 * @param allConstraints
	 * @return
	 */
	// private PointValuePair optimizeByDistributingEqually(List<Inverter>
	// targetInverters,
	// List<Constraint> allConstraints) {
	//
	// double[] weights = new double[targetInverters.size()];
	// for (int invIndex = 1; invIndex < targetInverters.size(); invIndex++) {
	// for (double weight = 1; weight > 0; weight -= LEARNING_RATE) {
	// }
	// }
	//
	// List<Constraint> constraints = new ArrayList<>(allConstraints);
	// for (int i = 1; i < targetInverters.size(); i++) {
	// Inverter inv0 = targetInverters.get(0);
	// Inverter inv1 = targetInverters.get(i);
	// constraints.add(new Constraint(inv0.toString() + "/" + inv1.toString() + ":
	// distribute equally",
	// new LinearCoefficient[] { //
	// new LinearCoefficient(this.data.getCoefficient(inv0.getEss(),
	// inv0.getPhase(), Pwr.ACTIVE),
	// 1), //
	// new LinearCoefficient(this.data.getCoefficient(inv1.getEss(),
	// inv1.getPhase(), Pwr.ACTIVE),
	// 1) //
	// }, Relationship.EQUALS, 0));
	// }
	//
	// try {
	// return this.solveWithConstraints(constraints);
	// } catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
	// log.warn("Unable to solve with optimizeByDistributingEqually()");
	// return null;
	// }
	// }

	/**
	 * Tries to keep all Target Inverters in the right TargetDirection; then
	 * maximizes them in order.
	 * 
	 * @param allInverters    a list of all inverters
	 * @param targetInverters a list of target inverters
	 * @param allConstraints  a list of all Cosntraints
	 * @param targetDirection the target direction
	 * @return a solution or null
	 */
	private PointValuePair optimizeByKeepingTargetDirectionAndMaximizingInOrder(List<Inverter> allInverters,
			List<Inverter> targetInverters, List<Constraint> allConstraints, TargetDirection targetDirection) {
		List<Constraint> constraints = new ArrayList<>(allConstraints);

		// Add Zero-Constraint for all Inverters that are not Target
		for (Inverter inv : allInverters) {
			if (!targetInverters.contains(inv)) {
				constraints.add(this.data.createSimpleConstraint(inv.toString() + ": is not a target inverter",
						inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, 0));
				constraints.add(this.data.createSimpleConstraint(inv.toString() + ": is not a target inverter",
						inv.getEssId(), inv.getPhase(), Pwr.REACTIVE, Relationship.EQUALS, 0));
			}
		}

		PointValuePair result = this.solveWithConstraints(constraints);
		PointValuePair thisSolution = null;

		Relationship relationship = Relationship.EQUALS;
		switch (targetDirection) {
		case CHARGE:
			relationship = Relationship.LESS_OR_EQUALS;
			break;
		case DISCHARGE:
			relationship = Relationship.GREATER_OR_EQUALS;
			break;
		case KEEP_ZERO:
			relationship = Relationship.EQUALS;
			break;
		}

		for (Inverter inv : targetInverters) {
			// Create Constraint to force Ess positive/negative/zero according to
			// targetDirection
			Constraint c = this.data.createSimpleConstraint(inv.toString() + ": Force " + targetDirection.name(),
					inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, relationship, 0);
			constraints.add(c);
			// Try to solve with Constraint
			try {
				thisSolution = this.solveWithConstraints(constraints);
				result = thisSolution; // only if solving was successful
			} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
				// solving failed
				constraints.remove(c);
			}
		}

		if (targetDirection == TargetDirection.KEEP_ZERO && result != null) {
			return result;
		}

		// Try maximizing all inverters in order in target direction
		for (Inverter inv : targetInverters) {
			GoalType goal;
			if (targetDirection == TargetDirection.CHARGE) {
				goal = GoalType.MINIMIZE;
			} else {
				goal = GoalType.MAXIMIZE;
			}
			double target = this.getActivePowerExtrema(inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, goal);
			Constraint c = this.data.createSimpleConstraint(inv.toString() + ": Set " + goal.name() + " value",
					inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, target);
			constraints.add(c);
			// Try to solve with Constraint
			try {
				thisSolution = this.solveWithConstraints(constraints);
				result = thisSolution; // only if solving was successful
			} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
				// If solving fails: remove the Constraints
				constraints.remove(c);
			}
		}

		return result;
	}

	/**
	 * Tries to adjust the weights used in last applyPower() towards the target
	 * weights using a learning rate. If this fails it tries to start from the
	 * target weights towards a given existing solution.
	 *
	 * @param targetDirection the target direction
	 * @param allInverters    a list of all inverters
	 * @param targetInverters a list of target inverters
	 * @param allConstraints  a list of all Constraints
	 * @return a solution or null
	 */
	private PointValuePair optimizeByMovingTowardsTarget(TargetDirection targetDirection, List<Inverter> allInverters,
			List<Inverter> targetInverters, List<Constraint> allConstraints) {
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
					targetWeights.put(inv, (100 - (inv.getWeight() / sumWeights)));
					break;
				case DISCHARGE:
					targetWeights.put(inv, inv.getWeight() / sumWeights);
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
					Constraint c = this.data.createSimpleConstraint(inv.toString() + ": next weight = 0",
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
								new LinearCoefficient(
										this.data.getCoefficient(invA.getEssId(), invA.getPhase(), Pwr.ACTIVE),
										nextWeights.get(invB)),
								new LinearCoefficient(
										this.data.getCoefficient(invB.getEssId(), invB.getPhase(), Pwr.ACTIVE),
										nextWeights.get(invA) * -1) },
						Relationship.EQUALS, 0);
				constraints.add(c);
			}

			try {
				PointValuePair solution = this.solveWithConstraints(constraints);
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

	/**
	 * Rounds each solution value to the Inverter precision; following this logic.
	 *
	 * <p>
	 * On Discharge (Power > 0)
	 *
	 * <ul>
	 * <li>if SoC > 50 %: round up (more discharge)
	 * <li>if SoC < 50 %: round down (less discharge)
	 * </ul>
	 *
	 * <p>
	 * On Charge (Power < 0)
	 *
	 * <ul>
	 * <li>if SoC > 50 %: round down (less charge)
	 * <li>if SoC < 50 %: round up (more discharge)
	 * </ul>
	 *
	 * @param allInverters    a list of all inverters
	 * @param solution        a solution
	 * @param targetDirection the target direction
	 * @return a map of inverters to PowerTuples
	 */
	// TODO: round value of one inverter, apply constraint, repeat... to further
	// optimize this
	private Map<Inverter, PowerTuple> applyInverterPrecisions(List<Inverter> allInverters, PointValuePair solution,
			TargetDirection targetDirection) {
		Map<Inverter, PowerTuple> result = new HashMap<>();
		double[] point = solution.getPoint();
		for (Inverter inv : allInverters) {
			Round round = Round.TOWARDS_ZERO;
			String essId = inv.getEssId();
			ManagedSymmetricEss ess = this.data.getEss(essId);
			int soc = ess.getSoc().value().orElse(0);
			int precision = ess.getPowerPrecision();
			PowerTuple powerTuple = new PowerTuple();
			for (Pwr pwr : Pwr.values()) {
				Coefficient c = this.data.getCoefficient(essId, inv.getPhase(), pwr);
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

	private TargetDirection activeTargetDirection = null;
	private int targetDirectionChangedSince = 0;

	/**
	 * Finds the target Inverters, i.e. the Inverters that are minimally required to
	 * fulfill all Constraints.
	 * 
	 * <p>
	 * This method therefore tries to remove inverters in order until there is no
	 * solution anymore. It than re-adds that inverter and returns the solution.
	 * 
	 * @param allInverters    a list of all inverters
	 * @param targetDirection the target direction
	 * @return a list of target inverters
	 */
	private List<Inverter> getTargetInverters(List<Inverter> allInverters, TargetDirection targetDirection) {
		List<Inverter> disabledInverters = new ArrayList<>();

		// Change target direction only once in a while
		if (this.activeTargetDirection == null || targetDirectionChangedSince > 100) {
			if (this.debugMode) {
				log.info("Change target direction from [" + this.activeTargetDirection + "] to [" + targetDirection
						+ "] after [" + targetDirectionChangedSince + "]");
			}
			this.activeTargetDirection = targetDirection;
		}
		if (this.activeTargetDirection != targetDirection) {
			this.targetDirectionChangedSince++;
		} else {
			this.targetDirectionChangedSince = 0;
		}

		// For CHARGE take list as it is; for DISCHARGE reverse it. This prefers
		// high-weight inverters (e.g. high state-of-charge) on DISCHARGE and low-weight
		// inverters (e.g. low state-of-charge) on CHARGE.
		List<Inverter> allInvertersTargetDirection;
		if (this.activeTargetDirection == TargetDirection.DISCHARGE) {
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
		// get result in the order of preferred usage
		if (this.activeTargetDirection == TargetDirection.CHARGE) {
			result = Lists.reverse(result);
		}
		return result;
	}

	/**
	 * Solves the problem, while setting all DisabledInverters to EQUALS zero.
	 * 
	 * @param disabledInverters a list of disabled inverters
	 * @return a solution
	 * @throws NoFeasibleSolutionException if not solvable
	 * @throws UnboundedSolutionException  if not solvable
	 */
	private PointValuePair solveWithDisabledInverters(List<Inverter> disabledInverters)
			throws NoFeasibleSolutionException, UnboundedSolutionException {
		List<Constraint> constraints = this.data.getConstraintsWithoutDisabledInverters(disabledInverters);
		return this.solveWithConstraints(constraints);
	}

	/**
	 * Solves the problem with the given list of Constraints.
	 * 
	 * @param constraints a list of Constraints
	 * @return a solution
	 * @throws NoFeasibleSolutionException if not solvable
	 * @throws UnboundedSolutionException  if not solvable
	 */
	private PointValuePair solveWithConstraints(List<Constraint> constraints)
			throws NoFeasibleSolutionException, UnboundedSolutionException {
		List<LinearConstraint> linearConstraints = Solver.convertToLinearConstraints(this.data, constraints);
		return this.solveWithLinearConstraints(linearConstraints);
	}

	/**
	 * Solves the problem with the given list of LinearConstraints.
	 * 
	 * @param constraints a list of LinearConstraints
	 * @return a solution
	 * @throws NoFeasibleSolutionException if not solvable
	 * @throws UnboundedSolutionException  if not solvable
	 */
	private PointValuePair solveWithLinearConstraints(List<LinearConstraint> constraints)
			throws NoFeasibleSolutionException, UnboundedSolutionException {
		LinearObjectiveFunction objectiveFunction = Solver.getDefaultObjectiveFunction(this.data);

		SimplexSolver solver = new SimplexSolver();
		return solver.optimize(//
				objectiveFunction, //
				new LinearConstraintSet(constraints), //
				GoalType.MINIMIZE, //
				PivotSelectionRule.BLAND);
	}

	/**
	 * Solves the problem. Applies all set Constraints.
	 * 
	 * @return a solution
	 * @throws NoFeasibleSolutionException if not solvable
	 * @throws UnboundedSolutionException  if not solvable
	 */
	private PointValuePair solveWithAllConstraints() throws NoFeasibleSolutionException, UnboundedSolutionException {
		List<Constraint> allConstraints = this.data.getConstraintsForAllInverters();
		return this.solveWithConstraints(allConstraints);
	}

	public enum TargetDirection {
		KEEP_ZERO, CHARGE, DISCHARGE;
	}

	/**
	 * Gets the TargetDirection of the Problem, i.e. whether it is a DISCHARGE or
	 * CHARGE problem.
	 * 
	 * @return the target direction
	 */
	public TargetDirection getTargetDirection() {
		List<Constraint> constraints = this.data.getConstraintsForAllInverters();
		Constraint equals0 = this.data.createPConstraint(Relationship.EQUALS, 0);
		constraints.add(equals0);
		try {
			this.solveWithConstraints(constraints);
			return TargetDirection.KEEP_ZERO;
		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
			constraints.remove(equals0);
			Constraint greaterOrEquals0 = this.data.createPConstraint(Relationship.GREATER_OR_EQUALS, 0);
			constraints.add(greaterOrEquals0);
			try {
				this.solveWithConstraints(constraints);
				return TargetDirection.DISCHARGE;
			} catch (NoFeasibleSolutionException | UnboundedSolutionException e2) {
				constraints.remove(greaterOrEquals0);
				Constraint lessOrEquals0 = this.data.createPConstraint(Relationship.LESS_OR_EQUALS, 0);
				constraints.add(lessOrEquals0);
				this.solveWithConstraints(constraints);
				return TargetDirection.CHARGE;
			}
		}
	}

	/**
	 * Send the final solution to each Inverter.
	 * 
	 * @param finalSolution a map of inverters to PowerTuples
	 */
	private void applySolution(Map<Inverter, PowerTuple> finalSolution) {
		// Info-Log
		StringBuilder b = new StringBuilder("Apply Power: ");
		List<Inverter> inverters = new ArrayList<>(finalSolution.keySet());
		inverters.sort((i1, i2) -> {
			return i1.toString().compareTo(i2.toString());
		});
		for (Inverter inv : inverters) {
			if (inv instanceof DummyInverter) {
				continue;
			}
			b.append(inv.toString() + " " + finalSolution.get(inv).toString() + " ");
		}
//		log.info(b.toString());

		// store last value inside Inverter
		finalSolution.forEach((inv, powerTuple) -> {
			inv.setLastActivePower(powerTuple.getActivePower());
		});

		for (String essId : this.data.getEssIds()) {
			ManagedSymmetricEss ess = this.data.getEss(essId);
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
				if (Objects.equals(essId, i.getEssId())) {
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
				try {
					((ManagedAsymmetricEss) ess).applyPower(//
							invL1.getActivePower(), invL1.getReactivePower(), //
							invL2.getActivePower(), invL2.getReactivePower(), //
							invL3.getActivePower(), invL3.getReactivePower());
					
					// announce applying power was ok
					ess.getApplyPowerFailed().setNextValue(false);

				} catch (OpenemsNamedException e) {
					this.log.warn("Error in Ess [" + ess.id() + "] apply power: " + e.getMessage());

					// announce running failed
					ess.getApplyPowerFailed().setNextValue(true);
				}

			} else if (inv != null) {
				// set debug channels on Ess
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER).setNextValue(inv.getActivePower());
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER)
						.setNextValue(inv.getReactivePower());
				// apply Power
				try {
					ess.applyPower(inv.getActivePower(), inv.getReactivePower());

					// announce applying power was ok
					ess.getApplyPowerFailed().setNextValue(false);

				} catch (OpenemsNamedException e) {
					this.log.warn("Error in Ess [" + ess.id() + "] apply power: " + e.getMessage());

					// announce running failed
					ess.getApplyPowerFailed().setNextValue(true);
				}

			} else {
				log.error("No Solution for [" + ess.id() + "] available!");
			}
		}
	}

	/**
	 * Gets all Constraints converted to Linear Constraints.
	 * 
	 * @param data        the data object
	 * @param constraints a list of Constraints
	 * @return a list of LinearConstraints
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
	 * @param data the Data object
	 * @return a LinearObjectiveFunction
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
	 * @param data the Data object
	 * @return an array of '0' coefficients
	 */
	public static double[] getEmptyCoefficients(Data data) {
		return new double[data.getCoefficients().getNoOfCoefficients()];
	}

	/**
	 * Activates/deactivates the Debug Mode.
	 * 
	 * @param debugMode true to activate
	 */
	protected void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	/**
	 * Sets the solver strategy.
	 * 
	 * @param strategy the SolverStrategy
	 */
	public void setStrategy(SolverStrategy strategy) {
		this.strategy = strategy;
	}
}
