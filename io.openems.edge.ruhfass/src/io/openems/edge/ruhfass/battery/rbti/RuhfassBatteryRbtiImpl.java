package io.openems.edge.ruhfass.battery.rbti;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.concurrent.atomic.AtomicReference;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.protection.BatteryVoltageProtection;
import io.openems.edge.battery.protection.BatteryVoltageProtectionLimits;
import io.openems.edge.battery.protection.BatteryVoltageProtectionLimits.BatteryVoltageSpecification;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ruhfass.battery.rbti.statemachine.Context;
import io.openems.edge.ruhfass.battery.rbti.statemachine.StateMachine;
import io.openems.edge.ruhfass.battery.rbti.statemachine.StateMachine.State;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ruhfass.Battery.RBTI", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE })
public class RuhfassBatteryRbtiImpl extends AbstractOpenemsModbusComponent implements RuhfassBatteryRbti, Battery,
		ModbusComponent, BatteryVoltageProtection, EventHandler, ModbusSlave, StartStoppable, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private static final int CAPACITY = 161_100; // [Wh]
	private static final int INNER_RESISTANCE = 150; // [mOhm]

	private final Logger log = LoggerFactory.getLogger(RuhfassBatteryRbtiImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);
	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);
	private final BatteryVoltageProtectionLimits<RuhfassBatteryRbtiImpl> voltageProtectionLimits;

	private Config config = null;
	private BatteryProtection batteryProtection = null;

	public RuhfassBatteryRbtiImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryProtection.ChannelId.values(), //
				BatteryVoltageProtection.ChannelId.values(), //
				RuhfassBatteryRbti.ChannelId.values() //
		);
		setValue(this, Battery.ChannelId.CAPACITY, CAPACITY);
		setValue(this, Battery.ChannelId.INNER_RESISTANCE, INNER_RESISTANCE);
		this.voltageProtectionLimits = new BatteryVoltageProtectionLimits<>(getBatteryVoltageSpecification(), this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), 1 /* modbusUnitId */, this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.batteryProtection = BatteryProtection.create(this)//
				.applyBatteryProtectionDefinition(new BatteryProtectionDefinition(), this.componentManager).build();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var o = this.config.batteryChannel().offset;

		return new ModbusProtocol(this, //
				new FC6WriteRegisterTask(o, //
						m(RuhfassBatteryRbti.ChannelId.SET_EV_RESIDUAL_CAN_SIMULATION, new UnsignedWordElement(o))), //
				new FC6WriteRegisterTask(o + 1, //
						m(RuhfassBatteryRbti.ChannelId.SET_HV_BATTERY_ACTIVATION_REQUEST,
								new UnsignedWordElement(o + 1))), //

				new FC3ReadRegistersTask(o + 64, Priority.LOW, //
						m(RuhfassBatteryRbti.ChannelId.BATTERY_CELL_TYPE, new UnsignedQuadruplewordElement(o + 64)), //
						m(RuhfassBatteryRbti.ChannelId.TOTAL_NUMBER_OF_CELLS, new UnsignedWordElement(o + 68))) //
		);
	}

	@Override
	public String debugLog() {
		return Battery.generateDebugLog(this, this.stateMachine);
	}

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	@Override
	public StartStop getStartStopTarget() {
		return switch (this.config.startStop()) {
		case AUTO -> this.startStopTarget.get();
		case START -> StartStop.START;
		case STOP -> StartStop.STOP;
		};
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(RuhfassBatteryRbtiImpl.class, accessMode, 100) //
						.build());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.batteryProtection.apply();
			this.voltageProtectionLimits.updateLimits();
		}
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			this.handleStateMachine();
		}
		}
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		setValue(this, RuhfassBatteryRbti.ChannelId.STATE_MACHINE, this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		try {
			var context = new Context(this, this.componentManager.getClock());

			// Call the StateMachine
			this.stateMachine.run(context);
			setValue(this, RuhfassBatteryRbti.ChannelId.RUN_FAILED, false);

		} catch (OpenemsNamedException e) {
			setValue(this, RuhfassBatteryRbti.ChannelId.RUN_FAILED, true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	private static BatteryVoltageSpecification getBatteryVoltageSpecification() {
		final var cells = 26;
		final var modules = 9;
		final var maximumCellVoltageLimit = 4175;
		final var minimumCellVoltageLimit = 3143;
		final var maximumCellVoltageOperationLimit = 4130;
		final var minimumCellVoltageOperationLimit = 3250;
		final var maximumChargeVoltage = 964;
		final var minimumDischargeVoltage = 763;
		return new BatteryVoltageSpecification(cells, modules, maximumCellVoltageLimit, minimumCellVoltageLimit,
				maximumCellVoltageOperationLimit, minimumCellVoltageOperationLimit, maximumChargeVoltage,
				minimumDischargeVoltage);
	}
}
