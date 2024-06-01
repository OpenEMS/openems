package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.core.power.data.InverterPrecision;
import io.openems.edge.ess.core.power.data.LogUtil;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.core.power.optimizers.AddConstraintsForNotStrictlyDefinedCoefficients;
import io.openems.edge.ess.core.power.optimizers.KeepAllEqual;
import io.openems.edge.ess.core.power.optimizers.KeepAllNearEqual;
import io.openems.edge.ess.core.power.optimizers.KeepTargetDirectionAndMaximizeInOrder;
import io.openems.edge.ess.core.power.optimizers.MoveTowardsTarget;
import io.openems.edge.ess.core.power.optimizers.Optimizers;
import io.openems.edge.ess.core.power.solver.ConstraintSolver;
import io.openems.edge.ess.core.power.solver.PowerTuple;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.OnSolved;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.PowerException.Type;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.power.api.SolverStrategy;

public class Solver {

	private final Logger log = LoggerFactory.getLogger(Solver.class);
	private final Data data;
	private final Optimizers optimizers = new Optimizers();

	private boolean debugMode = EssPower.DEFAULT_DEBUG_MODE;
	private OnSolved onSolvedCallback = (isSolved, duration, strategy) -> {
	};

	private final ThrowingFunction<List<Inverter>, PointValuePair, Exception> solveWithDisabledInverters;

