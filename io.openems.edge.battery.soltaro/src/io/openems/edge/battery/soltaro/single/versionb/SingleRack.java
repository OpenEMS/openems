package io.openems.edge.battery.soltaro.single.versionb;

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
import io.openems.edge.battery.soltaro.State;
import io.openems.edge.battery.soltaro.single.versionb.Enums.AutoSetFunction;
import io.openems.edge.battery.soltaro.single.versionb.Enums.ContactorControl;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
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
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.Soltaro.SingleRack.VersionB", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class SingleRack extends AbstractOpenemsModbusComponent implements Battery, OpenemsComponent, EventHandler, ModbusSlave
{ // , // JsonApi // TODO

	protected static final int SYSTEM_ON = 1;
	protected final static int SYSTEM_OFF = 0;

	private static final String KEY_TEMPERATURE = "_TEMPERATURE";
	private static final String KEY_VOLTAGE = "_VOLTAGE";
	private static final Integer SYSTEM_RESET = 0x1;
	private static final String NUMBER_FORMAT = "%03d"; // creates string number with leading zeros
	private static final double MAX_TOLERANCE_CELL_VOLTAGE_CHANGES_MILLIVOLT = 50;
	private static final double MAX_TOLERANCE_CELL_VOLTAGES_MILLIVOLT = 400;
	
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
	private int DELAY_AUTO_ID_SECONDS = 5;
	private int DELAY_AFTER_CONFIGURING_FINISHED = 5;

	// Remind last min and max cell voltages to register a cell drift
	private double lastMinCellVoltage = Double.MIN_VALUE;
	private double lastMaxCellVoltage = Double.MIN_VALUE;
	private ResetState resetState = ResetState.NONE;
	
	private LocalDateTime pendingTimestamp;

	public SingleRack() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				SingleRackChannelId.values() //
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
		log.info("SingleRack.handleStateMachine(): State: " + this.getStateMachineState());
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
			} else if (this.isCellVoltagesDrift()) {				
				this.setStateMachineState(State.ERROR_CELL_VOLTAGES_DRIFT);				
			}
			else {
//				// if minimal cell voltage is lower than configured minimal cell voltage, then force system to charge
//				IntegerReadChannel minCellVoltageChannel = this.channel(SingleRackChannelId.CLUSTER_1_MIN_CELL_VOLTAGE);
//				Optional<Integer> minCellVoltageOpt = minCellVoltageChannel.value().asOptional();
//				if (minCellVoltageOpt.isPresent()) {
//					int minCellVoltage =  minCellVoltageOpt.get();
//					if (minCellVoltage < this.config.minimalCellVoltage()) {
//						// set the discharge current negative to force the system to charge
//						// TODO check if this is working!
//						this.getDischargeMaxCurrent().setNextValue( (-1) * this.getChargeMaxCurrent().value().get() );
//					}
//				}		
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
		case ERROR_CELL_VOLTAGES_DRIFT:
			this.handleCellDrift();
		}

		this.getReadyForWorking().setNextValue(readyForWorking);
	}

	private void handleCellDrift() {
		// To reset the cell drift phenomenon, first sleep and then reset the system 		
		switch(this.resetState) {
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
			this.setStateMachineState(State.UNDEFINED);
			break;
		}
	}

	private void resetSystem() {

		IntegerWriteChannel resetChannel = this.channel(SingleRackChannelId.SYSTEM_RESET);
		try {
			resetChannel.setNextWriteValue(SYSTEM_RESET);				
		} catch (OpenemsNamedException e) {
			System.out.println("Error while trying to reset the system!");
		}
	}

	private void sleepSystem() {

		IntegerWriteChannel sleepChannel = this.channel(SingleRackChannelId.SLEEP);
		try {
			sleepChannel.setNextWriteValue(0x1);
		} catch (OpenemsNamedException e) {
			System.out.println("Error while trying to sleep the system!");
		}
		
	}

	/*
	 * This function tries to find out if cell voltages has been drifted away see
	 * \doc\cell_drift.png If this phenomenon has happened, a system reset is
	 * necessary
	 */
	private boolean isCellVoltagesDrift() {

		Optional<Integer> maxCellVoltageOpt = this.getMaxCellVoltage().getNextValue().asOptional();
		Optional<Integer> minCellVoltageOpt = this.getMinCellVoltage().getNextValue().asOptional();

		if (!maxCellVoltageOpt.isPresent() || !minCellVoltageOpt.isPresent()) {
			return false; // no new values, comparison not possible
		}

		double currentMaxCellVoltage = maxCellVoltageOpt.get();
		double currentMinCellVoltage = minCellVoltageOpt.get();

		if (lastMaxCellVoltage == Double.MIN_VALUE || lastMinCellVoltage == Double.MIN_VALUE) {
			// Not all values has been set yet, check is not possible
			lastMaxCellVoltage = currentMaxCellVoltage;
			lastMinCellVoltage = currentMinCellVoltage;

			return false;
		}
		double deltaMax = lastMaxCellVoltage - currentMaxCellVoltage;
		double deltaMin = lastMinCellVoltage - currentMinCellVoltage;
		double deltaMinMax = currentMaxCellVoltage - currentMinCellVoltage;

		lastMaxCellVoltage = currentMaxCellVoltage;
		lastMinCellVoltage = currentMinCellVoltage;

		if (deltaMax < 0 && deltaMin > 0) { // max cell rises, min cell falls
			// at least one of them changes faster than typically
			if (deltaMinMax > MAX_TOLERANCE_CELL_VOLTAGES_MILLIVOLT && //
					(Math.abs(deltaMax) > MAX_TOLERANCE_CELL_VOLTAGE_CHANGES_MILLIVOLT
							|| Math.abs(deltaMin) > MAX_TOLERANCE_CELL_VOLTAGE_CHANGES_MILLIVOLT)) {

				// If cells are neighbours then there is a drift error
				Optional<Integer> minCellVoltageIdOpt = this.channel(SingleRackChannelId.CLUSTER_1_MIN_CELL_VOLTAGE_ID);
				Optional<Integer> maxCellVoltageIdOpt = this.channel(SingleRackChannelId.CLUSTER_1_MAX_CELL_VOLTAGE_ID);

				if (!minCellVoltageIdOpt.isPresent() || !maxCellVoltageIdOpt.isPresent()) {
					return false;
				}

				int minCellVoltageId = minCellVoltageIdOpt.get();
				int maxCellVoltageId = maxCellVoltageIdOpt.get();

				if (Math.abs(minCellVoltageId - maxCellVoltageId) == 1) {
					return true;
				}

			}

		}

		return false;
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

	private void initializeCallbacks() {

		this.channel(SingleRackChannelId.CLUSTER_1_VOLTAGE).onChange(value -> {
			@SuppressWarnings("unchecked")
			Optional<Integer> vOpt = (Optional<Integer>) value.asOptional();
			if (!vOpt.isPresent()) {
				return;
			}
			int voltage_volt = (int) (vOpt.get() * 0.001);
			log.debug("callback voltage, value: " + voltage_volt);
			this.channel(Battery.ChannelId.VOLTAGE).setNextValue(voltage_volt);
		});

		this.channel(SingleRackChannelId.CLUSTER_1_MIN_CELL_VOLTAGE).onChange(value -> {
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
		this.channel(SingleRackChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM).onChange(value -> {
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
		this.channel(SingleRackChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM).onChange(value -> {
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
		this.channel(SingleRackChannelId.SYSTEM_MAX_CHARGE_CURRENT).onChange(value -> {
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
		this.channel(SingleRackChannelId.SYSTEM_MAX_DISCHARGE_CURRENT).onChange(value -> {
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
				if (timeAfterAutoId.plusSeconds(DELAY_AUTO_ID_SECONDS).isAfter(LocalDateTime.now())) {
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
				if (timeAfterAutoId.plusSeconds(DELAY_AUTO_ID_SECONDS).isAfter(LocalDateTime.now())) {
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
				if (configuringFinished.plusSeconds(DELAY_AFTER_CONFIGURING_FINISHED).isAfter(LocalDateTime.now())) {
					System.out.println(">>> Delay time after configuring!");
				} else {
					System.out.println("Delay time after configuring is over, reset system");
					IntegerWriteChannel resetChannel = this.channel(SingleRackChannelId.SYSTEM_RESET);
					try {
						resetChannel.setNextWriteValue(SYSTEM_RESET);
						configuringFinished = null;
					} catch (OpenemsNamedException e) {
						System.out.println("Error while trying to reset the system!");
					}
				}
			}
			break;
		case RESTART_AFTER_SETTING:
			// A manual restart is needed
			System.out.println("====>>>  Please restart system manually!");
		case NONE:
			break;
		}
	}

	private void setVoltageRanges() {

		try {
			IntegerWriteChannel level1OverVoltageChannel = this
					.channel(SingleRackChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM);
			level1OverVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_OVER_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level1OverVoltageChannelRecover = this
					.channel(SingleRackChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER);
			level1OverVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
					* ModuleParameters.LEVEL_1_TOTAL_OVER_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			IntegerWriteChannel level1LowVoltageChannel = this
					.channel(SingleRackChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM);
			level1LowVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_LOW_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level1LowVoltageChannelRecover = this
					.channel(SingleRackChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER);
			level1LowVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
					* ModuleParameters.LEVEL_1_TOTAL_LOW_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			IntegerWriteChannel level2OverVoltageChannel = this
					.channel(SingleRackChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION);
			level2OverVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_OVER_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level2OverVoltageChannelRecover = this
					.channel(SingleRackChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER);
			level2OverVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
					* ModuleParameters.LEVEL_2_TOTAL_OVER_VOLTAGE_RECOVER_MILLIVOLT.getValue());

			IntegerWriteChannel level2LowVoltageChannel = this
					.channel(SingleRackChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION);
			level2LowVoltageChannel.setNextWriteValue(
					this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_LOW_VOLTAGE_MILLIVOLT.getValue());

			IntegerWriteChannel level2LowVoltageChannelRecover = this
					.channel(SingleRackChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER);
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
				.channel(SingleRackChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID);
		Optional<Integer> autoSetTemperatureSlavesIdOpt = autoSetTemperatureSlavesIdChannel.value().asOptional();
		if (!autoSetTemperatureSlavesIdOpt.isPresent()) {
			return;
		}
		int autoSetTemperatureSlaves = autoSetTemperatureSlavesIdOpt.get();
		if (autoSetTemperatureSlaves == Enums.AutoSetFunction.FAILURE.getValue()) {
			log.error("Auto set temperature slaves id failed! Start configuring process again!");
			// Auto set failed, try again
			nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
		} else if (autoSetTemperatureSlaves == Enums.AutoSetFunction.SUCCES.getValue()) {
			log.info("Auto set temperature slaves id succeeded!");
			nextConfiguringProcess = ConfiguringProcess.SET_VOLTAGE_RANGES;
		}
	}

	private void setTemperatureIdAutoConfiguring() {

		IntegerWriteChannel autoSetSlavesTemperatureIdChannel = this
				.channel(SingleRackChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID);
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
		IntegerReadChannel autoSetSlavesIdChannel = this.channel(SingleRackChannelId.AUTO_SET_SLAVES_ID);
		Optional<Integer> autoSetSlavesIdOpt = autoSetSlavesIdChannel.value().asOptional();
		if (!autoSetSlavesIdOpt.isPresent()) {
			return;
		}
		int autoSetSlaves = autoSetSlavesIdOpt.get();
		if (autoSetSlaves == Enums.AutoSetFunction.FAILURE.getValue()) {
			log.error("Auto set slaves id failed! Start configuring process again!");
			// Auto set failed, try again
			nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
		} else if (autoSetSlaves == Enums.AutoSetFunction.SUCCES.getValue()) {
			log.info("Auto set slaves id succeeded!");
			nextConfiguringProcess = ConfiguringProcess.SET_TEMPERATURE_ID_AUTO_CONFIGURING;
		}
	}

	private void setIdAutoConfiguring() {
		// Set number of modules
		IntegerWriteChannel autoSetSlavesIdChannel = this.channel(SingleRackChannelId.AUTO_SET_SLAVES_ID);
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
				.channel(SingleRackChannelId.WORK_PARAMETER_PCS_COMMUNICATION_RATE);
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

	private boolean isSystemRunning() {
		EnumReadChannel contactorControlChannel = this.channel(SingleRackChannelId.BMS_CONTACTOR_CONTROL);
		ContactorControl cc = contactorControlChannel.value().asEnum();
		return cc == ContactorControl.ON_GRID;
	}

	private boolean isSystemStopped() {
		EnumReadChannel contactorControlChannel = this.channel(SingleRackChannelId.BMS_CONTACTOR_CONTROL);
		ContactorControl cc = contactorControlChannel.value().asEnum();
		return cc == ContactorControl.CUT_OFF;
	}

	/**
	 * Checks whether system has an undefined state, e.g. rack 1 & 2 are configured,
	 * but only rack 1 is running. This state can only be reached at startup coming
	 * from state undefined
	 */
	private boolean isSystemStatePending() {
		return !isSystemRunning() && !isSystemStopped();
	}

	private boolean isAlarmLevel2Error() {
		return (readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_SOC_LOW)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_TEMPERATURE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_POLES_TEMPERATURE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_INSULATION_LOW)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)
				|| readValueFromBooleanChannel(SingleRackChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW));
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
				+ "|Charge:" + this.getChargeMaxVoltage().value() + ";" + this.getChargeMaxCurrent().value() + "|State:"
				+ this.getStateMachineState();
	}

	private void startSystem() {
		EnumWriteChannel contactorControlChannel = this.channel(SingleRackChannelId.BMS_CONTACTOR_CONTROL);
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
		EnumWriteChannel contactorControlChannel = this.channel(SingleRackChannelId.BMS_CONTACTOR_CONTROL);
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
		this.channel(SingleRackChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	private void setSoCLowAlarm(int soCLowAlarm) {
		try {
			((IntegerWriteChannel) this.channel(SingleRackChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION))
					.setNextWriteValue(soCLowAlarm);
			((IntegerWriteChannel) this.channel(SingleRackChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER))
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
						m(SingleRackChannelId.BMS_CONTACTOR_CONTROL, new UnsignedWordElement(0x2010)) //
				),

				// System reset
				new FC6WriteRegisterTask(0x2004, m(SingleRackChannelId.SYSTEM_RESET, new UnsignedWordElement(0x2004)) //
				),

				// EMS timeout --> Watchdog
				new FC6WriteRegisterTask(0x201C,
						m(SingleRackChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x201C)) //
				),
				// Sleep
				new FC6WriteRegisterTask(0x201D, m(SingleRackChannelId.SLEEP, new UnsignedWordElement(0x201D)) //
				),

				// Work parameter
				new FC6WriteRegisterTask(0x20C1,
						m(SingleRackChannelId.WORK_PARAMETER_PCS_COMMUNICATION_RATE, new UnsignedWordElement(0x20C1)) //
				),

				// Paramaeters for configuring
				new FC6WriteRegisterTask(0x2014,
						m(SingleRackChannelId.AUTO_SET_SLAVES_ID, new UnsignedWordElement(0x2014))),
				new FC6WriteRegisterTask(0x2019,
						m(SingleRackChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID, new UnsignedWordElement(0x2019))),

				// Control registers
				new FC3ReadRegistersTask(0x2000, Priority.HIGH, //
						m(SingleRackChannelId.FAN_STATUS, new UnsignedWordElement(0x2000)), //
						m(SingleRackChannelId.MAIN_CONTACTOR_STATE, new UnsignedWordElement(0x2001)), //
						m(SingleRackChannelId.DRY_CONTACT_1_EXPORT, new UnsignedWordElement(0x2002)), //
						m(SingleRackChannelId.DRY_CONTACT_2_EXPORT, new UnsignedWordElement(0x2003)), //
						m(SingleRackChannelId.SYSTEM_RESET, new UnsignedWordElement(0x2004)), //
						m(SingleRackChannelId.SYSTEM_RUN_MODE, new UnsignedWordElement(0x2005)), //
						m(SingleRackChannelId.PRE_CONTACTOR_STATUS, new UnsignedWordElement(0x2006)), //
						m(new BitsWordElement(0x2007, this) //
								.bit(15, SingleRackChannelId.ALARM_FLAG_STATUS_DISCHARGE_TEMPERATURE_LOW) //
								.bit(14, SingleRackChannelId.ALARM_FLAG_STATUS_DISCHARGE_TEMPERATURE_HIGH) //
								.bit(13, SingleRackChannelId.ALARM_FLAG_STATUS_VOLTAGE_DIFFERENCE) //
								.bit(12, SingleRackChannelId.ALARM_FLAG_STATUS_INSULATION_LOW) //
								.bit(11, SingleRackChannelId.ALARM_FLAG_STATUS_CELL_VOLTAGE_DIFFERENCE) //
								.bit(10, SingleRackChannelId.ALARM_FLAG_STATUS_ELECTRODE_TEMPERATURE_HIGH) //
								.bit(9, SingleRackChannelId.ALARM_FLAG_STATUS_TEMPERATURE_DIFFERENCE) //
								.bit(8, SingleRackChannelId.ALARM_FLAG_STATUS_SOC_LOW) //
								.bit(7, SingleRackChannelId.ALARM_FLAG_STATUS_CELL_OVER_TEMPERATURE) //
								.bit(6, SingleRackChannelId.ALARM_FLAG_STATUS_CELL_LOW_TEMPERATURE) //
								.bit(5, SingleRackChannelId.ALARM_FLAG_STATUS_DISCHARGE_OVER_CURRENT) //
								.bit(4, SingleRackChannelId.ALARM_FLAG_STATUS_SYSTEM_LOW_VOLTAGE) //
								.bit(3, SingleRackChannelId.ALARM_FLAG_STATUS_CELL_LOW_VOLTAGE) //
								.bit(2, SingleRackChannelId.ALARM_FLAG_STATUS_CHARGE_OVER_CURRENT) //
								.bit(1, SingleRackChannelId.ALARM_FLAG_STATUS_SYSTEM_OVER_VOLTAGE) //
								.bit(0, SingleRackChannelId.ALARM_FLAG_STATUS_CELL_OVER_VOLTAGE) //
						), //
						m(new BitsWordElement(0x2008, this) //
								.bit(15, SingleRackChannelId.PROTECT_FLAG_STATUS_DISCHARGE_TEMPERATURE_LOW) //
								.bit(14, SingleRackChannelId.PROTECT_FLAG_STATUS_DISCHARGE_TEMPERATURE_HIGH) //
								.bit(13, SingleRackChannelId.PROTECT_FLAG_STATUS_VOLTAGE_DIFFERENCE) //
								.bit(12, SingleRackChannelId.PROTECT_FLAG_STATUS_INSULATION_LOW) //
								.bit(11, SingleRackChannelId.PROTECT_FLAG_STATUS_CELL_VOLTAGE_DIFFERENCE) //
								.bit(10, SingleRackChannelId.PROTECT_FLAG_STATUS_ELECTRODE_TEMPERATURE_HIGH) //
								.bit(9, SingleRackChannelId.PROTECT_FLAG_STATUS_TEMPERATURE_DIFFERENCE) //
								.bit(8, SingleRackChannelId.PROTECT_FLAG_STATUS_SOC_LOW) //
								.bit(7, SingleRackChannelId.PROTECT_FLAG_STATUS_CELL_OVER_TEMPERATURE) //
								.bit(6, SingleRackChannelId.PROTECT_FLAG_STATUS_CELL_LOW_TEMPERATURE) //
								.bit(5, SingleRackChannelId.PROTECT_FLAG_STATUS_DISCHARGE_OVER_CURRENT) //
								.bit(4, SingleRackChannelId.PROTECT_FLAG_STATUS_SYSTEM_LOW_VOLTAGE) //
								.bit(3, SingleRackChannelId.PROTECT_FLAG_STATUS_CELL_LOW_VOLTAGE) //
								.bit(2, SingleRackChannelId.PROTECT_FLAG_STATUS_CHARGE_OVER_CURRENT) //
								.bit(1, SingleRackChannelId.PROTECT_FLAG_STATUS_SYSTEM_OVER_VOLTAGE) //
								.bit(0, SingleRackChannelId.PROTECT_FLAG_STATUS_CELL_OVER_VOLTAGE) //
						), //
						m(SingleRackChannelId.ALARM_FLAG_REGISTER_1, new UnsignedWordElement(0x2009)), //
						m(SingleRackChannelId.ALARM_FLAG_REGISTER_2, new UnsignedWordElement(0x200A)), //
						m(SingleRackChannelId.PROTECT_FLAG_REGISTER_1, new UnsignedWordElement(0x200B)), //
						m(SingleRackChannelId.PROTECT_FLAG_REGISTER_2, new UnsignedWordElement(0x200C)), //
						m(SingleRackChannelId.SHORT_CIRCUIT_FUNCTION, new UnsignedWordElement(0x200D)), //
						m(SingleRackChannelId.TESTING_IO, new UnsignedWordElement(0x200E)), //
						m(SingleRackChannelId.SOFT_SHUTDOWN, new UnsignedWordElement(0x200F)), //
						m(SingleRackChannelId.BMS_CONTACTOR_CONTROL, new UnsignedWordElement(0x2010)), //
						m(SingleRackChannelId.CURRENT_BOX_SELF_CALIBRATION, new UnsignedWordElement(0x2011)), //
						m(SingleRackChannelId.PCS_ALARM_RESET, new UnsignedWordElement(0x2012)), //
						m(SingleRackChannelId.INSULATION_SENSOR_FUNCTION, new UnsignedWordElement(0x2013)), //
						m(SingleRackChannelId.AUTO_SET_SLAVES_ID, new UnsignedWordElement(0x2014)), //
						new DummyRegisterElement(0x2015, 0x2018), //
						m(SingleRackChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID, new UnsignedWordElement(0x2019)), //
						m(SingleRackChannelId.TRANSPARENT_MASTER, new UnsignedWordElement(0x201A)), //
						m(SingleRackChannelId.SET_EMS_ADDRESS, new UnsignedWordElement(0x201B)), //
						m(SingleRackChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x201C)), //
						m(SingleRackChannelId.SLEEP, new UnsignedWordElement(0x201D)), //
						m(SingleRackChannelId.VOLTAGE_LOW_PROTECTION, new UnsignedWordElement(0x201E)) //
				), //

				// Voltage ranges
				new FC3ReadRegistersTask(0x2082, Priority.LOW, //
						m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2082),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0x2083, 0x2087),
						m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM,
								new UnsignedWordElement(0x2088), ElementToChannelConverter.SCALE_FACTOR_2) //
				),

				// Summary state
				new FC3ReadRegistersTask(0x2100, Priority.LOW,
						m(SingleRackChannelId.CLUSTER_1_VOLTAGE, new UnsignedWordElement(0x2100),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SingleRackChannelId.CLUSTER_1_CURRENT, new UnsignedWordElement(0x2101),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SingleRackChannelId.CLUSTER_1_CHARGE_INDICATION, new UnsignedWordElement(0x2102)),
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x2103)),
						m(SingleRackChannelId.CLUSTER_1_SOH, new UnsignedWordElement(0x2104)),
						m(SingleRackChannelId.CLUSTER_1_MAX_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2105)), //
						m(SingleRackChannelId.CLUSTER_1_MAX_CELL_VOLTAGE, new UnsignedWordElement(0x2106)), //
						m(SingleRackChannelId.CLUSTER_1_MIN_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2107)), //
						m(SingleRackChannelId.CLUSTER_1_MIN_CELL_VOLTAGE, new UnsignedWordElement(0x2108)), //
						m(SingleRackChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x2109)), //
						m(SingleRackChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE, new UnsignedWordElement(0x210A)), //
						m(SingleRackChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x210B)), //
						m(SingleRackChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE, new UnsignedWordElement(0x210C)), //
						m(SingleRackChannelId.MAX_CELL_RESISTANCE_ID, new UnsignedWordElement(0x210D)), //
						m(SingleRackChannelId.MAX_CELL_RESISTANCE, new UnsignedWordElement(0x210E),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(SingleRackChannelId.MIN_CELL_RESISTANCE_ID, new UnsignedWordElement(0x210F)), //
						m(SingleRackChannelId.MIN_CELL_RESISTANCE, new UnsignedWordElement(0x2110),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(SingleRackChannelId.POSITIVE_INSULATION, new UnsignedWordElement(0x2111)), //
						m(SingleRackChannelId.NEGATIVE_INSULATION, new UnsignedWordElement(0x2112)), //
						m(SingleRackChannelId.MAIN_CONTACTOR_FLAG, new UnsignedWordElement(0x2113)), //
						new DummyRegisterElement(0x2114),
						m(SingleRackChannelId.ENVIRONMENT_TEMPERATURE, new UnsignedWordElement(0x2115)), //
						m(SingleRackChannelId.SYSTEM_INSULATION, new UnsignedWordElement(0x2116)), //
						m(SingleRackChannelId.CELL_VOLTAGE_DIFFERENCE, new UnsignedWordElement(0x2117)), //
						m(SingleRackChannelId.TOTAL_VOLTAGE_DIFFERENCE, new UnsignedWordElement(0x2118),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SingleRackChannelId.POWER_TEMPERATURE, new UnsignedWordElement(0x2119)), //
						m(SingleRackChannelId.POWER_SUPPLY_VOLTAGE, new UnsignedWordElement(0x211A)) //
				),

				// Critical state
				new FC3ReadRegistersTask(0x2140, Priority.HIGH, //
						m(new BitsWordElement(0x2140, this) //
								.bit(0, SingleRackChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH) //
								.bit(1, SingleRackChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SingleRackChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH) //
								.bit(3, SingleRackChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW) //
								.bit(4, SingleRackChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW) //
								.bit(5, SingleRackChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH) //
								.bit(6, SingleRackChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH) //
								.bit(7, SingleRackChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW) //
								.bit(8, SingleRackChannelId.ALARM_LEVEL_2_SOC_LOW) //
								.bit(9, SingleRackChannelId.ALARM_LEVEL_2_TEMPERATURE_DIFFERENCE_HIGH) //
								.bit(10, SingleRackChannelId.ALARM_LEVEL_2_POLES_TEMPERATURE_DIFFERENCE_HIGH) //
								.bit(11, SingleRackChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_DIFFERENCE_HIGH) //
								.bit(12, SingleRackChannelId.ALARM_LEVEL_2_INSULATION_LOW) //
								.bit(13, SingleRackChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_DIFFERENCE_HIGH) //
								.bit(14, SingleRackChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, SingleRackChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW) //
						), //
						m(new BitsWordElement(0x2141, this) //
								.bit(0, SingleRackChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_HIGH) //
								.bit(1, SingleRackChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SingleRackChannelId.ALARM_LEVEL_1_CHA_CURRENT_HIGH) //
								.bit(3, SingleRackChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_LOW) //
								.bit(4, SingleRackChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW) //
								.bit(5, SingleRackChannelId.ALARM_LEVEL_1_DISCHA_CURRENT_HIGH) //
								.bit(6, SingleRackChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH) //
								.bit(7, SingleRackChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_LOW) //
								.bit(8, SingleRackChannelId.ALARM_LEVEL_1_SOC_LOW) //
								.bit(9, SingleRackChannelId.ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH) //
								.bit(10, SingleRackChannelId.ALARM_LEVEL_1_POLE_TEMPERATURE_TOO_HIGH) //
								.bit(11, SingleRackChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH) //
								.bit(12, SingleRackChannelId.ALARM_LEVEL_1_INSULATION_LOW) //
								.bit(13, SingleRackChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH) //
								.bit(14, SingleRackChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, SingleRackChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW) //
						), //
						m(SingleRackChannelId.CLUSTER_RUN_STATE, new UnsignedWordElement(0x2142)), //

						m(SingleRackChannelId.MAXIMUM_CELL_VOLTAGE_NUMBER_WHEN_ALARM, new UnsignedWordElement(0x2143)), //
						m(SingleRackChannelId.MAXIMUM_CELL_VOLTAGE_WHEN_ALARM, new UnsignedWordElement(0x2144)), //
						m(SingleRackChannelId.MAXIMUM_CELL_VOLTAGE_NUMBER_WHEN_STOPPED,
								new UnsignedWordElement(0x2145)), //
						m(SingleRackChannelId.MAXIMUM_CELL_VOLTAGE_WHEN_STOPPED, new UnsignedWordElement(0x2146)), //
						m(SingleRackChannelId.MINIMUM_CELL_VOLTAGE_NUMBER_WHEN_ALARM, new UnsignedWordElement(0x2147)), //
						m(SingleRackChannelId.MINIMUM_CELL_VOLTAGE_WHEN_ALARM, new UnsignedWordElement(0x2148)), //
						m(SingleRackChannelId.MINIMUM_CELL_VOLTAGE_NUMBER_WHEN_STOPPED,
								new UnsignedWordElement(0x2149)), //
						m(SingleRackChannelId.MINIMUM_CELL_VOLTAGE_WHEN_STOPPED, new UnsignedWordElement(0x214A)), //
						m(SingleRackChannelId.OVER_VOLTAGE_VALUE_WHEN_ALARM, new UnsignedWordElement(0x214B)), //
						m(SingleRackChannelId.OVER_VOLTAGE_VALUE_WHEN_STOPPED, new UnsignedWordElement(0x214C)), //
						m(SingleRackChannelId.UNDER_VOLTAGE_VALUE_WHEN_ALARM, new UnsignedWordElement(0x214D)), //
						m(SingleRackChannelId.UNDER_VOLTAGE_VALUE_WHEN_STOPPED, new UnsignedWordElement(0x214E)), //
						m(SingleRackChannelId.OVER_CHARGE_CURRENT_WHEN_ALARM, new UnsignedWordElement(0x214F)), //
						m(SingleRackChannelId.OVER_CHARGE_CURRENT_WHEN_STOPPED, new UnsignedWordElement(0x2150)), //
						m(SingleRackChannelId.OVER_DISCHARGE_CURRENT_WHEN_ALARM, new UnsignedWordElement(0x2151)), //
						m(SingleRackChannelId.OVER_DISCHARGE_CURRENT_WHEN_STOPPED, new UnsignedWordElement(0x2152)), //
						m(SingleRackChannelId.NUMBER_OF_TEMPERATURE_WHEN_ALARM, new UnsignedWordElement(0x2153)), //
						new DummyRegisterElement(0x2154, 0x215A), //
						m(SingleRackChannelId.OTHER_ALARM_EQUIPMENT_FAILURE, new UnsignedWordElement(0x215B)), //
						new DummyRegisterElement(0x215C, 0x215F), //
						m(SingleRackChannelId.SYSTEM_MAX_CHARGE_CURRENT, new UnsignedWordElement(0x2160),
								ElementToChannelConverter.SCALE_FACTOR_2), // TODO Check if correct!
						m(SingleRackChannelId.SYSTEM_MAX_DISCHARGE_CURRENT, new UnsignedWordElement(0x2161),
								ElementToChannelConverter.SCALE_FACTOR_2) // TODO Check if correct!
				), //

				// Cluster info
				new FC3ReadRegistersTask(0x2180, Priority.LOW, //
						m(SingleRackChannelId.CYCLE_TIME, new UnsignedWordElement(0x2180)), //
						m(SingleRackChannelId.TOTAL_CAPACITY_HIGH_BITS, new UnsignedWordElement(0x2181)), //
						m(SingleRackChannelId.TOTAL_CAPACITY_LOW_BITS, new UnsignedWordElement(0x2182)), //
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
								.bit(0, SingleRackChannelId.FAILURE_SAMPLING_WIRE)//
								.bit(1, SingleRackChannelId.FAILURE_CONNECTOR_WIRE)//
								.bit(2, SingleRackChannelId.FAILURE_LTC6803)//
								.bit(3, SingleRackChannelId.FAILURE_VOLTAGE_SAMPLING)//
								.bit(4, SingleRackChannelId.FAILURE_TEMP_SAMPLING)//
								.bit(5, SingleRackChannelId.FAILURE_TEMP_SENSOR)//
								.bit(6, SingleRackChannelId.FAILURE_GR_T)//
								.bit(7, SingleRackChannelId.FAILURE_PCB)//
								.bit(8, SingleRackChannelId.FAILURE_BALANCING_MODULE)//
								.bit(9, SingleRackChannelId.FAILURE_TEMP_SAMPLING_LINE)//
								.bit(10, SingleRackChannelId.FAILURE_INTRANET_COMMUNICATION)//
								.bit(11, SingleRackChannelId.FAILURE_EEPROM)//
								.bit(12, SingleRackChannelId.FAILURE_INITIALIZATION)//
						), //
						m(SingleRackChannelId.SYSTEM_TIME_HIGH, new UnsignedWordElement(0x2186)), //
						m(SingleRackChannelId.SYSTEM_TIME_LOW, new UnsignedWordElement(0x2187)), //
						new DummyRegisterElement(0x2188, 0x218E), //
						m(SingleRackChannelId.LAST_TIME_CHARGE_CAPACITY_LOW_BITS, new UnsignedWordElement(0x218F),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(SingleRackChannelId.LAST_TIME_CHARGE_END_TIME_HIGH_BITS, new UnsignedWordElement(0x2190)), //
						m(SingleRackChannelId.LAST_TIME_CHARGE_END_TIME_LOW_BITS, new UnsignedWordElement(0x2191)), //
						new DummyRegisterElement(0x2192), //
						m(SingleRackChannelId.LAST_TIME_DISCHARGE_CAPACITY_LOW_BITS, new UnsignedWordElement(0x2193),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(SingleRackChannelId.LAST_TIME_DISCHARGE_END_TIME_HIGH_BITS, new UnsignedWordElement(0x2194)), //
						m(SingleRackChannelId.LAST_TIME_DISCHARGE_END_TIME_LOW_BITS, new UnsignedWordElement(0x2195)), //
						m(SingleRackChannelId.CELL_OVER_VOLTAGE_STOP_TIMES, new UnsignedWordElement(0x2196)), //
						m(SingleRackChannelId.BATTERY_OVER_VOLTAGE_STOP_TIMES, new UnsignedWordElement(0x2197)), //
						m(SingleRackChannelId.BATTERY_CHARGE_OVER_CURRENT_STOP_TIMES, new UnsignedWordElement(0x2198)), //
						m(SingleRackChannelId.CELL_VOLTAGE_LOW_STOP_TIMES, new UnsignedWordElement(0x2199)), //
						m(SingleRackChannelId.BATTERY_VOLTAGE_LOW_STOP_TIMES, new UnsignedWordElement(0x219A)), //
						m(SingleRackChannelId.BATTERY_DISCHARGE_OVER_CURRENT_STOP_TIMES,
								new UnsignedWordElement(0x219B)), //
						m(SingleRackChannelId.BATTERY_OVER_TEMPERATURE_STOP_TIMES, new UnsignedWordElement(0x219C)), //
						m(SingleRackChannelId.BATTERY_TEMPERATURE_LOW_STOP_TIMES, new UnsignedWordElement(0x219D)), //
						m(SingleRackChannelId.CELL_OVER_VOLTAGE_ALARM_TIMES, new UnsignedWordElement(0x219E)), //
						m(SingleRackChannelId.BATTERY_OVER_VOLTAGE_ALARM_TIMES, new UnsignedWordElement(0x219F)), //
						m(SingleRackChannelId.BATTERY_CHARGE_OVER_CURRENT_ALARM_TIMES, new UnsignedWordElement(0x21A0)), //
						m(SingleRackChannelId.CELL_VOLTAGE_LOW_ALARM_TIMES, new UnsignedWordElement(0x21A1)), //
						m(SingleRackChannelId.BATTERY_VOLTAGE_LOW_ALARM_TIMES, new UnsignedWordElement(0x21A2)), //
						m(SingleRackChannelId.BATTERY_DISCHARGE_OVER_CURRENT_ALARM_TIMES,
								new UnsignedWordElement(0x21A3)), //
						m(SingleRackChannelId.BATTERY_OVER_TEMPERATURE_ALARM_TIMES, new UnsignedWordElement(0x21A4)), //
						m(SingleRackChannelId.BATTERY_TEMPERATURE_LOW_ALARM_TIMES, new UnsignedWordElement(0x21A5)), //
						m(SingleRackChannelId.SYSTEM_SHORT_CIRCUIT_PROTECTION_TIMES, new UnsignedWordElement(0x21A6)), //
						m(SingleRackChannelId.SYSTEM_GR_OVER_TEMPERATURE_STOP_TIMES, new UnsignedWordElement(0x21A7)), //
						new DummyRegisterElement(0x21A8), //
						m(SingleRackChannelId.SYSTEM_GR_OVER_TEMPERATURE_ALARM_TIMES, new UnsignedWordElement(0x21A9)), //
						new DummyRegisterElement(0x21AA), //
						m(SingleRackChannelId.BATTERY_VOLTAGE_DIFFERENCE_ALARM_TIMES, new UnsignedWordElement(0x21AB)), //
						m(SingleRackChannelId.BATTERY_VOLTAGE_DIFFERENCE_STOP_TIMES, new UnsignedWordElement(0x21AC)), //
						new DummyRegisterElement(0x21AD, 0x21B3), //
						m(SingleRackChannelId.SLAVE_TEMPERATURE_COMMUNICATION_ERROR_HIGH,
								new UnsignedWordElement(0x21B4)), //
						m(SingleRackChannelId.SLAVE_TEMPERATURE_COMMUNICATION_ERROR_LOW,
								new UnsignedWordElement(0x21B5)) //
				) //
		); //

		if (!config.ReduceTasks()) {

			// Add tasks to read/write work and warn parameters
			// Stop parameter
			Task writeStopParameters = new FC16WriteRegistersTask(0x2040, //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2040)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2041)), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2042), ElementToChannelConverter.SCALE_FACTOR_2), // TODO
																										// Check if
																										// correct!
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2043),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x2044), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2045), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2046)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2048), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2049),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x204A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x204B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_PROTECTION,
							new UnsignedWordElement(0x204C)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
							new UnsignedWordElement(0x204D)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_PROTECTION,
							new UnsignedWordElement(0x204E)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
							new UnsignedWordElement(0x204F)), //
					m(SingleRackChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION, new UnsignedWordElement(0x2050)), //
					m(SingleRackChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER, new UnsignedWordElement(0x2051)), //
					m(SingleRackChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION, new UnsignedWordElement(0x2052)), //
					m(SingleRackChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION_RECOVER, new UnsignedWordElement(0x2053)), //
					m(SingleRackChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x2054)), //
					m(SingleRackChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2055)), //
					m(SingleRackChannelId.STOP_PARAMETER_INSULATION_PROTECTION, new UnsignedWordElement(0x2056)), //
					m(SingleRackChannelId.STOP_PARAMETER_INSULATION_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2057)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x2058)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2059)), //
					m(SingleRackChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x205A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x205C)), //
					m(SingleRackChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205D)), //
					m(SingleRackChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION,
							new UnsignedWordElement(0x205E)), //
					m(SingleRackChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205F)), //
					m(SingleRackChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x2060)), //
					m(SingleRackChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2061)) //
			);

