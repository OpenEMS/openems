package io.openems.edge.ess.power.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;

/**
 * This holds the the linear solver. It tries to solve the distribution of
 * Active and Reactive Power among the ESSs using a linear objective function.
 * 
 * The LinearSolver is a fast approach to prove that a solution to a problem is
 * available and gives a first solution. It fails in taking in account more
 * complicated constraints that cannot be wrapped in a linear equation.
 */
public class Power {

	private final Logger log = LoggerFactory.getLogger(Power.class);

	/**
	 * Holds the total number of coefficients
	 */
	private final int totalCoefficients;

	/**
	 * Holds all ManagedSymmetricEss objects that represent physical ESS (i.e. no
	 * MetaEss). It is filled by constructor.
	 */
	protected final BiMap<ManagedSymmetricEss, Integer> realEsss = HashBiMap.create();

	/**
	 * Holds all ManagedSymmetricEss objects covered by this Power object
	 */
	private final Set<ManagedSymmetricEss> allEsss = new HashSet<>();

	/**
	 * Holds the indices of coefficients for Active Power (P)
	 */
	private final int[] pIndices;

	/**
	 * Holds the indices of coefficients for Reactive Power (P)
	 */
	private final int[] qIndices;

	/**
	 * Holds the Objective Function for the solver
	 */
	private final LinearObjectiveFunction objectiveFunction;

	/**
	 * Holds the static constraints. Those constraints stay forever. They can be
	 * adjusted by keeping a reference and calling the setValue() method.
	 */
	private final List<Constraint> staticConstraints = new ArrayList<>();

	/**
	 * Holds the cycle constraints. Those constraints are cleared on every Cycle by
	 * the applyPower()-method.
	 */
	private final List<Constraint> cycleConstraints = new ArrayList<>();

	/**
	 * Holds Constraints were all coefficients are in Quadrant I (all >= 0)
	 */
	private final LinearConstraint[] coefficientsInQuadrantI;

	/**
	 * Holds Constraints were all coefficients are in Quadrant III (all <= 0)
	 */
	private final LinearConstraint[] coefficientsInQuadrantIII;

	public Power(ManagedSymmetricEss... esss) {
		int coefficientIndex = 0;
		for (ManagedSymmetricEss ess : esss) {
			coefficientIndex = this.addEss(ess, coefficientIndex);
		}
		this.totalCoefficients = coefficientIndex;

		// create basic objective function
		double[] c = new double[this.totalCoefficients];
		for (int i = 0; i < c.length; i++) {
			c[i] = 1;
		}
		this.objectiveFunction = new LinearObjectiveFunction(c, 0);

		// store p and q indices
		this.pIndices = new int[this.totalCoefficients / 2];
		this.qIndices = new int[this.totalCoefficients / 2];
		for (int i = 0; i < this.totalCoefficients / 2; i++) {
			this.pIndices[i] = i * 2;
			this.qIndices[i] = i * 2 + 1;
		}

		// create helper constraints
		this.coefficientsInQuadrantI = new LinearConstraint[this.totalCoefficients];
		this.coefficientsInQuadrantIII = new LinearConstraint[this.totalCoefficients];
		for (int i = 0; i < this.totalCoefficients; i++) {
			double[] coefficients = new double[this.totalCoefficients];
			coefficients[i] = 1;
			this.coefficientsInQuadrantI[i] = new LinearConstraint(coefficients, Relationship.GEQ, 0);
			this.coefficientsInQuadrantIII[i] = new LinearConstraint(coefficients, Relationship.GEQ, 0);
		}
	}

	private int addEss(ManagedSymmetricEss ess, int coefficientIndex) {
		this.allEsss.add(ess);
		if (ess instanceof MetaEss) {
			for (ManagedSymmetricEss subEss : ((MetaEss) ess).getEsss()) {
				coefficientIndex = this.addEss(subEss, coefficientIndex);
			}
		} else {
			if (!(this.realEsss.containsKey(ess))) {
				this.realEsss.put(ess, coefficientIndex);
				if (ess instanceof ManagedAsymmetricEss) {
					// nothing
				} else {
					/*
					 * ManagedSymmetricEss: all phases need to be equal
					 */
					this.addConstraint(new Constraint( //
							ConstraintType.STATIC, //
							new Coefficient[] { //
									new Coefficient(ess, Phase.L1, Pwr.ACTIVE, 1), //
									new Coefficient(ess, Phase.L2, Pwr.ACTIVE, -1), //
							}, Relationship.EQ, 0));
					this.addConstraint(new Constraint( //
							ConstraintType.STATIC, //
							new Coefficient[] { //
									new Coefficient(ess, Phase.L1, Pwr.ACTIVE, 1), //
									new Coefficient(ess, Phase.L3, Pwr.ACTIVE, -1), //
							}, Relationship.EQ, 0));
					this.addConstraint(new Constraint( //
							ConstraintType.STATIC, //
							new Coefficient[] { //
									new Coefficient(ess, Phase.L1, Pwr.REACTIVE, 1), //
									new Coefficient(ess, Phase.L2, Pwr.REACTIVE, -1), //
							}, Relationship.EQ, 0));
					this.addConstraint(new Constraint( //
							ConstraintType.STATIC, //
							new Coefficient[] { //
									new Coefficient(ess, Phase.L1, Pwr.REACTIVE, 1), //
									new Coefficient(ess, Phase.L3, Pwr.REACTIVE, -1), //
							}, Relationship.EQ, 0));
				}
				coefficientIndex += 6;
			}
		}
		return coefficientIndex;
	}

