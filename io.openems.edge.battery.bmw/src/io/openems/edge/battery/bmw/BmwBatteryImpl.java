package io.openems.edge.battery.bmw;

import java.time.LocalDateTime;
import java.util.Optional;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.bmw.enums.*;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bmw.Battery", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class BmwBatteryImpl extends AbstractOpenemsModbusComponent
		implements BmwBattery, Battery, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(BmwBatteryImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private ComponentManager manager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private static final Integer OPEN_CONTACTORS = 0;
	private static final Integer CLOSE_CONTACTORS = 4;

	private State state = State.UNDEFINED;

	private Config config;

	private LocalDateTime errorDelayIsOver = null;
	private int unsuccessfulStarts = 0;
	private LocalDateTime startAttemptTime = null;
	private LocalDateTime pendingTimestamp;

	public BmwBatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				BMWChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
	}

	private void handleStateMachine() {
		boolean readyForWorking = false;
		switch (this.getStateMachineState()) {
		case ERROR:
			this.clearError();
			// TODO Reset BMS? anything else?
			errorDelayIsOver = LocalDateTime.now().plusSeconds(this.config.errorDelay());
			setStateMachineState(State.ERRORDELAY);
			break;
		case ERRORDELAY:
			if (LocalDateTime.now().isAfter(errorDelayIsOver)) {
				errorDelayIsOver = null;
				if (this.isError()) {
					this.setStateMachineState(State.ERROR);
				} else {
					this.setStateMachineState(State.OFF);
				}
			}
			break;
		case INIT:
			if (this.isSystemRunning()) {
				this.setStateMachineState(State.RUNNING);
				unsuccessfulStarts = 0;
				startAttemptTime = null;
			} else {
				if (startAttemptTime.plusSeconds(config.maxStartTime()).isBefore(LocalDateTime.now())) {
					startAttemptTime = null;
					unsuccessfulStarts++;
					this.stopSystem();
					this.setStateMachineState(State.STOPPING);
					if (unsuccessfulStarts >= this.config.maxStartAttempts()) {
						errorDelayIsOver = LocalDateTime.now().plusSeconds(this.config.startUnsuccessfulDelay());
						this.setStateMachineState(State.ERRORDELAY);
						unsuccessfulStarts = 0;
					}
				}
			}
			break;
		case OFF:
			log.debug("in case 'OFF'; try to start the system");
			this.startSystem();
			log.debug("set state to 'INIT'");
			this.setStateMachineState(State.INIT);
			startAttemptTime = LocalDateTime.now();
			break;
		case RUNNING:
			if (this.isError()) {
				this.setStateMachineState(State.ERROR);
			} else if (!this.isSystemRunning()) {
				this.setStateMachineState(State.UNDEFINED);
			} else {
				this.setStateMachineState(State.RUNNING);
				readyForWorking = true;
			}
			break;
		case STOPPING:
			if (this.isError()) {
				this.setStateMachineState(State.ERROR);
			} else {
				if (this.isSystemStopped()) {
					this.setStateMachineState(State.OFF);
				}
			}
			break;
		case UNDEFINED:
			if (this.isError()) {
				this.setStateMachineState(State.ERROR);
			} else if (this.isSystemStopped()) {
				this.setStateMachineState(State.OFF);
			} else if (this.isSystemRunning()) {
				this.setStateMachineState(State.RUNNING);
			} else if (this.isSystemStatePending()) {
				this.setStateMachineState(State.PENDING);
			}
			break;
		case PENDING:
			if (this.pendingTimestamp == null) {
				this.pendingTimestamp = LocalDateTime.now();
			}
			if (this.pendingTimestamp.plusSeconds(this.config.pendingTolerance()).isBefore(LocalDateTime.now())) {
				// System state could not be determined, stop and start it
				this.pendingTimestamp = null;
				this.stopSystem();
				this.setStateMachineState(State.OFF);
			} else {
				if (this.isError()) {
					this.setStateMachineState(State.ERROR);
					this.pendingTimestamp = null;
				} else if (this.isSystemStopped()) {
					this.setStateMachineState(State.OFF);
					this.pendingTimestamp = null;
				} else if (this.isSystemRunning()) {
					this.setStateMachineState(State.RUNNING);
					this.pendingTimestamp = null;
				}
			}
			break;
		case STANDBY:
			break;
		}

		// this.getReadyForWorking().setNextValue(readyForWorking);
		if (readyForWorking) {
			this._setStartStop(StartStop.START);
		} else {
			this._setStartStop(StartStop.STOP);
		}
	}

	private void clearError() {
		BooleanWriteChannel clearErrorChannel = this.channel(BMWChannelId.BMS_STATE_COMMAND_CLEAR_ERROR);
		try {
			clearErrorChannel.setNextWriteValue(true);
		} catch (OpenemsNamedException e) {
			// TODO should Fault state channel, but after start stop feature
			log.error("Error while trying to reset the system!");
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.handleBatteryState();
			break;
		}
	}

	private void handleBatteryState() {
		switch (this.config.batteryState()) {
		case DEFAULT:
			handleStateMachine();
			break;
		case OFF:
			stopSystem();
			break;
		case ON:
			startSystem();
			break;
		}
	}