//			//Warn parameter
			Task writeWarnParameters = new FC16WriteRegistersTask(0x2080, //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2080)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2081)), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2082),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2083),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x2084), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2085), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM, new UnsignedWordElement(0x2086)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2087)), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_ALARM, new UnsignedWordElement(0x2088),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2089),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x208A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x208B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_ALARM, new UnsignedWordElement(0x208C)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
							new UnsignedWordElement(0x208D)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_ALARM, new UnsignedWordElement(0x208E)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
							new UnsignedWordElement(0x208F)), //
					m(SingleRackChannelId.WARN_PARAMETER_SOC_LOW_ALARM, new UnsignedWordElement(0x2090)), //
					m(SingleRackChannelId.WARN_PARAMETER_SOC_LOW_ALARM_RECOVER, new UnsignedWordElement(0x2091)), //
					m(SingleRackChannelId.WARN_PARAMETER_SOC_HIGH_ALARM, new UnsignedWordElement(0x2092)), //
					m(SingleRackChannelId.WARN_PARAMETER_SOC_HIGH_ALARM_RECOVER, new UnsignedWordElement(0x2093)), //
					m(SingleRackChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM,
							new UnsignedWordElement(0x2094)), //
					m(SingleRackChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER,
							new UnsignedWordElement(0x2095)), //
					m(SingleRackChannelId.WARN_PARAMETER_INSULATION_ALARM, new UnsignedWordElement(0x2096)), //
					m(SingleRackChannelId.WARN_PARAMETER_INSULATION_ALARM_RECOVER, new UnsignedWordElement(0x2097)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM,
							new UnsignedWordElement(0x2098)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x2099)), //
					m(SingleRackChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM,
							new UnsignedWordElement(0x209A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x209B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM,
							new UnsignedWordElement(0x209C)), //
					m(SingleRackChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER,
							new UnsignedWordElement(0x209D)), //
					new DummyRegisterElement(0x209E),
					m(SingleRackChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM,
							new UnsignedWordElement(0x209F)), //
					m(SingleRackChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER,
							new UnsignedWordElement(0x20A0)), //
					m(SingleRackChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM, new UnsignedWordElement(0x20A1)), //
					m(SingleRackChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x20A2)) //
			);

			// Stop parameter
			Task readStopParameters = new FC3ReadRegistersTask(0x2040, Priority.LOW, //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION, new UnsignedWordElement(0x2040)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2041)), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2042), ElementToChannelConverter.SCALE_FACTOR_2), // TODO
																										// Check if
																										// correct!
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2043),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x2044), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2045), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2046)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_PROTECTION,
							new UnsignedWordElement(0x2048), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2049),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION,
							new UnsignedWordElement(0x204A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x204B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_PROTECTION,
							new UnsignedWordElement(0x204C)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
							new UnsignedWordElement(0x204D)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_PROTECTION,
							new UnsignedWordElement(0x204E)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
							new UnsignedWordElement(0x204F)), //
					m(SingleRackChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION, new UnsignedWordElement(0x2050)), //
					m(SingleRackChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER, new UnsignedWordElement(0x2051)), //
					m(SingleRackChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION, new UnsignedWordElement(0x2052)), //
					m(SingleRackChannelId.STOP_PARAMETER_SOC_HIGH_PROTECTION_RECOVER, new UnsignedWordElement(0x2053)), //
					m(SingleRackChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x2054)), //
					m(SingleRackChannelId.STOP_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2055)), //
					m(SingleRackChannelId.STOP_PARAMETER_INSULATION_PROTECTION, new UnsignedWordElement(0x2056)), //
					m(SingleRackChannelId.STOP_PARAMETER_INSULATION_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2057)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x2058)), //
					m(SingleRackChannelId.STOP_PARAMETER_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2059)), //
					m(SingleRackChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x205A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION,
							new UnsignedWordElement(0x205C)), //
					m(SingleRackChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205D)), //
					m(SingleRackChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION,
							new UnsignedWordElement(0x205E)), //
					m(SingleRackChannelId.STOP_PARAMETER_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER,
							new UnsignedWordElement(0x205F)), //
					m(SingleRackChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION,
							new UnsignedWordElement(0x2060)), //
					m(SingleRackChannelId.STOP_PARAMETER_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER,
							new UnsignedWordElement(0x2061)) //
			);

