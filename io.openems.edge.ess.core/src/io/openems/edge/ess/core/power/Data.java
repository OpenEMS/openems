package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.Relationship;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class Data {

	/**
	 * Holds all ManagedSymmetricEss objects covered by this Power object
	 */
	protected final BiMap<String, ManagedSymmetricEss> allEsss = HashBiMap.create();

	private final int COEFFICIENTS_PER_ESS = 6;

	/**
	 * Holds the cycle constraints. Those constraints are cleared on every Cycle by
	 * the applyPower()-method.
	 */
	private final List<Constraint> cycleConstraints = new ArrayList<>();

	/**
	 * Holds all MetaEss objects
	 */
	protected final Set<MetaEss> metaEsss = new HashSet<>();

	/**
	 * Holds all ManagedSymmetricEss objects that represent physical ESS (i.e. no
	 * MetaEss).
	 */
	protected final ListMultimap<ManagedSymmetricEss, Constraint> realEsss = MultimapBuilder.linkedHashKeys()
			.arrayListValues().build();

	/**
	 * Holds the static constraints. Those constraints stay forever. They can be
	 * adjusted by keeping a reference and calling the setValue() method.
	 */
	private final List<Constraint> staticConstraints = new ArrayList<>();

	/**
	 * Recursive helper method for getEssIndex
	 * 
	 * @param ess
	 * @param retry
	 * @return
	 */
	private synchronized Optional<Integer> _getEssIndex(ManagedSymmetricEss ess, boolean retry) {
		boolean found = false;
		int essIndex = 0;
		for (ManagedSymmetricEss realEss : this.realEsss.keys()) {
			if (realEss.equals(ess)) {
				found = true;
				break;
			} else {
				essIndex += COEFFICIENTS_PER_ESS;
			}
		}
		if (found) {
			return Optional.of(essIndex);
		}

		// not found -> re-add all Ess. This adds Sub-Ess of MetaEss.
		this.realEsss.clear();
		this.allEsss.values().forEach(e -> this.addEss(e));
		if (retry) {
			return this._getEssIndex(ess, false);
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Add a Constraint
	 * 
	 * @param constraint
	 * @return
	 */
	public synchronized Constraint addConstraint(Constraint constraint) {
		this.getConstraintListForType(constraint.getType()).add(constraint);
		return constraint;
	}

	/**
	 * Add an ManagedSymmetricEss
	 * 
	 * @param ess
	 */
	public synchronized void addEss(ManagedSymmetricEss ess) {
		this.allEsss.put(ess.id(), ess);
		if (ess instanceof MetaEss) {
			MetaEss e = (MetaEss) ess;
			this.metaEsss.add(e);
			for (ManagedSymmetricEss subEss : e.getEsss()) {
				this.addEss(subEss);
			}
		} else {
			if (!(this.realEsss.containsKey(ess))) {
				this.realEsss.put(ess, null);

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
			}
		}
	}

	/**
	 * Add a simple Constraint
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
		return this.addConstraint(Utils.createSimpleConstraint(ess, type, phase, pwr, relationship, value));
	}

	/**
	 * Clear Cycle constraints, keeping only the 'staticConstraints' for next Cycle.
	 */
	public void clearCycleConstraints() {
		this.cycleConstraints.clear();
	}

	/**
	 * Get Indices of ActivePower coefficients
	 * 
	 * @return
	 */
	public IntStream getActivePowerCoefficientIndices() {
		return IntStream.iterate(0, i -> i + 2).limit(this.getNoOfCoefficients() / 2);
	}

	/**
	 * Gets all Constraints (Static + Cycle)
	 * 
	 * @return
	 */
	public Stream<Constraint> getAllConstraints() {
		return Stream.concat(this.staticConstraints.stream(), this.cycleConstraints.stream());
	}

	/**
	 * Gets all Constraints as LinearConstraints
	 * 
	 * @return
	 */
	public List<LinearConstraint> getAllLinearConstraints() {
		List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		this.getAllConstraints().forEachOrdered(c -> {
			LinearConstraint lc = Utils.toLinearConstraint(this, c);
			if (lc != null) {
				constraints.add(lc);
			}
		});
		return constraints;
	}

	/**
	 * Get the correct list for the ConstraintType
	 * 
	 * @param type
	 * @return
	 */
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
	 * Create Constraints to keep all coefficients in Quadrant I
	 * 
	 * @return
	 */
	public LinearConstraint[] createConstraintsForQuadrantI() {
		LinearConstraint[] result = new LinearConstraint[this.getNoOfCoefficients()];
		for (int i = 0; i < this.getNoOfCoefficients(); i++) {
			double[] coefficients = this.createEmptyCoefficients();
			coefficients[i] = 1;
			result[i] = new LinearConstraint(coefficients, Relationship.GEQ, 0);
		}
		return result;
	}

	/**
	 * Creates an empty Coefficient array
	 * 
	 * @return
	 */
	public double[] createEmptyCoefficients() {
		return new double[this.getNoOfCoefficients()];
	}

	/**
	 * Gets the coefficient start index for the given ManagedSymmetricEss
	 * 
	 * @param ess
	 * @return
	 */
	public synchronized Optional<Integer> getEssIndex(ManagedSymmetricEss ess) {
		return this._getEssIndex(ess, true);
	}

	/**
	 * Gets the total number of Coefficients
	 */
	public int getNoOfCoefficients() {
		return this.realEsss.size() * COEFFICIENTS_PER_ESS;
	}

	/**
	 * Creates a Simple Objective Function: 1*p1 + 1*q1 + 1*p2 + 1*q2 + ...
	 * 
	 * @return
	 */
	public LinearObjectiveFunction createSimpleObjectiveFunction() {
		double[] c = this.createEmptyCoefficients();
		for (int i = 0; i < c.length; i++) {
			c[i] = 1;
		}
		return new LinearObjectiveFunction(c, 0);
	}

	/**
	 * Removes a Constraint
	 * 
	 * @param constraint
	 */
	public synchronized void removeConstraint(Constraint constraint) {
		if (constraint == null) {
			return;
		}
		this.getConstraintListForType(constraint.getType()).remove(constraint);
	}

	/**
	 * Removes a ManagedSymmetricEss
	 * 
	 * @param ess
	 */
	public synchronized void removeEss(ManagedSymmetricEss ess) {
		this.allEsss.remove(ess.id(), ess);
		if (ess instanceof MetaEss) {
			MetaEss e = (MetaEss) ess;
			this.metaEsss.remove(e);
			for (ManagedSymmetricEss subEss : e.getEsss()) {
				this.removeEss(subEss);
			}
		} else {
			List<Constraint> constraints = this.realEsss.get(ess);
			if (constraints != null) {
				for (Constraint constraint : constraints) {
					this.removeConstraint(constraint);
				}
			}
			this.realEsss.removeAll(ess);
		}
	}

}
