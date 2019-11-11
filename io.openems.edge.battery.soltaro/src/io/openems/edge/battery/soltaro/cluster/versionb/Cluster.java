package io.openems.edge.battery.soltaro.cluster.versionb;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
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
import io.openems.edge.battery.soltaro.BatteryState;
import io.openems.edge.battery.soltaro.ModuleParameters;
import io.openems.edge.battery.soltaro.ResetState;
import io.openems.edge.battery.soltaro.State;
import io.openems.edge.battery.soltaro.cluster.versionb.Enums.ContactorControl;
import io.openems.edge.battery.soltaro.cluster.versionb.Enums.RackUsage;
import io.openems.edge.battery.soltaro.cluster.versionb.Enums.StartStop;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.Soltaro.Cluster.VersionB", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class Cluster extends AbstractOpenemsModbusComponent
		implements Battery, OpenemsComponent, EventHandler, ModbusSlave {

	public static final int DISCHARGE_MAX_A = 0; // default value 0 to avoid damages
	public static final int CHARGE_MAX_A = 0; // default value 0 to avoid damages

	private static final int ADDRESS_OFFSET_RACK_1 = 0x2000;
	private static final int ADDRESS_OFFSET_RACK_2 = 0x3000;
	private static final int ADDRESS_OFFSET_RACK_3 = 0x4000;
	private static final int ADDRESS_OFFSET_RACK_4 = 0x5000;
	private static final int ADDRESS_OFFSET_RACK_5 = 0x6000;
	private static final int OFFSET_CONTACTOR_CONTROL = 0x10;

	// Helper that holds general information about single racks, independent if they
	// are used or not
	private static final Map<Integer, RackInfo> RACK_INFO = createRackInfo();
	private final Logger log = LoggerFactory.getLogger(Cluster.class);

	@Reference
	protected ConfigurationAdmin cm;

	// If an error has occurred, this indicates the time when next action could be
	// done
	private LocalDateTime errorDelayIsOver = null;
	private int unsuccessfulStarts = 0;
	private LocalDateTime startAttemptTime = null;
	private String modbusBridgeId;
	private BatteryState batteryState;
	private State state = State.UNDEFINED;
	private Config config;
	private Map<Integer, SingleRack> racks = new HashMap<>();
	// this timestamp is used to wait a certain time if system state could no be
	// determined at once
	private LocalDateTime pendingTimestamp;

	private ResetState resetState = ResetState.NONE;
	private boolean resetDone;

	public Cluster() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				ClusterChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		// Create racks dynamically, do this before super() call because super() uses
		// getModbusProtocol, and it is using racks...
		for (int i : config.racks()) {
			this.racks.put(i, new SingleRack(i, config.numberOfSlaves(), RACK_INFO.get(i).addressOffset, this));
		}

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());

		this.config = config;
		this.modbusBridgeId = config.modbus_id();
		this.batteryState = config.batteryState();

		this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).setNextValue(Cluster.CHARGE_MAX_A);
		this.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT).setNextValue(Cluster.DISCHARGE_MAX_A);
		this.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE)
				.setNextValue(this.config.numberOfSlaves() * ModuleParameters.MAX_VOLTAGE_MILLIVOLT.getValue() / 1000);
		this.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE)
				.setNextValue(this.config.numberOfSlaves() * ModuleParameters.MIN_VOLTAGE_MILLIVOLT.getValue() / 1000);
		this.channel(Battery.ChannelId.CAPACITY).setNextValue(
				this.config.racks().length * this.config.numberOfSlaves() * this.config.moduleType().getCapacity_Wh());
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
		switch (this.batteryState) {
		case DEFAULT:
			this.handleStateMachine();
			break;
		case OFF:
			this.stopSystem();
			break;
		case ON:
			this.startSystem();
			break;
		case CONFIGURE:
			this.logWarn(this.log, "Cluster cannot be configured currently!");
			break;
		}
	}

	private void handleStateMachine() {
		// log.info("Cluster.doNormalHandling(): State: " +
		// this.getStateMachineState());
		boolean readyForWorking = false;
		switch (this.getStateMachineState()) {
		case ERROR:
			stopSystem();
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
			this.startSystem();
			this.setStateMachineState(State.INIT);
			startAttemptTime = LocalDateTime.now();
			break;
		case RUNNING:
			if (this.isSystemRunning()) {
				if (this.isError()) {
					this.setStateMachineState(State.ERROR);
				} else {
					readyForWorking = true;
					this.setStateMachineState(State.RUNNING);
				}
			} else {
				this.setStateMachineState(State.UNDEFINED);
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

	private void handleErrorsWithReset() {
		// To reset the cell drift phenomenon, first sleep and then reset the system
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

	private boolean isError() {
		// still TODO define what is exactly an error
		if (this.readValueFromStateChannel(ClusterChannelId.MASTER_ALARM_LEVEL_2_INSULATION)) {
			return true;
		}
		if (this.readValueFromStateChannel(ClusterChannelId.MASTER_ALARM_PCS_EMS_CONTROL_FAIL)) {
			return true;
		}
		if (this.readValueFromStateChannel(ClusterChannelId.MASTER_ALARM_PCS_EMS_COMMUNICATION_FAILURE)) {
			return true;
		}
		if (this.readValueFromStateChannel(ClusterChannelId.MASTER_ALARM_COMMUNICATION_ERROR_WITH_SUBMASTER)) {
			return true;
		}

		// Check for communication errors
		for (int key : this.racks.keySet()) {
			if (this.readValueFromStateChannel(RACK_INFO.get(key).subMasterCommunicationAlarmChannelId)) {
				return true;
			}
		}

		return false;
	}

	protected Channel<?> addChannel(io.openems.edge.common.channel.ChannelId channelId) {
		return super.addChannel(channelId);
	}

	private boolean readValueFromStateChannel(io.openems.edge.common.channel.ChannelId channelId) {
		StateChannel s = this.channel(channelId);
		Optional<Boolean> val = s.value().asOptional();
		return val.isPresent() && val.get();
	}

	private boolean isSystemStopped() {
		return haveAllRacksTheSameContactorControlState(ContactorControl.CUT_OFF);
	}

	private boolean isSystemRunning() {
		return haveAllRacksTheSameContactorControlState(ContactorControl.ON_GRID);
	}

	private boolean haveAllRacksTheSameContactorControlState(ContactorControl cctrl) {
		boolean b = true;
		for (SingleRack r : this.racks.values()) {
			b = b && cctrl == this.channel(RACK_INFO.get(r.getRackNumber()).positiveContactorChannelId).value()
					.asEnum();
		}
		return b;
	}

	/**
	 * Checks whether system has an undefined state, e.g. rack 1 & 2 are configured,
	 * but only rack 1 is running. This state can only be reached at startup coming
	 * from state undefined
	 */
	private boolean isSystemStatePending() {
		boolean b = true;

		for (SingleRack r : this.racks.values()) {
			EnumReadChannel channel = this.channel(RACK_INFO.get(r.getRackNumber()).positiveContactorChannelId);
			Optional<Integer> val = channel.value().asOptional();
			b = b && val.isPresent();
		}

		return b && !isSystemRunning() && !isSystemStopped();
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value() //
				+ "|Discharge:" + this.getDischargeMinVoltage().value() + ";" + this.getDischargeMaxCurrent().value() //
				+ "|Charge:" + this.getChargeMaxVoltage().value() + ";" + this.getChargeMaxCurrent().value();
	}

	private void sleepSystem() {
		// Write sleep and reset to all racks
		for (SingleRack rack : this.racks.values()) {
			IntegerWriteChannel sleepChannel = (IntegerWriteChannel) rack.getChannel(SingleRack.KEY_SLEEP);
			try {
				sleepChannel.setNextWriteValue(0x1);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Error while trying to sleep the system!");
			}
			this.setStateMachineState(State.UNDEFINED);
		}
	}

	private void resetSystem() {
		// Write reset to all racks and the master

		IntegerWriteChannel resetMasterChannel = (IntegerWriteChannel) this.channel(ClusterChannelId.RESET);
		try {
			resetMasterChannel.setNextWriteValue(0x1);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Error while trying to reset the master!");
		}

		for (SingleRack rack : this.racks.values()) {
			IntegerWriteChannel resetChannel = (IntegerWriteChannel) rack.getChannel(SingleRack.KEY_RESET);
			try {
				resetChannel.setNextWriteValue(0x1);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Error while trying to reset the system!");
			}
		}
	}

	private void startSystem() {
		EnumWriteChannel startStopChannel = this.channel(ClusterChannelId.START_STOP);
		try {
			startStopChannel.setNextWriteValue(StartStop.START);
			// Only set the racks that are used, but set the others to unused
			for (int i : RACK_INFO.keySet()) {
				EnumWriteChannel rackUsageChannel = this.channel(RACK_INFO.get(i).usageChannelId);
				if (this.racks.containsKey(i)) {
					rackUsageChannel.setNextWriteValue(RackUsage.USED);
				} else {
					rackUsageChannel.setNextWriteValue(RackUsage.UNUSED);
				}
			}
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Error while trying to start system\n" + e.getMessage());
		}
	}

	private void stopSystem() {
		EnumWriteChannel startStopChannel = this.channel(ClusterChannelId.START_STOP);
		try {
			startStopChannel.setNextWriteValue(StartStop.STOP);
			// write to all racks unused!!
			for (RackInfo r : RACK_INFO.values()) {
				EnumWriteChannel rackUsageChannel = this.channel(r.usageChannelId);
				rackUsageChannel.setNextWriteValue(RackUsage.UNUSED);
			}
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Error while trying to stop system\n" + e.getMessage());
		}
	}

	public String getModbusBridgeId() {
		return modbusBridgeId;
	}

	public State getStateMachineState() {
		return state;
	}

	public void setStateMachineState(State state) {
		this.state = state;
		this.channel(ClusterChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		ModbusProtocol protocol = new ModbusProtocol(this, new Task[] {
				// -------- control registers of master --------------------------------------
				new FC16WriteRegistersTask(0x1004, //
						m(ClusterChannelId.RESET, new UnsignedWordElement(0x1004)) //
				), //

				new FC16WriteRegistersTask(0x1017, //
						m(ClusterChannelId.START_STOP, new UnsignedWordElement(0x1017)), //
						m(ClusterChannelId.RACK_1_USAGE, new UnsignedWordElement(0x1018)), //
						m(ClusterChannelId.RACK_2_USAGE, new UnsignedWordElement(0x1019)), //
						m(ClusterChannelId.RACK_3_USAGE, new UnsignedWordElement(0x101A)), //
						m(ClusterChannelId.RACK_4_USAGE, new UnsignedWordElement(0x101B)), //
						m(ClusterChannelId.RACK_5_USAGE, new UnsignedWordElement(0x101C)) //
				), //
				new FC3ReadRegistersTask(0x1017, Priority.HIGH,
						m(ClusterChannelId.START_STOP, new UnsignedWordElement(0x1017)), //
						m(ClusterChannelId.RACK_1_USAGE, new UnsignedWordElement(0x1018)), //
						m(ClusterChannelId.RACK_2_USAGE, new UnsignedWordElement(0x1019)), //
						m(ClusterChannelId.RACK_3_USAGE, new UnsignedWordElement(0x101A)), //
						m(ClusterChannelId.RACK_4_USAGE, new UnsignedWordElement(0x101B)), //
						m(ClusterChannelId.RACK_5_USAGE, new UnsignedWordElement(0x101C)) //
				), //

				new FC16WriteRegistersTask(0x101F,
						m(ClusterChannelId.SYSTEM_INSULATION_LEVEL_1, new UnsignedWordElement(0x101F)), //
						m(ClusterChannelId.SYSTEM_INSULATION_LEVEL_2, new UnsignedWordElement(0x1020)), //
						new DummyRegisterElement(0x1021), //
						m(ClusterChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x1022)), //
						m(ClusterChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1023)) //
				), //

				new FC3ReadRegistersTask(0x101F, Priority.LOW,
						m(ClusterChannelId.SYSTEM_INSULATION_LEVEL_1, new UnsignedWordElement(0x101F)), //
						m(ClusterChannelId.SYSTEM_INSULATION_LEVEL_2, new UnsignedWordElement(0x1020)), //
						new DummyRegisterElement(0x1021), //
						m(ClusterChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x1022)), //
						m(ClusterChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1023)) //
				), //

				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1),
						m(ClusterChannelId.RACK_1_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1))) //
				), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1), Priority.HIGH,
						m(ClusterChannelId.RACK_1_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1))) //
				), //

				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2),
						m(ClusterChannelId.RACK_2_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2))) //
				), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2), Priority.HIGH,
						m(ClusterChannelId.RACK_2_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2))) //
				), //

				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3),
						m(ClusterChannelId.RACK_2_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3))) //
				), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3), Priority.HIGH,
						m(ClusterChannelId.RACK_3_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3))) //
				), //

				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4),
						m(ClusterChannelId.RACK_4_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4))) //
				), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4), Priority.HIGH,
						m(ClusterChannelId.RACK_4_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4))) //
				), //

				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5),
						m(ClusterChannelId.RACK_5_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5))) //
				), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5), Priority.HIGH,
						m(ClusterChannelId.RACK_5_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5))) //
				), //

				// -------- state registers of master --------------------------------------
				new FC3ReadRegistersTask(0x1044, Priority.LOW, //
						m(ClusterChannelId.CHARGE_INDICATION, new UnsignedWordElement(0x1044)), //
						m(ClusterChannelId.SYSTEM_CURRENT, new UnsignedWordElement(0x1045), //
								ElementToChannelConverter.SCALE_FACTOR_2), // TODO Check if scale factor is correct
						new DummyRegisterElement(0x1046), //
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x1047)) //
								.onUpdateCallback(val -> {
									recalculateSoc();
								}), //
						m(ClusterChannelId.SYSTEM_RUNNING_STATE, new UnsignedWordElement(0x1048)), //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(0x1049), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC3ReadRegistersTask(0x104A, Priority.HIGH, //
						m(ClusterChannelId.SYSTEM_INSULATION, new UnsignedWordElement(0x104A)), //
						new DummyRegisterElement(0x104B, 0x104D), //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(0x104E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(0x104F),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), //

				new FC3ReadRegistersTask(0x1081, Priority.LOW, //
						m(new BitsWordElement(0x1081, this) //
								.bit(4, ClusterChannelId.MASTER_ALARM_LEVEL_2_INSULATION) //
								.bit(3, ClusterChannelId.MASTER_ALARM_LEVEL_1_INSULATION) //
								.bit(2, ClusterChannelId.MASTER_ALARM_PCS_EMS_CONTROL_FAIL) //
								.bit(1, ClusterChannelId.MASTER_ALARM_PCS_EMS_COMMUNICATION_FAILURE) //
								.bit(0, ClusterChannelId.MASTER_ALARM_COMMUNICATION_ERROR_WITH_SUBMASTER) //
						), //
						m(new BitsWordElement(0x1082, this) //
								.bit(4, ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_5) //
								.bit(3, ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_4) //
								.bit(2, ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_3) //
								.bit(1, ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_2) //
								.bit(0, ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_1) //
						), //
						m(new BitsWordElement(0x1083, this) //
								.bit(5, ClusterChannelId.RACK_1_LEVEL_2_ALARM) //
								.bit(4, ClusterChannelId.RACK_1_PCS_CONTROL_FAULT) //
								.bit(3, ClusterChannelId.RACK_1_COMMUNICATION_WITH_MASTER_ERROR) //
								.bit(2, ClusterChannelId.RACK_1_DEVICE_ERROR) //
								.bit(1, ClusterChannelId.RACK_1_CYCLE_OVER_CURRENT) //
								.bit(0, ClusterChannelId.RACK_1_VOLTAGE_DIFFERENCE) //
						), //
						m(new BitsWordElement(0x1084, this) //
								.bit(5, ClusterChannelId.RACK_2_LEVEL_2_ALARM) //
								.bit(4, ClusterChannelId.RACK_2_PCS_CONTROL_FAULT) //
								.bit(3, ClusterChannelId.RACK_2_COMMUNICATION_WITH_MASTER_ERROR) //
								.bit(2, ClusterChannelId.RACK_2_DEVICE_ERROR) //
								.bit(1, ClusterChannelId.RACK_2_CYCLE_OVER_CURRENT) //
								.bit(0, ClusterChannelId.RACK_2_VOLTAGE_DIFFERENCE) //
						), //
						m(new BitsWordElement(0x1085, this) //
								.bit(5, ClusterChannelId.RACK_3_LEVEL_2_ALARM) //
								.bit(4, ClusterChannelId.RACK_3_PCS_CONTROL_FAULT) //
								.bit(3, ClusterChannelId.RACK_3_COMMUNICATION_WITH_MASTER_ERROR) //
								.bit(2, ClusterChannelId.RACK_3_DEVICE_ERROR) //
								.bit(1, ClusterChannelId.RACK_3_CYCLE_OVER_CURRENT) //
								.bit(0, ClusterChannelId.RACK_3_VOLTAGE_DIFFERENCE) //
						), //
						m(new BitsWordElement(0x1086, this) //
								.bit(5, ClusterChannelId.RACK_4_LEVEL_2_ALARM) //
								.bit(4, ClusterChannelId.RACK_4_PCS_CONTROL_FAULT) //
								.bit(3, ClusterChannelId.RACK_4_COMMUNICATION_WITH_MASTER_ERROR) //
								.bit(2, ClusterChannelId.RACK_4_DEVICE_ERROR) //
								.bit(1, ClusterChannelId.RACK_4_CYCLE_OVER_CURRENT) //
								.bit(0, ClusterChannelId.RACK_4_VOLTAGE_DIFFERENCE) //
						), //
						m(new BitsWordElement(0x1087, this) //
								.bit(5, ClusterChannelId.RACK_5_LEVEL_2_ALARM) //
								.bit(4, ClusterChannelId.RACK_5_PCS_CONTROL_FAULT) //
								.bit(3, ClusterChannelId.RACK_5_COMMUNICATION_WITH_MASTER_ERROR) //
								.bit(2, ClusterChannelId.RACK_5_DEVICE_ERROR) //
								.bit(1, ClusterChannelId.RACK_5_CYCLE_OVER_CURRENT) //
								.bit(0, ClusterChannelId.RACK_5_VOLTAGE_DIFFERENCE) //
						) //
				) //

		});

		for (SingleRack rack : this.racks.values()) {
			protocol.addTasks(rack.getTasks().toArray(new Task[] {}));
		}

		return protocol;
	}

	private int getAddressContactorControl(int addressOffsetRack) {
		return addressOffsetRack + OFFSET_CONTACTOR_CONTROL;
	}

	protected final AbstractModbusElement<?> map(io.openems.edge.common.channel.ChannelId channelId,
			AbstractModbusElement<?> element) {
		return this.m(channelId, element);
	}

	protected final AbstractModbusElement<?> map(io.openems.edge.common.channel.ChannelId channelId,
			AbstractModbusElement<?> element, ElementToChannelConverter converter) {
		return this.m(channelId, element, converter);
	}

	protected final AbstractModbusElement<?> map(BitsWordElement bitsWordElement) {
		return super.m(bitsWordElement);
	}

	protected void recalculateSoc() {
		int i = 0;
		int soc = 0;

		for (SingleRack rack : this.racks.values()) {
			this.logInfo(this.log, "Rack " + rack.getRackNumber() + " has a SoC of " + rack.getSoC() + " %!");
			soc = soc + rack.getSoC();
			i++;
		}

		if (i > 0) {
			soc = soc / i;
		}

		// this.log.info("Calculated SoC: " + soc);

		this.channel(Battery.ChannelId.SOC).setNextValue(soc);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	private static Map<Integer, RackInfo> createRackInfo() {
		Map<Integer, RackInfo> map = new HashMap<Integer, RackInfo>();
		map.put(1,
				new RackInfo(ADDRESS_OFFSET_RACK_1, ClusterChannelId.RACK_1_USAGE,
						ClusterChannelId.RACK_1_POSITIVE_CONTACTOR,
						ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_1));
		map.put(2,
				new RackInfo(ADDRESS_OFFSET_RACK_2, ClusterChannelId.RACK_2_USAGE,
						ClusterChannelId.RACK_2_POSITIVE_CONTACTOR,
						ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_2));
		map.put(3,
				new RackInfo(ADDRESS_OFFSET_RACK_3, ClusterChannelId.RACK_3_USAGE,
						ClusterChannelId.RACK_3_POSITIVE_CONTACTOR,
						ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_3));
		map.put(4,
				new RackInfo(ADDRESS_OFFSET_RACK_4, ClusterChannelId.RACK_4_USAGE,
						ClusterChannelId.RACK_4_POSITIVE_CONTACTOR,
						ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_4));
		map.put(5,
				new RackInfo(ADDRESS_OFFSET_RACK_5, ClusterChannelId.RACK_5_USAGE,
						ClusterChannelId.RACK_5_POSITIVE_CONTACTOR,
						ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_5));

		return map;
	}

	// Helper class to get infos about connected racks
	private static class RackInfo {
		int addressOffset;
		ClusterChannelId usageChannelId;
		ClusterChannelId positiveContactorChannelId;
		ClusterChannelId subMasterCommunicationAlarmChannelId;

		RackInfo( //
				int addressOffset, //
				ClusterChannelId usageChannelId, //
				ClusterChannelId positiveContactorChannelId, //
				ClusterChannelId subMasterCommunicationAlarmChannelId //
		) {
			this.addressOffset = addressOffset;
			this.usageChannelId = usageChannelId;
			this.subMasterCommunicationAlarmChannelId = subMasterCommunicationAlarmChannelId;
			this.positiveContactorChannelId = positiveContactorChannelId;
		}
	}
}
