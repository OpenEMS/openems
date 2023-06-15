package io.openems.edge.battery.bmw;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;

import java.time.LocalDateTime;

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
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.bmw.enums.BmsState;
import io.openems.edge.battery.bmw.enums.State;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
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
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bmw.Battery", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class BmwBatteryImpl extends AbstractOpenemsModbusComponent
		implements BmwBattery, Battery, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, StartStoppable {

	private static final Integer OPEN_CONTACTORS = 0;
	private static final Integer CLOSE_CONTACTORS = 4;

	private final Logger log = LoggerFactory.getLogger(BmwBatteryImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private State state = State.UNDEFINED;
	private Config config;
	private LocalDateTime errorDelayIsOver = null;
	private int unsuccessfulStarts = 0;
	private LocalDateTime startAttemptTime = null;
	private LocalDateTime pendingTimestamp;

	public BmwBatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BmwBattery.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void handleStateMachine() {
		var readyForWorking = false;
		switch (this.getStateMachineState()) {
		case ERROR -> {
			this.clearError();
			// TODO Reset BMS? anything else?
			this.errorDelayIsOver = LocalDateTime.now().plusSeconds(this.config.errorDelay());
			this.setStateMachineState(State.ERRORDELAY);
		}
		case ERRORDELAY -> {
			if (LocalDateTime.now().isAfter(this.errorDelayIsOver)) {
				this.errorDelayIsOver = null;
				if (this.isError()) {
					this.setStateMachineState(State.ERROR);
				} else {
					this.setStateMachineState(State.OFF);
				}
			}
		}
		case INIT -> {
			if (this.isSystemRunning()) {
				this.setStateMachineState(State.RUNNING);
				this.unsuccessfulStarts = 0;
				this.startAttemptTime = null;
			} else if (this.startAttemptTime.plusSeconds(this.config.maxStartTime()).isBefore(LocalDateTime.now())) {
				this.startAttemptTime = null;
				this.unsuccessfulStarts++;
				this.stopSystem();
				this.setStateMachineState(State.STOPPING);
				if (this.unsuccessfulStarts >= this.config.maxStartAttempts()) {
					this.errorDelayIsOver = LocalDateTime.now().plusSeconds(this.config.startUnsuccessfulDelay());
					this.setStateMachineState(State.ERRORDELAY);
					this.unsuccessfulStarts = 0;
				}
			}
		}
		case OFF -> {
			this.logDebug(this.log, "in case 'OFF'; try to start the system");
			this.startSystem();
			this.logDebug(this.log, "set state to 'INIT'");
			this.setStateMachineState(State.INIT);
			this.startAttemptTime = LocalDateTime.now();
		}
		case RUNNING -> {
			if (this.isError()) {
				this.setStateMachineState(State.ERROR);
			} else if (!this.isSystemRunning()) {
				this.setStateMachineState(State.UNDEFINED);
			} else {
				this.setStateMachineState(State.RUNNING);
				readyForWorking = true;
			}
		}
		case STOPPING -> {
			if (this.isError()) {
				this.setStateMachineState(State.ERROR);
			} else if (this.isSystemStopped()) {
				this.setStateMachineState(State.OFF);
			}
		}
		case UNDEFINED -> {
			if (this.isError()) {
				this.setStateMachineState(State.ERROR);
			} else if (this.isSystemStopped()) {
				this.setStateMachineState(State.OFF);
			} else if (this.isSystemRunning()) {
				this.setStateMachineState(State.RUNNING);
			} else if (this.isSystemStatePending()) {
				this.setStateMachineState(State.PENDING);
			}
		}
		case PENDING -> {
			if (this.pendingTimestamp == null) {
				this.pendingTimestamp = LocalDateTime.now();
			}
			if (this.pendingTimestamp.plusSeconds(this.config.pendingTolerance()).isBefore(LocalDateTime.now())) {
				// System state could not be determined, stop and start it
				this.pendingTimestamp = null;
				this.stopSystem();
				this.setStateMachineState(State.OFF);
			} else if (this.isError()) {
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
		case STANDBY -> {
		}
		}

		// this.getReadyForWorking().setNextValue(readyForWorking);
		if (readyForWorking) {
			this._setStartStop(StartStop.START);
		} else {
			this._setStartStop(StartStop.STOP);
		}
	}

	private void clearError() {
		BooleanWriteChannel clearErrorChannel = this.channel(BmwBattery.ChannelId.BMS_STATE_COMMAND_CLEAR_ERROR);
		try {
			clearErrorChannel.setNextWriteValue(true);
		} catch (OpenemsNamedException e) {
			// TODO should Fault state channel, but after start stop feature
			this.logError(this.log, "Error while trying to reset the system!");
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.handleBatteryState();

		}
	}

	private void handleBatteryState() {
		switch (this.config.batteryState()) {
		case DEFAULT -> this.handleStateMachine();
		case OFF -> this.stopSystem();
		case ON -> this.startSystem();
		}
	}

	// TODO Check this needed or not
	// public void shutDownBattery() {
	// SymmetricEss ess;
	// try {
	// ess = this.manager.getComponent(this.config.Inverter_id());
	// } catch (OpenemsNamedException e1) {
	//
	// e1.printStackTrace();
	// return;
	// }
	// int activePowerInverter = ess.getActivePower().value().orElse(0);
	// int reactivePowerInverter = ess.getReactivePower().value().orElse(0);
	//
	// if (activePowerInverter == 0 && reactivePowerInverter == 0) {
	// IntegerWriteChannel commandChannel =
	// this.channel(BMWChannelId.BMS_STATE_COMMAND);
	// try {
	// commandChannel.setNextWriteValue(OPEN_CONTACTORS);
	// } catch (OpenemsNamedException e) {
	// log.error("Problem occurred during send start command");
	// }
	// }
	//
	// }

	private boolean isSystemRunning() {
		EnumReadChannel bmsStateChannel = this.channel(BmwBattery.ChannelId.BMS_STATE);
		BmsState bmsState = bmsStateChannel.value().asEnum();
		return bmsState == BmsState.OPERATION;
	}

	private boolean isSystemStopped() {
		EnumReadChannel bmsStateChannel = this.channel(BmwBattery.ChannelId.BMS_STATE);
		BmsState bmsState = bmsStateChannel.value().asEnum();
		return bmsState == BmsState.OFF;
	}

	/**
	 * Checks whether system has an undefined state.
	 *
	 * @return true if system is neither running nor stopped
	 */
	private boolean isSystemStatePending() {
		return !this.isSystemRunning() && !this.isSystemStopped();
	}

	private boolean isError() {
		EnumReadChannel bmsStateChannel = this.channel(BmwBattery.ChannelId.BMS_STATE);
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
		IntegerWriteChannel commandChannel = this.channel(BmwBattery.ChannelId.BMS_STATE_COMMAND);
		try {
			commandChannel.setNextWriteValue(CLOSE_CONTACTORS);
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			this.logError(this.log, "Problem occurred during send start command");
		}
	}

	private void stopSystem() {
		// TODO Currently not necessary, Battery starts itself?!
		this.log.debug("Stop system");
		IntegerWriteChannel commandChannel = this.channel(BmwBattery.ChannelId.BMS_STATE_COMMAND);
		try {
			commandChannel.setNextWriteValue(OPEN_CONTACTORS);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Problem occurred during send stopping command");
		}
	}

	private State getStateMachineState() {
		return this.state;
	}

	private void setStateMachineState(State state) {
		this.state = state;
		this.channel(BmwBattery.ChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		return new ModbusProtocol(this, //

				new FC16WriteRegistersTask(1399, //
						m(BmwBattery.ChannelId.HEART_BEAT, new UnsignedWordElement(1399)), //
						m(BmwBattery.ChannelId.BMS_STATE_COMMAND, new UnsignedWordElement(1400)), //
						m(BmwBattery.ChannelId.OPERATING_STATE_INVERTER, new UnsignedWordElement(1401)), //
						m(BmwBattery.ChannelId.DC_LINK_VOLTAGE, new UnsignedWordElement(1402), SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.DC_LINK_CURRENT, new UnsignedWordElement(1403)), //
						m(BmwBattery.ChannelId.OPERATION_MODE_REQUEST_GRANTED, new UnsignedWordElement(1404)), //
						m(BmwBattery.ChannelId.OPERATION_MODE_REQUEST_CANCELED, new UnsignedWordElement(1405)), //
						m(new BitsWordElement(1406, this) //
								.bit(1, BmwBattery.ChannelId.CONNECTION_STRATEGY_HIGH_SOC_FIRST) //
								.bit(0, BmwBattery.ChannelId.CONNECTION_STRATEGY_LOW_SOC_FIRST) //
						), //
						m(BmwBattery.ChannelId.SYSTEM_TIME, new UnsignedDoublewordElement(1407)) //
				),

				new FC4ReadInputRegistersTask(999, Priority.HIGH,
						// not defined by "BCS_HL-SW_Operating-Instructions_V1.0.2_under_work_ChL.pdf"
						m(BmwBattery.ChannelId.LIFE_SIGN, new UnsignedWordElement(999)),
						m(BmwBattery.ChannelId.BMS_STATE, new UnsignedWordElement(1000)), //
						m(BmwBattery.ChannelId.ERROR_BITS_1, new UnsignedWordElement(1001)), //
						m(BmwBattery.ChannelId.ERROR_BITS_2, new UnsignedWordElement(1002)), //
						m(BmwBattery.ChannelId.WARNING_BITS_1, new UnsignedWordElement(1003)), //
						m(BmwBattery.ChannelId.WARNING_BITS_2, new UnsignedWordElement(1004)), //
						// not defined by "BCS_HL-SW_Operating-Instructions_V1.0.2_under_work_ChL.pdf"
						m(BmwBattery.ChannelId.INFO_BITS, new UnsignedWordElement(1005)),
						m(BmwBattery.ChannelId.MAXIMUM_OPERATING_CURRENT, new SignedWordElement(1006)), //
						m(BmwBattery.ChannelId.MINIMUM_OPERATING_CURRENT, new SignedWordElement(1007)), //
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(1008), SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(1009), SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new SignedWordElement(1010)), //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new SignedWordElement(1011), INVERT), //
						m(BmwBattery.ChannelId.MAXIMUM_LIMIT_DYNAMIC_VOLTAGE, new UnsignedWordElement(1012),
								SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.MINIMUM_LIMIT_DYNAMIC_VOLTAGE, new UnsignedWordElement(1013),
								SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.NUMBER_OF_STRINGS_CONNECTED, new UnsignedWordElement(1014)), //
						m(BmwBattery.ChannelId.NUMBER_OF_STRINGS_INSTALLED, new UnsignedWordElement(1015)), //
						m(BmwBattery.ChannelId.SOC_ALL_STRINGS, new UnsignedWordElement(1016), SCALE_FACTOR_MINUS_2), //
						m(Battery.ChannelId.SOC, new UnsignedWordElement(1017), SCALE_FACTOR_MINUS_2), //
						m(BmwBattery.ChannelId.REMAINING_CHARGE_CAPACITY, new UnsignedWordElement(1018)), //
						m(BmwBattery.ChannelId.REMAINING_DISCHARGE_CAPACITY, new UnsignedWordElement(1019)), //
						m(BmwBattery.ChannelId.REMAINING_CHARGE_ENERGY, new UnsignedWordElement(1020)), //
						m(BmwBattery.ChannelId.REMAINING_DISCHARGE_ENERGY, new UnsignedWordElement(1021)), //
						m(BmwBattery.ChannelId.NOMINAL_ENERGY, new UnsignedWordElement(1022)), //
						m(BmwBattery.ChannelId.TOTAL_ENERGY, new UnsignedWordElement(1023)), //
						m(BmwBattery.ChannelId.NOMINAL_CAPACITY, new UnsignedWordElement(1024)), //
						m(Battery.ChannelId.CAPACITY, new UnsignedWordElement(1025)), //
						m(Battery.ChannelId.SOH, new UnsignedWordElement(1026), SCALE_FACTOR_MINUS_2), //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(1027), SCALE_FACTOR_MINUS_1), //
						m(BmwBattery.ChannelId.DC_VOLTAGE_AVERAGE, new UnsignedWordElement(1028),
								SCALE_FACTOR_MINUS_1)), //
				new FC4ReadInputRegistersTask(1029, Priority.HIGH, //
						m(new SignedWordElement(1029)) //
								.m(BmwBattery.ChannelId.DC_CURRENT, SCALE_FACTOR_MINUS_1) //
								.m(Battery.ChannelId.CURRENT, SCALE_FACTOR_MINUS_1) //
								.build()), //
				new FC4ReadInputRegistersTask(1030, Priority.HIGH, //
						m(BmwBattery.ChannelId.AVERAGE_TEMPERATURE, new SignedWordElement(1030))), //
				new FC4ReadInputRegistersTask(1031, Priority.HIGH, //
						m(new SignedWordElement(1031)) //
								.m(BmwBattery.ChannelId.MINIMUM_TEMPERATURE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MIN_CELL_TEMPERATURE, DIRECT_1_TO_1) //
								.build()), //
				new FC4ReadInputRegistersTask(1032, Priority.HIGH, //
						m(new SignedWordElement(1032)) //
								.m(BmwBattery.ChannelId.MAXIMUM_TEMPERATURE, DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MAX_CELL_TEMPERATURE, DIRECT_1_TO_1) //
								.build()), //
				new FC4ReadInputRegistersTask(1033, Priority.HIGH,
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(1033)), //
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(1034)), //
						m(BmwBattery.ChannelId.AVERAGE_CELL_VOLTAGE, new UnsignedWordElement(1035)), //
						// not defined by "BCS_HL-SW_Operating-Instructions_V1.0.2_under_work_ChL.pdf"
						m(BmwBattery.ChannelId.INTERNAL_RESISTANCE, new UnsignedWordElement(1036)),
						m(BmwBattery.ChannelId.INSULATION_RESISTANCE, new UnsignedWordElement(1037), DIRECT_1_TO_1), //
						// not defined by "BCS_HL-SW_Operating-Instructions_V1.0.2_under_work_ChL.pdf"
						m(BmwBattery.ChannelId.CONTAINER_TEMPERATURE, new UnsignedWordElement(1038),
								SCALE_FACTOR_MINUS_1),
						// not defined by "BCS_HL-SW_Operating-Instructions_V1.0.2_under_work_ChL.pdf"
						m(BmwBattery.ChannelId.AMBIENT_TEMPERATURE, new UnsignedWordElement(1039),
								SCALE_FACTOR_MINUS_1),
						// not defined by "BCS_HL-SW_Operating-Instructions_V1.0.2_under_work_ChL.pdf"
						m(BmwBattery.ChannelId.HUMIDITY_CONTAINER, new UnsignedWordElement(1040), SCALE_FACTOR_MINUS_1),
						m(BmwBattery.ChannelId.MAXIMUM_LIMIT_DYNAMIC_CURRENT_HIGH_RES, new SignedWordElement(1041),
								SCALE_FACTOR_2), //
						m(BmwBattery.ChannelId.MINIMUM_LIMIT_DYNAMIC_CURRENT_HIGH_RES, new SignedWordElement(1042),
								SCALE_FACTOR_2), //
						m(BmwBattery.ChannelId.FULL_CYCLE_COUNT, new UnsignedWordElement(1043)), //
						// not defined by "BCS_HL-SW_Operating-Instructions_V1.0.2_under_work_ChL.pdf"
						m(BmwBattery.ChannelId.OPERATING_TIME_COUNT, new UnsignedDoublewordElement(1044)),
						// not defined by "BCS_HL-SW_Operating-Instructions_V1.0.2_under_work_ChL.pdf"
						m(BmwBattery.ChannelId.COM_PRO_VERSION, new UnsignedDoublewordElement(1046)),
						// not defined by "BCS_HL-SW_Operating-Instructions_V1.0.2_under_work_ChL.pdf"
						// not defined by "BCS_HL-SW_Operating-Instructions_V1.0.2_under_work_ChL.pdf"
						m(BmwBattery.ChannelId.SERIAL_NUMBER, new UnsignedDoublewordElement(1048)),
						// not defined by "BCS_HL-SW_Operating-Instructions_V1.0.2_under_work_ChL.pdf"
						m(BmwBattery.ChannelId.SOFTWARE_VERSION, new UnsignedDoublewordElement(1050)) //
				));
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO Auto-generated method stub
	}
}
