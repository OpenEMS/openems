package io.openems.edge.battery.pylontech.powercubem2;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.Context;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine;
import io.openems.edge.battery.pylontech.powercubem2.statemachine.StateMachine.State;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery.Pylontech", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class PylontechPowercubeM2BatteryImpl extends AbstractOpenemsModbusComponent implements ModbusComponent,
		OpenemsComponent, Battery, EventHandler, ModbusSlave, StartStoppable, PylontechPowercubeM2Battery {

	public PylontechPowercubeM2BatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Battery.ChannelId.values(), //
				BatteryProtection.ChannelId.values(), PylontechPowercubeM2Battery.ChannelId.values() //
		);
	}

	private final Logger log = LoggerFactory.getLogger(PylontechPowercubeM2BatteryImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;
	private BatteryProtection batteryProtection = null;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new PylontechPowercubeM2BatteryProtectionDefinition(),
						this.componentManager) //
				.build();

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/*
	 * Calculates the Status based on the value provided - only looks at 3 LSBs
	 * 
	 * @return Status object
	 */
	protected Status getStatusFromRegisterValue(Integer value) {
		if (value == null) {
			return Status.UNDEFINED;
		}
		var strippedValue = value & 0x7; // Remove all but 3 LSB
		if (strippedValue < 0 || strippedValue > 3) {
			return Status.UNDEFINED; // The Modbus spec does not have values defined for > 3
		}
		return (value == null) ? Status.UNDEFINED : Status.valueOf(strippedValue);
	}

	protected void handleStatusRegister(Integer value, io.openems.edge.common.channel.ChannelId channelId) {
		this.channel(channelId).setNextValue(this.getStatusFromRegisterValue(value));
	}

	protected String convertVersionNumber(Integer value) {
		String versionString = "";
		if (value != null) {
			Integer minorVersionNumberFirstDigit = (value & 0xF);
			Integer minorVersionNumberSecondDigit = (value >> 4) & 0xF;
			Integer mainVersionNumberFirstDigit = (value >> 8) & 0xF;
			Integer mainVersionNumberSecondDigit = (value >> 12) & 0xF;
			String minorVersionNumber = Integer.toHexString(minorVersionNumberSecondDigit)
					+ Integer.toHexString(minorVersionNumberFirstDigit);
			String mainVersionNumber = Integer.toHexString(mainVersionNumberSecondDigit)
					+ Integer.toHexString(mainVersionNumberFirstDigit);
			versionString = "V" + mainVersionNumber + "." + minorVersionNumber;
		}
		return versionString;

	}

	protected void handleVersionNumber(Integer value) {
		String versionString = this.convertVersionNumber(value);

		this.channel(PylontechPowercubeM2Battery.ChannelId.VERSION_STRING)
				.setNextValue(value == null ? "" : versionString);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //

				// 3.2 Equipment Information
				new FC3ReadRegistersTask(0x100A, Priority.LOW, //
						m(new UnsignedWordElement(0x100A)).build().onUpdateCallback(value -> {
							this.handleVersionNumber(value);
						}),
						m(PylontechPowercubeM2Battery.ChannelId.PYLONTECH_INTERNAL_VERSION_NUMBER,
								new UnsignedWordElement(0x100B), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.SYSTEM_NUMBER_OF_PARALLEL_PILES,
								new UnsignedWordElement(0x100C), ElementToChannelConverter.DIRECT_1_TO_1)),

				// 3.4 System information
				new FC3ReadRegistersTask(0x1100, Priority.LOW,
						m(new UnsignedWordElement(0x1100)).build().onUpdateCallback(value -> {
							this.handleStatusRegister(value, PylontechPowercubeM2Battery.ChannelId.BASIC_STATUS);
						})),
				new FC3ReadRegistersTask(0x1100, Priority.LOW, m(new BitsWordElement(0x1100, this)
						.bit(3, PylontechPowercubeM2Battery.ChannelId.SYSTEM_ERROR_PROTECTION)
						.bit(4, PylontechPowercubeM2Battery.ChannelId.SYSTEM_CURRENT_PROTECTION)
						.bit(5, PylontechPowercubeM2Battery.ChannelId.SYSTEM_VOLTAGE_PROTECTION)
						.bit(6, PylontechPowercubeM2Battery.ChannelId.SYSTEM_TEMPERATURE_PROTECTION)
						.bit(7, PylontechPowercubeM2Battery.ChannelId.SYSTEM_VOLTAGE_WARNING)
						.bit(8, PylontechPowercubeM2Battery.ChannelId.SYSTEM_CURRENT_WARNING)
						.bit(9, PylontechPowercubeM2Battery.ChannelId.SYSTEM_TEMPERATURE_WARNING)
						.bit(10, PylontechPowercubeM2Battery.ChannelId.SYSTEM_IDLE_STATUS)
						.bit(11, PylontechPowercubeM2Battery.ChannelId.SYSTEM_CHARGE_STATUS)
						.bit(12, PylontechPowercubeM2Battery.ChannelId.SYSTEM_DISCHARGE_STATUS)
						.bit(13, PylontechPowercubeM2Battery.ChannelId.SYSTEM_SLEEP_STATUS)
						.bit(14, PylontechPowercubeM2Battery.ChannelId.SYSTEM_FAN_WARN)),
						m(new BitsWordElement(0x1101, this)
								.bit(0, PylontechPowercubeM2Battery.ChannelId.BATTERY_CELL_UNDER_VOLTAGE_PROTECTION)
								.bit(1, PylontechPowercubeM2Battery.ChannelId.BATTERY_CELL_OVER_VOLTAGE_PROTECTION)
								.bit(2, PylontechPowercubeM2Battery.ChannelId.PILE_UNDER_VOLTAGE_PROTECTION)
								.bit(3, PylontechPowercubeM2Battery.ChannelId.PILE_OVER_VOLTAGE_PROTECTION)
								.bit(4, PylontechPowercubeM2Battery.ChannelId.CHARGE_UNDER_TEMPERATURE_PROTECTION)
								.bit(5, PylontechPowercubeM2Battery.ChannelId.CHARGE_OVER_TEMPERATURE_PROTECTION)
								.bit(6, PylontechPowercubeM2Battery.ChannelId.DISCHARGE_UNDER_TEMPERATURE_PROTECTION)
								.bit(7, PylontechPowercubeM2Battery.ChannelId.DISCHARGE_OVER_TEMPERATURE_PROTECTION)
								.bit(8, PylontechPowercubeM2Battery.ChannelId.CHARGE_OVER_CURRENT_PROTECTION)
								.bit(9, PylontechPowercubeM2Battery.ChannelId.DISCHARGE_OVER_CURRENT_PROTECTION)
								.bit(10, PylontechPowercubeM2Battery.ChannelId.SHORT_CIRCUIT_PROTECTION)
								.bit(12, PylontechPowercubeM2Battery.ChannelId.MODULE_OVER_TEMPERATURE_PROTECTION)
								.bit(13, PylontechPowercubeM2Battery.ChannelId.MODULE_UNDER_VOLTAGE_PROTECTION)
								.bit(14, PylontechPowercubeM2Battery.ChannelId.MODULE_OVER_VOLTAGE_PROTECTION)),
						m(new BitsWordElement(0x1102, this)
								.bit(0, PylontechPowercubeM2Battery.ChannelId.BATTERY_CELL_LOW_VOLTAGE_WARNING)
								.bit(1, PylontechPowercubeM2Battery.ChannelId.BATTERY_CELL_HIGH_VOLTAGE_WARNING)
								.bit(2, PylontechPowercubeM2Battery.ChannelId.PILE_LOW_VOLTAGE_WARNING)
								.bit(3, PylontechPowercubeM2Battery.ChannelId.PILE_HIGH_VOLTAGE_WARNING)
								.bit(4, PylontechPowercubeM2Battery.ChannelId.CHARGE_LOW_TEMPERATURE_WARNING)
								.bit(5, PylontechPowercubeM2Battery.ChannelId.CHARGE_HIGH_TEMPERATURE_WARNING)
								.bit(6, PylontechPowercubeM2Battery.ChannelId.DISCHARGE_LOW_TEMPERATURE_WARNING)
								.bit(7, PylontechPowercubeM2Battery.ChannelId.DISCHARGE_HIGH_TEMPERATURE_WARNING)
								.bit(8, PylontechPowercubeM2Battery.ChannelId.CHARGE_OVER_CURRENT_WARNING)
								.bit(9, PylontechPowercubeM2Battery.ChannelId.DISCHARGE_OVER_CURRENT_WARNING)
								.bit(11, PylontechPowercubeM2Battery.ChannelId.BMS_HIGH_TEMPERATURE_WARNING)
								.bit(12, PylontechPowercubeM2Battery.ChannelId.MODULE_HIGH_TEMPERATURE_WARNING)
								.bit(13, PylontechPowercubeM2Battery.ChannelId.MODULE_LOW_VOLTAGE_WARNING)
								.bit(14, PylontechPowercubeM2Battery.ChannelId.MODULE_HIGH_VOLTAGE_WARNING))),
				new FC3ReadRegistersTask(0x1103, Priority.LOW, //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(0x1103),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.CURRENT, new SignedDoublewordElement(0x1104),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
																					
						m(PylontechPowercubeM2Battery.ChannelId.SYSTEM_TEMPERATURE, new SignedWordElement(0x1106), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x1107),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.CYCLE_TIMES, new UnsignedWordElement(0x1108),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(0x1109),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS, new SignedDoublewordElement(0x110A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(0x110C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, new SignedDoublewordElement(0x110D),
								ElementToChannelConverter.chain(ElementToChannelConverter.SCALE_FACTOR_MINUS_2,
										ElementToChannelConverter.KEEP_NEGATIVE_AND_INVERT)),
						m(new BitsWordElement(0x110F, this)
								.bit(0, PylontechPowercubeM2Battery.ChannelId.DISCHARGE_CIRCUIT_ACTIVE)
								.bit(1, PylontechPowercubeM2Battery.ChannelId.CHARGE_CIRCUIT_ACTIVE)
								.bit(2, PylontechPowercubeM2Battery.ChannelId.PRE_CHARGE_CIRCUIT_ACTIVE)
								.bit(3, PylontechPowercubeM2Battery.ChannelId.BUZZER_ACTIVE)
								.bit(4, PylontechPowercubeM2Battery.ChannelId.HEATING_FILM_ACTIVE)
								.bit(5, PylontechPowercubeM2Battery.ChannelId.CURRENT_LIMITING_MODULE_ACTIVE)
								.bit(6, PylontechPowercubeM2Battery.ChannelId.FAN_ACTIVE)),
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(0x1110),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(0x1111),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.MAX_VOLTAGE_CELL_NUMBER,
								new UnsignedWordElement(0x1112), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.MIN_VOLTAGE_CELL_NUMBER,
								new UnsignedWordElement(0x1113), ElementToChannelConverter.DIRECT_1_TO_1),
						m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new SignedWordElement(0x1114),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new SignedWordElement(0x1115),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(PylontechPowercubeM2Battery.ChannelId.MAX_TEMPERATURE_CELL_NUMBER,
								new UnsignedWordElement(0x1116), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.MIN_TEMPERATURE_CELL_NUMBER,
								new UnsignedWordElement(0x1117), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.MAX_MODULE_VOLTAGE, new UnsignedWordElement(0x1118),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(PylontechPowercubeM2Battery.ChannelId.MIN_MODULE_VOLTAGE, new UnsignedWordElement(0x1119),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(PylontechPowercubeM2Battery.ChannelId.MAX_VOLTAGE_MODULE_NUMBER,
								new UnsignedWordElement(0x111A), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.MIN_VOLTAGE_MODULE_NUMBER,
								new UnsignedWordElement(0x111B), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.MAX_MODULE_TEMPERATURE, new SignedWordElement(0x111C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(PylontechPowercubeM2Battery.ChannelId.MIN_MODULE_TEMPERATURE, new SignedWordElement(0x111D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(PylontechPowercubeM2Battery.ChannelId.MAX_TEMPERATURE_MODULE_NUMBER,
								new UnsignedWordElement(0x111E), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.MIN_TEMPERATURE_MODULE_NUMBER,
								new UnsignedWordElement(0x111F), ElementToChannelConverter.DIRECT_1_TO_1),
						m(Battery.ChannelId.SOH, new UnsignedWordElement(0x1120),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.REMAINING_CAPACITY,
								new UnsignedDoublewordElement(0x1121), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.CHARGE_CAPACITY, new UnsignedDoublewordElement(0x1123),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.DISCHARGE_CAPACITY,
								new UnsignedDoublewordElement(0x1125), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.DAILY_ACCUMULATED_CHARGE_CAPACITY,
								new UnsignedDoublewordElement(0x1127), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.DAILY_ACCUMULATED_DISCHARGE_CAPACITY,
								new UnsignedDoublewordElement(0x1129), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.HISTORICAL_ACCUMULATED_CHARGE_CAPACITY,
								new UnsignedDoublewordElement(0x112B), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.HISTORICAL_ACCUMULATED_DISCHARGE_CAPACITY,
								new UnsignedDoublewordElement(0x112D), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.REQUEST_FORCE_CHARGE_MARK,
								new UnsignedWordElement(0x112F), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.REQUEST_BALANCE_CHARGE_MARK,
								new UnsignedWordElement(0x1130), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.NUMBER_OF_PILES_IN_PARALLEL,
								new UnsignedWordElement(0x1131), ElementToChannelConverter.DIRECT_1_TO_1),
						m(new BitsWordElement(0x1132, this)
								.bit(0, PylontechPowercubeM2Battery.ChannelId.VOLTAGE_SENSOR_ERROR)
								.bit(1, PylontechPowercubeM2Battery.ChannelId.TEMPERATURE_SENSOR_ERROR)
								.bit(2, PylontechPowercubeM2Battery.ChannelId.INTERNAL_COMMUNICATION_ERROR)
								.bit(3, PylontechPowercubeM2Battery.ChannelId.INPUT_OVERVOLTAGE_ERROR)
								.bit(4, PylontechPowercubeM2Battery.ChannelId.INPUT_TRANSPOSITION_ERROR)
								.bit(5, PylontechPowercubeM2Battery.ChannelId.RELAY_ERROR)
								.bit(6, PylontechPowercubeM2Battery.ChannelId.BATTERY_DAMAGE_ERROR)
								.bit(7, PylontechPowercubeM2Battery.ChannelId.SWITCH_OFF_CIRCUIT_ERROR)
								.bit(8, PylontechPowercubeM2Battery.ChannelId.BMIC_ERROR)
								.bit(9, PylontechPowercubeM2Battery.ChannelId.INTERNAL_BUS_ERROR)
								.bit(10, PylontechPowercubeM2Battery.ChannelId.SELF_CHECK_FAILURE)
								.bit(11, PylontechPowercubeM2Battery.ChannelId.ABNORMAL_SECURITY_FUNCTION)
								.bit(12, PylontechPowercubeM2Battery.ChannelId.INSULATION_FAULT)
								.bit(13, PylontechPowercubeM2Battery.ChannelId.EMERGENCY_STOP_FAILURE))),
				new FC3ReadRegistersTask(0x1136, Priority.LOW, //
						m(PylontechPowercubeM2Battery.ChannelId.NUMBER_OF_MODULES_IN_SERIES_PER_PILE,
								new UnsignedWordElement(0x1136), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.NUMBER_OF_CELLS_IN_SERIES_PER_PILE,
								new UnsignedWordElement(0x1137), ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.CHARGE_FORBIDDEN_MARK, new UnsignedWordElement(0x1138),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.DISCHARGE_FORBIDDEN_MARK,
								new UnsignedWordElement(0x1139), ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(0x1148, Priority.LOW, //
						m(PylontechPowercubeM2Battery.ChannelId.INSULATION_RESISTANCE, new UnsignedWordElement(0x1148),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(PylontechPowercubeM2Battery.ChannelId.INSULATION_RESISTANCE_ERROR_LEVEL,
								new UnsignedWordElement(0x1149), ElementToChannelConverter.DIRECT_1_TO_1)),

				// Write sleep/wake register
				new FC6WriteRegisterTask(0x1090,
						m(PylontechPowercubeM2Battery.ChannelId.SLEEP_WAKE_CHANNEL, new UnsignedWordElement(0x1090)))

		// 3.5 Remote adjust information

		// 3.6 Single Battery Pile information

		);

	}

	@Override
	public String debugLog() {
		return Battery.generateDebugLog(this, this.stateMachine);
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		this.log.info("setStartStop called with value: " + value.toString());

		if (this.startStopTarget.getAndSet(value) != value) {
			// If the Start/Stop target is changed - (i.e the battery has been started from
			// outside) -> force the state machine into undefined (so that the state machine
			// will stop/start accordingly)
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

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	/**
	 * Callback for updating the channels if the number of piles in parallel ever
	 * changes. Not sure if we needed a callback for this, but I suppose it is good
	 * practice. Based on the Fenecon Commercial Implementation - the callback is
	 * set in the PylontechBattery.java file
	 */
	protected static final Consumer<Channel<Integer>> UPDATE_NUMBER_OF_PILES_CALLBACK = channel -> {
		channel.onChange((ignore, value) -> {
			((PylontechPowercubeM2BatteryImpl) channel.getComponent()).updateNumberOfPiles();
		});
	};

	/**
	 * Update number of piles and sets up the relevant channels. Needs to be called
	 * by a callback
	 */
	private synchronized void updateNumberOfPiles() {
		Channel<Integer> numberOfPilesChannel = this
				.channel(PylontechPowercubeM2Battery.ChannelId.NUMBER_OF_PILES_IN_PARALLEL);
		var numberOfPilesOpt = numberOfPilesChannel.value();

		if (!numberOfPilesOpt.isDefined()) {
			return; // If it's not defined - do nothing
		}

		if (numberOfPilesOpt.get() == 0) {
			return; // If no piles (why would this happen?) - do nothing
		}

		int numberOfPiles = numberOfPilesOpt.get();

		try {
			this.initializePileChannels(numberOfPiles);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to initialize channels for individual piles: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Used to iterate over piles. Starts from ONE not zero (to match Pylontech
	// docs).
	private int lastNumberOfPile = 1;

	/**
	 * Initialize channels per pile.
	 * 
	 * @param numberOfPiles Number of piles in battery system
	 * @throws OpenemsException on error
	 */
	private synchronized void initializePileChannels(int numberOfPiles) throws OpenemsException {

		for (var pile = this.lastNumberOfPile; pile <= numberOfPiles; pile++) {
			var pileOffset = this.getOffsetForPile(pile);

			if (pileOffset < 0) {
				// TODO: throw error!
			}
			final int pileFinal = pile; // Needed to be final to pass to generatePileChannel callback

			this.getModbusProtocol().addTasks(new FC3ReadRegistersTask(pileOffset + 0x0000, Priority.LOW,
					m(new UnsignedWordElement(pileOffset + 0x0000)).build().onUpdateCallback(value -> {
						this.handleStatusRegister(value,
								this.generatePileChannel(pileFinal, "STATUS", Doc.of(Status.values())));
					})),
					new FC3ReadRegistersTask(
							pileOffset + 0x0000, Priority.LOW, m(new BitsWordElement(pileOffset + 0x0000, this) // Get status from first 3 bits
									.bit(3, this.generatePileChannel(pile, "SYSTEM_ERROR_PROTECTION", Level.FAULT))
									.bit(4, this.generatePileChannel(pile, "CURRENT_PROTECTION", Level.FAULT))
									.bit(5, this.generatePileChannel(pile, "VOLTAGE_PROTECTION", Level.FAULT))
									.bit(6, this.generatePileChannel(pile, "TEMPERATURE_PROTECTION", Level.FAULT))
									.bit(7, this.generatePileChannel(pile, "VOLTAGE_WARNING", Level.WARNING))
									.bit(8, this.generatePileChannel(pile, "CURRENT_WARNING", Level.WARNING))
									.bit(9, this.generatePileChannel(pile, "TEMPERATURE_WARNING", Level.WARNING))
									.bit(10, this.generatePileChannel(pile, "PILE_SYSTEM_IDLE_STATUS",
											OpenemsType.BOOLEAN))
									.bit(11, this.generatePileChannel(pile, "PILE_SYSTEM_CHARGE_STATUS",
											OpenemsType.BOOLEAN))
									.bit(12, this.generatePileChannel(pile, "PILE_SYSTEM_DISCHARGE_STATUS",
											OpenemsType.BOOLEAN))
									.bit(13, this.generatePileChannel(pile, "PILE_SYSTEM_SLEEP_STATUS",
											OpenemsType.BOOLEAN))
									.bit(14, this.generatePileChannel(pile, "FAN_WARN", Level.WARNING))),
							m(new BitsWordElement(pileOffset + 0x0001, this)
									.bit(0, this.generatePileChannel(pile, "BATTERY_CELL_UNDER_VOLTAGE_PROTECTION",
											Level.FAULT))
									.bit(1, this.generatePileChannel(pile, "BATTERY_CELL_OVER_VOLTAGE_PROTECTION",
											Level.FAULT))
									.bit(2, this.generatePileChannel(pile, "PILE_UNDER_VOLTAGE_PROTECTION",
											Level.FAULT))
									.bit(3, this.generatePileChannel(pile, "PILE_OVER_VOLTAGE_PROTECTION", Level.FAULT))
									.bit(4, this.generatePileChannel(pile, "CHARGE_UNDER_TEMPERATURE_PROTECTION",
											Level.FAULT))
									.bit(5, this.generatePileChannel(pile, "CHARGE_OVER_TEMPERATURE_PROTECTION",
											Level.FAULT))
									.bit(6, this.generatePileChannel(pile, "DISCHARGE_UNDER_TEMPERATURE_PROTECTION",
											Level.FAULT))
									.bit(7, this.generatePileChannel(pile, "DISCHARGE_OVER_TEMPERATURE_PROTECTION",
											Level.FAULT))
									.bit(8, this.generatePileChannel(pile, "CHARGE_OVER_CURRENT_PROTECTION",
											Level.FAULT))
									.bit(9, this.generatePileChannel(pile, "DISCHARGE_OVER_CURRENT_PROTECTION",
											Level.FAULT))
									.bit(10, this.generatePileChannel(pile, "SHORT_CIRCUIT_PROTECTION", Level.FAULT))
									.bit(12, this.generatePileChannel(pile, "MODULE_OVER_TEMPERATURE_PROTECTION",
											Level.FAULT))
									.bit(13, this.generatePileChannel(pile, "MODULE_UNDER_VOLTAGE_PROTECTION",
											Level.FAULT))
									.bit(14, this.generatePileChannel(pile, "MODULE_OVER_VOLTAGE_PROTECTION",
											Level.FAULT))),
							m(new BitsWordElement(pileOffset + 0x0002, this)
									.bit(0, this.generatePileChannel(pile, "BATTERY_CELL_LOW_VOLTAGE_WARNING",
											Level.WARNING))
									.bit(1, this.generatePileChannel(pile, "BATTERY_CELL_HIGH_VOLTAGE_WARNING",
											Level.WARNING))
									.bit(2, this.generatePileChannel(pile, "PILE_LOW_VOLTAGE_WARNING", Level.WARNING))
									.bit(3, this.generatePileChannel(pile, "PILE_HIGH_VOLTAGE_WARNING", Level.WARNING))
									.bit(4, this.generatePileChannel(pile, "CHARGE_LOW_TEMPERATURE_WARNING",
											Level.WARNING))
									.bit(5, this.generatePileChannel(pile, "CHARGE_HIGH_TEMPERATURE_WARNING",
											Level.WARNING))
									.bit(6, this.generatePileChannel(pile, "DISCHARGE_LOW_TEMPERATURE_WARNING",
											Level.WARNING))
									.bit(7, this.generatePileChannel(pile, "DISCHARGE_HIGH_TEMPERATURE_WARNING",
											Level.WARNING))
									.bit(8, this.generatePileChannel(pile, "CHARGE_OVER_CURRENT_WARNING",
											Level.WARNING))
									.bit(9, this.generatePileChannel(pile, "DISCHARGE_OVER_CURRENT_WARNING",
											Level.WARNING))
									.bit(11, this.generatePileChannel(pile,
											"MAIN_CONTROLLER_BMS_HIGH_TEMPERATURE_WARNING", Level.WARNING))
									.bit(12, this.generatePileChannel(pile, "MODULE_HIGH_TEMPERATURE_WARNING",
											Level.WARNING))
									.bit(13, this.generatePileChannel(pile, "MODULE_LOW_TEMPERATURE_WARNING",
											Level.WARNING))
									.bit(14, this.generatePileChannel(pile, "MODULE_HIGH_VOLTAGE_WARNING",
											Level.WARNING))),
							m(this.generatePileChannel(pile, "TOTAL_VOLTAGE", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0003), // [V]
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
							m(this.generatePileChannel(pile, "CURRENT", OpenemsType.INTEGER), 
									new SignedDoublewordElement(pileOffset + 0x0004), // [A]
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
							m(this.generatePileChannel(pile, "TEMPERATURE", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0006), // [oC]
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
							m(this.generatePileChannel(pile, "SOC", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0007), // [%]
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
							m(this.generatePileChannel(pile, "CYCLE_TIME", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0008), // Unsure of unit
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "MAX_CHARGE_VOLTAGE", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0009), // [V}
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
							m(this.generatePileChannel(pile, "MAX_CHARGE_CURRENT", OpenemsType.INTEGER),
									new UnsignedDoublewordElement(pileOffset + 0x000A), // [A]
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
							m(this.generatePileChannel(pile, "MAX_DISCHARGE_VOLTAGE", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x000C),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
							m(this.generatePileChannel(pile, "MAX_DISCHARGE_CURRENT", OpenemsType.INTEGER),
									new SignedDoublewordElement(pileOffset + 0x000D),
									ElementToChannelConverter.chain(ElementToChannelConverter.SCALE_FACTOR_MINUS_2,
											ElementToChannelConverter.KEEP_NEGATIVE_AND_INVERT)),
							m(new BitsWordElement(pileOffset + 0x000F, this)
									.bit(0, this.generatePileChannel(pile, "DISCHARGE_CIRCUIT_ACTIVE",
											OpenemsType.BOOLEAN))
									.bit(1, this.generatePileChannel(pile, "CHARGE_CIRCUIT_ACTIVE",
											OpenemsType.BOOLEAN))
									.bit(2, this.generatePileChannel(pile, "PRE_CHARGE_CIRCUIT_ACTIVE",
											OpenemsType.BOOLEAN))
									.bit(3, this.generatePileChannel(pile, "BUZZER_ACTIVE", OpenemsType.BOOLEAN))
									.bit(4, this.generatePileChannel(pile, "HEATING_FILM_ACTIVE", OpenemsType.BOOLEAN))
									.bit(5, this.generatePileChannel(pile, "CURRENT_LIMITING_MODULE_ACTIVE",
											OpenemsType.BOOLEAN))
									.bit(6, this.generatePileChannel(pile, "FAN_ACTIVE", OpenemsType.BOOLEAN))),
							m(this.generatePileChannel(pile, "MAX_CELL_VOLTAGE", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0010),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							m(this.generatePileChannel(pile, "MIN_CELL_VOLTAGE", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0011),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
							m(this.generatePileChannel(pile, "SERIAL_NUMBER_OF_MAX_CELL_VOLTAGE", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0012),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "SERIAL_NUMBER_OF_MIN_CELL_VOLTAGE", OpenemsType.INTEGER), 
									new UnsignedWordElement(pileOffset + 0x0013),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "MAX_CELL_TEMPERATURE", OpenemsType.INTEGER),
									new SignedWordElement(pileOffset + 0x0014), // [oC]
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
							m(this.generatePileChannel(pile, "MIN_CELL_TEMPERATURE", OpenemsType.INTEGER),
									new SignedWordElement(pileOffset + 0x0015), // [oC]
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
							m(this.generatePileChannel(pile, "SERIAL_NUMBER_OF_MAX_CELL_TEMPERATURE",
									OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0016), //
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "SERIAL_NUMBER_OF_MIN_CELL_TEMPERATURE",
									OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0017), //
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "MAX_MODULE_VOLTAGE", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0018), // [V]
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
							m(this.generatePileChannel(pile, "MIN_MODULE_VOLTAGE", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0019), // [V]
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
							m(this.generatePileChannel(pile, "SERIAL_NUMBER_OF_MAX_MODULE_VOLTAGE",
									OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x001A), //
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "SERIAL_NUMBER_OF_MAX_MODULE_VOLTAGE",
									OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x001A), //
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "SERIAL_NUMBER_OF_MIN_MODULE_VOLTAGE",
									OpenemsType.INTEGER), 
									new UnsignedWordElement(pileOffset + 0x001B), //
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "MAX_MODULE_TEMPERATURE", OpenemsType.INTEGER),
									new SignedWordElement(pileOffset + 0x001C), // [oC]
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
							m(this.generatePileChannel(pile, "MIN_MODULE_TEMPERATURE", OpenemsType.INTEGER), 
									new SignedWordElement(pileOffset + 0x001D), // [oC]
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
							m(this.generatePileChannel(pile, "SERIAL_NUMBER_OF_MAX_MODULE_TEMPERATURE",
									OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x001E), //
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "SERIAL_NUMBER_OF_MIN_MODULE_TEMPERATURE",
									OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x001F), //
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "SOH", OpenemsType.INTEGER),
									new UnsignedWordElement(pileOffset + 0x0020), // [%]
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "REMAINING_CAPACITY", OpenemsType.INTEGER),
									new UnsignedDoublewordElement(pileOffset + 0x0021), // [Wh]
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "CHARGE_CAPACITY", OpenemsType.INTEGER), 
									new UnsignedDoublewordElement(pileOffset + 0x0023), // [Wh]
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "DISCHARGE_CAPACITY", OpenemsType.INTEGER),
									new UnsignedDoublewordElement(pileOffset + 0x0025), // [Wh]
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "DAILY_ACCUMULATED_CHARGE_CAPACITY", OpenemsType.INTEGER),
									new UnsignedDoublewordElement(pileOffset + 0x0027), // [Wh]
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "DAILY_ACCUMULATED_DISCHARGE_CAPACITY",
									OpenemsType.INTEGER),
									new UnsignedDoublewordElement(pileOffset + 0x0029), // [Wh]
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "HISTORICAL_ACCUMULATED_CHARGE_CAPACITY",
									OpenemsType.INTEGER),
									new UnsignedDoublewordElement(pileOffset + 0x002B), // [Wh]
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "HISTORICAL_ACCUMULATED_DISCHARGE_CAPACITY",
									OpenemsType.INTEGER), 
									new UnsignedDoublewordElement(pileOffset + 0x002D), // [Wh]
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "REQUEST_FORCE_CHARGE_MARK", OpenemsType.BOOLEAN),
									new UnsignedWordElement(pileOffset + 0x002F), // Yes/no
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(this.generatePileChannel(pile, "REQUEST_BALANCE_CHARGE_MARK", OpenemsType.BOOLEAN),
									new UnsignedWordElement(pileOffset + 0x0030), // Yes / No
									ElementToChannelConverter.DIRECT_1_TO_1)),
					new FC3ReadRegistersTask((pileOffset + 0x0032), Priority.LOW, //
							m(new BitsWordElement(pileOffset + 0x0032, this)
									.bit(0, this.generatePileChannel(pile, "VOLTAGE_SENSOR_ERROR", OpenemsType.BOOLEAN))
									.bit(1, this.generatePileChannel(pile, "TEMPERATURE_SENSOR_ERROR",
											OpenemsType.BOOLEAN))
									.bit(2, this.generatePileChannel(pile, "INTERNAL_COMMUNICATION_ERROR",
											OpenemsType.BOOLEAN))
									.bit(3, this.generatePileChannel(pile, "INPUT_OVER_VOLTAGE_ERROR",
											OpenemsType.BOOLEAN))
									.bit(4, this.generatePileChannel(pile, "INPUT_TRANSPOSITION_ERROR",
											OpenemsType.BOOLEAN))
									.bit(5, this.generatePileChannel(pile, "RELAY_ERROR", OpenemsType.BOOLEAN))
									.bit(6, this.generatePileChannel(pile, "BATTERY_DAMAGE_ERROR", OpenemsType.BOOLEAN))
									.bit(7, this.generatePileChannel(pile, "SWITCH_OFF_CIRCUIT_ERROR",
											OpenemsType.BOOLEAN))
									.bit(8, this.generatePileChannel(pile, "BMIC_ERROR", OpenemsType.BOOLEAN))
									.bit(9, this.generatePileChannel(pile, "INTERNAL_BUS_ERROR", OpenemsType.BOOLEAN))
									.bit(10, this.generatePileChannel(pile, "SELF_CHECK_FAILURE", OpenemsType.BOOLEAN))
									.bit(11, this.generatePileChannel(pile, "ABNORMAL_SECURITY_FUNCTION",
											OpenemsType.BOOLEAN))
									.bit(12, this.generatePileChannel(pile, "INSULATION_FAULT", OpenemsType.BOOLEAN))
									.bit(13, this.generatePileChannel(pile, "EMERGENCY_STOP_FAILURE",
											OpenemsType.BOOLEAN)))));
		}
	}

	/**
	 * Generates a channel ID for pile-specific channels.
	 * 
	 * @param pile            number of the pile
	 * @param channelIdSuffix e.g "SOH"
	 * @param openemsType     specified type e.g "INTEGER"
	 * @return a channel with channel-ID "PILE_1_SOH"
	 */
	private ChannelIdImpl generatePileChannel(int pile, String channelIdSuffix, OpenemsType openemsType) {
		var channelId = new ChannelIdImpl("PILE_" + pile + "_" + channelIdSuffix, Doc.of(openemsType));
		this.addChannel(channelId);
		return channelId;
	}

	/**
	 * Generates a channel ID for pile-specific channels.
	 * 
	 * @param pile            number of the pile
	 * @param channelIdSuffix e.g "SOH"
	 * @param doc             pre-made doc type for passing docs directly
	 * @return a channel with channel-ID "PILE_1_SOH"
	 */
	private ChannelIdImpl generatePileChannel(int pile, String channelIdSuffix, Doc doc) {
		var channelId = new ChannelIdImpl("PILE_" + pile + "_" + channelIdSuffix, doc);
		this.addChannel(channelId);
		return channelId;
	}

	/**
	 * Generates a channel ID for pile-specific channels.
	 * 
	 * @param pile            pile number
	 * @param channelIdSuffix e.g SOH
	 * @param level           OpenEMS.Level (e.g FAULT)
	 * @return a channel with channel-ID "PILE_1_SOH"
	 */
	private ChannelIdImpl generatePileChannel(int pile, String channelIdSuffix, Level level) {
		var channelId = new ChannelIdImpl("PILE_" + pile + "_" + channelIdSuffix, Doc.of(level));
		this.addChannel(channelId);
		return channelId;
	}

	/**
	 * Gets modbus offset for a given pileNumber. pileNumber starts from 1.
	 * 
	 * @param pileNumber pileNumber The pile the offset is for
	 * @return The Modbus offset for the pile
	 */
	private int getOffsetForPile(int pileNumber) {

		switch (pileNumber) {
		case 1:
			return 0x1400;
		case 2:
			return 0x1B00;
		case 3:
			return 0x2200;
		case 4:
			return 0x2900;
		case 5:
			return 0x3000;
		case 6:
			return 0x3700;
		case 7:
			return 0x3E00;
		case 8:
			return 0x4500;
		case 9:
			return 0x4C00;
		case 10:
			return 0x5300;
		case 11:
			return 0x5A00;
		case 12:
			return 0x6100;
		case 13:
			return 0x6800;
		case 14:
			return 0x6F00;
		case 15:
			return 0x7600;
		case 16:
			return 0x7D00;
		case 17:
			return 0x8400;
		case 18:
			return 0x8B00;
		case 19:
			return 0x9200;
		case 20:
			return 0x9900;
		case 21:
			return 0xA000;
		case 22:
			return 0xA700;
		case 23:
			return 0xAE00;
		case 24:
			return 0xB500;
		case 25:
			return 0xBC00;
		case 26:
			return 0xC300;
		case 27:
			return 0xCA00;
		case 28:
			return 0xD100;
		case 29:
			return 0xD800;
		case 30:
			return 0xDF00;
		case 31:
			return 0xE600;
		case 32:
			return 0xED00;
		}
		;

		return -1;

	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.batteryProtection.apply();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.handleStateMachine();
			break;
		}
	}

	/**
	 * Handles the state machine.
	 */
	private void handleStateMachine() {
		// Store the current state.
		this.channel(PylontechPowercubeM2Battery.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateMachine.getCurrentState());

		// Initialise start-stop channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		IntegerWriteChannel batteryWakeSleepChannel = null;
		try {
			batteryWakeSleepChannel = this.channel(PylontechPowercubeM2Battery.ChannelId.SLEEP_WAKE_CHANNEL);
		} catch (IllegalArgumentException e1) {
			this.logError(this.log, //
					"Setting BatteryWakeSleepChannel failed: " + e1.getMessage());
			e1.printStackTrace();
		}

		var context = new Context(this, batteryWakeSleepChannel);

		try {
			this.stateMachine.run(context);
			this.channel(PylontechPowercubeM2Battery.ChannelId.RUN_FAILED).setNextValue(false);
		} catch (OpenemsNamedException e) {
			this.channel(PylontechPowercubeM2Battery.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}
}