package io.openems.edge.ess.core.power;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
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
				"id=_power", //
				"enabled=true", //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
		})
public class PowerComponent extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler, Power {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * The duration needed for solving the Power
		 * 
		 * <ul>
		 * <li>Interface: PowerComponent
		 * <li>Type: Integer
		 * <li>Unit: milliseconds
		 * <li>Range: positive
		 * </ul>
		 */
		SOLVE_DURATION(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLISECONDS)),
		/**
		 * Whether the Power problem could be solved
		 * 
		 * <ul>
		 * <li>Interface: PowerComponent
		 * <li>Type: Boolean
		 * </ul>
		 */
		SOLVED(new Doc().type(OpenemsType.BOOLEAN));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	private final ChocoPower power;

	public PowerComponent() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		this.power = new ChocoPower(this);
	}

	@Activate
	void activate(ComponentContext context, Map<String, Object> properties) {
		super.activate(context, "_power", "_power", true);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

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

	protected BooleanReadChannel getSolvedChannel() {
		return this.channel(ChannelId.SOLVED);
	}

	protected IntegerReadChannel getSolveDurationChannel() {
		return this.channel(ChannelId.SOLVE_DURATION);
	}

}
