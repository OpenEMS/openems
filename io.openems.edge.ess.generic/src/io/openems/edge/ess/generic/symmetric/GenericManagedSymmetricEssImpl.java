package io.openems.edge.ess.generic.symmetric;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.symmetric.statemachine.Context;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Generic.ManagedSymmetric", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		} //
)
public class GenericManagedSymmetricEssImpl extends AbstractOpenemsComponent implements GenericManagedSymmetricEss,
		ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, StartStoppable {

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricBatteryInverter batteryInverter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private Battery battery;

	private final Logger log = LoggerFactory.getLogger(GenericManagedSymmetricEssImpl.class);

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	/**
	 * Helper wrapping class to handle everything related to Channels.
	 */
	private final ChannelManager channelHandler = new ChannelManager(this);

	private Config config = null;

	public GenericManagedSymmetricEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				GenericManagedSymmetricEss.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'BatteryInverter'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "batteryInverter",
				config.batteryInverter_id())) {
			return;
		}

		// update filter for 'Battery'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "battery", config.battery_id())) {
			return;
		}

		this.channelHandler.activate(this.battery, this.batteryInverter);
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		this.channelHandler.deactivate();
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.handleStateMachine();
			break;
		}
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		// Store the current State
		this.channel(GenericManagedSymmetricEss.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		Context context = new Context(this, this.battery, this.batteryInverter, this.config);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(GenericManagedSymmetricEss.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(GenericManagedSymmetricEss.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" //
				+ this.getAllowedChargePower().asStringWithoutUnit() + ";" //
				+ this.getAllowedDischargePower().asString() //
				+ "|" + this.channel(GenericManagedSymmetricEss.ChannelId.STATE_MACHINE).value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	/**
	 * Forwards the power request to the {@link SymmetricBatteryInverter}.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		this.batteryInverter.run(this.battery, activePower, reactivePower);
	}

	/**
	 * Retrieves PowerPrecision from {@link SymmetricBatteryInverter}.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public int getPowerPrecision() {
		return this.batteryInverter.getPowerPrecision();
	}

	/**
	 * Retrieves StaticConstraints from {@link SymmetricBatteryInverter}.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public Constraint[] getStaticConstraints() throws OpenemsNamedException {

		List<Constraint> result = new ArrayList<Constraint>();

		// Get BatteryInverterConstraints
		BatteryInverterConstraint[] constraints = this.batteryInverter.getStaticConstraints();

		for (int i = 0; i < constraints.length; i++) {
			BatteryInverterConstraint c = constraints[i];
			result.add(this.power.createSimpleConstraint(c.description, this, c.phase, c.pwr, c.relationship, c.value));
		}

		// If the GenericEss is not in State "STARTED" block ACTIVE and REACTIVE Power!
		if (!this.isStarted()) {
			result.add(this.createPowerConstraint("ActivePower Constraint ESS not Started", Phase.ALL, Pwr.ACTIVE,
					Relationship.EQUALS, 0));
			result.add(this.createPowerConstraint("ReactivePower Constraint ESS not Started", Phase.ALL, Pwr.REACTIVE,
					Relationship.EQUALS, 0));
		}
		return result.toArray(new Constraint[result.size()]);
	}

	private AtomicReference<StartStop> startStopTarget = new AtomicReference<StartStop>(StartStop.UNDEFINED);

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	@Override
	public StartStop getStartStopTarget() {
		switch (this.config.startStop()) {
		case AUTO:
			// read StartStop-Channel
			return this.startStopTarget.get();

		case START:
			// force START
			return StartStop.START;

		case STOP:
			// force STOP
			return StartStop.STOP;
		}

		assert false;
		return StartStop.UNDEFINED; // can never happen
	}

}
