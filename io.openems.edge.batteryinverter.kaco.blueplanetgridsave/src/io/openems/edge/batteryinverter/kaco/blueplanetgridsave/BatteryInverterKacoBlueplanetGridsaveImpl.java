package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static io.openems.edge.common.sum.GridMode.ON_GRID;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201ControlMode;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201CurrentState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201StVnd;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64202.S64202EnLimit;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.Context;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.batteryinverter.sunspec.AbstractSunSpecBatteryInverter;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery-Inverter.Kaco.BlueplanetGridsave", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BatteryInverterKacoBlueplanetGridsaveImpl extends AbstractSunSpecBatteryInverter
		implements BatteryInverterKacoBlueplanetGridsave, ManagedSymmetricBatteryInverter, SymmetricBatteryInverter,
		ModbusComponent, ModbusSlave, OpenemsComponent, TimedataProvider, StartStoppable {

	private static final int UNIT_ID = 1;
	private static final int READ_FROM_MODBUS_BLOCK = 1;
	private static final int DC_MIN_VOLTAGE_LIMIT = 650;
	private static final int DC_MAX_VOLTAGE_LIMIT = 1315;

	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);
	private final Logger log = LoggerFactory.getLogger(BatteryInverterKacoBlueplanetGridsaveImpl.class);
	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	private final CalculateEnergyFromPower calculateChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	/**
	 * Kaco 92 does not have model 64203.
	 */
	private boolean hasSunSpecModel64203 = false;

	/**
	 * Active SunSpec models for KACO blueplanet gridsave. Commented models are
	 * available but not used currently.
	 */
	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.HIGH) //
			.put(DefaultSunSpecModel.S_121, Priority.LOW) //
			.put(KacoSunSpecModel.S_64201, Priority.HIGH) //
			.put(KacoSunSpecModel.S_64202, Priority.LOW) //
			.put(KacoSunSpecModel.S_64203, Priority.LOW) //
			.put(KacoSunSpecModel.S_64204, Priority.LOW) //
			.build();

	// Further available SunSpec blocks provided by KACO blueplanet are:
	// .put(SunSpecModel.S_113, Priority.LOW) //
	// .put(SunSpecModel.S_120, Priority.LOW) //
	// .put(SunSpecModel.S_122, Priority.LOW) //
	// .put(SunSpecModel.S_123, Priority.LOW) //
	// .put(SunSpecModel.S_126, Priority.LOW) //
	// .put(SunSpecModel.S_129, Priority.LOW) //
	// .put(SunSpecModel.S_130, Priority.LOW) //
	// .put(SunSpecModel.S_132, Priority.LOW) //
	// .put(SunSpecModel.S_135, Priority.LOW) //
	// .put(SunSpecModel.S_136, Priority.LOW) //
	// .put(SunSpecModel.S_160, Priority.LOW) //

	public BatteryInverterKacoBlueplanetGridsaveImpl() {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryInverterKacoBlueplanetGridsave.ChannelId.values() //
		);
		this._setGridMode(ON_GRID);
		this._setDcMinVoltage(DC_MIN_VOLTAGE_LIMIT);
		this._setDcMaxVoltage(DC_MAX_VOLTAGE_LIMIT);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
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
		this.channel(BatteryInverterKacoBlueplanetGridsave.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Stop early if initialization is not finished
		if (!this.isSunSpecInitializationCompleted()) {
			return;
		}

		/*
		 * The WparamRmpTms parameter constrains performance changes using a PT1
		 * behavior. By default, a 1 second (1000 ms) duration is stored here. This
		 * duration can be reduced to 0.1 second (100 ms) for quicker control behavior.
		 * While a complete reduction to 0 is technically possible, it may result in
		 * overcurrent or overvoltage events, especially in situations involving high
		 * power changes and multiple devices. This feature is beneficial for FFR use
		 * cases and aids in preventing battery derating.
		 */
		setWriteValueIfNotRead(this.getSunSpecChannelOrError(KacoSunSpecModel.S64201.WPARAM_RMP_TMS), 100);

		// Set Display Information
		this.setDisplayInformation(battery);

		// Set Battery Limits
		this.setBatteryLimits(battery);

		// Set if there is grid disconnection failure
		this.handleGridDisconnection();

		// Calculate the Energy values from ActivePower.
		this.calculateEnergy();

		// Enable reactive power by default
		this.enableReactivePower();

		if (this.config.activateWatchdog()) {
			// Trigger the Watchdog
			this.triggerWatchdog();
		}

		// Prepare Context
		var context = new Context(this, //
				battery, //
				setActivePower, //
				setReactivePower, //
				this.componentManager.getClock());

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
	 * Enable the reactive power by default.
	 */
	private void enableReactivePower() {
		try {
			EnumWriteChannel channel = this.getSunSpecChannelOrError(KacoSunSpecModel.S64201.CONTROL_MODE);
			setWriteValueIfNotRead(channel, S64201ControlMode.SUNSPEC_CTRL_MODE_QFIX);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	private record HandleFaultChannels(//
			SunSpecPoint model, //
			OptionsEnum stateEnum, //
			Consumer<Boolean> method//
	) {

	}

	private void handleGridDisconnection() {
		Stream.of(
				new HandleFaultChannels(KacoSunSpecModel.S64201.ST_VND, S64201StVnd.POWADORPROTECT_DISCONNECTION,
						this::_setGridDisconnection), //
				new HandleFaultChannels(KacoSunSpecModel.S64201.ST_VND, S64201StVnd.GRID_FAILURE_PHASETOPHASE,
						this::_setGridFailureLineToLine), //
				new HandleFaultChannels(KacoSunSpecModel.S64201.ST_VND, S64201StVnd.LINE_FAILURE_UNDERFREQ,
						this::_setLineFailureUnderFreq), //
				new HandleFaultChannels(KacoSunSpecModel.S64201.ST_VND, S64201StVnd.LINE_FAILURE_OVERFREQ,
						this::_setLineFailureOverFreq), //
				new HandleFaultChannels(KacoSunSpecModel.S64201.ST_VND, S64201StVnd.PROTECTION_SHUTDOWN_LINE_1,
						this::_setProtectionShutdownLine1), //
				new HandleFaultChannels(KacoSunSpecModel.S64201.ST_VND, S64201StVnd.PROTECTION_SHUTDOWN_LINE_2,
						this::_setProtectionShutdownLine2), //
				new HandleFaultChannels(KacoSunSpecModel.S64201.ST_VND, S64201StVnd.PROTECTION_SHUTDOWN_LINE_3,
						this::_setProtectionShutdownLine3))//
				.forEach(t -> {
					try {
						var channel = this.getSunSpecChannelOrError(t.model);
						t.method.accept(Objects.equal(channel.value().get(), t.stateEnum));
					} catch (OpenemsException e) {
						this.logWarn(this.log, e.getMessage());
					}
				});
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(BatteryInverterKacoBlueplanetGridsave.class, accessMode, 100) //
						.build());
	}

	@Override
	public BatteryInverterConstraint[] getStaticConstraints() throws OpenemsException {
		if (this.stateMachine.getCurrentState() == State.RUNNING) {
			return BatteryInverterConstraint.NO_CONSTRAINTS;

		}
		// Block any power as long as we are not RUNNING
		return new BatteryInverterConstraint[] { //
				new BatteryInverterConstraint("KACO inverter not ready", Phase.ALL, Pwr.REACTIVE, //
						Relationship.EQUALS, 0d), //
				new BatteryInverterConstraint("KACO inverter not ready", Phase.ALL, Pwr.ACTIVE, //
						Relationship.EQUALS, 0d) //
		};
	}

	/**
	 * Sets the Battery Limits.
	 *
	 * @param battery the linked {@link Battery}
	 * @throws OpenemsNamedException on error
	 */
	private void setBatteryLimits(Battery battery) throws OpenemsNamedException {
		// Discharge Min Voltage
		FloatWriteChannel disMinVChannel = this.getSunSpecChannelOrError(KacoSunSpecModel.S64202.DIS_MIN_V_0);
		var dischargeMinVoltage = battery.getDischargeMinVoltage().get();
		if (Objects.equal(dischargeMinVoltage, 0)) {
			dischargeMinVoltage = null; // according to setup manual DIS_MIN_V must not be zero
		}
		disMinVChannel.setNextWriteValueFromObject(dischargeMinVoltage);

		// Charge Max Voltage
		FloatWriteChannel chaMaxVChannel = this.getSunSpecChannelOrError(KacoSunSpecModel.S64202.CHA_MAX_V_0);
		var chargeMaxVoltage = battery.getChargeMaxVoltage().get();
		if (Objects.equal(chargeMaxVoltage, 0)) {
			chargeMaxVoltage = null; // according to setup manual CHA_MAX_V must not be zero
		}
		chaMaxVChannel.setNextWriteValueFromObject(chargeMaxVoltage);

		// Discharge Max Current
		// negative value is corrected as zero
		FloatWriteChannel disMaxAChannel = this.getSunSpecChannelOrError(KacoSunSpecModel.S64202.DIS_MAX_A_0);
		disMaxAChannel.setNextWriteValue(Math.max(0F, battery.getDischargeMaxCurrent().orElse(0)));

		// Charge Max Current
		// negative value is corrected as zero
		FloatWriteChannel chaMaxAChannel = this.getSunSpecChannelOrError(KacoSunSpecModel.S64202.CHA_MAX_A_0);
		chaMaxAChannel.setNextWriteValue(Math.max(0F, battery.getChargeMaxCurrent().orElse(0)));

		// Activate Battery values
		EnumWriteChannel enLimitChannel = this.getSunSpecChannelOrError(KacoSunSpecModel.S64202.EN_LIMIT_0);
		enLimitChannel.setNextWriteValue(S64202EnLimit.ACTIVATE);
	}

	/**
	 * Sets the information that is shown on the Display, like State-of-Charge,
	 * State-of-Health and Max-Cell-Temperature.
	 *
	 * @param battery the linked {@link Battery}
	 * @throws OpenemsNamedException on error
	 */
	private void setDisplayInformation(Battery battery) throws OpenemsNamedException {
		if (this.hasSunSpecModel64203) {
			// State-of-Charge
			FloatWriteChannel batSocChannel = this.getSunSpecChannelOrError(KacoSunSpecModel.S64203.BAT_SOC_0);
			batSocChannel.setNextWriteValueFromObject(battery.getSoc().get());

			// State-of-Health
			FloatWriteChannel batSohChannel = this.getSunSpecChannelOrError(KacoSunSpecModel.S64203.BAT_SOH_0);
			batSohChannel.setNextWriteValueFromObject(battery.getSoh().get());

			// Max-Cell-Temperature
			FloatWriteChannel batTempChannel = this.getSunSpecChannelOrError(KacoSunSpecModel.S64203.BAT_TEMP_0);
			batTempChannel.setNextWriteValueFromObject(battery.getMaxCellTemperature().get());
		}
	}

	private Instant lastTriggerWatchdog = Instant.MIN;

	/**
	 * Triggers the Watchdog after WATCHDOG_TRIGGER passed.
	 *
	 * @throws OpenemsNamedException on error
	 */
	private void triggerWatchdog() throws OpenemsNamedException {
		var now = Instant.now(this.componentManager.getClock());
		if (Duration.between(this.lastTriggerWatchdog, now)
				.getSeconds() >= BatteryInverterKacoBlueplanetGridsave.WATCHDOG_TRIGGER_SECONDS) {
			IntegerWriteChannel watchdogChannel = this.getSunSpecChannelOrError(KacoSunSpecModel.S64201.WATCHDOG);
			watchdogChannel.setNextWriteValue(BatteryInverterKacoBlueplanetGridsave.WATCHDOG_TIMEOUT_SECONDS);
			this.lastTriggerWatchdog = now;
		}
	}

	/**
	 * Mark SunSpec initialization completed; this takes some time at startup.
	 */
	@Override
	protected void onSunSpecInitializationCompleted() {
		this.addCopyListener(//
				this.getSunSpecChannel(DefaultSunSpecModel.S121.W_MAX).get(), //
				SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER //
		);
		this.addCopyListener(//
				this.getSunSpecChannel(KacoSunSpecModel.S64201.W).get(), //
				SymmetricBatteryInverter.ChannelId.ACTIVE_POWER //
		);
		this.addCopyListener(//
				this.getSunSpecChannel(KacoSunSpecModel.S64201.V_AR).get(), //
				SymmetricBatteryInverter.ChannelId.REACTIVE_POWER //
		);
	}

	@Override
	public S64201CurrentState getCurrentState() {
		Optional<EnumReadChannel> channel = this.getSunSpecChannel(KacoSunSpecModel.S64201.CURRENT_STATE);
		if (channel.isPresent()) {
			return channel.get().value().asEnum();
		}
		return S64201CurrentState.UNDEFINED;
	}

	@Override
	protected SunSpecModel getSunSpecModel(int blockId) throws IllegalArgumentException {
		return KacoSunSpecModel.valueOf("S_" + blockId);
	}

	/**
	 * Calculate the Power-Precision from the Max Apparent Power using the SetPoint
	 * scale-factor.
	 */
	@Override
	public int getPowerPrecision() {
		Optional<IntegerReadChannel> scalefactorChannel = this.getSunSpecChannel(KacoSunSpecModel.S64201.W_SET_PCT_SF);
		if (!scalefactorChannel.isPresent()) {
			return 1;
		}
		var scalefactor = scalefactorChannel.get().value();
		var maxApparentPower = this.getMaxApparentPower();
		if (!scalefactor.isDefined() || !maxApparentPower.isDefined()) {
			return 1;
		}
		// Take one percent (0.01) of MaxApparentPower and then apply scalefactor
		return (int) (maxApparentPower.get() * 0.01 * Math.pow(10, scalefactor.get()));
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append(this.stateMachine.debugLog()) //
				.append("|State:").append(this.getCurrentState().asCamelCase()) //
				.toString();
	}

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
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

	/**
	 * Adds a Copy-Listener. It listens on setNextValue() and copies the value to
	 * the target channel.
	 *
	 * @param <T>             the Channel type
	 * @param sourceChannel   the source Channel
	 * @param targetChannelId the target ChannelId
	 */
	private <T> void addCopyListener(Channel<T> sourceChannel,
			io.openems.edge.common.channel.ChannelId targetChannelId) {
		Consumer<Value<T>> callback = value -> {
			Channel<T> targetChannel = this.channel(targetChannelId);
			targetChannel.setNextValue(value);
		};
		sourceChannel.onSetNextValue(callback);
		callback.accept(sourceChannel.getNextValue());
	}

	@Override
	public <T extends Channel<?>> T getSunSpecChannelOrError(SunSpecPoint point) throws OpenemsException {
		return super.getSunSpecChannelOrError(point);
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		var activePower = this.getActivePower().get();
		if (activePower == null) {
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

	@Override
	protected void addBlock(int startAddress, SunSpecModel model, Priority priority) {
		super.addBlock(startAddress, model, priority);

		// Mark S_64203 as available
		if (model.equals(KacoSunSpecModel.S_64203)) {
			this.hasSunSpecModel64203 = true;
		}
	}

	/**
	 * Checks if the system is in a running state. This method retrieves the
	 * system's global state and determines whether the system is in a running
	 * state.
	 *
	 * @return true if the system is in a running state, false otherwise.
	 */
	public boolean isRunning() {
		return this.getCurrentState() == S64201CurrentState.GRID_CONNECTED//
				|| this.getCurrentState() == S64201CurrentState.THROTTLED;
	}

	/**
	 * Checks if the system is in a stop state. This method retrieves the system's
	 * global state and determines whether the system is in a stop state.
	 *
	 * @return true if the system is in a stop state, false otherwise.
	 */
	public boolean isShutdown() {
		return this.getCurrentState() == S64201CurrentState.OFF //
				|| this.getCurrentState() == S64201CurrentState.STANDBY //
				|| this.getCurrentState() == S64201CurrentState.PRECHARGE//
				|| this.getCurrentState() == S64201CurrentState.SHUTTING_DOWN;
	}

	/**
	 * Checks if the system is in a fault state. This method retrieves the system's
	 * global state and determines whether the system is in a fault state.
	 *
	 * @return true if the system is in a fault state, false otherwise.
	 */
	public boolean hasFailure() {
		return this.hasFaults() || this.getCurrentState() == S64201CurrentState.FAULT;
	}

}
