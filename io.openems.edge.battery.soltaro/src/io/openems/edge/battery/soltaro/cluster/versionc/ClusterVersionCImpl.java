package io.openems.edge.battery.soltaro.cluster.versionc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

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
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.BatteryState;
import io.openems.edge.battery.soltaro.ResetState;
import io.openems.edge.battery.soltaro.SoltaroBattery;
import io.openems.edge.battery.soltaro.State;
import io.openems.edge.battery.soltaro.cluster.SoltaroCluster;
import io.openems.edge.battery.soltaro.cluster.enums.RackInfo;
import io.openems.edge.battery.soltaro.versionc.SoltaroBatteryVersionC;
import io.openems.edge.battery.soltaro.versionc.utils.CellChannelFactory;
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
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.Soltaro.Cluster.VersionC", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class ClusterVersionCImpl extends AbstractOpenemsModbusComponent
		implements ClusterVersionC, SoltaroBattery, Battery, OpenemsComponent, EventHandler, ModbusSlave {

	public static final int DISCHARGE_MAX_A = 0; // default value 0 to avoid damages
	public static final int CHARGE_MAX_A = 0; // default value 0 to avoid damages

	private final Logger log = LoggerFactory.getLogger(ClusterVersionCImpl.class);

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
	private Set<RackInfo> racks = new HashSet<>();
	// this timestamp is used to wait a certain time if system state could no be
	// determined at once
	private LocalDateTime pendingTimestamp;

	private ResetState resetState = ResetState.NONE;
	private boolean resetDone;

	public ClusterVersionCImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				SoltaroBattery.ChannelId.values(), //
				SoltaroBatteryVersionC.ChannelId.values(), //
				ClusterVersionC.ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		// Initialize active racks
		if (config.isRack1Used()) {
			this.racks.add(RackInfo.RACK_1);
		}
		if (config.isRack2Used()) {
			this.racks.add(RackInfo.RACK_2);
		}
		if (config.isRack3Used()) {
			this.racks.add(RackInfo.RACK_3);
		}
		if (config.isRack4Used()) {
			this.racks.add(RackInfo.RACK_4);
		}
		if (config.isRack5Used()) {
			this.racks.add(RackInfo.RACK_5);
		}

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());

		this.config = config;
//		this.modbusBridgeId = config.modbus_id();
//		this.batteryState = config.batteryState();
//
//		this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).setNextValue(ClusterVersionCImpl.CHARGE_MAX_A);
//		this.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT).setNextValue(ClusterVersionCImpl.DISCHARGE_MAX_A);
//		this.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE)
//				.setNextValue(this.config.numberOfSlaves() * ModuleParameters.MAX_VOLTAGE_MILLIVOLT.getValue() / 1000);
//		this.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE)
//				.setNextValue(this.config.numberOfSlaves() * ModuleParameters.MIN_VOLTAGE_MILLIVOLT.getValue() / 1000);
//		this.channel(Battery.ChannelId.CAPACITY).setNextValue(
//				this.config.racks().length * this.config.numberOfSlaves() * this.config.moduleType().getCapacity_Wh());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.updateChannels();
//			this.handleBatteryState();
			break;
		}
	}

	/**
	 * Updates Channels on AFTER_PROCESS_IMAGE event.
	 */
	private void updateChannels() {
//		this.recalculateSoc();
	}

