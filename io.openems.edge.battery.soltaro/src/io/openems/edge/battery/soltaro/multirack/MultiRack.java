package io.openems.edge.battery.soltaro.multirack;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.BatteryState;
import io.openems.edge.battery.soltaro.multirack.Enums.ContactorControl;
import io.openems.edge.battery.soltaro.multirack.Enums.RackUsage;
import io.openems.edge.battery.soltaro.multirack.Enums.StartStop;
import io.openems.edge.battery.soltaro.versionb.ModuleParameters;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.Fenecon.MultiRack", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
)
public class MultiRack extends AbstractOpenemsModbusComponent implements Battery, OpenemsComponent, EventHandler {

	public static final int DISCHARGE_MAX_A = 0; // default value 0 to avoid damages
	public static final int CHARGE_MAX_A = 0; // default value 0 to avoid damages

	private static final int ADDRESS_OFFSET_RACK_1 = 0x2000;
	private static final int ADDRESS_OFFSET_RACK_2 = 0x3000;
	private static final int ADDRESS_OFFSET_RACK_3 = 0x4000;
	private static final int ADDRESS_OFFSET_RACK_4 = 0x5000;
	private static final int ADDRESS_OFFSET_RACK_5 = 0x6000;
	private static final int OFFSET_CONTACTOR_CONTROL = 0x10;

	private static final Map<Integer, RackInfo> RACK_INFO = createRackInfo();
	private final Logger log = LoggerFactory.getLogger(MultiRack.class);

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

