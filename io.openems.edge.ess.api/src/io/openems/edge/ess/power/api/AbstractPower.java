package io.openems.edge.ess.power.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.asymmetric.api.ManagedAsymmetricEss;
import io.openems.edge.ess.symmetric.api.ManagedSymmetricEss;

public abstract class AbstractPower {

	// private final Logger log = LoggerFactory.getLogger(AbstractPower.class);

	/**
	 * Holds all Ess objects covered by this Power object
	 */
	private final Ess[] esss;

	/**
	 * Holds the Objective Function for the solver
	 */
	private final LinearObjectiveFunction objectiveFunction;

	/**
	 * Holds the total number of coefficients
	 */
	protected final int noOfCoefficients;

	/**
	 * Holds Constraints were all coefficients are in Quadrant I (all >= 0)
	 */
	protected final LinearConstraint[] coefficientsInQuadrantI;

	/**
	 * Holds Constraints were all coefficients are in Quadrant III (all <= 0)
	 */
	protected final LinearConstraint[] coefficientsInQuadrantIII;

	/**
	 * Holds the indices of coefficents for Active Power (P)
	 */
	protected final int[] pIndices;

	/**
	 * Holds the indices of coefficents for Reactive Power (P)
	 */
	protected final int[] qIndices;

	/**
	 * Holds the starting index for each Ess
	 */
	protected final Map<Ess, Integer> startIndices;;

	/**
	 * Holds the static constraints. Those constraints stay forever. They can be
	 * adjusted by keeping a reference and calling the setValue() method.
	 */
	private final List<AbstractConstraint> staticConstraints = new ArrayList<>();

	/**
	 * Holds the cycle constraints. Those constraints are cleared on every Cycle by
	 * the applyPower()-method.
	 */
	private final List<AbstractConstraint> cycleConstraints = new ArrayList<>();

