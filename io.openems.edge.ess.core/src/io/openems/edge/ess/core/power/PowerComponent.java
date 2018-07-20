package io.openems.edge.ess.core.power;

import org.apache.commons.math3.optim.linear.Relationship;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;

@Component( //
		immediate = true, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
		})
public class PowerComponent implements EventHandler, Power {

	private final LinearPower linearPower = new LinearPower();

	@Reference( //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	protected synchronized void addEss(ManagedSymmetricEss ess) {
		this.linearPower.addEss(ess);
	}

	protected synchronized void removeEss(ManagedSymmetricEss ess) {
		this.linearPower.removeEss(ess);
	}

	@Override
	public Constraint addSimpleConstraint(ManagedSymmetricEss ess, ConstraintType type, Phase phase, Pwr pwr,
			Relationship relationship, int value) {
		return this.linearPower.addSimpleConstraint(ess, type, phase, pwr, relationship, value);
	}

	@Override
	public Constraint addConstraint(Constraint constraint) {
		return this.linearPower.addConstraint(constraint);
	}

	@Override
	public Constraint addSimpleConstraintAndValidate(ManagedSymmetricEss ess, ConstraintType type, Phase phase, Pwr pwr,
			Relationship relationship, int value) throws PowerException {
		return this.linearPower.addSimpleConstraint(ess, type, phase, pwr, relationship, value);
	}

	@Override
	public Constraint addConstraintAndValidate(Constraint constraint) throws PowerException {
		return this.linearPower.addConstraintAndValidate(constraint);
	}

	@Override
	public void removeConstraint(Constraint constraint) {
		this.linearPower.removeConstraint(constraint);
	}

	@Override
	public int getMaxActivePower() {
		return this.linearPower.getMaxActivePower();
	}

	@Override
	public int getMinActivePower() {
		return this.linearPower.getMinActivePower();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE:
			this.linearPower.applyPower();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
			this.linearPower.clearCycleConstraints();
			break;
		}
	}

}
