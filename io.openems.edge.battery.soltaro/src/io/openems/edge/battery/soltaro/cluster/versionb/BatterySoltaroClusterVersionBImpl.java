package io.openems.edge.battery.soltaro.cluster.versionb;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.soltaro.cluster.SoltaroCluster;
import io.openems.edge.battery.soltaro.cluster.enums.ClusterStartStop;
import io.openems.edge.battery.soltaro.cluster.enums.RackUsage;
import io.openems.edge.battery.soltaro.common.batteryprotection.BatteryProtectionDefinitionSoltaro3000Wh;
import io.openems.edge.battery.soltaro.common.enums.BatteryState;
import io.openems.edge.battery.soltaro.common.enums.ResetState;
import io.openems.edge.battery.soltaro.common.enums.State;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bms.Soltaro.Cluster.VersionB", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class BatterySoltaroClusterVersionBImpl extends AbstractOpenemsModbusComponent implements SoltaroCluster,
		Battery, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, StartStoppable {

	private static final int ADDRESS_OFFSET_RACK_1 = 0x2000;
	private static final int ADDRESS_OFFSET_RACK_2 = 0x3000;
	private static final int ADDRESS_OFFSET_RACK_3 = 0x4000;
	private static final int ADDRESS_OFFSET_RACK_4 = 0x5000;
	private static final int ADDRESS_OFFSET_RACK_5 = 0x6000;
	private static final int OFFSET_CONTACTOR_CONTROL = 0x10;
	private static final int MIN_VOLTAGE_MILLIVOLT = 34_800;
	private static final int MAX_VOLTAGE_MILLIVOLT = 42_700;

	// Helper that holds general information about single racks, independent if they
	// are used or not
	private static final Map<Integer, RackInfo> RACK_INFO = createRackInfo();

	private final Logger log = LoggerFactory.getLogger(BatterySoltaroClusterVersionBImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	// If an error has occurred, this indicates the time when next action could be
	// done
	private LocalDateTime errorDelayIsOver = null;
	private int unsuccessfulStarts = 0;
	private LocalDateTime startAttemptTime = null;
	private String modbusBridgeId;
	private BatteryState batteryState;
	private State state = State.UNDEFINED;
	private Config config;
	private final Map<Integer, SingleRack> racks = new HashMap<>();
	// this timestamp is used to wait a certain time if system state could no be
	// determined at once
	private LocalDateTime pendingTimestamp;

	private ResetState resetState = ResetState.NONE;
	private boolean resetDone;
	private BatteryProtection batteryProtection = null;

	public BatterySoltaroClusterVersionBImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SoltaroCluster.ChannelId.values(), //
				BatterySoltaroClusterVersionB.ChannelId.values(), //
				BatteryProtection.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		// Create racks dynamically, do this before super() call because super() uses
		// getModbusProtocol, and it is using racks...
		for (int i : config.racks()) {
			var rackInfo = RACK_INFO.get(i);
			if (rackInfo == null) {
				throw new OpenemsException("Invalid configuration value [" + i + "] for 'racks'");
			}
			this.racks.put(i, new SingleRack(i, config.numberOfSlaves(), rackInfo.addressOffset, this));
		}

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		this.config = config;
		this.modbusBridgeId = config.modbus_id();
		this.batteryState = config.batteryState();

		// Initialize Battery-Protection
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new BatteryProtectionDefinitionSoltaro3000Wh(), this.componentManager) //
				.build();

		this._setChargeMaxVoltage(this.config.numberOfSlaves() * MAX_VOLTAGE_MILLIVOLT / 1000);
		this._setDischargeMinVoltage(this.config.numberOfSlaves() * MIN_VOLTAGE_MILLIVOLT / 1000);
		this._setCapacity(
				this.config.racks().length * this.config.numberOfSlaves() * this.config.moduleType().getCapacity_Wh());
	}

	@Override
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

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.batteryProtection.apply();
			break;

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
		}
	}

	private void handleStateMachine() {
		// log.info("Cluster.doNormalHandling(): State: " +
		// this.getStateMachineState());
		var readyForWorking = false;
		switch (this.getStateMachineState()) {
		case ERROR:
			this.stopSystem();
			this.errorDelayIsOver = LocalDateTime.now().plusSeconds(this.config.errorLevel2Delay());
			this.setStateMachineState(State.ERRORDELAY);
			break;
		case ERRORDELAY:
			// If we are in the error delay time, the system is reset, this can help
			// handling the errors
			if (LocalDateTime.now().isAfter(this.errorDelayIsOver)) {
				this.errorDelayIsOver = null;
				this.resetDone = false;
				if (this.isError()) {
					this.setStateMachineState(State.ERROR);
				} else {
					this.setStateMachineState(State.UNDEFINED);
				}
			} else if (!this.resetDone) {
				this.handleErrorsWithReset();
			}
			break;

		case INIT:
			if (this.isSystemRunning()) {
				this.setStateMachineState(State.RUNNING);
				this.unsuccessfulStarts = 0;
				this.startAttemptTime = null;
			} else if (this.startAttemptTime.plusSeconds(this.config.maxStartTime()).isBefore(LocalDateTime.now())) {
				this.startAttemptTime = null;
				this.unsuccessfulStarts++;
				this.stopSystem();
				this.setStateMachineState(State.STOPPING);
				if (this.unsuccessfulStarts >= this.config.maxStartAppempts()) {
					this.errorDelayIsOver = LocalDateTime.now().plusSeconds(this.config.startUnsuccessfulDelay());
					this.setStateMachineState(State.ERRORDELAY);
					this.unsuccessfulStarts = 0;
				}
			}
			break;
		case OFF:
			this.startSystem();
			this.setStateMachineState(State.INIT);
			this.startAttemptTime = LocalDateTime.now();
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
			} else if (this.isSystemStopped()) {
				this.setStateMachineState(State.OFF);
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
			break;
		case ERROR_HANDLING:
			this.handleErrorsWithReset();
			break;
		}

		// TODO start stop is not implemented; mark as started if 'readyForWorking'
		this._setStartStop(readyForWorking ? StartStop.START : StartStop.UNDEFINED);
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
			this.resetDone = true;
			break;
		}
	}

	private boolean isError() {
		// still TODO define what is exactly an error
		if (this.readValueFromStateChannel(BatterySoltaroClusterVersionB.ChannelId.MASTER_ALARM_LEVEL_2_INSULATION)) {
			return true;
		}
		if (this.readValueFromStateChannel(BatterySoltaroClusterVersionB.ChannelId.MASTER_ALARM_PCS_EMS_CONTROL_FAIL)) {
			return true;
		}
		if (this.readValueFromStateChannel(
				BatterySoltaroClusterVersionB.ChannelId.MASTER_ALARM_PCS_EMS_COMMUNICATION_FAILURE)) {
			return true;
		}
		if (this.readValueFromStateChannel(
				BatterySoltaroClusterVersionB.ChannelId.MASTER_ALARM_COMMUNICATION_ERROR_WITH_SUBMASTER)) {
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

	@Override
	protected Channel<?> addChannel(io.openems.edge.common.channel.ChannelId channelId) {
		return super.addChannel(channelId);
	}

	private boolean readValueFromStateChannel(io.openems.edge.common.channel.ChannelId channelId) {
		StateChannel s = this.channel(channelId);
		var val = s.value().asOptional();
		return val.isPresent() && val.get();
	}

	private boolean isSystemStopped() {
		return this.haveAllRacksTheSameContactorControlState(ContactorControl.CUT_OFF);
	}

	private boolean isSystemRunning() {
		return this.haveAllRacksTheSameContactorControlState(ContactorControl.ON_GRID);
	}

	private boolean haveAllRacksTheSameContactorControlState(ContactorControl cctrl) {
		var b = true;
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
	 *
	 * @return boolean
	 */
	private boolean isSystemStatePending() {
		var b = true;

		for (SingleRack r : this.racks.values()) {
			EnumReadChannel channel = this.channel(RACK_INFO.get(r.getRackNumber()).positiveContactorChannelId);
			var valOpt = channel.value().asOptional();
			b = b && valOpt.isPresent();
		}

		return b && !this.isSystemRunning() && !this.isSystemStopped();
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc() //
				+ "|Discharge:" + this.getDischargeMinVoltage() + ";" + this.getDischargeMaxCurrent() //
				+ "|Charge:" + this.getChargeMaxVoltage() + ";" + this.getChargeMaxCurrent() //
				+ "|Running: " + this.isSystemRunning() //
				+ "|U: " + this.getVoltage() //
				+ "|I: " + this.getCurrent();
	}

	private void sleepSystem() {
		// Write sleep and reset to all racks
		for (SingleRack rack : this.racks.values()) {
			var sleepChannel = (IntegerWriteChannel) rack.getChannel(SingleRack.KEY_SLEEP);
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

		var resetMasterChannel = (IntegerWriteChannel) this.channel(BatterySoltaroClusterVersionB.ChannelId.RESET);
		try {
			resetMasterChannel.setNextWriteValue(0x1);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Error while trying to reset the master!");
		}

		for (SingleRack rack : this.racks.values()) {
			var resetChannel = (IntegerWriteChannel) rack.getChannel(SingleRack.KEY_RESET);
			try {
				resetChannel.setNextWriteValue(0x1);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Error while trying to reset the system!");
			}
		}
	}

	private void startSystem() {
		try {
			this.setClusterStartStop(ClusterStartStop.START);
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
		try {
			this.setClusterStartStop(ClusterStartStop.STOP);
			// write to all racks unused!!
			for (RackInfo r : RACK_INFO.values()) {
				EnumWriteChannel rackUsageChannel = this.channel(r.usageChannelId);
				rackUsageChannel.setNextWriteValue(RackUsage.UNUSED);
			}
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Error while trying to stop system\n" + e.getMessage());
		}
	}

	/**
	 * Gets the ModbusBridgeId.
	 *
	 * @return String
	 */
	public String getModbusBridgeId() {
		return this.modbusBridgeId;
	}

	/**
	 * Gets the StateMachineState.
	 *
	 * @return State
	 */
	public State getStateMachineState() {
		return this.state;
	}

	/**
	 * Sets the StateMachineState.
	 *
	 * @param state the {@link State}
	 */
	public void setStateMachineState(State state) {
		this.state = state;
		this.channel(BatterySoltaroClusterVersionB.ChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var protocol = new ModbusProtocol(this,
				// -------- control registers of master --------------------------------------
				new FC16WriteRegistersTask(0x1004, //
						m(BatterySoltaroClusterVersionB.ChannelId.RESET, new UnsignedWordElement(0x1004))), //
				new FC16WriteRegistersTask(0x1017, //
						m(SoltaroCluster.ChannelId.CLUSTER_START_STOP, new UnsignedWordElement(0x1017)), //
						m(SoltaroCluster.ChannelId.RACK_1_USAGE, new UnsignedWordElement(0x1018)), //
						m(SoltaroCluster.ChannelId.RACK_2_USAGE, new UnsignedWordElement(0x1019)), //
						m(SoltaroCluster.ChannelId.RACK_3_USAGE, new UnsignedWordElement(0x101A)), //
						m(SoltaroCluster.ChannelId.RACK_4_USAGE, new UnsignedWordElement(0x101B)), //
						m(SoltaroCluster.ChannelId.RACK_5_USAGE, new UnsignedWordElement(0x101C))), //
				new FC3ReadRegistersTask(0x1017, Priority.HIGH,
						m(SoltaroCluster.ChannelId.CLUSTER_START_STOP, new UnsignedWordElement(0x1017)), //
						m(SoltaroCluster.ChannelId.RACK_1_USAGE, new UnsignedWordElement(0x1018)), //
						m(SoltaroCluster.ChannelId.RACK_2_USAGE, new UnsignedWordElement(0x1019)), //
						m(SoltaroCluster.ChannelId.RACK_3_USAGE, new UnsignedWordElement(0x101A)), //
						m(SoltaroCluster.ChannelId.RACK_4_USAGE, new UnsignedWordElement(0x101B)), //
						m(SoltaroCluster.ChannelId.RACK_5_USAGE, new UnsignedWordElement(0x101C))), //
				new FC16WriteRegistersTask(0x101F,
						m(BatterySoltaroClusterVersionB.ChannelId.SYSTEM_INSULATION_LEVEL_1,
								new UnsignedWordElement(0x101F)), //
						m(BatterySoltaroClusterVersionB.ChannelId.SYSTEM_INSULATION_LEVEL_2,
								new UnsignedWordElement(0x1020)), //
						new DummyRegisterElement(0x1021), //
						m(BatterySoltaroClusterVersionB.ChannelId.EMS_COMMUNICATION_TIMEOUT,
								new UnsignedWordElement(0x1022)), //
						m(BatterySoltaroClusterVersionB.ChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1023))), //
				new FC3ReadRegistersTask(0x101F, Priority.LOW,
						m(BatterySoltaroClusterVersionB.ChannelId.SYSTEM_INSULATION_LEVEL_1,
								new UnsignedWordElement(0x101F)), //
						m(BatterySoltaroClusterVersionB.ChannelId.SYSTEM_INSULATION_LEVEL_2,
								new UnsignedWordElement(0x1020)), //
						new DummyRegisterElement(0x1021), //
						m(BatterySoltaroClusterVersionB.ChannelId.EMS_COMMUNICATION_TIMEOUT,
								new UnsignedWordElement(0x1022)), //
						m(BatterySoltaroClusterVersionB.ChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1023))), //
				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1),
						m(BatterySoltaroClusterVersionB.ChannelId.RACK_1_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1)))), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1), Priority.HIGH,
						m(BatterySoltaroClusterVersionB.ChannelId.RACK_1_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1)))), //
				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2),
						m(BatterySoltaroClusterVersionB.ChannelId.RACK_2_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2)))), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2), Priority.HIGH,
						m(BatterySoltaroClusterVersionB.ChannelId.RACK_2_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2)))), //
				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3),
						m(BatterySoltaroClusterVersionB.ChannelId.RACK_3_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3)))), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3), Priority.HIGH,
						m(BatterySoltaroClusterVersionB.ChannelId.RACK_3_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3)))), //
				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4),
						m(BatterySoltaroClusterVersionB.ChannelId.RACK_4_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4)))), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4), Priority.HIGH,
						m(BatterySoltaroClusterVersionB.ChannelId.RACK_4_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4)))), //
				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5),
						m(BatterySoltaroClusterVersionB.ChannelId.RACK_5_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5)))), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5), Priority.HIGH,
						m(BatterySoltaroClusterVersionB.ChannelId.RACK_5_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5)))), //

				// -------- state registers of master --------------------------------------
				new FC3ReadRegistersTask(0x1044, Priority.LOW, //
						m(SoltaroCluster.ChannelId.CHARGE_INDICATION, new UnsignedWordElement(0x1044)), //
						m(Battery.ChannelId.CURRENT, new UnsignedWordElement(0x1045), //
								SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(0x1046), //
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x1047)) //
								.onUpdateCallback(val -> {
									this.recalculateSoc();
								}), //
						m(SoltaroCluster.ChannelId.SYSTEM_RUNNING_STATE, new UnsignedWordElement(0x1048)), //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(0x1049), //
								SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(0x104A, Priority.HIGH, //
						m(SoltaroCluster.ChannelId.SYSTEM_INSULATION, new UnsignedWordElement(0x104A)), //
						new DummyRegisterElement(0x104B, 0x104D), //
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS, new UnsignedWordElement(0x104E),
								SCALE_FACTOR_MINUS_1), //
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, new UnsignedWordElement(0x104F),
								SCALE_FACTOR_MINUS_1)), //
				new FC3ReadRegistersTask(0x1081, Priority.LOW, //
						m(new BitsWordElement(0x1081, this) //
								.bit(4, BatterySoltaroClusterVersionB.ChannelId.MASTER_ALARM_LEVEL_2_INSULATION) //
								.bit(3, BatterySoltaroClusterVersionB.ChannelId.MASTER_ALARM_LEVEL_1_INSULATION) //
								.bit(2, BatterySoltaroClusterVersionB.ChannelId.MASTER_ALARM_PCS_EMS_CONTROL_FAIL) //
								.bit(1, BatterySoltaroClusterVersionB.ChannelId.MASTER_ALARM_PCS_EMS_COMMUNICATION_FAILURE) //
								.bit(0, BatterySoltaroClusterVersionB.ChannelId.MASTER_ALARM_COMMUNICATION_ERROR_WITH_SUBMASTER) //
						), //
						new UnsignedWordElement(0x1082).onUpdateCallback(this.parseSubMasterCommunicationFailure),
						m(new BitsWordElement(0x1083, this) //
								.bit(5, BatterySoltaroClusterVersionB.ChannelId.RACK_1_LEVEL_2_ALARM) //
								.bit(4, BatterySoltaroClusterVersionB.ChannelId.RACK_1_PCS_CONTROL_FAULT) //
								.bit(3, BatterySoltaroClusterVersionB.ChannelId.RACK_1_COMMUNICATION_WITH_MASTER_ERROR) //
								.bit(2, BatterySoltaroClusterVersionB.ChannelId.RACK_1_DEVICE_ERROR) //
								.bit(1, BatterySoltaroClusterVersionB.ChannelId.RACK_1_CYCLE_OVER_CURRENT) //
								.bit(0, BatterySoltaroClusterVersionB.ChannelId.RACK_1_VOLTAGE_DIFFERENCE) //
						), //
						m(new BitsWordElement(0x1084, this) //
								.bit(5, BatterySoltaroClusterVersionB.ChannelId.RACK_2_LEVEL_2_ALARM) //
								.bit(4, BatterySoltaroClusterVersionB.ChannelId.RACK_2_PCS_CONTROL_FAULT) //
								.bit(3, BatterySoltaroClusterVersionB.ChannelId.RACK_2_COMMUNICATION_WITH_MASTER_ERROR) //
								.bit(2, BatterySoltaroClusterVersionB.ChannelId.RACK_2_DEVICE_ERROR) //
								.bit(1, BatterySoltaroClusterVersionB.ChannelId.RACK_2_CYCLE_OVER_CURRENT) //
								.bit(0, BatterySoltaroClusterVersionB.ChannelId.RACK_2_VOLTAGE_DIFFERENCE) //
						), //
						m(new BitsWordElement(0x1085, this) //
								.bit(5, BatterySoltaroClusterVersionB.ChannelId.RACK_3_LEVEL_2_ALARM) //
								.bit(4, BatterySoltaroClusterVersionB.ChannelId.RACK_3_PCS_CONTROL_FAULT) //
								.bit(3, BatterySoltaroClusterVersionB.ChannelId.RACK_3_COMMUNICATION_WITH_MASTER_ERROR) //
								.bit(2, BatterySoltaroClusterVersionB.ChannelId.RACK_3_DEVICE_ERROR) //
								.bit(1, BatterySoltaroClusterVersionB.ChannelId.RACK_3_CYCLE_OVER_CURRENT) //
								.bit(0, BatterySoltaroClusterVersionB.ChannelId.RACK_3_VOLTAGE_DIFFERENCE) //
						), //
						m(new BitsWordElement(0x1086, this) //
								.bit(5, BatterySoltaroClusterVersionB.ChannelId.RACK_4_LEVEL_2_ALARM) //
								.bit(4, BatterySoltaroClusterVersionB.ChannelId.RACK_4_PCS_CONTROL_FAULT) //
								.bit(3, BatterySoltaroClusterVersionB.ChannelId.RACK_4_COMMUNICATION_WITH_MASTER_ERROR) //
								.bit(2, BatterySoltaroClusterVersionB.ChannelId.RACK_4_DEVICE_ERROR) //
								.bit(1, BatterySoltaroClusterVersionB.ChannelId.RACK_4_CYCLE_OVER_CURRENT) //
								.bit(0, BatterySoltaroClusterVersionB.ChannelId.RACK_4_VOLTAGE_DIFFERENCE) //
						), //
						m(new BitsWordElement(0x1087, this) //
								.bit(5, BatterySoltaroClusterVersionB.ChannelId.RACK_5_LEVEL_2_ALARM) //
								.bit(4, BatterySoltaroClusterVersionB.ChannelId.RACK_5_PCS_CONTROL_FAULT) //
								.bit(3, BatterySoltaroClusterVersionB.ChannelId.RACK_5_COMMUNICATION_WITH_MASTER_ERROR) //
								.bit(2, BatterySoltaroClusterVersionB.ChannelId.RACK_5_DEVICE_ERROR) //
								.bit(1, BatterySoltaroClusterVersionB.ChannelId.RACK_5_CYCLE_OVER_CURRENT) //
								.bit(0, BatterySoltaroClusterVersionB.ChannelId.RACK_5_VOLTAGE_DIFFERENCE) //
						))); //

		for (SingleRack rack : this.racks.values()) {
			protocol.addTasks(rack.getTasks().toArray(new Task[] {}));
		}

		return protocol;
	}

	/**
	 * This method is used as callback for Modbus register 0x1082, which is holding
	 * bitwise information on the communication status of each rack. This method
	 * parses the state for all active racks and sets the StateChannels (e.g.
	 * SoltaroCluster.ChannelId.SUB_MASTER_1_COMMUNICATION_FAILURE) accordingly.
	 */
	protected final Consumer<Integer> parseSubMasterCommunicationFailure = value -> {
		if (value == null) {
			// assume no error
			value = 0;
		}
		for (Entry<Integer, RackInfo> entry : RACK_INFO.entrySet()) {
			final boolean hasFailure;
			if (this.racks.containsKey(entry.getKey())) {
				// rack is active
				hasFailure = value << ~(entry.getKey() - 1) < 0;
			} else {
				// rack is inactive -> always unset failure
				hasFailure = false;
			}
			this.channel(entry.getValue().subMasterCommunicationAlarmChannelId).setNextValue(hasFailure);
		}
	};

	private int getAddressContactorControl(int addressOffsetRack) {
		return addressOffsetRack + OFFSET_CONTACTOR_CONTROL;
	}

	protected final <T extends ModbusElement> T map(io.openems.edge.common.channel.ChannelId channelId, T element) {
		return this.m(channelId, element);
	}

	protected final <T extends ModbusElement> T map(io.openems.edge.common.channel.ChannelId channelId, T element,
			ElementToChannelConverter converter) {
		return this.m(channelId, element, converter);
	}

	protected final BitsWordElement map(BitsWordElement bitsWordElement) {
		return super.m(bitsWordElement);
	}

	protected void recalculateSoc() {
		var i = 0;
		var soc = 0;

		for (SingleRack rack : this.racks.values()) {
			soc = soc + rack.getSoC();
			i++;
		}

		if (i > 0) {
			soc = soc / i;
		}
		this._setSoc(soc);
	}

	protected void recalculateMaxCellVoltage() {
		var max = Integer.MIN_VALUE;

		for (SingleRack rack : this.racks.values()) {
			max = Math.max(max, rack.getMaximalCellVoltage());
		}
		this._setMaxCellVoltage(max);
	}

	protected void recalculateMinCellVoltage() {
		var min = Integer.MAX_VALUE;

		for (SingleRack rack : this.racks.values()) {
			min = Math.min(min, rack.getMinimalCellVoltage());
		}
		this._setMinCellVoltage(min);
	}

	protected void recalculateMaxCellTemperature() {
		var max = Integer.MIN_VALUE;

		for (SingleRack rack : this.racks.values()) {
			max = Math.max(max, rack.getMaximalCellTemperature());
		}
		this._setMaxCellTemperature(max);
	}

	protected void recalculateMinCellTemperature() {
		var min = Integer.MAX_VALUE;

		for (SingleRack rack : this.racks.values()) {
			min = Math.min(min, rack.getMinimalCellTemperature());
		}
		this._setMinCellTemperature(min);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	private static Map<Integer, RackInfo> createRackInfo() {
		Map<Integer, RackInfo> map = new HashMap<>();
		map.put(1,
				new RackInfo(ADDRESS_OFFSET_RACK_1, SoltaroCluster.ChannelId.RACK_1_USAGE,
						BatterySoltaroClusterVersionB.ChannelId.RACK_1_POSITIVE_CONTACTOR,
						SoltaroCluster.ChannelId.SUB_MASTER_1_COMMUNICATION_FAILURE));
		map.put(2,
				new RackInfo(ADDRESS_OFFSET_RACK_2, SoltaroCluster.ChannelId.RACK_2_USAGE,
						BatterySoltaroClusterVersionB.ChannelId.RACK_2_POSITIVE_CONTACTOR,
						SoltaroCluster.ChannelId.SUB_MASTER_2_COMMUNICATION_FAILURE));
		map.put(3,
				new RackInfo(ADDRESS_OFFSET_RACK_3, SoltaroCluster.ChannelId.RACK_3_USAGE,
						BatterySoltaroClusterVersionB.ChannelId.RACK_3_POSITIVE_CONTACTOR,
						SoltaroCluster.ChannelId.SUB_MASTER_3_COMMUNICATION_FAILURE));
		map.put(4,
				new RackInfo(ADDRESS_OFFSET_RACK_4, SoltaroCluster.ChannelId.RACK_4_USAGE,
						BatterySoltaroClusterVersionB.ChannelId.RACK_4_POSITIVE_CONTACTOR,
						SoltaroCluster.ChannelId.SUB_MASTER_4_COMMUNICATION_FAILURE));
		map.put(5,
				new RackInfo(ADDRESS_OFFSET_RACK_5, SoltaroCluster.ChannelId.RACK_5_USAGE,
						BatterySoltaroClusterVersionB.ChannelId.RACK_5_POSITIVE_CONTACTOR,
						SoltaroCluster.ChannelId.SUB_MASTER_5_COMMUNICATION_FAILURE));

		return map;
	}

	// Helper class to get infos about connected racks
	private static class RackInfo {
		private final int addressOffset;
		private final SoltaroCluster.ChannelId usageChannelId;
		private final BatterySoltaroClusterVersionB.ChannelId positiveContactorChannelId;
		private final SoltaroCluster.ChannelId subMasterCommunicationAlarmChannelId;

		protected RackInfo(//
				int addressOffset, //
				SoltaroCluster.ChannelId usageChannelId, //
				BatterySoltaroClusterVersionB.ChannelId positiveContactorChannelId, //
				SoltaroCluster.ChannelId subMasterCommunicationAlarmChannelId //
		) {
			this.addressOffset = addressOffset;
			this.usageChannelId = usageChannelId;
			this.subMasterCommunicationAlarmChannelId = subMasterCommunicationAlarmChannelId;
			this.positiveContactorChannelId = positiveContactorChannelId;
		}
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		switch (value) {
		case START:
		case UNDEFINED:
			// Current implementation always starts the Battery by default in
			// handleStateMachine()
			break;
		case STOP:
			throw new NotImplementedException("'STOP' is not implemented for Soltaro Cluster Version B");
		}
	}
}
