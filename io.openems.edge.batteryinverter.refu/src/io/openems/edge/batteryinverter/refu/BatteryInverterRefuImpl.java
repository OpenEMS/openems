package io.openems.edge.batteryinverter.refu;

import static io.openems.edge.common.sum.GridMode.ON_GRID;
import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Pwr.REACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.refu.RefuSunSpecModel.S64800.S64800PcsSetOperation;
import io.openems.edge.batteryinverter.refu.statemachine.Context;
import io.openems.edge.batteryinverter.refu.statemachine.StateMachine;
import io.openems.edge.batteryinverter.refu.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.bridge.modbus.sunspec.batteryinverter.AbstractSunSpecBatteryInverter;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery-Inverter.Refu", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class BatteryInverterRefuImpl extends AbstractSunSpecBatteryInverter
		implements BatteryInverterRefu, ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, ModbusComponent,
		OpenemsComponent, TimedataProvider, StartStoppable, ModbusSlave {

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	/**
	 * Max battery current limit (UINT16 with scale 0.01 to max 655 A).
	 */
	private static final float MAX_BAT_A_LIMIT = 655F; // [A]

	/**
	 * Minimum DC voltage [V].
	 *
	 * <p>
	 * From REFU specification: 1.46 * 180 V (minimum AC voltage) + 50 V = 313 V.
	 */
	private static final int DC_MIN_VOLTAGE = 313; // [V]

	/**
	 * Maximum DC voltage [V].
	 */
	private static final int DC_MAX_VOLTAGE = 1000; // [V]

	/**
	 * Factor for DC minimum voltage calculation: U_dc,min = factor * U_ac(LL) +
	 * offset.
	 */
	private static final double DC_MIN_VOLTAGE_FACTOR = 1.46;

	/**
	 * Offset [V] added to the scaled AC line voltage to derive DC minimum voltage.
	 */
	private static final int DC_MIN_VOLTAGE_OFFSET = 50; // [V]

	private final Logger log = LoggerFactory.getLogger(BatteryInverterRefuImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);
	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	private final CalculateEnergyFromPower calculateChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	/**
	 * Active SunSpec models for REFU inverters.
	 */
	private static final List<SunSpecModelEntry> ACTIVE_MODELS = List.of(
			SunSpecModelEntry.create(DefaultSunSpecModel.S_1).build(), //
			SunSpecModelEntry.create(DefaultSunSpecModel.S_103) //
					.setPriority(Priority.HIGH) //
					.setRequired(true) //
					.build(), //
			SunSpecModelEntry.create(DefaultSunSpecModel.S_120).build(), //
			SunSpecModelEntry.create(DefaultSunSpecModel.S_121).build(), //
			SunSpecModelEntry.create(RefuSunSpecModel.S_123) //
					.setPriority(Priority.HIGH) //
					.setRequired(true) //
					.build(), //
			SunSpecModelEntry.create(RefuSunSpecModel.S_64800) //
					.setPriority(Priority.HIGH) //
					.setRequired(true) //
					.build());

	public BatteryInverterRefuImpl() {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryInverterRefu.ChannelId.values() //
		);
		this._setGridMode(ON_GRID);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(),
				READ_FROM_MODBUS_BLOCK);

		this._setDcMinVoltage(DC_MIN_VOLTAGE);
		this._setDcMaxVoltage(DC_MAX_VOLTAGE);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {
		this.channel(BatteryInverterRefu.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		this._setStartStop(StartStop.UNDEFINED);

		if (!(this.isSunSpecInitializationCompleted() && this.areRequiredModelsRead())) {
			return;
		}

		this.setBatteryLimits(battery);
		this.calculateEnergy();

		if (this.config.activateWatchdog()) {
			this.triggerWatchdog();
		}

		var context = new Context(this, battery, setActivePower, setReactivePower, this.componentManager.getClock());

		try {
			this.stateMachine.run(context);
			this.channel(BatteryInverterRefu.ChannelId.RUN_FAILED).setNextValue(false);
		} catch (OpenemsNamedException e) {
			this.channel(BatteryInverterRefu.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public BatteryInverterConstraint[] getStaticConstraints() throws OpenemsException {
		if (this.stateMachine.getCurrentState() == State.RUNNING) {
			return BatteryInverterConstraint.NO_CONSTRAINTS;
		}
		return new BatteryInverterConstraint[] { //
				new BatteryInverterConstraint("REFU inverter not ready", ALL, REACTIVE, EQUALS, 0), //
				new BatteryInverterConstraint("REFU inverter not ready", ALL, ACTIVE, EQUALS, 0) //
		};
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(BatteryInverterRefu.class, accessMode, 100) //
						.channel(0, BatteryInverterRefu.ChannelId.STATE_MACHINE, ModbusType.UINT16) //
						.build());
	}

	@Override
	public int getPowerPrecision() {
		var maxApparentPower = this.getMaxApparentPower();
		if (!maxApparentPower.isDefined()) {
			return 1;
		}
		// 1% resolution of WMax
		return maxApparentPower.get() / 100;
	}

	@Override
	protected void onSunSpecInitializationCompleted() {
		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

		this.addCopyListener(//
				this.getSunSpecChannel(DefaultSunSpecModel.S121.W_MAX).get(), //
				SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER //
		);
		this.addCopyListener(//
				this.getSunSpecChannel(DefaultSunSpecModel.S103.W).get(), //
				SymmetricBatteryInverter.ChannelId.ACTIVE_POWER //
		);
		this.addCopyListener(//
				this.getSunSpecChannel(DefaultSunSpecModel.S103.V_AR).get(), //
				SymmetricBatteryInverter.ChannelId.REACTIVE_POWER //
		);
	}

	@Override
	protected SunSpecModel getSunSpecModel(int blockId) throws IllegalArgumentException {
		return switch (blockId) {
		case 123 -> RefuSunSpecModel.S_123;
		case 64800 -> RefuSunSpecModel.S_64800;
		default -> null;
		};
	}

	@Override
	public <T extends Channel<?>> T getSunSpecChannelOrError(SunSpecPoint point) throws OpenemsException {
		return super.getSunSpecChannelOrError(point);
	}

	/**
	 * Sets the battery charge/discharge current limits from the BMS.
	 *
	 * @param battery the linked {@link Battery}
	 * @throws OpenemsNamedException on error
	 */

	private void setBatteryLimits(Battery battery) throws OpenemsNamedException {
		FloatWriteChannel maxBatAChaChannel = this.getSunSpecChannelOrError(RefuSunSpecModel.S64800.MAX_BAT_A_CHA);
		maxBatAChaChannel.setNextWriteValue(
				TypeUtils.fitWithin(0F, MAX_BAT_A_LIMIT, battery.getChargeMaxCurrent().orElse(0).floatValue()));

		FloatWriteChannel maxBatADischaChannel = this
				.getSunSpecChannelOrError(RefuSunSpecModel.S64800.MAX_BAT_A_DISCHA);
		maxBatADischaChannel.setNextWriteValue(
				TypeUtils.fitWithin(0F, MAX_BAT_A_LIMIT, battery.getDischargeMaxCurrent().orElse(0).floatValue()));

		this.updateDcVoltageLimits();
	}

	/**
	 * Updates DC voltage limits for EP regulation.
	 *
	 * <p>
	 * DC_MIN is calculated from the minimum AC line-to-line voltage using: U_dc,min
	 * = 1.46 * U_ac(LL) + 50 V.
	 * </p>
	 *
	 * <p>
	 * DC_MAX is set to 1000 V (hardware limit).
	 * </p>
	 */
	private void updateDcVoltageLimits() {
		Optional<Channel<Float>> ppAbOpt = this.getSunSpecChannel(DefaultSunSpecModel.S103.P_P_VPH_A_B);
		Optional<Channel<Float>> ppBcOpt = this.getSunSpecChannel(DefaultSunSpecModel.S103.P_P_VPH_B_C);
		Optional<Channel<Float>> ppCaOpt = this.getSunSpecChannel(DefaultSunSpecModel.S103.P_P_VPH_C_A);

		var ppAB = ppAbOpt.map(ch -> ch.value().get()).orElse(null);
		var ppBC = ppBcOpt.map(ch -> ch.value().get()).orElse(null);
		var ppCA = ppCaOpt.map(ch -> ch.value().get()).orElse(null);

		if (ppAB != null && ppBC != null && ppCA != null) {
			double uAcLL = Math.min((double) ppAB, Math.min((double) ppBC, (double) ppCA));
			this._setDcMinVoltage((int) Math.ceil(DC_MIN_VOLTAGE_FACTOR * uAcLL + DC_MIN_VOLTAGE_OFFSET));
		}
		this._setDcMaxVoltage(DC_MAX_VOLTAGE);
	}

	/**
	 * Writes the current clock second (0 to 59) to CONTROLLER_HB each cycle.
	 *
	 * @throws OpenemsNamedException on error
	 */
	private void triggerWatchdog() throws OpenemsNamedException {
		IntegerWriteChannel controllerHbChannel = this.getSunSpecChannelOrError(RefuSunSpecModel.S64800.CONTROLLER_HB);
		controllerHbChannel.setNextWriteValue(LocalDateTime.now(this.componentManager.getClock()).getSecond());
	}

	/**
	 * Writes PCS_SET_OPERATION to model 64800.
	 *
	 * @param operation the desired {@link S64800PcsSetOperation}
	 * @throws OpenemsNamedException on error
	 */
	public void setPcsSetOperation(S64800PcsSetOperation operation) throws OpenemsNamedException {
		EnumWriteChannel pcsSetOperationChannel = this
				.getSunSpecChannelOrError(RefuSunSpecModel.S64800.PCS_SET_OPERATION);
		pcsSetOperationChannel.setNextWriteValue(operation);
	}

	/**
	 * Gets the PCS_SET_OPERATION channel as {@link EnumWriteChannel}.
	 * 
	 * @return Optional {@link EnumWriteChannel}
	 */
	public S64800PcsSetOperation getPcsSetOperation() {
		Optional<EnumWriteChannel> channel = this.getSunSpecChannel(RefuSunSpecModel.S64800.PCS_SET_OPERATION);
		if (channel.isEmpty()) {
			return S64800PcsSetOperation.UNDEFINED;
		}
		return channel.get().value().asEnum();
	}

	/**
	 * Pre-sets WMaxLimPct=0 and WMaxLim_Ena=ENABLED as required by the REFU startup
	 * sequence (doc step 8) before sending EXIT_STANDBY_MODE.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public void presetZeroPower() throws OpenemsNamedException {
		FloatWriteChannel wMaxLimPctChannel = this.getSunSpecChannelOrError(RefuSunSpecModel.S123.W_MAX_LIM_PCT);
		wMaxLimPctChannel.setNextWriteValue(0F);

		EnumWriteChannel wMaxLimEnaChannel = this.getSunSpecChannelOrError(RefuSunSpecModel.S123.W_MAX_LIM_ENA);
		wMaxLimEnaChannel.setNextWriteValue(RefuSunSpecModel.S123.S123Ena.ENABLED);
	}

	/**
	 * Gets the current operating state from SunSpec model 103 ST register.
	 *
	 * @return the {@link DefaultSunSpecModel.S103_St}
	 */
	public DefaultSunSpecModel.S103_St getOperatingState() {
		Optional<EnumReadChannel> channel = this.getSunSpecChannel(DefaultSunSpecModel.S103.ST);
		if (channel.isEmpty()) {
			return DefaultSunSpecModel.S103_St.UNDEFINED;
		}
		return channel.get().value().asEnum();
	}

	/**
	 * Checks if the inverter is in a running state (MPPT, THROTTLED, or STARTED).
	 *
	 * @return true if running
	 */
	public boolean isRunning() {
		return switch (this.getOperatingState()) {
		case MPPT, THROTTLED, STARTED -> true;
		default -> false;
		};
	}

	/**
	 * Checks if the inverter is in standby.
	 *
	 * @return true if in standby
	 */
	public boolean isInStandby() {
		return this.getOperatingState() == DefaultSunSpecModel.S103_St.STANDBY;
	}

	/**
	 * Checks if the inverter is shut down (OFF or sleeping).
	 *
	 * @return true if shut down
	 */
	public boolean isShutdown() {
		return switch (this.getOperatingState()) {
		case OFF, SLEEPING, SHUTTING_DOWN -> true;
		default -> false;
		};
	}

	/**
	 * Checks if the inverter is in a hard fault state.
	 *
	 * @return true if there is a fault
	 */
	public boolean hasFailure() {
		return this.hasFaults() || this.getOperatingState() == DefaultSunSpecModel.S103_St.FAULT;
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

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append(this.stateMachine.debugLog()) //
				.append("|State:").append(this.getOperatingState().getName()) //
				.toString();
	}

	private <T> void addCopyListener(Channel<T> sourceChannel,
			io.openems.edge.common.channel.ChannelId targetChannelId) {
		Consumer<Value<T>> callback = value -> {
			Channel<T> targetChannel = this.channel(targetChannelId);
			targetChannel.setNextValue(value);
		};
		sourceChannel.onSetNextValue(callback);
		callback.accept(sourceChannel.getNextValue());
	}

	private void calculateEnergy() {
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateChargeEnergy.update(null);
			this.calculateDischargeEnergy.update(null);
		} else if (activePower > 0) {
			this.calculateChargeEnergy.update(0);
			this.calculateDischargeEnergy.update(activePower);
		} else {
			this.calculateChargeEnergy.update(-activePower);
			this.calculateDischargeEnergy.update(0);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}