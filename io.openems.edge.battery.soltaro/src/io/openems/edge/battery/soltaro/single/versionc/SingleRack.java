package io.openems.edge.battery.soltaro.single.versionc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.ChannelIdImpl;
import io.openems.edge.battery.soltaro.ModuleParameters;
import io.openems.edge.battery.soltaro.ResetState;
import io.openems.edge.battery.soltaro.SoltaroBattery;
import io.openems.edge.battery.soltaro.State;
import io.openems.edge.battery.soltaro.single.versionc.enums.AutoSetFunction;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.battery.soltaro.single.versionc.enums.Sleep;
import io.openems.edge.battery.soltaro.single.versionc.enums.SystemReset;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bms.Soltaro.SingleRack.VersionC", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		})
public class SingleRack extends AbstractOpenemsModbusComponent
		implements Battery, OpenemsComponent, EventHandler, ModbusSlave {

	protected static final int SYSTEM_ON = 1;
	protected static final int SYSTEM_OFF = 0;

	private static final String KEY_TEMPERATURE = "_TEMPERATURE";
	private static final String KEY_VOLTAGE = "_VOLTAGE";
	private static final String NUMBER_FORMAT = "%03d"; // creates string number with leading zeros

	@Reference
	protected ConfigurationAdmin cm;

	private final Logger log = LoggerFactory.getLogger(SingleRack.class);
	private String modbusBridgeId;
	private State state = State.UNDEFINED;
	// if configuring is needed this is used to go through the necessary steps
	private ConfiguringProcess nextConfiguringProcess = ConfiguringProcess.NONE;
	private Config config;
	private Map<String, Channel<?>> channelMap;
	// If an error has occurred, this indicates the time when next action could be
	// done
	private LocalDateTime errorDelayIsOver = null;
	private int unsuccessfulStarts = 0;
	private LocalDateTime startAttemptTime = null;

	private LocalDateTime timeAfterAutoId = null;
	private LocalDateTime configuringFinished = null;
	private int delayAutoIdSeconds = 5;
	private int delayAfterConfiguringFinished = 5;

	private ResetState resetState = ResetState.NONE;
	private boolean resetDone;

	private LocalDateTime pendingTimestamp;

	public SingleRack() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				SingleRackChannelId.values(), //
				SoltaroBattery.ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.config = config;

		// adds dynamically created channels and save them into a map to access them
		// when modbus tasks are created
		channelMap = createDynamicChannels();

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		this.modbusBridgeId = config.modbus_id();

		setWatchdog(config.watchdog());
		setSoCLowAlarm(config.SoCLowAlarm());
		setCapacity();
	}

	private void setCapacity() {
		int capacity = this.config.numberOfSlaves() * this.config.moduleType().getCapacity_Wh();
		this.channel(Battery.ChannelId.CAPACITY).setNextValue(capacity);
	}

	private void handleStateMachine() {
		boolean readyForWorking = false;
		switch (this.getStateMachineState()) {
		case ERROR:
			// handle errors with resetting the system
			this.stopSystem();

			errorDelayIsOver = LocalDateTime.now().plusSeconds(config.errorLevel2Delay());
			setStateMachineState(State.ERRORDELAY);
			break;

		case ERRORDELAY:
			// If we are in the error delay time, the system is resetted, this can help
			// handling the rrors
			if (LocalDateTime.now().isAfter(errorDelayIsOver)) {
				errorDelayIsOver = null;
				resetDone = false;
				if (this.isError()) {
					this.setStateMachineState(State.ERROR);
				} else {
					this.setStateMachineState(State.UNDEFINED);
				}
			} else if (!resetDone) {
				this.handleErrorsWithReset();
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
					if (unsuccessfulStarts >= config.maxStartAppempts()) {
						errorDelayIsOver = LocalDateTime.now().plusSeconds(config.startUnsuccessfulDelay());
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
				this.checkAllowedCurrent();
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
		case ERROR_HANDLING:
			this.handleErrorsWithReset();
			break;
		}

		this.getReadyForWorking().setNextValue(readyForWorking);
	}

	private void checkAllowedCurrent() {
		if (isPoleTemperatureTooHot()) {
			this.limitMaxCurrent();
		}
	}

	private void limitMaxCurrent() {
		// TODO limit current
	}

	private boolean isPoleTemperatureTooHot() {
		StateChannel preAlarmChannel = this.channel(SingleRackChannelId.PRE_ALARM_POWER_POLE_HIGH);
		boolean preAlarm = preAlarmChannel.value().orElse(false);
		StateChannel level1Channel = this.channel(SingleRackChannelId.LEVEL1_POWER_POLE_TEMP_HIGH);
		boolean level1 = level1Channel.value().orElse(false);
		StateChannel level2Channel = this.channel(SingleRackChannelId.LEVEL2_POWER_POLE_TEMP_HIGH);
		boolean level2 = level2Channel.value().orElse(false);
		return preAlarm || level1 || level2;
	}

	private void handleErrorsWithReset() {
		// To reset , first sleep and then reset the system
		switch (this.resetState) {
		case NONE:
			this.resetState = ResetState.SLEEP;
			break;
		case SLEEP:
			this.sleepSystem();
			this.resetState = ResetState.RESET;
			break;
		case RESET:
			this.resetSystem();
			this.resetState = ResetState.FINISHED;
			break;
		case FINISHED:
			this.resetState = ResetState.NONE;
			this.setStateMachineState(State.ERRORDELAY);
			resetDone = true;
			break;
		}
	}

	private void resetSystem() {
		EnumWriteChannel resetChannel = this.channel(SingleRackChannelId.SYSTEM_RESET);
		try {
			resetChannel.setNextWriteValue(SystemReset.ACTIVATE);
		} catch (OpenemsNamedException e) {
			// TODO should throw an exception
			System.out.println("Error while trying to reset the system!");
		}
	}

	private void sleepSystem() {
		EnumWriteChannel sleepChannel = this.channel(SingleRackChannelId.SLEEP);
		try {
			sleepChannel.setNextWriteValue(Sleep.ACTIVATE);
		} catch (OpenemsNamedException e) {
			// TODO should throw an exception
			System.out.println("Error while trying to send the system to sleep!");
		}
	}

	/*
	 * creates a map containing channels for voltage and temperature depending on
	 * the number of modules
	 */
	private Map<String, Channel<?>> createDynamicChannels() {
		Map<String, Channel<?>> map = new HashMap<>();

		int voltSensors = ModuleParameters.VOLTAGE_SENSORS_PER_MODULE.getValue();
		for (int i = 0; i < this.config.numberOfSlaves(); i++) {
			for (int j = i * voltSensors; j < (i + 1) * voltSensors; j++) {
				String key = getSingleCellPrefix(j) + KEY_VOLTAGE;
				IntegerDoc doc = new IntegerDoc();
				io.openems.edge.common.channel.ChannelId channelId = new ChannelIdImpl(key, doc.unit(Unit.MILLIVOLT));
				IntegerReadChannel integerReadChannel = (IntegerReadChannel) this.addChannel(channelId);
				map.put(key, integerReadChannel);
			}
		}

		int tempSensors = ModuleParameters.TEMPERATURE_SENSORS_PER_MODULE.getValue();
		for (int i = 0; i < this.config.numberOfSlaves(); i++) {
			for (int j = i * tempSensors; j < (i + 1) * tempSensors; j++) {
				String key = getSingleCellPrefix(j) + KEY_TEMPERATURE;

				IntegerDoc doc = new IntegerDoc();
				io.openems.edge.common.channel.ChannelId channelId = new ChannelIdImpl(key,
						doc.unit(Unit.DEZIDEGREE_CELSIUS));
				IntegerReadChannel integerReadChannel = (IntegerReadChannel) this.addChannel(channelId);
				map.put(key, integerReadChannel);
			}
		}
		return map;
	}

	private String getSingleCellPrefix(int num) {
		return "CLUSTER_1_BATTERY_" + String.format(NUMBER_FORMAT, num);
	}

	private void setWatchdog(int time_seconds) {
		try {
			IntegerWriteChannel c = this.channel(SingleRackChannelId.EMS_COMMUNICATION_TIMEOUT);
			c.setNextWriteValue(time_seconds);
		} catch (OpenemsNamedException e) {
			log.error("Error while setting ems timeout!\n" + e.getMessage());
		}
	}

	@Deactivate
	protected void deactivate() {
		// Remove dynamically created channels when component is deactivated
		for (Channel<?> c : this.channelMap.values()) {
			this.removeChannel(c);
		}
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
		switch (config.batteryState()) {
		case DEFAULT:
			handleStateMachine();
			break;
		case OFF:
			stopSystem();
			break;
		case ON:
			startSystem();
			break;
		case CONFIGURE:
			configureSlaves();
			break;

		}
	}

	private void configureSlaves() {
		if (nextConfiguringProcess == ConfiguringProcess.NONE) {
			nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
		}

		switch (nextConfiguringProcess) {
		case CONFIGURING_STARTED:
			System.out.println(" ===> CONFIGURING STARTED: setNumberOfModules() <===");
			setNumberOfModules();
			break;
		case SET_ID_AUTO_CONFIGURING:
			System.out.println(" ===> SET_ID_AUTO_CONFIGURING: setIdAutoConfiguring() <===");
			setIdAutoConfiguring();
			break;
		case CHECK_ID_AUTO_CONFIGURING:
			if (timeAfterAutoId != null) {
				if (timeAfterAutoId.plusSeconds(delayAutoIdSeconds).isAfter(LocalDateTime.now())) {
					break;
				} else {
					timeAfterAutoId = null;
				}
			}
			System.out.println(" ===> CHECK_ID_AUTO_CONFIGURING: checkIdAutoConfiguring() <===");
			checkIdAutoConfiguring();
			break;
		case SET_TEMPERATURE_ID_AUTO_CONFIGURING:
			System.out.println(" ===> SET_TEMPERATURE_ID_AUTO_CONFIGURING: setTemperatureIdAutoConfiguring() <===");
			setTemperatureIdAutoConfiguring();
			break;
		case CHECK_TEMPERATURE_ID_AUTO_CONFIGURING:
			if (timeAfterAutoId != null) {
				if (timeAfterAutoId.plusSeconds(delayAutoIdSeconds).isAfter(LocalDateTime.now())) {
					break;
				} else {
					timeAfterAutoId = null;
				}
			}
			System.out.println(" ===> CHECK_TEMPERATURE_ID_AUTO_CONFIGURING: checkTemperatureIdAutoConfiguring() <===");
			checkTemperatureIdAutoConfiguring();
			break;
		case SET_VOLTAGE_RANGES:
			System.out.println(" ===> SET_VOLTAGE_RANGES: setVoltageRanges() <===");
			setVoltageRanges();

			break;
		case CONFIGURING_FINISHED:
			System.out.println("====>>> Configuring successful! <<<====");

			if (configuringFinished == null) {
				nextConfiguringProcess = ConfiguringProcess.RESTART_AFTER_SETTING;
			} else {
				if (configuringFinished.plusSeconds(delayAfterConfiguringFinished).isAfter(LocalDateTime.now())) {
					System.out.println(">>> Delay time after configuring!");
				} else {
					System.out.println("Delay time after configuring is over, reset system");
					this.logInfo(this.log,
							"Soltaro Rack Version C [CONFIGURING_FINISHED SYSTEM_RESET] is not implemented!");
					this.resetSystem();
				}
			}
			break;
		case RESTART_AFTER_SETTING:
			// A manual restart is needed
			System.out.println("====>>>  Please restart system manually!");
			break;
		case NONE:
			break;
		}
	}

	private void setVoltageRanges() {
		try {
			IntegerWriteChannel level1OverVoltageChannel = this
					.channel(SingleRackChannelId.LEVEL1_SYSTEM_OVER_VOLTAGE_PROTECTION);
			level1OverVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_OVER_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level1OverVoltageChannelRecover = this
					.channel(SingleRackChannelId.LEVEL1_SYSTEM_OVER_VOLTAGE_RECOVER);
			level1OverVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
					* ModuleParameters.LEVEL_1_TOTAL_OVER_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			IntegerWriteChannel level1LowVoltageChannel = this
					.channel(SingleRackChannelId.LEVEL1_SYSTEM_UNDER_VOLTAGE_PROTECTION);
			level1LowVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_LOW_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level1LowVoltageChannelRecover = this
					.channel(SingleRackChannelId.LEVEL1_SYSTEM_UNDER_VOLTAGE_RECOVER);
			level1LowVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
					* ModuleParameters.LEVEL_1_TOTAL_LOW_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			IntegerWriteChannel level2OverVoltageChannel = this
					.channel(SingleRackChannelId.LEVEL2_SYSTEM_OVER_VOLTAGE_PROTECTION);
			level2OverVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_OVER_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level2OverVoltageChannelRecover = this
					.channel(SingleRackChannelId.LEVEL2_SYSTEM_OVER_VOLTAGE_RECOVER);
			level2OverVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
					* ModuleParameters.LEVEL_2_TOTAL_OVER_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			IntegerWriteChannel level2LowVoltageChannel = this
					.channel(SingleRackChannelId.LEVEL2_SYSTEM_UNDER_VOLTAGE_PROTECTION);
			level2LowVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_LOW_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level2LowVoltageChannelRecover = this
					.channel(SingleRackChannelId.LEVEL2_SYSTEM_UNDER_VOLTAGE_RECOVER);
			level2LowVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
					* ModuleParameters.LEVEL_2_TOTAL_LOW_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			nextConfiguringProcess = ConfiguringProcess.CONFIGURING_FINISHED;
			configuringFinished = LocalDateTime.now();

		} catch (OpenemsNamedException e) {
			log.error("Setting voltage ranges not successful!");
			// TODO Should throw Exception/write Warning-State-Channel
		}
	}

	private void checkTemperatureIdAutoConfiguring() {
		EnumWriteChannel channel = this.channel(SingleRackChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID);
		AutoSetFunction value = channel.value().asEnum();
		switch (value) {
		case FAILURE:
			this.logError(this.log, "Auto set temperature slaves id failed! Start configuring process again!");
			// Auto set failed, try again
			this.nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
			return;
		case SUCCESS:
			this.logInfo(this.log, "Auto set temperature slaves id succeeded!");
			nextConfiguringProcess = ConfiguringProcess.SET_VOLTAGE_RANGES;
			return;
		case START_AUTO_SETTING:
		case INIT_MODE:
		case UNDEFINED:
			// Waiting...
			return;
		}
	}

	private void setTemperatureIdAutoConfiguring() {
		EnumWriteChannel channel = this.channel(SingleRackChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID);
		try {
			channel.setNextWriteValue(AutoSetFunction.START_AUTO_SETTING);
			this.timeAfterAutoId = LocalDateTime.now();
			this.nextConfiguringProcess = ConfiguringProcess.CHECK_TEMPERATURE_ID_AUTO_CONFIGURING;
		} catch (OpenemsNamedException e) {
			// Set was not successful, it will be tried until it succeeded
			this.logError(this.log, "Setting temperature id auto set not successful");
		}
	}

	private void checkIdAutoConfiguring() {
		EnumWriteChannel channel = this.channel(SingleRackChannelId.AUTO_SET_SLAVES_ID);
		AutoSetFunction value = channel.value().asEnum();
		switch (value) {
		case FAILURE:
			this.logError(this.log, "Auto set slaves id failed! Start configuring process again!");
			// Auto set failed, try again
			this.nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
			return;
		case SUCCESS:
			this.logInfo(this.log, "Auto set slaves id succeeded!");
			nextConfiguringProcess = ConfiguringProcess.SET_TEMPERATURE_ID_AUTO_CONFIGURING;
			return;
		case START_AUTO_SETTING:
		case INIT_MODE:
		case UNDEFINED:
			// Waiting...
			return;
		}
	}

	private void setIdAutoConfiguring() {
		EnumWriteChannel channel = this.channel(SingleRackChannelId.AUTO_SET_SLAVES_ID);
		try {
			channel.setNextWriteValue(AutoSetFunction.START_AUTO_SETTING);
			this.timeAfterAutoId = LocalDateTime.now();
			this.nextConfiguringProcess = ConfiguringProcess.CHECK_ID_AUTO_CONFIGURING;
		} catch (OpenemsNamedException e) {
			// Set was not successful, it will be tried until it succeeded
			this.logError(this.log, "Setting slave numbers not successful");
		}
	}

	private void setNumberOfModules() {
		// Set number of modules
		IntegerWriteChannel numberOfSlavesChannel = this
				.channel(SingleRackChannelId.WORK_PARAMETER_PCS_COMMUNICATION_RATE);
		try {
			numberOfSlavesChannel.setNextWriteValue(this.config.numberOfSlaves());
			nextConfiguringProcess = ConfiguringProcess.SET_ID_AUTO_CONFIGURING;
		} catch (OpenemsNamedException e) {
			// Set was not successful, it will be tried until it succeeded
			this.logError(this.log, "Setting slave numbers not successful. Will try again till it succeeds");
		}
	}

	private enum ConfiguringProcess {
		NONE, CONFIGURING_STARTED, SET_ID_AUTO_CONFIGURING, CHECK_ID_AUTO_CONFIGURING,
		SET_TEMPERATURE_ID_AUTO_CONFIGURING, CHECK_TEMPERATURE_ID_AUTO_CONFIGURING, SET_VOLTAGE_RANGES,
		CONFIGURING_FINISHED, RESTART_AFTER_SETTING
	}

	private boolean isSystemRunning() {
		EnumWriteChannel preChargeControlChannel = this.channel(SingleRackChannelId.PRE_CHARGE_CONTROL);
		PreChargeControl value = preChargeControlChannel.value().asEnum();
		switch (value) {
		case PRE_CHARGING:
		case SWITCH_OFF:
		case SWITCH_ON:
		case UNDEFINED:
			return false;
		case RUNNING:
			return true;
		}
		assert (true); // should never come here
		return false;
	}

	private boolean isSystemStopped() {
		EnumWriteChannel preChargeControlChannel = this.channel(SingleRackChannelId.PRE_CHARGE_CONTROL);
		PreChargeControl value = preChargeControlChannel.value().asEnum();
		switch (value) {
		case PRE_CHARGING:
		case SWITCH_ON:
		case RUNNING:
		case UNDEFINED:
			return false;
		case SWITCH_OFF:
			return true;
		}
		assert (true); // should never come here
		return false;
	}

	/**
	 * Checks whether system has an undefined state, e.g. rack 1 & 2 are configured,
	 * but only rack 1 is running. This state can only be reached at startup coming
	 * from state undefined
	 * 
	 * @return boolean
	 */
	private boolean isSystemStatePending() {
		return !isSystemRunning() && !isSystemStopped();
	}

	private boolean isAlarmLevel2Error() {
		return (readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_CELL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_TOTAL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_CHARGE_CURRENT_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_CELL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_TOTAL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_DISCHARGE_CURRENT_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_CHARGE_TEMP_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_CHARGE_TEMP_LOW)
				|| readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_POWER_POLE_TEMP_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_INSULATION_VALUE)
				|| readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_DISCHARGE_TEMP_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.LEVEL2_DISCHARGE_TEMP_LOW));
	}

	private boolean isSlaveCommunicationError() {
		boolean b = false;
		switch (this.config.numberOfSlaves()) {
		case 20:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_20_COMMUNICATION_ERROR);
		case 19:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_19_COMMUNICATION_ERROR);
		case 18:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_18_COMMUNICATION_ERROR);
		case 17:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_17_COMMUNICATION_ERROR);
		case 16:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_16_COMMUNICATION_ERROR);
		case 15:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_15_COMMUNICATION_ERROR);
		case 14:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_14_COMMUNICATION_ERROR);
		case 13:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_13_COMMUNICATION_ERROR);
		case 12:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_12_COMMUNICATION_ERROR);
		case 11:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_11_COMMUNICATION_ERROR);
		case 10:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_10_COMMUNICATION_ERROR);
		case 9:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_9_COMMUNICATION_ERROR);
		case 8:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_8_COMMUNICATION_ERROR);
		case 7:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_7_COMMUNICATION_ERROR);
		case 6:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_6_COMMUNICATION_ERROR);
		case 5:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_5_COMMUNICATION_ERROR);
		case 4:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_4_COMMUNICATION_ERROR);
		case 3:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_3_COMMUNICATION_ERROR);
		case 2:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_2_COMMUNICATION_ERROR);
		case 1:
			b = b || readValueFromBooleanChannel(SingleRackChannelId.SLAVE_1_COMMUNICATION_ERROR);
		}
		return b;
	}

	private boolean isError() {
		return isAlarmLevel2Error() || isSlaveCommunicationError();
	}

	private boolean readValueFromBooleanChannel(SingleRackChannelId singleRackChannelId) {
		StateChannel r = this.channel(singleRackChannelId);
		Optional<Boolean> bOpt = r.value().asOptional();
		return bOpt.isPresent() && bOpt.get();
	}

	public String getModbusBridgeId() {
		return modbusBridgeId;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value() //
				+ "|Discharge:" + this.getDischargeMinVoltage().value() + ";" + this.getDischargeMaxCurrent().value() //
				+ "|Charge:" + this.getChargeMaxVoltage().value() + ";" + this.getChargeMaxCurrent().value() //
				+ "|State:" + this.getStateMachineState();
	}

	private void startSystem() {
		EnumWriteChannel preChargeControlChannel = this.channel(SingleRackChannelId.PRE_CHARGE_CONTROL);
		// To avoid hardware damages do not send start command if system has already
		// started
		switch ((PreChargeControl) preChargeControlChannel.value().asEnum()) {
		case UNDEFINED: // -1 - No value yet
		case SWITCH_ON: // 1 - Connection Initiating
		case PRE_CHARGING: // 2 - Intermediate State
		case RUNNING: // 3 - Running
			return;
		case SWITCH_OFF:
			try {
				this.logInfo(this.log, "Setting PreChargeControl [SWITCH_ON]");
				preChargeControlChannel.setNextWriteValue(PreChargeControl.SWITCH_ON);
			} catch (OpenemsNamedException e) {
				// TODO should throw Exception
				this.logError(this.log, "Error while trying to start system" + e.getMessage());
			}
		}
	}

	private void stopSystem() {
		EnumWriteChannel preChargeControlChannel = this.channel(SingleRackChannelId.PRE_CHARGE_CONTROL);
		// To avoid hardware damages do not send start command if system has already
		// started
		switch ((PreChargeControl) preChargeControlChannel.value().asEnum()) {
		case UNDEFINED: // -1 - No value yet
		case SWITCH_OFF: // 0 - Switched Off
			return;
		case SWITCH_ON: // 1 - Connection Initiating
		case PRE_CHARGING: // 2 - Intermediate State
		case RUNNING: // 3 - Running
			try {
				this.logInfo(this.log, "Setting PreChargeControl [SWITCH_OFF]");
				preChargeControlChannel.setNextWriteValue(PreChargeControl.SWITCH_OFF);
			} catch (OpenemsNamedException e) {
				// TODO should throw Exception
				this.logError(this.log, "Error while trying to stop system" + e.getMessage());
			}
		}
	}

	public State getStateMachineState() {
		return this.state;
	}

	public void setStateMachineState(State state) {
		this.state = state;
		this.channel(SingleRackChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	private void setSoCLowAlarm(int soCLowAlarm) {
		try {
			IntegerWriteChannel protectionChannel = this.channel(SingleRackChannelId.LEVEL1_SOC_LOW_PROTECTION);
			protectionChannel.setNextWriteValue(soCLowAlarm);
			IntegerWriteChannel recoverChannel = this.channel(SingleRackChannelId.LEVEL1_SOC_LOW_PROTECTION_RECOVER);
			recoverChannel.setNextWriteValue(soCLowAlarm);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Error while setting parameter for soc low protection!" + e.getMessage());
			// TODO should throw exception
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		ModbusProtocol protocol = new ModbusProtocol(this, //
				new FC6WriteRegisterTask(0x2004, //
						m(SingleRackChannelId.SYSTEM_RESET, new UnsignedWordElement(0x2004)) //
				), //
				new FC6WriteRegisterTask(0x2010, //
						m(SingleRackChannelId.PRE_CHARGE_CONTROL, new UnsignedWordElement(0x2010)) //
				), //
				new FC6WriteRegisterTask(0x2014, //
						m(SingleRackChannelId.AUTO_SET_SLAVES_ID, new UnsignedWordElement(0x2014)) //
				), //
				new FC6WriteRegisterTask(0x2019, //
						m(SingleRackChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID, new UnsignedWordElement(0x2019)) //
				), //
				new FC6WriteRegisterTask(0x201D, //
						m(SingleRackChannelId.SLEEP, new UnsignedWordElement(0x201D)) //
				), //
				new FC16WriteRegistersTask(0x200B, //
						m(SingleRackChannelId.EMS_ADDRESS, new UnsignedWordElement(0x200B)), //
						m(SingleRackChannelId.EMS_BAUDRATE, new UnsignedWordElement(0x200C)) //
				), //
				new FC6WriteRegisterTask(0x20C1, //
						m(SingleRackChannelId.WORK_PARAMETER_PCS_COMMUNICATION_RATE, new UnsignedWordElement(0x20C1)) //
				), //
				new FC6WriteRegisterTask(0x20F4,
						m(SingleRackChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x20F4)) //
				), //
				new FC6WriteRegisterTask(0x20CC, //
						m(SingleRackChannelId.SYSTEM_TOTAL_CAPACITY, new UnsignedWordElement(0x20CC)) //
				), //
				new FC6WriteRegisterTask(0x2015, //
						m(SingleRackChannelId.SET_SUB_MASTER_ADDRESS, new UnsignedWordElement(0x2015)) //
				), //
				new FC6WriteRegisterTask(0x20F3, //
						m(SingleRackChannelId.VOLTAGE_LOW_PROTECTION, new UnsignedWordElement(0x20F3)) //
				), //
				new FC3ReadRegistersTask(0x20F4, Priority.LOW, //
						m(SingleRackChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x20F4)) //
				), //
				new FC3ReadRegistersTask(0x20CC, Priority.LOW, //
						m(SingleRackChannelId.SYSTEM_TOTAL_CAPACITY, new UnsignedWordElement(0x20CC)) //
				), //
				new FC3ReadRegistersTask(0x2015, Priority.LOW, //
						m(SingleRackChannelId.SET_SUB_MASTER_ADDRESS, new UnsignedWordElement(0x2015)) //
				), //
				new FC3ReadRegistersTask(0x20F3, Priority.LOW, //
						m(SingleRackChannelId.VOLTAGE_LOW_PROTECTION, new UnsignedWordElement(0x20F3)) //
				),
				// Single Cluster Running Status Registers
				new FC3ReadRegistersTask(0x2100, Priority.LOW, //
						m(new UnsignedWordElement(0x2100)) //
								.m(SingleRackChannelId.CLUSTER_1_VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_2) // [mV]
								.m(Battery.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [V]
								.build(), //
						m(new UnsignedWordElement(0x2101)) //
								.m(SingleRackChannelId.CLUSTER_1_CURRENT, ElementToChannelConverter.SCALE_FACTOR_2) // [mA]
								.m(Battery.ChannelId.CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [A]
								.build(), //
						m(SoltaroBattery.ChannelId.CHARGE_INDICATION, new UnsignedWordElement(0x2102)),
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x2103)), m(new UnsignedWordElement(0x2104)) //
								.m(SingleRackChannelId.CLUSTER_1_SOH, ElementToChannelConverter.DIRECT_1_TO_1) // [%]
								.m(Battery.ChannelId.SOH, ElementToChannelConverter.DIRECT_1_TO_1) // [%]
								.build(), //
						m(SingleRackChannelId.CLUSTER_1_MAX_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2105)), //
						m(new UnsignedWordElement(0x2106)) //
								.m(SingleRackChannelId.CLUSTER_1_MAX_CELL_VOLTAGE,
										ElementToChannelConverter.DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MAX_CELL_VOLTAGE, ElementToChannelConverter.DIRECT_1_TO_1) //
								.build(), //
						m(SingleRackChannelId.CLUSTER_1_MIN_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2107)), //
						m(new UnsignedWordElement(0x2108)) //
								.m(SingleRackChannelId.CLUSTER_1_MIN_CELL_VOLTAGE,
										ElementToChannelConverter.DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MIN_CELL_VOLTAGE, ElementToChannelConverter.DIRECT_1_TO_1) //
								.build(), //
						m(SingleRackChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x2109)), //
						m(new UnsignedWordElement(0x210A)) //
								.m(SingleRackChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE,
										ElementToChannelConverter.DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MAX_CELL_TEMPERATURE,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
								.build(), //
						m(SingleRackChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x210B)), //
						m(new UnsignedWordElement(0x210C)) //
								.m(SingleRackChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE,
										ElementToChannelConverter.DIRECT_1_TO_1) //
								.m(Battery.ChannelId.MIN_CELL_TEMPERATURE,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
								.build(), //
						m(SingleRackChannelId.CLUSTER_1_AVERAGE_VOLTAGE, new UnsignedWordElement(0x210D)), //
						m(SingleRackChannelId.CLUSTER_1_SYSTEM_INSULATION, new UnsignedWordElement(0x210E)), //
						m(new UnsignedWordElement(0x210F)) //
								.m(SingleRackChannelId.SYSTEM_MAX_CHARGE_CURRENT,
										ElementToChannelConverter.SCALE_FACTOR_2) //
								.m(Battery.ChannelId.CHARGE_MAX_CURRENT, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
								.build(), //
						m(new UnsignedWordElement(0x2110)) //
								.m(SingleRackChannelId.SYSTEM_MAX_DISCHARGE_CURRENT,
										ElementToChannelConverter.SCALE_FACTOR_2) //
								.m(Battery.ChannelId.DISCHARGE_MAX_CURRENT,
										ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
								.build(), //
						m(SingleRackChannelId.POSITIVE_INSULATION, new UnsignedWordElement(0x2111)), //
						m(SingleRackChannelId.NEGATIVE_INSULATION, new UnsignedWordElement(0x2112)), //
						m(SingleRackChannelId.CLUSTER_RUN_STATE, new UnsignedWordElement(0x2113)), //
						m(SingleRackChannelId.CLUSTER_1_AVG_TEMPERATURE, new UnsignedWordElement(0x2114)) //
				), //
				new FC3ReadRegistersTask(0x218b, Priority.LOW,
						m(SingleRackChannelId.CLUSTER_1_PROJECT_ID, new UnsignedWordElement(0x218b)), //
						m(SingleRackChannelId.CLUSTER_1_VERSION_MAJOR, new UnsignedWordElement(0x218c)), //
						m(SingleRackChannelId.CLUSTER_1_VERSION_SUB, new UnsignedWordElement(0x218d)), //
						m(SingleRackChannelId.CLUSTER_1_VERSION_MODIFY, new UnsignedWordElement(0x218e)) //
				), //

				// System Warning/Shut Down Status Registers
				new FC3ReadRegistersTask(0x2140, Priority.LOW, //
						// Level 2 Alarm: BMS Self-protect, main contactor shut down
						m(new BitsWordElement(0x2140, this) //
								.bit(0, SingleRackChannelId.LEVEL2_CELL_VOLTAGE_HIGH) //
								.bit(1, SingleRackChannelId.LEVEL2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SingleRackChannelId.LEVEL2_CHARGE_CURRENT_HIGH) //
								.bit(3, SingleRackChannelId.LEVEL2_CELL_VOLTAGE_LOW) //
								.bit(4, SingleRackChannelId.LEVEL2_TOTAL_VOLTAGE_LOW) //
								.bit(5, SingleRackChannelId.LEVEL2_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SingleRackChannelId.LEVEL2_CHARGE_TEMP_HIGH) //
								.bit(7, SingleRackChannelId.LEVEL2_CHARGE_TEMP_LOW) //
								// 8 -> Reserved
								// 9 -> Reserved
								.bit(10, SingleRackChannelId.LEVEL2_POWER_POLE_TEMP_HIGH) //
								// 11 -> Reserved
								.bit(12, SingleRackChannelId.LEVEL2_INSULATION_VALUE) //
								// 13 -> Reserved
								.bit(14, SingleRackChannelId.LEVEL2_DISCHARGE_TEMP_HIGH) //
								.bit(15, SingleRackChannelId.LEVEL2_DISCHARGE_TEMP_LOW) //
						), //
							// Level 1 Alarm: EMS Control to stop charge, discharge, charge&discharge
						m(new BitsWordElement(0x2141, this) //
								.bit(0, SingleRackChannelId.LEVEL1_CELL_VOLTAGE_HIGH) //
								.bit(1, SingleRackChannelId.LEVEL1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SingleRackChannelId.LEVEL1_CHARGE_CURRENT_HIGH) //
								.bit(3, SingleRackChannelId.LEVEL1_CELL_VOLTAGE_LOW) //
								.bit(4, SingleRackChannelId.LEVEL1_TOTAL_VOLTAGE_LOW) //
								.bit(5, SingleRackChannelId.LEVEL1_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SingleRackChannelId.LEVEL1_CHARGE_TEMP_HIGH) //
								.bit(7, SingleRackChannelId.LEVEL1_CHARGE_TEMP_LOW) //
								// 8 -> Reserved
								// 9 -> Reserved
								.bit(10, SingleRackChannelId.LEVEL1_POWER_POLE_TEMP_HIGH) //
								// 11 -> Reserved
								.bit(12, SingleRackChannelId.LEVEL1_INSULATION_VALUE) //
								// 13 -> Reserved
								.bit(14, SingleRackChannelId.LEVEL1_DISCHARGE_TEMP_HIGH) //
								.bit(15, SingleRackChannelId.LEVEL1_DISCHARGE_TEMP_LOW) //
						), //
							// Pre-Alarm: Temperature Alarm will active current limication
						m(new BitsWordElement(0x2142, this) //
								.bit(0, SingleRackChannelId.PRE_ALARM_CELL_VOLTAGE_HIGH) //
								.bit(1, SingleRackChannelId.PRE_ALARM_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SingleRackChannelId.PRE_ALARM_CHARGE_CURRENT_HIGH) //
								.bit(3, SingleRackChannelId.PRE_ALARM_CELL_VOLTAGE_LOW) //
								.bit(4, SingleRackChannelId.PRE_ALARM_TOTAL_VOLTAGE_LOW) //
								.bit(5, SingleRackChannelId.PRE_ALARM_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SingleRackChannelId.PRE_ALARM_CHARGE_TEMP_HIGH) //
								.bit(7, SingleRackChannelId.PRE_ALARM_CHARGE_TEMP_LOW) //
								.bit(8, SingleRackChannelId.PRE_ALARM_SOC_LOW) //
								.bit(9, SingleRackChannelId.PRE_ALARM_TEMP_DIFF_TOO_BIG) //
								.bit(10, SingleRackChannelId.PRE_ALARM_POWER_POLE_HIGH) //
								.bit(11, SingleRackChannelId.PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(12, SingleRackChannelId.PRE_ALARM_INSULATION_FAIL) //
								.bit(13, SingleRackChannelId.PRE_ALARM_TOTAL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(14, SingleRackChannelId.PRE_ALARM_DISCHARGE_TEMP_HIGH) //
								.bit(15, SingleRackChannelId.PRE_ALARM_DISCHARGE_TEMP_LOW) //
						) //
				), //

				// Other Alarm Info
				new FC3ReadRegistersTask(0x21A5, Priority.LOW, //
						m(new BitsWordElement(0x21A5, this) //
								.bit(0, SingleRackChannelId.ALARM_COMMUNICATION_TO_MASTER_BMS) //
								.bit(1, SingleRackChannelId.ALARM_COMMUNICATION_TO_SLAVE_BMS) //
								.bit(2, SingleRackChannelId.ALARM_COMMUNICATION_SLAVE_BMS_TO_TEMP_SENSORS) //
								.bit(3, SingleRackChannelId.ALARM_SLAVE_BMS_HARDWARE) //
						)), //

				// Slave BMS Fault Message Registers
				new FC3ReadRegistersTask(0x2180, Priority.LOW, //
						m(SingleRackChannelId.CYCLE_TIME, new UnsignedWordElement(0x2180)), //
						// TODO to be checked, was high + low bits
						m(SingleRackChannelId.TOTAL_CAPACITY, new UnsignedDoublewordElement(0x2181)),
						m(new BitsWordElement(0x2183, this) //
								.bit(3, SingleRackChannelId.SLAVE_20_COMMUNICATION_ERROR)//
								.bit(2, SingleRackChannelId.SLAVE_19_COMMUNICATION_ERROR)//
								.bit(1, SingleRackChannelId.SLAVE_18_COMMUNICATION_ERROR)//
								.bit(0, SingleRackChannelId.SLAVE_17_COMMUNICATION_ERROR)//
						), //
						m(new BitsWordElement(0x2184, this) //
								.bit(15, SingleRackChannelId.SLAVE_16_COMMUNICATION_ERROR)//
								.bit(14, SingleRackChannelId.SLAVE_15_COMMUNICATION_ERROR)//
								.bit(13, SingleRackChannelId.SLAVE_14_COMMUNICATION_ERROR)//
								.bit(12, SingleRackChannelId.SLAVE_13_COMMUNICATION_ERROR)//
								.bit(11, SingleRackChannelId.SLAVE_12_COMMUNICATION_ERROR)//
								.bit(10, SingleRackChannelId.SLAVE_11_COMMUNICATION_ERROR)//
								.bit(9, SingleRackChannelId.SLAVE_10_COMMUNICATION_ERROR)//
								.bit(8, SingleRackChannelId.SLAVE_9_COMMUNICATION_ERROR)//
								.bit(7, SingleRackChannelId.SLAVE_8_COMMUNICATION_ERROR)//
								.bit(6, SingleRackChannelId.SLAVE_7_COMMUNICATION_ERROR)//
								.bit(5, SingleRackChannelId.SLAVE_6_COMMUNICATION_ERROR)//
								.bit(4, SingleRackChannelId.SLAVE_5_COMMUNICATION_ERROR)//
								.bit(3, SingleRackChannelId.SLAVE_4_COMMUNICATION_ERROR)//
								.bit(2, SingleRackChannelId.SLAVE_3_COMMUNICATION_ERROR)//
								.bit(1, SingleRackChannelId.SLAVE_2_COMMUNICATION_ERROR)//
								.bit(0, SingleRackChannelId.SLAVE_1_COMMUNICATION_ERROR)//
						), //
						m(new BitsWordElement(0x2185, this) //
								.bit(0, SingleRackChannelId.SLAVE_BMS_VOLTAGE_SENSOR_CABLES)//
								.bit(1, SingleRackChannelId.SLAVE_BMS_POWER_CABLE)//
								.bit(2, SingleRackChannelId.SLAVE_BMS_LTC6803)//
								.bit(3, SingleRackChannelId.SLAVE_BMS_VOLTAGE_SENSORS)//
								.bit(4, SingleRackChannelId.SLAVE_BMS_TEMP_SENSOR_CABLES)//
								.bit(5, SingleRackChannelId.SLAVE_BMS_TEMP_SENSORS)//
								.bit(6, SingleRackChannelId.SLAVE_BMS_POWER_POLE_TEMP_SENSOR)//
								.bit(7, SingleRackChannelId.SLAVE_BMS_TEMP_BOARD_COM)//
								.bit(8, SingleRackChannelId.SLAVE_BMS_BALANCE_MODULE)//
								.bit(9, SingleRackChannelId.SLAVE_BMS_TEMP_SENSORS2)//
								.bit(10, SingleRackChannelId.SLAVE_BMS_INTERNAL_COM)//
								.bit(11, SingleRackChannelId.SLAVE_BMS_EEPROM)//
								.bit(12, SingleRackChannelId.SLAVE_BMS_INIT)//
						))); //
		{
			AbstractModbusElement<?>[] elements = new AbstractModbusElement<?>[] {
					m(SingleRackChannelId.PRE_ALARM_CELL_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2080)), //
					m(SingleRackChannelId.PRE_ALARM_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2081)), //
					m(SingleRackChannelId.PRE_ALARM_SYSTEM_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2082),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.PRE_ALARM_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2083),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_ALARM, new UnsignedWordElement(0x2084),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_RECOVER, new UnsignedWordElement(0x2085),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.PRE_ALARM_CELL_UNDER_VOLTAGE_ALARM, new UnsignedWordElement(0x2086)), //
					m(SingleRackChannelId.PRE_ALARM_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2087)), //
					m(SingleRackChannelId.PRE_ALARM_SYSTEM_UNDER_VOLTAGE_ALARM, new UnsignedWordElement(0x2088),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.PRE_ALARM_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2089),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x208A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x208B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.PRE_ALARM_CELL_OVER_TEMPERATURE_ALARM, new UnsignedWordElement(0x208C)), //
					m(SingleRackChannelId.PRE_ALARM_CELL_OVER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x208D)), //
					m(SingleRackChannelId.PRE_ALARM_CELL_UNDER_TEMPERATURE_ALARM, new UnsignedWordElement(0x208E)), //
					m(SingleRackChannelId.PRE_ALARM_CELL_UNDER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x208F)), //
					m(SingleRackChannelId.PRE_ALARM_SOC_LOW_ALARM, new UnsignedWordElement(0x2090)), //
					m(SingleRackChannelId.PRE_ALARM_SOC_LOW_ALARM_RECOVER, new UnsignedWordElement(0x2091)), //
					new DummyRegisterElement(0x2092, 0x2093),
					m(SingleRackChannelId.PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM, new UnsignedWordElement(0x2094)), //
					m(SingleRackChannelId.PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER,
							new UnsignedWordElement(0x2095)), //
					m(SingleRackChannelId.PRE_ALARM_INSULATION_ALARM, new UnsignedWordElement(0x2096)), //
					m(SingleRackChannelId.PRE_ALARM_INSULATION_ALARM_RECOVER, new UnsignedWordElement(0x2097)), //
					m(SingleRackChannelId.PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM, new UnsignedWordElement(0x2098)), //
					m(SingleRackChannelId.PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x2099)), //
					m(SingleRackChannelId.PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM, new UnsignedWordElement(0x209A),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x209B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM, new UnsignedWordElement(0x209C)), //
					m(SingleRackChannelId.PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER,
							new UnsignedWordElement(0x209D)), //
					m(SingleRackChannelId.PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM, new UnsignedWordElement(0x209E)), //
					m(SingleRackChannelId.PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER,
							new UnsignedWordElement(0x209F)), //
					m(SingleRackChannelId.PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM, new UnsignedWordElement(0x20A0)), //
					m(SingleRackChannelId.PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x20A1)) //
			};
			protocol.addTask(new FC16WriteRegistersTask(0x2080, elements));
			protocol.addTask(new FC3ReadRegistersTask(0x2080, Priority.LOW, elements));
		}

		// WARN_LEVEL1 (Level1 warning registers RW)
		{
			AbstractModbusElement<?>[] elements = new AbstractModbusElement<?>[] {
					m(SingleRackChannelId.LEVEL1_CELL_OVER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2040)), //
					m(SingleRackChannelId.LEVEL1_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2041)), //
					m(SingleRackChannelId.LEVEL1_SYSTEM_OVER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2042),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL1_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2043),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION, new UnsignedWordElement(0x2044),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_RECOVER, new UnsignedWordElement(0x2045),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL1_CELL_UNDER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2046)), //
					m(SingleRackChannelId.LEVEL1_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)), //
					m(SingleRackChannelId.LEVEL1_SYSTEM_UNDER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2048),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL1_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2049),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x204A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER, new UnsignedWordElement(0x204B),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL1_CELL_OVER_TEMPERATURE_PROTECTION, new UnsignedWordElement(0x204C)), //
					m(SingleRackChannelId.LEVEL1_CELL_OVER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x204D)), //
					m(SingleRackChannelId.LEVEL1_CELL_UNDER_TEMPERATURE_PROTECTION, new UnsignedWordElement(0x204E)), //
					m(SingleRackChannelId.LEVEL1_CELL_UNDER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x204F)), //
					m(SingleRackChannelId.LEVEL1_SOC_LOW_PROTECTION, new UnsignedWordElement(0x2050)), //
					m(SingleRackChannelId.LEVEL1_SOC_LOW_PROTECTION_RECOVER, new UnsignedWordElement(0x2051)), //
					new DummyRegisterElement(0x2052, 0x2053), //
					m(SingleRackChannelId.LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x2054)), //
					m(SingleRackChannelId.LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2055)), //
					m(SingleRackChannelId.LEVEL1_INSULATION_PROTECTION, new UnsignedWordElement(0x2056)), //
					m(SingleRackChannelId.LEVEL1_INSULATION_PROTECTION_RECOVER, new UnsignedWordElement(0x2057)), //
					m(SingleRackChannelId.LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION, new UnsignedWordElement(0x2058)), //
					m(SingleRackChannelId.LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2059)), //
					m(SingleRackChannelId.LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION, new UnsignedWordElement(0x205A),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x205C)), //
					m(SingleRackChannelId.LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205D)), //
					m(SingleRackChannelId.LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION, new UnsignedWordElement(0x205E)), //
					m(SingleRackChannelId.LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205F)), //
					m(SingleRackChannelId.LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION, new UnsignedWordElement(0x2060)), //
					m(SingleRackChannelId.LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2061)) //
			};
			protocol.addTask(new FC16WriteRegistersTask(0x2040, elements));
			protocol.addTask(new FC3ReadRegistersTask(0x2040, Priority.LOW, elements));
		}

		// WARN_LEVEL2 (Level2 Protection registers RW)
		{
			AbstractModbusElement<?>[] elements = new AbstractModbusElement<?>[] {
					m(SingleRackChannelId.LEVEL2_CELL_OVER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2400)), //
					m(SingleRackChannelId.LEVEL2_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2401)), //
					m(new UnsignedWordElement(0x2402)) //
							.m(SingleRackChannelId.LEVEL2_SYSTEM_OVER_VOLTAGE_PROTECTION,
									ElementToChannelConverter.SCALE_FACTOR_2) // [mV]
							.m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [V]
							.build(), //
					m(SingleRackChannelId.LEVEL2_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2403),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION, new UnsignedWordElement(0x2404),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_RECOVER, new UnsignedWordElement(0x2405),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL2_CELL_UNDER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2406)), //
					m(SingleRackChannelId.LEVEL2_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2407)), //
					m(new UnsignedWordElement(0x2408)) //
							.m(SingleRackChannelId.LEVEL2_SYSTEM_UNDER_VOLTAGE_PROTECTION,
									ElementToChannelConverter.SCALE_FACTOR_2) // [mV]
							.m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [V]
							.build(), //
					m(SingleRackChannelId.LEVEL2_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2409),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x240A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER, new UnsignedWordElement(0x240B),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL2_CELL_OVER_TEMPERATURE_PROTECTION, new UnsignedWordElement(0x240C)), //
					m(SingleRackChannelId.LEVEL2_CELL_OVER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x240D)), //
					m(SingleRackChannelId.LEVEL2_CELL_UNDER_TEMPERATURE_PROTECTION, new UnsignedWordElement(0x240E)), //
					m(SingleRackChannelId.LEVEL2_CELL_UNDER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x240F)), //
					m(SingleRackChannelId.LEVEL2_SOC_LOW_PROTECTION, new UnsignedWordElement(0x2410)), //
					m(SingleRackChannelId.LEVEL2_SOC_LOW_PROTECTION_RECOVER, new UnsignedWordElement(0x2411)), //
					new DummyRegisterElement(0x2412, 0x2413), //
					m(SingleRackChannelId.LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x2414)), //
					m(SingleRackChannelId.LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2415)), //
					m(SingleRackChannelId.LEVEL2_INSULATION_PROTECTION, new UnsignedWordElement(0x2416)), //
					m(SingleRackChannelId.LEVEL2_INSULATION_PROTECTION_RECOVER, new UnsignedWordElement(0x2417)), //
					m(SingleRackChannelId.LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION, new UnsignedWordElement(0x2418)), //
					m(SingleRackChannelId.LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2419)), //
					m(SingleRackChannelId.LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION, new UnsignedWordElement(0x241A),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x241B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x241C)), //
					m(SingleRackChannelId.LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x241D)), //
					m(SingleRackChannelId.LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION, new UnsignedWordElement(0x241E)), //
					m(SingleRackChannelId.LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER,
							new UnsignedWordElement(0x241F)), //
					m(SingleRackChannelId.LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION, new UnsignedWordElement(0x2420)), //
					m(SingleRackChannelId.LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2421)) //
			};
			protocol.addTask(new FC16WriteRegistersTask(0x2400, elements));
			protocol.addTask(new FC3ReadRegistersTask(0x2400, Priority.LOW, elements));
		}

		// Add tasks for cell voltages and temperatures according to the number of
		// slaves, one task per module is created
		// Cell voltages
		int offset = ModuleParameters.ADDRESS_OFFSET.getValue();
		int voltOffset = ModuleParameters.VOLTAGE_ADDRESS_OFFSET.getValue();
		int voltSensors = ModuleParameters.VOLTAGE_SENSORS_PER_MODULE.getValue();
		for (int i = 0; i < this.config.numberOfSlaves(); i++) {
			Collection<AbstractModbusElement<?>> elements = new ArrayList<>();
			for (int j = i * voltSensors; j < (i + 1) * voltSensors; j++) {
				String key = getSingleCellPrefix(j) + KEY_VOLTAGE;
				UnsignedWordElement uwe = new UnsignedWordElement(offset + voltOffset + j);
				AbstractModbusElement<?> ame = m(channelMap.get(key).channelId(), uwe);
				elements.add(ame);
			}
			protocol.addTask(new FC3ReadRegistersTask(offset + voltOffset + i * voltSensors, Priority.LOW,
					elements.toArray(new AbstractModbusElement<?>[0])));
		}

		// Cell temperatures
		int tempOffset = ModuleParameters.TEMPERATURE_ADDRESS_OFFSET.getValue();
		int tempSensors = ModuleParameters.TEMPERATURE_SENSORS_PER_MODULE.getValue();
		for (int i = 0; i < this.config.numberOfSlaves(); i++) {
			Collection<AbstractModbusElement<?>> elements = new ArrayList<>();
			for (int j = i * tempSensors; j < (i + 1) * tempSensors; j++) {
				String key = getSingleCellPrefix(j) + KEY_TEMPERATURE;
				SignedWordElement swe = new SignedWordElement(offset + tempOffset + j);
				AbstractModbusElement<?> ame = m(channelMap.get(key).channelId(), swe);
				elements.add(ame);
			}
			protocol.addTask(new FC3ReadRegistersTask(offset + tempOffset + i * tempSensors, Priority.LOW,
					elements.toArray(new AbstractModbusElement<?>[0])));
		}

		return protocol;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

}
