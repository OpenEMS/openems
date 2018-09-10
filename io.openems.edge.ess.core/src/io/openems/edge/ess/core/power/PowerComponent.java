package io.openems.edge.ess.core.power;

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
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Component( //
		immediate = true, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
		})
public class PowerComponent implements EventHandler, Power {

	private final ChocoPower power = new ChocoPower();

	@Reference( //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	protected synchronized void addEss(ManagedSymmetricEss ess) {
		this.power.addEss(ess);
	}

	protected synchronized void removeEss(ManagedSymmetricEss ess) {
		this.power.removeEss(ess);
	}

	@Override
	public Constraint addSimpleConstraint(ManagedSymmetricEss ess, ConstraintType type, Phase phase, Pwr pwr,
			Relationship relationship, int value) {
		return this.power.addSimpleConstraint(ess, type, phase, pwr, relationship, value);
	}

	@Override
	public Constraint addConstraint(Constraint constraint) {
		return this.power.addConstraint(constraint);
	}

	@Override
	public void removeConstraint(Constraint constraint) {
		this.power.removeConstraint(constraint);
	}

	@Override
	public int getMaxActivePower() {
		return this.power.getMaxActivePower();
	}

	@Override
	public int getMinActivePower() {
		return this.power.getMinActivePower();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE:
			this.power.applyPower();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
			this.power.initializeNextCycle();
			break;
		}
	}

}