//	private void handleBatteryState() {
//		switch (this.batteryState) {
//		case DEFAULT:
//			this.handleStateMachine();
//			break;
//		case OFF:
//			this.stopSystem();
//			break;
//		case ON:
//			this.startSystem();
//			break;
//		case CONFIGURE:
//			this.logWarn(this.log, "Cluster cannot be configured currently!");
//			break;
//		}
//	}
//
//	private void handleStateMachine() {
//		// log.info("Cluster.doNormalHandling(): State: " +
//		// this.getStateMachineState());
//		boolean readyForWorking = false;
//		switch (this.getStateMachineState()) {
//		case ERROR:
//			stopSystem();
//			errorDelayIsOver = LocalDateTime.now().plusSeconds(config.errorLevel2Delay());
//			setStateMachineState(State.ERRORDELAY);
//			break;
//		case ERRORDELAY:
//			// If we are in the error delay time, the system is resetted, this can help
//			// handling the rrors
//			if (LocalDateTime.now().isAfter(errorDelayIsOver)) {
//				errorDelayIsOver = null;
//				resetDone = false;
//				if (this.isError()) {
//					this.setStateMachineState(State.ERROR);
//				} else {
//					this.setStateMachineState(State.UNDEFINED);
//				}
//			} else if (!resetDone) {
//				this.handleErrorsWithReset();
//			}
//			break;
//
//		case INIT:
//			if (this.isSystemRunning()) {
//				this.setStateMachineState(State.RUNNING);
//				unsuccessfulStarts = 0;
//				startAttemptTime = null;
//			} else {
//				if (startAttemptTime.plusSeconds(config.maxStartTime()).isBefore(LocalDateTime.now())) {
//					startAttemptTime = null;
//					unsuccessfulStarts++;
//					this.stopSystem();
//					this.setStateMachineState(State.STOPPING);
//					if (unsuccessfulStarts >= config.maxStartAppempts()) {
//						errorDelayIsOver = LocalDateTime.now().plusSeconds(config.startUnsuccessfulDelay());
//						this.setStateMachineState(State.ERRORDELAY);
//						unsuccessfulStarts = 0;
//					}
//				}
//			}
//			break;
//		case OFF:
//			this.startSystem();
//			this.setStateMachineState(State.INIT);
//			startAttemptTime = LocalDateTime.now();
//			break;
//		case RUNNING:
//			if (this.isSystemRunning()) {
//				if (this.isError()) {
//					this.setStateMachineState(State.ERROR);
//				} else {
//					readyForWorking = true;
//					this.setStateMachineState(State.RUNNING);
//				}
//			} else {
//				this.setStateMachineState(State.UNDEFINED);
//			}
//			break;
//		case STOPPING:
//			if (this.isError()) {
//				this.setStateMachineState(State.ERROR);
//			} else {
//				if (this.isSystemStopped()) {
//					this.setStateMachineState(State.OFF);
//				}
//			}
//			break;
//		case UNDEFINED:
//			if (this.isError()) {
//				this.setStateMachineState(State.ERROR);
//			} else if (this.isSystemStopped()) {
//				this.setStateMachineState(State.OFF);
//			} else if (this.isSystemRunning()) {
//				this.setStateMachineState(State.RUNNING);
//			} else if (this.isSystemStatePending()) {
//				this.setStateMachineState(State.PENDING);
//			}
//			break;
//		case PENDING:
//			if (this.pendingTimestamp == null) {
//				this.pendingTimestamp = LocalDateTime.now();
//			}
//			if (this.pendingTimestamp.plusSeconds(this.config.pendingTolerance()).isBefore(LocalDateTime.now())) {
//				// System state could not be determined, stop and start it
//				this.pendingTimestamp = null;
//				this.stopSystem();
//				this.setStateMachineState(State.OFF);
//			} else {
//				if (this.isError()) {
//					this.setStateMachineState(State.ERROR);
//					this.pendingTimestamp = null;
//				} else if (this.isSystemStopped()) {
//					this.setStateMachineState(State.OFF);
//					this.pendingTimestamp = null;
//				} else if (this.isSystemRunning()) {
//					this.setStateMachineState(State.RUNNING);
//					this.pendingTimestamp = null;
//				}
//			}
//			break;
//		case ERROR_HANDLING:
//			this.handleErrorsWithReset();
//			break;
//		}
//		this.getReadyForWorking().setNextValue(readyForWorking);
//	}
//
//	private void handleErrorsWithReset() {
//		// To reset the cell drift phenomenon, first sleep and then reset the system
//		switch (this.resetState) {
//		case NONE:
//			this.resetState = ResetState.SLEEP;
//			break;
//		case SLEEP:
//			this.sleepSystem();
//			this.resetState = ResetState.RESET;
//			break;
//		case RESET:
//			this.resetSystem();
//			this.resetState = ResetState.FINISHED;
//			break;
//		case FINISHED:
//			this.resetState = ResetState.NONE;
//			this.setStateMachineState(State.ERRORDELAY);
//			resetDone = true;
//			break;
//		}
//	}
//
//	private boolean isError() {
//		// still TODO define what is exactly an error
//		if (this.readValueFromStateChannel(ClusterChannelId.MASTER_ALARM_LEVEL_2_INSULATION)) {
//			return true;
//		}
//		if (this.readValueFromStateChannel(ClusterChannelId.MASTER_ALARM_PCS_EMS_CONTROL_FAIL)) {
//			return true;
//		}
//		if (this.readValueFromStateChannel(ClusterChannelId.MASTER_ALARM_PCS_EMS_COMMUNICATION_FAILURE)) {
//			return true;
//		}
//		if (this.readValueFromStateChannel(ClusterChannelId.MASTER_ALARM_COMMUNICATION_ERROR_WITH_SUBMASTER)) {
//			return true;
//		}
//
//		// Check for communication errors
//		for (int key : this.racks.keySet()) {
//			if (this.readValueFromStateChannel(RACK_INFO.get(key).subMasterCommunicationAlarmChannelId)) {
//				return true;
//			}
//		}
//
//		return false;
//	}
//
//	protected Channel<?> addChannel(io.openems.edge.common.channel.ChannelId channelId) {
//		return super.addChannel(channelId);
//	}
//
//	private boolean readValueFromStateChannel(io.openems.edge.common.channel.ChannelId channelId) {
//		StateChannel s = this.channel(channelId);
//		Optional<Boolean> val = s.value().asOptional();
//		return val.isPresent() && val.get();
//	}
//
//	private boolean isSystemStopped() {
//		return haveAllRacksTheSameContactorControlState(ContactorControl.CUT_OFF);
//	}
//
//	private boolean isSystemRunning() {
//		return haveAllRacksTheSameContactorControlState(ContactorControl.ON_GRID);
//	}
//
//	private boolean haveAllRacksTheSameContactorControlState(ContactorControl cctrl) {
//		boolean b = true;
//		for (SingleRack r : this.racks.values()) {
//			b = b && cctrl == this.channel(RACK_INFO.get(r.getRackNumber()).positiveContactorChannelId).value()
//					.asEnum();
//		}
//		return b;
//	}
//
//	/**
//	 * Checks whether system has an undefined state, e.g. rack 1 & 2 are configured,
//	 * but only rack 1 is running. This state can only be reached at startup coming
//	 * from state undefined
//	 */
//	private boolean isSystemStatePending() {
//		boolean b = true;
//
//		for (SingleRack r : this.racks.values()) {
//			EnumReadChannel channel = this.channel(RACK_INFO.get(r.getRackNumber()).positiveContactorChannelId);
//			Optional<Integer> val = channel.value().asOptional();
//			b = b && val.isPresent();
//		}
//
//		return b && !isSystemRunning() && !isSystemStopped();
//	}
//
//	@Override
//	public String debugLog() {
//		return "SoC:" + this.getSoc().value() //
//				+ "|Discharge:" + this.getDischargeMinVoltage().value() + ";" + this.getDischargeMaxCurrent().value() //
//				+ "|Charge:" + this.getChargeMaxVoltage().value() + ";" + this.getChargeMaxCurrent().value();
//	}
//
//	private void sleepSystem() {
//		// Write sleep and reset to all racks
//		for (SingleRack rack : this.racks.values()) {
//			IntegerWriteChannel sleepChannel = (IntegerWriteChannel) rack.getChannel(SingleRack.KEY_SLEEP);
//			try {
//				sleepChannel.setNextWriteValue(0x1);
//			} catch (OpenemsNamedException e) {
//				this.logError(this.log, "Error while trying to sleep the system!");
//			}
//			this.setStateMachineState(State.UNDEFINED);
//		}
//	}
//
//	private void resetSystem() {
//		// Write reset to all racks and the master
//
//		IntegerWriteChannel resetMasterChannel = (IntegerWriteChannel) this.channel(ClusterChannelId.RESET);
//		try {
//			resetMasterChannel.setNextWriteValue(0x1);
//		} catch (OpenemsNamedException e) {
//			this.logError(this.log, "Error while trying to reset the master!");
//		}
//
//		for (SingleRack rack : this.racks.values()) {
//			IntegerWriteChannel resetChannel = (IntegerWriteChannel) rack.getChannel(SingleRack.KEY_RESET);
//			try {
//				resetChannel.setNextWriteValue(0x1);
//			} catch (OpenemsNamedException e) {
//				this.logError(this.log, "Error while trying to reset the system!");
//			}
//		}
//	}
//
//	private void startSystem() {
//		EnumWriteChannel startStopChannel = this.channel(ClusterChannelId.START_STOP);
//		try {
//			startStopChannel.setNextWriteValue(StartStop.START);
//			// Only set the racks that are used, but set the others to unused
//			for (int i : RACK_INFO.keySet()) {
//				EnumWriteChannel rackUsageChannel = this.channel(RACK_INFO.get(i).usageChannelId);
//				if (this.racks.containsKey(i)) {
//					rackUsageChannel.setNextWriteValue(RackUsage.USED);
//				} else {
//					rackUsageChannel.setNextWriteValue(RackUsage.UNUSED);
//				}
//			}
//		} catch (OpenemsNamedException e) {
//			this.logError(this.log, "Error while trying to start system\n" + e.getMessage());
//		}
//	}
//
//	private void stopSystem() {
//		EnumWriteChannel startStopChannel = this.channel(ClusterChannelId.START_STOP);
//		try {
//			startStopChannel.setNextWriteValue(StartStop.STOP);
//			// write to all racks unused!!
//			for (RackInfo r : RACK_INFO.values()) {
//				EnumWriteChannel rackUsageChannel = this.channel(r.usageChannelId);
//				rackUsageChannel.setNextWriteValue(RackUsage.UNUSED);
//			}
//		} catch (OpenemsNamedException e) {
//			this.logError(this.log, "Error while trying to stop system\n" + e.getMessage());
//		}
//	}
//
//	public String getModbusBridgeId() {
//		return modbusBridgeId;
//	}
//
//	public State getStateMachineState() {
//		return state;
//	}
//
//	public void setStateMachineState(State state) {
//		this.state = state;
//		this.channel(ClusterChannelId.STATE_MACHINE).setNextValue(this.state);
//	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		ModbusProtocol protocol = new ModbusProtocol(this,
				/*
				 * BMS Control Registers
				 */
				new FC16WriteRegistersTask(0x1024,
						m(SoltaroBatteryVersionC.ChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x1024)), //
						m(SoltaroBatteryVersionC.ChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1025)), //
						m(SoltaroBatteryVersionC.ChannelId.EMS_BAUDRATE, new UnsignedWordElement(0x1026)) //
				), //
				new FC3ReadRegistersTask(0x1024, Priority.LOW,
						m(SoltaroBatteryVersionC.ChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x1024)), //
						m(SoltaroBatteryVersionC.ChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1025)), //
						m(SoltaroBatteryVersionC.ChannelId.EMS_BAUDRATE, new UnsignedWordElement(0x1026)) //
				), //
				new FC16WriteRegistersTask(0x10C3, //
						m(SoltaroCluster.ChannelId.START_STOP, new UnsignedWordElement(0x10C3)), //
						m(SoltaroCluster.ChannelId.RACK_1_USAGE, new UnsignedWordElement(0x10C4)), //
						m(SoltaroCluster.ChannelId.RACK_2_USAGE, new UnsignedWordElement(0x10C5)), //
						m(SoltaroCluster.ChannelId.RACK_3_USAGE, new UnsignedWordElement(0x10C6)), //
						m(SoltaroCluster.ChannelId.RACK_4_USAGE, new UnsignedWordElement(0x10C7)), //
						m(SoltaroCluster.ChannelId.RACK_5_USAGE, new UnsignedWordElement(0x10C8)) //
				), //
				new FC3ReadRegistersTask(0x10C3, Priority.LOW,
						m(SoltaroCluster.ChannelId.START_STOP, new UnsignedWordElement(0x10C3)), //
						m(SoltaroCluster.ChannelId.RACK_1_USAGE, new UnsignedWordElement(0x10C4)), //
						m(SoltaroCluster.ChannelId.RACK_2_USAGE, new UnsignedWordElement(0x10C5)), //
						m(SoltaroCluster.ChannelId.RACK_3_USAGE, new UnsignedWordElement(0x10C6)), //
						m(SoltaroCluster.ChannelId.RACK_4_USAGE, new UnsignedWordElement(0x10C7)), //
						m(SoltaroCluster.ChannelId.RACK_5_USAGE, new UnsignedWordElement(0x10C8)) //
				), //
				new FC16WriteRegistersTask(0x2010, //
						m(SoltaroCluster.ChannelId.RACK_1_POSITIVE_CONTACTOR, new UnsignedWordElement(0x2010)) //
				), //
				new FC3ReadRegistersTask(0x2010, Priority.LOW, //
						m(SoltaroCluster.ChannelId.RACK_1_POSITIVE_CONTACTOR, new UnsignedWordElement(0x2010)) //
				), //
				new FC16WriteRegistersTask(0x3010, //
						m(SoltaroCluster.ChannelId.RACK_2_POSITIVE_CONTACTOR, new UnsignedWordElement(0x3010)) //
				), //
				new FC3ReadRegistersTask(0x3010, Priority.LOW, //
						m(SoltaroCluster.ChannelId.RACK_2_POSITIVE_CONTACTOR, new UnsignedWordElement(0x3010)) //
				), //
				new FC16WriteRegistersTask(0x4010, //
						m(SoltaroCluster.ChannelId.RACK_3_POSITIVE_CONTACTOR, new UnsignedWordElement(0x4010)) //
				), //
				new FC3ReadRegistersTask(0x4010, Priority.LOW, //
						m(SoltaroCluster.ChannelId.RACK_3_POSITIVE_CONTACTOR, new UnsignedWordElement(0x4010)) //
				), //
				new FC16WriteRegistersTask(0x5010, //
						m(SoltaroCluster.ChannelId.RACK_4_POSITIVE_CONTACTOR, new UnsignedWordElement(0x5010)) //
				), //
				new FC3ReadRegistersTask(0x5010, Priority.LOW, //
						m(SoltaroCluster.ChannelId.RACK_4_POSITIVE_CONTACTOR, new UnsignedWordElement(0x5010)) //
				), //
				new FC16WriteRegistersTask(0x6010, //
						m(SoltaroCluster.ChannelId.RACK_5_POSITIVE_CONTACTOR, new UnsignedWordElement(0x6010)) //
				), //
				new FC3ReadRegistersTask(0x6010, Priority.LOW, //
						m(SoltaroCluster.ChannelId.RACK_5_POSITIVE_CONTACTOR, new UnsignedWordElement(0x6010)) //
				), //
				/*
				 * BMS System Running Status Registers
				 */
				new FC3ReadRegistersTask(0x1044, Priority.LOW, //
						m(SoltaroBattery.ChannelId.CHARGE_INDICATION, new UnsignedWordElement(0x1044)), //
						m(SoltaroCluster.ChannelId.SYSTEM_CURRENT, new UnsignedWordElement(0x1045), //
								ElementToChannelConverter.SCALE_FACTOR_2),
						new DummyRegisterElement(0x1046), //
						m(ClusterVersionC.ChannelId.ORIGINAL_SOC, new UnsignedWordElement(0x1047)), //
						m(SoltaroCluster.ChannelId.SYSTEM_RUNNING_STATE, new UnsignedWordElement(0x1048)), //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(0x1049), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SoltaroCluster.ChannelId.SYSTEM_INSULATION, new UnsignedWordElement(0x104A)), //
						new DummyRegisterElement(0x104B, 0x104D), //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(0x104E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(0x104F),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC3ReadRegistersTask(0x1081, Priority.LOW, //
						m(new BitsWordElement(0x1081, this) //
								.bit(0, ClusterVersionC.ChannelId.MASTER_PCS_COMMUNICATION_FAILURE) //
								.bit(1, ClusterVersionC.ChannelId.MASTER_PCS_CONTROL_FAILURE) //
								.bit(2, ClusterVersionC.ChannelId.MASTER_EMS_COMMUNICATION_FAILURE) //
						), //
						m(new BitsWordElement(0x1082, this) //
								.bit(0, SoltaroCluster.ChannelId.SUB_MASTER_1_COMMUNICATION_FAILURE) //
								.bit(1, SoltaroCluster.ChannelId.SUB_MASTER_2_COMMUNICATION_FAILURE) //
								.bit(2, SoltaroCluster.ChannelId.SUB_MASTER_3_COMMUNICATION_FAILURE) //
								.bit(3, SoltaroCluster.ChannelId.SUB_MASTER_4_COMMUNICATION_FAILURE) //
								.bit(4, SoltaroCluster.ChannelId.SUB_MASTER_5_COMMUNICATION_FAILURE) //
						), //
						m(new BitsWordElement(0x1083, this) //
								.bit(0, ClusterVersionC.ChannelId.RACK_1_VOLTAGE_DIFFERENCE) //
								.bit(1, ClusterVersionC.ChannelId.RACK_1_OVER_CURRENT) //
								.bit(2, ClusterVersionC.ChannelId.RACK_1_HARDWARE_FAILURE) //
								.bit(3, ClusterVersionC.ChannelId.RACK_1_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, ClusterVersionC.ChannelId.RACK_1_PCS_CONTROL_FAILURE) //
								.bit(5, ClusterVersionC.ChannelId.RACK_1_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1084, this) //
								.bit(0, ClusterVersionC.ChannelId.RACK_2_VOLTAGE_DIFFERENCE) //
								.bit(1, ClusterVersionC.ChannelId.RACK_2_OVER_CURRENT) //
								.bit(2, ClusterVersionC.ChannelId.RACK_2_HARDWARE_FAILURE) //
								.bit(3, ClusterVersionC.ChannelId.RACK_2_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, ClusterVersionC.ChannelId.RACK_2_PCS_CONTROL_FAILURE) //
								.bit(5, ClusterVersionC.ChannelId.RACK_2_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1085, this) //
								.bit(0, ClusterVersionC.ChannelId.RACK_3_VOLTAGE_DIFFERENCE) //
								.bit(1, ClusterVersionC.ChannelId.RACK_3_OVER_CURRENT) //
								.bit(2, ClusterVersionC.ChannelId.RACK_3_HARDWARE_FAILURE) //
								.bit(3, ClusterVersionC.ChannelId.RACK_3_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, ClusterVersionC.ChannelId.RACK_3_PCS_CONTROL_FAILURE) //
								.bit(5, ClusterVersionC.ChannelId.RACK_3_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1086, this) //
								.bit(0, ClusterVersionC.ChannelId.RACK_4_VOLTAGE_DIFFERENCE) //
								.bit(1, ClusterVersionC.ChannelId.RACK_4_OVER_CURRENT) //
								.bit(2, ClusterVersionC.ChannelId.RACK_4_HARDWARE_FAILURE) //
								.bit(3, ClusterVersionC.ChannelId.RACK_4_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, ClusterVersionC.ChannelId.RACK_4_PCS_CONTROL_FAILURE) //
								.bit(5, ClusterVersionC.ChannelId.RACK_4_LEVEL_2_ALARM) //
						), //
						m(new BitsWordElement(0x1087, this) //
								.bit(0, ClusterVersionC.ChannelId.RACK_5_VOLTAGE_DIFFERENCE) //
								.bit(1, ClusterVersionC.ChannelId.RACK_5_OVER_CURRENT) //
								.bit(2, ClusterVersionC.ChannelId.RACK_5_HARDWARE_FAILURE) //
								.bit(3, ClusterVersionC.ChannelId.RACK_5_COMMUNICATION_TO_MASTER_FAILURE) //
								.bit(4, ClusterVersionC.ChannelId.RACK_5_PCS_CONTROL_FAILURE) //
								.bit(5, ClusterVersionC.ChannelId.RACK_5_LEVEL_2_ALARM) //
						), //
						new DummyRegisterElement(0x1088, 0x1092), //
						// Pre-Alarm Summary: Temperature Alarm can be used for current limitation,
						// while all other alarms are just for alarm. Note: Alarm for all clusters
						m(new BitsWordElement(0x1093, this) //
								.bit(0, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_HIGH) //
								.bit(1, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CHARGE_CURRENT_HIGH) //
								.bit(3, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_LOW) //
								.bit(4, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_LOW) //
								.bit(5, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CHARGE_TEMP_HIGH) //
								.bit(7, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CHARGE_TEMP_LOW) //
								.bit(8, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_SOC_LOW) //
								.bit(9, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_TEMP_DIFF_TOO_BIG) //
								.bit(10, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_POWER_POLE_HIGH) //
								.bit(11, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(12, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_INSULATION_FAIL) //
								.bit(13, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_TOTAL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(14, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMP_HIGH) //
								.bit(15, SoltaroBatteryVersionC.ChannelId.PRE_ALARM_DISCHARGE_TEMP_LOW)), //
						// Level 1 Alarm Summary
						m(new BitsWordElement(0x1094, this) //
								.bit(0, SoltaroBatteryVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_HIGH) //
								.bit(1, SoltaroBatteryVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SoltaroBatteryVersionC.ChannelId.LEVEL1_CHARGE_CURRENT_HIGH) //
								.bit(3, SoltaroBatteryVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_LOW) //
								.bit(4, SoltaroBatteryVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_LOW) //
								.bit(5, SoltaroBatteryVersionC.ChannelId.LEVEL1_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SoltaroBatteryVersionC.ChannelId.LEVEL1_CHARGE_TEMP_HIGH) //
								.bit(7, SoltaroBatteryVersionC.ChannelId.LEVEL1_CHARGE_TEMP_LOW) //
								.bit(8, SoltaroBatteryVersionC.ChannelId.LEVEL1_SOC_LOW) //
								.bit(9, SoltaroBatteryVersionC.ChannelId.LEVEL1_TEMP_DIFF_TOO_BIG) //
								.bit(10, SoltaroBatteryVersionC.ChannelId.LEVEL1_POWER_POLE_TEMP_HIGH) //
								.bit(11, SoltaroBatteryVersionC.ChannelId.LEVEL1_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(12, SoltaroBatteryVersionC.ChannelId.LEVEL1_INSULATION_VALUE) //
								.bit(13, SoltaroBatteryVersionC.ChannelId.LEVEL1_TOTAL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(14, SoltaroBatteryVersionC.ChannelId.LEVEL1_DISCHARGE_TEMP_HIGH) //
								.bit(15, SoltaroBatteryVersionC.ChannelId.LEVEL1_DISCHARGE_TEMP_LOW)), //
						// Level 2 Alarm Summary
						m(new BitsWordElement(0x1095, this) //
								.bit(0, SoltaroBatteryVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_HIGH) //
								.bit(1, SoltaroBatteryVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SoltaroBatteryVersionC.ChannelId.LEVEL2_CHARGE_CURRENT_HIGH) //
								.bit(3, SoltaroBatteryVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_LOW) //
								.bit(4, SoltaroBatteryVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_LOW) //
								.bit(5, SoltaroBatteryVersionC.ChannelId.LEVEL2_DISCHARGE_CURRENT_HIGH) //
								.bit(6, SoltaroBatteryVersionC.ChannelId.LEVEL2_CHARGE_TEMP_HIGH) //
								.bit(7, SoltaroBatteryVersionC.ChannelId.LEVEL2_CHARGE_TEMP_LOW) //
								.bit(8, SoltaroBatteryVersionC.ChannelId.LEVEL2_SOC_LOW) //
								.bit(9, SoltaroBatteryVersionC.ChannelId.LEVEL2_TEMP_DIFF_TOO_BIG) //
								.bit(10, SoltaroBatteryVersionC.ChannelId.LEVEL2_POWER_POLE_TEMP_HIGH) //
								.bit(11, SoltaroBatteryVersionC.ChannelId.LEVEL2_CELL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(12, SoltaroBatteryVersionC.ChannelId.LEVEL2_INSULATION_VALUE) //
								.bit(13, SoltaroBatteryVersionC.ChannelId.LEVEL2_TOTAL_VOLTAGE_DIFF_TOO_BIG) //
								.bit(14, SoltaroBatteryVersionC.ChannelId.LEVEL2_DISCHARGE_TEMP_HIGH) //
								.bit(15, SoltaroBatteryVersionC.ChannelId.LEVEL2_DISCHARGE_TEMP_LOW) //
						) //
				)); //

		// Create racks dynamically, do this before super() call because super() uses
		// getModbusProtocol, and it is using racks...
		for (RackInfo r : this.racks) {
			protocol.addTasks(//
					// Single Cluster Control Registers (running without Master BMS)
					new FC6WriteRegisterTask(r.offset + 0x0010, //
							m(rack(r, SingleRack.PRE_CHARGE_CONTROL), new UnsignedWordElement(r.offset + 0x0010)) //
					), //
					new FC16WriteRegistersTask(r.offset + 0x000B, //
							m(rack(r, SingleRack.EMS_ADDRESS), new UnsignedWordElement(r.offset + 0x000B)), //
							m(rack(r, SingleRack.EMS_BAUDRATE), new UnsignedWordElement(r.offset + 0x000C)) //
					), //
					new FC6WriteRegisterTask(r.offset + 0x00F4,
							m(rack(r, SingleRack.EMS_COMMUNICATION_TIMEOUT), new UnsignedWordElement(r.offset + 0x00F4)) //
					), //
					new FC3ReadRegistersTask(r.offset + 0x000B, Priority.LOW, //
							m(rack(r, SingleRack.EMS_ADDRESS), new UnsignedWordElement(r.offset + 0x000B)), //
							m(rack(r, SingleRack.EMS_BAUDRATE), new UnsignedWordElement(r.offset + 0x000C)), //
							new DummyRegisterElement(r.offset + 0x000D, r.offset + 0x000F),
							m(rack(r, SingleRack.PRE_CHARGE_CONTROL), new UnsignedWordElement(r.offset + 0x0010)), //
							new DummyRegisterElement(r.offset + 0x0011, r.offset + 0x0014),
							m(rack(r, SingleRack.SET_SUB_MASTER_ADDRESS), new UnsignedWordElement(r.offset + 0x0015)) //
					), //
					new FC3ReadRegistersTask(r.offset + 0x00F4, Priority.LOW, //
							m(rack(r, SingleRack.EMS_COMMUNICATION_TIMEOUT), new UnsignedWordElement(r.offset + 0x00F4)) //
					),

					// Single Cluster Control Registers (General)
					new FC6WriteRegisterTask(r.offset + 0x00CC, //
							m(rack(r, SingleRack.SYSTEM_TOTAL_CAPACITY), new UnsignedWordElement(r.offset + 0x00CC)) //
					), //
					new FC6WriteRegisterTask(r.offset + 0x0015, //
							m(rack(r, SingleRack.SET_SUB_MASTER_ADDRESS), new UnsignedWordElement(r.offset + 0x0015)) //
					), //
					new FC6WriteRegisterTask(r.offset + 0x00F3, //
							m(rack(r, SingleRack.VOLTAGE_LOW_PROTECTION), new UnsignedWordElement(r.offset + 0x00F3)) //
					), //
					new FC3ReadRegistersTask(r.offset + 0x00CC, Priority.LOW, //
							m(this.rack(r, SingleRack.SYSTEM_TOTAL_CAPACITY),
									new UnsignedWordElement(r.offset + 0x00CC)) //
					),

					// Single Cluster Status Registers
					new FC3ReadRegistersTask(r.offset + 0x100, Priority.HIGH, //
							m(rack(r, SingleRack.VOLTAGE), new UnsignedWordElement(r.offset + 0x100),
									ElementToChannelConverter.SCALE_FACTOR_2),
							m(rack(r, SingleRack.CURRENT), new UnsignedWordElement(r.offset + 0x101),
									ElementToChannelConverter.SCALE_FACTOR_2),
							m(rack(r, SingleRack.CHARGE_INDICATION), new UnsignedWordElement(r.offset + 0x102)),
							m(rack(r, SingleRack.SOC), new UnsignedWordElement(r.offset + 0x103)),
							m(rack(r, SingleRack.SOH), new UnsignedWordElement(r.offset + 0x104)),
							m(rack(r, SingleRack.MAX_CELL_VOLTAGE_ID), new UnsignedWordElement(r.offset + 0x105)),
							m(rack(r, SingleRack.MAX_CELL_VOLTAGE), new UnsignedWordElement(r.offset + 0x106)),
							m(rack(r, SingleRack.MIN_CELL_VOLTAGE_ID), new UnsignedWordElement(r.offset + 0x107)),
							m(rack(r, SingleRack.MIN_CELL_VOLTAGE), new UnsignedWordElement(r.offset + 0x108)),
							m(rack(r, SingleRack.MAX_CELL_TEMPERATURE_ID), new UnsignedWordElement(r.offset + 0x109)),
							m(rack(r, SingleRack.MAX_CELL_TEMPERATURE), new UnsignedWordElement(r.offset + 0x10A)),
							m(rack(r, SingleRack.MIN_CELL_TEMPERATURE_ID), new UnsignedWordElement(r.offset + 0x10B)),
							m(rack(r, SingleRack.MIN_CELL_TEMPERATURE), new UnsignedWordElement(r.offset + 0x10C)),
							m(rack(r, SingleRack.AVERAGE_VOLTAGE), new UnsignedWordElement(r.offset + 0x10D)),
							m(rack(r, SingleRack.SYSTEM_INSULATION), new UnsignedWordElement(r.offset + 0x10E)),
							m(rack(r, SingleRack.SYSTEM_MAX_CHARGE_CURRENT), new UnsignedWordElement(r.offset + 0x10F),
									ElementToChannelConverter.SCALE_FACTOR_2),
							m(rack(r, SingleRack.SYSTEM_MAX_DISCHARGE_CURRENT),
									new UnsignedWordElement(r.offset + 0x110),
									ElementToChannelConverter.SCALE_FACTOR_2),
							m(rack(r, SingleRack.POSITIVE_INSULATION), new UnsignedWordElement(r.offset + 0x111)),
							m(rack(r, SingleRack.NEGATIVE_INSULATION), new UnsignedWordElement(r.offset + 0x112)),
							m(rack(r, SingleRack.CLUSTER_RUN_STATE), new UnsignedWordElement(r.offset + 0x113)),
							m(rack(r, SingleRack.AVG_TEMPERATURE), new UnsignedWordElement(r.offset + 0x114))),
					new FC3ReadRegistersTask(r.offset + 0x18b, Priority.LOW,
							m(rack(r, SingleRack.PROJECT_ID), new UnsignedWordElement(r.offset + 0x18b)),
							m(rack(r, SingleRack.VERSION_MAJOR), new UnsignedWordElement(r.offset + 0x18c)),
							m(rack(r, SingleRack.VERSION_SUB), new UnsignedWordElement(r.offset + 0x18d)),
							m(rack(r, SingleRack.VERSION_MODIFY), new UnsignedWordElement(r.offset + 0x18e))),

					// System Warning/Shut Down Status Registers
					new FC3ReadRegistersTask(r.offset + 0x140, Priority.LOW,
							// Level 2 Alarm: BMS Self-protect, main contactor shut down
							m(new BitsWordElement(r.offset + 0x140, this) //
									.bit(0, rack(r, SingleRack.LEVEL2_CELL_VOLTAGE_HIGH)) //
									.bit(1, rack(r, SingleRack.LEVEL2_TOTAL_VOLTAGE_HIGH)) //
									.bit(2, rack(r, SingleRack.LEVEL2_CHARGE_CURRENT_HIGH)) //
									.bit(3, rack(r, SingleRack.LEVEL2_CELL_VOLTAGE_LOW)) //
									.bit(4, rack(r, SingleRack.LEVEL2_TOTAL_VOLTAGE_LOW)) //
									.bit(5, rack(r, SingleRack.LEVEL2_DISCHARGE_CURRENT_HIGH)) //
									.bit(6, rack(r, SingleRack.LEVEL2_CHARGE_TEMP_HIGH)) //
									.bit(7, rack(r, SingleRack.LEVEL2_CHARGE_TEMP_LOW)) //
									// 8 -> Reserved
									// 9 -> Reserved
									.bit(10, rack(r, SingleRack.LEVEL2_POWER_POLE_TEMP_HIGH)) //
									// 11 -> Reserved
									.bit(12, rack(r, SingleRack.LEVEL2_INSULATION_VALUE)) //
									// 13 -> Reserved
									.bit(14, rack(r, SingleRack.LEVEL2_DISCHARGE_TEMP_HIGH)) //
									.bit(15, rack(r, SingleRack.LEVEL2_DISCHARGE_TEMP_LOW)) //
							),
							// Level 1 Alarm: EMS Control to stop charge, discharge, charge&discharge
							m(new BitsWordElement(r.offset + 0x141, this) //
									.bit(0, rack(r, SingleRack.LEVEL1_CELL_VOLTAGE_HIGH)) //
									.bit(1, rack(r, SingleRack.LEVEL1_TOTAL_VOLTAGE_HIGH)) //
									.bit(2, rack(r, SingleRack.LEVEL1_CHARGE_CURRENT_HIGH)) //
									.bit(3, rack(r, SingleRack.LEVEL1_CELL_VOLTAGE_LOW)) //
									.bit(4, rack(r, SingleRack.LEVEL1_TOTAL_VOLTAGE_LOW)) //
									.bit(5, rack(r, SingleRack.LEVEL1_DISCHARGE_CURRENT_HIGH)) //
									.bit(6, rack(r, SingleRack.LEVEL1_CHARGE_TEMP_HIGH)) //
									.bit(7, rack(r, SingleRack.LEVEL1_CHARGE_TEMP_LOW)) //
									.bit(8, rack(r, SingleRack.LEVEL1_SOC_LOW)) //
									.bit(9, rack(r, SingleRack.LEVEL1_TEMP_DIFF_TOO_BIG)) //
									.bit(10, rack(r, SingleRack.LEVEL1_POWER_POLE_TEMP_HIGH)) //
									.bit(11, rack(r, SingleRack.LEVEL1_CELL_VOLTAGE_DIFF_TOO_BIG)) //
									.bit(12, rack(r, SingleRack.LEVEL1_INSULATION_VALUE)) //
									.bit(13, rack(r, SingleRack.LEVEL1_TOTAL_VOLTAGE_DIFF_TOO_BIG)) //
									.bit(14, rack(r, SingleRack.LEVEL1_DISCHARGE_TEMP_HIGH)) //
									.bit(15, rack(r, SingleRack.LEVEL1_DISCHARGE_TEMP_LOW)) //
							),
							// Pre-Alarm: Temperature Alarm will active current limication
							m(new BitsWordElement(r.offset + 0x142, this) //
									.bit(0, rack(r, SingleRack.PRE_ALARM_CELL_VOLTAGE_HIGH)) //
									.bit(1, rack(r, SingleRack.PRE_ALARM_TOTAL_VOLTAGE_HIGH)) //
									.bit(2, rack(r, SingleRack.PRE_ALARM_CHARGE_CURRENT_HIGH)) //
									.bit(3, rack(r, SingleRack.PRE_ALARM_CELL_VOLTAGE_LOW)) //
									.bit(4, rack(r, SingleRack.PRE_ALARM_TOTAL_VOLTAGE_LOW)) //
									.bit(5, rack(r, SingleRack.PRE_ALARM_DISCHARGE_CURRENT_HIGH)) //
									.bit(6, rack(r, SingleRack.PRE_ALARM_CHARGE_TEMP_HIGH)) //
									.bit(7, rack(r, SingleRack.PRE_ALARM_CHARGE_TEMP_LOW)) //
									.bit(8, rack(r, SingleRack.PRE_ALARM_SOC_LOW)) //
									.bit(9, rack(r, SingleRack.PRE_ALARM_TEMP_DIFF_TOO_BIG)) //
									.bit(10, rack(r, SingleRack.PRE_ALARM_POWER_POLE_HIGH))//
									.bit(11, rack(r, SingleRack.PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG)) //
									.bit(12, rack(r, SingleRack.PRE_ALARM_INSULATION_FAIL)) //
									.bit(13, rack(r, SingleRack.PRE_ALARM_TOTAL_VOLTAGE_DIFF_TOO_BIG)) //
									.bit(14, rack(r, SingleRack.PRE_ALARM_DISCHARGE_TEMP_HIGH)) //
									.bit(15, rack(r, SingleRack.PRE_ALARM_DISCHARGE_TEMP_LOW)) //
							) //
					),
					// Other Alarm Info
					new FC3ReadRegistersTask(r.offset + 0x1A5, Priority.LOW, //
							m(new BitsWordElement(r.offset + 0x1A5, this) //
									.bit(0, rack(r, SingleRack.ALARM_COMMUNICATION_TO_MASTER_BMS)) //
									.bit(1, rack(r, SingleRack.ALARM_COMMUNICATION_TO_SLAVE_BMS)) //
									.bit(2, rack(r, SingleRack.ALARM_COMMUNICATION_SLAVE_BMS_TO_TEMP_SENSORS)) //
									.bit(3, rack(r, SingleRack.ALARM_SLAVE_BMS_HARDWARE)) //
							)),
					// Slave BMS Fault Message Registers
					new FC3ReadRegistersTask(r.offset + 0x185, Priority.LOW, //
							m(new BitsWordElement(r.offset + 0x185, this) //
									.bit(0, rack(r, SingleRack.SLAVE_BMS_VOLTAGE_SENSOR_CABLES)) //
									.bit(1, rack(r, SingleRack.SLAVE_BMS_POWER_CABLE)) //
									.bit(2, rack(r, SingleRack.SLAVE_BMS_LTC6803)) //
									.bit(3, rack(r, SingleRack.SLAVE_BMS_VOLTAGE_SENSORS)) //
									.bit(4, rack(r, SingleRack.SLAVE_BMS_TEMP_SENSOR_CABLES)) //
									.bit(5, rack(r, SingleRack.SLAVE_BMS_TEMP_SENSORS)) //
									.bit(6, rack(r, SingleRack.SLAVE_BMS_POWER_POLE_TEMP_SENSOR)) //
									.bit(7, rack(r, SingleRack.SLAVE_BMS_TEMP_BOARD_COM)) //
									.bit(8, rack(r, SingleRack.SLAVE_BMS_BALANCE_MODULE)) //
									.bit(9, rack(r, SingleRack.SLAVE_BMS_TEMP_SENSORS2)) //
									.bit(10, rack(r, SingleRack.SLAVE_BMS_INTERNAL_COM)) //
									.bit(11, rack(r, SingleRack.SLAVE_BMS_EEPROM)) //
									.bit(12, rack(r, SingleRack.SLAVE_BMS_INIT)) //
							)) //
			); //

			/*
			 * Add tasks for cell voltages and temperatures according to the number of
			 * slaves, one task per module is created Cell voltages
			 */
			Consumer<CellChannelFactory.Type> addCellChannels = (type) -> {
				for (int i = 0; i < this.config.numberOfSlaves(); i++) {
					AbstractModbusElement<?>[] elements = new AbstractModbusElement<?>[type.getSensorsPerModule()];
					for (int j = 0; j < type.getSensorsPerModule(); j++) {
						int sensorIndex = i * type.getSensorsPerModule() + j;
						io.openems.edge.common.channel.ChannelId channelId = CellChannelFactory.create(r, type,
								sensorIndex);
						// Register the Channel at this Component
						this.addChannel(channelId);
						// Add the Modbus Element and map it to the Channel
						elements[j] = m(channelId, new UnsignedWordElement(type.getOffset() + sensorIndex));
					}
					// Add a Modbus read task for this module
					protocol.addTask(//
							new FC3ReadRegistersTask(r.offset + type.getOffset() + i * type.getSensorsPerModule(),
									Priority.LOW, elements));
				}
			};
			addCellChannels.accept(CellChannelFactory.Type.VOLTAGE);
			addCellChannels.accept(CellChannelFactory.Type.TEMPERATURE);

			// WARN_LEVEL_Pre Alarm (Pre Alarm configuration registers RW)
			{
				AbstractModbusElement<?>[] elements = new AbstractModbusElement<?>[] {
						m(rack(r, SingleRack.PRE_ALARM_CELL_OVER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x080)), //
						m(rack(r, SingleRack.PRE_ALARM_CELL_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x081)), //
						m(rack(r, SingleRack.PRE_ALARM_SYSTEM_OVER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x082), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.PRE_ALARM_SYSTEM_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x083), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_ALARM),
								new UnsignedWordElement(r.offset + 0x084), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.PRE_ALARM_SYSTEM_CHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x085), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.PRE_ALARM_CELL_UNDER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x086)), //
						m(rack(r, SingleRack.PRE_ALARM_CELL_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x087)), //
						m(rack(r, SingleRack.PRE_ALARM_SYSTEM_UNDER_VOLTAGE_ALARM),
								new UnsignedWordElement(r.offset + 0x088), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.PRE_ALARM_SYSTEM_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x089), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_ALARM),
								new UnsignedWordElement(r.offset + 0x08A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.PRE_ALARM_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x08B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.PRE_ALARM_CELL_OVER_TEMPERATURE_ALARM),
								new UnsignedWordElement(r.offset + 0x08C)), //
						m(rack(r, SingleRack.PRE_ALARM_CELL_OVER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x08D)), //
						m(rack(r, SingleRack.PRE_ALARM_CELL_UNDER_TEMPERATURE_ALARM),
								new UnsignedWordElement(r.offset + 0x08E)), //
						m(rack(r, SingleRack.PRE_ALARM_CELL_UNDER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x08F)), //
						m(rack(r, SingleRack.PRE_ALARM_SOC_LOW_ALARM), new UnsignedWordElement(r.offset + 0x090)), //
						m(rack(r, SingleRack.PRE_ALARM_SOC_LOW_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x091)), //
						new DummyRegisterElement(r.offset + 0x092, r.offset + 0x093),
						m(rack(r, SingleRack.PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM),
								new UnsignedWordElement(r.offset + 0x094)), //
						m(rack(r, SingleRack.PRE_ALARM_CONNECTOR_TEMPERATURE_HIGH_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x095)), //
						m(rack(r, SingleRack.PRE_ALARM_INSULATION_ALARM), new UnsignedWordElement(r.offset + 0x096)), //
						m(rack(r, SingleRack.PRE_ALARM_INSULATION_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x097)), //
						m(rack(r, SingleRack.PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM),
								new UnsignedWordElement(r.offset + 0x098)), //
						m(rack(r, SingleRack.PRE_ALARM_CELL_VOLTAGE_DIFFERENCE_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x099)), //
						m(rack(r, SingleRack.PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM),
								new UnsignedWordElement(r.offset + 0x09A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.PRE_ALARM_TOTAL_VOLTAGE_DIFFERENCE_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x09B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM),
								new UnsignedWordElement(r.offset + 0x09C)), //
						m(rack(r, SingleRack.PRE_ALARM_DISCHARGE_TEMPERATURE_HIGH_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x09D)), //
						m(rack(r, SingleRack.PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM),
								new UnsignedWordElement(r.offset + 0x09E)), //
						m(rack(r, SingleRack.PRE_ALARM_DISCHARGE_TEMPERATURE_LOW_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x09F)), //
						m(rack(r, SingleRack.PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM),
								new UnsignedWordElement(r.offset + 0x0A0)), //
						m(rack(r, SingleRack.PRE_ALARM_TEMPERATURE_DIFFERENCE_ALARM_RECOVER),
								new UnsignedWordElement(r.offset + 0x0A1)) //
				};
				protocol.addTask(new FC16WriteRegistersTask(r.offset + 0x080, elements));
				protocol.addTask(new FC3ReadRegistersTask(r.offset + 0x080, Priority.LOW, elements));
			}

			// WARN_LEVEL1 (Level1 warning registers RW)
			{
				AbstractModbusElement<?>[] elements = new AbstractModbusElement<?>[] {
						m(rack(r, SingleRack.LEVEL1_CELL_OVER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x040)), //
						m(rack(r, SingleRack.LEVEL1_CELL_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x041)), //
						m(rack(r, SingleRack.LEVEL1_SYSTEM_OVER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x042), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL1_SYSTEM_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x043), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x044), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL1_SYSTEM_CHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x045), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL1_CELL_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x046)), //
						m(rack(r, SingleRack.LEVEL1_CELL_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x047)), //
						m(rack(r, SingleRack.LEVEL1_SYSTEM_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x048), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL1_SYSTEM_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x049), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x04A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL1_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x04B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL1_CELL_OVER_TEMPERATURE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x04C)), //
						m(rack(r, SingleRack.LEVEL1_CELL_OVER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x04D)), //
						m(rack(r, SingleRack.LEVEL1_CELL_UNDER_TEMPERATURE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x04E)), //
						m(rack(r, SingleRack.LEVEL1_CELL_UNDER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x04F)), //
						m(rack(r, SingleRack.LEVEL1_SOC_LOW_PROTECTION), new UnsignedWordElement(r.offset + 0x050)), //
						m(rack(r, SingleRack.LEVEL1_SOC_LOW_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x051)), //
						new DummyRegisterElement(r.offset + 0x052, r.offset + 0x053), //
						m(rack(r, SingleRack.LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION),
								new UnsignedWordElement(r.offset + 0x054)), //
						m(rack(r, SingleRack.LEVEL1_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x055)), //
						m(rack(r, SingleRack.LEVEL1_INSULATION_PROTECTION), new UnsignedWordElement(r.offset + 0x056)), //
						m(rack(r, SingleRack.LEVEL1_INSULATION_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x057)), //
						m(rack(r, SingleRack.LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x058)), //
						m(rack(r, SingleRack.LEVEL1_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x059)), //
						m(rack(r, SingleRack.LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x05A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL1_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x05B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION),
								new UnsignedWordElement(r.offset + 0x05C)), //
						m(rack(r, SingleRack.LEVEL1_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x05D)), //
						m(rack(r, SingleRack.LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION),
								new UnsignedWordElement(r.offset + 0x05E)), //
						m(rack(r, SingleRack.LEVEL1_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x05F)), //
						m(rack(r, SingleRack.LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x060)), //
						m(rack(r, SingleRack.LEVEL1_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x061)) //
				};
				protocol.addTask(new FC16WriteRegistersTask(r.offset + 0x040, elements));
				protocol.addTask(new FC3ReadRegistersTask(r.offset + 0x040, Priority.LOW, elements));
			}

			// WARN_LEVEL2 (Level2 Protection registers RW)
			{
				AbstractModbusElement<?>[] elements = new AbstractModbusElement<?>[] {
						m(rack(r, SingleRack.LEVEL2_CELL_OVER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x400)), //
						m(rack(r, SingleRack.LEVEL2_CELL_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x401)), //
						m(new UnsignedWordElement(r.offset + 0x402)) //
								.m(rack(r, SingleRack.LEVEL2_SYSTEM_OVER_VOLTAGE_PROTECTION),
										ElementToChannelConverter.SCALE_FACTOR_2) // [mV]
								.m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_1) // [V]
								.build(), //
						m(rack(r, SingleRack.LEVEL2_SYSTEM_OVER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x403), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x404), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL2_SYSTEM_CHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x405), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL2_CELL_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x406)), //
						m(rack(r, SingleRack.LEVEL2_CELL_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x407)), //
						m(rack(r, SingleRack.LEVEL2_SYSTEM_UNDER_VOLTAGE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x408), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL2_SYSTEM_UNDER_VOLTAGE_RECOVER),
								new UnsignedWordElement(r.offset + 0x409), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_PROTECTION),
								new UnsignedWordElement(r.offset + 0x40A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL2_SYSTEM_DISCHARGE_OVER_CURRENT_RECOVER),
								new UnsignedWordElement(r.offset + 0x40B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL2_CELL_OVER_TEMPERATURE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x40C)), //
						m(rack(r, SingleRack.LEVEL2_CELL_OVER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x40D)), //
						m(rack(r, SingleRack.LEVEL2_CELL_UNDER_TEMPERATURE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x40E)), //
						m(rack(r, SingleRack.LEVEL2_CELL_UNDER_TEMPERATURE_RECOVER),
								new UnsignedWordElement(r.offset + 0x40F)), //
						m(rack(r, SingleRack.LEVEL2_SOC_LOW_PROTECTION), new UnsignedWordElement(r.offset + 0x410)), //
						m(rack(r, SingleRack.LEVEL2_SOC_LOW_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x411)), //
						new DummyRegisterElement(r.offset + 0x412, r.offset + 0x413), //
						m(rack(r, SingleRack.LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION),
								new UnsignedWordElement(r.offset + 0x414)), //
						m(rack(r, SingleRack.LEVEL2_CONNECTOR_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x415)), //
						m(rack(r, SingleRack.LEVEL2_INSULATION_PROTECTION), new UnsignedWordElement(r.offset + 0x416)), //
						m(rack(r, SingleRack.LEVEL2_INSULATION_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x417)), //
						m(rack(r, SingleRack.LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x418)), //
						m(rack(r, SingleRack.LEVEL2_CELL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x419)), //
						m(rack(r, SingleRack.LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x41A), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL2_TOTAL_VOLTAGE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x41B), ElementToChannelConverter.SCALE_FACTOR_2), //
						m(rack(r, SingleRack.LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION),
								new UnsignedWordElement(r.offset + 0x41C)), //
						m(rack(r, SingleRack.LEVEL2_DISCHARGE_TEMPERATURE_HIGH_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x41D)), //
						m(rack(r, SingleRack.LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION),
								new UnsignedWordElement(r.offset + 0x41E)), //
						m(rack(r, SingleRack.LEVEL2_DISCHARGE_TEMPERATURE_LOW_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x41F)), //
						m(rack(r, SingleRack.LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION),
								new UnsignedWordElement(r.offset + 0x420)), //
						m(rack(r, SingleRack.LEVEL2_TEMPERATURE_DIFFERENCE_PROTECTION_RECOVER),
								new UnsignedWordElement(r.offset + 0x421)) //
				};
				protocol.addTask(new FC16WriteRegistersTask(r.offset + 0x400, elements));
				protocol.addTask(new FC3ReadRegistersTask(r.offset + 0x400, Priority.LOW, elements));
			}

		}

