package io.openems.edge.ess.core.power;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public interface PowerDistributionHandler {

	/**
	 * Called on changed {@link ManagedSymmetricEss}-References.
	 */
	public void onUpdateEsss();

	/**
	 * Called on {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_PROCESS_IMAGE}.
	 */
	public void onAfterProcessImage();
	
	/**
	 * Called on {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_WRITE}.
	 */
	public void onBeforeWriteEvent();

	/**
	 * Called on {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_WRITE}.
	 */
	public void onAfterWriteEvent();

	/**
	 * Adds a {@link Constraint}.
	 * 
	 * @param constraint the Constraint
	 */
	public void addConstraint(Constraint constraint);

	/**
	 * Adds a {@link Constraint} and validates it.
	 * 
	 * @param constraint the Constraint
	 * @throws OpenemsException on validation error
	 */
	public void addConstraintAndValidate(Constraint constraint) throws OpenemsException;

	/**
	 * Gets the given {@link Coefficient}.
	 * 
	 * @param ess   the {@link ManagedSymmetricEss}
	 * @param phase the {@link SingleOrAllPhase}
	 * @param pwr   the {@link Pwr}
	 * @return the {@link Coefficient}
	 * @throws OpenemsException on error
	 */
	public Coefficient getCoefficient(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr) throws OpenemsException;

	/**
	 * Creates a simple {@link Constraint}.
	 * 
	 * @param description  a description
	 * @param ess          the {@link ManagedSymmetricEss}
	 * @param phase        the {@link SingleOrAllPhase}
	 * @param pwr          the {@link Pwr}
	 * @param relationship the {@link Relationship}
	 * @param value        the value
	 * @return the {@link Constraint}
	 * @throws OpenemsException on error
	 */
	public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, SingleOrAllPhase phase,
			Pwr pwr, Relationship relationship, double value) throws OpenemsException;

	/**
	 * Removes the given {@link Constraint}.
	 * 
	 * @param constraint the Constraint
	 */
	public void removeConstraint(Constraint constraint);

	/**
	 * Gets the extrem value for the given term.
	 * 
	 * @param ess   the {@link ManagedSymmetricEss}
	 * @param phase the {@link SingleOrAllPhase}
	 * @param pwr   the {@link Pwr}
	 * @param goal  the {@link GoalType}
	 * @return the value
	 */
	public int getPowerExtrema(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr, GoalType goal);
}
