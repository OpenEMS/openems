package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.PivotSelectionRule;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;

/**
 * This holds the the linear solver. It tries to solve the distribution of
 * Active and Reactive Power among the ESSs using a linear objective function.
 * 
 * The LinearSolver is a fast approach to prove that a solution to a problem is
 * available and gives a first solution. It fails in taking in account more
 * complicated constraints that cannot be wrapped in a linear equation.
 */
public class LinearPower implements Power {

	private final Logger log = LoggerFactory.getLogger(LinearPower.class);

	/*
	 * Holds a reference to the Data POJO
	 */
	private final Data data = new Data();

	public Constraint addSimpleConstraintAndValidate(ManagedSymmetricEss ess, ConstraintType type, Phase phase, Pwr pwr,
			Relationship relationship, int value) throws PowerException {
		return this.addConstraintAndValidate(//
				Utils.createSimpleConstraint(ess, type, phase, pwr, relationship, value));
	}

	/**
	 * Adds a Constraint
	 * 
	 * @param constraint
	 * @return
	 */
	public synchronized Constraint addConstraint(Constraint constraint) {
		return this.data.addConstraint(constraint);
	}

	/**
	 * Adds a Constraint if
	 * 
	 * @param constraint
	 * @return
	 * @throws PowerException
	 */
	public synchronized Constraint addConstraintAndValidate(Constraint constraint) throws PowerException {
		LinearConstraint lc = this.data.toLinearConstraint(constraint);
		if (!this.isSolvable(lc)) {
			// throws the exception if it is not solvable
			throw new PowerException(new NoFeasibleSolutionException());
		}
		this.addConstraint(constraint);
		return constraint;
	}

	public synchronized void addEss(ManagedSymmetricEss ess) {
		this.data.addEss(ess);
	}

	public Constraint addSimpleConstraint(ManagedSymmetricEss ess, ConstraintType type, Phase phase, Pwr pwr,
			Relationship relationship, int value) {
		return this.data.addSimpleConstraint(ess, type, phase, pwr, relationship, value);
	}

