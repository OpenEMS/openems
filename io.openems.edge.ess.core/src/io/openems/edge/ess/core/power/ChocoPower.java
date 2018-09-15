package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.chocosolver.solver.Solution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Goal;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

/**
 * Wraps Choco solver (http://www.choco-solver.org/). It tries to solve the
 * distribution of Active and Reactive Power among the ESSs using an objective
 * function.
 */
public class ChocoPower implements Power {

	private final Logger log = LoggerFactory.getLogger(ChocoPower.class);

	protected final PowerComponent parent;
	
	/*
	 * Holds a reference to the ChocoPowerWorker
	 */
	private final ChocoPowerWorker worker;

	/**
	 * Holds all EssWrapper objects covered by this Power object
	 */
	protected final Map<ManagedSymmetricEss, EssWrapper> esss = new ConcurrentHashMap<>();

	/**
	 * Holds the cycle constraints. Those constraints are cleared on every Cycle by
	 * the applyPower()-method.
	 */
	private final List<Constraint> cycleConstraints = new ArrayList<>();

	/**
	 * Holds the static constraints. Those constraints stay forever. They can be
	 * adjusted by keeping a reference and calling the setValue() method.
	 */
	private final List<Constraint> staticConstraints = new ArrayList<>();

	public ChocoPower(PowerComponent parent) {
		this.worker = new ChocoPowerWorker(this);
		this.parent = parent;
	}

	/**
	 * Clear Cycle constraints, keeping only the 'staticConstraints' for next Cycle.
	 */
	public synchronized void initializeNextCycle() {
		// clear cycle constraints and optimizers
		this.cycleConstraints.clear();
	}

	/**
	 * Adds a Constraint
	 * 
	 * @param constraint
	 * @return
	 */
	public synchronized Constraint addConstraint(Constraint constraint) {
		this.getConstraintListForType(constraint.getType()).add(constraint);
		return constraint;
	}

	/**
	 * Get the correct list for the ConstraintType
	 * 
	 * @param type
	 * @return
	 */
	private synchronized List<Constraint> getConstraintListForType(ConstraintType type) {
		switch (type) {
		case STATIC:
			return this.staticConstraints;
		case CYCLE:
			return this.cycleConstraints;
		}
		throw new IllegalArgumentException("This should never happen!");
	}

	/**
	 * Gets all Constraints (Static + Cycle)
	 * 
	 * @return
	 */
	public synchronized Stream<Constraint> getAllConstraints() {
		return Stream.concat(this.staticConstraints.stream(), this.cycleConstraints.stream());
	}

	/**
	 * Add a ManagedSymmetricEss
	 * 
	 * @param ess
	 */
	public synchronized void addEss(ManagedSymmetricEss ess) {
		boolean hadAlreadyBeenAdded = this.esss.containsKey(ess);
		if (hadAlreadyBeenAdded) {
			return;
		}
		if (ess instanceof MetaEss) {
			return; // do not add MetaEss
		}
		EssWrapper wrapper = new EssWrapper(ess);
		this.esss.put(ess, wrapper);
	}

	/**
	 * Removes a ManagedSymmetricEss
	 * 
	 * @param ess
	 */
	public synchronized void removeEss(ManagedSymmetricEss ess) {
		/*
		 * find all existing Constraints for this Ess and remove them
		 */
		Consumer<Iterator<Constraint>> constraintHandler = (i) -> {
			while (i.hasNext()) {
				Constraint ct = i.next();
				boolean constraintHasThisEss = Stream.of(ct.getCoefficients()) //
						.anyMatch(co -> co.getEss().equals(ess));
				if (constraintHasThisEss) {
					i.remove();
					// NOTE: if this Constraint is for several Ess we remove it completely!
				}
			}
		};
		constraintHandler.accept(this.staticConstraints.iterator());
		constraintHandler.accept(this.cycleConstraints.iterator());

		// remove Ess from wrappers
		this.esss.remove(ess);
	}

	/**
	 * Adds a Simple Constraint
	 */
	public synchronized Constraint addSimpleConstraint(ManagedSymmetricEss ess, ConstraintType type, Phase phase,
			Pwr pwr, Relationship relationship, int value) {
		return this.addConstraint(//
				new Constraint( //
						type, new Coefficient[] { //
								new Coefficient(ess, phase, pwr, 1) }, //
						relationship, //
						value));
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
	 * This is the final method of the Power class. It should never be called
	 * manually, as it is getting called by the framework.
	 * <ul>
	 * <li>It solves the Objective Function
	 * <li>It calls the applyPower() methods of each Ess (via EssWrapper)
	 * </ul>
	 * 
	 * @throws Exception
	 */
	public synchronized void applyPower() {
		Solution solution = this.worker.solve();

		if (!this.esss.isEmpty() && solution == null) {
			log.warn("Unable to find a Solution under the current constraints!");
			this.esss.keySet().forEach(e -> {
				log.warn("- Ess [" + e.id() + "]: " //
						+ "AllowedCharge [" + e.getAllowedCharge().value() + "] " //
						+ "AllowedDischarge [" + e.getAllowedDischarge().value() + "] " //
						+ "MaxApparent [" + e.getMaxApparentPower().value() + "]");
			});
			this.getAllConstraints().forEachOrdered(c -> {
				log.warn("- Constraint: " + c.toString());
			});
		}

		this.esss.values().forEach(wrapper -> {
			wrapper.applyPower(this, solution);
		});
	}

	/**
	 * Gets the maximum possible total Active Power under the active Constraints.
	 */
	public synchronized int getMaxActivePower() {
		return this.worker.getActivePowerExtrema(Goal.MAXIMIZE);
	}

	/**
	 * Gets the minimum possible total Active Power under the active Constraints.
	 */
	public synchronized int getMinActivePower() {
		return this.worker.getActivePowerExtrema(Goal.MINIMIZE);
	}
}
