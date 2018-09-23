package io.openems.edge.ess.core.power;
//package io.openems.edge.ess.core.power;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Set;
//import java.util.function.Consumer;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//import java.util.stream.Stream;
//
//import org.apache.commons.math3.optim.linear.LinearConstraint;
//import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
//import org.apache.commons.math3.optim.linear.Relationship;
//
//import io.openems.edge.ess.api.ManagedAsymmetricEss;
//import io.openems.edge.ess.api.ManagedSymmetricEss;
//import io.openems.edge.ess.api.MetaEss;
//import io.openems.edge.ess.power.api.Coefficient;
//import io.openems.edge.ess.power.api.Constraint;
//import io.openems.edge.ess.power.api.ConstraintType;
//import io.openems.edge.ess.power.api.Phase;
//import io.openems.edge.ess.power.api.Pwr;
//
//public class Data {
//
//	/**
//	 * Holds all ManagedSymmetricEss objects covered by this Power object
//	 */
//	protected final Set<ManagedSymmetricEss> allEsss = new HashSet<>();
//
//	private final int COEFFICIENTS_PER_ESS = 6;
//
//	/**
//	 * Holds the cycle constraints. Those constraints are cleared on every Cycle by
//	 * the applyPower()-method.
//	 */
//	private final List<Constraint> cycleConstraints = new ArrayList<>();
//
//	/**
//	 * Holds all MetaEss objects
//	 */
//	protected final Set<MetaEss> metaEsss = new HashSet<>();
//
//	/**
//	 * Holds all ManagedSymmetricEss objects that represent physical ESS (i.e. no
//	 * MetaEss).
//	 */
//	protected final Set<ManagedSymmetricEss> realEsss = new HashSet<>();
//

