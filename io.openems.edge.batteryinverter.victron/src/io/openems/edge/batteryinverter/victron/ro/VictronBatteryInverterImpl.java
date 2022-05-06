package io.openems.edge.batteryinverter.victron.ro;

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
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.victron.ro.statemachine.Context;
import io.openems.edge.batteryinverter.victron.ro.statemachine.StateMachine;
import io.openems.edge.batteryinverter.victron.ro.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery-Inverter.Victron", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "="
						+ EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "="
						+ EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		}) //
public class VictronBatteryInverterImpl extends AbstractOpenemsModbusComponent
		implements VictronBatteryInverter, OffGridBatteryInverter,
		ManagedSymmetricBatteryInverter, SymmetricBatteryInverter,
		ModbusComponent, OpenemsComponent, StartStoppable {

	private final Logger log = LoggerFactory
			.getLogger(VictronBatteryInverterImpl.class);

	public static final int MAX_APPARENT_POWER = 15_000;
	public static final int DEFAULT_UNIT_ID = 100;

	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	protected Config config;

	public VictronBatteryInverterImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
//				OffGridBatteryInverter.ChannelId.values(), //
				VictronBatteryInverter.ChannelId.values() //
		);

	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	protected void activate(ComponentContext context, Config config)
			throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(),
				config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}

		// TODO read following values from inverter
		this._setMaxApparentPower(
				VictronBatteryInverterImpl.MAX_APPARENT_POWER);
