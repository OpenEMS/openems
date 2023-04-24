package io.openems.edge.ess.generic.offgrid;

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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.common.AbstractGenericManagedEss;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.offgrid.statemachine.Context;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine;
import io.openems.edge.ess.generic.offgrid.statemachine.StateMachine.OffGridState;
import io.openems.edge.ess.generic.symmetric.ChannelManager;
import io.openems.edge.ess.generic.symmetric.GenericManagedSymmetricEss;
import io.openems.edge.ess.offgrid.api.OffGridEss;
import io.openems.edge.ess.offgrid.api.OffGridSwitch;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Generic.OffGrid", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class GenericOffGridEssImpl
		extends AbstractGenericManagedEss<GenericManagedSymmetricEss, Battery, ManagedSymmetricBatteryInverter>
		implements GenericManagedSymmetricEss, OffGridEss, GenericManagedEss, ManagedSymmetricEss, SymmetricEss,
		OpenemsComponent, EventHandler, StartStoppable, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(GenericOffGridEssImpl.class);

	/**
	 * Manages the {@link OffGridState}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(OffGridState.UNDEFINED);
	private final ChannelManager channelManager = new ChannelManager(this);

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private OffGridBatteryInverter batteryInverter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private Battery battery;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private OffGridSwitch offGridSwitch;

	public GenericOffGridEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				GenericManagedEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				GenericOffGridEss.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), this.cm, config.batteryInverter_id(),
				config.battery_id(), config.startStop());

		// update filter for 'Off Grid Switch'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "offGridSwitch",
				config.offGridSwitch_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.getChannelManager().deactivate();
		super.deactivate();
	}

	@Override
	protected void handleStateMachine() {
		// Store the current State
		this.channel(GenericOffGridEss.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// TODO check if grid switched -> then force next state;
		// tell to GridSwitch-State via Context, that grid switched and handle the logic
		// that is currently in StartedInOffGrid

		// Prepare Context
		var context = new Context(this, this.getBattery(), this.getBatteryInverter(), this.getOffGridSwitch(),
				this.componentManager);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(GenericOffGridEss.ChannelId.RUN_FAILED).setNextValue(false);
		} catch (OpenemsNamedException e) {
			this.channel(GenericOffGridEss.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public String debugLog() {
		return super.genericDebugLog() //
				.append("|").append(this.channel(GenericOffGridEss.ChannelId.STATE_MACHINE).value().asOptionString()) //
				.append("|").append(this.getGridModeChannel().value().asOptionString()) //
				.toString();
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
	protected OffGridBatteryInverter getBatteryInverter() {
		return this.batteryInverter;
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	protected OffGridSwitch getOffGridSwitch() {
		return this.offGridSwitch;
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(OffGridState.UNDEFINED);
		}
	}
	
	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				StartStoppable.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(GenericOffGridEss.class, accessMode, 100) //
						.channel(0, GenericOffGridEss.ChannelId.STATE_MACHINE, ModbusType.UINT16) //
						.build());
	}
}