	/**
	 * Helper for toLinearConstraint-method. Creates the coefficients array for a
	 * LinearConstraint from given meta data
	 * 
	 * @param coefficients
	 * @param ess
	 * @param phase
	 * @param pwr
	 * @param value
	 */
	private void getCoefficients(double[] coefficients, ManagedSymmetricEss ess, Phase phase, Pwr pwr, double value) {
		if (ess instanceof MetaEss) {
			for (ManagedSymmetricEss subEss : ((MetaEss) ess).getEsss()) {
				this.getCoefficients(coefficients, subEss, phase, pwr, value);
			}
			return;
		}
		int essIndex = this.realEsss.get(ess);
		int pwrOffset = pwr.getOffset();
		switch (phase) {
		case ALL:
			coefficients[essIndex + Phase.L1.getOffset() + pwrOffset] = value;
			coefficients[essIndex + Phase.L2.getOffset() + pwrOffset] = value;
			coefficients[essIndex + Phase.L3.getOffset() + pwrOffset] = value;
			break;
		case L1:
		case L2:
		case L3:
			coefficients[essIndex + phase.getOffset() + pwrOffset] = value;
			break;
		}
	}

	/**
	 * Creates a LinearConstraint - suitable for linear optimization problem - from
	 * a OpenEMS Constraint object
	 * 
	 * @param constraint
	 * @return
	 */
	private LinearConstraint toLinearConstraint(Constraint constraint) {
		double[] coefficients = new double[this.totalCoefficients];
		for (Coefficient coefficient : constraint.getCoefficients()) {
			this.getCoefficients(coefficients, coefficient.getEss(), coefficient.getPhase(), coefficient.getPwr(),
					coefficient.getValue());
		}
		return new LinearConstraint(coefficients, constraint.getRelationship(), constraint.getValue());
	}

	/**
	 * Gets all linear constraints from all Ess
	 * 
	 * @return
	 */
	private List<LinearConstraint> getAllLinearConstraints() {
		List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		Stream.concat(this.staticConstraints.stream(), this.cycleConstraints.stream()).forEachOrdered(c -> {
			LinearConstraint lc = toLinearConstraint(c);
			constraints.add(lc);
		});
		return constraints;
	}

	private List<Constraint> getConstraintListForType(ConstraintType type) {
		switch (type) {
		case STATIC:
			return this.staticConstraints;
		case CYCLE:
			return this.cycleConstraints;
		}
		throw new IllegalArgumentException("This should never happen!");
	}

	/**
	 * Adds a Constraint.
	 * 
	 * @param type
	 * @param constraint
	 */
	public synchronized Constraint addConstraint(Constraint constraint) {
		getConstraintListForType(constraint.getType()).add(constraint);
		return constraint;
	}

	/**
	 * Adds a Constraint if the problem is still solvable afterwards.
	 * 
	 * @param type
	 * @param constraint
	 * @throws PowerException
	 */
	public synchronized Constraint addConstraintAndValidate(Constraint constraint) throws PowerException {
		LinearConstraint lc = this.toLinearConstraint(constraint);
		if (!this.isSolvable(lc)) {
			// throws the exception if it is not solvable
			throw new PowerException(new NoFeasibleSolutionException());
		}
		getConstraintListForType(constraint.getType()).add(constraint);
		return constraint;
	}

	/**
	 * Adds a Constraint using a ConstraintBuilder. Make sure to call 'build()' once
	 * finished.
	 * 
	 * @return
	 */
	public ConstraintBuilder addConstraint() {
		return new ConstraintBuilder(this);
	}