//			// Warn parameter
			Task readWarnParameters = new FC3ReadRegistersTask(0x2080, Priority.LOW, //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM, new UnsignedWordElement(0x2080)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2081)), //
					new DummyRegisterElement(0x2082),
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_OVER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2083),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x2084), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_CHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x2085), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM, new UnsignedWordElement(0x2086)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2087)), //
					new DummyRegisterElement(0x2088),
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_UNDER_VOLTAGE_RECOVER, new UnsignedWordElement(0x2089),
							ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM,
							new UnsignedWordElement(0x208A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER,
							new UnsignedWordElement(0x208B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_ALARM, new UnsignedWordElement(0x208C)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_OVER_TEMPERATURE_RECOVER,
							new UnsignedWordElement(0x208D)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_ALARM, new UnsignedWordElement(0x208E)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_UNDER_TEMPERATURE_RECOVER,
							new UnsignedWordElement(0x208F)), //
					m(SingleRackChannelId.WARN_PARAMETER_SOC_LOW_ALARM, new UnsignedWordElement(0x2090)), //
					m(SingleRackChannelId.WARN_PARAMETER_SOC_LOW_ALARM_RECOVER, new UnsignedWordElement(0x2091)), //
					m(SingleRackChannelId.WARN_PARAMETER_SOC_HIGH_ALARM, new UnsignedWordElement(0x2092)), //
					m(SingleRackChannelId.WARN_PARAMETER_SOC_HIGH_ALARM_RECOVER, new UnsignedWordElement(0x2093)), //
					m(SingleRackChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM,
							new UnsignedWordElement(0x2094)), //
					m(SingleRackChannelId.WARN_PARAMETER_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER,
							new UnsignedWordElement(0x2095)), //
					m(SingleRackChannelId.WARN_PARAMETER_INSULATION_ALARM, new UnsignedWordElement(0x2096)), //
					m(SingleRackChannelId.WARN_PARAMETER_INSULATION_ALARM_RECOVER, new UnsignedWordElement(0x2097)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM,
							new UnsignedWordElement(0x2098)), //
					m(SingleRackChannelId.WARN_PARAMETER_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x2099)), //
					m(SingleRackChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM,
							new UnsignedWordElement(0x209A), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER,
							new UnsignedWordElement(0x209B), ElementToChannelConverter.SCALE_FACTOR_2), //
					m(SingleRackChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM,
							new UnsignedWordElement(0x209C)), //
					m(SingleRackChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER,
							new UnsignedWordElement(0x209D)), //
					new DummyRegisterElement(0x209E),
					m(SingleRackChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM,
							new UnsignedWordElement(0x209F)), //
					m(SingleRackChannelId.WARN_PARAMETER_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER,
							new UnsignedWordElement(0x20A0)), //
					m(SingleRackChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM, new UnsignedWordElement(0x20A1)), //
					m(SingleRackChannelId.WARN_PARAMETER_TEMPERATURE_DIFFERENCE_ALARM_RECOVER,
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

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}	
}