	public AbstractPower(ManagedSymmetricEss... esss) {
		this.esss = esss;

		/*
		 * Initialize Objective Function and final helper variables
		 */
		List<Double> coefficents = new ArrayList<>();
		List<Integer> pIndices = new ArrayList<>();
		List<Integer> qIndices = new ArrayList<>();
		Map<Ess, Integer> startIndices = new HashMap<>();
		int index = 0;
		for (Ess ess : esss) {
			startIndices.put(ess, index);
			coefficents.add(1d); // P L1
			pIndices.add(index);

			coefficents.add(1d); // Q L1
			qIndices.add(index + 1);

			coefficents.add(1d); // P L2
			pIndices.add(index + 2);

			coefficents.add(1d); // Q L2
			qIndices.add(index + 3);

			coefficents.add(1d); // P L3
			pIndices.add(index + 4);

			coefficents.add(1d); // Q L3
			qIndices.add(index + 5);

			index += 6;
		}
		// Copy coefficients to array
		double[] c = new double[coefficents.size()];
		for (int i = 0; i < coefficents.size(); i++) {
			c[i] = coefficents.get(i);
		}
		// create objective function
		this.objectiveFunction = new LinearObjectiveFunction(c, 0);
		// store helpers
		this.noOfCoefficients = coefficents.size();
		this.pIndices = new int[pIndices.size()];
		for (int i = 0; i < pIndices.size(); i++) {
			this.pIndices[i] = pIndices.get(i);
		}
		this.qIndices = new int[qIndices.size()];
		for (int i = 0; i < qIndices.size(); i++) {
			this.qIndices[i] = qIndices.get(i);
		}
		this.startIndices = Collections.unmodifiableMap(startIndices);
		// create helper constraints
		this.coefficientsInQuadrantI = new LinearConstraint[noOfCoefficients];
		this.coefficientsInQuadrantIII = new LinearConstraint[noOfCoefficients];
		for (int i = 0; i < noOfCoefficients; i++) {
			double[] coefficients = new double[this.noOfCoefficients];
			;
			coefficients[i] = 1;
			this.coefficientsInQuadrantI[i] = new LinearConstraint(coefficients, Relationship.GEQ, 0);
			this.coefficientsInQuadrantIII[i] = new LinearConstraint(coefficients, Relationship.GEQ, 0);
		}
		/*
		 * Create initial constraints
		 */
		for (Ess ess : esss) {
			if (ess instanceof ManagedAsymmetricEss) {
				/*
				 * ManagedAsymmetricEss
				 */
			} else {
				/*
				 * ManagedSymmetricEss: Add Constraint to keep L1 == L2 == L3
				 */
				int i = this.startIndices.get(ess);

				// p1 - p2 = 0
				double[] coefficients = new double[this.noOfCoefficients];
				coefficients[i + Phase.L1.getOffset() + Pwr.ACTIVE.getOffset()] = 1;
				coefficients[i + Phase.L2.getOffset() + Pwr.ACTIVE.getOffset()] = -1;
				this.addConstraint(ConstraintType.STATIC, //
						new LinearConstraintWrapper(coefficients, Relationship.EQ, 0,
								"Keep L1 == L2 for SymmetricEss"));

				// p1 - p3 = 0
				coefficients = new double[this.noOfCoefficients];
				coefficients[i + Phase.L1.getOffset() + Pwr.ACTIVE.getOffset()] = 1;
				coefficients[i + Phase.L3.getOffset() + Pwr.ACTIVE.getOffset()] = -1;
				this.addConstraint(ConstraintType.STATIC, //
						new LinearConstraintWrapper(coefficients, Relationship.EQ, 0,
								"Keep L1 == L3 for SymmetricEss"));

				// q1 - q2 = 0
				coefficients = new double[this.noOfCoefficients];
				coefficients[i + Phase.L1.getOffset() + Pwr.REACTIVE.getOffset()] = 1;
				coefficients[i + Phase.L2.getOffset() + Pwr.REACTIVE.getOffset()] = -1;
				this.addConstraint(ConstraintType.STATIC, //
						new LinearConstraintWrapper(coefficients, Relationship.EQ, 0,
								"Keep L1 == L2 for SymmetricEss"));

				// q1 - q3 = 0
				coefficients = new double[this.noOfCoefficients];
				coefficients[i + Phase.L1.getOffset() + Pwr.REACTIVE.getOffset()] = 1;
				coefficients[i + Phase.L3.getOffset() + Pwr.REACTIVE.getOffset()] = -1;
				this.addConstraint(ConstraintType.STATIC, //
						new LinearConstraintWrapper(coefficients, Relationship.EQ, 0,
								"Keep L1 == L3 for SymmetricEss"));
			}
		}

		// TODO add constraint to keep every coefficient < Integer.MAX
	}

	/**
	 * Merges Static and Cycle Constraints
	 * 
	 * @return
	 */
	private List<LinearConstraint> getAllConstraints() {
		List<LinearConstraint> constraints = new ArrayList<>();
		Stream.concat(this.staticConstraints.stream(), this.cycleConstraints.stream()).forEachOrdered(constraint -> {
			for (LinearConstraint linearConstraint : constraint.getConstraints()) {
				constraints.add(linearConstraint);
			}
		});
		return constraints;
	}

	/**
	 * Adds a Constraint.
	 * 
	 * @param type
	 * @param constraint
	 */
	public synchronized <T extends AbstractConstraint> T addConstraint(ConstraintType type, T constraint) {
		// log.debug("Add " + type.name() + ": " +
		// Utils.abstractConstraintToString(constraint));

		// get correct list for ConstraintType
		List<AbstractConstraint> constraints;
		switch (type) {
		case STATIC:
			constraints = this.staticConstraints;
			break;
		case CYCLE:
			constraints = this.cycleConstraints;
			break;
		default:
			throw new IllegalArgumentException("This should never happen!");
		}
		// Add constraint to list
		constraints.add(constraint);
		return constraint;
	}