//
//	/**
//	 * Recursive helper method for getEssIndex
//	 * 
//	 * @param ess
//	 * @param retry
//	 * @return
//	 */
//	private synchronized int _getEssIndex(ManagedSymmetricEss ess) {
//		boolean found = false;
//		int essIndex = 0;
//		for (ManagedSymmetricEss realEss : this.realEsss) {
//			if (realEss.equals(ess)) {
//				found = true;
//				break;
//			} else {
//				essIndex += COEFFICIENTS_PER_ESS;
//			}
//		}
//		if (found) {
//			return essIndex;
//		}
//
//		// not found -> add ess
//		throw new IllegalArgumentException(
//				"Ess [" + ess.id() + "; " + ess.hashCode() + "] was not found in the system.");
//	}
//
//	/**
//	 * Add a Constraint
//	 * 
//	 * @param constraint
//	 * @return
//	 */
//	public synchronized Constraint addConstraint(Constraint constraint) {
//		this.getConstraintListForType(constraint.getType()).add(constraint);
//		return constraint;
//	}
//
//	/**
//	 * Add an ManagedSymmetricEss
//	 * 
//	 * @param ess
//	 */
//	public synchronized void addEss(ManagedSymmetricEss ess) {
//		boolean wasAlreadyAdded = !this.allEsss.add(ess);
//		if (wasAlreadyAdded) {
//			return;
//		}
//		if (ess instanceof MetaEss) {
//			this.metaEsss.add((MetaEss) ess);
//
//		} else {
//			if (!(this.realEsss.contains(ess))) {
//				this.realEsss.add(ess);
//
//				if (ess instanceof ManagedAsymmetricEss) {
//					// nothing
//				} else {
//					/*
//					 * ManagedSymmetricEss: all phases need to be equal
//					 */
//					this.addConstraint(new Constraint( //
//							ConstraintType.STATIC, //
//							new Coefficient[] { //
//									new Coefficient(ess, Phase.L1, Pwr.ACTIVE, 1), //
//									new Coefficient(ess, Phase.L2, Pwr.ACTIVE, -1), //
//							}, Relationship.EQ, 0));
//					this.addConstraint(new Constraint( //
//							ConstraintType.STATIC, //
//							new Coefficient[] { //
//									new Coefficient(ess, Phase.L1, Pwr.ACTIVE, 1), //
//									new Coefficient(ess, Phase.L3, Pwr.ACTIVE, -1), //
//							}, Relationship.EQ, 0));
//					this.addConstraint(new Constraint( //
//							ConstraintType.STATIC, //
//							new Coefficient[] { //
//									new Coefficient(ess, Phase.L1, Pwr.REACTIVE, 1), //
//									new Coefficient(ess, Phase.L2, Pwr.REACTIVE, -1), //
//							}, Relationship.EQ, 0));
//					this.addConstraint(new Constraint( //
//							ConstraintType.STATIC, //
//							new Coefficient[] { //
//									new Coefficient(ess, Phase.L1, Pwr.REACTIVE, 1), //
//									new Coefficient(ess, Phase.L3, Pwr.REACTIVE, -1), //
//							}, Relationship.EQ, 0));
//				}
//			}
//		}
//	}
//
//	/**
//	 * Removes a ManagedSymmetricEss
//	 * 
//	 * @param ess
//	 */
//	public synchronized void removeEss(ManagedSymmetricEss ess) {
//		this.allEsss.remove(ess);
//		if (ess instanceof MetaEss) {
//			this.metaEsss.remove((MetaEss) ess);
//
//		} else {
//			/*
//			 * find all existing Constraints for this Ess and remove them
//			 */
//			Consumer<Iterator<Constraint>> constraintHandler = (i) -> {
//				while (i.hasNext()) {
//					Constraint ct = i.next();
//					boolean constraintHasThisEss = Stream.of(ct.getCoefficients()) //
//							.anyMatch(co -> co.getEss().equals(ess));
//					if (constraintHasThisEss) {
//						i.remove();
//						// NOTE: what happens if this Constraint is for several Ess? Should we keep it
//						// and only remove the matching coefficients?
//					}
//				}
//			};
//			constraintHandler.accept(this.staticConstraints.iterator());
//			constraintHandler.accept(this.cycleConstraints.iterator());
//
//			this.realEsss.remove(ess);
//		}
//	}
//
//	/**
//	 * Add a simple Constraint
//	 * 
//	 * @param ess
//	 * @param type
//	 * @param phase
//	 * @param pwr
//	 * @param relationship
//	 * @param value
//	 * @return
//	 */
//	public synchronized Constraint addSimpleConstraint(ManagedSymmetricEss ess, ConstraintType type, Phase phase,
//			Pwr pwr, Relationship relationship, int value) {
//		return this.addConstraint(Utils.createSimpleConstraint(ess, type, phase, pwr, relationship, value));
//	}
//
//	/**
//	 * Clear Cycle constraints, keeping only the 'staticConstraints' for next Cycle.
//	 */
//	public synchronized void clearCycleConstraints() {
//		this.cycleConstraints.clear();
//	}
//
//	/**
//	 * Get Indices of ActivePower coefficients
//	 * 
//	 * @return
//	 */
//	public synchronized IntStream getActivePowerCoefficientIndices() {
//		return IntStream.iterate(0, i -> i + 2).limit(this.getNoOfCoefficients() / 2);
//	}
//
//	/**
//	 * Gets all Constraints (Static + Cycle)
//	 * 
//	 * @return
//	 */
//	public synchronized List<Constraint> getAllConstraints() {
//		return Stream.concat(this.staticConstraints.stream(), this.cycleConstraints.stream())
//				.collect(Collectors.toList());
//	}
//
//	/**
//	 * Gets all Constraints as LinearConstraints
//	 * 
//	 * @return
//	 */
//	public synchronized List<LinearConstraint> getAllLinearConstraints() {
//		List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
//		for (Constraint c : this.getAllConstraints()) {
//			LinearConstraint lc = this.toLinearConstraint(c);
//			if (lc != null) {
//				constraints.add(lc);
//			}
//		}
//		return constraints;
//	}
//
//	/**
//	 * Get the correct list for the ConstraintType
//	 * 
//	 * @param type
//	 * @return
//	 */
//	private synchronized List<Constraint> getConstraintListForType(ConstraintType type) {
//		switch (type) {
//		case STATIC:
//			return this.staticConstraints;
//		case CYCLE:
//			return this.cycleConstraints;
//		}
//		throw new IllegalArgumentException("This should never happen!");
//	}
//
//	/**
//	 * Create Constraints to keep all coefficients in Quadrant I
//	 * 
//	 * @return
//	 */
//	public synchronized LinearConstraint[] createConstraintsForQuadrantI() {
//		LinearConstraint[] result = new LinearConstraint[this.getNoOfCoefficients()];
//		for (int i = 0; i < this.getNoOfCoefficients(); i++) {
//			double[] coefficients = this.createEmptyCoefficients();
//			coefficients[i] = 1;
//			result[i] = new LinearConstraint(coefficients, Relationship.GEQ, 0);
//		}
//		return result;
//	}
//
//	/**
//	 * Creates an empty Coefficient array
//	 * 
//	 * @return
//	 */
//	public synchronized double[] createEmptyCoefficients() {
//		return new double[this.getNoOfCoefficients()];
//	}
//
//	/**
//	 * Gets the coefficient start index for the given ManagedSymmetricEss - if it is
//	 * available
//	 * 
//	 * @param ess
//	 * @return
//	 */
//	public synchronized int getEssIndex(ManagedSymmetricEss ess) {
//		return this._getEssIndex(ess);
//	}
//
//	/**
//	 * Gets the total number of Coefficients
//	 */
//	public synchronized int getNoOfCoefficients() {
//		return this.realEsss.size() * COEFFICIENTS_PER_ESS;
//	}
//
//	/**
//	 * Creates a Simple Objective Function: 1*p1 + 1*q1 + 1*p2 + 1*q2 + ...
//	 * 
//	 * @return
//	 */
//	public synchronized LinearObjectiveFunction createSimpleObjectiveFunction() {
//		double[] c = this.createEmptyCoefficients();
//		for (int i = 0; i < c.length; i++) {
//			c[i] = 1;
//		}
//		return new LinearObjectiveFunction(c, 0);
//	}
//
//	/**
//	 * Removes a Constraint
//	 * 
//	 * @param constraint
//	 */
//	public synchronized void removeConstraint(Constraint constraint) {
//		if (constraint == null) {
//			return;
//		}
//		this.getConstraintListForType(constraint.getType()).remove(constraint);
//	}
//
//	/**
//	 * Creates a LinearConstraint - suitable for linear optimization problem - from
//	 * a OpenEMS Constraint object
//	 * 
//	 * @param constraint
//	 * @return
//	 */
//	protected synchronized LinearConstraint toLinearConstraint(Constraint constraint) {
//		if (constraint.isEnabled()) {
//			double[] coefficients = this.createEmptyCoefficients();
//			for (Coefficient coefficient : constraint.getCoefficients()) {
//				this.getCoefficients(coefficients, coefficient.getEss(), coefficient.getPhase(), coefficient.getPwr(),
//						coefficient.getValue());
//			}
//			return new LinearConstraint(coefficients, constraint.getRelationship(), constraint.getValue().get());
//		} else {
//			return null;
//		}
//	}
//
//	/**
//	 * Helper for toLinearConstraint-method. Creates the coefficients array for a
//	 * LinearConstraint from given meta data
//	 * 
//	 * @param coefficients
//	 * @param ess
//	 * @param phase
//	 * @param pwr
//	 * @param value
//	 */
//	private synchronized void getCoefficients(double[] coefficients, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
//			double value) {
//		if (ess instanceof MetaEss) {
//			for (ManagedSymmetricEss subEss : ((MetaEss) ess).getEsss()) {
//				this.getCoefficients(coefficients, subEss, phase, pwr, value);
//			}
//			return;
//		}
//
//		int essIndex = this.getEssIndex(ess);
//		int pwrOffset = pwr.getOffset();
//		switch (phase) {
//		case ALL:
//			coefficients[essIndex + Phase.L1.getOffset() + pwrOffset] = value;
//			coefficients[essIndex + Phase.L2.getOffset() + pwrOffset] = value;
//			coefficients[essIndex + Phase.L3.getOffset() + pwrOffset] = value;
//			break;
//		case L1:
//		case L2:
//		case L3:
//			coefficients[essIndex + phase.getOffset() + pwrOffset] = value;
//			break;
//		}
//	}
//}
