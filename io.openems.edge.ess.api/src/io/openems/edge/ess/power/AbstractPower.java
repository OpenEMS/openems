package io.openems.edge.ess.power;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.asymmetric.api.ManagedAsymmetricEss;
import io.openems.edge.ess.symmetric.api.ManagedSymmetricEss;

public abstract class AbstractPower {

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
	private final LinearConstraint[] coefficientsInQuadrantI;

	/**
	 * Holds Constraints were all coefficients are in Quadrant III (all <= 0)
	 */
	private final LinearConstraint[] coefficientsInQuadrantIII;

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
			if (ess instanceof ManagedAsymmetricEss) {
				/*
				 * ManagedAsymmetricEss: Add P/Q for each phase L1/2/3
				 */
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
			} else {
				/*
				 * ManagedSymmetricEss: Add P/Q
				 */
				coefficents.add(1d); // P
				pIndices.add(index);

				coefficents.add(1d); // Q
				qIndices.add(index + 1);

				index += 2;
			}
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
			double[] coefficients = getZeroCoefficients();
			coefficients[i] = 1;
			this.coefficientsInQuadrantI[i] = new LinearConstraint(coefficients, Relationship.GEQ, 0);
			this.coefficientsInQuadrantIII[i] = new LinearConstraint(coefficients, Relationship.GEQ, 0);
		}
		/*
		 * Create initial constraints
		 */
		// TODO add constraint to keep every coefficient < Integer.MAX
	}

	private double[] getZeroCoefficients() {
		double[] coefficients = new double[this.noOfCoefficients];
		for (int i = 0; i < coefficients.length; i++) {
			coefficients[i] = 0;
		}
		return coefficients;
	}

	/**
	 * Solves the Objective Function
	 * 
	 * @return
	 * @throws PowerException
	 */
	public synchronized double[] solve() throws PowerException {
		SimplexSolver solver = new SimplexSolver();

		// merge static + cycle constraints
		List<LinearConstraint> linearConstraints = new ArrayList<>();
		Stream.concat(this.staticConstraints.stream(), this.cycleConstraints.stream()).forEachOrdered(constraint -> {
			for (LinearConstraint linearConstraint : constraint.getConstraints()) {
				linearConstraints.add(linearConstraint);
			}
		});

		// copy to array (let space for 'noOfCoefficients' LinearConstraints)
		LinearConstraint[] c = new LinearConstraint[linearConstraints.size() + noOfCoefficients];
		for (int i = 0; i < linearConstraints.size(); i++) {
			c[i] = linearConstraints.get(i);
		}
		
		/*
		 * Try MINIMIZE of the function (with each coefficient >= 0 - Quadrant I)
		 */
		System.arraycopy(this.coefficientsInQuadrantI, 0, c, linearConstraints.size(), noOfCoefficients);
		try {
			PointValuePair solution = solver.optimize(this.objectiveFunction, new LinearConstraintSet(c),
					GoalType.MINIMIZE, PivotSelectionRule.BLAND);
			return solution.getPoint();
		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
			// Error on MINIMIZE -> try MAXIMIZE
		}
		
		/*
		 * Try MAXIMIZE of the function (with each coefficient <= 0 - Quadrant III)
		 */
		System.arraycopy(this.coefficientsInQuadrantIII, 0, c, linearConstraints.size(), noOfCoefficients);
		try {
			PointValuePair solution = solver.optimize(this.objectiveFunction, new LinearConstraintSet(c),
					GoalType.MAXIMIZE, PivotSelectionRule.BLAND);
			return solution.getPoint();
		} catch (NoFeasibleSolutionException e) {
			/*
			 * No Solution possible
			 */
			throw new PowerException("No Solution");
		} catch (UnboundedSolutionException e) {
			/*
			 * Unbounded -> return zeros
			 */
			double[] solution = new double[this.noOfCoefficients];
			for (int i = 0; i < this.noOfCoefficients; i++) {
				solution[i] = 0;
			}
			return solution;
		}
	}

	/**
	 * Tries to solve the Objective Function including the new constraint. If
	 * solving is ok, adds the Constraint to the list of constraints.
	 * 
	 * @param constraint
	 * @throws PowerException
	 *             if solving fails
	 */
	protected synchronized void addConstraint(ConstraintType type, AbstractConstraint constraint)
			throws PowerException {
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
		try {
			this.solve();
		} catch (PowerException e) {
			// remove constraint from list if solving failed + forward exception
			constraints.remove(constraint);
			throw e;
		}
	}

	/**
	 * This is the final method of the Power class:
	 * <ul>
	 * <li>It solves the Objective Function
	 * <li>It calls the applyPower() methods of each Ess
	 * <li>It clears the 'cycleConstraints', keeping only the 'staticConstraints'
	 * for next Cycle.
	 * </ul>
	 */
	public synchronized void applyPower() {
		double[] solution;
		try {
			solution = this.solve();
		} catch (PowerException e) {
			// unable to solve: use zeros
			solution = new double[this.noOfCoefficients];
			for (int i = 0; i < this.noOfCoefficients; i++) {
				solution[i] = 0;
			}
		}

		/*
		 * forward result to Ess
		 */
		int i = 0;
		for (Ess ess : this.esss) {
			if (ess instanceof ManagedAsymmetricEss) {
				/*
				 * ManagedAsymmetricEss
				 */
				((ManagedAsymmetricEss) ess).applyPower( //
						(int) solution[i], (int) solution[i + 1], //
						(int) solution[i + 2], (int) solution[i + 3], //
						(int) solution[i + 4], (int) solution[i + 5] //
				);
				i += 6;
			} else {
				/*
				 * ManagedSymmetricEss
				 */
				((ManagedSymmetricEss) ess).applyPower((int) solution[i], (int) solution[i + 1]);
				i += 2;
			}
		}

		/*
		 * Clear Cycle constraints
		 */
		this.cycleConstraints.clear();

		// TODO from old implementation
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
		// /**
		// * round values to required accuracy by inverter; following this logic:
		// *
		// * On Discharge (Power > 0)
		// *
		// * <ul>
		// * <li>if SoC > 50 %: round up (more discharge)
		// * <li>if SoC < 50 %: round down (less discharge)
		// * </ul>
		// *
		// * On Charge (Power < 0)
		// *
		// * <ul>
		// * <li>if SoC > 50 %: round down (less charge)
		// * <li>if SoC < 50 %: round up (more discharge)
		// * </ul>
		// */
		// Round round = Round.DOWN;
		// Optional<Integer> socOpt = this.parent.getSoc().value().asOptional();
		// if (socOpt.isPresent()) {
		// int soc = socOpt.get();
		// if (activePower > 0 && soc > 50 || activePower < 0 && soc < 50) {
		// round = Round.UP;
		// }
		// }
		// activePower = IntUtils.roundToPrecision(activePower, round, powerPrecision);
		// reactivePower = IntUtils.roundToPrecision(reactivePower, round,
		// powerPrecision);
	}
}
