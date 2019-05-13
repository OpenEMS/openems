package io.openems.edge.battery.soltaro.cluster.versiona;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.BatteryState;
import io.openems.edge.battery.soltaro.State;
import io.openems.edge.battery.soltaro.cluster.versiona.Enums.RackUsage;
import io.openems.edge.battery.soltaro.cluster.versiona.Enums.StartStop;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.Soltaro.Cluster.VersionA", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class Cluster extends AbstractOpenemsModbusComponent implements Battery, OpenemsComponent, EventHandler {

	public static final int DISCHARGE_MIN_V = 696;
	public static final int CHARGE_MAX_V = 854;
	public static final int DISCHARGE_MAX_A = 0;
	public static final int CHARGE_MAX_A = 0;
	public static final Integer CAPACITY_KWH = 150;

	private final Logger log = LoggerFactory.getLogger(Cluster.class);
	private String modbusBridgeId;
	private BatteryState batteryState;
	@Reference
	protected ConfigurationAdmin cm;
	private State state = State.UNDEFINED;
	private Config config;

	public Cluster() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				ClusterChannelId.values() //
		);
		this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).setNextValue(Cluster.CHARGE_MAX_A);
		this.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE).setNextValue(Cluster.CHARGE_MAX_V);
		this.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT).setNextValue(Cluster.DISCHARGE_MAX_A);
		this.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE).setNextValue(Cluster.DISCHARGE_MIN_V);
		this.channel(Battery.ChannelId.CAPACITY).setNextValue(Cluster.CAPACITY_KWH);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		log.info("Cluster.activate()");
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());

		this.modbusBridgeId = config.modbus_id();
		this.batteryState = config.batteryState();
		this.state = State.UNDEFINED;
	}

	@SuppressWarnings("unchecked")
	private void recalcMaxCurrent() {
		int chargeMaxCurrent = 0;
		int dischargeMaxCurrent = 0;

		if (config.rack1IsUsed() && !config.rack2IsUsed() && !config.rack3IsUsed()) {
			// Only rack 1 is configured --> use max current of rack 1
			Optional<Integer> chargeMaxCurrentOpt = (Optional<Integer>) this
					.channel(ClusterChannelId.RACK_1_MAX_CHARGE_CURRENT).value().asOptional();
			Optional<Integer> dischargeMaxCurrentOpt = (Optional<Integer>) this
					.channel(ClusterChannelId.RACK_1_MAX_DISCHARGE_CURRENT).value().asOptional();
			if (chargeMaxCurrentOpt.isPresent()) {
				chargeMaxCurrent = chargeMaxCurrentOpt.get();
			}
			if (dischargeMaxCurrentOpt.isPresent()) {
				dischargeMaxCurrent = dischargeMaxCurrentOpt.get();
			}
		} else if (!config.rack1IsUsed() && config.rack2IsUsed() && !config.rack3IsUsed()) {
			// Only rack 2 is configured --> use max current of rack 2
			Optional<Integer> chargeMaxCurrentOpt = (Optional<Integer>) this
					.channel(ClusterChannelId.RACK_2_MAX_CHARGE_CURRENT).value().asOptional();
			Optional<Integer> dischargeMaxCurrentOpt = (Optional<Integer>) this
					.channel(ClusterChannelId.RACK_2_MAX_DISCHARGE_CURRENT).value().asOptional();
			if (chargeMaxCurrentOpt.isPresent()) {
				chargeMaxCurrent = chargeMaxCurrentOpt.get();
			}
			if (dischargeMaxCurrentOpt.isPresent()) {
				dischargeMaxCurrent = dischargeMaxCurrentOpt.get();
			}
		} else if (!config.rack1IsUsed() && !config.rack2IsUsed() && config.rack3IsUsed()) {
			// Only rack 3 is configured --> use max current of rack 3
			Optional<Integer> chargeMaxCurrentOpt = (Optional<Integer>) this
					.channel(ClusterChannelId.RACK_3_MAX_CHARGE_CURRENT).value().asOptional();
			Optional<Integer> dischargeMaxCurrentOpt = (Optional<Integer>) this
					.channel(ClusterChannelId.RACK_3_MAX_DISCHARGE_CURRENT).value().asOptional();
			if (chargeMaxCurrentOpt.isPresent()) {
				chargeMaxCurrent = chargeMaxCurrentOpt.get();
			}
			if (dischargeMaxCurrentOpt.isPresent()) {
				dischargeMaxCurrent = dischargeMaxCurrentOpt.get();
			}
		} else {
			// more than one rack is configured, use information of cluster
			Optional<Integer> chargeMaxCurrentOpt = (Optional<Integer>) this.channel(ClusterChannelId.CHARGE_MAX_CURRENT_CLUSTER)
					.value().asOptional();
			Optional<Integer> dischargeMaxCurrentOpt = (Optional<Integer>) this
					.channel(ClusterChannelId.DISCHARGE_MAX_CURRENT_CLUSTER).value().asOptional();
			if (chargeMaxCurrentOpt.isPresent()) {
				chargeMaxCurrent = chargeMaxCurrentOpt.get();
			}
			if (dischargeMaxCurrentOpt.isPresent()) {
				dischargeMaxCurrent = dischargeMaxCurrentOpt.get();
			}
		}

		this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).setNextValue(chargeMaxCurrent);
		this.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT).setNextValue(dischargeMaxCurrent);
		this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).nextProcessImage();
		this.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT).nextProcessImage();
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
			recalcMaxCurrent();
			handleStateMachine();
			break;
		case OFF:
			stopSystem();
			break;
		case ON:
			startSystem();
			break;
		case CONFIGURE:
			System.out.println("Cluster cannot be configured currently!");
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
		log.info("Cluster.doNormalHandling(): State: " + this.getStateMachineState());
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
			this.startSystem();
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
			this.stopSystem();
			this.setStateMachineState(State.OFF);
			break;
		}
		this.getReadyForWorking().setNextValue(readyForWorking);
	}

	private boolean isError() {
		if (readValueFromStateChannel(ClusterChannelId.MASTER_ALARM_PCS_OUT_OF_CONTROL))
			return true;

		if (config.rack1IsUsed()) {
			if (readValueFromStateChannel(ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_1_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_1_ALARM_LEVEL_2_CHA_CURRENT_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_VOLTAGE_LOW))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_1_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_1_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW))
				return true;
		}
		if (config.rack2IsUsed()) {
			if (readValueFromStateChannel(ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_2_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_2_ALARM_LEVEL_2_CHA_CURRENT_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_VOLTAGE_LOW))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_2_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_2_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW))
				return true;
		}
		if (config.rack3IsUsed()) {
			if (readValueFromStateChannel(ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_3_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_3_ALARM_LEVEL_2_CHA_CURRENT_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_VOLTAGE_LOW))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_3_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_3_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH))
				return true;
			if (readValueFromStateChannel(ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW))
				return true;
		}
		return false;
	}

	private boolean readValueFromStateChannel(io.openems.edge.common.channel.ChannelId channelId) {
		StateChannel s = this.channel(channelId);
		Optional<Boolean> val = s.value().asOptional();
		return val.isPresent() && val.get();
	}

	private boolean isSystemStopped() {
		// System if definitely stopped when all racks are stopped, if only one or two
		// racks are configured, the other ones
		// must also be stopped, otherwise status is not clear

		IntegerReadChannel rack1StateChannel = this.channel(ClusterChannelId.RACK_1_STATE);
		IntegerReadChannel rack2StateChannel = this.channel(ClusterChannelId.RACK_2_STATE);
		IntegerReadChannel rack3StateChannel = this.channel(ClusterChannelId.RACK_3_STATE);

		Optional<Integer> v1 = rack1StateChannel.value().asOptional();
		Optional<Integer> v2 = rack2StateChannel.value().asOptional();
		Optional<Integer> v3 = rack3StateChannel.value().asOptional();

		int stop = StartStop.STOP.getValue();
		boolean ret = v1.isPresent() && v2.isPresent() && v3.isPresent() && v1.get() == stop && v2.get() == stop
				&& v3.get() == stop;

		return ret;
	}

	private boolean isSystemRunning() {
		// System is running when the configured racks are running, other racks must be
		// stopped, otherwise
		// status is not definitively clear

		IntegerReadChannel rack1StateChannel = this.channel(ClusterChannelId.RACK_1_STATE);
		IntegerReadChannel rack2StateChannel = this.channel(ClusterChannelId.RACK_2_STATE);
		IntegerReadChannel rack3StateChannel = this.channel(ClusterChannelId.RACK_3_STATE);

		Optional<Integer> val1Opt = rack1StateChannel.value().asOptional();
		Optional<Integer> val2Opt = rack2StateChannel.value().asOptional();
		Optional<Integer> val3Opt = rack3StateChannel.value().asOptional();

		if (!val1Opt.isPresent() || !val2Opt.isPresent() || !val3Opt.isPresent()) {
			return false;
		}

		int v1 = val1Opt.get();
		int v2 = val2Opt.get();
		int v3 = val3Opt.get();

		int start = StartStop.START.getValue();
		int stop = StartStop.STOP.getValue();

		boolean ret = false;

		if (config.rack1IsUsed() && !config.rack2IsUsed() && !config.rack3IsUsed()) { // r1
			ret = (v1 == start && v2 == stop && v3 == stop);
		} else if (config.rack1IsUsed() && config.rack2IsUsed() && !config.rack3IsUsed()) { // r1 & r2
			ret = (v1 == start && v2 == start && v3 == stop);
		} else if (config.rack1IsUsed() && !config.rack2IsUsed() && config.rack3IsUsed()) { // r1 & r3
			ret = (v1 == start && v2 == stop && v3 == start);
		} else if (config.rack1IsUsed() && config.rack2IsUsed() && config.rack3IsUsed()) { // r1 & r2 & r3
			ret = (v1 == start && v2 == start && v3 == start);
		} else if (!config.rack1IsUsed() && config.rack2IsUsed() && !config.rack3IsUsed()) { // r2
			ret = (v1 == stop && v2 == start && v3 == stop);
		} else if (!config.rack1IsUsed() && config.rack2IsUsed() && config.rack3IsUsed()) { // r2 && r3
			ret = (v1 == stop && v2 == start && v3 == start);
		} else if (!config.rack1IsUsed() && !config.rack2IsUsed() && config.rack3IsUsed()) { // r3
			ret = (v1 == stop && v2 == stop && v3 == start);
		}

		return ret;
	}

	/**
	 * Checks whether system has an undefined state, e.g. rack 1 & 2 are configured,
	 * but only rack 1 is running. This state can only be reached at startup coming
	 * from state undefined
	 */
	private boolean isSystemStatePending() {
		boolean ret = true;
		if (ret && config.rack1IsUsed()) {
			IntegerReadChannel rack1StateChannel = this.channel(ClusterChannelId.RACK_1_STATE);
			Optional<Integer> val = rack1StateChannel.value().asOptional();
			ret = ret && val.isPresent();
		}

		if (ret && config.rack2IsUsed()) {
			IntegerReadChannel rack2StateChannel = this.channel(ClusterChannelId.RACK_2_STATE);
			Optional<Integer> val = rack2StateChannel.value().asOptional();
			ret = ret && val.isPresent();
		}

		if (ret && config.rack3IsUsed()) {
			IntegerReadChannel rack3StateChannel = this.channel(ClusterChannelId.RACK_3_STATE);
			Optional<Integer> val = rack3StateChannel.value().asOptional();
			ret = ret && val.isPresent();
		}

		return ret && !isSystemRunning() && !isSystemStopped();
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value() //
				+ "|Discharge:" + this.getDischargeMinVoltage().value() + ";" + this.getDischargeMaxCurrent().value() //
				+ "|Charge:" + this.getChargeMaxVoltage().value() + ";" + this.getChargeMaxCurrent().value();
	}

	private void startSystem() {
		IntegerWriteChannel rack1UsageChannel = this.channel(ClusterChannelId.RACK_1_USAGE);
		IntegerWriteChannel rack2UsageChannel = this.channel(ClusterChannelId.RACK_2_USAGE);
		IntegerWriteChannel rack3UsageChannel = this.channel(ClusterChannelId.RACK_3_USAGE);

		IntegerWriteChannel startStopChannel = this.channel(ClusterChannelId.START_STOP);
		try {
			startStopChannel.setNextWriteValue(StartStop.START.getValue());

			if (config.rack1IsUsed()) {
				rack1UsageChannel.setNextWriteValue(RackUsage.USED.getValue());
			} else {
				rack1UsageChannel.setNextWriteValue(RackUsage.UNUSED.getValue());
			}
			if (config.rack2IsUsed()) {
				rack2UsageChannel.setNextWriteValue(RackUsage.USED.getValue());
			} else {
				rack2UsageChannel.setNextWriteValue(RackUsage.UNUSED.getValue());
			}
			if (config.rack3IsUsed()) {
				rack3UsageChannel.setNextWriteValue(RackUsage.USED.getValue());
			} else {
				rack3UsageChannel.setNextWriteValue(RackUsage.UNUSED.getValue());
			}
		} catch (OpenemsNamedException e) {
			log.error("Error while trying to start system\n" + e.getMessage());
		}
	}

	private void stopSystem() {

		IntegerWriteChannel startStopChannel = this.channel(ClusterChannelId.START_STOP);
		IntegerWriteChannel rack1UsageChannel = this.channel(ClusterChannelId.RACK_1_USAGE);
		IntegerWriteChannel rack2UsageChannel = this.channel(ClusterChannelId.RACK_2_USAGE);
		IntegerWriteChannel rack3UsageChannel = this.channel(ClusterChannelId.RACK_3_USAGE);
		try {
			startStopChannel.setNextWriteValue(StartStop.STOP.getValue());
			rack1UsageChannel.setNextWriteValue(RackUsage.UNUSED.getValue());
			rack2UsageChannel.setNextWriteValue(RackUsage.UNUSED.getValue());
			rack3UsageChannel.setNextWriteValue(RackUsage.UNUSED.getValue());
		} catch (OpenemsNamedException e) {
			log.error("Error while trying to stop system\n" + e.getMessage());
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

	private static final int BASE_ADDRESS_RACK_1 = 0x2000;
	private static final int BASE_ADDRESS_RACK_2 = 0x3000;
	private static final int BASE_ADDRESS_RACK_3 = 0x4000;

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		Collection<Task> tasks = new ArrayList<>();
		tasks.addAll(Arrays.asList(new Task[] {
				// -------- Registers of master --------------------------------------
				new FC16WriteRegistersTask(0x1017, m(ClusterChannelId.START_STOP, new UnsignedWordElement(0x1017)), //
						m(ClusterChannelId.RACK_1_USAGE, new UnsignedWordElement(0x1018)), //
						m(ClusterChannelId.RACK_2_USAGE, new UnsignedWordElement(0x1019)), //
						m(ClusterChannelId.RACK_3_USAGE, new UnsignedWordElement(0x101A)) //
				), //
				new FC3ReadRegistersTask(0x1017, Priority.HIGH,
						m(ClusterChannelId.START_STOP, new UnsignedWordElement(0x1017)), //
						m(ClusterChannelId.RACK_1_USAGE, new UnsignedWordElement(0x1018)), //
						m(ClusterChannelId.RACK_2_USAGE, new UnsignedWordElement(0x1019)), //
						m(ClusterChannelId.RACK_3_USAGE, new UnsignedWordElement(0x101A)) //
				), //

				new FC3ReadRegistersTask(0x1044, Priority.LOW, //
						m(ClusterChannelId.CHARGE_INDICATION, new UnsignedWordElement(0x1044)), //
						m(Battery.ChannelId.CURRENT, new UnsignedWordElement(0x1045), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(0x1046, 0x1047), //
//						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x1047)), // SoC is not calculated correctly
						m(ClusterChannelId.SYSTEM_RUNNING_STATE, new UnsignedWordElement(0x1048)), //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(0x1049), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), //
				new FC3ReadRegistersTask(0x104D, Priority.HIGH, //
						m(ClusterChannelId.CHARGE_MAX_CURRENT_CLUSTER, new UnsignedWordElement(0x104D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(ClusterChannelId.DISCHARGE_MAX_CURRENT_CLUSTER, new UnsignedWordElement(0x104E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), //
				new FC3ReadRegistersTask(0x1081, Priority.LOW, //
						m(new BitsWordElement(0x1081, this) //
								.bit(1, ClusterChannelId.MASTER_ALARM_PCS_OUT_OF_CONTROL) //
								.bit(0, ClusterChannelId.MASTER_ALARM_PCS_COMMUNICATION_FAULT) //
						), //
						m(new BitsWordElement(0x1082, this) //
								.bit(0, ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_1) //
								.bit(1, ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_2) //
								.bit(2, ClusterChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_3) //
						) //
				) //
		}));

		// ---------------- registers of rack 1 -----------------------------
		tasks.addAll(Arrays.asList(new Task[] { new FC16WriteRegistersTask(BASE_ADDRESS_RACK_1 + 0x1, //
				m(ClusterChannelId.RACK_1_STATE, new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x1)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_1 + 0x1, Priority.HIGH, //
						m(ClusterChannelId.RACK_1_STATE, new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x1)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_1 + 0x100, Priority.LOW, //
						m(ClusterChannelId.RACK_1_VOLTAGE, new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x100), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(ClusterChannelId.RACK_1_CURRENT, new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x101), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(ClusterChannelId.RACK_1_CHARGE_INDICATION,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x102)), //
						m(ClusterChannelId.RACK_1_SOC,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x103).onUpdateCallback(val -> {
									recalculateSoc();
								})), //
						m(ClusterChannelId.RACK_1_SOH, new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x104)), //
						m(ClusterChannelId.RACK_1_MAX_CELL_VOLTAGE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x105)), //
						m(ClusterChannelId.RACK_1_MAX_CELL_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x106)), //
						m(ClusterChannelId.RACK_1_MIN_CELL_VOLTAGE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x107)), //
						m(ClusterChannelId.RACK_1_MIN_CELL_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x108)), //
						m(ClusterChannelId.RACK_1_MAX_CELL_TEMPERATURE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x109)), //
						m(ClusterChannelId.RACK_1_MAX_CELL_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x10A)), //
						m(ClusterChannelId.RACK_1_MIN_CELL_TEMPERATURE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x10B)), //
						m(ClusterChannelId.RACK_1_MIN_CELL_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x10C)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_1 + 0x140, Priority.LOW, //
						m(new BitsWordElement(BASE_ADDRESS_RACK_1 + 0x140, this) //
								.bit(0, ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH) //
								.bit(1, ClusterChannelId.RACK_1_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, ClusterChannelId.RACK_1_ALARM_LEVEL_2_CHA_CURRENT_HIGH) //
								.bit(3, ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_VOLTAGE_LOW) //
								.bit(4, ClusterChannelId.RACK_1_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW) //
								.bit(5, ClusterChannelId.RACK_1_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH) //
								.bit(6, ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH) //
								.bit(7, ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW) //
								.bit(14, ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, ClusterChannelId.RACK_1_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW) //
						), //
						m(new BitsWordElement(BASE_ADDRESS_RACK_1 + 0x141, this) //
								.bit(0, ClusterChannelId.RACK_1_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH) //
								.bit(1, ClusterChannelId.RACK_1_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, ClusterChannelId.RACK_1_ALARM_LEVEL_1_CHA_CURRENT_HIGH) //
								.bit(3, ClusterChannelId.RACK_1_ALARM_LEVEL_1_CELL_VOLTAGE_LOW) //
								.bit(4, ClusterChannelId.RACK_1_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW) //
								.bit(5, ClusterChannelId.RACK_1_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH) //
								.bit(6, ClusterChannelId.RACK_1_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH) //
								.bit(7, ClusterChannelId.RACK_1_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW) //
								.bit(8, ClusterChannelId.RACK_1_ALARM_LEVEL_1_SOC_LOW) //
								.bit(9, ClusterChannelId.RACK_1_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH) //
								.bit(11, ClusterChannelId.RACK_1_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH) //
								.bit(12, ClusterChannelId.RACK_1_ALARM_LEVEL_1_INSULATION_LOW) //
								.bit(13, ClusterChannelId.RACK_1_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH) //
								.bit(14, ClusterChannelId.RACK_1_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, ClusterChannelId.RACK_1_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW) //
						), //
						m(ClusterChannelId.RACK_1_RUN_STATE, new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x142)) //
				), //
				new FC3ReadRegistersTask(BASE_ADDRESS_RACK_1 + 0x160, Priority.HIGH, //
						m(ClusterChannelId.RACK_1_MAX_CHARGE_CURRENT,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x160),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(ClusterChannelId.RACK_1_MAX_DISCHARGE_CURRENT,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x161),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_1 + 0x185, Priority.LOW, //
						m(new BitsWordElement(BASE_ADDRESS_RACK_1 + 0x185, this) //
								.bit(0, ClusterChannelId.RACK_1_FAILURE_SAMPLING_WIRE)//
								.bit(1, ClusterChannelId.RACK_1_FAILURE_CONNECTOR_WIRE)//
								.bit(2, ClusterChannelId.RACK_1_FAILURE_LTC6803)//
								.bit(3, ClusterChannelId.RACK_1_FAILURE_VOLTAGE_SAMPLING)//
								.bit(4, ClusterChannelId.RACK_1_FAILURE_TEMP_SAMPLING)//
								.bit(5, ClusterChannelId.RACK_1_FAILURE_TEMP_SENSOR)//
								.bit(8, ClusterChannelId.RACK_1_FAILURE_BALANCING_MODULE)//
								.bit(9, ClusterChannelId.RACK_1_FAILURE_TEMP_SAMPLING_LINE)//
								.bit(10, ClusterChannelId.RACK_1_FAILURE_INTRANET_COMMUNICATION)//
								.bit(11, ClusterChannelId.RACK_1_FAILURE_EEPROM)//
								.bit(12, ClusterChannelId.RACK_1_FAILURE_INITIALIZATION)//
						) //
				), //
				new FC3ReadRegistersTask(BASE_ADDRESS_RACK_1 + 0x800, Priority.LOW, //
						m(ClusterChannelId.RACK_1_BATTERY_000_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x800)), //
						m(ClusterChannelId.RACK_1_BATTERY_001_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x801)), //
						m(ClusterChannelId.RACK_1_BATTERY_002_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x802)), //
						m(ClusterChannelId.RACK_1_BATTERY_003_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x803)), //
						m(ClusterChannelId.RACK_1_BATTERY_004_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x804)), //
						m(ClusterChannelId.RACK_1_BATTERY_005_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x805)), //
						m(ClusterChannelId.RACK_1_BATTERY_006_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x806)), //
						m(ClusterChannelId.RACK_1_BATTERY_007_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x807)), //
						m(ClusterChannelId.RACK_1_BATTERY_008_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x808)), //
						m(ClusterChannelId.RACK_1_BATTERY_009_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x809)), //
						m(ClusterChannelId.RACK_1_BATTERY_010_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x80A)), //
						m(ClusterChannelId.RACK_1_BATTERY_011_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x80B)), //
						m(ClusterChannelId.RACK_1_BATTERY_012_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x80C)), //
						m(ClusterChannelId.RACK_1_BATTERY_013_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x80D)), //
						m(ClusterChannelId.RACK_1_BATTERY_014_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x80E)), //
						m(ClusterChannelId.RACK_1_BATTERY_015_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x80F)), //
						m(ClusterChannelId.RACK_1_BATTERY_016_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x810)), //
						m(ClusterChannelId.RACK_1_BATTERY_017_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x811)), //
						m(ClusterChannelId.RACK_1_BATTERY_018_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x812)), //
						m(ClusterChannelId.RACK_1_BATTERY_019_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x813)), //
						m(ClusterChannelId.RACK_1_BATTERY_020_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x814)), //
						m(ClusterChannelId.RACK_1_BATTERY_021_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x815)), //
						m(ClusterChannelId.RACK_1_BATTERY_022_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x816)), //
						m(ClusterChannelId.RACK_1_BATTERY_023_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x817)), //
						m(ClusterChannelId.RACK_1_BATTERY_024_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x818)), //
						m(ClusterChannelId.RACK_1_BATTERY_025_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x819)), //
						m(ClusterChannelId.RACK_1_BATTERY_026_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x81A)), //
						m(ClusterChannelId.RACK_1_BATTERY_027_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x81B)), //
						m(ClusterChannelId.RACK_1_BATTERY_028_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x81C)), //
						m(ClusterChannelId.RACK_1_BATTERY_029_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x81D)), //
						m(ClusterChannelId.RACK_1_BATTERY_030_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x81E)), //
						m(ClusterChannelId.RACK_1_BATTERY_031_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x81F)), //
						m(ClusterChannelId.RACK_1_BATTERY_032_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x820)), //
						m(ClusterChannelId.RACK_1_BATTERY_033_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x821)), //
						m(ClusterChannelId.RACK_1_BATTERY_034_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x822)), //
						m(ClusterChannelId.RACK_1_BATTERY_035_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x823)), //
						m(ClusterChannelId.RACK_1_BATTERY_036_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x824)), //
						m(ClusterChannelId.RACK_1_BATTERY_037_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x825)), //
						m(ClusterChannelId.RACK_1_BATTERY_038_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x826)), //
						m(ClusterChannelId.RACK_1_BATTERY_039_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x827)), //
						m(ClusterChannelId.RACK_1_BATTERY_040_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x828)), //
						m(ClusterChannelId.RACK_1_BATTERY_041_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x829)), //
						m(ClusterChannelId.RACK_1_BATTERY_042_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x82A)), //
						m(ClusterChannelId.RACK_1_BATTERY_043_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x82B)), //
						m(ClusterChannelId.RACK_1_BATTERY_044_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x82C)), //
						m(ClusterChannelId.RACK_1_BATTERY_045_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x82D)), //
						m(ClusterChannelId.RACK_1_BATTERY_046_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x82E)), //
						m(ClusterChannelId.RACK_1_BATTERY_047_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x82F)), //
						m(ClusterChannelId.RACK_1_BATTERY_048_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x830)), //
						m(ClusterChannelId.RACK_1_BATTERY_049_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x831)), //
						m(ClusterChannelId.RACK_1_BATTERY_050_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x832)), //
						m(ClusterChannelId.RACK_1_BATTERY_051_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x833)), //
						m(ClusterChannelId.RACK_1_BATTERY_052_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x834)), //
						m(ClusterChannelId.RACK_1_BATTERY_053_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x835)), //
						m(ClusterChannelId.RACK_1_BATTERY_054_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x836)), //
						m(ClusterChannelId.RACK_1_BATTERY_055_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x837)), //
						m(ClusterChannelId.RACK_1_BATTERY_056_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x838)), //
						m(ClusterChannelId.RACK_1_BATTERY_057_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x839)), //
						m(ClusterChannelId.RACK_1_BATTERY_058_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x83A)), //
						m(ClusterChannelId.RACK_1_BATTERY_059_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x83B)), //
						m(ClusterChannelId.RACK_1_BATTERY_060_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x83C)) //
				),

				new FC3ReadRegistersTask(BASE_ADDRESS_RACK_1 + 0x83D, Priority.LOW, //
						m(ClusterChannelId.RACK_1_BATTERY_061_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x83D)), //
						m(ClusterChannelId.RACK_1_BATTERY_062_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x83E)), //
						m(ClusterChannelId.RACK_1_BATTERY_063_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x83F)), //
						m(ClusterChannelId.RACK_1_BATTERY_064_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x840)), //
						m(ClusterChannelId.RACK_1_BATTERY_065_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x841)), //
						m(ClusterChannelId.RACK_1_BATTERY_066_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x842)), //
						m(ClusterChannelId.RACK_1_BATTERY_067_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x843)), //
						m(ClusterChannelId.RACK_1_BATTERY_068_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x844)), //
						m(ClusterChannelId.RACK_1_BATTERY_069_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x845)), //
						m(ClusterChannelId.RACK_1_BATTERY_070_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x846)), //
						m(ClusterChannelId.RACK_1_BATTERY_071_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x847)), //
						m(ClusterChannelId.RACK_1_BATTERY_072_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x848)), //
						m(ClusterChannelId.RACK_1_BATTERY_073_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x849)), //
						m(ClusterChannelId.RACK_1_BATTERY_074_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x84A)), //
						m(ClusterChannelId.RACK_1_BATTERY_075_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x84B)), //
						m(ClusterChannelId.RACK_1_BATTERY_076_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x84C)), //
						m(ClusterChannelId.RACK_1_BATTERY_077_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x84D)), //
						m(ClusterChannelId.RACK_1_BATTERY_078_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x84E)), //
						m(ClusterChannelId.RACK_1_BATTERY_079_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x84F)), //
						m(ClusterChannelId.RACK_1_BATTERY_080_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x850)), //
						m(ClusterChannelId.RACK_1_BATTERY_081_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x851)), //
						m(ClusterChannelId.RACK_1_BATTERY_082_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x852)), //
						m(ClusterChannelId.RACK_1_BATTERY_083_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x853)), //
						m(ClusterChannelId.RACK_1_BATTERY_084_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x854)), //
						m(ClusterChannelId.RACK_1_BATTERY_085_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x855)), //
						m(ClusterChannelId.RACK_1_BATTERY_086_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x856)), //
						m(ClusterChannelId.RACK_1_BATTERY_087_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x857)), //
						m(ClusterChannelId.RACK_1_BATTERY_088_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x858)), //
						m(ClusterChannelId.RACK_1_BATTERY_089_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x859)), //
						m(ClusterChannelId.RACK_1_BATTERY_090_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x85A)), //
						m(ClusterChannelId.RACK_1_BATTERY_091_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x85B)), //
						m(ClusterChannelId.RACK_1_BATTERY_092_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x85C)), //
						m(ClusterChannelId.RACK_1_BATTERY_093_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x85D)), //
						m(ClusterChannelId.RACK_1_BATTERY_094_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x85E)), //
						m(ClusterChannelId.RACK_1_BATTERY_095_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x85F)), //
						m(ClusterChannelId.RACK_1_BATTERY_096_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x860)), //
						m(ClusterChannelId.RACK_1_BATTERY_097_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x861)), //
						m(ClusterChannelId.RACK_1_BATTERY_098_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x862)), //
						m(ClusterChannelId.RACK_1_BATTERY_099_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x863)), //
						m(ClusterChannelId.RACK_1_BATTERY_100_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x864)), //
						m(ClusterChannelId.RACK_1_BATTERY_101_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x865)), //
						m(ClusterChannelId.RACK_1_BATTERY_102_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x866)), //
						m(ClusterChannelId.RACK_1_BATTERY_103_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x867)), //
						m(ClusterChannelId.RACK_1_BATTERY_104_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x868)), //
						m(ClusterChannelId.RACK_1_BATTERY_105_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x869)), //
						m(ClusterChannelId.RACK_1_BATTERY_106_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x86A)), //
						m(ClusterChannelId.RACK_1_BATTERY_107_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x86B)), //
						m(ClusterChannelId.RACK_1_BATTERY_108_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x86C)), //
						m(ClusterChannelId.RACK_1_BATTERY_109_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x86D)), //
						m(ClusterChannelId.RACK_1_BATTERY_110_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x86E)), //
						m(ClusterChannelId.RACK_1_BATTERY_111_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x86F)), //
						m(ClusterChannelId.RACK_1_BATTERY_112_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x870)), //
						m(ClusterChannelId.RACK_1_BATTERY_113_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x871)), //
						m(ClusterChannelId.RACK_1_BATTERY_114_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x872)), //
						m(ClusterChannelId.RACK_1_BATTERY_115_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x873)), //
						m(ClusterChannelId.RACK_1_BATTERY_116_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x874)), //
						m(ClusterChannelId.RACK_1_BATTERY_117_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x875)), //
						m(ClusterChannelId.RACK_1_BATTERY_118_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x876)), //
						m(ClusterChannelId.RACK_1_BATTERY_119_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x877)) //
				), //
				new FC3ReadRegistersTask(BASE_ADDRESS_RACK_1 + 0x878, Priority.LOW, //
						m(ClusterChannelId.RACK_1_BATTERY_120_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x878)), //
						m(ClusterChannelId.RACK_1_BATTERY_121_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x879)), //
						m(ClusterChannelId.RACK_1_BATTERY_122_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x87A)), //
						m(ClusterChannelId.RACK_1_BATTERY_123_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x87B)), //
						m(ClusterChannelId.RACK_1_BATTERY_124_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x87C)), //
						m(ClusterChannelId.RACK_1_BATTERY_125_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x87D)), //
						m(ClusterChannelId.RACK_1_BATTERY_126_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x87E)), //
						m(ClusterChannelId.RACK_1_BATTERY_127_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x87F)), //
						m(ClusterChannelId.RACK_1_BATTERY_128_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x880)), //
						m(ClusterChannelId.RACK_1_BATTERY_129_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x881)), //
						m(ClusterChannelId.RACK_1_BATTERY_130_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x882)), //
						m(ClusterChannelId.RACK_1_BATTERY_131_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x883)), //
						m(ClusterChannelId.RACK_1_BATTERY_132_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x884)), //
						m(ClusterChannelId.RACK_1_BATTERY_133_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x885)), //
						m(ClusterChannelId.RACK_1_BATTERY_134_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x886)), //
						m(ClusterChannelId.RACK_1_BATTERY_135_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x887)), //
						m(ClusterChannelId.RACK_1_BATTERY_136_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x888)), //
						m(ClusterChannelId.RACK_1_BATTERY_137_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x889)), //
						m(ClusterChannelId.RACK_1_BATTERY_138_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x88A)), //
						m(ClusterChannelId.RACK_1_BATTERY_139_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x88B)), //
						m(ClusterChannelId.RACK_1_BATTERY_140_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x88C)), //
						m(ClusterChannelId.RACK_1_BATTERY_141_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x88D)), //
						m(ClusterChannelId.RACK_1_BATTERY_142_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x88E)), //
						m(ClusterChannelId.RACK_1_BATTERY_143_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x88F)), //
						m(ClusterChannelId.RACK_1_BATTERY_144_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x890)), //
						m(ClusterChannelId.RACK_1_BATTERY_145_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x891)), //
						m(ClusterChannelId.RACK_1_BATTERY_146_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x892)), //
						m(ClusterChannelId.RACK_1_BATTERY_147_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x893)), //
						m(ClusterChannelId.RACK_1_BATTERY_148_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x894)), //
						m(ClusterChannelId.RACK_1_BATTERY_149_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x895)), //
						m(ClusterChannelId.RACK_1_BATTERY_150_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x896)), //
						m(ClusterChannelId.RACK_1_BATTERY_151_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x897)), //
						m(ClusterChannelId.RACK_1_BATTERY_152_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x898)), //
						m(ClusterChannelId.RACK_1_BATTERY_153_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x899)), //
						m(ClusterChannelId.RACK_1_BATTERY_154_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x89A)), //
						m(ClusterChannelId.RACK_1_BATTERY_155_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x89B)), //
						m(ClusterChannelId.RACK_1_BATTERY_156_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x89C)), //
						m(ClusterChannelId.RACK_1_BATTERY_157_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x89D)), //
						m(ClusterChannelId.RACK_1_BATTERY_158_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x89E)), //
						m(ClusterChannelId.RACK_1_BATTERY_159_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x89F)), //
						m(ClusterChannelId.RACK_1_BATTERY_160_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8A0)), //
						m(ClusterChannelId.RACK_1_BATTERY_161_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8A1)), //
						m(ClusterChannelId.RACK_1_BATTERY_162_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8A2)), //
						m(ClusterChannelId.RACK_1_BATTERY_163_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8A3)), //
						m(ClusterChannelId.RACK_1_BATTERY_164_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8A4)), //
						m(ClusterChannelId.RACK_1_BATTERY_165_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8A5)), //
						m(ClusterChannelId.RACK_1_BATTERY_166_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8A6)), //
						m(ClusterChannelId.RACK_1_BATTERY_167_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8A7)), //
						m(ClusterChannelId.RACK_1_BATTERY_168_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8A8)), //
						m(ClusterChannelId.RACK_1_BATTERY_169_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8A9)), //
						m(ClusterChannelId.RACK_1_BATTERY_170_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8AA)), //
						m(ClusterChannelId.RACK_1_BATTERY_171_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8AB)), //
						m(ClusterChannelId.RACK_1_BATTERY_172_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8AC)), //
						m(ClusterChannelId.RACK_1_BATTERY_173_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8AD)), //
						m(ClusterChannelId.RACK_1_BATTERY_174_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8AE)), //
						m(ClusterChannelId.RACK_1_BATTERY_175_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8AF)), //
						m(ClusterChannelId.RACK_1_BATTERY_176_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8B0)), //
						m(ClusterChannelId.RACK_1_BATTERY_177_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8B1)), //
						m(ClusterChannelId.RACK_1_BATTERY_178_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8B2)), //
						m(ClusterChannelId.RACK_1_BATTERY_179_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8B3)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_1 + 0x8B4, Priority.LOW, //
						m(ClusterChannelId.RACK_1_BATTERY_180_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8B4)), //
						m(ClusterChannelId.RACK_1_BATTERY_181_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8B5)), //
						m(ClusterChannelId.RACK_1_BATTERY_182_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8B6)), //
						m(ClusterChannelId.RACK_1_BATTERY_183_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8B7)), //
						m(ClusterChannelId.RACK_1_BATTERY_184_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8B8)), //
						m(ClusterChannelId.RACK_1_BATTERY_185_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8B9)), //
						m(ClusterChannelId.RACK_1_BATTERY_186_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8BA)), //
						m(ClusterChannelId.RACK_1_BATTERY_187_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8BB)), //
						m(ClusterChannelId.RACK_1_BATTERY_188_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8BC)), //
						m(ClusterChannelId.RACK_1_BATTERY_189_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8BD)), //
						m(ClusterChannelId.RACK_1_BATTERY_190_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8BE)), //
						m(ClusterChannelId.RACK_1_BATTERY_191_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8BF)), //
						m(ClusterChannelId.RACK_1_BATTERY_192_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8C0)), //
						m(ClusterChannelId.RACK_1_BATTERY_193_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8C1)), //
						m(ClusterChannelId.RACK_1_BATTERY_194_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8C2)), //
						m(ClusterChannelId.RACK_1_BATTERY_195_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8C3)), //
						m(ClusterChannelId.RACK_1_BATTERY_196_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8C4)), //
						m(ClusterChannelId.RACK_1_BATTERY_197_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8C5)), //
						m(ClusterChannelId.RACK_1_BATTERY_198_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8C6)), //
						m(ClusterChannelId.RACK_1_BATTERY_199_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8C7)), //
						m(ClusterChannelId.RACK_1_BATTERY_200_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8C8)), //
						m(ClusterChannelId.RACK_1_BATTERY_201_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8C9)), //
						m(ClusterChannelId.RACK_1_BATTERY_202_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8CA)), //
						m(ClusterChannelId.RACK_1_BATTERY_203_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0x8CB)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_1 + 0xC00, Priority.LOW, //
						m(ClusterChannelId.RACK_1_BATTERY_000_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC00)), //
						m(ClusterChannelId.RACK_1_BATTERY_001_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC01)), //
						m(ClusterChannelId.RACK_1_BATTERY_002_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC02)), //
						m(ClusterChannelId.RACK_1_BATTERY_003_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC03)), //
						m(ClusterChannelId.RACK_1_BATTERY_004_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC04)), //
						m(ClusterChannelId.RACK_1_BATTERY_005_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC05)), //
						m(ClusterChannelId.RACK_1_BATTERY_006_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC06)), //
						m(ClusterChannelId.RACK_1_BATTERY_007_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC07)), //
						m(ClusterChannelId.RACK_1_BATTERY_008_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC08)), //
						m(ClusterChannelId.RACK_1_BATTERY_009_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC09)), //
						m(ClusterChannelId.RACK_1_BATTERY_010_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC0A)), //
						m(ClusterChannelId.RACK_1_BATTERY_011_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC0B)), //
						m(ClusterChannelId.RACK_1_BATTERY_012_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC0C)), //
						m(ClusterChannelId.RACK_1_BATTERY_013_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC0D)), //
						m(ClusterChannelId.RACK_1_BATTERY_014_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC0E)), //
						m(ClusterChannelId.RACK_1_BATTERY_015_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC0F)), //
						m(ClusterChannelId.RACK_1_BATTERY_016_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC10)), //
						m(ClusterChannelId.RACK_1_BATTERY_017_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC11)), //
						m(ClusterChannelId.RACK_1_BATTERY_018_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC12)), //
						m(ClusterChannelId.RACK_1_BATTERY_019_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC13)), //
						m(ClusterChannelId.RACK_1_BATTERY_020_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC14)), //
						m(ClusterChannelId.RACK_1_BATTERY_021_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC15)), //
						m(ClusterChannelId.RACK_1_BATTERY_022_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC16)), //
						m(ClusterChannelId.RACK_1_BATTERY_023_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC17)), //
						m(ClusterChannelId.RACK_1_BATTERY_024_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC18)), //
						m(ClusterChannelId.RACK_1_BATTERY_025_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC19)), //
						m(ClusterChannelId.RACK_1_BATTERY_026_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC1A)), //
						m(ClusterChannelId.RACK_1_BATTERY_027_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC1B)), //
						m(ClusterChannelId.RACK_1_BATTERY_028_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC1C)), //
						m(ClusterChannelId.RACK_1_BATTERY_029_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC1D)), //
						m(ClusterChannelId.RACK_1_BATTERY_030_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC1E)), //
						m(ClusterChannelId.RACK_1_BATTERY_031_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC1F)), //
						m(ClusterChannelId.RACK_1_BATTERY_032_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC20)), //
						m(ClusterChannelId.RACK_1_BATTERY_033_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC21)), //
						m(ClusterChannelId.RACK_1_BATTERY_034_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC22)), //
						m(ClusterChannelId.RACK_1_BATTERY_035_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC23)), //
						m(ClusterChannelId.RACK_1_BATTERY_036_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC24)), //
						m(ClusterChannelId.RACK_1_BATTERY_037_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC25)), //
						m(ClusterChannelId.RACK_1_BATTERY_038_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC26)), //
						m(ClusterChannelId.RACK_1_BATTERY_039_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC27)), //
						m(ClusterChannelId.RACK_1_BATTERY_040_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC28)), //
						m(ClusterChannelId.RACK_1_BATTERY_041_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC29)), //
						m(ClusterChannelId.RACK_1_BATTERY_042_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC2A)), //
						m(ClusterChannelId.RACK_1_BATTERY_043_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC2B)), //
						m(ClusterChannelId.RACK_1_BATTERY_044_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC2C)), //
						m(ClusterChannelId.RACK_1_BATTERY_045_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC2D)), //
						m(ClusterChannelId.RACK_1_BATTERY_046_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC2E)), //
						m(ClusterChannelId.RACK_1_BATTERY_047_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC2F)), //
						m(ClusterChannelId.RACK_1_BATTERY_048_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC30)), //
						m(ClusterChannelId.RACK_1_BATTERY_049_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC31)), //
						m(ClusterChannelId.RACK_1_BATTERY_050_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC32)), //
						m(ClusterChannelId.RACK_1_BATTERY_051_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC33)), //
						m(ClusterChannelId.RACK_1_BATTERY_052_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC34)), //
						m(ClusterChannelId.RACK_1_BATTERY_053_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC35)), //
						m(ClusterChannelId.RACK_1_BATTERY_054_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC36)), //
						m(ClusterChannelId.RACK_1_BATTERY_055_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC37)), //
						m(ClusterChannelId.RACK_1_BATTERY_056_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC38)), //
						m(ClusterChannelId.RACK_1_BATTERY_057_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC39)), //
						m(ClusterChannelId.RACK_1_BATTERY_058_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC3A)), //
						m(ClusterChannelId.RACK_1_BATTERY_059_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC3B)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_1 + 0xC3C, Priority.LOW, //
						m(ClusterChannelId.RACK_1_BATTERY_060_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC3C)), //
						m(ClusterChannelId.RACK_1_BATTERY_061_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC3D)), //
						m(ClusterChannelId.RACK_1_BATTERY_062_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC3E)), //
						m(ClusterChannelId.RACK_1_BATTERY_063_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC3F)), //
						m(ClusterChannelId.RACK_1_BATTERY_064_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC40)), //
						m(ClusterChannelId.RACK_1_BATTERY_065_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC41)), //
						m(ClusterChannelId.RACK_1_BATTERY_066_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC42)), //
						m(ClusterChannelId.RACK_1_BATTERY_067_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_1 + 0xC43)) //
				) //
		}));

		// ---------------- registers of rack 2 -----------------------------
		tasks.addAll(Arrays.asList(new Task[] { new FC16WriteRegistersTask(BASE_ADDRESS_RACK_2 + 0x1, //
				m(ClusterChannelId.RACK_2_STATE, new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x1)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_2 + 0x1, Priority.HIGH, //
						m(ClusterChannelId.RACK_2_STATE, new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x1)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_2 + 0x100, Priority.LOW, //
						m(ClusterChannelId.RACK_2_VOLTAGE, new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x100), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(ClusterChannelId.RACK_2_CURRENT, new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x101), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(ClusterChannelId.RACK_2_CHARGE_INDICATION,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x102)), //
						m(ClusterChannelId.RACK_2_SOC,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x103).onUpdateCallback(val -> {
									recalculateSoc();
								})), //
						m(ClusterChannelId.RACK_2_SOH, new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x104)), //
						m(ClusterChannelId.RACK_2_MAX_CELL_VOLTAGE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x105)), //
						m(ClusterChannelId.RACK_2_MAX_CELL_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x106)), //
						m(ClusterChannelId.RACK_2_MIN_CELL_VOLTAGE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x107)), //
						m(ClusterChannelId.RACK_2_MIN_CELL_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x108)), //
						m(ClusterChannelId.RACK_2_MAX_CELL_TEMPERATURE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x109)), //
						m(ClusterChannelId.RACK_2_MAX_CELL_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x10A)), //
						m(ClusterChannelId.RACK_2_MIN_CELL_TEMPERATURE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x10B)), //
						m(ClusterChannelId.RACK_2_MIN_CELL_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x10C)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_2 + 0x140, Priority.LOW, //
						m(new BitsWordElement(BASE_ADDRESS_RACK_2 + 0x140, this) //
								.bit(0, ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH) //
								.bit(1, ClusterChannelId.RACK_2_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, ClusterChannelId.RACK_2_ALARM_LEVEL_2_CHA_CURRENT_HIGH) //
								.bit(3, ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_VOLTAGE_LOW) //
								.bit(4, ClusterChannelId.RACK_2_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW) //
								.bit(5, ClusterChannelId.RACK_2_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH) //
								.bit(6, ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH) //
								.bit(7, ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW) //
								.bit(14, ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, ClusterChannelId.RACK_2_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW) //
						), //
						m(new BitsWordElement(BASE_ADDRESS_RACK_2 + 0x141, this) //
								.bit(0, ClusterChannelId.RACK_2_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH) //
								.bit(1, ClusterChannelId.RACK_2_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, ClusterChannelId.RACK_2_ALARM_LEVEL_1_CHA_CURRENT_HIGH) //
								.bit(3, ClusterChannelId.RACK_2_ALARM_LEVEL_1_CELL_VOLTAGE_LOW) //
								.bit(4, ClusterChannelId.RACK_2_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW) //
								.bit(5, ClusterChannelId.RACK_2_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH) //
								.bit(6, ClusterChannelId.RACK_2_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH) //
								.bit(7, ClusterChannelId.RACK_2_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW) //
								.bit(8, ClusterChannelId.RACK_2_ALARM_LEVEL_1_SOC_LOW) //
								.bit(9, ClusterChannelId.RACK_2_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH) //
								.bit(11, ClusterChannelId.RACK_2_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH) //
								.bit(12, ClusterChannelId.RACK_2_ALARM_LEVEL_1_INSULATION_LOW) //
								.bit(13, ClusterChannelId.RACK_2_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH) //
								.bit(14, ClusterChannelId.RACK_2_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, ClusterChannelId.RACK_2_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW) //
						), //
						m(ClusterChannelId.RACK_2_RUN_STATE, new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x142)) //
				), //
				new FC3ReadRegistersTask(BASE_ADDRESS_RACK_2 + 0x160, Priority.HIGH, //
						m(ClusterChannelId.RACK_2_MAX_CHARGE_CURRENT,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x160),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(ClusterChannelId.RACK_2_MAX_DISCHARGE_CURRENT,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x161),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_2 + 0x185, Priority.LOW, //
						m(new BitsWordElement(BASE_ADDRESS_RACK_2 + 0x185, this) //
								.bit(0, ClusterChannelId.RACK_2_FAILURE_SAMPLING_WIRE)//
								.bit(1, ClusterChannelId.RACK_2_FAILURE_CONNECTOR_WIRE)//
								.bit(2, ClusterChannelId.RACK_2_FAILURE_LTC6803)//
								.bit(3, ClusterChannelId.RACK_2_FAILURE_VOLTAGE_SAMPLING)//
								.bit(4, ClusterChannelId.RACK_2_FAILURE_TEMP_SAMPLING)//
								.bit(5, ClusterChannelId.RACK_2_FAILURE_TEMP_SENSOR)//
								.bit(8, ClusterChannelId.RACK_2_FAILURE_BALANCING_MODULE)//
								.bit(9, ClusterChannelId.RACK_2_FAILURE_TEMP_SAMPLING_LINE)//
								.bit(10, ClusterChannelId.RACK_2_FAILURE_INTRANET_COMMUNICATION)//
								.bit(11, ClusterChannelId.RACK_2_FAILURE_EEPROM)//
								.bit(12, ClusterChannelId.RACK_2_FAILURE_INITIALIZATION)//
						) //
				), //
				new FC3ReadRegistersTask(BASE_ADDRESS_RACK_2 + 0x800, Priority.LOW, //
						m(ClusterChannelId.RACK_2_BATTERY_000_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x800)), //
						m(ClusterChannelId.RACK_2_BATTERY_001_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x801)), //
						m(ClusterChannelId.RACK_2_BATTERY_002_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x802)), //
						m(ClusterChannelId.RACK_2_BATTERY_003_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x803)), //
						m(ClusterChannelId.RACK_2_BATTERY_004_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x804)), //
						m(ClusterChannelId.RACK_2_BATTERY_005_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x805)), //
						m(ClusterChannelId.RACK_2_BATTERY_006_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x806)), //
						m(ClusterChannelId.RACK_2_BATTERY_007_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x807)), //
						m(ClusterChannelId.RACK_2_BATTERY_008_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x808)), //
						m(ClusterChannelId.RACK_2_BATTERY_009_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x809)), //
						m(ClusterChannelId.RACK_2_BATTERY_010_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x80A)), //
						m(ClusterChannelId.RACK_2_BATTERY_011_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x80B)), //
						m(ClusterChannelId.RACK_2_BATTERY_012_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x80C)), //
						m(ClusterChannelId.RACK_2_BATTERY_013_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x80D)), //
						m(ClusterChannelId.RACK_2_BATTERY_014_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x80E)), //
						m(ClusterChannelId.RACK_2_BATTERY_015_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x80F)), //
						m(ClusterChannelId.RACK_2_BATTERY_016_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x810)), //
						m(ClusterChannelId.RACK_2_BATTERY_017_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x811)), //
						m(ClusterChannelId.RACK_2_BATTERY_018_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x812)), //
						m(ClusterChannelId.RACK_2_BATTERY_019_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x813)), //
						m(ClusterChannelId.RACK_2_BATTERY_020_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x814)), //
						m(ClusterChannelId.RACK_2_BATTERY_021_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x815)), //
						m(ClusterChannelId.RACK_2_BATTERY_022_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x816)), //
						m(ClusterChannelId.RACK_2_BATTERY_023_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x817)), //
						m(ClusterChannelId.RACK_2_BATTERY_024_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x818)), //
						m(ClusterChannelId.RACK_2_BATTERY_025_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x819)), //
						m(ClusterChannelId.RACK_2_BATTERY_026_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x81A)), //
						m(ClusterChannelId.RACK_2_BATTERY_027_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x81B)), //
						m(ClusterChannelId.RACK_2_BATTERY_028_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x81C)), //
						m(ClusterChannelId.RACK_2_BATTERY_029_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x81D)), //
						m(ClusterChannelId.RACK_2_BATTERY_030_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x81E)), //
						m(ClusterChannelId.RACK_2_BATTERY_031_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x81F)), //
						m(ClusterChannelId.RACK_2_BATTERY_032_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x820)), //
						m(ClusterChannelId.RACK_2_BATTERY_033_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x821)), //
						m(ClusterChannelId.RACK_2_BATTERY_034_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x822)), //
						m(ClusterChannelId.RACK_2_BATTERY_035_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x823)), //
						m(ClusterChannelId.RACK_2_BATTERY_036_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x824)), //
						m(ClusterChannelId.RACK_2_BATTERY_037_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x825)), //
						m(ClusterChannelId.RACK_2_BATTERY_038_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x826)), //
						m(ClusterChannelId.RACK_2_BATTERY_039_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x827)), //
						m(ClusterChannelId.RACK_2_BATTERY_040_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x828)), //
						m(ClusterChannelId.RACK_2_BATTERY_041_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x829)), //
						m(ClusterChannelId.RACK_2_BATTERY_042_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x82A)), //
						m(ClusterChannelId.RACK_2_BATTERY_043_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x82B)), //
						m(ClusterChannelId.RACK_2_BATTERY_044_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x82C)), //
						m(ClusterChannelId.RACK_2_BATTERY_045_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x82D)), //
						m(ClusterChannelId.RACK_2_BATTERY_046_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x82E)), //
						m(ClusterChannelId.RACK_2_BATTERY_047_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x82F)), //
						m(ClusterChannelId.RACK_2_BATTERY_048_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x830)), //
						m(ClusterChannelId.RACK_2_BATTERY_049_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x831)), //
						m(ClusterChannelId.RACK_2_BATTERY_050_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x832)), //
						m(ClusterChannelId.RACK_2_BATTERY_051_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x833)), //
						m(ClusterChannelId.RACK_2_BATTERY_052_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x834)), //
						m(ClusterChannelId.RACK_2_BATTERY_053_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x835)), //
						m(ClusterChannelId.RACK_2_BATTERY_054_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x836)), //
						m(ClusterChannelId.RACK_2_BATTERY_055_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x837)), //
						m(ClusterChannelId.RACK_2_BATTERY_056_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x838)), //
						m(ClusterChannelId.RACK_2_BATTERY_057_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x839)), //
						m(ClusterChannelId.RACK_2_BATTERY_058_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x83A)), //
						m(ClusterChannelId.RACK_2_BATTERY_059_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x83B)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_2 + 0x83C, Priority.LOW, //
						m(ClusterChannelId.RACK_2_BATTERY_060_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x83C)), //
						m(ClusterChannelId.RACK_2_BATTERY_061_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x83D)), //
						m(ClusterChannelId.RACK_2_BATTERY_062_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x83E)), //
						m(ClusterChannelId.RACK_2_BATTERY_063_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x83F)), //
						m(ClusterChannelId.RACK_2_BATTERY_064_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x840)), //
						m(ClusterChannelId.RACK_2_BATTERY_065_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x841)), //
						m(ClusterChannelId.RACK_2_BATTERY_066_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x842)), //
						m(ClusterChannelId.RACK_2_BATTERY_067_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x843)), //
						m(ClusterChannelId.RACK_2_BATTERY_068_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x844)), //
						m(ClusterChannelId.RACK_2_BATTERY_069_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x845)), //
						m(ClusterChannelId.RACK_2_BATTERY_070_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x846)), //
						m(ClusterChannelId.RACK_2_BATTERY_071_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x847)), //
						m(ClusterChannelId.RACK_2_BATTERY_072_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x848)), //
						m(ClusterChannelId.RACK_2_BATTERY_073_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x849)), //
						m(ClusterChannelId.RACK_2_BATTERY_074_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x84A)), //
						m(ClusterChannelId.RACK_2_BATTERY_075_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x84B)), //
						m(ClusterChannelId.RACK_2_BATTERY_076_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x84C)), //
						m(ClusterChannelId.RACK_2_BATTERY_077_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x84D)), //
						m(ClusterChannelId.RACK_2_BATTERY_078_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x84E)), //
						m(ClusterChannelId.RACK_2_BATTERY_079_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x84F)), //
						m(ClusterChannelId.RACK_2_BATTERY_080_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x850)), //
						m(ClusterChannelId.RACK_2_BATTERY_081_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x851)), //
						m(ClusterChannelId.RACK_2_BATTERY_082_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x852)), //
						m(ClusterChannelId.RACK_2_BATTERY_083_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x853)), //
						m(ClusterChannelId.RACK_2_BATTERY_084_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x854)), //
						m(ClusterChannelId.RACK_2_BATTERY_085_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x855)), //
						m(ClusterChannelId.RACK_2_BATTERY_086_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x856)), //
						m(ClusterChannelId.RACK_2_BATTERY_087_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x857)), //
						m(ClusterChannelId.RACK_2_BATTERY_088_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x858)), //
						m(ClusterChannelId.RACK_2_BATTERY_089_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x859)), //
						m(ClusterChannelId.RACK_2_BATTERY_090_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x85A)), //
						m(ClusterChannelId.RACK_2_BATTERY_091_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x85B)), //
						m(ClusterChannelId.RACK_2_BATTERY_092_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x85C)), //
						m(ClusterChannelId.RACK_2_BATTERY_093_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x85D)), //
						m(ClusterChannelId.RACK_2_BATTERY_094_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x85E)), //
						m(ClusterChannelId.RACK_2_BATTERY_095_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x85F)), //
						m(ClusterChannelId.RACK_2_BATTERY_096_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x860)), //
						m(ClusterChannelId.RACK_2_BATTERY_097_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x861)), //
						m(ClusterChannelId.RACK_2_BATTERY_098_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x862)), //
						m(ClusterChannelId.RACK_2_BATTERY_099_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x863)), //
						m(ClusterChannelId.RACK_2_BATTERY_100_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x864)), //
						m(ClusterChannelId.RACK_2_BATTERY_101_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x865)), //
						m(ClusterChannelId.RACK_2_BATTERY_102_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x866)), //
						m(ClusterChannelId.RACK_2_BATTERY_103_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x867)), //
						m(ClusterChannelId.RACK_2_BATTERY_104_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x868)), //
						m(ClusterChannelId.RACK_2_BATTERY_105_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x869)), //
						m(ClusterChannelId.RACK_2_BATTERY_106_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x86A)), //
						m(ClusterChannelId.RACK_2_BATTERY_107_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x86B)), //
						m(ClusterChannelId.RACK_2_BATTERY_108_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x86C)), //
						m(ClusterChannelId.RACK_2_BATTERY_109_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x86D)), //
						m(ClusterChannelId.RACK_2_BATTERY_110_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x86E)), //
						m(ClusterChannelId.RACK_2_BATTERY_111_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x86F)), //
						m(ClusterChannelId.RACK_2_BATTERY_112_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x870)), //
						m(ClusterChannelId.RACK_2_BATTERY_113_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x871)), //
						m(ClusterChannelId.RACK_2_BATTERY_114_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x872)), //
						m(ClusterChannelId.RACK_2_BATTERY_115_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x873)), //
						m(ClusterChannelId.RACK_2_BATTERY_116_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x874)), //
						m(ClusterChannelId.RACK_2_BATTERY_117_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x875)), //
						m(ClusterChannelId.RACK_2_BATTERY_118_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x876)), //
						m(ClusterChannelId.RACK_2_BATTERY_119_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x877)) //
				), //
				new FC3ReadRegistersTask(BASE_ADDRESS_RACK_2 + 0x878, Priority.LOW, //
						m(ClusterChannelId.RACK_2_BATTERY_120_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x878)), //
						m(ClusterChannelId.RACK_2_BATTERY_121_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x879)), //
						m(ClusterChannelId.RACK_2_BATTERY_122_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x87A)), //
						m(ClusterChannelId.RACK_2_BATTERY_123_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x87B)), //
						m(ClusterChannelId.RACK_2_BATTERY_124_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x87C)), //
						m(ClusterChannelId.RACK_2_BATTERY_125_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x87D)), //
						m(ClusterChannelId.RACK_2_BATTERY_126_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x87E)), //
						m(ClusterChannelId.RACK_2_BATTERY_127_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x87F)), //
						m(ClusterChannelId.RACK_2_BATTERY_128_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x880)), //
						m(ClusterChannelId.RACK_2_BATTERY_129_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x881)), //
						m(ClusterChannelId.RACK_2_BATTERY_130_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x882)), //
						m(ClusterChannelId.RACK_2_BATTERY_131_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x883)), //
						m(ClusterChannelId.RACK_2_BATTERY_132_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x884)), //
						m(ClusterChannelId.RACK_2_BATTERY_133_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x885)), //
						m(ClusterChannelId.RACK_2_BATTERY_134_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x886)), //
						m(ClusterChannelId.RACK_2_BATTERY_135_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x887)), //
						m(ClusterChannelId.RACK_2_BATTERY_136_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x888)), //
						m(ClusterChannelId.RACK_2_BATTERY_137_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x889)), //
						m(ClusterChannelId.RACK_2_BATTERY_138_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x88A)), //
						m(ClusterChannelId.RACK_2_BATTERY_139_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x88B)), //
						m(ClusterChannelId.RACK_2_BATTERY_140_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x88C)), //
						m(ClusterChannelId.RACK_2_BATTERY_141_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x88D)), //
						m(ClusterChannelId.RACK_2_BATTERY_142_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x88E)), //
						m(ClusterChannelId.RACK_2_BATTERY_143_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x88F)), //
						m(ClusterChannelId.RACK_2_BATTERY_144_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x890)), //
						m(ClusterChannelId.RACK_2_BATTERY_145_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x891)), //
						m(ClusterChannelId.RACK_2_BATTERY_146_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x892)), //
						m(ClusterChannelId.RACK_2_BATTERY_147_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x893)), //
						m(ClusterChannelId.RACK_2_BATTERY_148_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x894)), //
						m(ClusterChannelId.RACK_2_BATTERY_149_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x895)), //
						m(ClusterChannelId.RACK_2_BATTERY_150_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x896)), //
						m(ClusterChannelId.RACK_2_BATTERY_151_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x897)), //
						m(ClusterChannelId.RACK_2_BATTERY_152_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x898)), //
						m(ClusterChannelId.RACK_2_BATTERY_153_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x899)), //
						m(ClusterChannelId.RACK_2_BATTERY_154_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x89A)), //
						m(ClusterChannelId.RACK_2_BATTERY_155_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x89B)), //
						m(ClusterChannelId.RACK_2_BATTERY_156_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x89C)), //
						m(ClusterChannelId.RACK_2_BATTERY_157_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x89D)), //
						m(ClusterChannelId.RACK_2_BATTERY_158_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x89E)), //
						m(ClusterChannelId.RACK_2_BATTERY_159_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x89F)), //
						m(ClusterChannelId.RACK_2_BATTERY_160_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8A0)), //
						m(ClusterChannelId.RACK_2_BATTERY_161_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8A1)), //
						m(ClusterChannelId.RACK_2_BATTERY_162_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8A2)), //
						m(ClusterChannelId.RACK_2_BATTERY_163_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8A3)), //
						m(ClusterChannelId.RACK_2_BATTERY_164_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8A4)), //
						m(ClusterChannelId.RACK_2_BATTERY_165_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8A5)), //
						m(ClusterChannelId.RACK_2_BATTERY_166_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8A6)), //
						m(ClusterChannelId.RACK_2_BATTERY_167_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8A7)), //
						m(ClusterChannelId.RACK_2_BATTERY_168_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8A8)), //
						m(ClusterChannelId.RACK_2_BATTERY_169_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8A9)), //
						m(ClusterChannelId.RACK_2_BATTERY_170_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8AA)), //
						m(ClusterChannelId.RACK_2_BATTERY_171_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8AB)), //
						m(ClusterChannelId.RACK_2_BATTERY_172_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8AC)), //
						m(ClusterChannelId.RACK_2_BATTERY_173_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8AD)), //
						m(ClusterChannelId.RACK_2_BATTERY_174_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8AE)), //
						m(ClusterChannelId.RACK_2_BATTERY_175_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8AF)), //
						m(ClusterChannelId.RACK_2_BATTERY_176_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8B0)), //
						m(ClusterChannelId.RACK_2_BATTERY_177_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8B1)), //
						m(ClusterChannelId.RACK_2_BATTERY_178_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8B2)), //
						m(ClusterChannelId.RACK_2_BATTERY_179_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8B3)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_2 + 0x8B4, Priority.LOW, //
						m(ClusterChannelId.RACK_2_BATTERY_180_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8B4)), //
						m(ClusterChannelId.RACK_2_BATTERY_181_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8B5)), //
						m(ClusterChannelId.RACK_2_BATTERY_182_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8B6)), //
						m(ClusterChannelId.RACK_2_BATTERY_183_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8B7)), //
						m(ClusterChannelId.RACK_2_BATTERY_184_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8B8)), //
						m(ClusterChannelId.RACK_2_BATTERY_185_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8B9)), //
						m(ClusterChannelId.RACK_2_BATTERY_186_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8BA)), //
						m(ClusterChannelId.RACK_2_BATTERY_187_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8BB)), //
						m(ClusterChannelId.RACK_2_BATTERY_188_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8BC)), //
						m(ClusterChannelId.RACK_2_BATTERY_189_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8BD)), //
						m(ClusterChannelId.RACK_2_BATTERY_190_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8BE)), //
						m(ClusterChannelId.RACK_2_BATTERY_191_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8BF)), //
						m(ClusterChannelId.RACK_2_BATTERY_192_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8C0)), //
						m(ClusterChannelId.RACK_2_BATTERY_193_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8C1)), //
						m(ClusterChannelId.RACK_2_BATTERY_194_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8C2)), //
						m(ClusterChannelId.RACK_2_BATTERY_195_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8C3)), //
						m(ClusterChannelId.RACK_2_BATTERY_196_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8C4)), //
						m(ClusterChannelId.RACK_2_BATTERY_197_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8C5)), //
						m(ClusterChannelId.RACK_2_BATTERY_198_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8C6)), //
						m(ClusterChannelId.RACK_2_BATTERY_199_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8C7)), //
						m(ClusterChannelId.RACK_2_BATTERY_200_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8C8)), //
						m(ClusterChannelId.RACK_2_BATTERY_201_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8C9)), //
						m(ClusterChannelId.RACK_2_BATTERY_202_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8CA)), //
						m(ClusterChannelId.RACK_2_BATTERY_203_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0x8CB)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_2 + 0xC00, Priority.LOW, //
						m(ClusterChannelId.RACK_2_BATTERY_000_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC00)), //
						m(ClusterChannelId.RACK_2_BATTERY_001_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC01)), //
						m(ClusterChannelId.RACK_2_BATTERY_002_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC02)), //
						m(ClusterChannelId.RACK_2_BATTERY_003_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC03)), //
						m(ClusterChannelId.RACK_2_BATTERY_004_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC04)), //
						m(ClusterChannelId.RACK_2_BATTERY_005_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC05)), //
						m(ClusterChannelId.RACK_2_BATTERY_006_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC06)), //
						m(ClusterChannelId.RACK_2_BATTERY_007_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC07)), //
						m(ClusterChannelId.RACK_2_BATTERY_008_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC08)), //
						m(ClusterChannelId.RACK_2_BATTERY_009_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC09)), //
						m(ClusterChannelId.RACK_2_BATTERY_010_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC0A)), //
						m(ClusterChannelId.RACK_2_BATTERY_011_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC0B)), //
						m(ClusterChannelId.RACK_2_BATTERY_012_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC0C)), //
						m(ClusterChannelId.RACK_2_BATTERY_013_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC0D)), //
						m(ClusterChannelId.RACK_2_BATTERY_014_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC0E)), //
						m(ClusterChannelId.RACK_2_BATTERY_015_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC0F)), //
						m(ClusterChannelId.RACK_2_BATTERY_016_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC10)), //
						m(ClusterChannelId.RACK_2_BATTERY_017_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC11)), //
						m(ClusterChannelId.RACK_2_BATTERY_018_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC12)), //
						m(ClusterChannelId.RACK_2_BATTERY_019_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC13)), //
						m(ClusterChannelId.RACK_2_BATTERY_020_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC14)), //
						m(ClusterChannelId.RACK_2_BATTERY_021_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC15)), //
						m(ClusterChannelId.RACK_2_BATTERY_022_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC16)), //
						m(ClusterChannelId.RACK_2_BATTERY_023_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC17)), //
						m(ClusterChannelId.RACK_2_BATTERY_024_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC18)), //
						m(ClusterChannelId.RACK_2_BATTERY_025_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC19)), //
						m(ClusterChannelId.RACK_2_BATTERY_026_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC1A)), //
						m(ClusterChannelId.RACK_2_BATTERY_027_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC1B)), //
						m(ClusterChannelId.RACK_2_BATTERY_028_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC1C)), //
						m(ClusterChannelId.RACK_2_BATTERY_029_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC1D)), //
						m(ClusterChannelId.RACK_2_BATTERY_030_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC1E)), //
						m(ClusterChannelId.RACK_2_BATTERY_031_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC1F)), //
						m(ClusterChannelId.RACK_2_BATTERY_032_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC20)), //
						m(ClusterChannelId.RACK_2_BATTERY_033_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC21)), //
						m(ClusterChannelId.RACK_2_BATTERY_034_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC22)), //
						m(ClusterChannelId.RACK_2_BATTERY_035_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC23)), //
						m(ClusterChannelId.RACK_2_BATTERY_036_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC24)), //
						m(ClusterChannelId.RACK_2_BATTERY_037_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC25)), //
						m(ClusterChannelId.RACK_2_BATTERY_038_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC26)), //
						m(ClusterChannelId.RACK_2_BATTERY_039_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC27)), //
						m(ClusterChannelId.RACK_2_BATTERY_040_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC28)), //
						m(ClusterChannelId.RACK_2_BATTERY_041_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC29)), //
						m(ClusterChannelId.RACK_2_BATTERY_042_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC2A)), //
						m(ClusterChannelId.RACK_2_BATTERY_043_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC2B)), //
						m(ClusterChannelId.RACK_2_BATTERY_044_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC2C)), //
						m(ClusterChannelId.RACK_2_BATTERY_045_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC2D)), //
						m(ClusterChannelId.RACK_2_BATTERY_046_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC2E)), //
						m(ClusterChannelId.RACK_2_BATTERY_047_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC2F)), //
						m(ClusterChannelId.RACK_2_BATTERY_048_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC30)), //
						m(ClusterChannelId.RACK_2_BATTERY_049_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC31)), //
						m(ClusterChannelId.RACK_2_BATTERY_050_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC32)), //
						m(ClusterChannelId.RACK_2_BATTERY_051_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC33)), //
						m(ClusterChannelId.RACK_2_BATTERY_052_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC34)), //
						m(ClusterChannelId.RACK_2_BATTERY_053_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC35)), //
						m(ClusterChannelId.RACK_2_BATTERY_054_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC36)), //
						m(ClusterChannelId.RACK_2_BATTERY_055_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC37)), //
						m(ClusterChannelId.RACK_2_BATTERY_056_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC38)), //
						m(ClusterChannelId.RACK_2_BATTERY_057_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC39)), //
						m(ClusterChannelId.RACK_2_BATTERY_058_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC3A)), //
						m(ClusterChannelId.RACK_2_BATTERY_059_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC3B)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_2 + 0xC3C, Priority.LOW, //
						m(ClusterChannelId.RACK_2_BATTERY_060_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC3C)), //
						m(ClusterChannelId.RACK_2_BATTERY_061_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC3D)), //
						m(ClusterChannelId.RACK_2_BATTERY_062_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC3E)), //
						m(ClusterChannelId.RACK_2_BATTERY_063_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC3F)), //
						m(ClusterChannelId.RACK_2_BATTERY_064_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC40)), //
						m(ClusterChannelId.RACK_2_BATTERY_065_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC41)), //
						m(ClusterChannelId.RACK_2_BATTERY_066_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC42)), //
						m(ClusterChannelId.RACK_2_BATTERY_067_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_2 + 0xC43)) //
				) //
		}));

		// ---------------- registers of rack 3 -----------------------------
		tasks.addAll(Arrays.asList(new Task[] {

				new FC16WriteRegistersTask(BASE_ADDRESS_RACK_3 + 0x1, //
						m(ClusterChannelId.RACK_3_STATE, new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x1)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_3 + 0x1, Priority.HIGH, //
						m(ClusterChannelId.RACK_3_STATE, new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x1)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_3 + 0x100, Priority.LOW, //
						m(ClusterChannelId.RACK_3_VOLTAGE, new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x100), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(ClusterChannelId.RACK_3_CURRENT, new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x101), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(ClusterChannelId.RACK_3_CHARGE_INDICATION,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x102)), //
						m(ClusterChannelId.RACK_3_SOC,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x103).onUpdateCallback(val -> {
									recalculateSoc();
								})), //
						m(ClusterChannelId.RACK_3_SOH, new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x104)), //
						m(ClusterChannelId.RACK_3_MAX_CELL_VOLTAGE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x105)), //
						m(ClusterChannelId.RACK_3_MAX_CELL_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x106)), //
						m(ClusterChannelId.RACK_3_MIN_CELL_VOLTAGE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x107)), //
						m(ClusterChannelId.RACK_3_MIN_CELL_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x108)), //
						m(ClusterChannelId.RACK_3_MAX_CELL_TEMPERATURE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x109)), //
						m(ClusterChannelId.RACK_3_MAX_CELL_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x10A)), //
						m(ClusterChannelId.RACK_3_MIN_CELL_TEMPERATURE_ID,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x10B)), //
						m(ClusterChannelId.RACK_3_MIN_CELL_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x10C)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_3 + 0x140, Priority.LOW, //
						m(new BitsWordElement(BASE_ADDRESS_RACK_3 + 0x140, this) //
								.bit(0, ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH) //
								.bit(1, ClusterChannelId.RACK_3_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, ClusterChannelId.RACK_3_ALARM_LEVEL_2_CHA_CURRENT_HIGH) //
								.bit(3, ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_VOLTAGE_LOW) //
								.bit(4, ClusterChannelId.RACK_3_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW) //
								.bit(5, ClusterChannelId.RACK_3_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH) //
								.bit(6, ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH) //
								.bit(7, ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW) //
								.bit(14, ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, ClusterChannelId.RACK_3_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW) //
						), //
						m(new BitsWordElement(BASE_ADDRESS_RACK_3 + 0x141, this) //
								.bit(0, ClusterChannelId.RACK_3_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH) //
								.bit(1, ClusterChannelId.RACK_3_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, ClusterChannelId.RACK_3_ALARM_LEVEL_1_CHA_CURRENT_HIGH) //
								.bit(3, ClusterChannelId.RACK_3_ALARM_LEVEL_1_CELL_VOLTAGE_LOW) //
								.bit(4, ClusterChannelId.RACK_3_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW) //
								.bit(5, ClusterChannelId.RACK_3_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH) //
								.bit(6, ClusterChannelId.RACK_3_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH) //
								.bit(7, ClusterChannelId.RACK_3_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW) //
								.bit(8, ClusterChannelId.RACK_3_ALARM_LEVEL_1_SOC_LOW) //
								.bit(9, ClusterChannelId.RACK_3_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH) //
								.bit(11, ClusterChannelId.RACK_3_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH) //
								.bit(12, ClusterChannelId.RACK_3_ALARM_LEVEL_1_INSULATION_LOW) //
								.bit(13, ClusterChannelId.RACK_3_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH) //
								.bit(14, ClusterChannelId.RACK_3_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, ClusterChannelId.RACK_3_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW) //
						), //
						m(ClusterChannelId.RACK_3_RUN_STATE, new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x142)) //
				), //
				new FC3ReadRegistersTask(BASE_ADDRESS_RACK_3 + 0x160, Priority.HIGH, //
						m(ClusterChannelId.RACK_3_MAX_CHARGE_CURRENT,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x160),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(ClusterChannelId.RACK_3_MAX_DISCHARGE_CURRENT,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x161),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_3 + 0x185, Priority.LOW, //
						m(new BitsWordElement(BASE_ADDRESS_RACK_3 + 0x185, this) //
								.bit(0, ClusterChannelId.RACK_3_FAILURE_SAMPLING_WIRE)//
								.bit(1, ClusterChannelId.RACK_3_FAILURE_CONNECTOR_WIRE)//
								.bit(2, ClusterChannelId.RACK_3_FAILURE_LTC6803)//
								.bit(3, ClusterChannelId.RACK_3_FAILURE_VOLTAGE_SAMPLING)//
								.bit(4, ClusterChannelId.RACK_3_FAILURE_TEMP_SAMPLING)//
								.bit(5, ClusterChannelId.RACK_3_FAILURE_TEMP_SENSOR)//
								.bit(8, ClusterChannelId.RACK_3_FAILURE_BALANCING_MODULE)//
								.bit(9, ClusterChannelId.RACK_3_FAILURE_TEMP_SAMPLING_LINE)//
								.bit(10, ClusterChannelId.RACK_3_FAILURE_INTRANET_COMMUNICATION)//
								.bit(11, ClusterChannelId.RACK_3_FAILURE_EEPROM)//
								.bit(12, ClusterChannelId.RACK_3_FAILURE_INITIALIZATION)//
						) //
				), //
				new FC3ReadRegistersTask(BASE_ADDRESS_RACK_3 + 0x800, Priority.LOW, //
						m(ClusterChannelId.RACK_3_BATTERY_000_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x800)), //
						m(ClusterChannelId.RACK_3_BATTERY_001_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x801)), //
						m(ClusterChannelId.RACK_3_BATTERY_002_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x802)), //
						m(ClusterChannelId.RACK_3_BATTERY_003_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x803)), //
						m(ClusterChannelId.RACK_3_BATTERY_004_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x804)), //
						m(ClusterChannelId.RACK_3_BATTERY_005_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x805)), //
						m(ClusterChannelId.RACK_3_BATTERY_006_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x806)), //
						m(ClusterChannelId.RACK_3_BATTERY_007_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x807)), //
						m(ClusterChannelId.RACK_3_BATTERY_008_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x808)), //
						m(ClusterChannelId.RACK_3_BATTERY_009_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x809)), //
						m(ClusterChannelId.RACK_3_BATTERY_010_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x80A)), //
						m(ClusterChannelId.RACK_3_BATTERY_011_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x80B)), //
						m(ClusterChannelId.RACK_3_BATTERY_012_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x80C)), //
						m(ClusterChannelId.RACK_3_BATTERY_013_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x80D)), //
						m(ClusterChannelId.RACK_3_BATTERY_014_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x80E)), //
						m(ClusterChannelId.RACK_3_BATTERY_015_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x80F)), //
						m(ClusterChannelId.RACK_3_BATTERY_016_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x810)), //
						m(ClusterChannelId.RACK_3_BATTERY_017_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x811)), //
						m(ClusterChannelId.RACK_3_BATTERY_018_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x812)), //
						m(ClusterChannelId.RACK_3_BATTERY_019_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x813)), //
						m(ClusterChannelId.RACK_3_BATTERY_020_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x814)), //
						m(ClusterChannelId.RACK_3_BATTERY_021_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x815)), //
						m(ClusterChannelId.RACK_3_BATTERY_022_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x816)), //
						m(ClusterChannelId.RACK_3_BATTERY_023_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x817)), //
						m(ClusterChannelId.RACK_3_BATTERY_024_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x818)), //
						m(ClusterChannelId.RACK_3_BATTERY_025_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x819)), //
						m(ClusterChannelId.RACK_3_BATTERY_026_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x81A)), //
						m(ClusterChannelId.RACK_3_BATTERY_027_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x81B)), //
						m(ClusterChannelId.RACK_3_BATTERY_028_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x81C)), //
						m(ClusterChannelId.RACK_3_BATTERY_029_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x81D)), //
						m(ClusterChannelId.RACK_3_BATTERY_030_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x81E)), //
						m(ClusterChannelId.RACK_3_BATTERY_031_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x81F)), //
						m(ClusterChannelId.RACK_3_BATTERY_032_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x820)), //
						m(ClusterChannelId.RACK_3_BATTERY_033_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x821)), //
						m(ClusterChannelId.RACK_3_BATTERY_034_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x822)), //
						m(ClusterChannelId.RACK_3_BATTERY_035_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x823)), //
						m(ClusterChannelId.RACK_3_BATTERY_036_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x824)), //
						m(ClusterChannelId.RACK_3_BATTERY_037_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x825)), //
						m(ClusterChannelId.RACK_3_BATTERY_038_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x826)), //
						m(ClusterChannelId.RACK_3_BATTERY_039_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x827)), //
						m(ClusterChannelId.RACK_3_BATTERY_040_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x828)), //
						m(ClusterChannelId.RACK_3_BATTERY_041_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x829)), //
						m(ClusterChannelId.RACK_3_BATTERY_042_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x82A)), //
						m(ClusterChannelId.RACK_3_BATTERY_043_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x82B)), //
						m(ClusterChannelId.RACK_3_BATTERY_044_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x82C)), //
						m(ClusterChannelId.RACK_3_BATTERY_045_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x82D)), //
						m(ClusterChannelId.RACK_3_BATTERY_046_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x82E)), //
						m(ClusterChannelId.RACK_3_BATTERY_047_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x82F)), //
						m(ClusterChannelId.RACK_3_BATTERY_048_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x830)), //
						m(ClusterChannelId.RACK_3_BATTERY_049_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x831)), //
						m(ClusterChannelId.RACK_3_BATTERY_050_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x832)), //
						m(ClusterChannelId.RACK_3_BATTERY_051_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x833)), //
						m(ClusterChannelId.RACK_3_BATTERY_052_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x834)), //
						m(ClusterChannelId.RACK_3_BATTERY_053_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x835)), //
						m(ClusterChannelId.RACK_3_BATTERY_054_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x836)), //
						m(ClusterChannelId.RACK_3_BATTERY_055_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x837)), //
						m(ClusterChannelId.RACK_3_BATTERY_056_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x838)), //
						m(ClusterChannelId.RACK_3_BATTERY_057_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x839)), //
						m(ClusterChannelId.RACK_3_BATTERY_058_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x83A)), //
						m(ClusterChannelId.RACK_3_BATTERY_059_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x83B)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_3 + 0x83C, Priority.LOW, //
						m(ClusterChannelId.RACK_3_BATTERY_060_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x83C)), //
						m(ClusterChannelId.RACK_3_BATTERY_061_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x83D)), //
						m(ClusterChannelId.RACK_3_BATTERY_062_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x83E)), //
						m(ClusterChannelId.RACK_3_BATTERY_063_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x83F)), //
						m(ClusterChannelId.RACK_3_BATTERY_064_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x840)), //
						m(ClusterChannelId.RACK_3_BATTERY_065_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x841)), //
						m(ClusterChannelId.RACK_3_BATTERY_066_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x842)), //
						m(ClusterChannelId.RACK_3_BATTERY_067_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x843)), //
						m(ClusterChannelId.RACK_3_BATTERY_068_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x844)), //
						m(ClusterChannelId.RACK_3_BATTERY_069_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x845)), //
						m(ClusterChannelId.RACK_3_BATTERY_070_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x846)), //
						m(ClusterChannelId.RACK_3_BATTERY_071_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x847)), //
						m(ClusterChannelId.RACK_3_BATTERY_072_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x848)), //
						m(ClusterChannelId.RACK_3_BATTERY_073_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x849)), //
						m(ClusterChannelId.RACK_3_BATTERY_074_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x84A)), //
						m(ClusterChannelId.RACK_3_BATTERY_075_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x84B)), //
						m(ClusterChannelId.RACK_3_BATTERY_076_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x84C)), //
						m(ClusterChannelId.RACK_3_BATTERY_077_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x84D)), //
						m(ClusterChannelId.RACK_3_BATTERY_078_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x84E)), //
						m(ClusterChannelId.RACK_3_BATTERY_079_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x84F)), //
						m(ClusterChannelId.RACK_3_BATTERY_080_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x850)), //
						m(ClusterChannelId.RACK_3_BATTERY_081_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x851)), //
						m(ClusterChannelId.RACK_3_BATTERY_082_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x852)), //
						m(ClusterChannelId.RACK_3_BATTERY_083_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x853)), //
						m(ClusterChannelId.RACK_3_BATTERY_084_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x854)), //
						m(ClusterChannelId.RACK_3_BATTERY_085_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x855)), //
						m(ClusterChannelId.RACK_3_BATTERY_086_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x856)), //
						m(ClusterChannelId.RACK_3_BATTERY_087_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x857)), //
						m(ClusterChannelId.RACK_3_BATTERY_088_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x858)), //
						m(ClusterChannelId.RACK_3_BATTERY_089_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x859)), //
						m(ClusterChannelId.RACK_3_BATTERY_090_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x85A)), //
						m(ClusterChannelId.RACK_3_BATTERY_091_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x85B)), //
						m(ClusterChannelId.RACK_3_BATTERY_092_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x85C)), //
						m(ClusterChannelId.RACK_3_BATTERY_093_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x85D)), //
						m(ClusterChannelId.RACK_3_BATTERY_094_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x85E)), //
						m(ClusterChannelId.RACK_3_BATTERY_095_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x85F)), //
						m(ClusterChannelId.RACK_3_BATTERY_096_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x860)), //
						m(ClusterChannelId.RACK_3_BATTERY_097_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x861)), //
						m(ClusterChannelId.RACK_3_BATTERY_098_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x862)), //
						m(ClusterChannelId.RACK_3_BATTERY_099_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x863)), //
						m(ClusterChannelId.RACK_3_BATTERY_100_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x864)), //
						m(ClusterChannelId.RACK_3_BATTERY_101_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x865)), //
						m(ClusterChannelId.RACK_3_BATTERY_102_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x866)), //
						m(ClusterChannelId.RACK_3_BATTERY_103_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x867)), //
						m(ClusterChannelId.RACK_3_BATTERY_104_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x868)), //
						m(ClusterChannelId.RACK_3_BATTERY_105_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x869)), //
						m(ClusterChannelId.RACK_3_BATTERY_106_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x86A)), //
						m(ClusterChannelId.RACK_3_BATTERY_107_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x86B)), //
						m(ClusterChannelId.RACK_3_BATTERY_108_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x86C)), //
						m(ClusterChannelId.RACK_3_BATTERY_109_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x86D)), //
						m(ClusterChannelId.RACK_3_BATTERY_110_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x86E)), //
						m(ClusterChannelId.RACK_3_BATTERY_111_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x86F)), //
						m(ClusterChannelId.RACK_3_BATTERY_112_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x870)), //
						m(ClusterChannelId.RACK_3_BATTERY_113_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x871)), //
						m(ClusterChannelId.RACK_3_BATTERY_114_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x872)), //
						m(ClusterChannelId.RACK_3_BATTERY_115_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x873)), //
						m(ClusterChannelId.RACK_3_BATTERY_116_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x874)), //
						m(ClusterChannelId.RACK_3_BATTERY_117_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x875)), //
						m(ClusterChannelId.RACK_3_BATTERY_118_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x876)), //
						m(ClusterChannelId.RACK_3_BATTERY_119_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x877)) //
				), //
				new FC3ReadRegistersTask(BASE_ADDRESS_RACK_3 + 0x878, Priority.LOW, //
						m(ClusterChannelId.RACK_3_BATTERY_120_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x878)), //
						m(ClusterChannelId.RACK_3_BATTERY_121_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x879)), //
						m(ClusterChannelId.RACK_3_BATTERY_122_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x87A)), //
						m(ClusterChannelId.RACK_3_BATTERY_123_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x87B)), //
						m(ClusterChannelId.RACK_3_BATTERY_124_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x87C)), //
						m(ClusterChannelId.RACK_3_BATTERY_125_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x87D)), //
						m(ClusterChannelId.RACK_3_BATTERY_126_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x87E)), //
						m(ClusterChannelId.RACK_3_BATTERY_127_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x87F)), //
						m(ClusterChannelId.RACK_3_BATTERY_128_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x880)), //
						m(ClusterChannelId.RACK_3_BATTERY_129_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x881)), //
						m(ClusterChannelId.RACK_3_BATTERY_130_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x882)), //
						m(ClusterChannelId.RACK_3_BATTERY_131_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x883)), //
						m(ClusterChannelId.RACK_3_BATTERY_132_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x884)), //
						m(ClusterChannelId.RACK_3_BATTERY_133_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x885)), //
						m(ClusterChannelId.RACK_3_BATTERY_134_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x886)), //
						m(ClusterChannelId.RACK_3_BATTERY_135_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x887)), //
						m(ClusterChannelId.RACK_3_BATTERY_136_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x888)), //
						m(ClusterChannelId.RACK_3_BATTERY_137_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x889)), //
						m(ClusterChannelId.RACK_3_BATTERY_138_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x88A)), //
						m(ClusterChannelId.RACK_3_BATTERY_139_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x88B)), //
						m(ClusterChannelId.RACK_3_BATTERY_140_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x88C)), //
						m(ClusterChannelId.RACK_3_BATTERY_141_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x88D)), //
						m(ClusterChannelId.RACK_3_BATTERY_142_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x88E)), //
						m(ClusterChannelId.RACK_3_BATTERY_143_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x88F)), //
						m(ClusterChannelId.RACK_3_BATTERY_144_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x890)), //
						m(ClusterChannelId.RACK_3_BATTERY_145_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x891)), //
						m(ClusterChannelId.RACK_3_BATTERY_146_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x892)), //
						m(ClusterChannelId.RACK_3_BATTERY_147_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x893)), //
						m(ClusterChannelId.RACK_3_BATTERY_148_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x894)), //
						m(ClusterChannelId.RACK_3_BATTERY_149_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x895)), //
						m(ClusterChannelId.RACK_3_BATTERY_150_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x896)), //
						m(ClusterChannelId.RACK_3_BATTERY_151_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x897)), //
						m(ClusterChannelId.RACK_3_BATTERY_152_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x898)), //
						m(ClusterChannelId.RACK_3_BATTERY_153_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x899)), //
						m(ClusterChannelId.RACK_3_BATTERY_154_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x89A)), //
						m(ClusterChannelId.RACK_3_BATTERY_155_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x89B)), //
						m(ClusterChannelId.RACK_3_BATTERY_156_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x89C)), //
						m(ClusterChannelId.RACK_3_BATTERY_157_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x89D)), //
						m(ClusterChannelId.RACK_3_BATTERY_158_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x89E)), //
						m(ClusterChannelId.RACK_3_BATTERY_159_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x89F)), //
						m(ClusterChannelId.RACK_3_BATTERY_160_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8A0)), //
						m(ClusterChannelId.RACK_3_BATTERY_161_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8A1)), //
						m(ClusterChannelId.RACK_3_BATTERY_162_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8A2)), //
						m(ClusterChannelId.RACK_3_BATTERY_163_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8A3)), //
						m(ClusterChannelId.RACK_3_BATTERY_164_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8A4)), //
						m(ClusterChannelId.RACK_3_BATTERY_165_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8A5)), //
						m(ClusterChannelId.RACK_3_BATTERY_166_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8A6)), //
						m(ClusterChannelId.RACK_3_BATTERY_167_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8A7)), //
						m(ClusterChannelId.RACK_3_BATTERY_168_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8A8)), //
						m(ClusterChannelId.RACK_3_BATTERY_169_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8A9)), //
						m(ClusterChannelId.RACK_3_BATTERY_170_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8AA)), //
						m(ClusterChannelId.RACK_3_BATTERY_171_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8AB)), //
						m(ClusterChannelId.RACK_3_BATTERY_172_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8AC)), //
						m(ClusterChannelId.RACK_3_BATTERY_173_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8AD)), //
						m(ClusterChannelId.RACK_3_BATTERY_174_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8AE)), //
						m(ClusterChannelId.RACK_3_BATTERY_175_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8AF)), //
						m(ClusterChannelId.RACK_3_BATTERY_176_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8B0)), //
						m(ClusterChannelId.RACK_3_BATTERY_177_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8B1)), //
						m(ClusterChannelId.RACK_3_BATTERY_178_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8B2)), //
						m(ClusterChannelId.RACK_3_BATTERY_179_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8B3)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_3 + 0x8B4, Priority.LOW, //
						m(ClusterChannelId.RACK_3_BATTERY_180_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8B4)), //
						m(ClusterChannelId.RACK_3_BATTERY_181_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8B5)), //
						m(ClusterChannelId.RACK_3_BATTERY_182_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8B6)), //
						m(ClusterChannelId.RACK_3_BATTERY_183_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8B7)), //
						m(ClusterChannelId.RACK_3_BATTERY_184_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8B8)), //
						m(ClusterChannelId.RACK_3_BATTERY_185_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8B9)), //
						m(ClusterChannelId.RACK_3_BATTERY_186_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8BA)), //
						m(ClusterChannelId.RACK_3_BATTERY_187_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8BB)), //
						m(ClusterChannelId.RACK_3_BATTERY_188_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8BC)), //
						m(ClusterChannelId.RACK_3_BATTERY_189_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8BD)), //
						m(ClusterChannelId.RACK_3_BATTERY_190_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8BE)), //
						m(ClusterChannelId.RACK_3_BATTERY_191_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8BF)), //
						m(ClusterChannelId.RACK_3_BATTERY_192_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8C0)), //
						m(ClusterChannelId.RACK_3_BATTERY_193_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8C1)), //
						m(ClusterChannelId.RACK_3_BATTERY_194_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8C2)), //
						m(ClusterChannelId.RACK_3_BATTERY_195_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8C3)), //
						m(ClusterChannelId.RACK_3_BATTERY_196_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8C4)), //
						m(ClusterChannelId.RACK_3_BATTERY_197_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8C5)), //
						m(ClusterChannelId.RACK_3_BATTERY_198_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8C6)), //
						m(ClusterChannelId.RACK_3_BATTERY_199_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8C7)), //
						m(ClusterChannelId.RACK_3_BATTERY_200_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8C8)), //
						m(ClusterChannelId.RACK_3_BATTERY_201_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8C9)), //
						m(ClusterChannelId.RACK_3_BATTERY_202_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8CA)), //
						m(ClusterChannelId.RACK_3_BATTERY_203_VOLTAGE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0x8CB)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_3 + 0xC00, Priority.LOW, //
						m(ClusterChannelId.RACK_3_BATTERY_000_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC00)), //
						m(ClusterChannelId.RACK_3_BATTERY_001_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC01)), //
						m(ClusterChannelId.RACK_3_BATTERY_002_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC02)), //
						m(ClusterChannelId.RACK_3_BATTERY_003_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC03)), //
						m(ClusterChannelId.RACK_3_BATTERY_004_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC04)), //
						m(ClusterChannelId.RACK_3_BATTERY_005_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC05)), //
						m(ClusterChannelId.RACK_3_BATTERY_006_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC06)), //
						m(ClusterChannelId.RACK_3_BATTERY_007_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC07)), //
						m(ClusterChannelId.RACK_3_BATTERY_008_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC08)), //
						m(ClusterChannelId.RACK_3_BATTERY_009_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC09)), //
						m(ClusterChannelId.RACK_3_BATTERY_010_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC0A)), //
						m(ClusterChannelId.RACK_3_BATTERY_011_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC0B)), //
						m(ClusterChannelId.RACK_3_BATTERY_012_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC0C)), //
						m(ClusterChannelId.RACK_3_BATTERY_013_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC0D)), //
						m(ClusterChannelId.RACK_3_BATTERY_014_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC0E)), //
						m(ClusterChannelId.RACK_3_BATTERY_015_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC0F)), //
						m(ClusterChannelId.RACK_3_BATTERY_016_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC10)), //
						m(ClusterChannelId.RACK_3_BATTERY_017_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC11)), //
						m(ClusterChannelId.RACK_3_BATTERY_018_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC12)), //
						m(ClusterChannelId.RACK_3_BATTERY_019_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC13)), //
						m(ClusterChannelId.RACK_3_BATTERY_020_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC14)), //
						m(ClusterChannelId.RACK_3_BATTERY_021_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC15)), //
						m(ClusterChannelId.RACK_3_BATTERY_022_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC16)), //
						m(ClusterChannelId.RACK_3_BATTERY_023_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC17)), //
						m(ClusterChannelId.RACK_3_BATTERY_024_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC18)), //
						m(ClusterChannelId.RACK_3_BATTERY_025_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC19)), //
						m(ClusterChannelId.RACK_3_BATTERY_026_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC1A)), //
						m(ClusterChannelId.RACK_3_BATTERY_027_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC1B)), //
						m(ClusterChannelId.RACK_3_BATTERY_028_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC1C)), //
						m(ClusterChannelId.RACK_3_BATTERY_029_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC1D)), //
						m(ClusterChannelId.RACK_3_BATTERY_030_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC1E)), //
						m(ClusterChannelId.RACK_3_BATTERY_031_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC1F)), //
						m(ClusterChannelId.RACK_3_BATTERY_032_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC20)), //
						m(ClusterChannelId.RACK_3_BATTERY_033_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC21)), //
						m(ClusterChannelId.RACK_3_BATTERY_034_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC22)), //
						m(ClusterChannelId.RACK_3_BATTERY_035_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC23)), //
						m(ClusterChannelId.RACK_3_BATTERY_036_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC24)), //
						m(ClusterChannelId.RACK_3_BATTERY_037_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC25)), //
						m(ClusterChannelId.RACK_3_BATTERY_038_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC26)), //
						m(ClusterChannelId.RACK_3_BATTERY_039_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC27)), //
						m(ClusterChannelId.RACK_3_BATTERY_040_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC28)), //
						m(ClusterChannelId.RACK_3_BATTERY_041_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC29)), //
						m(ClusterChannelId.RACK_3_BATTERY_042_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC2A)), //
						m(ClusterChannelId.RACK_3_BATTERY_043_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC2B)), //
						m(ClusterChannelId.RACK_3_BATTERY_044_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC2C)), //
						m(ClusterChannelId.RACK_3_BATTERY_045_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC2D)), //
						m(ClusterChannelId.RACK_3_BATTERY_046_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC2E)), //
						m(ClusterChannelId.RACK_3_BATTERY_047_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC2F)), //
						m(ClusterChannelId.RACK_3_BATTERY_048_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC30)), //
						m(ClusterChannelId.RACK_3_BATTERY_049_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC31)), //
						m(ClusterChannelId.RACK_3_BATTERY_050_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC32)), //
						m(ClusterChannelId.RACK_3_BATTERY_051_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC33)), //
						m(ClusterChannelId.RACK_3_BATTERY_052_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC34)), //
						m(ClusterChannelId.RACK_3_BATTERY_053_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC35)), //
						m(ClusterChannelId.RACK_3_BATTERY_054_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC36)), //
						m(ClusterChannelId.RACK_3_BATTERY_055_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC37)), //
						m(ClusterChannelId.RACK_3_BATTERY_056_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC38)), //
						m(ClusterChannelId.RACK_3_BATTERY_057_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC39)), //
						m(ClusterChannelId.RACK_3_BATTERY_058_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC3A)), //
						m(ClusterChannelId.RACK_3_BATTERY_059_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC3B)) //
				), new FC3ReadRegistersTask(BASE_ADDRESS_RACK_3 + 0xC3C, Priority.LOW, //
						m(ClusterChannelId.RACK_3_BATTERY_060_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC3C)), //
						m(ClusterChannelId.RACK_3_BATTERY_061_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC3D)), //
						m(ClusterChannelId.RACK_3_BATTERY_062_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC3E)), //
						m(ClusterChannelId.RACK_3_BATTERY_063_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC3F)), //
						m(ClusterChannelId.RACK_3_BATTERY_064_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC40)), //
						m(ClusterChannelId.RACK_3_BATTERY_065_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC41)), //
						m(ClusterChannelId.RACK_3_BATTERY_066_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC42)), //
						m(ClusterChannelId.RACK_3_BATTERY_067_TEMPERATURE,
								new UnsignedWordElement(BASE_ADDRESS_RACK_3 + 0xC43)) //
				) //
		}));

		return new ModbusProtocol(this, tasks.toArray(new Task[0]));//

	}

	private void recalculateSoc() {
		int i = 0;
		int soc = 0;

		if (config.rack1IsUsed()) {
			IntegerReadChannel r = this.channel(ClusterChannelId.RACK_1_SOC);
			Optional<Integer> s = r.value().asOptional();
			if (s.isPresent()) {
				i++;
				soc = soc + s.get();
			}
		}

		if (config.rack2IsUsed()) {
			IntegerReadChannel r = this.channel(ClusterChannelId.RACK_2_SOC);
			Optional<Integer> s = r.value().asOptional();
			if (s.isPresent()) {
				i++;
				soc = soc + s.get();
			}
		}

		if (config.rack3IsUsed()) {
			IntegerReadChannel r = this.channel(ClusterChannelId.RACK_3_SOC);
			Optional<Integer> s = r.value().asOptional();
			if (s.isPresent()) {
				i++;
				soc = soc + s.get();
			}
		}

		if (i > 0) {
			soc = soc / i;
		}

		this.channel(Battery.ChannelId.SOC).setNextValue(soc);
	}
}
