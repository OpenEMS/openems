package io.openems.edge.battery.enfasbms;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.MULTIPLY;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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
import io.openems.edge.battery.enfasbms.statemachine.Context;
import io.openems.edge.battery.enfasbms.statemachine.StateMachine;
import io.openems.edge.battery.enfasbms.statemachine.StateMachine.State;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery.EnfasBms", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class EnfasBmsImpl extends AbstractOpenemsModbusComponent
		implements EnfasBms, ModbusComponent, OpenemsComponent, Battery, EventHandler, ModbusSlave, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(EnfasBmsImpl.class);
	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	/** Manages the {@link State}s of the StateMachine. */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private Config config = null;
	private BatteryProtection batteryProtection = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EnfasBmsImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryProtection.ChannelId.values(), //
				EnfasBms.ChannelId.values() //
		);
		this.registerCellVoltageListeners();
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new EnfasBmsBatteryProtection(), this.componentManager) //
				.build();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append(this.stateMachine.debugLog()) //
				.append("|SoC:").append(this.getSoc()) //
				.append("|Actual:").append(this.getVoltage()) //
				.append(";").append(this.getCurrent()) //
				.append("|Charge:").append(this.getChargeMaxVoltage()) //
				.append(";").append(this.getChargeMaxCurrent()) //
				.append("|Discharge:").append(this.getDischargeMinVoltage()) //
				.append(";").append(this.getDischargeMaxCurrent()) //
				.toString();
	}

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	@Override
	public StartStop getStartStopTarget() {
		return switch (this.config.startStop()) {
		case AUTO -> this.startStopTarget.get();
		case START -> StartStop.START;
		case STOP -> StartStop.STOP;
		};
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EnfasBms.class, accessMode, 100) //
						.build());
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
			this.handleStateMachine();
			break;
		}
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		this.channel(EnfasBms.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		try {
			Context context = new Context(this);

			// Call the StateMachine
			this.stateMachine.run(context);
			this.channel(EnfasBms.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(EnfasBms.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(1, Priority.HIGH, //
						m(EnfasBms.ChannelId.MODULE_0_AVG_CELL_VOLTAGE, new UnsignedWordElement(1)),
						m(EnfasBms.ChannelId.MODULE_0_MAX_CELL_VOLTAGE, new UnsignedWordElement(2)),
						m(EnfasBms.ChannelId.MODULE_0_MIN_CELL_VOLTAGE, new UnsignedWordElement(3)),
						m(EnfasBms.ChannelId.MODULE_1_AVG_CELL_VOLTAGE, new UnsignedWordElement(4)),
						m(EnfasBms.ChannelId.MODULE_1_MAX_CELL_VOLTAGE, new UnsignedWordElement(5)),
						m(EnfasBms.ChannelId.MODULE_1_MIN_CELL_VOLTAGE, new UnsignedWordElement(6)),
						m(EnfasBms.ChannelId.MODULE_2_AVG_CELL_VOLTAGE, new UnsignedWordElement(7)),
						m(EnfasBms.ChannelId.MODULE_2_MAX_CELL_VOLTAGE, new UnsignedWordElement(8)),
						m(EnfasBms.ChannelId.MODULE_2_MIN_CELL_VOLTAGE, new UnsignedWordElement(9)),
						m(EnfasBms.ChannelId.MODULE_3_AVG_CELL_VOLTAGE, new UnsignedWordElement(10)),
						m(EnfasBms.ChannelId.MODULE_3_MAX_CELL_VOLTAGE, new UnsignedWordElement(11)),
						m(EnfasBms.ChannelId.MODULE_3_MIN_CELL_VOLTAGE, new UnsignedWordElement(12)),
						m(EnfasBms.ChannelId.MODULE_4_AVG_CELL_VOLTAGE, new UnsignedWordElement(13)),
						m(EnfasBms.ChannelId.MODULE_4_MAX_CELL_VOLTAGE, new UnsignedWordElement(14)),
						m(EnfasBms.ChannelId.MODULE_4_MIN_CELL_VOLTAGE, new UnsignedWordElement(15)),
						m(EnfasBms.ChannelId.MODULE_5_AVG_CELL_VOLTAGE, new UnsignedWordElement(16)),
						m(EnfasBms.ChannelId.MODULE_5_MAX_CELL_VOLTAGE, new UnsignedWordElement(17)),
						m(EnfasBms.ChannelId.MODULE_5_MIN_CELL_VOLTAGE, new UnsignedWordElement(18)),
						m(EnfasBms.ChannelId.MODULE_6_AVG_CELL_VOLTAGE, new UnsignedWordElement(19)),
						m(EnfasBms.ChannelId.MODULE_6_MAX_CELL_VOLTAGE, new UnsignedWordElement(20)),
						m(EnfasBms.ChannelId.MODULE_6_MIN_CELL_VOLTAGE, new UnsignedWordElement(21)),
						m(EnfasBms.ChannelId.MODULE_7_AVG_CELL_VOLTAGE, new UnsignedWordElement(22)),
						m(EnfasBms.ChannelId.MODULE_7_MAX_CELL_VOLTAGE, new UnsignedWordElement(23)),
						m(EnfasBms.ChannelId.MODULE_7_MIN_CELL_VOLTAGE, new UnsignedWordElement(24)),
						m(EnfasBms.ChannelId.MODULE_8_AVG_CELL_VOLTAGE, new UnsignedWordElement(25)),
						m(EnfasBms.ChannelId.MODULE_8_MAX_CELL_VOLTAGE, new UnsignedWordElement(26)),
						m(EnfasBms.ChannelId.MODULE_8_MIN_CELL_VOLTAGE, new UnsignedWordElement(27)),
						m(EnfasBms.ChannelId.MODULE_0_AVG_TEMPERATURE, new UnsignedWordElement(28)),
						m(EnfasBms.ChannelId.MODULE_0_MAX_TEMPERATURE, new UnsignedWordElement(29)),
						m(EnfasBms.ChannelId.MODULE_0_MIN_TEMPERATURE, new UnsignedWordElement(30)),
						m(EnfasBms.ChannelId.MODULE_1_AVG_TEMPERATURE, new UnsignedWordElement(31)),
						m(EnfasBms.ChannelId.MODULE_1_MAX_TEMPERATURE, new UnsignedWordElement(32)),
						m(EnfasBms.ChannelId.MODULE_1_MIN_TEMPERATURE, new UnsignedWordElement(33)),
						m(EnfasBms.ChannelId.MODULE_2_AVG_TEMPERATURE, new UnsignedWordElement(34)),
						m(EnfasBms.ChannelId.MODULE_2_MAX_TEMPERATURE, new UnsignedWordElement(35)),
						m(EnfasBms.ChannelId.MODULE_2_MIN_TEMPERATURE, new UnsignedWordElement(36)),
						m(EnfasBms.ChannelId.MODULE_3_AVG_TEMPERATURE, new UnsignedWordElement(37)),
						m(EnfasBms.ChannelId.MODULE_3_MAX_TEMPERATURE, new UnsignedWordElement(38)),
						m(EnfasBms.ChannelId.MODULE_3_MIN_TEMPERATURE, new UnsignedWordElement(39)),
						m(EnfasBms.ChannelId.MODULE_4_AVG_TEMPERATURE, new UnsignedWordElement(40)),
						m(EnfasBms.ChannelId.MODULE_4_MAX_TEMPERATURE, new UnsignedWordElement(41)),
						m(EnfasBms.ChannelId.MODULE_4_MIN_TEMPERATURE, new UnsignedWordElement(42)),
						m(EnfasBms.ChannelId.MODULE_5_AVG_TEMPERATURE, new UnsignedWordElement(43)),
						m(EnfasBms.ChannelId.MODULE_5_MAX_TEMPERATURE, new UnsignedWordElement(44)),
						m(EnfasBms.ChannelId.MODULE_5_MIN_TEMPERATURE, new UnsignedWordElement(45)),
						m(EnfasBms.ChannelId.MODULE_6_AVG_TEMPERATURE, new UnsignedWordElement(46)),
						m(EnfasBms.ChannelId.MODULE_6_MAX_TEMPERATURE, new UnsignedWordElement(47)),
						m(EnfasBms.ChannelId.MODULE_6_MIN_TEMPERATURE, new UnsignedWordElement(48)),
						m(EnfasBms.ChannelId.MODULE_7_AVG_TEMPERATURE, new UnsignedWordElement(49)),
						m(EnfasBms.ChannelId.MODULE_7_MAX_TEMPERATURE, new UnsignedWordElement(50)),
						m(EnfasBms.ChannelId.MODULE_7_MIN_TEMPERATURE, new UnsignedWordElement(51)),
						m(EnfasBms.ChannelId.MODULE_8_AVG_TEMPERATURE, new UnsignedWordElement(52)),
						m(EnfasBms.ChannelId.MODULE_8_MAX_TEMPERATURE, new UnsignedWordElement(53)),
						m(EnfasBms.ChannelId.MODULE_8_MIN_TEMPERATURE, new UnsignedWordElement(54)),
						m(EnfasBms.ChannelId.MODULE_0_SOH, new UnsignedWordElement(55)),
						m(EnfasBms.ChannelId.MODULE_1_SOH, new UnsignedWordElement(56)),
						m(EnfasBms.ChannelId.MODULE_2_SOH, new UnsignedWordElement(57)),
						m(EnfasBms.ChannelId.MODULE_3_SOH, new UnsignedWordElement(58)),
						m(EnfasBms.ChannelId.MODULE_4_SOH, new UnsignedWordElement(59)),
						m(EnfasBms.ChannelId.MODULE_5_SOH, new UnsignedWordElement(60)),
						m(EnfasBms.ChannelId.MODULE_6_SOH, new UnsignedWordElement(61)),
						m(EnfasBms.ChannelId.MODULE_7_SOH, new UnsignedWordElement(62)),
						m(EnfasBms.ChannelId.MODULE_8_SOH, new UnsignedWordElement(63)),
						m(EnfasBms.ChannelId.MODULE_0_STATE_ENERGY, new UnsignedDoublewordElement(64),
								SCALE_FACTOR_MINUS_2),
						m(EnfasBms.ChannelId.MODULE_1_STATE_ENERGY, new UnsignedDoublewordElement(66), //
								SCALE_FACTOR_MINUS_2),
						m(EnfasBms.ChannelId.MODULE_2_STATE_ENERGY, new UnsignedDoublewordElement(68), //
								SCALE_FACTOR_MINUS_2),
						m(EnfasBms.ChannelId.MODULE_3_STATE_ENERGY, new UnsignedDoublewordElement(70), //
								SCALE_FACTOR_MINUS_2),
						m(EnfasBms.ChannelId.MODULE_4_STATE_ENERGY, new UnsignedDoublewordElement(72), //
								SCALE_FACTOR_MINUS_2),
						m(EnfasBms.ChannelId.MODULE_5_STATE_ENERGY, new UnsignedDoublewordElement(74), //
								SCALE_FACTOR_MINUS_2),
						m(EnfasBms.ChannelId.MODULE_6_STATE_ENERGY, new UnsignedDoublewordElement(76), //
								SCALE_FACTOR_MINUS_2),
						m(EnfasBms.ChannelId.MODULE_7_STATE_ENERGY, new UnsignedDoublewordElement(78), //
								SCALE_FACTOR_MINUS_2),
						m(EnfasBms.ChannelId.MODULE_8_STATE_ENERGY, new UnsignedDoublewordElement(80), //
								SCALE_FACTOR_MINUS_2),
						m(EnfasBms.ChannelId.SYSTEM_GLOBALSTATE, new UnsignedWordElement(82)), //
						m(EnfasBms.ChannelId.SYSTEM_EMERGENCY_SHUTDHOWN, new UnsignedWordElement(83)), //
						m(EnfasBms.ChannelId.SYSTEM_CONTACTOR_STATE, new UnsignedWordElement(84)), //
						m(EnfasBms.ChannelId.SYSTEM_BALANCING_STATE, new UnsignedWordElement(85)), //
						m(EnfasBms.ChannelId.SYSTEM_SAFETY_EVENTS, new UnsignedWordElement(86)), //
						m(EnfasBms.ChannelId.SYSTEM_WARNING_EVENTS, new UnsignedWordElement(87)), //
						m(Battery.ChannelId.CURRENT, new SignedDoublewordElement(88), //
								SCALE_FACTOR_MINUS_3), //
						m(EnfasBms.ChannelId.PACK_SHUNT_TEMPERATURE, new UnsignedWordElement(90)), //
						m(EnfasBms.ChannelId.PACK_PRECHARGE_TEMPERATURE, new UnsignedWordElement(91)), //
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(92)),
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS, new UnsignedWordElement(93)), //
						m(EnfasBms.ChannelId.PACK_LIMIT_MAX_CHARGE_POWER, new UnsignedWordElement(94), //
								MULTIPLY(0.2)),
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, new UnsignedWordElement(95)),
						m(EnfasBms.ChannelId.PACK_LIMIT_MAX_DISCHARGE_POWER, new UnsignedWordElement(96), //
								SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(97)), //
						m(Battery.ChannelId.SOC, new UnsignedWordElement(98)), //
						m(Battery.ChannelId.SOH, new UnsignedWordElement(99)), //
						m(EnfasBms.ChannelId.PACK_STATE_ENERGY, new UnsignedDoublewordElement(100), //
								SCALE_FACTOR_MINUS_2),
						m(Battery.ChannelId.VOLTAGE, new UnsignedDoublewordElement(102), //
								SCALE_FACTOR_MINUS_3),
						m(EnfasBms.ChannelId.PACK_HIGH_VOLTAGE_PLUS, new UnsignedWordElement(104), //
								SCALE_FACTOR_MINUS_1),
						m(EnfasBms.ChannelId.PACK_HIGH_VOLTAGE_MINUS, new UnsignedWordElement(105), //
								SCALE_FACTOR_MINUS_1),
						m(new BitsWordElement(106, this) //
								.bit(0, EnfasBms.ChannelId.STRING_EVENT_CELL_OT_CUTOFF)
								.bit(1, EnfasBms.ChannelId.STRING_EVENT_CELL_OT_MAX)
								.bit(2, EnfasBms.ChannelId.STRING_EVENT_CELL_OT_WARN)
								.bit(3, EnfasBms.ChannelId.STRING_EVENT_CELL_OV_CUTOFF)
								.bit(4, EnfasBms.ChannelId.STRING_EVENT_CELL_OV_MAX)
								.bit(5, EnfasBms.ChannelId.STRING_EVENT_CELL_OV_WARN)
								.bit(6, EnfasBms.ChannelId.STRING_EVENT_CELL_UT_CUTOFF)
								.bit(7, EnfasBms.ChannelId.STRING_EVENT_CELL_UT_MAX)
								.bit(8, EnfasBms.ChannelId.STRING_EVENT_CELL_UT_WARN)
								.bit(9, EnfasBms.ChannelId.STRING_EVENT_CELL_UV_CUTOFF)
								.bit(10, EnfasBms.ChannelId.STRING_EVENT_CELL_UV_MAX)
								.bit(11, EnfasBms.ChannelId.STRING_EVENT_CELL_UV_WARN)
								.bit(12, EnfasBms.ChannelId.STRING_EVENT_COMMUNICATION_ERR)
								.bit(13, EnfasBms.ChannelId.STRING_EVENT_CONT_NEGATIVE_ERR)
								.bit(14, EnfasBms.ChannelId.STRING_EVENT_CONT_POSITIVE_ERR)
								.bit(15, EnfasBms.ChannelId.STRING_EVENT_CURRENT_OC_CHARGE)),
						m(new BitsWordElement(107, this) //
								.bit(0, EnfasBms.ChannelId.STRING_EVENT_CURRENT_OC_DISCHARGE)
								.bit(1, EnfasBms.ChannelId.STRING_EVENT_CURRENT_MEASURE_ERROR)
								.bit(2, EnfasBms.ChannelId.STRING_EVENT_INSULATION_ERROR)
								.bit(3, EnfasBms.ChannelId.STRING_EVENT_INTERNAL_ERROR)
								.bit(4, EnfasBms.ChannelId.STRING_EVENT_SOC_ERROR)
								.bit(5, EnfasBms.ChannelId.STRING_EVENT_SOH_ERROR)
								.bit(6, EnfasBms.ChannelId.STRING_EVENT_STRING_CONNECTED)
								.bit(7, EnfasBms.ChannelId.STRING_EVENT_TEMP_DIFFERENCE_ERROR)), //
						m(new BitsWordElement(108, this) //
								.bit(0, EnfasBms.ChannelId.STRING_EVENT_BALANCING_ERROR)
								.bit(1, EnfasBms.ChannelId.STRING_EVENT_SOC_HIGH_ERROR)),
						m(EnfasBms.ChannelId.SOFTWARE_VERSION_MAJOR, //
								new UnsignedWordElement(109)), //
						m(EnfasBms.ChannelId.SOFTWARE_VERSION_MINOR, //
								new UnsignedWordElement(110)), //
						m(EnfasBms.ChannelId.SOFTWARE_VERSION_PATH, //
								new UnsignedWordElement(111)), //
						m(EnfasBms.ChannelId.CSC_INIT_BROKEN_AT, //
								new UnsignedWordElement(112)),
						m(EnfasBms.ChannelId.STRING_MODULE_COUNT, //
								new UnsignedWordElement(113))),

				new FC16WriteRegistersTask(1, //
						m(EnfasBms.ChannelId.COMMAND_STATE_REQUEST, //
								new UnsignedWordElement(1)),
						m(EnfasBms.ChannelId.COMMAND_ACTIVATE_BALANCING, //
								new UnsignedWordElement(2)), //
						m(EnfasBms.ChannelId.COMMAND_BALANCING_THRESHOLD, //
								new UnsignedWordElement(3)), //
						m(EnfasBms.ChannelId.COMMAND_CUSTOM_CURRENT_CHARGE_LIMIT, //
								new UnsignedWordElement(4)), //
						m(EnfasBms.ChannelId.COMMAND_CUSTOM_CURRENT_DISCHARGE_LIMIT, //
								new UnsignedWordElement(5)),
						m(EnfasBms.ChannelId.COMMAND_RESET_1, //
								new UnsignedWordElement(6)), //
						m(EnfasBms.ChannelId.COMMAND_RESET_2, //
								new UnsignedWordElement(7)), //
						m(EnfasBms.ChannelId.COMMAND_ALIVE_COUNTER, //
								new UnsignedWordElement(8)) //
				));//
	}

	/**
	 * Registers listeners for 'onChange' event of any MODULE_X_MIN_CELL_VOLTAGE and
	 * MODULE_X_MAX_CELL_VOLTAGE to update the total 'MinCellVoltage' and
	 * 'MaxCellVoltage' Channels.
	 */
	public void registerCellVoltageListeners() {
		getMaxCellChannelIds()//
				.forEach(t -> this.channel(t) //
						.onChange((a, b) -> this.updateMaxCellVoltage()));
		getMinCellChannelIds()//
				.forEach(t -> this.channel(t) //
						.onChange((a, b) -> this.updateMinCellVoltage()));
	}

	/**
	 * Called at 'onChange' event of any MODULE_X_MAX_CELL_VOLTAGE channel. Updates
	 * the total 'MaxCellVoltage' Channel.
	 */
	private void updateMaxCellVoltage() {
		getMaxCellChannelIds()//
				.map(this::getValueFromChannel)//
				.filter(Objects::nonNull) //
				.mapToInt(Integer::intValue) //
				.max()//
				.ifPresentOrElse(//
						i -> this._setMaxCellVoltage(i), //
						() -> this._setMaxCellVoltage(null) //
				);
	}

	/**
	 * Called at 'onChange' event of any MODULE_X_MIN_CELL_VOLTAGE channel. Updates
	 * the 'MinCellVoltage' Channel.
	 */
	private void updateMinCellVoltage() {
		getMinCellChannelIds()//
				.map(this::getValueFromChannel)//
				.filter(Objects::nonNull) //
				.mapToInt(Integer::intValue) //
				.min()//
				.ifPresentOrElse(//
						i -> this._setMinCellVoltage(i), //
						() -> this._setMinCellVoltage(null) //
				);
	}

	/**
	 * Method to get the value from the channel.
	 * 
	 * @param channelId channel-id to which the value to be get.
	 * @return actual value as {@link Integer} or null
	 */
	private Integer getValueFromChannel(EnfasBms.ChannelId channelId) {
		IntegerReadChannel readChannel = channel(channelId);
		var val = readChannel.getNextValue();
		if (val.isDefined()) {
			return val.get();
		}
		return null;
	}

	/**
	 * Method to return stream of enfas's max cell voltage channels.
	 * 
	 * @return {@link Stream} of {@link EnfasBms}
	 */
	private static Stream<EnfasBms.ChannelId> getMaxCellChannelIds() {
		return Stream.of(//
				EnfasBms.ChannelId.MODULE_0_MAX_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_1_MAX_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_2_MAX_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_3_MAX_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_4_MAX_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_5_MAX_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_6_MAX_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_7_MAX_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_8_MAX_CELL_VOLTAGE);
	}

	/**
	 * Method to return stream of enfas's min cell voltage channels.
	 * 
	 * @return {@link Stream} of {@link EnfasBms}
	 */
	private static Stream<EnfasBms.ChannelId> getMinCellChannelIds() {
		return Stream.of(//
				EnfasBms.ChannelId.MODULE_0_MIN_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_1_MIN_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_2_MIN_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_3_MIN_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_4_MIN_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_5_MIN_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_6_MIN_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_7_MIN_CELL_VOLTAGE, //
				EnfasBms.ChannelId.MODULE_8_MIN_CELL_VOLTAGE);
	}
}