	/**
	 * Adds a simple constraint
	 * 
	 * @param ess
	 * @param type
	 * @param phase
	 * @param pwr
	 * @param relationship
	 * @param value
	 * @return
	 */
	public Constraint addSimpleConstraint(ManagedSymmetricEss ess, ConstraintType type, Phase phase, Pwr pwr,
			Relationship relationship, int value) {
		return this.addConstraint(//
				new Constraint( //
						type, new Coefficient[] { //
								new Coefficient(ess, phase, pwr, 1) }, //
						relationship, //
						value));
	}

	/**
	 * Adds a simple constraint if the problem is still solvable afterwards.
	 * 
	 * @param ess
	 * @param type
	 * @param phase
	 * @param pwr
	 * @param relationship
	 * @param value
	 * @return
	 * @throws PowerException
	 */
	public Constraint addSimpleConstraintAndValidate(ManagedSymmetricEss ess, ConstraintType type, Phase phase, Pwr pwr,
			Relationship relationship, int value) throws PowerException {
		return this.addConstraintAndValidate(//
				new Constraint( //
						type, new Coefficient[] { //
								new Coefficient(ess, phase, pwr, 1) }, //
						relationship, //
						value));
	}

	/**
	 * Removes a Constraint.
	 * 
	 * @param type
	 * @param constraint
	 */
	public synchronized void removeConstraint(Constraint constraint) {
		getConstraintListForType(constraint.getType()).remove(constraint);
	}

	/**
	 * Helper: Copies the LinearConstraints list to an array
	 * 
	 * @param constraints
	 * @return
	 */
	private static LinearConstraint[] copyToArray(List<LinearConstraint> constraints) {
		LinearConstraint[] c = new LinearConstraint[constraints.size()];
		for (int i = 0; i < constraints.size(); i++) {
			c[i] = constraints.get(i);
		}
		return c;
	}

	/**
	 * Returns whether the objective function is solvable under the currently given
	 * constraints.
	 * 
	 * @return
	 */
	public boolean isSolvable(LinearConstraint... additionalConstraints) {
		try {
			this.solve(this.objectiveFunction, GoalType.MINIMIZE);
			return true;
		} catch (PowerException e) {
			switch (e.getType()) {
			case NO_FEASIBLE_SOLUTION:
				return false;
			case UNBOUNDED_SOLUTION:
				return true; // it's unbounded, but still solvable
			}
		}
		throw new IllegalArgumentException("AbstractPower.isSolvable() - Should never come here...");
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
	 * Solves the Objective Function
	 * 
	 * @return
	 * @throws PowerException
	 */
	public synchronized double[] solve(LinearObjectiveFunction objectiveFunction, GoalType goalType,
			LinearConstraint... additionalConstraints) throws PowerException {
		// log.debug("Additional Constraints");
		// for (LinearConstraint c : additionalConstraints) {
		// log.debug(Utils.linearConstraintToString(c, ""));
		// }
		// log.debug(Utils.objectiveFunctionToString(objectiveFunction, goalType));

		// copy to array (let space for 'additionalConstraints')
		List<LinearConstraint> constraints = this.getAllLinearConstraints();
		Arrays.stream(additionalConstraints).forEach(c -> constraints.add(c));
		LinearConstraint[] c = copyToArray(constraints);

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
			return new double[this.totalCoefficients];
		}

		/*
		 * Try to solve with Constraints to keep Ess1 == Ess2 == Ess3
		 */
		try {
			List<LinearConstraint> constraints = new ArrayList<>();
			for (int i = 2; i < this.totalCoefficients; i += 2) {
				// Active Power
				double[] coefficients = new double[this.totalCoefficients];
				coefficients[0] = 1;
				coefficients[i] = -1;
				constraints.add(new LinearConstraint(coefficients, Relationship.EQ, 0));
				// Reactive Power
				coefficients = new double[this.totalCoefficients];
				coefficients[1] = 1;
				coefficients[i + 1] = -1;
				constraints.add(new LinearConstraint(coefficients, Relationship.EQ, 0));
			}
			return this.solve(this.objectiveFunction, GoalType.MINIMIZE, constraints);
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
				double[] coefficients = new double[this.totalCoefficients];

				// solve function that finds extremal for p
				Arrays.stream(this.pIndices).forEach(i -> coefficients[i] = 1);
				final double[] solution = this.solve(new LinearObjectiveFunction(coefficients, 0), goalType,
						this.coefficientsInQuadrantI);

				// set result as fixed values for p and try to solve q.
				final List<LinearConstraint> pConstraints = new ArrayList<>();
				Arrays.stream(this.pIndices).forEach(i -> {
					double[] cs = new double[this.totalCoefficients];
					cs[i] = 1;
					pConstraints.add(new LinearConstraint(cs, Relationship.EQ, solution[i]));
				});
				pConstraints.addAll(Arrays.asList(this.coefficientsInQuadrantI));
				return this.solve(this.objectiveFunction, goalType, pConstraints);
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
				return this.solve(this.objectiveFunction, goalType);
			} catch (PowerException e) {
				// Error -> next try
			}
		}

