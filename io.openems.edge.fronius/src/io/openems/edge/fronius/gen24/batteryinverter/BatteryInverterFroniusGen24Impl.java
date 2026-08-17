package io.openems.edge.fronius.gen24.batteryinverter;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.batteryinverter.AbstractSunSpecBatteryInverter;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.fronius.gen24.battery.FroniusGen24Battery;
import io.openems.edge.fronius.gen24.dccharger.FroniusGen24DcCharger;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Ess.Fronius.Gen24.Inverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE) //
@GenerateTargetsFromReferences("Modbus")
public class BatteryInverterFroniusGen24Impl extends AbstractSunSpecBatteryInverter
		implements BatteryInverterFroniusGen24, HybridManagedSymmetricBatteryInverter, ManagedSymmetricBatteryInverter,
		SymmetricBatteryInverter, StartStoppable, ModbusComponent, TimedataProvider, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BatteryInverterFroniusGen24Impl.class);

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder() //
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.HIGH) //
			.put(DefaultSunSpecModel.S_120, Priority.LOW) //
			.put(DefaultSunSpecModel.S_121, Priority.LOW) //
			.put(DefaultSunSpecModel.S_122, Priority.LOW) //
			.put(DefaultSunSpecModel.S_123, Priority.LOW) //
			.put(DefaultSunSpecModel.S_124, Priority.LOW) //
			.put(S160SunSpecModel.S_160, Priority.HIGH) //
			.build();

	@Reference
	protected Power power;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateActiveChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateActiveDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this,
			HybridManagedSymmetricBatteryInverter.ChannelId.DC_CHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
			HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_ENERGY);

	private final ApplyPowerHandler applyPowerHandler = new ApplyPowerHandler(this);

	/**
	 * Registered {@link FroniusGen24DcCharger}s, bound dynamically via
	 * {@link #addCharger}/{@link #removeCharger} (OSGi dynamic multiple Reference).
	 * Chargers hold no reference back to this BatteryInverter - matches the pattern
	 * used by GoodWe ({@code AbstractGoodWe.chargers}) and FENECON Commercial40
	 * ({@code EssFeneconCommercial40Impl.chargers}).
	 */
	private final List<FroniusGen24DcCharger> chargers = new CopyOnWriteArrayList<>();

	private Config config;

	public BatteryInverterFroniusGen24Impl() throws OpenemsException {

		super(ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				HybridManagedSymmetricBatteryInverter.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				new io.openems.edge.common.channel.ChannelId[] {
						ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT },
				BatteryInverterFroniusGen24.ChannelId.values());
	}

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))" //
	)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {

		this.config = config;

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(),
				READ_FROM_MODBUS_BLOCK);

		this._setGridMode(GridMode.ON_GRID);
		this._setConfiguredControlMode(config.controlMode());
		this._setInitializing(true);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void onSunSpecInitializationCompleted() {

		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

		this.mapFirstPointToChannel(SymmetricBatteryInverter.ChannelId.ACTIVE_POWER,
				ElementToChannelConverter.DIRECT_1_TO_1, DefaultSunSpecModel.S103.W);

		this.mapFirstPointToChannel(SymmetricBatteryInverter.ChannelId.REACTIVE_POWER,
				ElementToChannelConverter.DIRECT_1_TO_1, DefaultSunSpecModel.S103.V_AR);

		this.mapFirstPointToChannel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER,
				ElementToChannelConverter.DIRECT_1_TO_1, DefaultSunSpecModel.S121.W_MAX);

		this.mapFirstPointToChannel(BatteryInverterFroniusGen24.ChannelId.OPERATING_STATE,
				enumConverter(DefaultSunSpecModel.S103_St.values()), DefaultSunSpecModel.S103.ST);

		this.installListeners();

		this._setInitializing(false);
	}

	/**
	 * Builds a converter that resolves a raw Modbus/SunSpec integer value to the
	 * matching {@link io.openems.common.types.OptionsEnum} constant.
	 *
	 * <p>
	 * {@link ElementToChannelConverter#DIRECT_1_TO_1} is not sufficient here: it
	 * passes the raw value through unchanged, so the target Channel would end up
	 * holding a plain {@code Integer} instead of the declared enum constant -
	 * causing a {@link ClassCastException} the moment anything reads the Channel as
	 * its declared enum type.
	 *
	 * @param values the target enum's {@code values()}
	 * @return the converter
	 */
	private static ElementToChannelConverter enumConverter(io.openems.common.types.OptionsEnum[] values) {
		return new ElementToChannelConverter(value -> {
			if (value == null) {
				return null;
			}
			int intValue = ((Number) value).intValue();
			for (var option : values) {
				if (option.getValue() == intValue) {
					return option;
				}
			}
			return values.length > 0 ? values[0].getUndefined() : null;
		});
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {

		this.calculateEnergy();

		if (!(battery instanceof FroniusGen24Battery froniusBattery)) {

			this.log.warn("Unsupported battery type: {}", battery.getClass().getSimpleName());

			return;
		}

		this.applyPowerHandler.apply(froniusBattery, setActivePower, setReactivePower, this.config.controlMode());
	}

	private void recalculateDcDischargePower() {

		try {

			int chargePower = this.getModule3DcwChannel().getNextValue().orElse(0F).intValue();

			int dischargePower = this.getModule4DcwChannel().getNextValue().orElse(0F).intValue();

			int batteryPower = dischargePower - chargePower;

			this._setDcDischargePower(batteryPower);

			if (batteryPower > 0) {

				this.log.info("Battery DISCHARGING with {} W " + "[charge={} W, discharge={} W]", batteryPower,
						chargePower, dischargePower);

			} else if (batteryPower < 0) {

				this.log.info("Battery CHARGING with {} W " + "[charge={} W, discharge={} W]", Math.abs(batteryPower),
						chargePower, dischargePower);

			} else {

				this.log.info("Battery IDLE " + "[charge={} W, discharge={} W]", chargePower, dischargePower);
			}

		} catch (OpenemsException e) {

			this.log.warn("Failed to calculate DC battery power", e);
		}
	}

	private void installListeners() {

		final Consumer<Value<Float>> calculateFloat = ignore -> {
			this.recalculateDcDischargePower();
		};

		try {

			this.getModule1DcwChannel().onSetNextValue(calculateFloat);

			this.getModule2DcwChannel().onSetNextValue(calculateFloat);

			this.getModule3DcwChannel().onSetNextValue(calculateFloat);

			this.getModule4DcwChannel().onSetNextValue(calculateFloat);

		} catch (OpenemsException e) {

			this.log.warn("Failed to install listeners", e);
		}
	}

	private void calculateEnergy() {

		var activePower = this.getActivePower().get();

		if (activePower == null) {

			this.calculateActiveChargeEnergy.update(null);
			this.calculateActiveDischargeEnergy.update(null);

		} else if (activePower > 0) {

			this.calculateActiveChargeEnergy.update(0);
			this.calculateActiveDischargeEnergy.update(activePower);

		} else {

			this.calculateActiveChargeEnergy.update(activePower * -1);
			this.calculateActiveDischargeEnergy.update(0);
		}

		var dcPower = this.getDcDischargePower().get();

		if (dcPower == null) {

			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);

		} else if (dcPower > 0) {

			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(dcPower);

		} else {

			this.calculateDcChargeEnergy.update(dcPower * -1);
			this.calculateDcDischargeEnergy.update(0);
		}
	}

	@Override
	public Integer getSurplusPower() {
		return 0;
	}

	@Override
	public String debugLog() {
		return "|L:" + this.getActivePower().asString();
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {

		this._setStartStop(value);
	}

	@Override
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, //
			policy = DYNAMIC, //
			policyOption = GREEDY, //
			unbind = "removeCharger")
	public void addCharger(FroniusGen24DcCharger charger) {
		this.chargers.add(charger);
	}

	@Override
	public void removeCharger(FroniusGen24DcCharger charger) {
		this.chargers.remove(charger);
	}

	@Override
	public Integer getDcPvPower() {

		return this.chargers.stream().map(charger -> charger.getActualPower().get()).filter(java.util.Objects::nonNull)
				.reduce(Integer::sum).orElse(null);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public Channel<Float> getModule1DcwChannel() throws OpenemsException {

		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_1_D_C_W);
	}

	@Override
	public Channel<Float> getModule2DcwChannel() throws OpenemsException {

		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_2_D_C_W);
	}

	/**
	 * Gets the DC power channel for module 3.
	 *
	 * @return the channel
	 * @throws OpenemsException on error
	 */
	public Channel<Float> getModule3DcwChannel() throws OpenemsException {

		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_3_D_C_W);
	}

	/**
	 * Gets the DC power channel for module 4.
	 *
	 * @return the channel
	 * @throws OpenemsException on error
	 */
	public Channel<Float> getModule4DcwChannel() throws OpenemsException {

		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_4_D_C_W);
	}

	/**
	 * Gets the maximum charge power channel.
	 *
	 * <p>
	 * Package-private: only used internally by {@link ApplyPowerHandler}, which
	 * holds a reference to this concrete class (not the public
	 * {@link BatteryInverterFroniusGen24} interface), so this doesn't need to be
	 * public/interface-exposed.
	 *
	 * @return the channel
	 * @throws OpenemsException on error
	 */
	Channel<Float> getStorageWChaMaxChannel() throws OpenemsException {

		return this.getSunSpecChannelOrError(DefaultSunSpecModel.S124.W_CHA_MAX);
	}

	@Override
	public boolean isInitialized() {
		return this.isSunSpecInitializationCompleted();
	}

	@Override
	public IntegerReadChannel getActivePowerChannel() {
		return this.channel(SymmetricBatteryInverter.ChannelId.ACTIVE_POWER);
	}

	@Override
	public IntegerReadChannel getReactivePowerChannel() {
		return this.channel(SymmetricBatteryInverter.ChannelId.REACTIVE_POWER);
	}

	@Override
	public IntegerReadChannel getMaxApparentPowerChannel() {
		return this.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER);
	}

	@Override
	public boolean isManaged() {
		return true;
	}

	@Override
	public void _setActivePower(Integer value) {
		this.channel(SymmetricBatteryInverter.ChannelId.ACTIVE_POWER).setNextValue(value);
	}

	@Override
	public void _setActivePower(int value) {
		this.channel(SymmetricBatteryInverter.ChannelId.ACTIVE_POWER).setNextValue(value);
	}

	@Override
	public void _setReactivePower(Integer value) {
		this.channel(SymmetricBatteryInverter.ChannelId.REACTIVE_POWER).setNextValue(value);
	}

	@Override
	public void _setReactivePower(int value) {
		this.channel(SymmetricBatteryInverter.ChannelId.REACTIVE_POWER).setNextValue(value);
	}

	@Override
	public void _setMaxApparentPower(Integer value) {
		this.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER).setNextValue(value);
	}

	@Override
	public void _setMaxApparentPower(int value) {
		this.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER).setNextValue(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Value<Integer> getActivePower() {
		return (Value<Integer>) this.channel(SymmetricBatteryInverter.ChannelId.ACTIVE_POWER).value();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Value<Integer> getReactivePower() {
		return (Value<Integer>) this.channel(SymmetricBatteryInverter.ChannelId.REACTIVE_POWER).value();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Value<Integer> getMaxApparentPower() {
		return (Value<Integer>) this.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER).value();
	}

	// -------------------------------------------------------------------------
	// Package-visible helper methods for ApplyPowerHandler (WMaxLim S123)
	// -------------------------------------------------------------------------

	/**
	 * S121.WMax - Rated power of the inverter.
	 *
	 * @return the channel
	 * @throws OpenemsException on error
	 */
	Channel<Float> getWMaxChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(DefaultSunSpecModel.S121.W_MAX);
	}

	/**
	 * Write S123.WMaxLimPct. Value 0..100%, written as FloatWriteChannel.
	 *
	 * @param value the percentage value (0..100)
	 * @throws OpenemsNamedException on error
	 */
	void writeWMaxLimPct(int value) throws OpenemsNamedException {
		int pct = Math.max(0, Math.min(100, value));

		((FloatWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S123.W_MAX_LIM_PCT))
				.setNextWriteValue((float) pct);
	}

	/**
	 * Write S123.WMaxLim_Ena. Expects enum 0/1.
	 *
	 * @param value 0 to disable, 1 to enable
	 * @throws OpenemsNamedException on error
	 */
	void writeWMaxLimEna(int value) throws OpenemsNamedException {
		int ena = value == 0 ? 0 : 1;

		((EnumWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S123.W_MAX_LIM_ENA))
				.setNextWriteValue(ena);
	}
}