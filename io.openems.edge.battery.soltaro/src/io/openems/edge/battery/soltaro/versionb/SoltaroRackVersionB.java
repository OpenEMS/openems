package io.openems.edge.battery.soltaro.versionb;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.AutoSetFunction;
import io.openems.edge.battery.soltaro.versionb.VersionBEnums.ContactorControl;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
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
		implements Battery, OpenemsComponent, EventHandler { // JsonApi impl TODO

	// Default values for the battery ranges
	public static final int DISCHARGE_MIN_V = 696; // 34,8 x Number of modules
	public static final int CHARGE_MAX_V = 854; // 42,7 x Number of Modules
	public static final int DISCHARGE_MAX_A = 0; // For safety set it initially to 0
	public static final int CHARGE_MAX_A = 0;

	protected final static int SYSTEM_ON = 1;
	protected final static int SYSTEM_OFF = 0;

	public static final Integer CAPACITY_KWH = 50; // TODO depends on number of modules
	private static final Integer SYSTEM_RESET = 0x1;

	private final Logger log = LoggerFactory.getLogger(SoltaroRackVersionB.class);

	private String modbusBridgeId;
	private State state = State.UNDEFINED;
	// if configuring is needed this is used to go through the necessary steps
	private ConfiguringProcess nextConfiguringProcess = ConfiguringProcess.NONE;

	@Reference
	protected ConfigurationAdmin cm;

//	private LocalDateTime lastCommandSent = LocalDateTime.now(); // timer variable to avoid that commands are sent to
	// fast
//	private LocalDateTime timeForSystemInitialization = null;
//	private boolean isStopping = false; // indicates that system is stopping; during that time no commands should be sent //not necessary beacause if state in state machine
	private Config config;

	public SoltaroRackVersionB() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		this.modbusBridgeId = config.modbus_id();
		initializeCallbacks();
		
		setWatchdog(config.watchdog());
	}

	private void setWatchdog(int time_seconds) {
		try {
			IntegerWriteChannel c = this.channel(VersionBChannelId.EMS_COMMUNICATION_TIMEOUT);
			c.setNextWriteValue(time_seconds);
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	void debug(String t) {
		System.out.println(t);
	}

	private <T> void initializeCallbacks() {

		this.channel(VersionBChannelId.CLUSTER_1_VOLTAGE).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> vOpt = (Optional<Integer>) value.asOptional();
			if (!vOpt.isPresent()) {
				return;
			}
			int voltage_volt = (int) (vOpt.get() * 0.001);
			debug("callback voltage, value: " + voltage_volt);
			this.channel(Battery.ChannelId.VOLTAGE).setNextValue(voltage_volt);
		});

		this.channel(VersionBChannelId.CLUSTER_1_MIN_CELL_VOLTAGE).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> vOpt = (Optional<Integer>) value.asOptional();
			if (!vOpt.isPresent()) {
				return;
			}
			int voltage_millivolt = vOpt.get();
			debug("callback min cell voltage, value: " + voltage_millivolt);
			this.channel(Battery.ChannelId.MIN_CELL_VOLTAGE).setNextValue(voltage_millivolt);
		});

		// Battery ranges
		// ==> CHARGE_MAX_VOLTAGE 0x2042
		// (m(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION)
		// ==> DISCHARGE_MIN_VOLTAGE 0x2048
		// (VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION)
		// ==> CHARGE_MAX_CURRENT 0x2160 (VersionBChannelId.SYSTEM_MAX_CHARGE_CURRENT)
		// ==> DISCHARGE_MAX_CURRENT 0x2161
		// (VersionBChannelId.SYSTEM_MAX_DISCHARGE_CURRENT)
		this.channel(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> vOpt = (Optional<Integer>) value.asOptional();
			if (!vOpt.isPresent()) {
				return;
			}
			int max_charge_voltage = (int) (vOpt.get() * 0.001);
			debug("callback battery range, max charge voltage, value: " + max_charge_voltage);
			this.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE).setNextValue(max_charge_voltage);
		});

		this.channel(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> vOpt = (Optional<Integer>) value.asOptional();
			if (!vOpt.isPresent()) {
				return;
			}
			int min_discharge_voltage = (int) (vOpt.get() * 0.001);
			debug("callback battery range, min discharge voltage, value: " + min_discharge_voltage);
			this.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE).setNextValue(min_discharge_voltage);
		});

		this.channel(VersionBChannelId.SYSTEM_MAX_CHARGE_CURRENT).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> cOpt = (Optional<Integer>) value.asOptional();
			if (!cOpt.isPresent()) {
				return;
			}
			int max_current = (int) (cOpt.get() * 0.001);
			debug("callback battery range, max charge current, value: " + max_current);
			this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).setNextValue(max_current);
		});

		this.channel(VersionBChannelId.SYSTEM_MAX_DISCHARGE_CURRENT).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> cOpt = (Optional<Integer>) value.asOptional();
			if (!cOpt.isPresent()) {
				return;
			}
			int max_current = (int) (cOpt.get() * 0.001);
			debug("callback battery range, max discharge current, value: " + max_current);
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

	// If an error has occurred, this indicates the time when next action could be
	// done
	private LocalDateTime errorDelayIsOver = null;
	private int unsuccessfulStarts = 0;
	//
	private LocalDateTime startAttemptTime = null;

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
			debug("in case 'OFF'; try to start the system");
			this.startSystem();
			debug("set state to 'INIT'");
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
//			if (this.isConfiguringNeeded() == ConfiguringNecessary.NECESSARY) {
//				this.setStateMachineState(State.CONFIGURING);
//			} else 
			if (this.isError()) {
				this.setStateMachineState(State.ERROR);
			} else if (this.isSystemStopped()) {
				this.setStateMachineState(State.OFF);
			} else if (this.isSystemIsRunning()) {
				this.setStateMachineState(State.RUNNING);
			} 
//			else if (this.isSystemStateUndefined()) {
//				this.setStateMachineState(State.PENDING);
//			}
			break;
//		case PENDING:
//			if (isSystemStateUndefined()) {
//				break;
//			} else {
//				this.stopSystem();
//				this.setStateMachineState(State.OFF);
//				break;
//			}
		
