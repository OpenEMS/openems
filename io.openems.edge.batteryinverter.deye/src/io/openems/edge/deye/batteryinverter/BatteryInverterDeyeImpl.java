package io.openems.edge.deye.batteryinverter;

import java.util.Objects;
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
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.deye.batteryinverter.enums.EnableDisable;
import io.openems.edge.deye.batteryinverter.enums.FrequencyLevel;
import io.openems.edge.deye.batteryinverter.enums.GridCodeSelection;
import io.openems.edge.deye.batteryinverter.enums.PowerRisingMode;
import io.openems.edge.deye.batteryinverter.enums.VoltageLevel;
import io.openems.edge.deye.batteryinverter.statemachine.Context;
import io.openems.edge.deye.batteryinverter.statemachine.StateMachine;
import io.openems.edge.deye.batteryinverter.statemachine.StateMachine.State;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery-Inverter.Deye", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class BatteryInverterDeyeImpl extends AbstractOpenemsModbusComponent
		implements BatteryInverterDeye, OffGridBatteryInverter, ManagedSymmetricBatteryInverter,
		SymmetricBatteryInverter, ModbusComponent, OpenemsComponent, TimedataProvider, StartStoppable {

	public static final int DEFAULT_EMS_TIMEOUT = 60;
	public static final int DEFAULT_BMS_TIMEOUT = 0;
	public static final EnableDisable DEFAULT_GRID_EXISTENCE_DETECTION_ON = EnableDisable.DISABLE;
	public static final PowerRisingMode DEFAULT_POWER_RISING_MODE = PowerRisingMode.STEP;

	private static final int MAX_CURRENT = 90; // [A]
	private static final int MAX_TOPPING_CHARGE_VOLTAGE = 750;

	private final Logger log = LoggerFactory.getLogger(BatteryInverterDeyeImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private final CalculateEnergyFromPower calculateChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	protected Config config;

	public BatteryInverterDeyeImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				OffGridBatteryInverter.ChannelId.values(), //
				BatteryInverterDeye.ChannelId.values() //
		);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this,
				new FC3ReadRegistersTask(672, Priority.HIGH, m(BatteryInverterDeye.ChannelId.ACTIVE_POWER_1,
						new SignedWordElement(672)),m(BatteryInverterDeye.ChannelId.ACTIVE_POWER_2,
						new SignedWordElement(673))));
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {
		// Store the current State
		this.channel(BatteryInverterDeye.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Set Default Settings
		this.setDefaultSettings();

		// Set Battery Limits
		this.setBatteryLimits(battery);

		// Calculate the Energy values from ActivePower.
		this.calculateEnergy();

		// Prepare Context
		var context = new Context(this, this.config, this.targetGridMode.get(), setActivePower, setReactivePower);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(BatteryInverterDeye.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(BatteryInverterDeye.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	/**
	 * Updates the Channel if its current value is not equal to the new value.
	 *
	 * @param channelId Sinexcel Channel-Id
	 * @param value     {@link OptionsEnum} value.
	 * @throws IllegalArgumentException on error
	 */
	private void updateIfNotEqual(BatteryInverterDeye.ChannelId channelId, OptionsEnum value)
			throws IllegalArgumentException, OpenemsNamedException {
		this.updateIfNotEqual(channelId, value.getValue());
	}

	/**
	 * Updates the Channel if its current value is not equal to the new value.
	 *
	 * @param channelId Sinexcel Channel-Id
	 * @param newValue  Integer value.
	 * @throws IllegalArgumentException on error
	 */
	private void updateIfNotEqual(BatteryInverterDeye.ChannelId channelId, Integer newValue)
			throws IllegalArgumentException {
		WriteChannel<Integer> channel = this.channel(channelId);
		var currentValue = channel.value();
		if (currentValue.isDefined() && !Objects.equals(currentValue.get(), newValue)) {
			try {
				channel.setNextWriteValue(newValue);
			} catch (OpenemsNamedException e) {
				this.logWarn(this.log, "Unable to update Channel [" + channel.address() + "] from [" + currentValue
						+ "] to [" + newValue + "]");
				e.printStackTrace();
			}
		}
	}

	private void updateIfNotEqual(BatteryInverterDeye.ChannelId channelId, VoltageLevel voltageLevel)
			throws IllegalArgumentException, OpenemsNamedException {
		IntegerWriteChannel channel = this.channel(channelId);
		channel.setNextWriteValue(voltageLevel.getValue());
	}

	private void updateIfNotEqual(BatteryInverterDeye.ChannelId channelId, FrequencyLevel frequencyLevel)
			throws IllegalArgumentException, OpenemsNamedException {
		IntegerWriteChannel channel = this.channel(channelId);
		channel.setNextWriteValue(frequencyLevel.getValue());
	}

	private void updateIfNotEqual(BatteryInverterDeye.ChannelId channelId, GridCodeSelection gridCodeSelection)
			throws IllegalArgumentException, OpenemsNamedException {
		IntegerWriteChannel channel = this.channel(channelId);
		channel.setNextWriteValue(gridCodeSelection.getValue());
	}

	private void updateIfNotEqual(BatteryInverterDeye.ChannelId channelId, EnableDisable value)
			throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel channel = this.channel(channelId);
		switch (value) {
		case ENABLE:
			channel.setNextWriteValue(true);
			break;
		case DISABLE:
			channel.setNextWriteValue(false);
			break;
		}
	}

	/**
	 * Sets some default settings on the inverter, like Timeout.
	 *
	 * @throws OpenemsNamedException on error
	 */
	private void setDefaultSettings() throws OpenemsNamedException {
		this.updateIfNotEqual(BatteryInverterDeye.ChannelId.EMS_TIMEOUT, DEFAULT_EMS_TIMEOUT);
		this.updateIfNotEqual(BatteryInverterDeye.ChannelId.BMS_TIMEOUT, DEFAULT_BMS_TIMEOUT);
		this.updateIfNotEqual(BatteryInverterDeye.ChannelId.GRID_EXISTENCE_DETECTION_ON,
				DEFAULT_GRID_EXISTENCE_DETECTION_ON);
		this.updateIfNotEqual(BatteryInverterDeye.ChannelId.POWER_RISING_MODE, DEFAULT_POWER_RISING_MODE);

		switch (this.config.countryCode()) {
		case AUSTRIA:
		case GERMANY:
		case SWITZERLAND:
			this.updateIfNotEqual(BatteryInverterDeye.ChannelId.VOLTAGE_LEVEL, VoltageLevel.V_400);
			this.updateIfNotEqual(BatteryInverterDeye.ChannelId.FREQUENCY_LEVEL, FrequencyLevel.HZ_50);
			this.updateIfNotEqual(BatteryInverterDeye.ChannelId.GRID_CODE_SELECTION, GridCodeSelection.VDE);
			break;
		}

		this.updateIfNotEqual(BatteryInverterDeye.ChannelId.INVERTER_WIRING_TOPOLOGY, this.config.emergencyPower());
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
		this.updateIfNotEqual(BatteryInverterDeye.ChannelId.DISCHARGE_MIN_VOLTAGE,
				battery.getDischargeMinVoltage().get());

		// Charge Max Voltage
		this.updateIfNotEqual(BatteryInverterDeye.ChannelId.CHARGE_MAX_VOLTAGE, battery.getChargeMaxVoltage().get());

		// Topping Charge Voltage
		this.updateIfNotEqual(BatteryInverterDeye.ChannelId.TOPPING_CHARGE_VOLTAGE,
				TypeUtils.min(battery.getChargeMaxVoltage().get(), MAX_TOPPING_CHARGE_VOLTAGE));

		// Float Charge Voltage
		this.updateIfNotEqual(BatteryInverterDeye.ChannelId.FLOAT_CHARGE_VOLTAGE,
				TypeUtils.min(battery.getChargeMaxVoltage().get(), MAX_TOPPING_CHARGE_VOLTAGE));

		// Discharge Max Current
		// negative value is corrected as zero
		this.updateIfNotEqual(BatteryInverterDeye.ChannelId.DISCHARGE_MAX_CURRENT,
				TypeUtils.fitWithin(0 /* enforce positive */, MAX_CURRENT, battery.getDischargeMaxCurrent().orElse(0)));

		// Charge Max Current
		// negative value is corrected as zero
		this.updateIfNotEqual(BatteryInverterDeye.ChannelId.CHARGE_MAX_CURRENT,
				TypeUtils.fitWithin(0 /* enforce positive */, MAX_CURRENT, battery.getChargeMaxCurrent().orElse(0)));
	}

	@Override
	public String debugLog() {
		return "MOIN - L:" + this.getActivePower().asString();
	}

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	/**
	 * Gets the inverter start-stop target.
	 * 
	 * @return {@link StartStop}
	 */
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

		}
		// Block any power as long as we are not RUNNING
		return new BatteryInverterConstraint[] { //
				new BatteryInverterConstraint("Sinexcel inverter not ready", Phase.ALL, Pwr.REACTIVE, //
						Relationship.EQUALS, 0d), //
				new BatteryInverterConstraint("Sinexcel inverter not ready", Phase.ALL, Pwr.ACTIVE, //
						Relationship.EQUALS, 0d) //
		};
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

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		var activePower1 = this.getActive1PowerChannel().value().get();
		var activePower2 = this.getActive2PowerChannel().value().get();
		var activePower = activePower1 + activePower2;
		if (activePower1 == null && activePower2 == null) {
			// Not available
			this.calculateChargeEnergy.update(null);
			this.calculateDischargeEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Grid
			this.calculateChargeEnergy.update(0);
			this.calculateDischargeEnergy.update(activePower);
		} else {
			// Sell-To-Grid
			this.calculateChargeEnergy.update(activePower * -1);
			this.calculateDischargeEnergy.update(0);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