		/*
		 * We should never get here... Objective Function is not solvable -> return
		 * zeros
		 */
		return new double[this.totalCoefficients];
	}

	/**
	 * Clear Cycle constraints, keeping only the 'staticConstraints' for next Cycle.
	 */
	public void clearCycleConstraints() {
		this.cycleConstraints.clear();
	}

	/**
	 * Gets the maximum possible total Active Power under the active Constraints.
	 */
	public int getMaxActivePower() {
		return this.getActivePowerExtrema(GoalType.MAXIMIZE);
	}

	/**
	 * Gets the minimum possible total Active Power under the active Constraints.
	 */
	public int getMinActivePower() {
		return this.getActivePowerExtrema(GoalType.MINIMIZE);
	}

	private int getActivePowerExtrema(GoalType goalType) {
		double[] coefficients = new double[this.totalCoefficients];
		Arrays.stream(this.pIndices).forEach(i -> coefficients[i] = 1);
		double[] solution;
		try {
			solution = this.solve(new LinearObjectiveFunction(coefficients, 0), goalType);
			return (int) (Arrays.stream(this.pIndices).mapToDouble(i -> solution[i]).sum());
		} catch (PowerException e) {
			log.warn("Unable to " + goalType.name() + " Active Power. Setting it to zero.");
			return 0;
		}
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
		double[] solution = this.solveOptimally();

		this.realEsss.forEach((ess, i) -> {
			if (ess instanceof ManagedAsymmetricEss) {
				/*
				 * ManagedAsymmetricEss
				 */
				ManagedAsymmetricEss e = (ManagedAsymmetricEss) ess;

				// Active Power
				int activePowerL1 = Utils.roundToInverterPrecision(e,
						solution[i + Phase.L1.getOffset() + Pwr.ACTIVE.getOffset()]);
				int reactivePowerL1 = Utils.roundToInverterPrecision(e,
						solution[i + Phase.L1.getOffset() + Pwr.REACTIVE.getOffset()]);
				int activePowerL2 = Utils.roundToInverterPrecision(e,
						solution[i + Phase.L2.getOffset() + Pwr.ACTIVE.getOffset()]);
				int reactivePowerL2 = Utils.roundToInverterPrecision(e,
						solution[i + Phase.L2.getOffset() + Pwr.REACTIVE.getOffset()]);
				int activePowerL3 = Utils.roundToInverterPrecision(e,
						solution[i + Phase.L3.getOffset() + Pwr.ACTIVE.getOffset()]);
				int reactivePowerL3 = Utils.roundToInverterPrecision(e,
						solution[i + Phase.L3.getOffset() + Pwr.REACTIVE.getOffset()]);

				// set debug channels on parent
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L1).setNextValue(activePowerL1);
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L1).setNextValue(reactivePowerL1);
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L2).setNextValue(activePowerL2);
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L2).setNextValue(reactivePowerL2);
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER_L3).setNextValue(activePowerL3);
				ess.channel(ManagedAsymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER_L3).setNextValue(reactivePowerL3);
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER)
						.setNextValue(activePowerL1 + activePowerL2 + activePowerL3);
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER)
						.setNextValue(reactivePowerL1 + reactivePowerL2 + reactivePowerL3);

				e.applyPower(activePowerL1, reactivePowerL1, activePowerL2, reactivePowerL2, activePowerL3,
						reactivePowerL3);
			} else {
				/*
				 * ManagedSymmetricEss
				 */
				ManagedSymmetricEss e = (ManagedSymmetricEss) ess;

				// Active Power
				int activePower = Utils.roundToInverterPrecision(e, //
						solution[i + Phase.L1.getOffset() + Pwr.ACTIVE.getOffset()] //
								+ solution[i + Phase.L2.getOffset() + Pwr.ACTIVE.getOffset()] //
								+ solution[i + Phase.L3.getOffset() + Pwr.ACTIVE.getOffset()]); //
				int reactivePower = Utils.roundToInverterPrecision(e, //
						solution[i + Phase.L1.getOffset() + Pwr.REACTIVE.getOffset()] //
								+ solution[i + Phase.L2.getOffset() + Pwr.REACTIVE.getOffset()] //
								+ solution[i + Phase.L3.getOffset() + Pwr.REACTIVE.getOffset()]); //

				// set debug channels on parent
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER).setNextValue(activePower);
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER).setNextValue(reactivePower);

				e.applyPower(activePower, reactivePower);
			}
		});

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
