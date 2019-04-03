package io.openems.edge.battery.soltaro.versionb;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.multirack.ChannelIdImpl;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.AutoSetFunction;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.ContactorControl;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.Fenecon.Soltaro.VersionB", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class SoltaroRackVersionB extends AbstractOpenemsModbusComponent
		implements Battery, OpenemsComponent, EventHandler { // , JsonApi TODO

	protected static final int SYSTEM_ON = 1;
	protected final static int SYSTEM_OFF = 0;

	private static final String KEY_TEMPERATURE = "_TEMPERATURE";
	private static final String KEY_VOLTAGE = "_VOLTAGE";
	private static final Integer SYSTEM_RESET = 0x1;
	private static final String NUMBER_FORMAT = "%03d"; // creates string number with leading zeros

	@Reference
	protected ConfigurationAdmin cm;

	private final Logger log = LoggerFactory.getLogger(SoltaroRackVersionB.class);
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
	private int DELAY_AUTO_ID_SECONDS = 5;
	private int DELAY_AFTER_CONFIGURING_FINISHED = 5;

	public SoltaroRackVersionB() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				VersionBChannelId.values() //
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

		super.activate(context, config.id(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		this.modbusBridgeId = config.modbus_id();
		initializeCallbacks();

		setWatchdog(config.watchdog());
		setSoCLowAlarm(config.SoCLowAlarm());
		setCapacity();
	}

	private void setCapacity() {
		int capacity = this.config.numberOfSlaves() * ModuleParameters.CAPACITY_WH.getValue() / 1000;
		this.channel(Battery.ChannelId.CAPACITY).setNextValue(capacity);
	}

	private void handleStateMachine() {
		log.info("SoltaroRackVersionB.handleStateMachine(): State: " + this.getStateMachineState());
		boolean readyForWorking = false;
		switch (this.getStateMachineState()) {
		case ERROR:
			stopSystem();
			errorDelayIsOver = LocalDateTime.now().plusSeconds(config.errorLevel2Delay());
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
			if (this.isSystemIsRunning()) {
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
			} else {
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
			if (isSystemStateUndefined()) { // do nothing until state is clearly defined
				log.info(" ===>>> STATE is currently undefined! <<<===");
				break;
			}
			if (this.isError()) {
				this.setStateMachineState(State.ERROR);
			} else if (this.isSystemStopped()) {
				this.setStateMachineState(State.OFF);
			} else if (this.isSystemIsRunning()) {
				this.setStateMachineState(State.RUNNING);
			}
			break;
		}

		this.getReadyForWorking().setNextValue(readyForWorking);
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
			IntegerWriteChannel c = this.channel(VersionBChannelId.EMS_COMMUNICATION_TIMEOUT);
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

	private void initializeCallbacks() {

		this.channel(VersionBChannelId.CLUSTER_1_VOLTAGE).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> vOpt = (Optional<Integer>) value.asOptional();
			if (!vOpt.isPresent()) {
				return;
			}
			int voltage_volt = (int) (vOpt.get() * 0.001);
			log.debug("callback voltage, value: " + voltage_volt);
			this.channel(Battery.ChannelId.VOLTAGE).setNextValue(voltage_volt);
		});

		this.channel(VersionBChannelId.CLUSTER_1_MIN_CELL_VOLTAGE).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> vOpt = (Optional<Integer>) value.asOptional();
			if (!vOpt.isPresent()) {
				return;
			}
			int voltage_millivolt = vOpt.get();
			log.debug("callback min cell voltage, value: " + voltage_millivolt);
			this.channel(Battery.ChannelId.MIN_CELL_VOLTAGE).setNextValue(voltage_millivolt);
		});

		// write battery ranges to according channels in battery api
		// MAX_VOLTAGE x2082
		this.channel(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> vOpt = (Optional<Integer>) value.asOptional();
			if (!vOpt.isPresent()) {
				return;
			}
			int max_charge_voltage = (int) (vOpt.get() * 0.001);
			log.debug("callback battery range, max charge voltage, value: " + max_charge_voltage);
			this.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE).setNextValue(max_charge_voltage);
		});

		// DISCHARGE_MIN_VOLTAGE 0x2088
		this.channel(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> vOpt = (Optional<Integer>) value.asOptional();
			if (!vOpt.isPresent()) {
				return;
			}
			int min_discharge_voltage = (int) (vOpt.get() * 0.001);
			log.debug("callback battery range, min discharge voltage, value: " + min_discharge_voltage);
			this.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE).setNextValue(min_discharge_voltage);
		});

		// CHARGE_MAX_CURRENT 0x2160
		this.channel(VersionBChannelId.SYSTEM_MAX_CHARGE_CURRENT).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> cOpt = (Optional<Integer>) value.asOptional();
			if (!cOpt.isPresent()) {
				return;
			}
			int max_current = (int) (cOpt.get() * 0.001);
			log.debug("callback battery range, max charge current, value: " + max_current);
			this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).setNextValue(max_current);
		});

		// DISCHARGE_MAX_CURRENT 0x2161
		this.channel(VersionBChannelId.SYSTEM_MAX_DISCHARGE_CURRENT).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> cOpt = (Optional<Integer>) value.asOptional();
			if (!cOpt.isPresent()) {
				return;
			}
			int max_current = (int) (cOpt.get() * 0.001);
			log.debug("callback battery range, max discharge current, value: " + max_current);
			this.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT).setNextValue(max_current);
		});

	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			handleBatteryState();
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
		case CONFIGURE_SLAVES:
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
			log.info(" ===> CONFIGURING STARTED: setNumberOfModules() <===");
			setNumberOfModules();
			break;
		case SET_ID_AUTO_CONFIGURING:
			log.info(" ===> SET_ID_AUTO_CONFIGURING: setIdAutoConfiguring() <===");
			setIdAutoConfiguring();
			break;
		case CHECK_ID_AUTO_CONFIGURING:
			if (timeAfterAutoId != null) {
				if (timeAfterAutoId.plusSeconds(DELAY_AUTO_ID_SECONDS).isAfter(LocalDateTime.now())) {
					break;
				} else {
					timeAfterAutoId = null;
				}
			}
			log.info(" ===> CHECK_ID_AUTO_CONFIGURING: checkIdAutoConfiguring() <===");
			checkIdAutoConfiguring();
			break;
		case SET_TEMPERATURE_ID_AUTO_CONFIGURING:
			log.info(" ===> SET_TEMPERATURE_ID_AUTO_CONFIGURING: setTemperatureIdAutoConfiguring() <===");
			setTemperatureIdAutoConfiguring();
			break;
		case CHECK_TEMPERATURE_ID_AUTO_CONFIGURING:
			if (timeAfterAutoId != null) {
				if (timeAfterAutoId.plusSeconds(DELAY_AUTO_ID_SECONDS).isAfter(LocalDateTime.now())) {
					break;
				} else {
					timeAfterAutoId = null;
				}
			}
			log.info(" ===> CHECK_TEMPERATURE_ID_AUTO_CONFIGURING: checkTemperatureIdAutoConfiguring() <===");
			checkTemperatureIdAutoConfiguring();
			break;
		case SET_VOLTAGE_RANGES:
			log.info(" ===> SET_VOLTAGE_RANGES: setVoltageRanges() <===");
			setVoltageRanges();

			break;
		case CONFIGURING_FINISHED:
			log.info("====>>> Configuring successful! <<<====");

			if (configuringFinished == null) {
				nextConfiguringProcess = ConfiguringProcess.RESTART_AFTER_SETTING;
			} else {
				if (configuringFinished.plusSeconds(DELAY_AFTER_CONFIGURING_FINISHED).isAfter(LocalDateTime.now())) {
					log.info(">>> Delay time after configuring!");
				} else {
					log.info("Delay time after configuring is over, reset system");
					IntegerWriteChannel resetChannel = this.channel(VersionBChannelId.SYSTEM_RESET);
					try {
						resetChannel.setNextWriteValue(SYSTEM_RESET);
						configuringFinished = null;
					} catch (OpenemsNamedException e) {
						log.error("Error while trying to reset the system!");
					}
				}
			}
			break;
		case RESTART_AFTER_SETTING:
			// A manual restart is needed
		case NONE:
			break;
		}
	}

	private void setVoltageRanges() {

		try {
			IntegerWriteChannel level1OverVoltageChannel = this
					.channel(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM);
			level1OverVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_OVER_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level1OverVoltageChannelRecover = this
					.channel(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER);
			level1OverVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
					* ModuleParameters.LEVEL_1_TOTAL_OVER_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			IntegerWriteChannel level1LowVoltageChannel = this
					.channel(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM);
			level1LowVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_LOW_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level1LowVoltageChannelRecover = this
					.channel(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER);
			level1LowVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
					* ModuleParameters.LEVEL_1_TOTAL_LOW_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			IntegerWriteChannel level2OverVoltageChannel = this
					.channel(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION);
			level2OverVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_OVER_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level2OverVoltageChannelRecover = this
					.channel(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER);
			level2OverVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
					* ModuleParameters.LEVEL_2_TOTAL_OVER_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			IntegerWriteChannel level2LowVoltageChannel = this
					.channel(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION);
			level2LowVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_LOW_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level2LowVoltageChannelRecover = this
					.channel(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER);
			level2LowVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
					* ModuleParameters.LEVEL_2_TOTAL_LOW_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			nextConfiguringProcess = ConfiguringProcess.CONFIGURING_FINISHED;
			configuringFinished = LocalDateTime.now();

		} catch (OpenemsNamedException e) {
			log.error("Setting voltage ranges not successful!");
		}

	}

	private void checkTemperatureIdAutoConfiguring() {
		IntegerReadChannel autoSetTemperatureSlavesIdChannel = this
				.channel(VersionBChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID);
		Optional<Integer> autoSetTemperatureSlavesIdOpt = autoSetTemperatureSlavesIdChannel.value().asOptional();
		if (!autoSetTemperatureSlavesIdOpt.isPresent()) {
			return;
		}
		int autoSetTemperatureSlaves = autoSetTemperatureSlavesIdOpt.get();
		if (autoSetTemperatureSlaves == VersionBEnums.AutoSetFunction.FAILURE.getValue()) {
			log.error("Auto set temperature slaves id failed! Start configuring process again!");
			// Auto set failed, try again
			nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
		} else if (autoSetTemperatureSlaves == VersionBEnums.AutoSetFunction.SUCCES.getValue()) {
			log.info("Auto set temperature slaves id succeeded!");
			nextConfiguringProcess = ConfiguringProcess.SET_VOLTAGE_RANGES;
		}
	}

	private void setTemperatureIdAutoConfiguring() {

		IntegerWriteChannel autoSetSlavesTemperatureIdChannel = this
				.channel(VersionBChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID);
		try {
			autoSetSlavesTemperatureIdChannel.setNextWriteValue(AutoSetFunction.START_AUTO_SETTING.getValue());
			timeAfterAutoId = LocalDateTime.now();
			nextConfiguringProcess = ConfiguringProcess.CHECK_TEMPERATURE_ID_AUTO_CONFIGURING;
		} catch (OpenemsNamedException e) {
			log.error("Setting temperature id auto set not successful"); // Set was not successful, it will be tried
																			// until it succeeded
		}
	}

	private void checkIdAutoConfiguring() {
		IntegerReadChannel autoSetSlavesIdChannel = this.channel(VersionBChannelId.AUTO_SET_SLAVES_ID);
		Optional<Integer> autoSetSlavesIdOpt = autoSetSlavesIdChannel.value().asOptional();
		if (!autoSetSlavesIdOpt.isPresent()) {
			return;
		}
		int autoSetSlaves = autoSetSlavesIdOpt.get();
		if (autoSetSlaves == VersionBEnums.AutoSetFunction.FAILURE.getValue()) {
			log.error("Auto set slaves id failed! Start configuring process again!");
			// Auto set failed, try again
			nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
		} else if (autoSetSlaves == VersionBEnums.AutoSetFunction.SUCCES.getValue()) {
			log.info("Auto set slaves id succeeded!");
			nextConfiguringProcess = ConfiguringProcess.SET_TEMPERATURE_ID_AUTO_CONFIGURING;
		}
	}

	private void setIdAutoConfiguring() {
		// Set number of modules
		IntegerWriteChannel autoSetSlavesIdChannel = this.channel(VersionBChannelId.AUTO_SET_SLAVES_ID);
		try {
			autoSetSlavesIdChannel.setNextWriteValue(AutoSetFunction.START_AUTO_SETTING.getValue());
			timeAfterAutoId = LocalDateTime.now();
			nextConfiguringProcess = ConfiguringProcess.CHECK_ID_AUTO_CONFIGURING;
		} catch (OpenemsNamedException e) {
			log.error("Setting slave numbers not successful"); // Set was not successful, it will be tried until it
																// succeeded
		}
	}

	private void setNumberOfModules() {
		// Set number of modules
		IntegerWriteChannel numberOfSlavesChannel = this
				.channel(VersionBChannelId.WORK_PARAMETER_PCS_COMMUNICATION_RATE);
		try {
			numberOfSlavesChannel.setNextWriteValue(this.config.numberOfSlaves());
			nextConfiguringProcess = ConfiguringProcess.SET_ID_AUTO_CONFIGURING;
		} catch (OpenemsNamedException e) {
			log.error("Setting slave numbers not successful"); // Set was not successful, it will be tried until it
																// succeeded
		}
	}

	private enum ConfiguringProcess {
		NONE, CONFIGURING_STARTED, SET_ID_AUTO_CONFIGURING, CHECK_ID_AUTO_CONFIGURING,
		SET_TEMPERATURE_ID_AUTO_CONFIGURING, CHECK_TEMPERATURE_ID_AUTO_CONFIGURING, SET_VOLTAGE_RANGES,
		CONFIGURING_FINISHED, RESTART_AFTER_SETTING
	}

	private boolean isSystemStateUndefined() { // System is undefined if it is definitely not started and not stopped
		return (!isSystemIsRunning() && !isSystemStopped());
	}

	private boolean isSystemIsRunning() {
		EnumReadChannel contactorControlChannel = this.channel(VersionBChannelId.BMS_CONTACTOR_CONTROL);
		ContactorControl cc = contactorControlChannel.value().asEnum();
		return cc == ContactorControl.ON_GRID;
	}

	private boolean isSystemStopped() {
		EnumReadChannel contactorControlChannel = this.channel(VersionBChannelId.BMS_CONTACTOR_CONTROL);
		ContactorControl cc = contactorControlChannel.value().asEnum();
		return cc == ContactorControl.CUT_OFF;
	}

	private boolean isAlarmLevel2Error() {
		return (readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_SOC_LOW)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_TEMPERATURE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_POLES_TEMPERATURE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_INSULATION_LOW)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)
				|| readValueFromBooleanChannel(VersionBChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW));
	}

	private boolean isSlaveCommunicationError() {
		return readValueFromBooleanChannel(VersionBChannelId.SLAVE_20_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_19_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_18_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_17_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_16_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_15_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_14_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_13_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_12_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_11_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_10_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_9_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_8_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_7_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_6_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_5_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_4_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_3_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_2_COMMUNICATION_ERROR)
				|| readValueFromBooleanChannel(VersionBChannelId.SLAVE_1_COMMUNICATION_ERROR);
	}

	private boolean isError() {
		return isAlarmLevel2Error() || isSlaveCommunicationError();
	}

	private boolean readValueFromBooleanChannel(VersionBChannelId channelId) {
		StateChannel r = this.channel(channelId);
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
				+ "|Charge:" + this.getChargeMaxVoltage().value() + ";" + this.getChargeMaxCurrent().value() + "|State:"
				+ this.getStateMachineState();
	}

	private void startSystem() {
		EnumWriteChannel contactorControlChannel = this.channel(VersionBChannelId.BMS_CONTACTOR_CONTROL);
		ContactorControl cc = contactorControlChannel.value().asEnum();
		// To avoid hardware damages do not send start command if system has already
		// started
		if (cc == ContactorControl.ON_GRID || cc == ContactorControl.CONNECTION_INITIATING) {
			return;
		}

		try {
			log.debug("write value to contactor control channel: value: " + SYSTEM_ON);
			contactorControlChannel.setNextWriteValue(SYSTEM_ON);
		} catch (OpenemsNamedException e) {
			log.error("Error while trying to start system\n" + e.getMessage());
		}
	}

	private void stopSystem() {
		EnumWriteChannel contactorControlChannel = this.channel(VersionBChannelId.BMS_CONTACTOR_CONTROL);
		ContactorControl cc = contactorControlChannel.value().asEnum();
		// To avoid hardware damages do not send stop command if system has already
		// stopped
		if (cc == ContactorControl.CUT_OFF) {
			return;
		}

		try {
			log.debug("write value to contactor control channel: value: " + SYSTEM_OFF);
			contactorControlChannel.setNextWriteValue(SYSTEM_OFF);
		} catch (OpenemsNamedException e) {
			log.error("Error while trying to stop system\n" + e.getMessage());
		}
	}

	public State getStateMachineState() {
		return state;
	}

	public void setStateMachineState(State state) {
		this.state = state;
		this.channel(VersionBChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	private void setSoCLowAlarm(int soCLowAlarm) {
		try {
			((IntegerWriteChannel) this.channel(VersionBChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION))
					.setNextWriteValue(soCLowAlarm);
			((IntegerWriteChannel) this.channel(VersionBChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER))
					.setNextWriteValue(soCLowAlarm);
		} catch (OpenemsNamedException e) {
			log.error("Error while setting parameter for soc low protection!" + e.getMessage());
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		ModbusProtocol protocol = new ModbusProtocol(this, //
				// Main switch
				new FC6WriteRegisterTask(0x2010,
						m(VersionBChannelId.BMS_CONTACTOR_CONTROL, new UnsignedWordElement(0x2010)) //
				),

				// System reset
				new FC6WriteRegisterTask(0x2004, m(VersionBChannelId.SYSTEM_RESET, new UnsignedWordElement(0x2004)) //
				),

				// EMS timeout --> Watchdog
				new FC6WriteRegisterTask(0x201C,
						m(VersionBChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x201C)) //
				),
				// Sleep
				new FC6WriteRegisterTask(0x201D, m(VersionBChannelId.SLEEP, new UnsignedWordElement(0x201D)) //
				),

				// Work parameter
				new FC6WriteRegisterTask(0x20C1,
						m(VersionBChannelId.WORK_PARAMETER_PCS_COMMUNICATION_RATE, new UnsignedWordElement(0x20C1)) //
				),

				// Paramaeters for configuring
				new FC6WriteRegisterTask(0x2014,
						m(VersionBChannelId.AUTO_SET_SLAVES_ID, new UnsignedWordElement(0x2014))),
				new FC6WriteRegisterTask(0x2019,
						m(VersionBChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID, new UnsignedWordElement(0x2019))),

				// Control registers
				new FC3ReadRegistersTask(0x2000, Priority.HIGH, //
						m(VersionBChannelId.FAN_STATUS, new UnsignedWordElement(0x2000)), //
						m(VersionBChannelId.MAIN_CONTACTOR_STATE, new UnsignedWordElement(0x2001)), //
						m(VersionBChannelId.DRY_CONTACT_1_EXPORT, new UnsignedWordElement(0x2002)), //
						m(VersionBChannelId.DRY_CONTACT_2_EXPORT, new UnsignedWordElement(0x2003)), //
						m(VersionBChannelId.SYSTEM_RESET, new UnsignedWordElement(0x2004)), //
						m(VersionBChannelId.SYSTEM_RUN_MODE, new UnsignedWordElement(0x2005)), //
						m(VersionBChannelId.PRE_CONTACTOR_STATUS, new UnsignedWordElement(0x2006)), //
						bm(new UnsignedWordElement(0x2007)) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_DISCHARGE_TEMPERATURE_LOW, 15) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_DISCHARGE_TEMPERATURE_HIGH, 14) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_VOLTAGE_DIFFERENCE, 13) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_INSULATION_LOW, 12) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_CELL_VOLTAGE_DIFFERENCE, 11) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_ELECTRODE_TEMPERATURE_HIGH, 10) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_TEMPERATURE_DIFFERENCE, 9) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_SOC_LOW, 8) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_CELL_OVER_TEMPERATURE, 7) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_CELL_LOW_TEMPERATURE, 6) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_DISCHARGE_OVER_CURRENT, 5) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_SYSTEM_LOW_VOLTAGE, 4) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_CELL_LOW_VOLTAGE, 3) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_CHARGE_OVER_CURRENT, 2) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_SYSTEM_OVER_VOLTAGE, 1) //
								.m(VersionBChannelId.ALARM_FLAG_STATUS_CELL_OVER_VOLTAGE, 0) //
								.build(), //
						bm(new UnsignedWordElement(0x2008)) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_DISCHARGE_TEMPERATURE_LOW, 15) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_DISCHARGE_TEMPERATURE_HIGH, 14) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_VOLTAGE_DIFFERENCE, 13) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_INSULATION_LOW, 12) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_CELL_VOLTAGE_DIFFERENCE, 11) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_ELECTRODE_TEMPERATURE_HIGH, 10) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_TEMPERATURE_DIFFERENCE, 9) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_SOC_LOW, 8) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_CELL_OVER_TEMPERATURE, 7) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_CELL_LOW_TEMPERATURE, 6) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_DISCHARGE_OVER_CURRENT, 5) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_SYSTEM_LOW_VOLTAGE, 4) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_CELL_LOW_VOLTAGE, 3) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_CHARGE_OVER_CURRENT, 2) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_SYSTEM_OVER_VOLTAGE, 1) //
								.m(VersionBChannelId.PROTECT_FLAG_STATUS_CELL_OVER_VOLTAGE, 0) //
								.build(), //
						m(VersionBChannelId.ALARM_FLAG_REGISTER_1, new UnsignedWordElement(0x2009)), //
						m(VersionBChannelId.ALARM_FLAG_REGISTER_2, new UnsignedWordElement(0x200A)), //
						m(VersionBChannelId.PROTECT_FLAG_REGISTER_1, new UnsignedWordElement(0x200B)), //
						m(VersionBChannelId.PROTECT_FLAG_REGISTER_2, new UnsignedWordElement(0x200C)), //
						m(VersionBChannelId.SHORT_CIRCUIT_FUNCTION, new UnsignedWordElement(0x200D)), //
						m(VersionBChannelId.TESTING_IO, new UnsignedWordElement(0x200E)), //
						m(VersionBChannelId.SOFT_SHUTDOWN, new UnsignedWordElement(0x200F)), //
						m(VersionBChannelId.BMS_CONTACTOR_CONTROL, new UnsignedWordElement(0x2010)), //
						m(VersionBChannelId.CURRENT_BOX_SELF_CALIBRATION, new UnsignedWordElement(0x2011)), //
						m(VersionBChannelId.PCS_ALARM_RESET, new UnsignedWordElement(0x2012)), //
						m(VersionBChannelId.INSULATION_SENSOR_FUNCTION, new UnsignedWordElement(0x2013)), //
						m(VersionBChannelId.AUTO_SET_SLAVES_ID, new UnsignedWordElement(0x2014)), //
						new DummyRegisterElement(0x2015, 0x2018), //
						m(VersionBChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID, new UnsignedWordElement(0x2019)), //
						m(VersionBChannelId.TRANSPARENT_MASTER, new UnsignedWordElement(0x201A)), //
						m(VersionBChannelId.SET_EMS_ADDRESS, new UnsignedWordElement(0x201B)), //
						m(VersionBChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x201C)), //
						m(VersionBChannelId.SLEEP, new UnsignedWordElement(0x201D)), //
						m(VersionBChannelId.VOLTAGE_LOW_PROTECTION, new UnsignedWordElement(0x201E)) //
				), //

				// Voltage ranges
				new FC3ReadRegistersTask(0x2082, Priority.LOW, //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2082),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0x2083, 0x2087),
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM, new UnsignedWordElement(0x2088),
								ElementToChannelConverter.SCALE_FACTOR_2) //
				),

				// Summary state
				new FC3ReadRegistersTask(0x2100, Priority.LOW,
						m(VersionBChannelId.CLUSTER_1_VOLTAGE, new UnsignedWordElement(0x2100),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.CLUSTER_1_CURRENT, new UnsignedWordElement(0x2101),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.CLUSTER_1_CHARGE_INDICATION, new UnsignedWordElement(0x2102)),
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x2103)),
						m(VersionBChannelId.CLUSTER_1_SOH, new UnsignedWordElement(0x2104)),
						m(VersionBChannelId.CLUSTER_1_MAX_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2105)), //
						m(VersionBChannelId.CLUSTER_1_MAX_CELL_VOLTAGE, new UnsignedWordElement(0x2106)), //
						m(VersionBChannelId.CLUSTER_1_MIN_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2107)), //
						m(VersionBChannelId.CLUSTER_1_MIN_CELL_VOLTAGE, new UnsignedWordElement(0x2108)), //
						m(VersionBChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x2109)), //
						m(VersionBChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE, new UnsignedWordElement(0x210A)), //
						m(VersionBChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x210B)), //
						m(VersionBChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE, new UnsignedWordElement(0x210C)), //
						m(VersionBChannelId.MAX_CELL_RESISTANCE_ID, new UnsignedWordElement(0x210D)), //
						m(VersionBChannelId.MAX_CELL_RESISTANCE, new UnsignedWordElement(0x210E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(VersionBChannelId.MIN_CELL_RESISTANCE_ID, new UnsignedWordElement(0x210F)), //
						m(VersionBChannelId.MIN_CELL_RESISTANCE, new UnsignedWordElement(0x2110),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(VersionBChannelId.POSITIVE_INSULATION, new UnsignedWordElement(0x2111)), //
						m(VersionBChannelId.NEGATIVE_INSULATION, new UnsignedWordElement(0x2112)), //
						m(VersionBChannelId.MAIN_CONTACTOR_FLAG, new UnsignedWordElement(0x2113)), //
						new DummyRegisterElement(0x2114),
						m(VersionBChannelId.ENVIRONMENT_TEMPERATURE, new UnsignedWordElement(0x2115)), //
						m(VersionBChannelId.SYSTEM_INSULATION, new UnsignedWordElement(0x2116)), //
						m(VersionBChannelId.CELL_VOLTAGE_DIFFERENCE, new UnsignedWordElement(0x2117)), //
						m(VersionBChannelId.TOTAL_VOLTAGE_DIFFERENCE, new UnsignedWordElement(0x2118),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.POWER_TEMPERATURE, new UnsignedWordElement(0x2119)), //
						m(VersionBChannelId.POWER_SUPPLY_VOLTAGE, new UnsignedWordElement(0x211A)) //
				),

				// Critical state
				new FC3ReadRegistersTask(0x2140, Priority.HIGH, //
						bm(new UnsignedWordElement(0x2140)) //
								.m(VersionBChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH, 0) //
								.m(VersionBChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH, 1) //
								.m(VersionBChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH, 2) //
								.m(VersionBChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW, 3) //
								.m(VersionBChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW, 4) //
								.m(VersionBChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH, 5) //
								.m(VersionBChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH, 6) //
								.m(VersionBChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW, 7) //
								.m(VersionBChannelId.ALARM_LEVEL_2_SOC_LOW, 8) //
								.m(VersionBChannelId.ALARM_LEVEL_2_TEMPERATURE_DIFFERENCE_HIGH, 9) //
								.m(VersionBChannelId.ALARM_LEVEL_2_POLES_TEMPERATURE_DIFFERENCE_HIGH, 10) //
								.m(VersionBChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_DIFFERENCE_HIGH, 11) //
								.m(VersionBChannelId.ALARM_LEVEL_2_INSULATION_LOW, 12) //
								.m(VersionBChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_DIFFERENCE_HIGH, 13) //
								.m(VersionBChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH, 14) //
								.m(VersionBChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW, 15) //
								.build(), //
						bm(new UnsignedWordElement(0x2141)) //
								.m(VersionBChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_HIGH, 0) //
								.m(VersionBChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH, 1) //
								.m(VersionBChannelId.ALARM_LEVEL_1_CHA_CURRENT_HIGH, 2) //
								.m(VersionBChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_LOW, 3) //
								.m(VersionBChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW, 4) //
								.m(VersionBChannelId.ALARM_LEVEL_1_DISCHA_CURRENT_HIGH, 5) //
								.m(VersionBChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH, 6) //
								.m(VersionBChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_LOW, 7) //
								.m(VersionBChannelId.ALARM_LEVEL_1_SOC_LOW, 8) //
								.m(VersionBChannelId.ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH, 9) //
								.m(VersionBChannelId.ALARM_LEVEL_1_POLE_TEMPERATURE_TOO_HIGH, 10) //
								.m(VersionBChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH, 11) //
								.m(VersionBChannelId.ALARM_LEVEL_1_INSULATION_LOW, 12) //
								.m(VersionBChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH, 13) //
								.m(VersionBChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH, 14) //
								.m(VersionBChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW, 15) //
								.build(), //
						m(VersionBChannelId.CLUSTER_RUN_STATE, new UnsignedWordElement(0x2142)), //

						m(VersionBChannelId.MAXIMUM_CELL_VOLTAGE_NUMBER_WHEN_ALARM, new UnsignedWordElement(0x2143)), //
						m(VersionBChannelId.MAXIMUM_CELL_VOLTAGE_WHEN_ALARM, new UnsignedWordElement(0x2144)), //
						m(VersionBChannelId.MAXIMUM_CELL_VOLTAGE_NUMBER_WHEN_STOPPED, new UnsignedWordElement(0x2145)), //
						m(VersionBChannelId.MAXIMUM_CELL_VOLTAGE_WHEN_STOPPED, new UnsignedWordElement(0x2146)), //
						m(VersionBChannelId.MINIMUM_CELL_VOLTAGE_NUMBER_WHEN_ALARM, new UnsignedWordElement(0x2147)), //
						m(VersionBChannelId.MINIMUM_CELL_VOLTAGE_WHEN_ALARM, new UnsignedWordElement(0x2148)), //
						m(VersionBChannelId.MINIMUM_CELL_VOLTAGE_NUMBER_WHEN_STOPPED, new UnsignedWordElement(0x2149)), //
						m(VersionBChannelId.MINIMUM_CELL_VOLTAGE_WHEN_STOPPED, new UnsignedWordElement(0x214A)), //
						m(VersionBChannelId.OVER_VOLTAGE_VALUE_WHEN_ALARM, new UnsignedWordElement(0x214B)), //
						m(VersionBChannelId.OVER_VOLTAGE_VALUE_WHEN_STOPPED, new UnsignedWordElement(0x214C)), //
						m(VersionBChannelId.UNDER_VOLTAGE_VALUE_WHEN_ALARM, new UnsignedWordElement(0x214D)), //
						m(VersionBChannelId.UNDER_VOLTAGE_VALUE_WHEN_STOPPED, new UnsignedWordElement(0x214E)), //
						m(VersionBChannelId.OVER_CHARGE_CURRENT_WHEN_ALARM, new UnsignedWordElement(0x214F)), //
						m(VersionBChannelId.OVER_CHARGE_CURRENT_WHEN_STOPPED, new UnsignedWordElement(0x2150)), //
						m(VersionBChannelId.OVER_DISCHARGE_CURRENT_WHEN_ALARM, new UnsignedWordElement(0x2151)), //
						m(VersionBChannelId.OVER_DISCHARGE_CURRENT_WHEN_STOPPED, new UnsignedWordElement(0x2152)), //
						m(VersionBChannelId.NUMBER_OF_TEMPERATURE_WHEN_ALARM, new UnsignedWordElement(0x2153)), //
						new DummyRegisterElement(0x2154, 0x215A), //
						m(VersionBChannelId.OTHER_ALARM_EQUIPMENT_FAILURE, new UnsignedWordElement(0x215B)), //
						new DummyRegisterElement(0x215C, 0x215F), //
						m(VersionBChannelId.SYSTEM_MAX_CHARGE_CURRENT, new UnsignedWordElement(0x2160),
								ElementToChannelConverter.SCALE_FACTOR_2), // TODO Check if correct!
						m(VersionBChannelId.SYSTEM_MAX_DISCHARGE_CURRENT, new UnsignedWordElement(0x2161),
								ElementToChannelConverter.SCALE_FACTOR_2) // TODO Check if correct!
				), //

				// Cluster info
				new FC3ReadRegistersTask(0x2180, Priority.LOW, //
						m(VersionBChannelId.CYCLE_TIME, new UnsignedWordElement(0x2180)), //
						m(VersionBChannelId.TOTAL_CAPACITY_HIGH_BITS, new UnsignedWordElement(0x2181)), //
						m(VersionBChannelId.TOTAL_CAPACITY_LOW_BITS, new UnsignedWordElement(0x2182)), //
						bm(new UnsignedWordElement(0x2183)) //
								.m(VersionBChannelId.SLAVE_20_COMMUNICATION_ERROR, 3)//
								.m(VersionBChannelId.SLAVE_19_COMMUNICATION_ERROR, 2)//
								.m(VersionBChannelId.SLAVE_18_COMMUNICATION_ERROR, 1)//
								.m(VersionBChannelId.SLAVE_17_COMMUNICATION_ERROR, 0)//
								.build(), //
						bm(new UnsignedWordElement(0x2184)) //
								.m(VersionBChannelId.SLAVE_16_COMMUNICATION_ERROR, 15)//
								.m(VersionBChannelId.SLAVE_15_COMMUNICATION_ERROR, 14)//
								.m(VersionBChannelId.SLAVE_14_COMMUNICATION_ERROR, 13)//
								.m(VersionBChannelId.SLAVE_13_COMMUNICATION_ERROR, 12)//
								.m(VersionBChannelId.SLAVE_12_COMMUNICATION_ERROR, 11)//
								.m(VersionBChannelId.SLAVE_11_COMMUNICATION_ERROR, 10)//
								.m(VersionBChannelId.SLAVE_10_COMMUNICATION_ERROR, 9)//
								.m(VersionBChannelId.SLAVE_9_COMMUNICATION_ERROR, 8)//
								.m(VersionBChannelId.SLAVE_8_COMMUNICATION_ERROR, 7)//
								.m(VersionBChannelId.SLAVE_7_COMMUNICATION_ERROR, 6)//
								.m(VersionBChannelId.SLAVE_6_COMMUNICATION_ERROR, 5)//
								.m(VersionBChannelId.SLAVE_5_COMMUNICATION_ERROR, 4)//
								.m(VersionBChannelId.SLAVE_4_COMMUNICATION_ERROR, 3)//
								.m(VersionBChannelId.SLAVE_3_COMMUNICATION_ERROR, 2)//
								.m(VersionBChannelId.SLAVE_2_COMMUNICATION_ERROR, 1)//
								.m(VersionBChannelId.SLAVE_1_COMMUNICATION_ERROR, 0)//
								.build(), //
						bm(new UnsignedWordElement(0x2185)) //
								.m(VersionBChannelId.FAILURE_SAMPLING_WIRE, 0)//
								.m(VersionBChannelId.FAILURE_CONNECTOR_WIRE, 1)//
								.m(VersionBChannelId.FAILURE_LTC6803, 2)//
								.m(VersionBChannelId.FAILURE_VOLTAGE_SAMPLING, 3)//
								.m(VersionBChannelId.FAILURE_TEMP_SAMPLING, 4)//
								.m(VersionBChannelId.FAILURE_TEMP_SENSOR, 5)//
								.m(VersionBChannelId.FAILURE_GR_T, 6)//
								.m(VersionBChannelId.FAILURE_PCB, 7)//
								.m(VersionBChannelId.FAILURE_BALANCING_MODULE, 8)//
								.m(VersionBChannelId.FAILURE_TEMP_SAMPLING_LINE, 9)//
								.m(VersionBChannelId.FAILURE_INTRANET_COMMUNICATION, 10)//
								.m(VersionBChannelId.FAILURE_EEPROM, 11)//
								.m(VersionBChannelId.FAILURE_INITIALIZATION, 12)//
								.build(), //
						m(VersionBChannelId.SYSTEM_TIME_HIGH, new UnsignedWordElement(0x2186)), //
						m(VersionBChannelId.SYSTEM_TIME_LOW, new UnsignedWordElement(0x2187)), //
						new DummyRegisterElement(0x2188, 0x218E), //
						m(VersionBChannelId.LAST_TIME_CHARGE_CAPACITY_LOW_BITS, new UnsignedWordElement(0x218F),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(VersionBChannelId.LAST_TIME_CHARGE_END_TIME_HIGH_BITS, new UnsignedWordElement(0x2190)), //
						m(VersionBChannelId.LAST_TIME_CHARGE_END_TIME_LOW_BITS, new UnsignedWordElement(0x2191)), //
						new DummyRegisterElement(0x2192), //
						m(VersionBChannelId.LAST_TIME_DISCHARGE_CAPACITY_LOW_BITS, new UnsignedWordElement(0x2193),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(VersionBChannelId.LAST_TIME_DISCHARGE_END_TIME_HIGH_BITS, new UnsignedWordElement(0x2194)), //
						m(VersionBChannelId.LAST_TIME_DISCHARGE_END_TIME_LOW_BITS, new UnsignedWordElement(0x2195)), //
						m(VersionBChannelId.CELL_OVER_VOLTAGE_STOP_TIMES, new UnsignedWordElement(0x2196)), //
						m(VersionBChannelId.BATTERY_OVER_VOLTAGE_STOP_TIMES, new UnsignedWordElement(0x2197)), //
						m(VersionBChannelId.BATTERY_CHARGE_OVER_CURRENT_STOP_TIMES, new UnsignedWordElement(0x2198)), //
						m(VersionBChannelId.CELL_VOLTAGE_LOW_STOP_TIMES, new UnsignedWordElement(0x2199)), //
						m(VersionBChannelId.BATTERY_VOLTAGE_LOW_STOP_TIMES, new UnsignedWordElement(0x219A)), //
						m(VersionBChannelId.BATTERY_DISCHARGE_OVER_CURRENT_STOP_TIMES, new UnsignedWordElement(0x219B)), //
						m(VersionBChannelId.BATTERY_OVER_TEMPERATURE_STOP_TIMES, new UnsignedWordElement(0x219C)), //
						m(VersionBChannelId.BATTERY_TEMPERATURE_LOW_STOP_TIMES, new UnsignedWordElement(0x219D)), //
						m(VersionBChannelId.CELL_OVER_VOLTAGE_ALARM_TIMES, new UnsignedWordElement(0x219E)), //
						m(VersionBChannelId.BATTERY_OVER_VOLTAGE_ALARM_TIMES, new UnsignedWordElement(0x219F)), //
						m(VersionBChannelId.BATTERY_CHARGE_OVER_CURRENT_ALARM_TIMES, new UnsignedWordElement(0x21A0)), //
						m(VersionBChannelId.CELL_VOLTAGE_LOW_ALARM_TIMES, new UnsignedWordElement(0x21A1)), //
						m(VersionBChannelId.BATTERY_VOLTAGE_LOW_ALARM_TIMES, new UnsignedWordElement(0x21A2)), //
						m(VersionBChannelId.BATTERY_DISCHARGE_OVER_CURRENT_ALARM_TIMES,
								new UnsignedWordElement(0x21A3)), //
						m(VersionBChannelId.BATTERY_OVER_TEMPERATURE_ALARM_TIMES, new UnsignedWordElement(0x21A4)), //
						m(VersionBChannelId.BATTERY_TEMPERATURE_LOW_ALARM_TIMES, new UnsignedWordElement(0x21A5)), //
						m(VersionBChannelId.SYSTEM_SHORT_CIRCUIT_PROTECTION_TIMES, new UnsignedWordElement(0x21A6)), //
						m(VersionBChannelId.SYSTEM_GR_OVER_TEMPERATURE_STOP_TIMES, new UnsignedWordElement(0x21A7)), //
						new DummyRegisterElement(0x21A8), //
						m(VersionBChannelId.SYSTEM_GR_OVER_TEMPERATURE_ALARM_TIMES, new UnsignedWordElement(0x21A9)), //
						new DummyRegisterElement(0x21AA), //
						m(VersionBChannelId.BATTERY_VOLTAGE_DIFFERENCE_ALARM_TIMES, new UnsignedWordElement(0x21AB)), //
						m(VersionBChannelId.BATTERY_VOLTAGE_DIFFERENCE_STOP_TIMES, new UnsignedWordElement(0x21AC)), //
						new DummyRegisterElement(0x21AD, 0x21B3), //
						m(VersionBChannelId.SLAVE_TEMPERATURE_COMMUNICATION_ERROR_HIGH,
								new UnsignedWordElement(0x21B4)), //
						m(VersionBChannelId.SLAVE_TEMPERATURE_COMMUNICATION_ERROR_LOW, new UnsignedWordElement(0x21B5)) //
				) //
		); //

		if (!config.ReduceTasks()) {

			// Add tasks to read/write work and warn parameters
			// Stop parameter
			Task writeStopParameters = new FC16WriteRegistersTask(0x2040, //
					m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2040)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2041)), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2042),
							ElementToChannelConverter.SCALE_FACTOR_2), // TODO
																		// Check if
																		// correct!
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2043),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x2044), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2045), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2046)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2048),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2049),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x204A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x204B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_PROTECTION,
							new UnsignedWordElement(0x204C)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x204D)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_PROTECTION,
							new UnsignedWordElement(0x204E)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x204F)), //
					m(VersionBChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION, new UnsignedWordElement(0x2050)), //
					m(VersionBChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER, new UnsignedWordElement(0x2051)), //
					m(VersionBChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION, new UnsignedWordElement(0x2052)), //
					m(VersionBChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION_RECOVER, new UnsignedWordElement(0x2053)), //
					m(VersionBChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x2054)), //
					m(VersionBChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2055)), //
					m(VersionBChannelId.STOP_PARAMETER_INSULATION_PROTECTION, new UnsignedWordElement(0x2056)), //
					m(VersionBChannelId.STOP_PARAMETER_INSULATION_PROTECTION_RECOVER, new UnsignedWordElement(0x2057)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x2058)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2059)), //
					m(VersionBChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x205A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x205C)), //
					m(VersionBChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205D)), //
					m(VersionBChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION,
							new UnsignedWordElement(0x205E)), //
					m(VersionBChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205F)), //
					m(VersionBChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x2060)), //
					m(VersionBChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2061)) //
			);

