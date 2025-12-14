package io.openems.edge.ess.generic.symmetric;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.openems.edge.common.cycle.Cycle.DEFAULT_CYCLE_TIME;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State.UNDEFINED;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.session.Role;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.api.BatteryErrorAcknowledge;
import io.openems.edge.batteryinverter.api.BatteryInverterErrorAcknowledge;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.EssErrorAcknowledge;
import io.openems.edge.ess.api.EssErrorAcknowledgeRequest;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.common.AbstractGenericManagedEss;
import io.openems.edge.ess.generic.common.CycleProvider;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.common.RuntimeChannels;
import io.openems.edge.ess.generic.common.RuntimeChannelsProvider;
import io.openems.edge.ess.generic.symmetric.statemachine.Context;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Generic.ManagedSymmetric", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class EssGenericManagedSymmetricImpl
		extends AbstractGenericManagedEss<EssGenericManagedSymmetric, Battery, ManagedSymmetricBatteryInverter>
		implements EssGenericManagedSymmetric, GenericManagedEss, ManagedSymmetricEss, HybridEss, SymmetricEss,
		OpenemsComponent, EventHandler, StartStoppable, ModbusSlave, CycleProvider, EssProtection, EssErrorAcknowledge,
		ComponentJsonApi, TimedataProvider, RuntimeChannels {

	private final Logger log = LoggerFactory.getLogger(EssGenericManagedSymmetricImpl.class);
	private final StateMachine stateMachine = new StateMachine(UNDEFINED);
	private final ChannelManager channelManager = new ChannelManager(this);

	protected final RuntimeChannelsProvider runtimeChannelsProvider = new RuntimeChannelsProvider(this);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private Cycle cycle;

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	private ManagedSymmetricBatteryInverter batteryInverter;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	private Battery battery;

	public EssGenericManagedSymmetricImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				GenericManagedEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				EssGenericManagedSymmetric.ChannelId.values(), //
				EssProtection.ChannelId.values(), //
				EssErrorAcknowledge.ChannelId.values(), //
				RuntimeChannels.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), this.cm, config.batteryInverter_id(),
				config.battery_id(), config.startStop());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void handleStateMachine() {
		// Store the current State
		this._setStateMachine(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		var context = new Context(this, this.getBattery(), this.getBatteryInverter(), this.componentManager.getClock());

		// Call the StateMachine
		try {
			this.stateMachine.run(context);
			this._setRunFailed(false);
		} catch (OpenemsNamedException e) {
			this._setRunFailed(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			this.handleStateMachine();
			this.runtimeChannelsProvider.updateStateTime(this.getState());
		}
		}
	}

	@Override
	public String debugLog() {
		var sb = new StringBuilder(this.stateMachine.debugLog());
		super.genericDebugLog(sb);
		return sb.toString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	protected ChannelManager getChannelManager() {
		return this.channelManager;
	}

	@Override
	protected Battery getBattery() {
		return this.battery;
	}

	@Override
	protected ManagedSymmetricBatteryInverter getBatteryInverter() {
		return this.batteryInverter;
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	public boolean isManaged() {
		return this.batteryInverter.isManaged();
	}

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			this.stateMachine.forceNextState(UNDEFINED);
		}
	}

	@Override
	public int getCycleTime() {
		return this.cycle != null //
				? this.cycle.getCycleTime() //
				: DEFAULT_CYCLE_TIME;
	}

	@Override
	public String toString() {
		return toStringHelper(this) //
				.addValue(this.id()) //
				.toString();
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new EssErrorAcknowledgeRequest(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.ADMIN));
		}, call -> {
			this.executeErrorAcknowledge();
			return EmptyObject.INSTANCE;
		});
	}

	@Override
	public void executeErrorAcknowledge() {
		try {
			this._setTimeoutStartBattery(false);
			this._setTimeoutStartBatteryInverter(false);
			this._setTimeoutStopBattery(false);
			this._setTimeoutStopBatteryInverter(false);

			if (this.battery instanceof BatteryErrorAcknowledge b) {
				b.executeBatteryErrorAcknowledge();
			}

			if (this.batteryInverter instanceof BatteryInverterErrorAcknowledge bi) {
				bi.executeBatteryInverterErrorAcknowledge();
			}

			this.stateMachine.forceNextState(UNDEFINED);
		} catch (Exception e) {
			this.logError(this.log, e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