	/**
	 * This is the final method of the Power class. It should never be called
	 * manually, as it is getting called by the framework.
	 * <ul>
	 * <li>It solves the Linear Objective Function
	 * <li>It tries to improve the result using Genetic Algorithms
	 * <li>It calls the applyPower() methods of each Ess
	 * </ul>
	 * 
	 * @throws Exception
	 */
	public synchronized void applyPower() {
		// solve using linear solver
		double[] solutionArray = this.solveOptimally();

		Map<ManagedSymmetricEss, SymmetricSolution> solutions = Utils.toSolutions(this.data, solutionArray);

		// set debug channels on parent
		this.data.allEsss.forEach(ess -> {
			AtomicInteger activePower = new AtomicInteger(0);
			AtomicInteger reactivePower = new AtomicInteger(0);
			AtomicInteger activePowerL1 = new AtomicInteger(0);
			AtomicInteger reactivePowerL1 = new AtomicInteger(0);
			AtomicInteger activePowerL2 = new AtomicInteger(0);
			AtomicInteger reactivePowerL2 = new AtomicInteger(0);
			AtomicInteger activePowerL3 = new AtomicInteger(0);
			AtomicInteger reactivePowerL3 = new AtomicInteger(0);
			Utils.sumSolutions(ess, solutions, activePower, reactivePower, activePowerL1, reactivePowerL1,
					activePowerL2, reactivePowerL2, activePowerL3, reactivePowerL3);
			if (ess instanceof ManagedAsymmetricEss) {
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L1).setNextValue(activePowerL1.get());
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L1)
						.setNextValue(reactivePowerL1.get());
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L2).setNextValue(activePowerL2.get());
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L2)
						.setNextValue(reactivePowerL2.get());
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L3).setNextValue(activePowerL3.get());
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L3)
						.setNextValue(reactivePowerL3.get());
			}
			ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER).setNextValue(activePower.get());
			ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER).setNextValue(reactivePower.get());
		});

		this.data.realEsss.forEach(ess -> {
			SymmetricSolution ss = solutions.get(ess);
			if (ss == null) {
				return;
			}

			if (ess instanceof ManagedAsymmetricEss && ss instanceof AsymmetricSolution) {
				/*
				 * ManagedAsymmetricEss
				 */
				ManagedAsymmetricEss e = (ManagedAsymmetricEss) ess;
				AsymmetricSolution as = (AsymmetricSolution) ss;

				e.applyPower(as.getActivePowerL1(), as.getReactivePowerL1(), as.getActivePowerL2(),
						as.getReactivePowerL2(), as.getActivePowerL3(), as.getReactivePowerL3());
			} else {
				/*
				 * ManagedSymmetricEss
				 */
				ess.applyPower(ss.getActivePower(), ss.getReactivePower());
			}
		});
	}

	public synchronized void clearCycleConstraints() {
		this.data.clearCycleConstraints();
	}

	private synchronized int getActivePowerExtrema(GoalType goalType) {
		double[] coefficients = this.data.createEmptyCoefficients();
		this.data.getActivePowerCoefficientIndices().forEach(i -> coefficients[i] = 1);
		double[] solution;
		try {
			solution = this.solve(new LinearObjectiveFunction(coefficients, 0), goalType);
			return (int) (this.data.getActivePowerCoefficientIndices().mapToDouble(i -> solution[i]).sum());
		} catch (PowerException e) {
			log.warn("Unable to " + goalType.name() + " Active Power. Setting it to zero.");
			return 0;
		}
	}

	/**
	 * Gets the maximum possible total Active Power under the active Constraints.
	 */
	public synchronized int getMaxActivePower() {
		return this.getActivePowerExtrema(GoalType.MAXIMIZE);
	}

	/**
	 * Gets the minimum possible total Active Power under the active Constraints.
	 */
	public synchronized int getMinActivePower() {
		return this.getActivePowerExtrema(GoalType.MINIMIZE);
	}

	/**
	 * Returns whether the objective function is solvable under the currently given
	 * constraints.
	 * 
	 * @return
	 */
	public synchronized boolean isSolvable(LinearConstraint... additionalConstraints) {
		try {
			this.solve(this.data.createSimpleObjectiveFunction(), GoalType.MINIMIZE, additionalConstraints);
			return true;
		} catch (PowerException e) {
			switch (e.getType()) {
			case NO_FEASIBLE_SOLUTION:
				return false;
			case UNBOUNDED_SOLUTION:
				return true; // it's unbounded, but still solvable
			}
		}
		throw new IllegalArgumentException("LinearPower.isSolvable() - Should never come here...");
	}

	public synchronized void removeConstraint(Constraint constraint) {
		this.data.removeConstraint(constraint);
	}

	public synchronized void removeEss(ManagedSymmetricEss ess) {
		this.data.removeEss(ess);
	}

	/**
	 * Solves the Objective Function
	 * 
	 * @return
	 * @throws PowerException
	 */
	public synchronized double[] solve(LinearObjectiveFunction objectiveFunction, GoalType goalType,
			LinearConstraint... additionalConstraints) throws PowerException {
		List<LinearConstraint> constraints = this.data.getAllLinearConstraints();
		// log.info("Constraints");
		// for (LinearConstraint c : constraints) {
		// log.info(Utils.linearConstraintToString(c, ""));
		// }
		// log.info("Additional Constraints");
		// for (LinearConstraint c : additionalConstraints) {
		// log.info(Utils.linearConstraintToString(c, ""));
		// }
		// log.info(Utils.objectiveFunctionToString(objectiveFunction, goalType));

		// copy to array (let space for 'additionalConstraints')
		Arrays.stream(additionalConstraints).forEach(c -> constraints.add(c));
		LinearConstraint[] c = Utils.copyToArray(constraints);

		// solve + return result or throw Exception
		SimplexSolver solver = new SimplexSolver();
		try {
			PointValuePair solution = solver.optimize( //
					objectiveFunction, //
					new LinearConstraintSet(c), //
					goalType, //
					PivotSelectionRule.BLAND);
			return solution.getPoint();
		} catch (NoFeasibleSolutionException e) {
			throw new PowerException(e);
		} catch (UnboundedSolutionException e) {
			throw new PowerException(e);
		}
	}

	/**
	 * Solves the Objective Function
	 * 
	 * @return
	 * @throws PowerException
	 */
	public synchronized double[] solve(LinearObjectiveFunction objectiveFunction, GoalType goalType,
			List<LinearConstraint> additionalConstraints) throws PowerException {
		LinearConstraint[] cs = new LinearConstraint[additionalConstraints.size()];
		for (int i = 0; i < additionalConstraints.size(); i++) {
			cs[i] = additionalConstraints.get(i);
		}
		return this.solve(objectiveFunction, goalType, cs);
	}

	/**
	 * Gets Constraints for "0" if there is no definitive Constraint existing yet.
	 * 
	 * @return
	 */
	public List<Constraint> getNullConstraints() {
		List<Coefficient> allPossibleCoefficients = new ArrayList<>();
		for (ManagedSymmetricEss ess : this.data.realEsss) {
			for (Pwr pwr : Pwr.values()) {
				Stream.of(Phase.L1, Phase.L2, Phase.L3)
						.forEach(phase -> allPossibleCoefficients.add(new Coefficient(ess, phase, pwr, 1)));
			}
		}

		/*
		 * Remove Constraints from temporary list, that are already covered. i.e.
		 * Relationship is EQUALS and Constraint-Value is != 0
		 */
		for (Constraint constraint : this.data.getAllConstraints()) {
			if (constraint.getRelationship() != Relationship.EQ || constraint.getValue().orElse(0d) == 0) {
				continue;
			}
			for (Coefficient thisCoefficient : constraint.getCoefficients()) {
				Iterator<Coefficient> i = allPossibleCoefficients.iterator();
				while (i.hasNext()) {
					Coefficient possibleCoefficient = i.next();
					if (Utils.coefficientIsCoveredBy(possibleCoefficient, thisCoefficient.getEss(), thisCoefficient.getPhase(),
							thisCoefficient.getPwr())) {
						i.remove();
					}
				}
			}
		}

		// For every Possible Coefficient that is left: create a Null-Constraint
		// (Coefficient == 0) and return it
		List<Constraint> result = new ArrayList<>();
		for (Coefficient coefficient : allPossibleCoefficients) {
//			log.info("Add Null-Constraint for [" + coefficient.getEss().id() + "], " + coefficient.getPwr().name()
//					+ ", " + coefficient.getPhase().name());
			result.add(new Constraint(ConstraintType.CYCLE, new Coefficient[] { coefficient }, Relationship.EQ, 0));
		}
		return result;
	}

	/**
	 * Solves the linear objective function and tries to optimize the result as much
	 * as possible
	 * 
	 * @return
	 * @throws Exception
	 */
	private synchronized double[] solveOptimally() {
		/*
		 * Test if Objective Function is solvable. Otherwise return zeros.
		 */
		if (!this.isSolvable()) {
			return this.data.createEmptyCoefficients();
		}

		/**
		 * If there is no Constraint for an Ess, add a "0"-Constraint
		 */
		this.getNullConstraints().forEach(constraint -> this.addConstraint(constraint));

		/*
		 * Try to solve with Constraints to keep Ess1 == Ess2 == Ess3
		 */
		try {
			List<LinearConstraint> constraints = new ArrayList<>();
			for (int i = 2; i < this.data.getNoOfCoefficients(); i += 2) {
				// Active Power
				double[] coefficients = this.data.createEmptyCoefficients();
				coefficients[0] = 1;
				coefficients[i] = -1;
				constraints.add(new LinearConstraint(coefficients, Relationship.EQ, 0));
				// Reactive Power
				coefficients = this.data.createEmptyCoefficients();
				coefficients[1] = 1;
				coefficients[i + 1] = -1;
				constraints.add(new LinearConstraint(coefficients, Relationship.EQ, 0));
			}
			return this.solve(this.data.createSimpleObjectiveFunction(), GoalType.MINIMIZE, constraints);
		} catch (PowerException e) {
			// Error -> next try
		}

		for (GoalType goalType : new GoalType[] { GoalType.MINIMIZE, GoalType.MAXIMIZE }) {
			/**
			 * <ul>
			 * <li>try to MINIMIZE p >= 0; then MINIMIZE q >= 0
			 * <li>Fails? try to MAXIMIZE p >= 0; then MAXIMIZE q >= 0
			 * </ul>
			 */
			try {
				double[] coefficients = this.data.createEmptyCoefficients();

				// solve function that finds extremal for p
				this.data.getActivePowerCoefficientIndices().forEach(i -> coefficients[i] = 1);
				final double[] solution = this.solve(new LinearObjectiveFunction(coefficients, 0), goalType,
						this.data.createConstraintsForQuadrantI());

				// set result as fixed values for p and try to solve q.
				final List<LinearConstraint> pConstraints = new ArrayList<>();
				this.data.getActivePowerCoefficientIndices().forEach(i -> {
					double[] cs = this.data.createEmptyCoefficients();
					cs[i] = 1;
					pConstraints.add(new LinearConstraint(cs, Relationship.EQ, solution[i]));
				});
				pConstraints.addAll(Arrays.asList(this.data.createConstraintsForQuadrantI()));
				return this.solve(this.data.createSimpleObjectiveFunction(), goalType, pConstraints);
			} catch (PowerException e) {
				// Error -> next try
			}
		}

		for (GoalType goalType : new GoalType[] { GoalType.MINIMIZE, GoalType.MAXIMIZE }) {
			/**
			 * <ul>
			 * <li>Try to MINIMIZE without additional constraints
			 * <li>Fails? try to MAXIMIZE without additional constraints
			 * </ul>
			 */
			try {
				return this.solve(this.data.createSimpleObjectiveFunction(), goalType);
			} catch (PowerException e) {
				// Error -> next try
			}
		}

		/*
		 * We should never get here... Objective Function is not solvable -> return
		 * zeros
		 */
		return this.data.createEmptyCoefficients();

		// TODO evaluate if this should be migrated from old implementation
		// /*
		// * Avoid extreme changes in active/reactive power
		// *
		// * calculate the delta between last set power and current calculation and
		// apply
		// * it only partly
		// */
		// int activePowerDelta = (int) c.x - this.lastActivePower + 1 /* add 1 to avoid
		// rounding issues */;
		// int reactivePowerDelta = (int) c.y - this.lastReactivePower + 1 /* add 1 to
		// avoid rounding issues */;
		// activePower = this.lastActivePower + activePowerDelta / 2;
		// reactivePower = this.lastReactivePower + reactivePowerDelta / 2;
	}

}