//		case CONFIGURING:
//			configureSlaves();
//			break;
		}
		
		this.getReadyForWorking().setNextValue(readyForWorking);
	}

	private LocalDateTime timeAfterAutoId = null;
	private LocalDateTime configuringFinished = null;
	int DELAY_AUTO_ID_SECONDS = 5;
	int DELAY_AFTER_CONFIGURING_FINISHED = 5;
	
	private void configureSlaves() {
		if (nextConfiguringProcess == ConfiguringProcess.NONE) {
			nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
		}
		
		switch (nextConfiguringProcess) {
		case CONFIGURING_STARTED:
			log.info(" ===> CONFIGURING STARTED: setNumberOfModules() <===");
			setNumberOfModules();
			break;
//		case SET_SLAVE_NUMBER:
//			break;
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
//				this.setStateMachineState(State.OFF);				
			} else {
				if (configuringFinished.plusSeconds(DELAY_AFTER_CONFIGURING_FINISHED).isAfter(LocalDateTime.now())) {
					log.info(">>> Delay time after configuring!");
				} else {
					log.info("Delay time after configuring is over, reset system");
					//Reset System after configuration
					IntegerWriteChannel resetChannel = this.channel(VersionBChannelId.SYSTEM_RESET);
					try {
						resetChannel.setNextWriteValue(SYSTEM_RESET);
						configuringFinished = null;
					} catch (OpenemsException e) {
						log.error("Error while trying to reset the system!");
					}
				}
			}
			break;
		case RESTART_AFTER_SETTING:
			this.startSystem();
		case NONE:
			break;
		}
	}
	
	
	private void setVoltageRanges() {
		
		try {
			IntegerWriteChannel level1OverVoltageChannel = this.channel(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM);
			level1OverVoltageChannel.setNextWriteValue(this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_OVER_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level1OverVoltageChannelRecover = this.channel(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER);
			level1OverVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_OVER_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			IntegerWriteChannel level1LowVoltageChannel = this.channel(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM);
			level1LowVoltageChannel.setNextWriteValue(this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_LOW_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level1LowVoltageChannelRecover = this.channel(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER);
			level1LowVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_LOW_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			
			IntegerWriteChannel level2OverVoltageChannel = this.channel(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION);
			level2OverVoltageChannel.setNextWriteValue(this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_OVER_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level2OverVoltageChannelRecover = this.channel(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER);
			level2OverVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_OVER_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			IntegerWriteChannel level2LowVoltageChannel = this.channel(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION);
			level2LowVoltageChannel.setNextWriteValue(this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_LOW_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level2LowVoltageChannelRecover = this.channel(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER);
			level2LowVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_LOW_VOLTAGE_RECOVER_MILLIVOLT.getValue());
			
			nextConfiguringProcess = ConfiguringProcess.CONFIGURING_FINISHED;
			configuringFinished = LocalDateTime.now();
			
		} catch (OpenemsException e) {
			log.error("Setting voltage ranges not successful!");
		}
		
	}

	private void checkTemperatureIdAutoConfiguring() {
		IntegerReadChannel autoSetTemperatureSlavesIdChannel = this.channel(VersionBChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID);
		Optional<Integer> autoSetTemperatureSlavesIdOpt = autoSetTemperatureSlavesIdChannel.value().asOptional();
		if (!autoSetTemperatureSlavesIdOpt.isPresent()) {
			return;
		}
		int autoSetTemperatureSlaves = autoSetTemperatureSlavesIdOpt.get();
		if (autoSetTemperatureSlaves == VersionBEnums.AutoSetFunction.FAILURE.getValue()) {
			log.error("Auto set temperature slaves id failed! Start configuring process again!");
			//Auto set failed, try again
			nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
		} else if (autoSetTemperatureSlaves == VersionBEnums.AutoSetFunction.SUCCES.getValue()) {
			log.info("Auto set temperature slaves id succeeded!");
			nextConfiguringProcess = ConfiguringProcess.SET_VOLTAGE_RANGES;
		}
	}
	

	private void setTemperatureIdAutoConfiguring() {

		IntegerWriteChannel autoSetSlavesTemperatureIdChannel = this.channel(VersionBChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID);
		try {
			autoSetSlavesTemperatureIdChannel.setNextWriteValue(AutoSetFunction.START_AUTO_SETTING.getValue());
			timeAfterAutoId = LocalDateTime.now();
			nextConfiguringProcess = ConfiguringProcess.CHECK_TEMPERATURE_ID_AUTO_CONFIGURING;
		} catch (OpenemsException e) {
			log.error("Setting temperature id auto set not successful"); //Set was not successful, it will be tried until it succeeded 
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
			//Auto set failed, try again
			nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
		} else if (autoSetSlaves == VersionBEnums.AutoSetFunction.SUCCES.getValue()) {
			log.info("Auto set slaves id succeeded!");
			nextConfiguringProcess = ConfiguringProcess.SET_TEMPERATURE_ID_AUTO_CONFIGURING;
		}
	}

	private void setIdAutoConfiguring() {
		//Set number of modules
				IntegerWriteChannel autoSetSlavesIdChannel = this.channel(VersionBChannelId.AUTO_SET_SLAVES_ID);
				try {
					autoSetSlavesIdChannel.setNextWriteValue(AutoSetFunction.START_AUTO_SETTING.getValue());
					timeAfterAutoId = LocalDateTime.now();
					nextConfiguringProcess = ConfiguringProcess.CHECK_ID_AUTO_CONFIGURING;
				} catch (OpenemsException e) {
					log.error("Setting slave numbers not successful"); //Set was not successful, it will be tried until it succeeded 
				}
	}

	private void setNumberOfModules() {
		//Set number of modules
		IntegerWriteChannel numberOfSlavesChannel = this.channel(VersionBChannelId.WORK_PARAMETER_PCS_COMMUNICATION_RATE);
		try {
			numberOfSlavesChannel.setNextWriteValue(this.config.numberOfSlaves());
			nextConfiguringProcess = ConfiguringProcess.SET_ID_AUTO_CONFIGURING;
		} catch (OpenemsException e) {
			log.error("Setting slave numbers not successful"); //Set was not successful, it will be tried until it succeeded 
		}
	}
	
	private enum ConfiguringNecessary {
		UNDEFINED,
		NECESSARY,
		NOT_NECESSARY
	}
	
	private enum ConfiguringProcess {
		NONE,
		CONFIGURING_STARTED,
//		SET_SLAVE_NUMBER,
		SET_ID_AUTO_CONFIGURING,

		CHECK_ID_AUTO_CONFIGURING,
		SET_TEMPERATURE_ID_AUTO_CONFIGURING,
		CHECK_TEMPERATURE_ID_AUTO_CONFIGURING,
		SET_VOLTAGE_RANGES,
		CONFIGURING_FINISHED, RESTART_AFTER_SETTING
	}

	private ConfiguringNecessary isConfiguringNeeded() { //TODO Check if correct! ==> conf is needed when module numbers differ and/or slave comm errors are present
		@SuppressWarnings("unchecked")
		Optional<Integer> numberOfSlavesOpt = (Optional<Integer>) this.channel(VersionBChannelId.WORK_PARAMETER_PCS_COMMUNICATION_RATE).value().asOptional();
		if (! numberOfSlavesOpt.isPresent()) {
			return ConfiguringNecessary.UNDEFINED;
		}
		
		int numberOfModules = numberOfSlavesOpt.get();
		if (numberOfModules != this.config.numberOfSlaves()) {
			return ConfiguringNecessary.NECESSARY;
		}
		
//		@SuppressWarnings("unchecked")
//		Optional<Integer> systemVoltageVoltOpt = (Optional<Integer>) this.channel(VersionBChannelId.CLUSTER_1_VOLTAGE).value().asOptional();
//		if (!systemVoltageVoltOpt.isPresent()) {
//			return ConfiguringNecessary.UNDEFINED;
//		}
//		int systemVoltageVolt = systemVoltageVoltOpt.get();
//		int lowerRange = this.config.numberOfSlaves() * ModuleParameters.MIN_VOLTAGE_VOLT.getValue();
//		int upperRange = this.config.numberOfSlaves() * ModuleParameters.MAX_VOLTAGE_VOLT.getValue();
//		boolean voltageCorrect = lowerRange <= systemVoltageVolt && systemVoltageVolt <= upperRange; 
		
		if (! isSlaveCommunicationErrorValuesPresent() ) {
			return ConfiguringNecessary.UNDEFINED;
		}
		
//		if (!voltageCorrect || isSlaveCommunicationError()) {
//			return ConfiguringNecessary.NECESSARY;
//		};
		if (isSlaveCommunicationError()) {
			return ConfiguringNecessary.NECESSARY;
		};
		return ConfiguringNecessary.NOT_NECESSARY;
	}

	private boolean isSlaveCommunicationErrorValuesPresent() {
		VersionBChannelId[] channelIds = { 
				VersionBChannelId.SLAVE_20_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_19_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_18_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_17_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_16_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_15_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_14_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_13_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_12_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_11_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_10_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_9_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_8_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_7_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_6_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_5_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_4_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_3_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_2_COMMUNICATION_ERROR,
				VersionBChannelId.SLAVE_1_COMMUNICATION_ERROR
		};
		for (VersionBChannelId id : channelIds) {
			StateChannel r = this.channel(id);
			Optional<Boolean> bOpt = r.value().asOptional();
			if (!bOpt.isPresent()) {
				return false;
			}
		}
			
		return true;
	}

	private boolean isSystemStateUndefined() { // System is undefined if it is definitely not started and not stopped, and it is unknown if configuring is necessary
		return (isConfiguringNeeded() == ConfiguringNecessary.UNDEFINED) ||  (!isSystemIsRunning() && !isSystemStopped());
	}

	private boolean isSystemIsRunning() {
		IntegerReadChannel contactorControlChannel = this.channel(VersionBChannelId.BMS_CONTACTOR_CONTROL);		
		ContactorControl cc = contactorControlChannel.value().asEnum();
		return cc == ContactorControl.ON_GRID;
	}

	private boolean isSystemStopped() {
		IntegerReadChannel contactorControlChannel = this.channel(VersionBChannelId.BMS_CONTACTOR_CONTROL);		
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

	// Collects errors/warnings
	private Collection<ErrorCode> getErrorCodes() {
		Collection<ErrorCode> codes = new ArrayList<>();

		for (ErrorCode code : ErrorCode.values()) {
			if (code.getErrorChannelId() != null && readValueFromBooleanChannel(code.getErrorChannelId())) {
				codes.add(ErrorCode.getErrorCode(code.getErrorChannelId()));
			}
		}

		return codes;
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
				+ "|Charge:" + this.getChargeMaxVoltage().value() + ";" + this.getChargeMaxCurrent().value();
	}

	private void startSystem() {
		IntegerWriteChannel contactorControlChannel = this.channel(VersionBChannelId.BMS_CONTACTOR_CONTROL);

		Optional<Integer> contactorControlOpt = contactorControlChannel.value().asOptional();
		// To avoid hardware damages do not send start command if system has already
		// started
		if (contactorControlOpt.isPresent() && (
				contactorControlOpt.get() == ContactorControl.ON_GRID.getValue() ||
				contactorControlOpt.get() == ContactorControl.CONNECTION_INITIATING.getValue()
		)) {
			return;
		}

		try {
			debug("write value to contactor control channel: value: " + SYSTEM_ON);
			contactorControlChannel.setNextWriteValue(SYSTEM_ON);
		} catch (OpenemsException e) {
			log.error("Error while trying to start system\n" + e.getMessage());
		}
	}

	private void stopSystem() {
		IntegerWriteChannel contactorControlChannel = this.channel(VersionBChannelId.BMS_CONTACTOR_CONTROL);

		Optional<Integer> contactorControlOpt = contactorControlChannel.value().asOptional();
		// To avoid hardware damages do not send stop command if system has already
		// stopped
		if (contactorControlOpt.isPresent() && contactorControlOpt.get() == ContactorControl.CUT_OFF.getValue()) {
			return;
		}

		try {
			debug("write value to contactor control channel: value: " + SYSTEM_OFF);
			contactorControlChannel.setNextWriteValue(SYSTEM_OFF);
		} catch (OpenemsException e) {
			log.error("Error while trying to stop system\n" + e.getMessage());
		}
	}

	public State getStateMachineState() {
		return state;
	}

	public void setStateMachineState(State state) {
		
		if (state == State.ERROR) {
			for (ErrorCode c : getErrorCodes()) {
				log.error("Error detected: " + c.getName());
			}
		}
		
		this.state = state;
		this.channel(VersionBChannelId.STATE_MACHINE).setNextValue(this.state);
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
				
				//Paramaeters for configuring
				new FC6WriteRegisterTask(0x2014, 
					m(VersionBChannelId.AUTO_SET_SLAVES_ID, new UnsignedWordElement(0x2014))
				),
				new FC6WriteRegisterTask(0x2019, 
					m(VersionBChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID, new UnsignedWordElement(0x2019))
				),

				// Stop parameter
				new FC16WriteRegistersTask(0x2040, //
						m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2040)), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2041)), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2042), ElementToChannelConverter.SCALE_FACTOR_2), // TODO
																											// Check if
																											// correct!
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2043),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION,
								new UnsignedWordElement(0x2044), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x2045), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2046)), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2048), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2049), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
								new UnsignedWordElement(0x204A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x204B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_PROTECTION,
								new UnsignedWordElement(0x204C)), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
								new UnsignedWordElement(0x204D)), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_PROTECTION,
								new UnsignedWordElement(0x204E)), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
								new UnsignedWordElement(0x204F)), //
						m(VersionBChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION, new UnsignedWordElement(0x2050)), //
						m(VersionBChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER, new UnsignedWordElement(0x2051)), //
						m(VersionBChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION, new UnsignedWordElement(0x2052)), //
						m(VersionBChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION_RECOVER,
								new UnsignedWordElement(0x2053)), //
						m(VersionBChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
								new UnsignedWordElement(0x2054)), //
						m(VersionBChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
								new UnsignedWordElement(0x2055)), //
						m(VersionBChannelId.STOP_PARAMETER_INSULATION_PROTECTION, new UnsignedWordElement(0x2056)), //
						m(VersionBChannelId.STOP_PARAMETER_INSULATION_PROTECTION_RECOVER,
								new UnsignedWordElement(0x2057)), //
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
				),

