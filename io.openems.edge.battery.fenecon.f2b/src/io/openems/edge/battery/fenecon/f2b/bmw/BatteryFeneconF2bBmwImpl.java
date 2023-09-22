package io.openems.edge.battery.fenecon.f2b.bmw;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.ADD;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIVIDE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.MULTIPLY;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SET_NULL_FOR_DEFAULT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SUBTRACT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.chain;
import static io.openems.edge.common.channel.ChannelUtils.getValues;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
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
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.CoolingApproval;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.HeatingRequest;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.OperationState;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.RequestCharging;
import io.openems.edge.battery.fenecon.f2b.bmw.statemachine.Context;
import io.openems.edge.battery.fenecon.f2b.bmw.statemachine.StateMachine;
import io.openems.edge.battery.fenecon.f2b.bmw.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.filter.Pt1filter;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery.Fenecon.F2B.BMW", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
// TODO Extract AbstractBatteryFeneconF2b
public class BatteryFeneconF2bBmwImpl extends AbstractOpenemsModbusComponent implements BatteryFeneconF2bBmw,
		BatteryFeneconF2b, ModbusComponent, OpenemsComponent, Battery, ModbusSlave, StartStoppable, EventHandler {

	private static BmwOnChangeHandler BMW_ON_CHANGE_HANDLER = new BmwOnChangeHandler();

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);
	private final Logger log = LoggerFactory.getLogger(BatteryFeneconF2bBmwImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private static final ElementToChannelConverter SET_NULL_FOR_DEFAULT_0 = SET_NULL_FOR_DEFAULT(0);
	private static final ElementToChannelConverter SET_NULL_FOR_DEFAULT_3 = SET_NULL_FOR_DEFAULT(3);
	private static final ElementToChannelConverter SET_NULL_FOR_DEFAULT_F = SET_NULL_FOR_DEFAULT(0xF);
	private static final ElementToChannelConverter SET_NULL_FOR_DEFAULT_FF = SET_NULL_FOR_DEFAULT(0xFF);
	private static final ElementToChannelConverter SET_NULL_FOR_DEFAULT_0FFF = SET_NULL_FOR_DEFAULT(0x0FFF);
	private static final ElementToChannelConverter SET_NULL_FOR_DEFAULT_00FD = SET_NULL_FOR_DEFAULT(0x00FD);
	private static final ElementToChannelConverter SET_NULL_FOR_DEFAULT_00FF = SET_NULL_FOR_DEFAULT(0x00FF);
	private static final ElementToChannelConverter SET_NULL_FOR_DEFAULT_3FFF = SET_NULL_FOR_DEFAULT(0x3FFF);
	private static final ElementToChannelConverter SET_NULL_FOR_DEFAULT_FFFF = SET_NULL_FOR_DEFAULT(0xFFFF);

	/* TODO drop pt1 filter after battery protection implementation */
	private Pt1filter pt1FilterMaxCurrentVoltLimit;
	private OperationState operationState = OperationState.UNDEFINED;
	private Instant timeAtEntryForceCharge = Instant.MIN;
	private Instant timeAtEntryDeepDischargeVoltageControl = Instant.MIN;
	private boolean forceChargeWasActive = false;
	private boolean deepDischargeVoltageControlWasActive = false;

	private boolean hvContactorUnlocked = true;
	private boolean heatingTarget = false;
	private Config config = null;

	@Reference
	private Cycle cycle;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	public BatteryFeneconF2bBmwImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryFeneconF2b.ChannelId.values(), //
				BatteryFeneconF2bBmw.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		// TODO drop after battery protection implementation
		this.pt1FilterMaxCurrentVoltLimit = new Pt1filter(Constants.VOLTAGE_CONTROL_FILTER_TIME_CONSTANT, 1.0);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		var protocol = new ModbusProtocol(this, //
				new FC16WriteRegistersTask(1, //
						m(BatteryFeneconF2b.ChannelId.F2B_CAN_COMMUNICATION, new UnsignedWordElement(1)), //
						m(BatteryFeneconF2b.ChannelId.F2B_ERROR_RESET_REQUEST, new UnsignedWordElement(2)), //
						m(BatteryFeneconF2b.ChannelId.F2B_TERMINAL_15_HW, new UnsignedWordElement(3)), //
						m(BatteryFeneconF2b.ChannelId.F2B_TERMINAL_30C, new UnsignedWordElement(4)), //
						m(BatteryFeneconF2bBmw.ChannelId.TIMESTAMP_FOR_EVENTS_AND_ERROR_MEMORY,
								new UnsignedDoublewordElement(5)), //
						m(BatteryFeneconF2b.ChannelId.F2B_TERMINAL_15_SW, new UnsignedWordElement(7)), //
						m(BatteryFeneconF2bBmw.ChannelId.HV_CONTACTOR, new UnsignedWordElement(8)), //
						m(BatteryFeneconF2bBmw.ChannelId.INSULATION_MEASUREMENT, new UnsignedWordElement(9)), //
						m(BatteryFeneconF2bBmw.ChannelId.SET_BALANCING_TARGET_VOLTAGE, new UnsignedWordElement(10)), //
						m(BatteryFeneconF2bBmw.ChannelId.SET_BALANCING_CONDITIONS_FULFILLED,
								new UnsignedWordElement(11)),
						m(BatteryFeneconF2bBmw.ChannelId.SET_OCV_REACHED_AT_ALL_THE_BATTERIES,
								new UnsignedWordElement(12)),
						m(BatteryFeneconF2bBmw.ChannelId.SET_BALANCING_RUNNING, new UnsignedWordElement(13)),
						m(BatteryFeneconF2bBmw.ChannelId.COOLING_APPROVAL, new UnsignedWordElement(14)), //
						m(BatteryFeneconF2bBmw.ChannelId.REQUEST_COOLING_VALVE, new UnsignedWordElement(15)), //
						m(BatteryFeneconF2bBmw.ChannelId.ALLOCATES_BATTERY_HEATING_POWER, new UnsignedWordElement(16)), //
						m(BatteryFeneconF2bBmw.ChannelId.HEATING_RELEASED_POWER, new UnsignedWordElement(17),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(BatteryFeneconF2bBmw.ChannelId.REQUEST_CHARGING, new UnsignedWordElement(18)), //
						m(BatteryFeneconF2bBmw.ChannelId.PREDICTED_CHARGING_POWER, new UnsignedWordElement(19)),
						m(BatteryFeneconF2bBmw.ChannelId.PREDICTION_OF_THE_AVERAGE_EXPECTED_POWER_LOAD,
								new UnsignedWordElement(20)),
						m(BatteryFeneconF2b.ChannelId.F2B_TERMINAL_15_TOGGLE_REQUEST, new UnsignedWordElement(21)), //
						new DummyRegisterElement(22, 49), //
						m(BatteryFeneconF2b.ChannelId.F2B_RESET, new UnsignedWordElement(50))), //

				new FC3ReadRegistersTask(1, Priority.HIGH, //
						m(BatteryFeneconF2b.ChannelId.F2B_CAN_COMMUNICATION, new UnsignedWordElement(1)), //
						m(BatteryFeneconF2b.ChannelId.F2B_ERROR_RESET_REQUEST, new UnsignedWordElement(2)), //
						m(BatteryFeneconF2b.ChannelId.F2B_TERMINAL_15_HW, new UnsignedWordElement(3)), //
						m(BatteryFeneconF2b.ChannelId.F2B_TERMINAL_30C, new UnsignedWordElement(4)), //
						new DummyRegisterElement(5, 6), //
						m(BatteryFeneconF2b.ChannelId.F2B_TERMINAL_15_SW, new UnsignedWordElement(7)), //
						m(BatteryFeneconF2bBmw.ChannelId.HV_CONTACTOR, new UnsignedWordElement(8)), //
						m(BatteryFeneconF2bBmw.ChannelId.INSULATION_MEASUREMENT, new UnsignedWordElement(9)), //
						m(BatteryFeneconF2bBmw.ChannelId.SET_BALANCING_TARGET_VOLTAGE, new UnsignedWordElement(10)), //
						m(BatteryFeneconF2bBmw.ChannelId.SET_BALANCING_CONDITIONS_FULFILLED,
								new UnsignedWordElement(11)), //
						m(BatteryFeneconF2bBmw.ChannelId.SET_OCV_REACHED_AT_ALL_THE_BATTERIES,
								new UnsignedWordElement(12)), //
						m(BatteryFeneconF2bBmw.ChannelId.SET_BALANCING_RUNNING, new UnsignedWordElement(13)), //
						m(BatteryFeneconF2bBmw.ChannelId.COOLING_APPROVAL, new UnsignedWordElement(14)), //
						m(BatteryFeneconF2bBmw.ChannelId.REQUEST_COOLING_VALVE, new UnsignedWordElement(15)), //
						m(BatteryFeneconF2bBmw.ChannelId.ALLOCATES_BATTERY_HEATING_POWER, new UnsignedWordElement(16)), //
						m(BatteryFeneconF2bBmw.ChannelId.HEATING_RELEASED_POWER, new UnsignedWordElement(17),
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(BatteryFeneconF2bBmw.ChannelId.REQUEST_CHARGING, new UnsignedWordElement(18)), //
						m(BatteryFeneconF2bBmw.ChannelId.PREDICTED_CHARGING_POWER, new UnsignedWordElement(19),
								MULTIPLY(25)), //
						m(BatteryFeneconF2bBmw.ChannelId.PREDICTION_OF_THE_AVERAGE_EXPECTED_POWER_LOAD,
								new UnsignedWordElement(20),
								chain(MULTIPLY(25), ElementToChannelConverter.SCALE_FACTOR_MINUS_2)),
						m(BatteryFeneconF2b.ChannelId.F2B_TERMINAL_15_TOGGLE_REQUEST, new UnsignedWordElement(21)), //
						new DummyRegisterElement(22, 49), //
						m(BatteryFeneconF2b.ChannelId.F2B_RESET, new UnsignedWordElement(50))), //

				new FC3ReadRegistersTask(101, Priority.HIGH, //
						m(BatteryFeneconF2b.ChannelId.F2B_STATE, new UnsignedWordElement(101)), //
						m(new BitsWordElement(102, this)//
								.bit(0, BatteryFeneconF2b.ChannelId.F2B_T30C_NO_INPUT_VOLTAGE) //
								.bit(1, BatteryFeneconF2b.ChannelId.F2B_T30C_OUTPUT_ERROR)), //
						m(BatteryFeneconF2b.ChannelId.F2B_TERMINAL_15_HW_ERROR, new UnsignedWordElement(103)), //
						m(BatteryFeneconF2b.ChannelId.F2B_POWER_SUPPLY_ERROR_HV_SIDE, new UnsignedWordElement(104)), //
						m(BatteryFeneconF2b.ChannelId.F2B_TERMINAL_30F_INPUT_VOLTAGE, new UnsignedWordElement(105),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(BatteryFeneconF2b.ChannelId.F2B_TERMINAL_30C_INPUT_VOLTAGE, new UnsignedWordElement(106),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(BatteryFeneconF2bBmw.ChannelId.HV_CONTACTOR_STATUS, new UnsignedWordElement(107),
								SET_NULL_FOR_DEFAULT_3), //
						m(BatteryFeneconF2bBmw.ChannelId.CAT1_PRECHARGE_SYSTEM_IS_LOCKED, new UnsignedWordElement(108),
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						new DummyRegisterElement(109), //
						m(BatteryFeneconF2bBmw.ChannelId.CAT3_NO_LIMITATIONS_BUT_SERVICE_NEEDED,
								new UnsignedWordElement(110), //
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.CAT4_AT_LEAST_ONE_DERATING_ACTIVE,
								new UnsignedWordElement(111), //
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.CAT5_BATTERY_POWER_WILL_BE_LIMITED,
								new UnsignedWordElement(112),
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.CAT6_ZERO_CURRENT_REQUEST, new UnsignedWordElement(113),
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.CAT7_EMERGENCY_CONTACTOR_OPEN, new UnsignedWordElement(114), //
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //

						// AVL_U_HVSTO Internal Voltage of the EES.
						// This value is more accurate than AVL_U_LINK in case the contactors are closed
						m(BatteryFeneconF2b.ChannelId.INTERNAL_VOLTAGE, new UnsignedWordElement(115),
								SET_NULL_FOR_DEFAULT_FFFF).onUpdateCallback(t -> {
									this.updateBatteryVoltage();
								}), //
						// AVL_U_LINK, External voltage (at DC connector) of the battery
						m(BatteryFeneconF2bBmw.ChannelId.LINK_VOLTAGE, new UnsignedWordElement(116), //
								chain(SET_NULL_FOR_DEFAULT_00FF, MULTIPLY(4))), //
						// AVL_I_HVSTO, Actual current of the battery
						m(new UnsignedWordElement(117))//
								.m(BatteryFeneconF2bBmw.ChannelId.BATTERY_CURRENT,
										chain(SET_NULL_FOR_DEFAULT_FFFF, SUBTRACT(8192), INVERT))//
								.m(Battery.ChannelId.CURRENT,
										chain(SET_NULL_FOR_DEFAULT_FFFF, SUBTRACT(8192),
												ElementToChannelConverter.SCALE_FACTOR_MINUS_1, INVERT))
								.build(), //

						// CHGCOND_HVSTO, State of Charge (SoC) The SoC is calculated based on the
						// nominal capacity.The minimal value is always \"0\" and the maximum value
						// depends on the actual SoH (state of health) and will decrease over time
						m(BatteryFeneconF2bBmw.ChannelId.BATTERY_SOC, new UnsignedWordElement(118),
								SET_NULL_FOR_DEFAULT_00FF), //
						// MB_MIN_U_ELMNT_HVSTO, Minimum actual cell voltage
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(119), //
								chain(SET_NULL_FOR_DEFAULT_0, MULTIPLY(2), ElementToChannelConverter.DIRECT_1_TO_1)), //
						// MB_MAX_U_ELMNT_HVSTO , Maximum actual cell voltage
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(120), //
								chain(MULTIPLY(2), SET_NULL_FOR_DEFAULT_0)), // [0...8,188V]
						m(BatteryFeneconF2bBmw.ChannelId.SUMMED_CELL_VOLTAGES, new UnsignedWordElement(121), //
								chain(DIVIDE(5), SET_NULL_FOR_DEFAULT_0)), //
						// AVL_TEMP_HVSTO_MIN
						m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new UnsignedWordElement(122), //
								chain(SET_NULL_FOR_DEFAULT_00FF, SUBTRACT(50))), // ['-50...202°C]
						// AVL_TEMP_HVSTO_MAX
						m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new UnsignedWordElement(123),
								chain(SET_NULL_FOR_DEFAULT_00FF, SUBTRACT(50))), // ['-50...202°C]
						m(BatteryFeneconF2b.ChannelId.AVG_CELL_TEMPERATURE, new UnsignedWordElement(124), //
								chain(SET_NULL_FOR_DEFAULT_00FF, SUBTRACT(50))), //
						// I_DYN_MAX_DCHG_HVSTO,
						m(BatteryFeneconF2bBmw.ChannelId.BATTERY_DISCHARGE_MAX_CURRENT, new UnsignedWordElement(125), //
								chain(SET_NULL_FOR_DEFAULT_FFFF, SUBTRACT(8192), INVERT)), // [-819,2...819,0A]
						// U_MIN_DCHG_HVSTO
						m(new UnsignedWordElement(126))//
								.m(BatteryFeneconF2bBmw.ChannelId.BATTERY_DISCHARGE_MIN_VOLTAGE,
										SET_NULL_FOR_DEFAULT_FFFF) //
								.m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE,
										chain(SET_NULL_FOR_DEFAULT_FFFF,
												ElementToChannelConverter.SCALE_FACTOR_MINUS_1))//
								.build(), // [0 ... 819,0 V]
						m(BatteryFeneconF2bBmw.ChannelId.ALLOWED_DISCHARGE_POWER, new UnsignedWordElement(127), //
								chain(SET_NULL_FOR_DEFAULT_FFFF, SUBTRACT(196596), MULTIPLY(3))), //
						m(BatteryFeneconF2bBmw.ChannelId.MAX_ALLOWED_DISCHARGE_POWER, new UnsignedWordElement(128), //
								chain(SET_NULL_FOR_DEFAULT_FFFF, SUBTRACT(196596), MULTIPLY(3))), //
						// I_DYN_MAX_CHG_HVSTO, Maximum allowed charge current:In case the EES is
						// operated
						// at a higher charge current the EES might open the contactors
						m(BatteryFeneconF2bBmw.ChannelId.BATTERY_CHARGE_MAX_CURRENT, new UnsignedWordElement(129), //
								chain(SET_NULL_FOR_DEFAULT_FFFF, SUBTRACT(8192))),
						// U_MAX_CHG_HVSTO, Maximum allowed charge voltage: In case the EES is be
						// operated at a higher voltage while charging the EES might open the contactors
						m(new UnsignedWordElement(130))//
								.m(BatteryFeneconF2bBmw.ChannelId.BATTERY_CHARGE_MAX_VOLTAGE, SET_NULL_FOR_DEFAULT_FFFF) //
								.m(Battery.ChannelId.CHARGE_MAX_VOLTAGE,
										chain(SET_NULL_FOR_DEFAULT_FFFF,
												ElementToChannelConverter.SCALE_FACTOR_MINUS_1))//
								.build(), // [0 ... 819,0 V]
						m(BatteryFeneconF2bBmw.ChannelId.MAX_ALLOWED_CHARGE_POWER, new UnsignedWordElement(131), //
								chain(SET_NULL_FOR_DEFAULT_FFFF, MULTIPLY(3))), //
						m(BatteryFeneconF2bBmw.ChannelId.ALLOWED_CHARGE_POWER, new UnsignedWordElement(132), //
								chain(SET_NULL_FOR_DEFAULT_FFFF, MULTIPLY(3))), //
						m(BatteryFeneconF2bBmw.ChannelId.INSULATION_MEASUREMENT_STATUS, new UnsignedWordElement(133),
								SET_NULL_FOR_DEFAULT_3), //
						m(BatteryFeneconF2bBmw.ChannelId.INSULATION_RESISTANCE, new UnsignedWordElement(134),
								chain(SET_NULL_FOR_DEFAULT_00FF, SET_NULL_FOR_DEFAULT_00FD,
										ElementToChannelConverter.SCALE_FACTOR_1)), //
						m(BatteryFeneconF2bBmw.ChannelId.INSULATION_VALUE_WARNING, new UnsignedWordElement(135),
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.BALANCING_MIN_CELL_VOLTAGE, new UnsignedWordElement(136),
								SET_NULL_FOR_DEFAULT_3FFF), //
						m(BatteryFeneconF2bBmw.ChannelId.BALANCING_CONDITION, new UnsignedWordElement(137),
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.OCV_REACHED, new UnsignedWordElement(138),
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.BALANCING_STILL_RUNNING, new UnsignedWordElement(139),
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.MAX_BALANCING_TIME, new UnsignedDoublewordElement(140),
								SET_NULL_FOR_DEFAULT_3), //
						m(BatteryFeneconF2b.ChannelId.COOLING_VALVE_STATE, new UnsignedWordElement(142),
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.COOLING_REQUEST, new UnsignedWordElement(143),
								SET_NULL_FOR_DEFAULT_3), //
						m(BatteryFeneconF2bBmw.ChannelId.AVERAGE_COOLING_POWER, new UnsignedWordElement(144), //
								chain(SET_NULL_FOR_DEFAULT_00FF, MULTIPLY(20))), //
						m(BatteryFeneconF2bBmw.ChannelId.COOLING_PLATE_TEMPERATURE, new UnsignedWordElement(145), //
								chain(SET_NULL_FOR_DEFAULT_00FF, SUBTRACT(50))), //
						m(BatteryFeneconF2bBmw.ChannelId.COOLING_VALVE_ERROR_STATE, new UnsignedWordElement(146),
								SET_NULL_FOR_DEFAULT_F), //
						m(BatteryFeneconF2bBmw.ChannelId.HEATING_REQUEST, new UnsignedWordElement(147),
								SET_NULL_FOR_DEFAULT_FF), //
						m(BatteryFeneconF2bBmw.ChannelId.BATTERY_POWER, new UnsignedWordElement(148),
								chain(SET_NULL_FOR_DEFAULT_0FFF, ElementToChannelConverter.SCALE_FACTOR_1)), //
						m(BatteryFeneconF2bBmw.ChannelId.HEATING_POWER, new UnsignedWordElement(149),
								SET_NULL_FOR_DEFAULT_0FFF), //
						m(BatteryFeneconF2bBmw.ChannelId.PREDICTED_DISCHARGE_ENERGY, new UnsignedWordElement(150), //
								chain(SET_NULL_FOR_DEFAULT_0FFF, MULTIPLY(2),
										ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), //
						m(BatteryFeneconF2bBmw.ChannelId.PREDICTED_AVERAGE_ENERGY, new UnsignedWordElement(151),
								SET_NULL_FOR_DEFAULT_FFFF), //
						m(BatteryFeneconF2bBmw.ChannelId.PREDICTED_ENERGY_TO_RECEIVE_THE_TARGET_SOC,
								new UnsignedWordElement(152), //
								chain(SET_NULL_FOR_DEFAULT_FFFF, MULTIPLY(2),
										ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), //
						m(BatteryFeneconF2bBmw.ChannelId.MIN_SOC, new UnsignedWordElement(153), //
								chain(SET_NULL_FOR_DEFAULT_00FF, DIVIDE(2))), //
						m(BatteryFeneconF2bBmw.ChannelId.MAX_SOC, new UnsignedWordElement(154), //
								chain(SET_NULL_FOR_DEFAULT_00FF, DIVIDE(2))), //
						new DummyRegisterElement(155, 158), //
						m(BatteryFeneconF2bBmw.ChannelId.HV_WARN_CONCEPT_STATUS, new UnsignedWordElement(159),
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.BATTERY_EMERGENCY_MODE, new UnsignedWordElement(160),
								chain(SET_NULL_FOR_DEFAULT_3, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.INTERLOCK_LOOP_STATUS, new UnsignedWordElement(161),
								chain(SET_NULL_FOR_DEFAULT_3, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.CONTACTORS_DIAGNOSTIC_STATUS, new UnsignedWordElement(162),
								SET_NULL_FOR_DEFAULT_3), //
						m(BatteryFeneconF2bBmw.ChannelId.OPEN_CONTACTOR_INSULATION_ERROR, new UnsignedWordElement(163),
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2bBmw.ChannelId.CLOSED_CONTACTOR_INSULATION_ERROR,
								new UnsignedWordElement(164),
								chain(SET_NULL_FOR_DEFAULT_3, SET_NULL_FOR_DEFAULT_0, SUBTRACT(1))), //
						m(BatteryFeneconF2b.ChannelId.F2B_TIMESTAMP, new UnsignedDoublewordElement(165)), //
						m(BatteryFeneconF2b.ChannelId.F2B_WATCHDOG_STATE, new UnsignedWordElement(167)),
						m(BatteryFeneconF2b.ChannelId.F2B_WATCHDOG_TIMER_VALUE, new UnsignedWordElement(168)),
						m(new BitsWordElement(169, this) //
								.bit(0, BatteryFeneconF2bBmw.ChannelId.CAN_TIMEOUT_LIM_CHG_DCHG_HVSTO) //
								.bit(1, BatteryFeneconF2bBmw.ChannelId.CAN_TIMEOUT_STAT_HVSTO_2) //
								.bit(2, BatteryFeneconF2bBmw.ChannelId.CAN_TIMEOUT_ST_HVSTO_1)//
								.bit(3, BatteryFeneconF2bBmw.ChannelId.CAN_SIGNAL_INVALID_AVL_U_HVSTO))//
				));//

		var voltElementToChannelConverter = chain(SET_NULL_FOR_DEFAULT_FF, MULTIPLY(10),
				ADD(Constants.CELL_VOLTAGE_VALUE_OFFSET));
		var ameVolt = this.generateCellTasks(Constants.NUMBER_OF_VOLTAGE_CELLS, Constants.CELL_VOLTAGE_REGISTER_OFFSET,
				"_VOLTAGE", Unit.MILLIVOLT, voltElementToChannelConverter);

		var tempElementToChannelConverter = chain(SET_NULL_FOR_DEFAULT_FF,
				SUBTRACT(Constants.CELL_TEMPERATURE_VALUE_OFFSET));
		var ameTemp = this.generateCellTasks(Constants.NUMBER_OF_TEMPERATURE_CELLS,
				Constants.CELL_TEMPERATURE_REGISTER_OFFSET, "_TEMPERATURE", Unit.DEGREE_CELSIUS,
				tempElementToChannelConverter);

		protocol.addTasks(//
				new FC3ReadRegistersTask(Constants.CELL_VOLTAGE_REGISTER_OFFSET, Priority.LOW, ameVolt), //
				new FC3ReadRegistersTask(Constants.CELL_TEMPERATURE_REGISTER_OFFSET, Priority.LOW, ameTemp));
		return protocol;
	}

	/**
	 * Generates read tasks for the battery cell voltages and temperatures.
	 * 
	 * @param numberOfCells  number of voltage or temperature cells.
	 * @param registerOffset start address offset.
	 * @param prefix         channel identifier to generate a channel for cell task.
	 *                       e.g. "_VOLTAGE"
	 * @param unit           value unit e.g Unit.MILLIVOLT
	 * @param converter      to scale factors to be applied to the result.
	 * @return {@link AbstractModbusElement} a created map by register and channel
	 *         for {@link ModbusProtocol} task.
	 */
	private ModbusElement[] generateCellTasks(int numberOfCells, int registerOffset, String prefix, Unit unit,
			ElementToChannelConverter converter) {
		var ame = new ModbusElement[numberOfCells];
		for (var num = 0; num < numberOfCells; num++) {
			// Create Temperature Channel
			var channelId = new ChannelIdImpl(//
					getSingleCellPrefix(num + 1, prefix), //
					Doc.of(OpenemsType.INTEGER).unit(unit));
			addChannel(channelId);
			var uwe = new UnsignedWordElement(num + registerOffset);
			ame[num] = m(channelId, uwe, converter); //
		}
		return ame;
	}

	private static String getSingleCellPrefix(int num, String prefix) {
		return "CELL_" + prefix + String.format("%03d", num);
	}

	@Override
	public String debugLog() {
		return Battery.generateDebugLog(this, this.stateMachine);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(BatteryFeneconF2bBmw.class, accessMode, 100) //
						.build());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.heatingManagement();
			// TODO drop after battery protection implementation
			this.calculateBatteryValues();
			this.handleStateMachine();
			break;
		}
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		// Store the current State
		this._setStateMachine(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		var context = new Context(this, this.componentManager.getClock());

		// Call the StateMachine
		try {
			this.stateMachine.run(context);
			this._setRunFailed(false);
		} catch (OpenemsNamedException e) {
			this._setRunFailed(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	/**
	 * TODO drop after battery protection implementation The internal voltage is
	 * preferred when the battery is started, as the internal voltage is more
	 * accurate than the junction voltage.
	 */
	protected synchronized void updateBatteryVoltage() {
		Integer batteryVoltage = null;
		if (this.getInternalVoltage().isDefined() && this.getLinkVoltage().isDefined()) {
			if (this.isStarted()) {
				batteryVoltage = this.getInternalVoltage().get();
			} else {
				batteryVoltage = TypeUtils.multiply(this.getLinkVoltage().get(), 10);
			}
		}
		this._setLinkVoltageHighRes(batteryVoltage);
		this._setVoltage(TypeUtils.divide(batteryVoltage, 10));
	}

	/**
	 * Handles the battery cooling, in case of battery temperature goes higher than
	 * 32 degree. Cooling requirement will be asked by the battery.
	 * 
	 * @throws OpenemsNamedException on error.
	 */
	protected synchronized void updateCoolingRequest() {
		try {
			switch (this.getCoolingRequest()) {
			case UNDEFINED, NO_COOLING_REQUESTED -> this.setCoolingApproval(CoolingApproval.NOT_GRANTED);
			case COOLING_REQUESTED, URGENT_COOLING_REQUESTED -> this.setCoolingApproval(CoolingApproval.GRANTED);
			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	// TODO drop after battery protection implementation
	protected synchronized void updateSoc() {
		Channel<Double> batterySocChannel = this.channel(BatteryFeneconF2bBmw.ChannelId.BATTERY_SOC);
		var batterySoc = batterySocChannel.value();
		var soc = batterySoc.asOptional().map(t -> {
			var calculatedBatterySoc = (int) ((t / 10.0 - Constants.MIN_ALLOWED_SOC)
					* (100.0 / (Constants.MAX_ALLOWED_SOC - Constants.MIN_ALLOWED_SOC)) * 10.0);
			var unlimitedSocChannel = this.channel(BatteryFeneconF2bBmw.ChannelId.UNLIMITED_SOC);
			unlimitedSocChannel.setNextValue(calculatedBatterySoc);
			if (calculatedBatterySoc < 0) {
				return 0;
			}
			if (calculatedBatterySoc > 1000) {
				return 100;
			}
			return calculatedBatterySoc / 10;
		}).orElse(null);
		this._setSoc(soc);
	}

	@Override
	public void setHeatingTarget(boolean value) {
		this.heatingTarget = value;
	}

	@Override
	public boolean getHeatingTarget() {
		return this.heatingTarget;
	}

	/**
	 * Handles the battery heating, in case of battery temperature is less than 10
	 * degree.
	 */
	private void heatingManagement() {
		if (!this.isStarted() //
				|| !this.getHeatingTarget() //
				|| !this.getAvgCellTemperature().isDefined() //
				|| this.getAvgCellTemperature().get() < Constants.HEATING_START_TEMPERATURE) {
			return;
		}

		try {
			// Request heating
			this.setRequestCharging(RequestCharging.CHARGING_WITH_PRECONDITIONING);
			this.setPredictedChargingPower(Constants.PREDICTED_CHARGING_POWER);

			// Check whether heating triggered
			if (this.getHeatingRequest().isDefined() //
					&& this.getBatteryPower().isDefined()//
					&& this.getHeatingRequest().asEnum() == HeatingRequest.BATTERY_HEATING_PRECONDITIONING//
					&& this.getBatteryPower().get() == Constants.REQUIRED_BATTERY_POWER_FOR_HEATING) {

				// Release power if heating triggered
				this.setAllocatesBatteryHeatingPower(HeatingRequest.BATTERY_HEATING_PRECONDITIONING);
				this.setHeatingReleasedPower(Constants.RELEASED_POWER);
				return;
			}
			// Deactivate heating
			this.setHeatingReleasedPower(0);
			if (this.getHeatingPower().isDefined() && this.getHeatingPower().get() == 0) {
				this.setAllocatesBatteryHeatingPower(HeatingRequest.NOT);
				this.setPredictedChargingPower(0L);
				this.setRequestCharging(RequestCharging.NO_CHARGING);
			}
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, e.getMessage());
		}
	}

	@Override
	public void setStartStop(StartStop value) {
		this.startStopTarget.set(value);
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
	public void setHvContactorUnlocked(boolean value) {
		this.hvContactorUnlocked = value;
	}

	@Override
	public boolean isHvContactorUnlocked() {
		return this.hvContactorUnlocked;
	}

	public static record BatteryValues(int soc, int voltage, int chargeMaxVoltage, int dischargeMinVoltage) {
	}

	// TODO drop after battery protection implementation
	public static record BatteryProtectionValues(int unlimitedSoc, int linkVoltageHighRes, int batteryCurrent,
			int batteryChargeMaxVoltage, int batteryDischargeMinVoltage) {
	}

	// TODO drop after battery protection implementation
	private void calculateBatteryValues() {
		final var data = getValues(this, BatteryProtectionValues.class).orElse(null);

		// Channel values are not available
		if (data == null) {
			this._setChargeMaxCurrent(null);
			this._setDischargeMaxCurrent(null);
			return;
		}

		// If battery voltage is equal to 0, do not perform the regulation
		if (data.linkVoltageHighRes() == 0) {
			this._setChargeMaxCurrent(null);
			this._setDischargeMaxCurrent(null);
			return;
		}

		// Voltage control
		int chargeMaxCurrentVoltLimit = this.voltageRegulatorCalculateMaxCurrent(data.batteryCurrent(),
				data.linkVoltageHighRes(), data.batteryChargeMaxVoltage(), TypeUtils::subtract,
				this::_setChargeMaxCurrentVoltaLimitChannel);
		int dischargeMaxCurrentVoltLimit = this.voltageRegulatorCalculateMaxCurrent(data.batteryCurrent(),
				data.linkVoltageHighRes(), data.batteryDischargeMinVoltage(), TypeUtils::sum,
				this::_setDischargeMaxCurrentVoltLimit);

		var batteryPower = TypeUtils.multiply(data.linkVoltageHighRes(), data.batteryCurrent());

		// Deep Discharge Protection Voltage
		if ((data.linkVoltageHighRes() <= data.batteryDischargeMinVoltage()
				&& (batteryPower > -Constants.FORCE_CHARGE_POWER_ERROR_THRESHOLD))) {
			if (!this.deepDischargeVoltageControlWasActive) {
				this.timeAtEntryDeepDischargeVoltageControl = Instant.now();
			} else if (Duration.between(this.timeAtEntryDeepDischargeVoltageControl, Instant.now())
					.getSeconds() > Constants.DEEP_DISCHARGE_PROTECTION_ERROR_DELAY) {
				this.channel(BatteryFeneconF2bBmw.ChannelId.DEEP_DISCHARGE_PROTECTION_VOLTAGE_CONTROL)
						.setNextValue(true);
			}
			this.deepDischargeVoltageControlWasActive = true;
		} else {
			this.deepDischargeVoltageControlWasActive = false;
		}

		// calculate minimum chargeMaxCurrent and dischargeMaxCurrent
		int minChargeMaxCurrent = Math.min(chargeMaxCurrentVoltLimit, this.getBatteryChargeMaxCurrent().get() / 10);
		int minDischargeMaxCurrent = Math.min(dischargeMaxCurrentVoltLimit,
				this.getBatteryDischargeMaxCurrent().get() / 10);
		this.socRegulation(data.unlimitedSoc(), minChargeMaxCurrent, minDischargeMaxCurrent, batteryPower,
				data.linkVoltageHighRes());

	}

	// TODO drop after battery protection implementation
	private void socRegulation(Integer unlimitedSoc, Integer minChargeMaxCurrent, Integer minDischargeMaxCurrent,
			Integer batteryPower, Integer batteryLinkVoltageHighRes) {
		if (this.isStarted()) {
			switch (this.operationState) {
			case UNDEFINED:
				this.operationState = OperationState.OFF;
				break;
			case OFF:
				if (unlimitedSoc >= Constants.ONLY_DISCHARGE_UPPER_THRESHOLD) {
					this.operationState = OperationState.ONLY_DISCHARGE;
				} else if (unlimitedSoc >= Constants.ONLY_CHARGE_LOWER_THRESHOLD) {
					this.operationState = OperationState.NORMAL;
				} else {
					this.operationState = OperationState.FORCE_CHARGE;
				}
				this.forceChargeWasActive = false;
				break;
			case NORMAL:
				if (unlimitedSoc >= Constants.ONLY_DISCHARGE_UPPER_THRESHOLD) {
					this.operationState = OperationState.ONLY_DISCHARGE;
				} else if (unlimitedSoc <= Constants.ONLY_CHARGE_LOWER_THRESHOLD) {
					this.operationState = OperationState.ONLY_CHARGE;
				}
				this.forceChargeWasActive = false;
				break;
			case ONLY_DISCHARGE:
				if (unlimitedSoc <= Constants.ONLY_DISCHARGE_LOWER_THRESHOLD) {
					this.operationState = OperationState.NORMAL;
				}
				if (minChargeMaxCurrent > 0) {
					minChargeMaxCurrent = 0;
				}
				this.forceChargeWasActive = false;
				break;
			case ONLY_CHARGE:
				if (unlimitedSoc >= Constants.ONLY_CHARGE_UPPER_THRESHOLD) {
					this.operationState = OperationState.NORMAL;
				} else if (unlimitedSoc <= Constants.FORCE_CHARGE_LOWER_THRESHOLD) {
					this.operationState = OperationState.FORCE_CHARGE;
				}
				if (minDischargeMaxCurrent > 0) {
					minDischargeMaxCurrent = 0;
				}
				this.forceChargeWasActive = false;
				break;
			case FORCE_CHARGE:
				if (unlimitedSoc >= Constants.FORCE_CHARGE_UPPER_THRESHOLD) {
					this.operationState = OperationState.ONLY_CHARGE;
				}
				// on Entry:
				if (!this.forceChargeWasActive) {
					this.timeAtEntryForceCharge = Instant.now();
				} else if ((Duration.between(this.timeAtEntryForceCharge, Instant.now())
						.getSeconds() > Constants.DEEP_DISCHARGE_PROTECTION_ERROR_DELAY)
						&& (batteryPower > -Constants.FORCE_CHARGE_POWER_ERROR_THRESHOLD)) {
					this.channel(BatteryFeneconF2bBmw.ChannelId.DEEP_DISCHARGE_PROTECTION_LIMIT_SOC).setNextValue(true);
				}
				this.forceChargeWasActive = true;
				if (batteryLinkVoltageHighRes != 0) {
					minDischargeMaxCurrent = -Constants.FORCE_CHARGE_DISCHARGE_POWER / (batteryLinkVoltageHighRes);
				}
				break;
			}
		} else {
			this.operationState = OperationState.OFF;
			minChargeMaxCurrent = 0;
			minDischargeMaxCurrent = 0;
		}

		this.channel(BatteryFeneconF2bBmw.ChannelId.OPERATION_STATE).setNextValue(this.operationState);
		this._setChargeMaxCurrent(minChargeMaxCurrent);
		this._setDischargeMaxCurrent(minDischargeMaxCurrent);
	}

	// TODO drop after battery protection implementation
	private int voltageRegulatorCalculateMaxCurrent(int batteryCurrent, int batteryVoltage, int batteryLimitVoltage,
			BiFunction<Double, Double, Double> typeUtilsMethods, Consumer<Integer> batteryMethod) {

		// Update CycleTime of PT1-filter
		this.pt1FilterMaxCurrentVoltLimit.setCycleTime(this.cycle.getCycleTime() / 1000.0);

		// Calculate charge maximum current
		var deltaChargeCurrent = (TypeUtils.abs(TypeUtils.subtract(batteryVoltage, batteryLimitVoltage))
				+ Constants.VOLTAGE_CONTROL_OFFSET) / Constants.INNER_RESISTANCE;
		var maxCurrentVoltLimit = typeUtilsMethods.apply(deltaChargeCurrent, (double) batteryCurrent);
		// apply filter
		int resultMaxCurrent = (int) (this.pt1FilterMaxCurrentVoltLimit.applyPt1Filter(maxCurrentVoltLimit.intValue())
				/ 10);

		// compare with force charge-discharge power
		var forceChargeDischargePower = TypeUtils
				.multiply(TypeUtils.divide(Constants.FORCE_CHARGE_DISCHARGE_POWER, (batteryVoltage / 10)), -1);

		if (resultMaxCurrent < forceChargeDischargePower) {
			resultMaxCurrent = forceChargeDischargePower;
		}
		batteryMethod.accept(resultMaxCurrent);
		return resultMaxCurrent;
	}

	@Override
	public BmwOnChangeHandler getDeviceSpecificOnChangeHandler() {
		return BMW_ON_CHANGE_HANDLER;
	}
}