//		this.getBatteryInverterStateChannel().setNextValue(true);

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower)
			throws OpenemsNamedException {
		// Store the current State
		this.channel(VictronBatteryInverter.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Set Default Settings
		this.setDefaultSettings();

		// Set Battery Limits
		this.setBatteryLimits(battery);

		// Prepare Context
		Context context = new Context(this, this.config,
				this.targetGridMode.get(), setActivePower, setReactivePower);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(VictronBatteryInverter.ChannelId.RUN_FAILED)
					.setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(VictronBatteryInverter.ChannelId.RUN_FAILED)
					.setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

//	/**
//	 * Updates the Channel if its current value is not equal to the new value.
//	 * 
//	 * @param channelId Sinexcel Channel-Id
//	 * @param newValue  {@link OptionsEnum} value.
//	 * @throws IllegalArgumentException on error
//	 */
//	private void updateIfNotEqual(Victron.ChannelId channelId, OptionsEnum value)
//			throws IllegalArgumentException, OpenemsNamedException {
//		this.updateIfNotEqual(channelId, value.getValue());
//	}

//	/**
//	 * Updates the Channel if its current value is not equal to the new value.
//	 * 
//	 * @param channelId Sinexcel Channel-Id
//	 * @param newValue  Integer value.
//	 * @throws IllegalArgumentException on error
//	 */
//	private void updateIfNotEqual(Victron.ChannelId channelId, Integer newValue) throws IllegalArgumentException {
//		WriteChannel<Integer> channel = this.channel(channelId);
//		Value<Integer> currentValue = channel.value();
//		if (currentValue.isDefined() && !Objects.equals(currentValue.get(), newValue)) {
//			try {
//				channel.setNextWriteValue(newValue);
//			} catch (OpenemsNamedException e) {
//				this.logWarn(this.log, "Unable to update Channel [" + channel.address() + "] from [" + currentValue
//						+ "] to [" + newValue + "]");
//				e.printStackTrace();
//			}
//		}
//	}

	/**
	 * Sets some default settings on the inverter, like Timeout.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void setDefaultSettings() throws OpenemsNamedException {
//		this.updateIfNotEqual(Victron.ChannelId.EMS_TIMEOUT, DEFAULT_EMS_TIMEOUT);
//		this.updateIfNotEqual(Victron.ChannelId.BMS_TIMEOUT, DEFAULT_BMS_TIMEOUT);
//		this.updateIfNotEqual(Victron.ChannelId.GRID_EXISTENCE_DETECTION_ON, DEFAULT_GRID_EXISTENCE_DETECTION_ON);
//		this.updateIfNotEqual(Victron.ChannelId.POWER_RISING_MODE, DEFAULT_POWER_RISING_MODE);
	}

	/**
	 * Sets the Battery Limits.
	 * 
	 * @param battery the linked {@link Battery}
	 * @throws OpenemsNamedException on error
	 */
	private void setBatteryLimits(Battery battery)
			throws OpenemsNamedException {
//		// Upper voltage limit of battery protection >= Topping charge voltage >= Float
//		// charge voltage >= Lower voltage limit of battery protection (814 >= 809 >=
//		// 808 >= 813).
//		// Discharge Min Voltage
//		this.updateIfNotEqual(Victron.ChannelId.DISCHARGE_MIN_VOLTAGE, battery.getDischargeMinVoltage().get());
//
//		// Charge Max Voltage
//		this.updateIfNotEqual(Victron.ChannelId.CHARGE_MAX_VOLTAGE, battery.getChargeMaxVoltage().get());
//
//		// Topping Charge Voltage
//		this.updateIfNotEqual(Victron.ChannelId.TOPPING_CHARGE_VOLTAGE,
//				TypeUtils.min(battery.getChargeMaxVoltage().get(), MAX_TOPPING_CHARGE_VOLTAGE));
//
//		// Float Charge Voltage
//		this.updateIfNotEqual(Victron.ChannelId.FLOAT_CHARGE_VOLTAGE,
//				TypeUtils.min(battery.getChargeMaxVoltage().get(), MAX_TOPPING_CHARGE_VOLTAGE));
//
//		// Discharge Max Current
//		// negative value is corrected as zero
//		this.updateIfNotEqual(Victron.ChannelId.DISCHARGE_MAX_CURRENT,
//				TypeUtils.fitWithin(0 /* enforce positive */, MAX_CURRENT, battery.getDischargeMaxCurrent().orElse(0)));
//
//		// Charge Max Current
//		// negative value is corrected as zero
//		this.updateIfNotEqual(Victron.ChannelId.CHARGE_MAX_CURRENT,
//				TypeUtils.fitWithin(0 /* enforce positive */, MAX_CURRENT, battery.getChargeMaxCurrent().orElse(0)));
	}

	@Override
	public String debugLog() {
		return this.stateMachine.getCurrentState().asCamelCase() //
		;// + "|" + this.getGridModeChannel().value().asOptionString();
	}

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(
			StartStop.UNDEFINED);

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

	protected final AtomicReference<TargetGridMode> targetGridMode = new AtomicReference<>(
			TargetGridMode.GO_ON_GRID);

	@Override
	public void setTargetGridMode(TargetGridMode targetGridMode) {
		if (this.targetGridMode.getAndSet(targetGridMode) != targetGridMode) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	@Override
	public BatteryInverterConstraint[] getStaticConstraints()
			throws OpenemsException {
		if (this.stateMachine.getCurrentState() == State.RUNNING) {
			return BatteryInverterConstraint.NO_CONSTRAINTS;

		} else {
			// Block any power as long as we are not RUNNING
			return new BatteryInverterConstraint[] { //
					new BatteryInverterConstraint("Victron inverter not ready",
							Phase.ALL, Pwr.REACTIVE, //
							Relationship.EQUALS, 0d), //
					new BatteryInverterConstraint("Victron inverter not ready",
							Phase.ALL, Pwr.ACTIVE, //
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
				ManagedSymmetricBatteryInverter
						.getModbusSlaveNatureTable(accessMode) //
		);
	}

	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(800, Priority.HIGH, //
						m(VictronBatteryInverter.ChannelId.SERIAL_NUMBER,
								new StringWordElement(800, 6)),
						m(VictronBatteryInverter.ChannelId.CCGX_RELAY1_STATE,
								new UnsignedWordElement(806)),
						m(VictronBatteryInverter.ChannelId.CCGX_RELAY2_STATE,
								new UnsignedWordElement(807)),
						m(VictronBatteryInverter.ChannelId.AC_PV_ON_OUTPUT_POWER_L1,
								new UnsignedWordElement(808)),
						m(VictronBatteryInverter.ChannelId.AC_PV_ON_OUTPUT_POWER_L2,
								new UnsignedWordElement(809)),
						m(VictronBatteryInverter.ChannelId.AC_PV_ON_OUTPUT_POWER_L3,
								new UnsignedWordElement(810)),
						m(VictronBatteryInverter.ChannelId.AC_PV_ON_INPUT_POWER_L1,
								new UnsignedWordElement(811)),
						m(VictronBatteryInverter.ChannelId.AC_PV_ON_INPUT_POWER_L2,
								new UnsignedWordElement(812)),
						m(VictronBatteryInverter.ChannelId.AC_PV_ON_INPUT_POWER_L3,
								new UnsignedWordElement(813)),
						new DummyRegisterElement(814, 816),
						m(VictronBatteryInverter.ChannelId.AC_CONSUMPTION_POWER_L1,
								new UnsignedWordElement(817)),
						m(VictronBatteryInverter.ChannelId.AC_CONSUMPTION_POWER_L2,
								new UnsignedWordElement(818)),
						m(VictronBatteryInverter.ChannelId.AC_CONSUMPTION_POWER_L3,
								new UnsignedWordElement(819)),
						m(VictronBatteryInverter.ChannelId.GRID_POWER_L1,
								new SignedWordElement(820)),
						m(VictronBatteryInverter.ChannelId.GRID_POWER_L2,
								new SignedWordElement(821)),
						m(VictronBatteryInverter.ChannelId.GRID_POWER_L3,
								new SignedWordElement(822)),
						m(VictronBatteryInverter.ChannelId.AC_GENSET_POWER_L1,
								new SignedWordElement(823)),
						m(VictronBatteryInverter.ChannelId.AC_GENSET_POWER_L2,
								new SignedWordElement(824)),
						m(VictronBatteryInverter.ChannelId.AC_GENSET_POWER_L3,
								new SignedWordElement(825)),
						m(VictronBatteryInverter.ChannelId.ACTIVE_INPUT_SOURCE,
								new UnsignedWordElement(826))),
				new FC3ReadRegistersTask(840, Priority.HIGH, //
						m(VictronBatteryInverter.ChannelId.DC_BATTERY_VOLTAGE,
								new UnsignedWordElement(840),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronBatteryInverter.ChannelId.DC_BATTERY_CURRENT,
								new SignedWordElement(841),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(SymmetricBatteryInverter.ChannelId.ACTIVE_POWER,
								new SignedWordElement(842)),
						m(VictronBatteryInverter.ChannelId.BATTERY_SOC,
								new UnsignedWordElement(843)),
						m(VictronBatteryInverter.ChannelId.BATTERY_STATE,
								new UnsignedWordElement(844)),
						m(VictronBatteryInverter.ChannelId.BATTERY_CONSUMED_AMPHOURS,
								new UnsignedWordElement(845),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1_AND_INVERT),
						m(VictronBatteryInverter.ChannelId.BATTERY_TIME_TO_GO,
								new UnsignedWordElement(846),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC3ReadRegistersTask(850, Priority.LOW,
						m(VictronBatteryInverter.ChannelId.DC_PV_POWER,
								new UnsignedWordElement(850)),
						m(VictronBatteryInverter.ChannelId.DC_PV_CURRENT,
								new SignedWordElement(851),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				new FC3ReadRegistersTask(855, Priority.LOW,
						m(VictronBatteryInverter.ChannelId.CHARGER_POWER,
								new UnsignedWordElement(855))),
				new FC3ReadRegistersTask(860, Priority.LOW,
						m(VictronBatteryInverter.ChannelId.DC_SYSTEM_POWER,
								new SignedWordElement(860))),
				new FC3ReadRegistersTask(865, Priority.HIGH, m(
						VictronBatteryInverter.ChannelId.VE_BUS_CHARGE_CURRENT,
						new SignedWordElement(865),
						ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(VictronBatteryInverter.ChannelId.VE_BUS_CHARGE_POWER,
								new SignedWordElement(866))),

				new FC3ReadRegistersTask(2700, Priority.HIGH, m(
						VictronBatteryInverter.ChannelId.ESS_CONTROL_LOOP_SETPOINT,
						new SignedWordElement(2700)),
						m(VictronBatteryInverter.ChannelId.ESS_MAX_CHARGE_CURRENT_PERCENTAGE,
								new UnsignedWordElement(2701)),
						m(VictronBatteryInverter.ChannelId.ESS_MAX_DISCHARGE_CURRENT_PERCENTAGE,
								new UnsignedWordElement(2702)),
						m(VictronBatteryInverter.ChannelId.ESS_CONTROL_LOOP_SETPOINT_SCALE_FACTOR_2,
								new SignedWordElement(2703),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(VictronBatteryInverter.ChannelId.ESS_MAX_DISCHARGE_POWER,
								new UnsignedWordElement(2704),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(VictronBatteryInverter.ChannelId.SYSTEM_MAX_CHARGE_CURRENT,
								new SignedWordElement(2705)),
						m(VictronBatteryInverter.ChannelId.MAX_FEED_IN_POWER,
								new SignedWordElement(2706),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(VictronBatteryInverter.ChannelId.FEED_EXCESS_DC,
								new SignedWordElement(2707)),
						m(VictronBatteryInverter.ChannelId.DONT_FEED_EXCESS_AC,
								new SignedWordElement(2708)),
						m(VictronBatteryInverter.ChannelId.PV_POWER_LIMITER_ACTIVE,
								new SignedWordElement(2709))
//						m(Victron.ChannelId.MAX_CHARGE_VOLTAGE, new UnsignedWordElement(2710),      //ILLEGAL DATA ADRESS
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
				)); //

	}

	/**
	 * Executes a Soft-Start. Sets the internal DC relay. Once this register is
	 * set to 1, the PCS will start the soft-start procedure, otherwise, the PCS
	 * will do nothing on the DC input Every time the PCS is powered off, this
	 * register will be cleared to 0. In some particular cases, the BMS wants to
	 * re-softstart, the EMS should actively clear this register to 0, after BMS
	 * soft-started, set it to 1 again.
	 *
	 * @param switchOn true to switch internal DC relay on
	 * @throws OpenemsNamedException on error
	 */
	public void softStart(boolean switchOn) throws OpenemsNamedException {
//		BooleanWriteChannel setSoftStart = this.channel(Victron.ChannelId.SET_SOFT_START);
//		setSoftStart.setNextWriteValue(switchOn ? true : false);
	}
}