//			//Warn parameter
			Task writeWarnParameters = new FC16WriteRegistersTask(0x2080, //
					m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2080)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2081)), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2082),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2083),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x2084), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2085), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM, new UnsignedWordElement(0x2086)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2087)), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM, new UnsignedWordElement(0x2088),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2089),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x208A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x208B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_ALARM, new UnsignedWordElement(0x208C)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x208D)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_ALARM, new UnsignedWordElement(0x208E)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x208F)), //
					m(VersionBChannelId.WARN_PARAMETER_SOC_LOW_ALARM, new UnsignedWordElement(0x2090)), //
					m(VersionBChannelId.WARN_PARAMETER_SOC_LOW_ALARM_RECOVER, new UnsignedWordElement(0x2091)), //
					m(VersionBChannelId.WARN_PARAMETER_SOC_HIGH_ALARM, new UnsignedWordElement(0x2092)), //
					m(VersionBChannelId.WARN_PARAMETER_SOC_HIGH_ALARM_RECOVER, new UnsignedWordElement(0x2093)), //
					m(VersionBChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM,
							new UnsignedWordElement(0x2094)), //
					m(VersionBChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER,
							new UnsignedWordElement(0x2095)), //
					m(VersionBChannelId.WARN_PARAMETER_INSULATION_ALARM, new UnsignedWordElement(0x2096)), //
					m(VersionBChannelId.WARN_PARAMETER_INSULATION_ALARM_RECOVER, new UnsignedWordElement(0x2097)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM, new UnsignedWordElement(0x2098)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x2099)), //
					m(VersionBChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM, new UnsignedWordElement(0x209A),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x209B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM,
							new UnsignedWordElement(0x209C)), //
					m(VersionBChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER,
							new UnsignedWordElement(0x209D)), //
					new DummyRegisterElement(0x209E),
					m(VersionBChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM,
							new UnsignedWordElement(0x209F)), //
					m(VersionBChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER,
							new UnsignedWordElement(0x20A0)), //
					m(VersionBChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM, new UnsignedWordElement(0x20A1)), //
					m(VersionBChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x20A2)) //
			);

			// Stop parameter
			Task readStopParameters = new FC3ReadRegistersTask(0x2040, Priority.LOW, //
					m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2040)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2041)), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2042),
							ElementToChannelConverter.SCALE_FACTOR_2), // TODO
																		// Check if
																		// correct!
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2043),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x2044), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2045), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2046)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2048),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2049),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x204A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x204B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_PROTECTION,
							new UnsignedWordElement(0x204C)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x204D)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_PROTECTION,
							new UnsignedWordElement(0x204E)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x204F)), //
					m(VersionBChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION, new UnsignedWordElement(0x2050)), //
					m(VersionBChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER, new UnsignedWordElement(0x2051)), //
					m(VersionBChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION, new UnsignedWordElement(0x2052)), //
					m(VersionBChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION_RECOVER, new UnsignedWordElement(0x2053)), //
					m(VersionBChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x2054)), //
					m(VersionBChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2055)), //
					m(VersionBChannelId.STOP_PARAMETER_INSULATION_PROTECTION, new UnsignedWordElement(0x2056)), //
					m(VersionBChannelId.STOP_PARAMETER_INSULATION_PROTECTION_RECOVER, new UnsignedWordElement(0x2057)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x2058)), //
					m(VersionBChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2059)), //
					m(VersionBChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x205A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x205C)), //
					m(VersionBChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205D)), //
					m(VersionBChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION,
							new UnsignedWordElement(0x205E)), //
					m(VersionBChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205F)), //
					m(VersionBChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x2060)), //
					m(VersionBChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2061)) //
			);