	/**
	 * Removes a Constraint.
	 * 
	 * @param type
	 * @param constraint
	 */
	private synchronized void removeConstraint(ConstraintType type, AbstractConstraint constraint) {
		// get correct list for ConstraintType
		List<AbstractConstraint> constraints;
		switch (type) {
		case STATIC:
			constraints = this.staticConstraints;
			break;
		case CYCLE:
			constraints = this.cycleConstraints;
			break;
		default:
			throw new IllegalArgumentException("This should never happen!");
		}
		// Add constraint to list
		constraints.remove(constraint);
	}

	/**
	 * Adds a Constraint and tries to solve the Objective Function including the new
	 * constraint. If solving is ok, adds the Constraint to the list of constraints.
	 * Otherwise removes it again.
	 *
	 * @param type
	 * @param constraint
	 * @return the original constraint
	 * @throws PowerException
	 *             if solving fails
	 */
	protected synchronized <T extends AbstractConstraint> T addConstraintAndSolve(ConstraintType type, T constraint)
			throws PowerException {
		this.addConstraint(type, constraint);
		if (!this.isSolvable()) {
			this.removeConstraint(type, constraint);
			throw new PowerException(new NoFeasibleSolutionException(), constraint);
		}
		return constraint;
	}

	/**
	 * Returns whether the objective function is solvable under the currently given
	 * constraints.
	 * 
	 * @return
	 */
	public synchronized boolean isSolvable() {
		// copy to array
		List<LinearConstraint> constraints = this.getAllConstraints();
		LinearConstraint[] c = new LinearConstraint[constraints.size()];
		for (int i = 0; i < constraints.size(); i++) {
			c[i] = constraints.get(i);
		}

		// solve
		SimplexSolver solver = new SimplexSolver();
		try {
			solver.optimize(this.objectiveFunction, new LinearConstraintSet(c), GoalType.MINIMIZE,
					PivotSelectionRule.BLAND);
			return true;
		} catch (UnboundedSolutionException e) {
			return true; // it's unbounded, but still solvable
		} catch (NoFeasibleSolutionException e) {
			return false;
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
		List<LinearConstraint> constraints = this.getAllConstraints();
		LinearConstraint[] c = new LinearConstraint[constraints.size() + additionalConstraints.length];
		for (int i = 0; i < constraints.size(); i++) {
			c[i] = constraints.get(i);
		}
		System.arraycopy(additionalConstraints, 0, c, constraints.size(), additionalConstraints.length);

		// solve + return result or throw Exception
		SimplexSolver solver = new SimplexSolver();
		try {
			PointValuePair solution = solver.optimize(objectiveFunction, new LinearConstraintSet(c), goalType,
					PivotSelectionRule.BLAND);
			return solution.getPoint();
		} catch (NoFeasibleSolutionException e) {
			throw new PowerException(e);
		} catch (UnboundedSolutionException e) {
			throw new PowerException(e);
		}
	}

	private synchronized double[] solveOptimally() {
		/*
		 * Text if Objective Function is solvable. Otherwise return zeros.
		 */
		if (!this.isSolvable()) {
			return new double[this.noOfCoefficients];
		}

		for (GoalType goalType : new GoalType[] { GoalType.MINIMIZE, GoalType.MAXIMIZE }) {
			/**
			 * <ul>
			 * <li>try to MINIMIZE p >= 0; then MINIMIZE q >= 0
			 * <li>Fails? try to MAXIMIZE p >= 0; then MAXIMIZE q >= 0
			 * </ul>
			 */
			try {
				double[] coefficients = new double[this.noOfCoefficients];

				// solve function that finds extremal for p
				Arrays.stream(this.pIndices).forEach(i -> coefficients[i] = 1);
				final double[] solution = this.solve(new LinearObjectiveFunction(coefficients, 0), goalType,
						this.coefficientsInQuadrantI);

				// set result as fixed values for p and try to solve q.
				final List<LinearConstraint> pConstraints = new ArrayList<>();
				Arrays.stream(this.pIndices).forEach(i -> {
					double[] cs = new double[this.noOfCoefficients];
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
			/*
			 * 
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
		return new double[this.noOfCoefficients];
	}

	/**
	 * This is the final method of the Power class:
	 * <ul>
	 * <li>It solves the Objective Function
	 * <li>It calls the applyPower() methods of each Ess
	 * </ul>
	 */
	public synchronized void applyPower() {
		// log.debug("ApplyPower [" + Arrays.stream(this.esss).map(ess ->
		// ess.id()).collect(Collectors.joining(", "))
		// + "], Static [" + this.staticConstraints.size() + "], Cycle [" +
		// this.cycleConstraints.size() + "]");
		//
		// log.debug("Static Constraints");
		// for (AbstractConstraint c : this.staticConstraints) {
		// log.debug(c.toString());
		// }
		// log.debug("Cycle Constraints");
		// for (AbstractConstraint c : this.cycleConstraints) {
		// log.debug(c.toString());
		// }

		// solve optimally
		double[] solution = this.solveOptimally();

		// log.debug(Utils.solutionToString(solution));

		/*
		 * forward result to Ess
		 */
		int i = 0;
		for (Ess ess : this.esss) {
			if (ess instanceof ManagedAsymmetricEss) {
				/*
				 * ManagedAsymmetricEss
				 */
				ManagedAsymmetricEss e = (ManagedAsymmetricEss) ess;

				// Active Power
				int activePowerL1 = this.roundToInverterPrecision(e,
						solution[i + Pwr.ACTIVE.getOffset() + Phase.L1.getOffset()]);
				int activePowerL2 = this.roundToInverterPrecision(e,
						solution[i + Pwr.ACTIVE.getOffset() + Phase.L2.getOffset()]);
				int activePowerL3 = this.roundToInverterPrecision(e,
						solution[i + Pwr.ACTIVE.getOffset() + Phase.L3.getOffset()]);
				int reactivePowerL1 = this.roundToInverterPrecision(e,
						solution[i + Pwr.REACTIVE.getOffset() + Phase.L1.getOffset()]);
				int reactivePowerL2 = this.roundToInverterPrecision(e,
						solution[i + Pwr.REACTIVE.getOffset() + Phase.L2.getOffset()]);
				int reactivePowerL3 = this.roundToInverterPrecision(e,
						solution[i + Pwr.REACTIVE.getOffset() + Phase.L3.getOffset()]);

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
				int activePower = this.roundToInverterPrecision(e, //
						// L1 Active Power
						solution[i + Pwr.ACTIVE.getOffset() + Phase.L1.getOffset()]
								// L2 Active Power
								+ solution[i + Pwr.ACTIVE.getOffset() + Phase.L2.getOffset()]
								// L3 Active Power
								+ solution[i + Pwr.ACTIVE.getOffset() + Phase.L3.getOffset()]);
				int reactivePower = this.roundToInverterPrecision(e, //
						// L1 Reactive Power
						solution[i + Pwr.REACTIVE.getOffset() + Phase.L1.getOffset()]
								// L2 Reactive Power
								+ solution[i + Pwr.REACTIVE.getOffset() + Phase.L2.getOffset()]
								// L3 Reactive Power
								+ solution[i + Pwr.REACTIVE.getOffset() + Phase.L3.getOffset()]);

				// set debug channels on parent
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER).setNextValue(activePower);
				ess.channel(ManagedSymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER).setNextValue(reactivePower);

				e.applyPower(activePower, reactivePower);
			}
			i += 6;
		}

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

	/**
	 * Clear Cycle constraints, keeping only the 'staticConstraints' for next Cycle.
	 * 
	 */
	public void clearCycleConstraints() {
		this.cycleConstraints.clear();
	}

	/**
	 * Round values to accuracy of inverter; following this logic:
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
	 */
	private int roundToInverterPrecision(ManagedSymmetricEss ess, double value) {
		Round round = Round.DOWN;
		int precision = ess.getPowerPrecision();
		int soc = ess.getSoc().value().orElse(0);

		if (value > 0 && soc > 50 || value < 0 && soc < 50) {
			round = Round.UP;
		}

		return IntUtils.roundToPrecision((float) value, round, precision);
	}

	@Override
	public String toString() {
		return "Power [" + Stream.of(this.esss).map(ess -> ess.id()).collect(Collectors.joining(", ")) + "]";
	}
}
