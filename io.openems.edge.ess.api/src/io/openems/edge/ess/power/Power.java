package io.openems.edge.ess.power;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.asymmetric.api.ManagedAsymmetricEss;
import io.openems.edge.ess.symmetric.api.ManagedSymmetricEss;

public class Power extends AbstractPower {

	private final Logger log = LoggerFactory.getLogger(Power.class);

	public Power(ManagedSymmetricEss... esss) {
		super(esss);
	}

	/**
	 * Adds a constraint for total Active Power.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setActivePower(ConstraintType constraintType, Relationship relationship,
			int activePower) {
		return this.addConstraint(constraintType, generateConstraint(Pwr.ACTIVE, relationship, activePower));
	}

	/**
	 * Adds a constraint for total Active Power. Throws a PowerException if the
	 * objective function cannot be solved anymore after this Constraint was added.
	 * The Constraint is not added in this case.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setActivePowerAndSolve(ConstraintType constraintType, Relationship relationship,
			int activePower) throws PowerException {
		return this.addConstraintAndSolve(constraintType, generateConstraint(Pwr.ACTIVE, relationship, activePower));
	}

	/**
	 * Adds a constraint for Active Power for a given Ess.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setActivePower(ConstraintType constraintType, Relationship relationship,
			ManagedSymmetricEss ess, int activePower) {
		return this.addConstraint(constraintType, generateConstraint(Pwr.ACTIVE, relationship, ess, activePower));
	}

	/**
	 * Adds a constraint for Active Power for a given Ess. Throws a PowerException
	 * if the objective function cannot be solved anymore after this Constraint was
	 * added. The Constraint is not added in this case.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setActivePowerAndSolve(ConstraintType constraintType, Relationship relationship,
			ManagedSymmetricEss ess, int activePower) throws PowerException {
		return this.addConstraintAndSolve(constraintType,
				generateConstraint(Pwr.ACTIVE, relationship, ess, activePower));
	}

	/**
	 * Adds a constraint for Active Power for a given Phase of a given Ess.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setActivePower(ConstraintType constraintType, Relationship relationship,
			ManagedAsymmetricEss ess, Phase phase, int activePower) {
		return this.addConstraint(constraintType,
				generateConstraint(Pwr.ACTIVE, relationship, ess, phase, activePower));
	}

	/**
	 * Adds a constraint for Active Power for a given Phase of a given Ess. Throws a
	 * PowerException if the objective function cannot be solved anymore after this
	 * Constraint was added. The Constraint is not added in this case.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setActivePowerAndSolve(ConstraintType constraintType, Relationship relationship,
			ManagedAsymmetricEss ess, Phase phase, int activePower) throws PowerException {
		return this.addConstraintAndSolve(constraintType,
				generateConstraint(Pwr.ACTIVE, relationship, ess, phase, activePower));
	}

	/**
	 * Adds a constraint for total Reactive Power.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setReactivePower(ConstraintType constraintType, Relationship relationship,
			int reactivePower) {
		return this.addConstraint(constraintType, generateConstraint(Pwr.REACTIVE, relationship, reactivePower));
	}

	/**
	 * Adds a constraint for total Reactive Power. Throws a PowerException if the
	 * objective function cannot be solved anymore after this Constraint was added.
	 * The Constraint is not added in this case.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setReactivePowerAndSolve(ConstraintType constraintType, Relationship relationship,
			int reactivePower) throws PowerException {
		return this.addConstraintAndSolve(constraintType,
				generateConstraint(Pwr.REACTIVE, relationship, reactivePower));
	}

	/**
	 * Adds a constraint for Reactive Power for a given Ess.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setReactivePower(ConstraintType constraintType, Relationship relationship,
			ManagedSymmetricEss ess, int reactivePower) {
		return this.addConstraint(constraintType, generateConstraint(Pwr.REACTIVE, relationship, ess, reactivePower));
	}

	/**
	 * Adds a constraint for Reactive Power for a given Ess. Throws a PowerException
	 * if the objective function cannot be solved anymore after this Constraint was
	 * added. The Constraint is not added in this case.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setReactivePowerAndSolve(ConstraintType constraintType, Relationship relationship,
			ManagedSymmetricEss ess, int reactivePower) throws PowerException {
		return this.addConstraintAndSolve(constraintType,
				generateConstraint(Pwr.REACTIVE, relationship, ess, reactivePower));
	}

	/**
	 * Adds a constraint for Reactive Power for a given Phase of a given Ess.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setReactivePower(ConstraintType constraintType, Relationship relationship,
			ManagedAsymmetricEss ess, Phase phase, int reactivePower) {
		return this.addConstraint(constraintType,
				generateConstraint(Pwr.REACTIVE, relationship, ess, phase, reactivePower));
	}

	/**
	 * Adds a constraint for Reactive Power for a given Phase of a given Ess. Throws
	 * a PowerException if the objective function cannot be solved anymore after
	 * this Constraint was added. The Constraint is not added in this case.
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setReactivePowerAndSolve(ConstraintType constraintType, Relationship relationship,
			ManagedAsymmetricEss ess, Phase phase, int reactivePower) throws PowerException {
		return this.addConstraintAndSolve(constraintType,
				generateConstraint(Pwr.REACTIVE, relationship, ess, phase, reactivePower));
	}

	/**
	 * Adds a constraint for Max Apparent Power.
	 * 
	 * @param ess
	 * @param maxApparentPower
	 * @return
	 * @throws PowerException
	 */
	public CircleConstraint[] setMaxApparentPower(ManagedSymmetricEss ess, int maxApparentPower) {
		validateEss(ess);
		int essIndex = this.startIndices.get(ess);
		List<CircleConstraint> returnConstraints = new ArrayList<>();

		if (ess instanceof ManagedAsymmetricEss) {
			/*
			 * ManagedAsymmetricEss
			 */
		} else {
			/*
			 * ManagedSymmetricEss: split max apparent power in three phases
			 */
			maxApparentPower = maxApparentPower / 3;
		}

		// Create Max Apparent Power circle constraint for each phase
		for (Phase phase : Phase.values()) {
			int pIndex = essIndex + phase.getOffset();
			int qIndex = pIndex + 1;
			CircleConstraint constraint = new CircleConstraint(this.noOfCoefficients, pIndex, qIndex, maxApparentPower,
					ess.id() + " " + phase.name() + " Smax = " + maxApparentPower);
			this.addConstraint(ConstraintType.STATIC, constraint);
			returnConstraints.add(constraint);
		}

		// copy to array
		CircleConstraint[] c = new CircleConstraint[returnConstraints.size()];
		for (int i = 0; i < returnConstraints.size(); i++) {
			c[i] = returnConstraints.get(i);
		}
		return c;
	}