//			// Warn parameter
			Task readWarnParameters = new FC3ReadRegistersTask(0x2080, Priority.LOW, //
					m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2080)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2081)), //
					new DummyRegisterElement(0x2082),
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2083),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x2084), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2085), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM, new UnsignedWordElement(0x2086)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2087)), //
					new DummyRegisterElement(0x2088),
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2089),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x208A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x208B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_ALARM, new UnsignedWordElement(0x208C)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x208D)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_ALARM, new UnsignedWordElement(0x208E)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER, new UnsignedWordElement(0x208F)), //
					m(VersionBChannelId.WARN_PARAMETER_SOC_LOW_ALARM, new UnsignedWordElement(0x2090)), //
					m(VersionBChannelId.WARN_PARAMETER_SOC_LOW_ALARM_RECOVER, new UnsignedWordElement(0x2091)), //
					m(VersionBChannelId.WARN_PARAMETER_SOC_HIGH_ALARM, new UnsignedWordElement(0x2092)), //
					m(VersionBChannelId.WARN_PARAMETER_SOC_HIGH_ALARM_RECOVER, new UnsignedWordElement(0x2093)), //
					m(VersionBChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM,
							new UnsignedWordElement(0x2094)), //
					m(VersionBChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER,
							new UnsignedWordElement(0x2095)), //
					m(VersionBChannelId.WARN_PARAMETER_INSULATION_ALARM, new UnsignedWordElement(0x2096)), //
					m(VersionBChannelId.WARN_PARAMETER_INSULATION_ALARM_RECOVER, new UnsignedWordElement(0x2097)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM, new UnsignedWordElement(0x2098)), //
					m(VersionBChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x2099)), //
					m(VersionBChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM, new UnsignedWordElement(0x209A),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x209B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(VersionBChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM,
							new UnsignedWordElement(0x209C)), //
					m(VersionBChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER,
							new UnsignedWordElement(0x209D)), //
					new DummyRegisterElement(0x209E),
					m(VersionBChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM,
							new UnsignedWordElement(0x209F)), //
					m(VersionBChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER,
							new UnsignedWordElement(0x20A0)), //
					m(VersionBChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM, new UnsignedWordElement(0x20A1)), //
					m(VersionBChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x20A2)) //
			);

			protocol.addTask(readStopParameters);
			protocol.addTask(readWarnParameters);
			protocol.addTask(writeStopParameters);
			protocol.addTask(writeWarnParameters);

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
		}

		return protocol;
	}
}