	public MultiRack() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				MultiRackChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());

		this.config = config;
		this.modbusBridgeId = config.modbus_id();
		this.batteryState = config.batteryState();

		// Create racks dynamically
		for (int i : config.racks()) {
			this.racks.put(i, new SingleRack(i, config.numberOfSlaves(), RACK_INFO.get(i).addressOffset, this));
		}

		this.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).setNextValue(MultiRack.CHARGE_MAX_A);
		this.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT).setNextValue(MultiRack.DISCHARGE_MAX_A);
		this.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE)
				.setNextValue(this.config.numberOfSlaves() * ModuleParameters.MAX_VOLTAGE_MILLIVOLT.getValue() / 1000);
		this.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE)
				.setNextValue(this.config.numberOfSlaves() * ModuleParameters.MIN_VOLTAGE_MILLIVOLT.getValue() / 1000);
		this.channel(Battery.ChannelId.CAPACITY).setNextValue(this.config.racks().length * this.config.numberOfSlaves()
				* ModuleParameters.CAPACITY_WH.getValue() / 1000);
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

	private void handleStateMachine() {
		log.info("MultiRack.doNormalHandling(): State: " + this.getStateMachineState());
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
		// TODO define what is an error
		// should we look at the submasters for level 2 errors?
		if (readValueFromStateChannel(MultiRackChannelId.MASTER_ALARM_LEVEL_2_INSULATION)) {
			return true;
		}
		if (readValueFromStateChannel(MultiRackChannelId.MASTER_ALARM_PCS_EMS_CONTROL_FAIL)) {
			return true;
		}
		if (readValueFromStateChannel(MultiRackChannelId.MASTER_ALARM_PCS_EMS_COMMUNICATION_FAILURE)) {
			return true;
		}
		if (readValueFromStateChannel(MultiRackChannelId.MASTER_ALARM_COMMUNICATION_ERROR_WITH_SUBMASTER)) {
			return true;
		}

		return false;
	}

	public Channel<?> addChannel(io.openems.edge.common.channel.ChannelId channelId) {
		return super.addChannel(channelId);
	}

	private boolean readValueFromStateChannel(io.openems.edge.common.channel.ChannelId channelId) {
		StateChannel s = this.channel(channelId);
		Optional<Boolean> val = s.value().asOptional();
		return val.isPresent() && val.get();
	}

	private boolean isSystemStopped() {
		boolean ret = true;

		for (SingleRack rack : racks.values()) {
			ret = ret && ContactorControl.CUT_OFF == this
					.channel(RACK_INFO.get(rack.getRackNumber()).positiveContactorChannelId).value().asEnum();
		}

		return ret;
	}

	private boolean isSystemRunning() {
		boolean ret = true;

		for (SingleRack rack : racks.values()) {
			ret = ret && ContactorControl.ON_GRID == this
					.channel(RACK_INFO.get(rack.getRackNumber()).positiveContactorChannelId).value().asEnum();
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

		for (SingleRack rack : racks.values()) {
			EnumReadChannel channel = this.channel(RACK_INFO.get(rack.getRackNumber()).positiveContactorChannelId);
			Optional<Integer> val = channel.value().asOptional();
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
		EnumWriteChannel startStopChannel = this.channel(MultiRackChannelId.START_STOP);
		try {
			startStopChannel.setNextWriteValue(StartStop.START);
			// Only set the racks that are used
			for (int i = 1; i < 6; i++) {
				EnumWriteChannel rackUsageChannel = this.channel(RACK_INFO.get(i).usageChannelId);
				if (racks.containsKey(i)) {
					rackUsageChannel.setNextWriteValue(RackUsage.USED);
				} else {
					rackUsageChannel.setNextWriteValue(RackUsage.UNUSED);
				}
			}
		} catch (OpenemsNamedException e) {
			log.error("Error while trying to start system\n" + e.getMessage());
		}
	}

	private void stopSystem() {
		EnumWriteChannel startStopChannel = this.channel(MultiRackChannelId.START_STOP);
		try {
			startStopChannel.setNextWriteValue(StartStop.STOP);
			//write to all racks unused!!
			for (RackInfo r : RACK_INFO.values()) {
				EnumWriteChannel rackUsageChannel = this.channel(r.usageChannelId);
				rackUsageChannel.setNextWriteValue(RackUsage.UNUSED);
			}
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
		this.channel(MultiRackChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, new Task[] {
				// -------- control registers of master --------------------------------------
				new FC16WriteRegistersTask(0x1017, m(MultiRackChannelId.START_STOP, new UnsignedWordElement(0x1017)), //
						m(MultiRackChannelId.RACK_1_USAGE, new UnsignedWordElement(0x1018)), //
						m(MultiRackChannelId.RACK_2_USAGE, new UnsignedWordElement(0x1019)), //
						m(MultiRackChannelId.RACK_3_USAGE, new UnsignedWordElement(0x101A)), //
						m(MultiRackChannelId.RACK_4_USAGE, new UnsignedWordElement(0x101B)), //
						m(MultiRackChannelId.RACK_5_USAGE, new UnsignedWordElement(0x101C)) //
				), //
				new FC3ReadRegistersTask(0x1017, Priority.HIGH,
						m(MultiRackChannelId.START_STOP, new UnsignedWordElement(0x1017)), //
						m(MultiRackChannelId.RACK_1_USAGE, new UnsignedWordElement(0x1018)), //
						m(MultiRackChannelId.RACK_2_USAGE, new UnsignedWordElement(0x1019)), //
						m(MultiRackChannelId.RACK_3_USAGE, new UnsignedWordElement(0x101A)), //
						m(MultiRackChannelId.RACK_4_USAGE, new UnsignedWordElement(0x101B)), //
						m(MultiRackChannelId.RACK_5_USAGE, new UnsignedWordElement(0x101C)) //
				), //

				new FC16WriteRegistersTask(0x101F,
						m(MultiRackChannelId.SYSTEM_INSULATION_LEVEL_1, new UnsignedWordElement(0x101F)), //
						m(MultiRackChannelId.SYSTEM_INSULATION_LEVEL_2, new UnsignedWordElement(0x1020)), //
						new DummyRegisterElement(0x1021), //
						m(MultiRackChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x1022)), //
						m(MultiRackChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1023)) //
				), //

				new FC3ReadRegistersTask(0x101F, Priority.LOW,
						m(MultiRackChannelId.SYSTEM_INSULATION_LEVEL_1, new UnsignedWordElement(0x101F)), //
						m(MultiRackChannelId.SYSTEM_INSULATION_LEVEL_2, new UnsignedWordElement(0x1020)), //
						new DummyRegisterElement(0x1021), //
						m(MultiRackChannelId.EMS_COMMUNICATION_TIMEOUT, new UnsignedWordElement(0x1022)), //
						m(MultiRackChannelId.EMS_ADDRESS, new UnsignedWordElement(0x1023)) //
				), //

				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1),
						m(MultiRackChannelId.RACK_1_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1))) //
				), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1), Priority.HIGH,
						m(MultiRackChannelId.RACK_1_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_1))) //
				), //

				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2),
						m(MultiRackChannelId.RACK_2_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2))) //
				), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2), Priority.HIGH,
						m(MultiRackChannelId.RACK_2_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_2))) //
				), //

				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3),
						m(MultiRackChannelId.RACK_2_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3))) //
				), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3), Priority.HIGH,
						m(MultiRackChannelId.RACK_3_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_3))) //
				), //

				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4),
						m(MultiRackChannelId.RACK_4_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4))) //
				), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4), Priority.HIGH,
						m(MultiRackChannelId.RACK_4_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_4))) //
				), //

				new FC16WriteRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5),
						m(MultiRackChannelId.RACK_5_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5))) //
				), //
				new FC3ReadRegistersTask(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5), Priority.HIGH,
						m(MultiRackChannelId.RACK_5_POSITIVE_CONTACTOR,
								new UnsignedWordElement(this.getAddressContactorControl(ADDRESS_OFFSET_RACK_5))) //
				), //

				// -------- state registers of master --------------------------------------
				new FC3ReadRegistersTask(0x1044, Priority.LOW, //
						m(MultiRackChannelId.CHARGE_INDICATION, new UnsignedWordElement(0x1044)), //
						m(MultiRackChannelId.SYSTEM_CURRENT, new UnsignedWordElement(0x1045), //
								ElementToChannelConverter.SCALE_FACTOR_2), // TODO Check if scale factor is correct
						new DummyRegisterElement(0x1046), //
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x1047)), //
						m(MultiRackChannelId.SYSTEM_RUNNING_STATE, new UnsignedWordElement(0x1048)), //
						m(MultiRackChannelId.SYSTEM_VOLTAGE, new UnsignedWordElement(0x1049), //
								ElementToChannelConverter.SCALE_FACTOR_2) // TODO Check if scale factor is correct
				), //

				new FC3ReadRegistersTask(0x104A, Priority.HIGH, //
						m(MultiRackChannelId.SYSTEM_INSULATION, new UnsignedWordElement(0x104A)), //
						new DummyRegisterElement(0x104B, 0x104C), //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(0x104D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(0x104E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1) //
				), //

				new FC3ReadRegistersTask(0x1081, Priority.LOW, //
						bm(new UnsignedWordElement(0x1081)) //
								.m(MultiRackChannelId.MASTER_ALARM_LEVEL_2_INSULATION, 4) //
								.m(MultiRackChannelId.MASTER_ALARM_LEVEL_1_INSULATION, 3) //
								.m(MultiRackChannelId.MASTER_ALARM_PCS_EMS_CONTROL_FAIL, 2) //
								.m(MultiRackChannelId.MASTER_ALARM_PCS_EMS_COMMUNICATION_FAILURE, 1) //
								.m(MultiRackChannelId.MASTER_ALARM_COMMUNICATION_ERROR_WITH_SUBMASTER, 0) //
								.build(), //
						bm(new UnsignedWordElement(0x1082)) //
								.m(MultiRackChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_5, 4) //
								.m(MultiRackChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_4, 3) //
								.m(MultiRackChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_3, 2) //
								.m(MultiRackChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_2, 1) //
								.m(MultiRackChannelId.SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_1, 0) //
								.build(), //
						bm(new UnsignedWordElement(0x1083)) //
								.m(MultiRackChannelId.RACK_1_LEVEL_2_ALARM, 5) //
								.m(MultiRackChannelId.RACK_1_PCS_CONTROL_FAULT, 4) //
								.m(MultiRackChannelId.RACK_1_COMMUNICATION_WITH_MASTER_ERROR, 3) //
								.m(MultiRackChannelId.RACK_1_DEVICE_ERROR, 2) //
								.m(MultiRackChannelId.RACK_1_CYCLE_OVER_CURRENT, 1) //
								.m(MultiRackChannelId.RACK_1_VOLTAGE_DIFFERENCE, 0) //
								.build(), //
						bm(new UnsignedWordElement(0x1084)) //
								.m(MultiRackChannelId.RACK_2_LEVEL_2_ALARM, 5) //
								.m(MultiRackChannelId.RACK_2_PCS_CONTROL_FAULT, 4) //
								.m(MultiRackChannelId.RACK_2_COMMUNICATION_WITH_MASTER_ERROR, 3) //
								.m(MultiRackChannelId.RACK_2_DEVICE_ERROR, 2) //
								.m(MultiRackChannelId.RACK_2_CYCLE_OVER_CURRENT, 1) //
								.m(MultiRackChannelId.RACK_2_VOLTAGE_DIFFERENCE, 0) //
								.build(), //
						bm(new UnsignedWordElement(0x1085)) //
								.m(MultiRackChannelId.RACK_3_LEVEL_2_ALARM, 5) //
								.m(MultiRackChannelId.RACK_3_PCS_CONTROL_FAULT, 4) //
								.m(MultiRackChannelId.RACK_3_COMMUNICATION_WITH_MASTER_ERROR, 3) //
								.m(MultiRackChannelId.RACK_3_DEVICE_ERROR, 2) //
								.m(MultiRackChannelId.RACK_3_CYCLE_OVER_CURRENT, 1) //
								.m(MultiRackChannelId.RACK_3_VOLTAGE_DIFFERENCE, 0) //
								.build(), //
						bm(new UnsignedWordElement(0x1086)) //
								.m(MultiRackChannelId.RACK_4_LEVEL_2_ALARM, 5) //
								.m(MultiRackChannelId.RACK_4_PCS_CONTROL_FAULT, 4) //
								.m(MultiRackChannelId.RACK_4_COMMUNICATION_WITH_MASTER_ERROR, 3) //
								.m(MultiRackChannelId.RACK_4_DEVICE_ERROR, 2) //
								.m(MultiRackChannelId.RACK_4_CYCLE_OVER_CURRENT, 1) //
								.m(MultiRackChannelId.RACK_4_VOLTAGE_DIFFERENCE, 0) //
								.build(), //
						bm(new UnsignedWordElement(0x1087)) //
								.m(MultiRackChannelId.RACK_5_LEVEL_2_ALARM, 5) //
								.m(MultiRackChannelId.RACK_5_PCS_CONTROL_FAULT, 4) //
								.m(MultiRackChannelId.RACK_5_COMMUNICATION_WITH_MASTER_ERROR, 3) //
								.m(MultiRackChannelId.RACK_5_DEVICE_ERROR, 2) //
								.m(MultiRackChannelId.RACK_5_CYCLE_OVER_CURRENT, 1) //
								.m(MultiRackChannelId.RACK_5_VOLTAGE_DIFFERENCE, 0) //
								.build() //
				) //
		});

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

	protected final BitChannelMapper map(UnsignedWordElement element) {
		return this.bm(element);
	}

	private static Map<Integer, RackInfo> createRackInfo() {
		Map<Integer, RackInfo> map = new HashMap<Integer, RackInfo>();
		map.put(1, new RackInfo(ADDRESS_OFFSET_RACK_1, MultiRackChannelId.RACK_1_USAGE,
				MultiRackChannelId.RACK_1_POSITIVE_CONTACTOR));
		map.put(2, new RackInfo(ADDRESS_OFFSET_RACK_2, MultiRackChannelId.RACK_2_USAGE,
				MultiRackChannelId.RACK_2_POSITIVE_CONTACTOR));
		map.put(3, new RackInfo(ADDRESS_OFFSET_RACK_3, MultiRackChannelId.RACK_3_USAGE,
				MultiRackChannelId.RACK_3_POSITIVE_CONTACTOR));
		map.put(4, new RackInfo(ADDRESS_OFFSET_RACK_4, MultiRackChannelId.RACK_4_USAGE,
				MultiRackChannelId.RACK_4_POSITIVE_CONTACTOR));
		map.put(5, new RackInfo(ADDRESS_OFFSET_RACK_5, MultiRackChannelId.RACK_5_USAGE,
				MultiRackChannelId.RACK_5_POSITIVE_CONTACTOR));

		return map;
	}

	// Helper class to get infos about connected racks
	private static class RackInfo {
		int addressOffset;
		MultiRackChannelId usageChannelId;
		MultiRackChannelId positiveContactorChannelId;

		RackInfo(int addressOffset, MultiRackChannelId usageChannelId, MultiRackChannelId positiveContactorChannelId) {
			this.addressOffset = addressOffset;
			this.usageChannelId = usageChannelId;
			this.positiveContactorChannelId = positiveContactorChannelId;
		}
	}
}
