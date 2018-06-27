package io.openems.edge.ess.power;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.optim.linear.Relationship;

import io.openems.edge.ess.asymmetric.api.ManagedAsymmetricEss;
import io.openems.edge.ess.symmetric.api.ManagedSymmetricEss;

public class Power extends AbstractPower {

	public Power(ManagedSymmetricEss... esss) {
		super(esss);
	}

	/**
	 * Adds a constraint for total Active Power
	 * 
	 * @param relationship
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setActivePower(ConstraintType constraintType, Relationship relationship,
			int activePower) throws PowerException {
		CoefficientOneConstraint constraint = new CoefficientOneConstraint(this.noOfCoefficients, this.pIndices,
				relationship, activePower);
		this.addConstraint(constraintType, constraint);
		return constraint;
	}

	/**
	 * Adds a constraint for Active Power for given Ess
	 * 
	 * @param relationship
	 * @param ess
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setActivePower(ConstraintType constraintType, Relationship relationship,
			ManagedSymmetricEss ess, int activePower) throws PowerException {
		this.validateEss(ess);
		int[] indices = new int[0];
		if (ess instanceof ManagedAsymmetricEss) {
			/*
			 * ManagedAsymmetricEss
			 */
			indices = new int[] { this.startIndices.get(ess), this.startIndices.get(ess) + Phase.L2.getOffset(),
					this.startIndices.get(ess) + Phase.L3.getOffset() };
		} else {
			/*
			 * ManagedSymmetricEss
			 */
			indices = new int[] { this.startIndices.get(ess) };
		}
		CoefficientOneConstraint constraint = new CoefficientOneConstraint(this.noOfCoefficients, indices, relationship,
				activePower);
		this.addConstraint(constraintType, constraint);
		return constraint;
	}

	/**
	 * Adds a constraint for Active Power on given phase for given Ess
	 * 
	 * @param relationship
	 * @param ess
	 * @param phase
	 * @param activePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setActivePower(ConstraintType constraintType, Relationship relationship,
			ManagedAsymmetricEss ess, Phase phase, int activePower) throws PowerException {
		this.validateEss(ess);
		int[] indices = new int[] { this.startIndices.get(ess) + phase.getOffset() };
		CoefficientOneConstraint constraint = new CoefficientOneConstraint(this.noOfCoefficients, indices, relationship,
				activePower);
		this.addConstraint(constraintType, constraint);
		return constraint;
	}

	/**
	 * Adds a constraint for total Reactive Power
	 * 
	 * @param relationship
	 * @param reactivePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setReactivePower(ConstraintType constraintType, Relationship relationship,
			int reactivePower) throws PowerException {
		CoefficientOneConstraint constraint = new CoefficientOneConstraint(this.noOfCoefficients, this.qIndices,
				relationship, reactivePower);
		this.addConstraint(constraintType, constraint);
		return constraint;
	}

	/**
	 * Adds a constraint for Reactive Power for given Ess
	 * 
	 * @param relationship
	 * @param ess
	 * @param reactivePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setReactivePower(ConstraintType constraintType, Relationship relationship,
			ManagedSymmetricEss ess, int reactivePower) throws PowerException {
		this.validateEss(ess);
		int[] indices = new int[0];
		if (ess instanceof ManagedAsymmetricEss) {
			/*
			 * ManagedAsymmetricEss
			 */
			indices = new int[] { this.startIndices.get(ess) + 1, this.startIndices.get(ess) + 3,
					this.startIndices.get(ess) + 5 };
		} else {
			/*
			 * ManagedSymmetricEss
			 */
			indices = new int[] { this.startIndices.get(ess) + 1 };
		}
		CoefficientOneConstraint constraint = new CoefficientOneConstraint(this.noOfCoefficients, indices, relationship,
				reactivePower);
		this.addConstraint(constraintType, constraint);
		return constraint;
	}

	/**
	 * Adds a constraint for Reactive Power on given phase for given Ess
	 * 
	 * @param relationship
	 * @param ess
	 * @param phase
	 * @param reactivePower
	 * @return
	 * @throws PowerException
	 */
	public CoefficientOneConstraint setReactivePower(ConstraintType constraintType, Relationship relationship,
			ManagedAsymmetricEss ess, Phase phase, int reactivePower) throws PowerException {
		validateEss(ess);
		int[] indices = new int[] { this.startIndices.get(ess) + 1 + phase.getOffset() };
		CoefficientOneConstraint constraint = new CoefficientOneConstraint(this.noOfCoefficients, indices, relationship,
				reactivePower);
		this.addConstraint(constraintType, constraint);
		return constraint;
	}

	/**
	 * Adds a constraint for Max Apparent Power
	 * 
	 * @param ess
	 * @param maxApparentPower
	 * @return
	 * @throws PowerException
	 */
	public List<CircleConstraint> setMaxApparentPower(ManagedSymmetricEss ess, int maxApparentPower)
			throws PowerException {
		validateEss(ess);
		int essIndex = this.startIndices.get(ess);
		List<CircleConstraint> returnConstraints = new ArrayList<>();

		if (ess instanceof ManagedAsymmetricEss) {
			/*
			 * ManagedAsymmetricEss
			 */
			for (Phase phase : Phase.values()) {
				int pIndex = essIndex + phase.getOffset();
				int qIndex = pIndex + 1;
				CircleConstraint constraint = new CircleConstraint(this.noOfCoefficients, pIndex, qIndex,
						maxApparentPower);
				this.addConstraint(ConstraintType.STATIC, constraint);
				returnConstraints.add(constraint);
			}
		} else {
			/*
			 * ManagedSymmetricEss
			 */
			int pIndex = essIndex;
			int qIndex = pIndex + 1;
			CircleConstraint constraint = new CircleConstraint(this.noOfCoefficients, pIndex, qIndex, maxApparentPower);
			this.addConstraint(ConstraintType.STATIC, constraint);
			returnConstraints.add(constraint);
		}
		return returnConstraints;
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

}
