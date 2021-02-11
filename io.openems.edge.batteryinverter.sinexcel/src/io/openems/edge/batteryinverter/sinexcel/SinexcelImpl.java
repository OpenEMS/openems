package io.openems.edge.batteryinverter.sinexcel;

import java.util.concurrent.atomic.AtomicReference;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.sinexcel.statemachine.Context;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverterChain;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery-Inverter.Kaco.Sinexcel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		}) //
public class SinexcelImpl extends AbstractOpenemsModbusComponent implements Sinexcel, ManagedSymmetricBatteryInverter,
		SymmetricBatteryInverter, OpenemsComponent, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(SinexcelImpl.class);

	public static final int MAX_APPARENT_POWER = 30_000;
	public static final int DEFAULT_UNIT_ID = 1;

	private final static int MAX_CURRENT = 90; // [A]

	protected int slowChargeVoltage = 4370; // for new batteries - 3940
	protected int floatChargeVoltage = 4370; // for new batteries - 3940

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	@Reference
	protected ComponentManager componentManager;

	protected Config config;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	public SinexcelImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Sinexcel.ChannelId.values() //
		);
		this._setMaxApparentPower(SinexcelImpl.MAX_APPARENT_POWER);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.config = config;
		this.slowChargeVoltage = config.toppingCharge();
		this.floatChargeVoltage = config.toppingCharge();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {
		// Store the current State
		this.channel(Sinexcel.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Set Battery Limits
		this.setBatteryLimits(battery);

		// Prepare Context
		Context context = new Context(this, this.config, setActivePower, setReactivePower);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(Sinexcel.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(Sinexcel.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	/**
	 * Sets the Battery Limits.
	 * 
	 * @param battery the linked {@link Battery}
	 * @throws OpenemsNamedException on error
	 */
	private void setBatteryLimits(Battery battery) throws OpenemsNamedException {
		// Discharge Min Voltage
		IntegerWriteChannel dischargeMinVoltageChannel = this.channel(Sinexcel.ChannelId.DISCHARGE_MIN_V);
		Integer dischargeMinVoltage = battery.getDischargeMinVoltage().get();
		dischargeMinVoltageChannel.setNextWriteValue(dischargeMinVoltage);
		// Charge Max Voltage
		IntegerWriteChannel chargeMaxVoltageChannel = this.channel(Sinexcel.ChannelId.CHARGE_MAX_V);
		Integer chargeMaxVoltage = battery.getChargeMaxVoltage().get();
		chargeMaxVoltageChannel.setNextWriteValue(chargeMaxVoltage);

		// Discharge Max Current
		// negative value is corrected as zero
		IntegerWriteChannel dischargeMaxCurrentChannel = this.channel(Sinexcel.ChannelId.DISCHARGE_MAX_A);
		dischargeMaxCurrentChannel.setNextWriteValue(//
				/* enforce positive */ Math.max(0, //
						/* apply max current */ Math.min(MAX_CURRENT, battery.getDischargeMaxCurrent().orElse(0)) //
				));
		// Charge Max Current
		// negative value is corrected as zero
		IntegerWriteChannel chargeMaxCurrentChannel = this.channel(Sinexcel.ChannelId.CHARGE_MAX_A);
		chargeMaxCurrentChannel.setNextWriteValue(//
				/* enforce positive */ Math.max(0, //
						/* apply max current */ Math.min(MAX_CURRENT, battery.getChargeMaxCurrent().orElse(0)) //
				));
	}

	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //

				new FC6WriteRegisterTask(0x028A, //
						m(Sinexcel.ChannelId.MOD_ON_CMD, new UnsignedWordElement(0x028A))),
				new FC6WriteRegisterTask(0x028B, //
						m(Sinexcel.ChannelId.MOD_OFF_CMD, new UnsignedWordElement(0x028B))),
				new FC6WriteRegisterTask(0x028C, //
						m(Sinexcel.ChannelId.CLEAR_FAILURE_CMD, new UnsignedWordElement(0x028C))),
				new FC6WriteRegisterTask(0x028D, //
						m(Sinexcel.ChannelId.ON_GRID_CMD, new UnsignedWordElement(0x028D))),
				new FC6WriteRegisterTask(0x028E, //
						m(Sinexcel.ChannelId.OFF_GRID_CMD, new UnsignedWordElement(0x028E))),

				new FC6WriteRegisterTask(0x0290, // FIXME: not documented!
						m(Sinexcel.ChannelId.SET_INTERN_DC_RELAY, new UnsignedWordElement(0x0290))),

				new FC6WriteRegisterTask(0x0087, //
						m(Sinexcel.ChannelId.SET_ACTIVE_POWER, new SignedWordElement(0x0087),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), //
				new FC6WriteRegisterTask(0x0088,
						m(Sinexcel.ChannelId.SET_REACTIVE_POWER, new SignedWordElement(0x0088),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), //

				new FC16WriteRegistersTask(0x032B, //
						m(Sinexcel.ChannelId.CHARGE_MAX_A, new UnsignedWordElement(0x032B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(Sinexcel.ChannelId.DISCHARGE_MAX_A, new UnsignedWordElement(0x032C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), //

				new FC6WriteRegisterTask(0x0329,
						m(Sinexcel.ChannelId.SET_SLOW_CHARGE_VOLTAGE, new UnsignedWordElement(0x0329))),
				new FC6WriteRegisterTask(0x0328,
						m(Sinexcel.ChannelId.SET_FLOAT_CHARGE_VOLTAGE, new UnsignedWordElement(0x0328))),

				new FC16WriteRegistersTask(0x032D,
						m(Sinexcel.ChannelId.DISCHARGE_MIN_V, new UnsignedWordElement(0x032D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.CHARGE_MAX_V, new UnsignedWordElement(0x032E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				new FC16WriteRegistersTask(0x007E,
						m(Sinexcel.ChannelId.SET_ANALOG_CHARGE_ENERGY, new UnsignedDoublewordElement(0x007E))),
				new FC16WriteRegistersTask(0x0080,
						m(Sinexcel.ChannelId.SET_ANALOG_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x0080))),
				new FC16WriteRegistersTask(0x0090,
						m(Sinexcel.ChannelId.SET_ANALOG_DC_CHARGE_ENERGY, new UnsignedDoublewordElement(0x0090))),
				new FC16WriteRegistersTask(0x0092,
						m(Sinexcel.ChannelId.SET_ANALOG_DC_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x0092))),

				new FC3ReadRegistersTask(0x0001, Priority.ONCE, //
						m(Sinexcel.ChannelId.MODEL, new StringWordElement(0x0001, 16)), //
						m(Sinexcel.ChannelId.SERIAL, new StringWordElement(0x0011, 8))), //
				new FC3ReadRegistersTask(0x0065, Priority.LOW, //
						m(Sinexcel.ChannelId.INVOUTVOLT_L1, new UnsignedWordElement(0x0065),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.INVOUTVOLT_L2, new UnsignedWordElement(0x0066),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.INVOUTVOLT_L3, new UnsignedWordElement(0x0067),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.INVOUTCURRENT_L1, new UnsignedWordElement(0x0068),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.INVOUTCURRENT_L2, new UnsignedWordElement(0x0069),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.INVOUTCURRENT_L3, new UnsignedWordElement(0x006A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(0x006B, 0x007D), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x007E),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(0x0080),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0x0082, 0x0083),
						m(Sinexcel.ChannelId.TEMPERATURE, new SignedWordElement(0x0084)),
						new DummyRegisterElement(0x0085, 0x008C), //
						m(Sinexcel.ChannelId.DC_POWER, new SignedWordElement(0x008D),
								ElementToChannelConverter.SCALE_FACTOR_1),
						new DummyRegisterElement(0x008E, 0x008F), //
						m(Sinexcel.ChannelId.ANALOG_DC_CHARGE_ENERGY, new UnsignedDoublewordElement(0x0090)), //
						m(Sinexcel.ChannelId.ANALOG_DC_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x0092))), //

				new FC3ReadRegistersTask(0x0220, Priority.ONCE,
						m(Sinexcel.ChannelId.VERSION, new StringWordElement(0x0220, 8))), //

				new FC3ReadRegistersTask(0x0248, Priority.HIGH, //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x0248), //
								new ElementToChannelConverterChain(
										ElementToChannelConverter.SCALE_FACTOR_1, IGNORE_LESS_THAN_100)),
						new DummyRegisterElement(0x0249),
						m(Sinexcel.ChannelId.FREQUENCY, new SignedWordElement(0x024A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						new DummyRegisterElement(0x024B, 0x024D), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x024E)), //
						new DummyRegisterElement(0x024F, 0x0254), //
						m(Sinexcel.ChannelId.DC_CURRENT, new SignedWordElement(0x0255),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(0x0256), //
						m(Sinexcel.ChannelId.DC_VOLTAGE, new UnsignedWordElement(0x0257),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(0x0258, 0x0259), //
						m(Sinexcel.ChannelId.SINEXCEL_STATE, new UnsignedWordElement(0x025A))), //

				new FC3ReadRegistersTask(0x032D, Priority.LOW,
						m(Sinexcel.ChannelId.LOWER_VOLTAGE_LIMIT, new UnsignedWordElement(0x032D), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.UPPER_VOLTAGE_LIMIT, new UnsignedWordElement(0x032E), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

				new FC3ReadRegistersTask(0x0262, Priority.LOW, //
						m(new BitsWordElement(0x0262, this) //
								.bit(0, Sinexcel.ChannelId.STATE_0) //
								.bit(1, Sinexcel.ChannelId.STATE_1) //
								.bit(2, Sinexcel.ChannelId.STATE_2) //
								.bit(3, Sinexcel.ChannelId.STATE_3) //
								.bit(4, Sinexcel.ChannelId.STATE_4) //
								.bit(5, Sinexcel.ChannelId.STATE_5) //
								.bit(6, Sinexcel.ChannelId.AUTOMATIC_STANDBY_MODE) //
								.bit(7, Sinexcel.ChannelId.STATE_7) //
								.bit(8, Sinexcel.ChannelId.STATE_8) //
								.bit(9, Sinexcel.ChannelId.STATE_9) //
								.bit(10, Sinexcel.ChannelId.STATE_10) //
								.bit(11, Sinexcel.ChannelId.STATE_11) //
								.bit(12, Sinexcel.ChannelId.STATE_12) //
								.bit(13, Sinexcel.ChannelId.STATE_13) //
								.bit(14, Sinexcel.ChannelId.STATE_14) //
								.bit(15, Sinexcel.ChannelId.STATE_15))),

				new FC3ReadRegistersTask(0x0260, Priority.LOW, //
						m(new BitsWordElement(0x0260, this) //
								.bit(1, Sinexcel.ChannelId.SINEXCEL_STATE_1) //
								.bit(2, Sinexcel.ChannelId.SINEXCEL_STATE_2) //
								.bit(3, Sinexcel.ChannelId.SINEXCEL_STATE_3) //
								.bit(4, Sinexcel.ChannelId.SINEXCEL_STATE_4) //
								.bit(5, Sinexcel.ChannelId.SINEXCEL_STATE_5) //
								.bit(6, Sinexcel.ChannelId.SINEXCEL_STATE_6) //
								.bit(7, Sinexcel.ChannelId.SINEXCEL_STATE_7) //
								.bit(8, Sinexcel.ChannelId.SINEXCEL_STATE_8) //
								.bit(9, Sinexcel.ChannelId.SINEXCEL_STATE_9))),

				new FC3ReadRegistersTask(0x0020, Priority.LOW, //
						m(new BitsWordElement(0x0020, this) //
								.bit(0, Sinexcel.ChannelId.STATE_16) //
								.bit(1, Sinexcel.ChannelId.STATE_17) //
								.bit(2, Sinexcel.ChannelId.STATE_ON) //
								.bit(3, Sinexcel.ChannelId.STATE_19) //
								.bit(4, Sinexcel.ChannelId.STATE_20))),

				new FC3ReadRegistersTask(0x0024, Priority.LOW, //
						m(new BitsWordElement(0x0024, this) //
								.bit(0, Sinexcel.ChannelId.STATE_21) //
								.bit(1, Sinexcel.ChannelId.STATE_22) //
								.bit(2, Sinexcel.ChannelId.STATE_23) //
								.bit(3, Sinexcel.ChannelId.STATE_24) //
								.bit(4, Sinexcel.ChannelId.STATE_25) //
								.bit(5, Sinexcel.ChannelId.STATE_26) //
								.bit(6, Sinexcel.ChannelId.STATE_27) //
								.bit(7, Sinexcel.ChannelId.STATE_28) //
								.bit(8, Sinexcel.ChannelId.STATE_29) //
								.bit(9, Sinexcel.ChannelId.STATE_30) //
								.bit(10, Sinexcel.ChannelId.STATE_31) //
								.bit(11, Sinexcel.ChannelId.STATE_32) //
								.bit(12, Sinexcel.ChannelId.STATE_33))),

				new FC3ReadRegistersTask(0x0025, Priority.LOW, //
						m(new BitsWordElement(0x0025, this) //
								.bit(0, Sinexcel.ChannelId.STATE_34) //
								.bit(1, Sinexcel.ChannelId.STATE_35) //
								.bit(2, Sinexcel.ChannelId.STATE_36) //
								.bit(3, Sinexcel.ChannelId.STATE_37) //
								.bit(4, Sinexcel.ChannelId.STATE_38) //
								.bit(5, Sinexcel.ChannelId.STATE_39) //
								.bit(6, Sinexcel.ChannelId.STATE_40) //
								.bit(7, Sinexcel.ChannelId.STATE_41) //
								.bit(8, Sinexcel.ChannelId.STATE_42) //
								.bit(9, Sinexcel.ChannelId.STATE_43) //
								.bit(10, Sinexcel.ChannelId.STATE_44) //
								.bit(11, Sinexcel.ChannelId.STATE_45) //
								.bit(13, Sinexcel.ChannelId.STATE_47) //
								.bit(14, Sinexcel.ChannelId.STATE_48) //
								.bit(15, Sinexcel.ChannelId.STATE_49))),

				new FC3ReadRegistersTask(0x0026, Priority.LOW, //
						m(new BitsWordElement(0x0026, this) //
								.bit(0, Sinexcel.ChannelId.STATE_50) //
								.bit(2, Sinexcel.ChannelId.STATE_52) //
								.bit(3, Sinexcel.ChannelId.STATE_53) //
								.bit(4, Sinexcel.ChannelId.STATE_54))),

				new FC3ReadRegistersTask(0x0027, Priority.LOW, //
						m(new BitsWordElement(0x0027, this) //
								.bit(0, Sinexcel.ChannelId.STATE_55) //
								.bit(1, Sinexcel.ChannelId.STATE_56) //
								.bit(2, Sinexcel.ChannelId.STATE_57) //
								.bit(3, Sinexcel.ChannelId.STATE_58))),

				new FC3ReadRegistersTask(0x0028, Priority.LOW, //
						m(new BitsWordElement(0x0028, this) //
								.bit(0, Sinexcel.ChannelId.STATE_59) //
								.bit(1, Sinexcel.ChannelId.STATE_60) //
								.bit(2, Sinexcel.ChannelId.STATE_61) //
								.bit(3, Sinexcel.ChannelId.STATE_62) //
								.bit(4, Sinexcel.ChannelId.STATE_63) //
								.bit(5, Sinexcel.ChannelId.STATE_64))),

				new FC3ReadRegistersTask(0x002B, Priority.LOW, //
						m(new BitsWordElement(0x002B, this) //
								.bit(0, Sinexcel.ChannelId.STATE_65) //
								.bit(1, Sinexcel.ChannelId.STATE_66) //
								.bit(2, Sinexcel.ChannelId.STATE_67) //
								.bit(3, Sinexcel.ChannelId.STATE_68))),

				new FC3ReadRegistersTask(0x002C, Priority.LOW, //
						m(new BitsWordElement(0x002C, this) //
								.bit(0, Sinexcel.ChannelId.STATE_69) //
								.bit(1, Sinexcel.ChannelId.STATE_70) //
								.bit(2, Sinexcel.ChannelId.STATE_71) //
								.bit(3, Sinexcel.ChannelId.STATE_72) //
								.bit(4, Sinexcel.ChannelId.STATE_73))),

				new FC3ReadRegistersTask(0x002F, Priority.LOW, //
						m(new BitsWordElement(0x002F, this) //
								.bit(0, Sinexcel.ChannelId.STATE_74))));
	}

	@Override
	public String debugLog() {
		return this.stateMachine.getCurrentState().asCamelCase() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<StartStop>(StartStop.UNDEFINED);

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	@Override
	public StartStop getStartStopTarget() {
		switch (this.config.startStop()) {
		case AUTO:
			// read StartStop-Channel
			return this.startStopTarget.get();

		case START:
			// force START
			return StartStop.START;

		case STOP:
			// force STOP
			return StartStop.STOP;
		}

		assert false;
		return StartStop.UNDEFINED; // can never happen
	}

	@Override
	public BatteryInverterConstraint[] getStaticConstraints() throws OpenemsException {
		if (this.stateMachine.getCurrentState() == State.RUNNING) {
			return BatteryInverterConstraint.NO_CONSTRAINTS;

		} else {
			// Block any power as long as we are not RUNNING
			return new BatteryInverterConstraint[] { //
					new BatteryInverterConstraint("KACO inverter not ready", Phase.ALL, Pwr.REACTIVE, //
							Relationship.EQUALS, 0d), //
					new BatteryInverterConstraint("KACO inverter not ready", Phase.ALL, Pwr.ACTIVE, //
							Relationship.EQUALS, 0d) //
			};
		}
	}

	@Override
	public int getPowerPrecision() {
		return 100;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode) //
		);
	}

	/**
	 * The Sinexcel Battery Inverter claims to outputting a little bit of power even
	 * if it does not. This little filter ignores values for ActivePower less than
	 * 100 (charge/discharge).
	 */
	private static final ElementToChannelConverter IGNORE_LESS_THAN_100 = new ElementToChannelConverter(//
			obj -> {
				if (obj == null) {
					return null;
				}
				int value = (Short) obj;
				if (Math.abs(value) < 100) {
					return 0;
				} else {
					return value;
				}
			}, //
			value -> value);

}