//				//Warn parameter
				new FC16WriteRegistersTask(0x2080, //
						m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2080)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2081)), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2082), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, 
								new UnsignedWordElement(0x2083), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_ALARM,
								new UnsignedWordElement(0x2084), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x2085), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2086)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2087)), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2088), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2089), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM,
								new UnsignedWordElement(0x208A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x208B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_ALARM,
								new UnsignedWordElement(0x208C)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
								new UnsignedWordElement(0x208D)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_ALARM,
								new UnsignedWordElement(0x208E)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
								new UnsignedWordElement(0x208F)), //
						m(VersionBChannelId.WARN_PARAMETER_SOC_LOW_ALARM, new UnsignedWordElement(0x2090)), //
						m(VersionBChannelId.WARN_PARAMETER_SOC_LOW_ALARM_RECOVER, new UnsignedWordElement(0x2091)), //
						m(VersionBChannelId.WARN_PARAMETER_SOC_HIGH_ALARM, new UnsignedWordElement(0x2092)), //
						m(VersionBChannelId.WARN_PARAMETER_SOC_HIGH_ALARM_RECOVER,
								new UnsignedWordElement(0x2093)), //
						m(VersionBChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM,
								new UnsignedWordElement(0x2094)), //
						m(VersionBChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER,
								new UnsignedWordElement(0x2095)), //
						m(VersionBChannelId.WARN_PARAMETER_INSULATION_ALARM, new UnsignedWordElement(0x2096)), //
						m(VersionBChannelId.WARN_PARAMETER_INSULATION_ALARM_RECOVER,
								new UnsignedWordElement(0x2097)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM,
								new UnsignedWordElement(0x2098)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
								new UnsignedWordElement(0x2099)), //
						m(VersionBChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM,
								new UnsignedWordElement(0x209A), ElementToChannelConverter.SCALE_FACTOR_2), //
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
						m(VersionBChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM,
								new UnsignedWordElement(0x20A1)), //
						m(VersionBChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM_RECOVER,
								new UnsignedWordElement(0x20A2)) //
				),

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

				// Stop parameter
				new FC3ReadRegistersTask(0x2040, Priority.LOW, //
						m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2040)), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2041)), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2042), ElementToChannelConverter.SCALE_FACTOR_2), // TODO
																											// Check if
																											// correct!
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2043),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION,
								new UnsignedWordElement(0x2044), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x2045), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2046)), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION,
								new UnsignedWordElement(0x2048), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2049), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
								new UnsignedWordElement(0x204A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x204B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_PROTECTION,
								new UnsignedWordElement(0x204C)), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
								new UnsignedWordElement(0x204D)), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_PROTECTION,
								new UnsignedWordElement(0x204E)), //
						m(VersionBChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
								new UnsignedWordElement(0x204F)), //
						m(VersionBChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION, new UnsignedWordElement(0x2050)), //
						m(VersionBChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER, new UnsignedWordElement(0x2051)), //
						m(VersionBChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION, new UnsignedWordElement(0x2052)), //
						m(VersionBChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION_RECOVER,
								new UnsignedWordElement(0x2053)), //
						m(VersionBChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
								new UnsignedWordElement(0x2054)), //
						m(VersionBChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
								new UnsignedWordElement(0x2055)), //
						m(VersionBChannelId.STOP_PARAMETER_INSULATION_PROTECTION, new UnsignedWordElement(0x2056)), //
						m(VersionBChannelId.STOP_PARAMETER_INSULATION_PROTECTION_RECOVER,
								new UnsignedWordElement(0x2057)), //
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
				),