//  TODO Check this needed or not
//	public void shutDownBattery() {
//		SymmetricEss ess;
//		try {
//			ess = this.manager.getComponent(this.config.Inverter_id());
//		} catch (OpenemsNamedException e1) {
//
//			e1.printStackTrace();
//			return;
//		}
//		int activePowerInverter = ess.getActivePower().value().orElse(0);
//		int reactivePowerInverter = ess.getReactivePower().value().orElse(0);
//
//		if (activePowerInverter == 0 && reactivePowerInverter == 0) {
//			IntegerWriteChannel commandChannel = this.channel(BMWChannelId.BMS_STATE_COMMAND);
//			try {
//				commandChannel.setNextWriteValue(OPEN_CONTACTORS);
//			} catch (OpenemsNamedException e) {
//				log.error("Problem occurred during send start command");
//			}
//		}
//
//	}

	private boolean isSystemRunning() {
		EnumReadChannel bmsStateChannel = this.channel(BMWChannelId.BMS_STATE);
		BmsState bmsState = bmsStateChannel.value().asEnum();
		return bmsState == BmsState.OPERATION;
	}

	private boolean isSystemStopped() {
		EnumReadChannel bmsStateChannel = this.channel(BMWChannelId.BMS_STATE);
		BmsState bmsState = bmsStateChannel.value().asEnum();
		return bmsState == BmsState.OFF;
	}

	/**
	 * Checks whether system has an undefined state
	 */
	private boolean isSystemStatePending() {
		return !isSystemRunning() && !isSystemStopped();
	}

	private boolean isError() {
		EnumReadChannel bmsStateChannel = this.channel(BMWChannelId.BMS_STATE);
		BmsState bmsState = bmsStateChannel.value().asEnum();
		return bmsState == BmsState.ERROR;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc() //
				+ "|Discharge:" + this.getDischargeMinVoltage() + ";" + this.getDischargeMaxCurrent() //
				+ "|Charge:" + this.getChargeMaxVoltage() + ";" + this.getChargeMaxCurrent() //
				+ "|State:" + this.state.asCamelCase();
	}

	private void startSystem() {
		// TODO Currently not necessary, Battery starts itself?!
		this.log.debug("Start system");
		IntegerWriteChannel commandChannel = this.channel(BMWChannelId.BMS_STATE_COMMAND);
		try {
			commandChannel.setNextWriteValue(CLOSE_CONTACTORS);
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			log.error("Problem occurred during send start command");
		}
	}

	private void stopSystem() {
		// TODO Currently not necessary, Battery starts itself?!
		this.log.debug("Stop system");
		IntegerWriteChannel commandChannel = this.channel(BMWChannelId.BMS_STATE_COMMAND);
		try {
			commandChannel.setNextWriteValue(OPEN_CONTACTORS);
		} catch (OpenemsNamedException e) {
			log.error("Problem occurred during send stopping command");
		}
	}

	private State getStateMachineState() {
		return state;
	}

	private void setStateMachineState(State state) {
		this.state = state;
		this.channel(BMWChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		return new ModbusProtocol(this, //

				new FC16WriteRegistersTask(1399, m(BMWChannelId.HEART_BEAT, new UnsignedWordElement(1399)), //
						m(BMWChannelId.BMS_STATE_COMMAND, new UnsignedWordElement(1400)), //
						m(BMWChannelId.OPERATING_STATE_INVERTER, new UnsignedWordElement(1401)), //
						m(BMWChannelId.DC_LINK_VOLTAGE, new UnsignedWordElement(1402),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BMWChannelId.DC_LINK_CURRENT, new UnsignedWordElement(1403)), //
						m(BMWChannelId.OPERATION_MODE_REQUEST_GRANTED, new UnsignedWordElement(1404)), //
						m(BMWChannelId.OPERATION_MODE_REQUEST_CANCELED, new UnsignedWordElement(1405)), //
						m(new BitsWordElement(1406, this) //
								.bit(1, BMWChannelId.CONNECTION_STRATEGY_HIGH_SOC_FIRST) //
								.bit(0, BMWChannelId.CONNECTION_STRATEGY_LOW_SOC_FIRST) //
						), //
						m(BMWChannelId.SYSTEM_TIME, new UnsignedDoublewordElement(1407)) //
				),

				new FC4ReadInputRegistersTask(999, Priority.HIGH,
						m(BMWChannelId.LIFE_SIGN, new UnsignedWordElement(999)),
						m(BMWChannelId.BMS_STATE, new UnsignedWordElement(1000)), //
						m(BMWChannelId.ERROR_BITS_1, new UnsignedWordElement(1001)), //
						m(BMWChannelId.ERROR_BITS_2, new UnsignedWordElement(1002)), //
						m(BMWChannelId.WARNING_BITS_1, new UnsignedWordElement(1003)), //
						m(BMWChannelId.WARNING_BITS_2, new UnsignedWordElement(1004)), //
						m(BMWChannelId.INFO_BITS, new UnsignedWordElement(1005)), //
						m(BMWChannelId.MAXIMUM_OPERATING_CURRENT, new UnsignedWordElement(1006)), //
						m(BMWChannelId.MINIMUM_OPERATING_CURRENT, new SignedWordElement(1007)), //
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(1008),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(1009),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(1010)), //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new SignedWordElement(1011),
								ElementToChannelConverter.INVERT), //
						m(BMWChannelId.MAXIMUM_LIMIT_DYNAMIC_VOLTAGE, new UnsignedWordElement(1012),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BMWChannelId.MINIMUM_LIMIT_DYNAMIC_VOLTAGE, new UnsignedWordElement(1013),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BMWChannelId.NUMBER_OF_STRINGS_CONNECTED, new UnsignedWordElement(1014)), //
						m(BMWChannelId.NUMBER_OF_STRINGS_INSTALLED, new UnsignedWordElement(1015)), //
						m(BMWChannelId.SOC_ALL_STRINGS, new UnsignedWordElement(1016),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(Battery.ChannelId.SOC, new UnsignedWordElement(1017),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(BMWChannelId.REMAINING_CHARGE_CAPACITY, new UnsignedWordElement(1018)), //
						m(BMWChannelId.REMAINING_DISCHARGE_CAPACITY, new UnsignedWordElement(1019)), //
						m(BMWChannelId.REMAINING_CHARGE_ENERGY, new UnsignedWordElement(1020)), //
						m(BMWChannelId.REMAINING_DISCHARGE_ENERGY, new UnsignedWordElement(1021)), //
						m(BMWChannelId.NOMINAL_ENERGY, new UnsignedWordElement(1022)), //
						m(BMWChannelId.TOTAL_ENERGY, new UnsignedWordElement(1023)), //
						m(BMWChannelId.NOMINAL_CAPACITY, new UnsignedWordElement(1024)), //
						m(Battery.ChannelId.CAPACITY, new UnsignedWordElement(1025)), //
						m(Battery.ChannelId.SOH, new UnsignedWordElement(1026),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(1027),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BMWChannelId.DC_VOLTAGE_AVERAGE, new UnsignedWordElement(1028),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC4ReadInputRegistersTask(1029, Priority.HIGH, //
						m(new UnsignedWordElement(1029)) //
								.m(BMWChannelId.DC_CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
								.m(Battery.ChannelId.CURRENT, ElementToChannelConverter.SCALE_FACTOR_3) //
								.build()), //
				new FC4ReadInputRegistersTask(1030, Priority.HIGH,
						m(BMWChannelId.AVERAGE_TEMPERATURE, new UnsignedWordElement(1030)), //
						m(BMWChannelId.MINIMUM_TEMPERATURE, new UnsignedWordElement(1031)), //
						m(BMWChannelId.MAXIMUM_TEMPERATURE, new UnsignedWordElement(1032)), //
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(1033)), //
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(1034)), //
						m(BMWChannelId.AVERAGE_CELL_VOLTAGE, new UnsignedWordElement(1035)), //
						m(BMWChannelId.INTERNAL_RESISTANCE, new UnsignedWordElement(1036)), //
						m(BMWChannelId.INSULATION_RESISTANCE, new UnsignedWordElement(1037),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BMWChannelId.CONTAINER_TEMPERATURE, new UnsignedWordElement(1038),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BMWChannelId.AMBIENT_TEMPERATURE, new UnsignedWordElement(1039),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BMWChannelId.HUMIDITY_CONTAINER, new UnsignedWordElement(1040),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(BMWChannelId.MAXIMUM_LIMIT_DYNAMIC_CURRENT_HIGH_RES, new UnsignedWordElement(1041),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(BMWChannelId.MINIMUM_LIMIT_DYNAMIC_CURRENT_HIGH_RES, new UnsignedWordElement(1042),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(BMWChannelId.FULL_CYCLE_COUNT, new UnsignedWordElement(1043)), //
						m(BMWChannelId.OPERATING_TIME_COUNT, new UnsignedDoublewordElement(1044)), //
						m(BMWChannelId.COM_PRO_VERSION, new UnsignedDoublewordElement(1046)), //
						m(BMWChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(1048)), //
						m(BMWChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(1050)), //
						m(BMWChannelId.SOFTWARE_VERSION, new UnsignedDoublewordElement(1052)) //
				)

		);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO Auto-generated method stub
	}
}