//				
//				for (int i : config.racks()) {
//					this.racks.put(i, new SingleRack(i, config.numberOfSlaves(), RACK_INFO.get(i).addressOffset, this));
//				}
//				
//				// -------- control registers of master --------------------------------------
//				new FC16WriteRegistersTask(0x1004, //
//						m(ClusterChannelId.RESET, new UnsignedWordElement(0x1004)) //
//				), //

		// -------- state registers of master --------------------------------------

		return protocol;
	}

	/**
	 * Factory-Function for SingleRack-ChannelIds. Creates a ChannelId, registers
	 * the Channel and returns the ChannelId.
	 * 
	 * @param rackInfo   the {@link RackInfo}
	 * @param singleRack the {@link SingleRack}
	 * @return the {@link io.openems.edge.common.channel.ChannelId}
	 */
	private final io.openems.edge.common.channel.ChannelId rack(RackInfo rackInfo, SingleRack singleRack) {
		Channel<?> existingChannel = this._channel(singleRack.toChannelIdString(rackInfo));
		if (existingChannel != null) {
			return existingChannel.channelId();
		} else {
			io.openems.edge.common.channel.ChannelId channelId = singleRack.toChannelId(rackInfo);
			this.addChannel(channelId);
			return channelId;
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		// TODO Auto-generated method stub
		return null;
	};

//	private int getAddressContactorControl(int addressOffsetRack) {
//		return addressOffsetRack + OFFSET_CONTACTOR_CONTROL;
//	}

//
//	protected void recalculateSoc() {
//		int i = 0;
//		int soc = 0;
//
//		for (SingleRack rack : this.racks.values()) {
//			soc = soc + rack.getSoC();
//			i++;
//		}
//
//		if (i > 0) {
//			soc = soc / i;
//		}
//
//		this.channel(Battery.ChannelId.SOC).setNextValue(soc);
//	}
//
//	protected void recalculateMaxCellVoltage() {
//		int max = Integer.MIN_VALUE;
//
//		for (SingleRack rack : this.racks.values()) {
//			max = Math.max(max, rack.getMaximalCellVoltage());
//		}
//		this.channel(Battery.ChannelId.MAX_CELL_VOLTAGE).setNextValue(max);
//	}
//
//	protected void recalculateMinCellVoltage() {
//		int min = Integer.MAX_VALUE;
//
//		for (SingleRack rack : this.racks.values()) {
//			min = Math.min(min, rack.getMinimalCellVoltage());
//		}
//		this.channel(Battery.ChannelId.MIN_CELL_VOLTAGE).setNextValue(min);
//	}
//
//	protected void recalculateMaxCellTemperature() {
//		int max = Integer.MIN_VALUE;
//
//		for (SingleRack rack : this.racks.values()) {
//			max = Math.max(max, rack.getMaximalCellTemperature());
//		}
//		this.channel(Battery.ChannelId.MAX_CELL_TEMPERATURE).setNextValue(max);
//	}
//
//	protected void recalculateMinCellTemperature() {
//		int min = Integer.MAX_VALUE;
//
//		for (SingleRack rack : this.racks.values()) {
//			min = Math.min(min, rack.getMinimalCellTemperature());
//		}
//		this.channel(Battery.ChannelId.MIN_CELL_TEMPERATURE).setNextValue(min);
//	}
//
//	@Override
//	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
//		return new ModbusSlaveTable( //
//				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
//				Battery.getModbusSlaveNatureTable(accessMode) //
//		);
//	}
}
