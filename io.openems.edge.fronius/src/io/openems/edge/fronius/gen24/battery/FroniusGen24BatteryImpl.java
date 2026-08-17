package io.openems.edge.fronius.gen24.battery;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Map;
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

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.fronius.enums.BatteryPreset;
import io.openems.edge.fronius.gen24.batteryinverter.S160SunSpecModel;

/**
 * Battery component for the Fronius Gen24 hybrid inverter.
 *
 * <p>
 * The physical battery is connected to the inverter exclusively via RS485 -
 * there is no separate connection to the battery itself. All battery values
 * (SOC, module voltages/currents/power, capacity, charge power limit) are
 * exposed by the inverter's own SunSpec register map (Models S120, S124, S160 -
 * the same models {@code BatteryInverterFroniusGen24Impl} reads).
 *
 * <p>
 * This component performs its own, independent SunSpec discovery against that
 * same physical Modbus device, rather than holding an OSGi
 * {@literal @Reference} to {@code BatteryInverterFroniusGen24Impl}.
 * {@code GenericEss} is the only component that needs both, and passes the
 * Battery into {@code BatteryInverterFroniusGen24Impl#run(Battery, int, int)}
 * each cycle.
 *
 * <p>
 * IMPORTANT: {@code modbus_id}/{@code modbusUnitId} in {@link Config} must
 * match the BatteryInverter's Modbus configuration, since both components
 * independently talk to the same physical inverter.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Ess.Fronius.Gen24.Battery", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class FroniusGen24BatteryImpl extends AbstractOpenemsSunSpecComponent
		implements Battery, FroniusGen24Battery, ModbusComponent, OpenemsComponent {

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(DefaultSunSpecModel.S_120, Priority.LOW) //
			.put(DefaultSunSpecModel.S_124, Priority.LOW) //
			.put(S160SunSpecModel.S_160, Priority.HIGH) //
			.build();

	private final Logger log = LoggerFactory.getLogger(FroniusGen24BatteryImpl.class);

	private Config config;

	public FroniusGen24BatteryImpl() {
		super(ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				FroniusGen24Battery.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {

		this.config = config;

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(),
				READ_FROM_MODBUS_BLOCK);

		this.setConfiguredVoltageLimits();
		this.addProprietaryWriteRegisters();
	}

	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Adds the Fronius-proprietary (non-SunSpec) write registers for storage
	 * control mode, charge/discharge power rate limiting, and the watchdog revert
	 * timeout.
	 */
	private void addProprietaryWriteRegisters() throws OpenemsException {

		this.getModbusProtocol().addTask(//
				new FC16WriteRegistersTask(40348, //
						this.m(FroniusGen24Battery.ChannelId.SET_STORAGE_CONTROL_MODE,
								new UnsignedWordElement(40348))));

		this.getModbusProtocol().addTask(//
				new FC16WriteRegistersTask(40355, //
						this.m(FroniusGen24Battery.ChannelId.SET_OUT_W_RTE, new SignedWordElement(40355)), //
						this.m(FroniusGen24Battery.ChannelId.SET_IN_W_RTE, new SignedWordElement(40356))));

		// InOutWRte_RvrtTms (SunSpec S124 offset 16) - verified against Fronius'
		// official Gen24 register map (list no. 40359, minus Fronius' -1 addressing
		// offset = 40358). Valid range 0-28800s (28800s = 8h, confirming this is
		// the watchdog register).
		this.getModbusProtocol().addTask(//
				new FC16WriteRegistersTask(40358, //
						this.m(FroniusGen24Battery.ChannelId.SET_REVERT_TIMEOUT, new UnsignedWordElement(40358))));
	}

	@Override
	protected void onSunSpecInitializationCompleted() {

		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

		this.mapFirstPointToChannel(Battery.ChannelId.SOC, //
				ElementToChannelConverter.DIRECT_1_TO_1, DefaultSunSpecModel.S124.CHA_STATE);

		this.mapFirstPointToChannel(Battery.ChannelId.CAPACITY, //
				ElementToChannelConverter.DIRECT_1_TO_1, DefaultSunSpecModel.S120.W_H_RTG);

		this.mapFirstPointToChannel(FroniusGen24Battery.ChannelId.BAT_STATUS, //
				enumConverter(io.openems.edge.fronius.enums.BatteryState.values()), DefaultSunSpecModel.S124.CHA_ST);

		this.mapFirstPointToChannel(FroniusGen24Battery.ChannelId.DEBUG_INVERTER_STATE, //
				enumConverter(DefaultSunSpecModel.S103_St.values()), DefaultSunSpecModel.S103.ST);

		this.installListeners();
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

	private void installListeners() {

		try {
			final Consumer<Value<Float>> recalcCurrentAndVoltage = ignore -> this.recalculateCurrentAndVoltage();

			this.getModule3DcaChannel().onSetNextValue(recalcCurrentAndVoltage);
			this.getModule4DcaChannel().onSetNextValue(recalcCurrentAndVoltage);
			this.getModule3DcVChannel().onSetNextValue(recalcCurrentAndVoltage);
			this.getModule4DcVChannel().onSetNextValue(recalcCurrentAndVoltage);

			final Consumer<Value<Float>> recalcChargeDischargePower = ignore -> this.recalculateChargeDischargePower();

			this.getModule3DcwChannel().onSetNextValue(recalcChargeDischargePower);
			this.getModule4DcwChannel().onSetNextValue(recalcChargeDischargePower);

			final Consumer<Value<Float>> recalcLimits = ignore -> this.updatePowerLimits();

			this.getStorageWChaMaxChannel().onSetNextValue(recalcLimits);
			this.getModule3DcVChannel().onSetNextValue(recalcLimits);
			this.getModule4DcVChannel().onSetNextValue(recalcLimits);

			// BATTERY_WARNING/BATTERY_ERROR: S124 (Storage) has no dedicated
			// fault/event bitfield of its own, so these are derived from the
			// shared, whole-inverter S103.St status - Battery reads this
			// independently (same physical device the Inverter also reads it
			// from), since it holds no reference to the Inverter.
			final Consumer<Value<DefaultSunSpecModel.S103_St>> updateWarningAndError = value -> {
				var state = value.get();
				this._setBatteryWarning(state == DefaultSunSpecModel.S103_St.THROTTLED);
				this._setBatteryError(state == DefaultSunSpecModel.S103_St.FAULT);
			};
			this.getDebugInverterStateChannel().onSetNextValue(updateWarningAndError);

		} catch (OpenemsException e) {
			this.log.warn("Failed to install listeners", e);
		}
	}

	private void recalculateCurrentAndVoltage() {

		var currentModule3 = this.readFloat(this::getModule3DcaChannel);
		var currentModule4 = this.readFloat(this::getModule4DcaChannel);

		this.channel(Battery.ChannelId.CURRENT).setNextValue(this.maxAsInteger(currentModule3, currentModule4));

		var voltageModule3 = this.readFloat(this::getModule3DcVChannel);
		var voltageModule4 = this.readFloat(this::getModule4DcVChannel);

		this.channel(Battery.ChannelId.VOLTAGE).setNextValue(this.maxAsInteger(voltageModule3, voltageModule4));
	}

	private void recalculateChargeDischargePower() {

		this.channel(FroniusGen24Battery.ChannelId.CUR_BAT_CHA)
				.setNextValue(this.toInteger(this.readFloat(this::getModule3DcwChannel)));

		this.channel(FroniusGen24Battery.ChannelId.CUR_BAT_DSCH)
				.setNextValue(this.toInteger(this.readFloat(this::getModule4DcwChannel)));
	}

	private void updatePowerLimits() {

		var wChaMax = this.readFloat(this::getStorageWChaMaxChannel);
		var voltage = this.readBatteryVoltage();

		var currentLimit = this.calculateCurrentLimit(wChaMax, voltage);

		this._setChargeMaxCurrent(currentLimit);
		this._setDischargeMaxCurrent(currentLimit);
	}

	private void setConfiguredVoltageLimits() {
		var preset = this.config.batteryPreset();
		var modules = this.config.numberOfModules();

		var presetChargeMax = preset.getChargeMaxVoltage(modules);
		var presetDischargeMin = preset.getDischargeMinVoltage(modules);

		if (presetChargeMax != null && presetDischargeMin != null) {
			// Use preset values from the selected battery model
			this._setChargeMaxVoltage(presetChargeMax);
			this._setDischargeMinVoltage(presetDischargeMin);
		} else {
			// CUSTOM preset or invalid module count - use manually configured values
			if (preset != BatteryPreset.CUSTOM) {
				this.logWarn(this.log, "Battery preset [" + preset.getName() + "] does not support [" + modules
						+ "] modules. Falling back to CUSTOM values.");
			}
			this._setChargeMaxVoltage(Math.max(1, this.config.chargeMaxVoltage()));
			this._setDischargeMinVoltage(Math.max(1, this.config.dischargeMinVoltage()));
		}
	}

	private Integer readBatteryVoltage() {

		var nextValue = this.getVoltageChannel().getNextValue();

		if (nextValue.isDefined()) {
			return nextValue.get();
		}

		var value = this.getVoltageChannel().value();

		if (value.isDefined()) {
			return value.get();
		}

		return null;
	}

	private Float readFloat(FloatChannelSupplier channelSupplier) {

		try {

			var channel = channelSupplier.get();

			var nextValue = channel.getNextValue();

			if (nextValue.isDefined()) {
				return nextValue.get();
			}

			var value = channel.value();

			if (value.isDefined()) {
				return value.get();
			}

			return null;

		} catch (OpenemsException e) {
			return null;
		}
	}

	private Integer toInteger(Float value) {

		if (value == null) {
			return null;
		}

		return value.intValue();
	}

	private Integer maxAsInteger(Float first, Float second) {

		var firstValue = this.toInteger(first);
		var secondValue = this.toInteger(second);

		if (firstValue == null) {
			return secondValue;
		}

		if (secondValue == null) {
			return firstValue;
		}

		return Math.max(firstValue, secondValue);
	}

	private Integer calculateCurrentLimit(Float wChaMax, Integer voltage) {
		if (voltage == null || voltage <= 0) {
			return null;
		}
		if (wChaMax == null) {
			return null;
		}
		return Math.max(0, (int) Math.floor(Math.abs(wChaMax) / voltage));
	}

	private int getNumberOfModules() {
		return Math.max(1, this.config.numberOfModules());
	}

	private float getChargeMaxVoltagePerModule() {
		return (float) this.config.chargeMaxVoltage() / this.getNumberOfModules();
	}

	private float getDischargeMinVoltagePerModule() {
		return (float) this.config.dischargeMinVoltage() / this.getNumberOfModules();
	}

	// -------------------------------------------------------------------------
	// SunSpec channel accessors (own independent read of the same registers
	// BatteryInverterFroniusGen24Impl reads - see class Javadoc).
	// -------------------------------------------------------------------------

	private Channel<Float> getModule3DcaChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_3_D_C_A);
	}

	private Channel<Float> getModule4DcaChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_4_D_C_A);
	}

	private Channel<Float> getModule3DcVChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_3_D_C_V);
	}

	private Channel<Float> getModule4DcVChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_4_D_C_V);
	}

	private Channel<Float> getModule3DcwChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_3_D_C_W);
	}

	private Channel<Float> getModule4DcwChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_4_D_C_W);
	}

	private Channel<Float> getStorageWChaMaxChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(DefaultSunSpecModel.S124.W_CHA_MAX);
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		this._setStartStop(value);
	}

	@Override
	public String debugLog() {

		return "Soc:" + this.getSoc().asString() //
				+ "|V:" + this.getVoltage().asString() //
				+ "|ChaMaxV:" + this.getChargeMaxVoltage().asString() //
				+ "|DschMinV:" + this.getDischargeMinVoltage().asString() //
				+ "|ChaMaxI:" + this.getChargeMaxCurrent().asString() //
				+ "|DschMaxI:" + this.getDischargeMaxCurrent().asString() //
				+ "|Modules:" + this.getNumberOfModules() //
				+ "|ChaMaxV/Mod:" + this.getChargeMaxVoltagePerModule() //
				+ "|DschMinV/Mod:" + this.getDischargeMinVoltagePerModule();
	}

	@FunctionalInterface
	private interface FloatChannelSupplier {
		Channel<Float> get() throws OpenemsException;
	}
}
