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

import com.google.common.base.Objects;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.sinexcel.statemachine.Context;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverterChain;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
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
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery-Inverter.Sinexcel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		}) //
public class SinexcelImpl extends AbstractOpenemsModbusComponent implements Sinexcel, OffGridBatteryInverter,
		ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, ModbusComponent, OpenemsComponent, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(SinexcelImpl.class);

	public static final int MAX_APPARENT_POWER = 30_000;
	public static final int DEFAULT_UNIT_ID = 1;

	private static final int MAX_CURRENT = 90; // [A]
	private static final int DEFAULT_EMS_TIMEOUT = 60;
	private static final int DEFAULT_BMS_TIMEOUT = 0;
	private static final int DEFAULT_GRID_EXISTENCE_DETECTION_ON = 0;
	private static final int DEFAULT_POWER_CHANGE_MODE = 0; // 0 = STEP_MODE; 1 = RAMP_MODE
	private static final int MAX_TOPPING_CHARGE_VOLTAGE = 750; 

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	protected Config config;

	public SinexcelImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				OffGridBatteryInverter.ChannelId.values(), //
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

		// Set Default Settings
		this.setDefaultSettings();

		// Set Battery Limits
		this.setBatteryLimits(battery);

		// Prepare Context
		Context context = new Context(this, this.config, this.targetGridMode.get(), setActivePower, setReactivePower);

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
	 * Compares and sets some default settings on the inverter, like Timeout.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void setDefaultSettings() throws OpenemsNamedException {
		if (!Objects.equal(this.getEmsTimeout().get(), DEFAULT_EMS_TIMEOUT)) {
			this.setEmsTimeout(DEFAULT_EMS_TIMEOUT);
		}
		if (!Objects.equal(this.getBmsTimeout().get(), DEFAULT_BMS_TIMEOUT)) {
			this.setBmsTimeout(DEFAULT_BMS_TIMEOUT);
		}
		if (!Objects.equal(this.getGridExistenceDetectionOn().get(), DEFAULT_GRID_EXISTENCE_DETECTION_ON)) {
			this.setGridExistenceDetectionOn(DEFAULT_GRID_EXISTENCE_DETECTION_ON);
		}
		if (!Objects.equal(this.getPowerChangeMode().get(), DEFAULT_POWER_CHANGE_MODE)) {
			this.setPowerChangeMode(DEFAULT_POWER_CHANGE_MODE);
		}
	}

	/**
	 * Sets the Battery Limits.
	 * 
	 * @param battery the linked {@link Battery}
	 * @throws OpenemsNamedException on error
	 */
	private void setBatteryLimits(Battery battery) throws OpenemsNamedException {

		// Upper voltage limit of battery protection >= Topping charge voltage >= Float
		// charge voltage >= Lower voltage limit of battery protection (814 >= 809 >=
		// 808 >= 813).

		// Discharge Min Voltage
		IntegerWriteChannel dischargeMinVoltageChannel = this.channel(Sinexcel.ChannelId.DISCHARGE_MIN_V);
		Integer dischargeMinVoltage = battery.getDischargeMinVoltage().get();
		dischargeMinVoltageChannel.setNextWriteValue(dischargeMinVoltage);

		// Charge Max Voltage
		IntegerWriteChannel chargeMaxVoltageChannel = this.channel(Sinexcel.ChannelId.CHARGE_MAX_V);
		Integer chargeMaxVoltage = battery.getChargeMaxVoltage().get();
		chargeMaxVoltageChannel.setNextWriteValue(chargeMaxVoltage);

		IntegerWriteChannel setSlowChargeVoltage = this.channel(Sinexcel.ChannelId.TOPPING_CHARGE_VOLTAGE);
		setSlowChargeVoltage.setNextWriteValue(TypeUtils.min(chargeMaxVoltage, MAX_TOPPING_CHARGE_VOLTAGE));

		IntegerWriteChannel setFloatChargeVoltage = this.channel(Sinexcel.ChannelId.FLOAT_CHARGE_VOLTAGE);
		setFloatChargeVoltage.setNextWriteValue(TypeUtils.min(chargeMaxVoltage, MAX_TOPPING_CHARGE_VOLTAGE));

		// Discharge Max Current
		// negative value is corrected as zero
		IntegerWriteChannel dischargeMaxCurrentChannel = this.channel(Sinexcel.ChannelId.DISCHARGE_MAX_A);
		dischargeMaxCurrentChannel.setNextWriteValue(//
				TypeUtils.fitWithin(0 /* enforce positive */, MAX_CURRENT, battery.getDischargeMaxCurrent().orElse(0)));

		// Charge Max Current
		// negative value is corrected as zero
		IntegerWriteChannel chargeMaxCurrentChannel = this.channel(Sinexcel.ChannelId.CHARGE_MAX_A);
		chargeMaxCurrentChannel.setNextWriteValue(//
				TypeUtils.fitWithin(0 /* enforce positive */, MAX_CURRENT, battery.getChargeMaxCurrent().orElse(0)));
	}

	@Override
	public String debugLog() {
		return this.stateMachine.getCurrentState().asCamelCase() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

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

	protected final AtomicReference<TargetGridMode> targetGridMode = new AtomicReference<>(TargetGridMode.GO_ON_GRID);

	@Override
	public void setTargetGridMode(TargetGridMode targetGridMode) {
		if (this.targetGridMode.getAndSet(targetGridMode) != targetGridMode) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	@Override
	public BatteryInverterConstraint[] getStaticConstraints() throws OpenemsException {
		if (this.stateMachine.getCurrentState() == State.RUNNING) {
			return BatteryInverterConstraint.NO_CONSTRAINTS;

		} else {
			// Block any power as long as we are not RUNNING
			return new BatteryInverterConstraint[] { //
					new BatteryInverterConstraint("Sinexcel inverter not ready", Phase.ALL, Pwr.REACTIVE, //
							Relationship.EQUALS, 0d), //
					new BatteryInverterConstraint("Sinexcel inverter not ready", Phase.ALL, Pwr.ACTIVE, //
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
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode) //
		);
	}

	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0x001, Priority.ONCE, //
						m(Sinexcel.ChannelId.MODEL, new StringWordElement(0x001, 16)), //
						m(Sinexcel.ChannelId.SERIAL, new StringWordElement(0x011, 8))), //

				new FC3ReadRegistersTask(0x020, Priority.HIGH, //
						m(new BitsWordElement(0x020, this) //
								.bit(0, Sinexcel.ChannelId.STATE_16) //
								.bit(1, Sinexcel.ChannelId.STATE_17) //
								.bit(2, OffGridBatteryInverter.ChannelId.INVERTER_STATE) //
								.bit(3, Sinexcel.ChannelId.STATE_19) //
								.bit(4, Sinexcel.ChannelId.STATE_20))),
				new FC3ReadRegistersTask(0x024, Priority.LOW, //
						m(new BitsWordElement(0x024, this) //
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
								.bit(12, Sinexcel.ChannelId.STATE_33)),
						m(new BitsWordElement(0x025, this) //
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
								.bit(15, Sinexcel.ChannelId.STATE_49)),
						m(new BitsWordElement(0x026, this) //
								.bit(0, Sinexcel.ChannelId.STATE_50) //
								.bit(2, Sinexcel.ChannelId.STATE_52) //
								.bit(3, Sinexcel.ChannelId.STATE_53) //
								.bit(4, Sinexcel.ChannelId.STATE_54)),
						m(new BitsWordElement(0x027, this) //
								.bit(0, Sinexcel.ChannelId.STATE_55) //
								.bit(1, Sinexcel.ChannelId.STATE_56) //
								.bit(2, Sinexcel.ChannelId.STATE_57) //
								.bit(3, Sinexcel.ChannelId.STATE_58)),
						m(new BitsWordElement(0x028, this) //
								.bit(0, Sinexcel.ChannelId.STATE_59) //
								.bit(1, Sinexcel.ChannelId.STATE_60) //
								.bit(2, Sinexcel.ChannelId.STATE_61) //
								.bit(3, Sinexcel.ChannelId.STATE_62) //
								.bit(4, Sinexcel.ChannelId.STATE_63) //
								.bit(5, Sinexcel.ChannelId.STATE_64)),
						new DummyRegisterElement(0x029, 0x02A), m(new BitsWordElement(0x02B, this) //
								.bit(0, Sinexcel.ChannelId.STATE_65) //
								.bit(1, Sinexcel.ChannelId.STATE_66) //
								.bit(2, Sinexcel.ChannelId.STATE_67) //
								.bit(3, Sinexcel.ChannelId.STATE_68)),
						m(new BitsWordElement(0x02C, this) //
								.bit(0, Sinexcel.ChannelId.STATE_69) //
								.bit(1, Sinexcel.ChannelId.STATE_70) //
								.bit(2, Sinexcel.ChannelId.STATE_71) //
								.bit(3, Sinexcel.ChannelId.STATE_72) //
								.bit(4, Sinexcel.ChannelId.STATE_73)),
						new DummyRegisterElement(0x02D, 0x02E), m(new BitsWordElement(0x02F, this) //
								.bit(0, Sinexcel.ChannelId.STATE_74))),

				new FC3ReadRegistersTask(0x065, Priority.LOW, //
						m(Sinexcel.ChannelId.INVOUTVOLT_L1, new UnsignedWordElement(0x065),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.INVOUTVOLT_L2, new UnsignedWordElement(0x066),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.INVOUTVOLT_L3, new UnsignedWordElement(0x067),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.INVOUTCURRENT_L1, new UnsignedWordElement(0x068),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.INVOUTCURRENT_L2, new UnsignedWordElement(0x069),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.INVOUTCURRENT_L3, new UnsignedWordElement(0x06A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(0x06B, 0x07D), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x07E),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new UnsignedDoublewordElement(0x080),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0x082, 0x083),
						m(Sinexcel.ChannelId.TEMPERATURE, new SignedWordElement(0x084)),
						new DummyRegisterElement(0x085, 0x08C), //
						m(Sinexcel.ChannelId.DC_POWER, new SignedWordElement(0x08D),
								ElementToChannelConverter.SCALE_FACTOR_1),
						new DummyRegisterElement(0x08E, 0x08F), //
						m(Sinexcel.ChannelId.ANALOG_DC_CHARGE_ENERGY, new UnsignedDoublewordElement(0x090)), //
						m(Sinexcel.ChannelId.ANALOG_DC_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x092))), //

				new FC6WriteRegisterTask(0x087, //
						m(Sinexcel.ChannelId.SET_ACTIVE_POWER, new SignedWordElement(0x087),
								ElementToChannelConverter.SCALE_FACTOR_2)), // in 100 W

				new FC6WriteRegisterTask(0x088, //
						m(Sinexcel.ChannelId.SET_REACTIVE_POWER, new SignedWordElement(0x088),
								ElementToChannelConverter.SCALE_FACTOR_2)), // in 100 var

				new FC3ReadRegistersTask(0x08A, Priority.LOW,
						m(OffGridBatteryInverter.ChannelId.OFF_GRID_FREQUENCY, new SignedWordElement(0x08A), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				new FC16WriteRegistersTask(0x08A,
						m(OffGridBatteryInverter.ChannelId.OFF_GRID_FREQUENCY, new SignedWordElement(0x08A), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

				new FC6WriteRegisterTask(0x147, m(Sinexcel.ChannelId.EMS_TIMEOUT, new UnsignedWordElement(0x147))),
				new FC6WriteRegisterTask(0x149, m(Sinexcel.ChannelId.BMS_TIMEOUT, new UnsignedWordElement(0x149))),

				new FC3ReadRegistersTask(0x147, Priority.ONCE,
						m(Sinexcel.ChannelId.EMS_TIMEOUT, new UnsignedWordElement(0x147)), //
						new DummyRegisterElement(0x148),
						m(Sinexcel.ChannelId.BMS_TIMEOUT, new UnsignedWordElement(0x149))),

				new FC3ReadRegistersTask(0x220, Priority.ONCE,
						m(Sinexcel.ChannelId.VERSION, new StringWordElement(0x220, 8))), //

				new FC3ReadRegistersTask(0x248, Priority.HIGH, //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x248), //
								new ElementToChannelConverterChain(
										ElementToChannelConverter.SCALE_FACTOR_1, IGNORE_LESS_THAN_100)),
						new DummyRegisterElement(0x249),
						m(Sinexcel.ChannelId.FREQUENCY, new SignedWordElement(0x24A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						new DummyRegisterElement(0x24B, 0x24D), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x24E)), //
						new DummyRegisterElement(0x24F, 0x254), //
						m(Sinexcel.ChannelId.DC_CURRENT, new SignedWordElement(0x255),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(0x256), //
						m(Sinexcel.ChannelId.DC_VOLTAGE, new UnsignedWordElement(0x257),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				// TODO merge tasks
				new FC3ReadRegistersTask(0x260, Priority.HIGH,
						m(Sinexcel.ChannelId.SINEXCEL_STATE, new UnsignedWordElement(0x260))), //

				new FC3ReadRegistersTask(0x262, Priority.LOW, //
						m(new BitsWordElement(0x262, this) //
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

				// Required in high priority during startup/stop phase or on change of target
				// grid-mode
				new FC3ReadRegistersTask(0x28A, Priority.HIGH, //
						m(Sinexcel.ChannelId.SET_START_COMMAND, new UnsignedWordElement(0x28A)),
						m(Sinexcel.ChannelId.SET_STOP_COMMAND, new UnsignedWordElement(0x28B)),
						m(Sinexcel.ChannelId.CLEAR_FAILURE_CMD, new UnsignedWordElement(0x28C)),
						m(Sinexcel.ChannelId.SET_ON_GRID_MODE, new UnsignedWordElement(0x28D)),
						m(Sinexcel.ChannelId.SET_OFF_GRID_MODE, new UnsignedWordElement(0x28E)),
						new DummyRegisterElement(0x28F),
						m(Sinexcel.ChannelId.SET_INTERN_DC_RELAY, new UnsignedWordElement(0x290))),

				new FC6WriteRegisterTask(0x28A, //
						m(Sinexcel.ChannelId.SET_START_COMMAND, new UnsignedWordElement(0x28A))),
				new FC6WriteRegisterTask(0x28B, //
						m(Sinexcel.ChannelId.SET_STOP_COMMAND, new UnsignedWordElement(0x28B))),
				new FC6WriteRegisterTask(0x28C, //
						m(Sinexcel.ChannelId.CLEAR_FAILURE_CMD, new UnsignedWordElement(0x28C))),
				new FC6WriteRegisterTask(0x28D, //
						m(Sinexcel.ChannelId.SET_ON_GRID_MODE, new UnsignedWordElement(0x28D))),
				new FC6WriteRegisterTask(0x28E, //
						m(Sinexcel.ChannelId.SET_OFF_GRID_MODE, new UnsignedWordElement(0x28E))),
				new FC6WriteRegisterTask(0x290, // FIXME: not documented!
						m(Sinexcel.ChannelId.SET_INTERN_DC_RELAY, new UnsignedWordElement(0x290))),

				new FC3ReadRegistersTask(0x316, Priority.LOW, //
						m(Sinexcel.ChannelId.ANTI_ISLANDING, new UnsignedWordElement(0x316))),
				new FC6WriteRegisterTask(0x316, m(Sinexcel.ChannelId.ANTI_ISLANDING, new UnsignedWordElement(0x316))),

				new FC3ReadRegistersTask(0x319, Priority.LOW, //
						m(Sinexcel.ChannelId.POWER_CHANGE_MODE, new UnsignedWordElement(0x319))),
				new FC6WriteRegisterTask(0x319,
						m(Sinexcel.ChannelId.POWER_CHANGE_MODE, new UnsignedWordElement(0x319))),

				new FC3ReadRegistersTask(0x31D, Priority.LOW, //
						m(Sinexcel.ChannelId.GRID_EXISTENCE_DETECTION_ON, new UnsignedWordElement(0x31D))),
				new FC6WriteRegisterTask(0x31D,
						m(Sinexcel.ChannelId.GRID_EXISTENCE_DETECTION_ON, new UnsignedWordElement(0x31D))),

				new FC3ReadRegistersTask(0x328, Priority.LOW, //
						m(Sinexcel.ChannelId.FLOAT_CHARGE_VOLTAGE, new UnsignedWordElement(0x328),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.TOPPING_CHARGE_VOLTAGE, new UnsignedWordElement(0x329),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(0x32A),
						m(Sinexcel.ChannelId.CHARGE_MAX_A, new UnsignedWordElement(0x32B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(Sinexcel.ChannelId.DISCHARGE_MAX_A, new UnsignedWordElement(0x32C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.DISCHARGE_MIN_V, new UnsignedWordElement(0x32D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Sinexcel.ChannelId.CHARGE_MAX_V, new UnsignedWordElement(0x32E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

				new FC6WriteRegisterTask(0x328,
						m(Sinexcel.ChannelId.FLOAT_CHARGE_VOLTAGE, new UnsignedWordElement(0x328),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

				new FC6WriteRegisterTask(0x329,
						m(Sinexcel.ChannelId.TOPPING_CHARGE_VOLTAGE, new UnsignedWordElement(0x329),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

				new FC6WriteRegisterTask(0x32B, //
						m(Sinexcel.ChannelId.CHARGE_MAX_A, new UnsignedWordElement(0x32B),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC6WriteRegisterTask(0x32C, //
						m(Sinexcel.ChannelId.DISCHARGE_MAX_A, new UnsignedWordElement(0x32C),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC6WriteRegisterTask(0x32D,
						m(Sinexcel.ChannelId.DISCHARGE_MIN_V, new UnsignedWordElement(0x32D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				new FC6WriteRegisterTask(0x32E, m(Sinexcel.ChannelId.CHARGE_MAX_V, new UnsignedWordElement(0x32E),
						ElementToChannelConverter.SCALE_FACTOR_MINUS_1)));
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

	/**
	 * Executes a Soft-Start. Sets the internal DC relay. Once this register is set
	 * to 1, the PCS will start the soft-start procedure, otherwise, the PCS will do
	 * nothing on the DC input Every time the PCS is powered off, this register will
	 * be cleared to 0. In some particular cases, the BMS wants to re-softstart, the
	 * EMS should actively clear this register to 0, after BMS soft-started, set it
	 * to 1 again.
	 *
	 * @param switchOn true to switch internal DC relay on
	 * @throws OpenemsNamedException on error
	 */
	public void softStart(boolean switchOn) throws OpenemsNamedException {
		IntegerWriteChannel setDcRelay = this.channel(Sinexcel.ChannelId.SET_INTERN_DC_RELAY);
		setDcRelay.setNextWriteValue(switchOn ? 1 : 0);
	}
}