//				// Warn parameter
				new FC3ReadRegistersTask(0x2080, Priority.LOW, //
						m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2080)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2081)), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2082), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, 
								new UnsignedWordElement(0x2083), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_ALARM,
								new UnsignedWordElement(0x2084), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x2085), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2086)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2087)), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2088), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER,
								new UnsignedWordElement(0x2089), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM,
								new UnsignedWordElement(0x208A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
								new UnsignedWordElement(0x208B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_ALARM,
								new UnsignedWordElement(0x208C)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
								new UnsignedWordElement(0x208D)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_ALARM,
								new UnsignedWordElement(0x208E)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
								new UnsignedWordElement(0x208F)), //
						m(VersionBChannelId.WARN_PARAMETER_SOC_LOW_ALARM, new UnsignedWordElement(0x2090)), //
						m(VersionBChannelId.WARN_PARAMETER_SOC_LOW_ALARM_RECOVER, new UnsignedWordElement(0x2091)), //
						m(VersionBChannelId.WARN_PARAMETER_SOC_HIGH_ALARM, new UnsignedWordElement(0x2092)), //
						m(VersionBChannelId.WARN_PARAMETER_SOC_HIGH_ALARM_RECOVER,
								new UnsignedWordElement(0x2093)), //
						m(VersionBChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM,
								new UnsignedWordElement(0x2094)), //
						m(VersionBChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER,
								new UnsignedWordElement(0x2095)), //
						m(VersionBChannelId.WARN_PARAMETER_INSULATION_ALARM, new UnsignedWordElement(0x2096)), //
						m(VersionBChannelId.WARN_PARAMETER_INSULATION_ALARM_RECOVER,
								new UnsignedWordElement(0x2097)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM,
								new UnsignedWordElement(0x2098)), //
						m(VersionBChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
								new UnsignedWordElement(0x2099)), //
						m(VersionBChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM,
								new UnsignedWordElement(0x209A), ElementToChannelConverter.SCALE_FACTOR_2), //
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
						m(VersionBChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM,
								new UnsignedWordElement(0x20A1)), //
						m(VersionBChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM_RECOVER,
								new UnsignedWordElement(0x20A2)) //
				),

				new FC3ReadRegistersTask(0x20C0, Priority.LOW,
						m(VersionBChannelId.WORK_PARAMETER_PCS_MODBUS_ADDRESS, new UnsignedWordElement(0x20C0)), //
						m(VersionBChannelId.WORK_PARAMETER_PCS_COMMUNICATION_RATE, new UnsignedWordElement(0x20C1)), //
						m(VersionBChannelId.WORK_PARAMETER_CURRENT_FIX_COEFFICIENT, new UnsignedWordElement(0x20C2)), //
						m(VersionBChannelId.WORK_PARAMETER_CURRENT_FIX_OFFSET, new UnsignedWordElement(0x20C3)), //
						m(VersionBChannelId.WORK_PARAMETER_SET_CHARGER_OUTPUT_CURRENT, new UnsignedWordElement(0x20C4)), //
						m(VersionBChannelId.WORK_PARAMETER_SYSTEM_RTC_TIME_HIGH_BITS, new UnsignedWordElement(0x20C5)), //
						m(VersionBChannelId.WORK_PARAMETER_SYSTEM_RTC_TIME_LOW_BITS, new UnsignedWordElement(0x20C6)), //
						m(VersionBChannelId.WORK_PARAMETER_CELL_FLOAT_CHARGING, new UnsignedWordElement(0x20C7)), //
						m(VersionBChannelId.WORK_PARAMETER_CELL_AVERAGE_CHARGING, new UnsignedWordElement(0x20C8)), //
						m(VersionBChannelId.WORK_PARAMETER_CELL_STOP_DISCHARGING, new UnsignedWordElement(0x20C9)), //
						new DummyRegisterElement(0x20CA, 0x20CB),
						m(VersionBChannelId.WORK_PARAMETER_SYSTEM_CAPACITY, new UnsignedWordElement(0x20CC)), //
						new DummyRegisterElement(0x20CD, 0x20DE),
						m(VersionBChannelId.WORK_PARAMETER_SYSTEM_SOC, new UnsignedWordElement(0x20DF)), //
						m(VersionBChannelId.WORK_PARAMETER_SYSTEM_SOH_DEFAULT_VALUE, new UnsignedWordElement(0x20E0)) //
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

		// Add tasks for cell voltages and temperatures according to the number of
		// slaves
		if (config.numberOfSlaves() > 0) {
			if (config.numberOfSlaves() < 2) {
				protocol.addTask(new FC3ReadRegistersTask(0x2800, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_000_VOLTAGE, new UnsignedWordElement(0x2800)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_001_VOLTAGE, new UnsignedWordElement(0x2801)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_002_VOLTAGE, new UnsignedWordElement(0x2802)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_003_VOLTAGE, new UnsignedWordElement(0x2803)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_004_VOLTAGE, new UnsignedWordElement(0x2804)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_005_VOLTAGE, new UnsignedWordElement(0x2805)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_006_VOLTAGE, new UnsignedWordElement(0x2806)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_007_VOLTAGE, new UnsignedWordElement(0x2807)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_008_VOLTAGE, new UnsignedWordElement(0x2808)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_009_VOLTAGE, new UnsignedWordElement(0x2809)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C00, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_000_TEMPERATURE, new UnsignedWordElement(0x2C00)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_001_TEMPERATURE, new UnsignedWordElement(0x2C01)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_002_TEMPERATURE, new UnsignedWordElement(0x2C02)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_003_TEMPERATURE, new UnsignedWordElement(0x2C03)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_004_TEMPERATURE, new UnsignedWordElement(0x2C04)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_005_TEMPERATURE, new UnsignedWordElement(0x2C05)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_006_TEMPERATURE, new UnsignedWordElement(0x2C06)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_007_TEMPERATURE, new UnsignedWordElement(0x2C07)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_008_TEMPERATURE, new UnsignedWordElement(0x2C08)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_009_TEMPERATURE, new UnsignedWordElement(0x2C09)) //
				));
			}

			if (config.numberOfSlaves() < 3) {
				protocol.addTask(new FC3ReadRegistersTask(0x280A, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_010_VOLTAGE, new UnsignedWordElement(0x280A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_011_VOLTAGE, new UnsignedWordElement(0x280B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_012_VOLTAGE, new UnsignedWordElement(0x280C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_013_VOLTAGE, new UnsignedWordElement(0x280D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_014_VOLTAGE, new UnsignedWordElement(0x280E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_015_VOLTAGE, new UnsignedWordElement(0x280F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_016_VOLTAGE, new UnsignedWordElement(0x2810)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_017_VOLTAGE, new UnsignedWordElement(0x2811)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_018_VOLTAGE, new UnsignedWordElement(0x2812)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_019_VOLTAGE, new UnsignedWordElement(0x2813)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C0A, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_010_TEMPERATURE, new UnsignedWordElement(0x2C0A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_011_TEMPERATURE, new UnsignedWordElement(0x2C0B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_012_TEMPERATURE, new UnsignedWordElement(0x2C0C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_013_TEMPERATURE, new UnsignedWordElement(0x2C0D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_014_TEMPERATURE, new UnsignedWordElement(0x2C0E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_015_TEMPERATURE, new UnsignedWordElement(0x2C0F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_016_TEMPERATURE, new UnsignedWordElement(0x2C10)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_017_TEMPERATURE, new UnsignedWordElement(0x2C11)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_018_TEMPERATURE, new UnsignedWordElement(0x2C12)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_019_TEMPERATURE, new UnsignedWordElement(0x2C13)) //
				));
			}

			if (config.numberOfSlaves() < 4) {
				protocol.addTask(new FC3ReadRegistersTask(0x2814, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_020_VOLTAGE, new UnsignedWordElement(0x2814)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_021_VOLTAGE, new UnsignedWordElement(0x2815)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_022_VOLTAGE, new UnsignedWordElement(0x2816)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_023_VOLTAGE, new UnsignedWordElement(0x2817)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_024_VOLTAGE, new UnsignedWordElement(0x2818)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_025_VOLTAGE, new UnsignedWordElement(0x2819)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_026_VOLTAGE, new UnsignedWordElement(0x281A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_027_VOLTAGE, new UnsignedWordElement(0x281B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_028_VOLTAGE, new UnsignedWordElement(0x281C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_029_VOLTAGE, new UnsignedWordElement(0x281D)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C14, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_020_TEMPERATURE, new UnsignedWordElement(0x2C14)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_021_TEMPERATURE, new UnsignedWordElement(0x2C15)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_022_TEMPERATURE, new UnsignedWordElement(0x2C16)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_023_TEMPERATURE, new UnsignedWordElement(0x2C17)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_024_TEMPERATURE, new UnsignedWordElement(0x2C18)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_025_TEMPERATURE, new UnsignedWordElement(0x2C19)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_026_TEMPERATURE, new UnsignedWordElement(0x2C1A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_027_TEMPERATURE, new UnsignedWordElement(0x2C1B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_028_TEMPERATURE, new UnsignedWordElement(0x2C1C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_029_TEMPERATURE, new UnsignedWordElement(0x2C1D)) //
				));
			}

			if (config.numberOfSlaves() < 5) {
				protocol.addTask(new FC3ReadRegistersTask(0x281E, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_030_VOLTAGE, new UnsignedWordElement(0x281E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_031_VOLTAGE, new UnsignedWordElement(0x281F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_032_VOLTAGE, new UnsignedWordElement(0x2820)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_033_VOLTAGE, new UnsignedWordElement(0x2821)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_034_VOLTAGE, new UnsignedWordElement(0x2822)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_035_VOLTAGE, new UnsignedWordElement(0x2823)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_036_VOLTAGE, new UnsignedWordElement(0x2824)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_037_VOLTAGE, new UnsignedWordElement(0x2825)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_038_VOLTAGE, new UnsignedWordElement(0x2826)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_039_VOLTAGE, new UnsignedWordElement(0x2827)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C1E, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_030_TEMPERATURE, new UnsignedWordElement(0x2C1E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_031_TEMPERATURE, new UnsignedWordElement(0x2C1F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_032_TEMPERATURE, new UnsignedWordElement(0x2C20)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_033_TEMPERATURE, new UnsignedWordElement(0x2C21)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_034_TEMPERATURE, new UnsignedWordElement(0x2C22)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_035_TEMPERATURE, new UnsignedWordElement(0x2C23)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_036_TEMPERATURE, new UnsignedWordElement(0x2C24)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_037_TEMPERATURE, new UnsignedWordElement(0x2C25)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_038_TEMPERATURE, new UnsignedWordElement(0x2C26)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_039_TEMPERATURE, new UnsignedWordElement(0x2C27)) //
				));
			}

			if (config.numberOfSlaves() < 6) {
				protocol.addTask(new FC3ReadRegistersTask(0x2828, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_040_VOLTAGE, new UnsignedWordElement(0x2828)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_041_VOLTAGE, new UnsignedWordElement(0x2829)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_042_VOLTAGE, new UnsignedWordElement(0x282A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_043_VOLTAGE, new UnsignedWordElement(0x282B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_044_VOLTAGE, new UnsignedWordElement(0x282C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_045_VOLTAGE, new UnsignedWordElement(0x282D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_046_VOLTAGE, new UnsignedWordElement(0x282E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_047_VOLTAGE, new UnsignedWordElement(0x282F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_048_VOLTAGE, new UnsignedWordElement(0x2830)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_049_VOLTAGE, new UnsignedWordElement(0x2831)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C28, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_040_TEMPERATURE, new UnsignedWordElement(0x2C28)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_041_TEMPERATURE, new UnsignedWordElement(0x2C29)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_042_TEMPERATURE, new UnsignedWordElement(0x2C2A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_043_TEMPERATURE, new UnsignedWordElement(0x2C2B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_044_TEMPERATURE, new UnsignedWordElement(0x2C2C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_045_TEMPERATURE, new UnsignedWordElement(0x2C2D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_046_TEMPERATURE, new UnsignedWordElement(0x2C2E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_047_TEMPERATURE, new UnsignedWordElement(0x2C2F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_048_TEMPERATURE, new UnsignedWordElement(0x2C30)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_049_TEMPERATURE, new UnsignedWordElement(0x2C31)) //
				));
			}

			if (config.numberOfSlaves() < 7) {
				protocol.addTask(new FC3ReadRegistersTask(0x2832, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_050_VOLTAGE, new UnsignedWordElement(0x2832)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_051_VOLTAGE, new UnsignedWordElement(0x2833)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_052_VOLTAGE, new UnsignedWordElement(0x2834)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_053_VOLTAGE, new UnsignedWordElement(0x2835)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_054_VOLTAGE, new UnsignedWordElement(0x2836)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_055_VOLTAGE, new UnsignedWordElement(0x2837)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_056_VOLTAGE, new UnsignedWordElement(0x2838)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_057_VOLTAGE, new UnsignedWordElement(0x2839)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_058_VOLTAGE, new UnsignedWordElement(0x283A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_059_VOLTAGE, new UnsignedWordElement(0x283B)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C32, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_050_TEMPERATURE, new UnsignedWordElement(0x2C32)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_051_TEMPERATURE, new UnsignedWordElement(0x2C33)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_052_TEMPERATURE, new UnsignedWordElement(0x2C34)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_053_TEMPERATURE, new UnsignedWordElement(0x2C35)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_054_TEMPERATURE, new UnsignedWordElement(0x2C36)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_055_TEMPERATURE, new UnsignedWordElement(0x2C37)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_056_TEMPERATURE, new UnsignedWordElement(0x2C38)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_057_TEMPERATURE, new UnsignedWordElement(0x2C39)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_058_TEMPERATURE, new UnsignedWordElement(0x2C3A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_059_TEMPERATURE, new UnsignedWordElement(0x2C3B)) //
				));
			}

			if (config.numberOfSlaves() < 8) {
				protocol.addTask(new FC3ReadRegistersTask(0x283C, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_060_VOLTAGE, new UnsignedWordElement(0x283C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_061_VOLTAGE, new UnsignedWordElement(0x283D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_062_VOLTAGE, new UnsignedWordElement(0x283E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_063_VOLTAGE, new UnsignedWordElement(0x283F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_064_VOLTAGE, new UnsignedWordElement(0x2840)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_065_VOLTAGE, new UnsignedWordElement(0x2841)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_066_VOLTAGE, new UnsignedWordElement(0x2842)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_067_VOLTAGE, new UnsignedWordElement(0x2843)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_068_VOLTAGE, new UnsignedWordElement(0x2844)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_069_VOLTAGE, new UnsignedWordElement(0x2845)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C3C, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_060_TEMPERATURE, new UnsignedWordElement(0x2C3C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_061_TEMPERATURE, new UnsignedWordElement(0x2C3D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_062_TEMPERATURE, new UnsignedWordElement(0x2C3E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_063_TEMPERATURE, new UnsignedWordElement(0x2C3F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_064_TEMPERATURE, new UnsignedWordElement(0x2C40)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_065_TEMPERATURE, new UnsignedWordElement(0x2C41)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_066_TEMPERATURE, new UnsignedWordElement(0x2C42)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_067_TEMPERATURE, new UnsignedWordElement(0x2C43)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_068_TEMPERATURE, new UnsignedWordElement(0x2C44)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_069_TEMPERATURE, new UnsignedWordElement(0x2C45)) //
				));
			}

			if (config.numberOfSlaves() < 9) {
				protocol.addTask(new FC3ReadRegistersTask(0x2846, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_070_VOLTAGE, new UnsignedWordElement(0x2846)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_071_VOLTAGE, new UnsignedWordElement(0x2847)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_072_VOLTAGE, new UnsignedWordElement(0x2848)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_073_VOLTAGE, new UnsignedWordElement(0x2849)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_074_VOLTAGE, new UnsignedWordElement(0x284A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_075_VOLTAGE, new UnsignedWordElement(0x284B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_076_VOLTAGE, new UnsignedWordElement(0x284C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_077_VOLTAGE, new UnsignedWordElement(0x284D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_078_VOLTAGE, new UnsignedWordElement(0x284E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_079_VOLTAGE, new UnsignedWordElement(0x284F)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C46, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_070_TEMPERATURE, new UnsignedWordElement(0x2C46)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_071_TEMPERATURE, new UnsignedWordElement(0x2C47)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_072_TEMPERATURE, new UnsignedWordElement(0x2C48)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_073_TEMPERATURE, new UnsignedWordElement(0x2C49)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_074_TEMPERATURE, new UnsignedWordElement(0x2C4A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_075_TEMPERATURE, new UnsignedWordElement(0x2C4B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_076_TEMPERATURE, new UnsignedWordElement(0x2C4C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_077_TEMPERATURE, new UnsignedWordElement(0x2C4D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_078_TEMPERATURE, new UnsignedWordElement(0x2C4E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_079_TEMPERATURE, new UnsignedWordElement(0x2C4F)) //
				));
			}

			if (config.numberOfSlaves() < 10) {
				protocol.addTask(new FC3ReadRegistersTask(0x2850, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_080_VOLTAGE, new UnsignedWordElement(0x2850)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_081_VOLTAGE, new UnsignedWordElement(0x2851)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_082_VOLTAGE, new UnsignedWordElement(0x2852)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_083_VOLTAGE, new UnsignedWordElement(0x2853)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_084_VOLTAGE, new UnsignedWordElement(0x2854)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_085_VOLTAGE, new UnsignedWordElement(0x2855)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_086_VOLTAGE, new UnsignedWordElement(0x2856)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_087_VOLTAGE, new UnsignedWordElement(0x2857)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_088_VOLTAGE, new UnsignedWordElement(0x2858)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_089_VOLTAGE, new UnsignedWordElement(0x2859)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C50, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_080_TEMPERATURE, new UnsignedWordElement(0x2C50)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_081_TEMPERATURE, new UnsignedWordElement(0x2C51)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_082_TEMPERATURE, new UnsignedWordElement(0x2C52)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_083_TEMPERATURE, new UnsignedWordElement(0x2C53)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_084_TEMPERATURE, new UnsignedWordElement(0x2C54)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_085_TEMPERATURE, new UnsignedWordElement(0x2C55)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_086_TEMPERATURE, new UnsignedWordElement(0x2C56)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_087_TEMPERATURE, new UnsignedWordElement(0x2C57)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_088_TEMPERATURE, new UnsignedWordElement(0x2C58)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_089_TEMPERATURE, new UnsignedWordElement(0x2C59)) //
				));
			}

			if (config.numberOfSlaves() < 11) {
				protocol.addTask(new FC3ReadRegistersTask(0x285A, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_090_VOLTAGE, new UnsignedWordElement(0x285A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_091_VOLTAGE, new UnsignedWordElement(0x285B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_092_VOLTAGE, new UnsignedWordElement(0x285C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_093_VOLTAGE, new UnsignedWordElement(0x285D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_094_VOLTAGE, new UnsignedWordElement(0x285E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_095_VOLTAGE, new UnsignedWordElement(0x285F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_096_VOLTAGE, new UnsignedWordElement(0x2860)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_097_VOLTAGE, new UnsignedWordElement(0x2861)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_098_VOLTAGE, new UnsignedWordElement(0x2862)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_099_VOLTAGE, new UnsignedWordElement(0x2863)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C5A, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_090_TEMPERATURE, new UnsignedWordElement(0x2C5A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_091_TEMPERATURE, new UnsignedWordElement(0x2C5B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_092_TEMPERATURE, new UnsignedWordElement(0x2C5C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_093_TEMPERATURE, new UnsignedWordElement(0x2C5D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_094_TEMPERATURE, new UnsignedWordElement(0x2C5E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_095_TEMPERATURE, new UnsignedWordElement(0x2C5F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_096_TEMPERATURE, new UnsignedWordElement(0x2C60)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_097_TEMPERATURE, new UnsignedWordElement(0x2C61)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_098_TEMPERATURE, new UnsignedWordElement(0x2C62)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_099_TEMPERATURE, new UnsignedWordElement(0x2C63)) //
				));
			}

			if (config.numberOfSlaves() < 12) {
				protocol.addTask(new FC3ReadRegistersTask(0x2864, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_100_VOLTAGE, new UnsignedWordElement(0x2864)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_101_VOLTAGE, new UnsignedWordElement(0x2865)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_102_VOLTAGE, new UnsignedWordElement(0x2866)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_103_VOLTAGE, new UnsignedWordElement(0x2867)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_104_VOLTAGE, new UnsignedWordElement(0x2868)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_105_VOLTAGE, new UnsignedWordElement(0x2869)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_106_VOLTAGE, new UnsignedWordElement(0x286A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_107_VOLTAGE, new UnsignedWordElement(0x286B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_108_VOLTAGE, new UnsignedWordElement(0x286C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_109_VOLTAGE, new UnsignedWordElement(0x286D)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C64, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_100_TEMPERATURE, new UnsignedWordElement(0x2C64)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_101_TEMPERATURE, new UnsignedWordElement(0x2C65)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_102_TEMPERATURE, new UnsignedWordElement(0x2C66)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_103_TEMPERATURE, new UnsignedWordElement(0x2C67)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_104_TEMPERATURE, new UnsignedWordElement(0x2C68)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_105_TEMPERATURE, new UnsignedWordElement(0x2C69)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_106_TEMPERATURE, new UnsignedWordElement(0x2C6A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_107_TEMPERATURE, new UnsignedWordElement(0x2C6B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_108_TEMPERATURE, new UnsignedWordElement(0x2C6C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_109_TEMPERATURE, new UnsignedWordElement(0x2C6D)) //
				));
			}

			if (config.numberOfSlaves() < 13) {
				protocol.addTask(new FC3ReadRegistersTask(0x286E, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_110_VOLTAGE, new UnsignedWordElement(0x286E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_111_VOLTAGE, new UnsignedWordElement(0x286F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_112_VOLTAGE, new UnsignedWordElement(0x2870)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_113_VOLTAGE, new UnsignedWordElement(0x2871)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_114_VOLTAGE, new UnsignedWordElement(0x2872)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_115_VOLTAGE, new UnsignedWordElement(0x2873)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_116_VOLTAGE, new UnsignedWordElement(0x2874)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_117_VOLTAGE, new UnsignedWordElement(0x2875)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_118_VOLTAGE, new UnsignedWordElement(0x2876)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_119_VOLTAGE, new UnsignedWordElement(0x2877)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C6E, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_110_TEMPERATURE, new UnsignedWordElement(0x2C6E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_111_TEMPERATURE, new UnsignedWordElement(0x2C6F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_112_TEMPERATURE, new UnsignedWordElement(0x2C70)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_113_TEMPERATURE, new UnsignedWordElement(0x2C71)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_114_TEMPERATURE, new UnsignedWordElement(0x2C72)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_115_TEMPERATURE, new UnsignedWordElement(0x2C73)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_116_TEMPERATURE, new UnsignedWordElement(0x2C74)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_117_TEMPERATURE, new UnsignedWordElement(0x2C75)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_118_TEMPERATURE, new UnsignedWordElement(0x2C76)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_119_TEMPERATURE, new UnsignedWordElement(0x2C77)) //
				));
			}

			if (config.numberOfSlaves() < 14) {
				protocol.addTask(new FC3ReadRegistersTask(0x2878, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_120_VOLTAGE, new UnsignedWordElement(0x2878)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_121_VOLTAGE, new UnsignedWordElement(0x2879)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_122_VOLTAGE, new UnsignedWordElement(0x287A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_123_VOLTAGE, new UnsignedWordElement(0x287B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_124_VOLTAGE, new UnsignedWordElement(0x287C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_125_VOLTAGE, new UnsignedWordElement(0x287D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_126_VOLTAGE, new UnsignedWordElement(0x287E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_127_VOLTAGE, new UnsignedWordElement(0x287F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_128_VOLTAGE, new UnsignedWordElement(0x2880)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_129_VOLTAGE, new UnsignedWordElement(0x2881)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C78, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_120_TEMPERATURE, new UnsignedWordElement(0x2C78)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_121_TEMPERATURE, new UnsignedWordElement(0x2C79)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_122_TEMPERATURE, new UnsignedWordElement(0x2C7A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_123_TEMPERATURE, new UnsignedWordElement(0x2C7B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_124_TEMPERATURE, new UnsignedWordElement(0x2C7C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_125_TEMPERATURE, new UnsignedWordElement(0x2C7D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_126_TEMPERATURE, new UnsignedWordElement(0x2C7E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_127_TEMPERATURE, new UnsignedWordElement(0x2C7F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_128_TEMPERATURE, new UnsignedWordElement(0x2C80)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_129_TEMPERATURE, new UnsignedWordElement(0x2C81)) //
				));
			}

			if (config.numberOfSlaves() < 15) {
				protocol.addTask(new FC3ReadRegistersTask(0x2882, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_130_VOLTAGE, new UnsignedWordElement(0x2882)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_131_VOLTAGE, new UnsignedWordElement(0x2883)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_132_VOLTAGE, new UnsignedWordElement(0x2884)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_133_VOLTAGE, new UnsignedWordElement(0x2885)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_134_VOLTAGE, new UnsignedWordElement(0x2886)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_135_VOLTAGE, new UnsignedWordElement(0x2887)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_136_VOLTAGE, new UnsignedWordElement(0x2888)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_137_VOLTAGE, new UnsignedWordElement(0x2889)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_138_VOLTAGE, new UnsignedWordElement(0x288A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_139_VOLTAGE, new UnsignedWordElement(0x288B)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C82, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_130_TEMPERATURE, new UnsignedWordElement(0x2C82)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_131_TEMPERATURE, new UnsignedWordElement(0x2C83)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_132_TEMPERATURE, new UnsignedWordElement(0x2C84)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_133_TEMPERATURE, new UnsignedWordElement(0x2C85)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_134_TEMPERATURE, new UnsignedWordElement(0x2C86)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_135_TEMPERATURE, new UnsignedWordElement(0x2C87)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_136_TEMPERATURE, new UnsignedWordElement(0x2C88)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_137_TEMPERATURE, new UnsignedWordElement(0x2C89)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_138_TEMPERATURE, new UnsignedWordElement(0x2C8A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_139_TEMPERATURE, new UnsignedWordElement(0x2C8B)) //
				));
			}

			if (config.numberOfSlaves() < 16) {
				protocol.addTask(new FC3ReadRegistersTask(0x288C, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_140_VOLTAGE, new UnsignedWordElement(0x288C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_141_VOLTAGE, new UnsignedWordElement(0x288D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_142_VOLTAGE, new UnsignedWordElement(0x288E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_143_VOLTAGE, new UnsignedWordElement(0x288F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_144_VOLTAGE, new UnsignedWordElement(0x2890)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_145_VOLTAGE, new UnsignedWordElement(0x2891)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_146_VOLTAGE, new UnsignedWordElement(0x2892)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_147_VOLTAGE, new UnsignedWordElement(0x2893)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_148_VOLTAGE, new UnsignedWordElement(0x2894)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_149_VOLTAGE, new UnsignedWordElement(0x2895)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C8C, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_140_TEMPERATURE, new UnsignedWordElement(0x2C8C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_141_TEMPERATURE, new UnsignedWordElement(0x2C8D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_142_TEMPERATURE, new UnsignedWordElement(0x2C8E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_143_TEMPERATURE, new UnsignedWordElement(0x2C8F)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_144_TEMPERATURE, new UnsignedWordElement(0x2C90)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_145_TEMPERATURE, new UnsignedWordElement(0x2C91)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_146_TEMPERATURE, new UnsignedWordElement(0x2C92)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_147_TEMPERATURE, new UnsignedWordElement(0x2C93)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_148_TEMPERATURE, new UnsignedWordElement(0x2C94)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_149_TEMPERATURE, new UnsignedWordElement(0x2C95)) //
				));
			}

			if (config.numberOfSlaves() < 17) {
				protocol.addTask(new FC3ReadRegistersTask(0x2896, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_150_VOLTAGE, new UnsignedWordElement(0x2896)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_151_VOLTAGE, new UnsignedWordElement(0x2897)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_152_VOLTAGE, new UnsignedWordElement(0x2898)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_153_VOLTAGE, new UnsignedWordElement(0x2899)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_154_VOLTAGE, new UnsignedWordElement(0x289A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_155_VOLTAGE, new UnsignedWordElement(0x289B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_156_VOLTAGE, new UnsignedWordElement(0x289C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_157_VOLTAGE, new UnsignedWordElement(0x289D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_158_VOLTAGE, new UnsignedWordElement(0x289E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_159_VOLTAGE, new UnsignedWordElement(0x289F)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2C96, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_150_TEMPERATURE, new UnsignedWordElement(0x2C96)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_151_TEMPERATURE, new UnsignedWordElement(0x2C97)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_152_TEMPERATURE, new UnsignedWordElement(0x2C98)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_153_TEMPERATURE, new UnsignedWordElement(0x2C99)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_154_TEMPERATURE, new UnsignedWordElement(0x2C9A)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_155_TEMPERATURE, new UnsignedWordElement(0x2C9B)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_156_TEMPERATURE, new UnsignedWordElement(0x2C9C)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_157_TEMPERATURE, new UnsignedWordElement(0x2C9D)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_158_TEMPERATURE, new UnsignedWordElement(0x2C9E)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_159_TEMPERATURE, new UnsignedWordElement(0x2C9F)) //
				));
			}

			if (config.numberOfSlaves() < 18) {
				protocol.addTask(new FC3ReadRegistersTask(0x28A0, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_160_VOLTAGE, new UnsignedWordElement(0x28A0)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_161_VOLTAGE, new UnsignedWordElement(0x28A1)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_162_VOLTAGE, new UnsignedWordElement(0x28A2)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_163_VOLTAGE, new UnsignedWordElement(0x28A3)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_164_VOLTAGE, new UnsignedWordElement(0x28A4)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_165_VOLTAGE, new UnsignedWordElement(0x28A5)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_166_VOLTAGE, new UnsignedWordElement(0x28A6)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_167_VOLTAGE, new UnsignedWordElement(0x28A7)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_168_VOLTAGE, new UnsignedWordElement(0x28A8)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_169_VOLTAGE, new UnsignedWordElement(0x28A9)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2CA0, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_160_TEMPERATURE, new UnsignedWordElement(0x2CA0)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_161_TEMPERATURE, new UnsignedWordElement(0x2CA1)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_162_TEMPERATURE, new UnsignedWordElement(0x2CA2)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_163_TEMPERATURE, new UnsignedWordElement(0x2CA3)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_164_TEMPERATURE, new UnsignedWordElement(0x2CA4)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_165_TEMPERATURE, new UnsignedWordElement(0x2CA5)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_166_TEMPERATURE, new UnsignedWordElement(0x2CA6)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_167_TEMPERATURE, new UnsignedWordElement(0x2CA7)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_168_TEMPERATURE, new UnsignedWordElement(0x2CA8)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_169_TEMPERATURE, new UnsignedWordElement(0x2CA9)) //
				));
			}

			if (config.numberOfSlaves() < 19) {
				protocol.addTask(new FC3ReadRegistersTask(0x28AA, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_170_VOLTAGE, new UnsignedWordElement(0x28AA)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_171_VOLTAGE, new UnsignedWordElement(0x28AB)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_172_VOLTAGE, new UnsignedWordElement(0x28AC)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_173_VOLTAGE, new UnsignedWordElement(0x28AD)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_174_VOLTAGE, new UnsignedWordElement(0x28AE)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_175_VOLTAGE, new UnsignedWordElement(0x28AF)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_176_VOLTAGE, new UnsignedWordElement(0x28B0)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_177_VOLTAGE, new UnsignedWordElement(0x28B1)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_178_VOLTAGE, new UnsignedWordElement(0x28B2)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_179_VOLTAGE, new UnsignedWordElement(0x28B3)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2CAA, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_170_TEMPERATURE, new UnsignedWordElement(0x2CAA)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_171_TEMPERATURE, new UnsignedWordElement(0x2CAB)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_172_TEMPERATURE, new UnsignedWordElement(0x2CAC)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_173_TEMPERATURE, new UnsignedWordElement(0x2CAD)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_174_TEMPERATURE, new UnsignedWordElement(0x2CAE)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_175_TEMPERATURE, new UnsignedWordElement(0x2CAF)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_176_TEMPERATURE, new UnsignedWordElement(0x2CB0)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_177_TEMPERATURE, new UnsignedWordElement(0x2CB1)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_178_TEMPERATURE, new UnsignedWordElement(0x2CB2)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_179_TEMPERATURE, new UnsignedWordElement(0x2CB3)) //
				));
			}

			if (config.numberOfSlaves() < 20) {
				protocol.addTask(new FC3ReadRegistersTask(0x28B4, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_180_VOLTAGE, new UnsignedWordElement(0x28B4)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_181_VOLTAGE, new UnsignedWordElement(0x28B5)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_182_VOLTAGE, new UnsignedWordElement(0x28B6)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_183_VOLTAGE, new UnsignedWordElement(0x28B7)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_184_VOLTAGE, new UnsignedWordElement(0x28B8)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_185_VOLTAGE, new UnsignedWordElement(0x28B9)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_186_VOLTAGE, new UnsignedWordElement(0x28BA)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_187_VOLTAGE, new UnsignedWordElement(0x28BB)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_188_VOLTAGE, new UnsignedWordElement(0x28BC)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_189_VOLTAGE, new UnsignedWordElement(0x28BD)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2CB4, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_180_TEMPERATURE, new UnsignedWordElement(0x2CB4)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_181_TEMPERATURE, new UnsignedWordElement(0x2CB5)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_182_TEMPERATURE, new UnsignedWordElement(0x2CB6)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_183_TEMPERATURE, new UnsignedWordElement(0x2CB7)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_184_TEMPERATURE, new UnsignedWordElement(0x2CB8)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_185_TEMPERATURE, new UnsignedWordElement(0x2CB9)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_186_TEMPERATURE, new UnsignedWordElement(0x2CBA)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_187_TEMPERATURE, new UnsignedWordElement(0x2CBB)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_188_TEMPERATURE, new UnsignedWordElement(0x2CBC)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_189_TEMPERATURE, new UnsignedWordElement(0x2CBD)) //
				));
			}

			if (config.numberOfSlaves() < 21) {
				protocol.addTask(new FC3ReadRegistersTask(0x28BE, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_190_VOLTAGE, new UnsignedWordElement(0x28BE)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_191_VOLTAGE, new UnsignedWordElement(0x28BF)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_192_VOLTAGE, new UnsignedWordElement(0x28C0)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_193_VOLTAGE, new UnsignedWordElement(0x28C1)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_194_VOLTAGE, new UnsignedWordElement(0x28C2)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_195_VOLTAGE, new UnsignedWordElement(0x28C3)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_196_VOLTAGE, new UnsignedWordElement(0x28C4)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_197_VOLTAGE, new UnsignedWordElement(0x28C5)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_198_VOLTAGE, new UnsignedWordElement(0x28C6)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_199_VOLTAGE, new UnsignedWordElement(0x28C7)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2CBE, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_190_TEMPERATURE, new UnsignedWordElement(0x2CBE)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_191_TEMPERATURE, new UnsignedWordElement(0x2CBF)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_192_TEMPERATURE, new UnsignedWordElement(0x2CC0)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_193_TEMPERATURE, new UnsignedWordElement(0x2CC1)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_194_TEMPERATURE, new UnsignedWordElement(0x2CC2)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_195_TEMPERATURE, new UnsignedWordElement(0x2CC3)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_196_TEMPERATURE, new UnsignedWordElement(0x2CC4)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_197_TEMPERATURE, new UnsignedWordElement(0x2CC5)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_198_TEMPERATURE, new UnsignedWordElement(0x2CC6)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_199_TEMPERATURE, new UnsignedWordElement(0x2CC7)) //
				));
			}

			if (config.numberOfSlaves() < 22) {
				protocol.addTask(new FC3ReadRegistersTask(0x28C8, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_200_VOLTAGE, new UnsignedWordElement(0x28C8)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_201_VOLTAGE, new UnsignedWordElement(0x28C9)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_202_VOLTAGE, new UnsignedWordElement(0x28CA)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_203_VOLTAGE, new UnsignedWordElement(0x28CB)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_204_VOLTAGE, new UnsignedWordElement(0x28CC)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_205_VOLTAGE, new UnsignedWordElement(0x28CD)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_206_VOLTAGE, new UnsignedWordElement(0x28CE)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_207_VOLTAGE, new UnsignedWordElement(0x28CF)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_208_VOLTAGE, new UnsignedWordElement(0x28D0)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_209_VOLTAGE, new UnsignedWordElement(0x28D1)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2CC8, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_200_TEMPERATURE, new UnsignedWordElement(0x2CC8)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_201_TEMPERATURE, new UnsignedWordElement(0x2CC9)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_202_TEMPERATURE, new UnsignedWordElement(0x2CCA)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_203_TEMPERATURE, new UnsignedWordElement(0x2CCB)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_204_TEMPERATURE, new UnsignedWordElement(0x2CCC)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_205_TEMPERATURE, new UnsignedWordElement(0x2CCD)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_206_TEMPERATURE, new UnsignedWordElement(0x2CCE)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_207_TEMPERATURE, new UnsignedWordElement(0x2CCF)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_208_TEMPERATURE, new UnsignedWordElement(0x2CD0)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_209_TEMPERATURE, new UnsignedWordElement(0x2CD1)) //
				));
			}

			if (config.numberOfSlaves() < 23) {
				protocol.addTask(new FC3ReadRegistersTask(0x28D2, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_210_VOLTAGE, new UnsignedWordElement(0x28D2)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_211_VOLTAGE, new UnsignedWordElement(0x28D3)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_212_VOLTAGE, new UnsignedWordElement(0x28D4)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_213_VOLTAGE, new UnsignedWordElement(0x28D5)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_214_VOLTAGE, new UnsignedWordElement(0x28D6)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_215_VOLTAGE, new UnsignedWordElement(0x28D7)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_216_VOLTAGE, new UnsignedWordElement(0x28D8)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_217_VOLTAGE, new UnsignedWordElement(0x28D9)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_218_VOLTAGE, new UnsignedWordElement(0x28DA)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_219_VOLTAGE, new UnsignedWordElement(0x28DB)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2CD2, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_210_TEMPERATURE, new UnsignedWordElement(0x2CD2)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_211_TEMPERATURE, new UnsignedWordElement(0x2CD3)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_212_TEMPERATURE, new UnsignedWordElement(0x2CD4)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_213_TEMPERATURE, new UnsignedWordElement(0x2CD5)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_214_TEMPERATURE, new UnsignedWordElement(0x2CD6)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_215_TEMPERATURE, new UnsignedWordElement(0x2CD7)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_216_TEMPERATURE, new UnsignedWordElement(0x2CD8)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_217_TEMPERATURE, new UnsignedWordElement(0x2CD9)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_218_TEMPERATURE, new UnsignedWordElement(0x2CDA)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_219_TEMPERATURE, new UnsignedWordElement(0x2CDB)) //
				));
			}

			if (config.numberOfSlaves() < 24) {
				protocol.addTask(new FC3ReadRegistersTask(0x28DC, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_220_VOLTAGE, new UnsignedWordElement(0x28DC)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_221_VOLTAGE, new UnsignedWordElement(0x28DD)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_222_VOLTAGE, new UnsignedWordElement(0x28DE)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_223_VOLTAGE, new UnsignedWordElement(0x28DF)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_224_VOLTAGE, new UnsignedWordElement(0x28E0)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_225_VOLTAGE, new UnsignedWordElement(0x28E1)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_226_VOLTAGE, new UnsignedWordElement(0x28E2)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_227_VOLTAGE, new UnsignedWordElement(0x28E3)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_228_VOLTAGE, new UnsignedWordElement(0x28E4)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_229_VOLTAGE, new UnsignedWordElement(0x28E5)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2CDC, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_220_TEMPERATURE, new UnsignedWordElement(0x2CDC)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_221_TEMPERATURE, new UnsignedWordElement(0x2CDD)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_222_TEMPERATURE, new UnsignedWordElement(0x2CDE)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_223_TEMPERATURE, new UnsignedWordElement(0x2CDF)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_224_TEMPERATURE, new UnsignedWordElement(0x2CE0)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_225_TEMPERATURE, new UnsignedWordElement(0x2CE1)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_226_TEMPERATURE, new UnsignedWordElement(0x2CE2)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_227_TEMPERATURE, new UnsignedWordElement(0x2CE3)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_228_TEMPERATURE, new UnsignedWordElement(0x2CE4)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_229_TEMPERATURE, new UnsignedWordElement(0x2CE5)) //
				));
			}

			if (config.numberOfSlaves() < 25) {
				protocol.addTask(new FC3ReadRegistersTask(0x28E6, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_230_VOLTAGE, new UnsignedWordElement(0x28E6)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_231_VOLTAGE, new UnsignedWordElement(0x28E7)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_232_VOLTAGE, new UnsignedWordElement(0x28E8)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_233_VOLTAGE, new UnsignedWordElement(0x28E9)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_234_VOLTAGE, new UnsignedWordElement(0x28EA)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_235_VOLTAGE, new UnsignedWordElement(0x28EB)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_236_VOLTAGE, new UnsignedWordElement(0x28EC)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_237_VOLTAGE, new UnsignedWordElement(0x28ED)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_238_VOLTAGE, new UnsignedWordElement(0x28EE)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_239_VOLTAGE, new UnsignedWordElement(0x28EF)) //
				));

				protocol.addTask(new FC3ReadRegistersTask(0x2CE6, Priority.LOW, //
						m(VersionBChannelId.CLUSTER_1_BATTERY_230_TEMPERATURE, new UnsignedWordElement(0x2CE6)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_231_TEMPERATURE, new UnsignedWordElement(0x2CE7)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_232_TEMPERATURE, new UnsignedWordElement(0x2CE8)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_233_TEMPERATURE, new UnsignedWordElement(0x2CE9)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_234_TEMPERATURE, new UnsignedWordElement(0x2CEA)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_235_TEMPERATURE, new UnsignedWordElement(0x2CEB)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_236_TEMPERATURE, new UnsignedWordElement(0x2CEC)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_237_TEMPERATURE, new UnsignedWordElement(0x2CED)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_238_TEMPERATURE, new UnsignedWordElement(0x2CEE)), //
						m(VersionBChannelId.CLUSTER_1_BATTERY_239_TEMPERATURE, new UnsignedWordElement(0x2CEF)) //
				));
			}
		}
		return protocol;
	}
}