	/**
	 * Throws an IllegalArgumentException if this Ess is not registered.
	 * 
	 * @param ess
	 * @throws IllegalArgumentException
	 */
	private void validateEss(ManagedSymmetricEss ess) throws IllegalArgumentException {
		if (ess == null) {
			throw new IllegalArgumentException("Ess is null.");
		}
		if (!this.startIndices.containsKey(ess)) {
			throw new IllegalArgumentException("This Ess is not registered.");
		}
	}

	private CoefficientOneConstraint generateConstraint(Pwr pwr, Relationship relationship, int power) {
		int[] indices;
		switch (pwr) {
		case ACTIVE:
			indices = this.pIndices;
			break;
		case REACTIVE:
			indices = this.qIndices;
			break;
		default:
			throw new IllegalArgumentException("This should never happen!");
		}
		return new CoefficientOneConstraint(this.noOfCoefficients, indices, relationship, power,
				"Set " + pwr.toString() + " " + relationship.toString() + " " + power);
	}

	private CoefficientOneConstraint generateConstraint(Pwr pwr, Relationship relationship, ManagedSymmetricEss ess,
			int power) {
		this.validateEss(ess);
		int[] indices = new int[] { //
				this.startIndices.get(ess) + pwr.getOffset() + Phase.L1.getOffset(), //
				this.startIndices.get(ess) + pwr.getOffset() + Phase.L2.getOffset(), //
				this.startIndices.get(ess) + pwr.getOffset() + Phase.L3.getOffset() };
		return new CoefficientOneConstraint(this.noOfCoefficients, indices, relationship, power,
				"Set " + pwr.toString() + " for [" + ess.id() + "] " + relationship.toString() + " " + power);
	}

	private CoefficientOneConstraint generateConstraint(Pwr pwr, Relationship relationship, ManagedAsymmetricEss ess,
			Phase phase, int power) {
		this.validateEss(ess);
		int[] indices = new int[] { this.startIndices.get(ess) + pwr.getOffset() + phase.getOffset() };
		return new CoefficientOneConstraint(this.noOfCoefficients, indices, relationship, power, "Set " + pwr.toString()
				+ " for [" + ess.id() + "," + phase.name() + "] " + relationship.toString() + " " + power);
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
		double[] coefficients = new double[this.noOfCoefficients];
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
}