	public Solver(Data data) {
		this.data = data;

		/**
		 * Solves the problem, while setting all DisabledInverters to EQUALS zero.
		 *
		 * @param disabledInverters a list of disabled inverters
		 * @return a solution
		 * @throws NoFeasibleSolutionException if not solvable
		 * @throws UnboundedSolutionException  if not solvable
		 * @throws OpenemsException
		 */
		this.solveWithDisabledInverters = disabledInverters -> {
			var constraints = this.data.getConstraintsWithoutDisabledInverters(disabledInverters);
			return ConstraintSolver.solve(this.data.getCoefficients(), constraints);
		};
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
	 * Tests whether the Problem is solvable under the current Constraints.
	 *
	 * @throws OpenemsException on error
	 */
	public void isSolvableOrError() throws OpenemsException {
		try {
			ConstraintSolver.solve(this.data.getCoefficients(), this.data.getConstraintsForAllInverters());
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
			ConstraintSolver.solve(this.data.getCoefficients(), this.data.getConstraintsForAllInverters());
			return true;
		} catch (NoFeasibleSolutionException | UnboundedSolutionException | OpenemsException e) {
			return false;
		}
	}

	/**
	 * Solve and optimize the equation system.
	 *
	 * <p>
	 * When finished, this method calls the applyPower() methods of
	 * {@link ManagedSymmetricEss} or {@link ManagedAsymmetricEss}.
	 *
	 * @param strategy the {@link SolverStrategy} to follow
	 */
	public void solve(SolverStrategy strategy) {
		// measure duration
		final var startTime = System.nanoTime();

		// No Inverters -> nothing to do
		if (this.data.getInverters().isEmpty()) {
			this.onSolvedCallback.accept(true, 0, SolverStrategy.NONE);
			return;
		}
		var allInverters = this.data.getInverters();

		var solution = new SolveSolution(SolverStrategy.NONE, null);

		List<Constraint> allConstraints = new ArrayList<>();
		TargetDirection targetDirection = null;
		try {
			// Check if the Problem is solvable at all.
			allConstraints = this.data.getConstraintsForAllInverters();

			// Add Strict constraints if required
			AddConstraintsForNotStrictlyDefinedCoefficients.apply(allInverters, this.data.getCoefficients(),
					allConstraints);

			// Print log with currently active EQUALS != 0 Constraints
			if (this.debugMode) {
				this.log.info("Currently active EQUALS constraints");
				for (Constraint c : allConstraints) {
					if (c.getRelationship() == Relationship.EQUALS && c.getValue().orElse(0d) != 0d) {
						this.log.info("- " + c.toString());
					}
				}
			}

			// Evaluates whether it is a CHARGE or DISCHARGE problem.
			targetDirection = TargetDirection.from(//
					this.data.getInverters(), //
					this.data.getCoefficients(), //
					this.data.getConstraintsForAllInverters() //
			);

			// Gets the target-Inverters, i.e. the Inverters that are minimally required to
			// solve the Problem.
			var targetInverters = this.optimizers.reduceNumberOfUsedInverters.apply(allInverters, targetDirection,
					this.solveWithDisabledInverters);

			switch (strategy) {
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

			case OPTIMIZE_BY_KEEPING_ALL_EQUAL:
			case OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL:
				solution = this.tryStrategies(targetDirection, allInverters, targetInverters, allConstraints,
						SolverStrategy.OPTIMIZE_BY_KEEPING_ALL_EQUAL, //
						SolverStrategy.OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL, //
						SolverStrategy.OPTIMIZE_BY_KEEPING_TARGET_DIRECTION_AND_MAXIMIZING_IN_ORDER,
						SolverStrategy.OPTIMIZE_BY_MOVING_TOWARDS_TARGET);
				break;
			}

		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
			if (this.debugMode) {
				LogUtil.debugLogConstraints(this.log,
						"[" + e.getMessage() + "] Unable to solve under the following constraints:", allConstraints);
			} else {
				this.log.warn("Power-Solver: Unable to solve under constraints!");
			}
		} catch (OpenemsException e) {
			this.log.warn("Power-Solver: Solve failed: " + e.getMessage());
		}

		// finish time measure (in milliseconds)
		var duration = (int) (System.nanoTime() - startTime) / 1_000_000;

		// announce success/failure
		var isSolved = solution.getPoints() != null;
		this.onSolvedCallback.accept(isSolved, duration, solution.getSolvedBy());

		// Apply final Solution to Inverters
		if (isSolved) {
			Map<Inverter, PowerTuple> inverterSolutionMap;
			try {
				inverterSolutionMap = InverterPrecision.apply(this.data.getCoefficients(), allInverters,
						this.data.getEsss(), solution.getPoints(), targetDirection);

			} catch (OpenemsException e) {
				this.log.warn("Power-Solver: Applying Inverter Precisions failed: " + e.getMessage());
				inverterSolutionMap = this.getZeroSolution(allInverters);
			}
			this.applySolution(inverterSolutionMap);
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
	 * @throws OpenemsException on error
	 */
	private SolveSolution tryStrategies(TargetDirection targetDirection, List<Inverter> allInverters,
			List<Inverter> targetInverters, List<Constraint> allConstraints, SolverStrategy... strategies)
			throws OpenemsException {
		PointValuePair solution = null;
		for (SolverStrategy strategy : strategies) {
			switch (strategy) {
			case UNDEFINED:
			case NONE:
				break;
			case ALL_CONSTRAINTS:
				solution = ConstraintSolver.solve(this.data.getCoefficients(), allConstraints);
				break;
			case OPTIMIZE_BY_MOVING_TOWARDS_TARGET:
				solution = MoveTowardsTarget.apply(this.data.getCoefficients(), targetDirection, allInverters,
						targetInverters, allConstraints);
				break;
			case OPTIMIZE_BY_KEEPING_TARGET_DIRECTION_AND_MAXIMIZING_IN_ORDER:
				solution = KeepTargetDirectionAndMaximizeInOrder.apply(this.data.getCoefficients(), allInverters,
						targetInverters, allConstraints, targetDirection);
				break;
			case OPTIMIZE_BY_KEEPING_ALL_EQUAL:
				solution = KeepAllEqual.apply(this.data.getCoefficients(), allInverters, allConstraints);
				break;
			case OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL:
				solution = KeepAllNearEqual.apply(this.data.getCoefficients(), this.data.getEsss(), allInverters,
						allConstraints, targetDirection);
				break;
			}

			if (solution != null) {
				return new SolveSolution(strategy, solution);
			}
		}
		// no strategy was successful -> try allConstraints
		solution = ConstraintSolver.solve(this.data.getCoefficients(), allConstraints);
		if (solution != null) {
			return new SolveSolution(SolverStrategy.ALL_CONSTRAINTS, solution);
		}
		return new SolveSolution(SolverStrategy.NONE, null);
	}

	private Map<Inverter, PowerTuple> getZeroSolution(List<Inverter> allInverters) {
		Map<Inverter, PowerTuple> result = new HashMap<>();
		for (Inverter inv : allInverters) {
			var powerTuple = new PowerTuple();
			for (Pwr pwr : Pwr.values()) {
				powerTuple.setValue(pwr, 0);
			}
			result.put(inv, powerTuple);
		}
		return result;
	}

	/**
	 * Send the final solution to each Inverter.
	 *
	 * @param finalSolution a map of inverters to PowerTuples
	 */
	private void applySolution(Map<Inverter, PowerTuple> finalSolution) {
		// store last value inside Inverter
		finalSolution.forEach((inv, powerTuple) -> {
			inv.setLastActivePower(powerTuple.getActivePower());
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
				var i = entry.getKey();
				if (Objects.equals(ess.id(), i.getEssId())) {
					var pt = entry.getValue();
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
				/*
				 * Call applyPower() of ManagedAsymmetricEss
				 */

				if (invL1 == null) {
					invL1 = new PowerTuple();
				}
				if (invL2 == null) {
					invL2 = new PowerTuple();
				}
				if (invL3 == null) {
					invL3 = new PowerTuple();
				}

				var e = (ManagedAsymmetricEss) ess;
				// set debug channels on Ess
				e._setDebugSetActivePower(invL1.getActivePower() + invL2.getActivePower() + invL3.getActivePower());
				e._setDebugSetReactivePower(
						invL1.getReactivePower() + invL2.getReactivePower() + invL3.getReactivePower());
				e._setDebugSetActivePowerL1(invL1.getActivePower());
				e._setDebugSetActivePowerL2(invL2.getActivePower());
				e._setDebugSetActivePowerL3(invL3.getActivePower());
				e._setDebugSetReactivePowerL1(invL1.getReactivePower());
				e._setDebugSetReactivePowerL2(invL2.getReactivePower());
				e._setDebugSetReactivePowerL3(invL3.getReactivePower());
				// apply Power
				try {
					e.applyPower(//
							invL1.getActivePower(), invL1.getReactivePower(), //
							invL2.getActivePower(), invL2.getReactivePower(), //
							invL3.getActivePower(), invL3.getReactivePower());

					// announce applying power was ok
					ess._setApplyPowerFailed(false);

				} catch (OpenemsNamedException ex) {
					this.log.warn("Error in Ess [" + ess.id() + "] apply power: " + ex.getMessage());

					// announce running failed
					ess._setApplyPowerFailed(true);
				}

			} else if (inv != null) {
				/*
				 * Call applyPower() of ManagedSymmetricEss
				 */

				// set debug channels on Ess
				ess._setDebugSetActivePower(inv.getActivePower());
				ess._setDebugSetReactivePower(inv.getReactivePower());

				// apply Power
				try {
					ess.applyPower(inv.getActivePower(), inv.getReactivePower());

					// announce applying power was ok
					ess._setApplyPowerFailed(false);

				} catch (OpenemsNamedException e) {
					this.log.warn("Error in Ess [" + ess.id() + "] apply power: " + e.getMessage());

					// announce running failed
					ess._setApplyPowerFailed(true);
				}

			} else {
				this.log.error("No Solution for [" + ess.id() + "] available!");
			}
		}
	}

	protected void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}
}
