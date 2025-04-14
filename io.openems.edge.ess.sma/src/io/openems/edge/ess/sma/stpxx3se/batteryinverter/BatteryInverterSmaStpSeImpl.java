package io.openems.edge.ess.sma.stpxx3se.batteryinverter;

import java.util.Map;
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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.sunspec.AbstractSunSpecBatteryInverter;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.sma.stpxx3se.battery.SmaBattery;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Sma.StpSe.Inverter", immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BatteryInverterSmaStpSeImpl extends AbstractSunSpecBatteryInverter
		implements BatteryInverterSmaStpSe, HybridManagedSymmetricBatteryInverter, ManagedSymmetricBatteryInverter,
		SymmetricBatteryInverter, StartStoppable, ModbusComponent, TimedataProvider, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BatteryInverterSmaStpSeImpl.class);
	private static final int READ_FROM_MODBUS_BLOCK = 1;

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) // common
			.put(DefaultSunSpecModel.S_103, Priority.HIGH) // Inverter 3phase
			.put(DefaultSunSpecModel.S_120, Priority.LOW) // Nameplate
			.put(DefaultSunSpecModel.S_121, Priority.LOW) // Basic Settings
			.put(DefaultSunSpecModel.S_122, Priority.LOW) // Measurement Status
			// .put(DefaultSunSpecModel.S_123, Priority.LOW) // Immediate Control PV
			// .put(DefaultSunSpecModel.S_64870, Priority.HIGH) // Vendor specific
			.put(S160SunSpecModel.S_160, Priority.HIGH) // Multiple MPPT Inverter Extension Model
			.build();

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Sum sum;

	@Reference
	protected Power power;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
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

	private Config config;

	private boolean firstRun = true;

	public BatteryInverterSmaStpSeImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				HybridManagedSymmetricBatteryInverter.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryInverterSmaStpSe.ChannelId.values() //
		);

	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}
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

		this.mapFirstPointToChannel(SymmetricBatteryInverter.ChannelId.REACTIVE_POWER,
				ElementToChannelConverter.DIRECT_1_TO_1, DefaultSunSpecModel.S103.V_AR);

		this.mapFirstPointToChannel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER,
				ElementToChannelConverter.DIRECT_1_TO_1, DefaultSunSpecModel.S121.W_MAX);

		this.installListeners();
		this._setInitializing(false);
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {
		this.calculateEnergy();
		this._setWrongBattery(!(battery instanceof SmaBattery));
		if (battery instanceof SmaBattery sma) {
			if (this.firstRun) {
				this.installBatteryListeners(sma);
				this.firstRun = false;
			}
			this.applyPowerHandler.apply(sma, setActivePower, setReactivePower, this.config.controlMode(),
					this.sum.getGridActivePower(), this.getActivePower());
		}
	}

	private void installBatteryListeners(SmaBattery sma) {
		final Consumer<Value<Integer>> calculate = ignore -> {
			this._setDcDischargePower(TypeUtils.subtract(//
					sma.getCurBatDschChannel().getNextValue().get(), //
					sma.getCurBatChaChannel().getNextValue().get())); //
		};
		sma.getCurBatChaChannel().onSetNextValue(calculate);
		sma.getCurBatDschChannel().onSetNextValue(calculate);
	}

	private void installListeners() {
		// NOTE: Twice the same callback needed, as we have to install it on Integer-
		// and Float-Channels
		final Consumer<Value<Integer>> calculate = ignore -> {
			Integer p1;
			Integer p2;
			try {
				p1 = this.getModule1DcwChannel().getNextValue() //
						.orElse(0F).intValue();
				p2 = this.getModule2DcwChannel().getNextValue() //
						.orElse(0F).intValue();
			} catch (OpenemsException e) {
				return;
			}
			this._setActivePower(TypeUtils.sum(//
					p1, p2, this.getDcDischargePowerChannel().getNextValue().get())); //
		};
		final Consumer<Value<Float>> calculateF = ignore -> {
			Integer p1;
			Integer p2;
			try {
				p1 = this.getModule1DcwChannel().getNextValue() //
						.orElse(0F).intValue();
				p2 = this.getModule2DcwChannel().getNextValue() //
						.orElse(0F).intValue();
			} catch (OpenemsException e) {
				return;
			}
			this._setActivePower(TypeUtils.sum(//
					p1, p2, this.getDcDischargePowerChannel().getNextValue().get())); //
		};
		this.getDcDischargePowerChannel().onSetNextValue(calculate);
		try {
			this.getModule1DcwChannel().onSetNextValue(calculateF);
			this.getModule2DcwChannel().onSetNextValue(calculateF);
		} catch (OpenemsException e) {
			// We should never land here
			;
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// AC
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateActiveChargeEnergy.update(null);
			this.calculateActiveDischargeEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Inverter
			this.calculateActiveChargeEnergy.update(0);
			this.calculateActiveDischargeEnergy.update(activePower);
		} else {
			// Sell-To-Inverter
			this.calculateActiveChargeEnergy.update(activePower * -1);
			this.calculateActiveDischargeEnergy.update(0);
		}
		// DC
		var dcPower = this.getDcDischargePower().get();
		if (dcPower == null) {
			// Not available
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		} else if (dcPower > 0) {
			// Discharging battery
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(dcPower);
		} else {
			// Charging battery
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
		return "|L:" + this.getActivePower().asString(); //
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
	public Integer getDcPvPower() {
		try {
			return TypeUtils.sum(//
					// TODO Catch null more elegantly maybe
					this.getModule1DcwChannel().value() //
							.orElse(0F).intValue(), //
					this.getModule2DcwChannel().value() //
							.orElse(0F).intValue());
		} catch (OpenemsException e) {
			return null;
		}
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
	public Channel<Float> getModule1DcaChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_1_D_C_A);
	}

	@Override
	public Channel<Float> getModule1DcvChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_1_D_C_V);
	}

	@Override
	public Channel<Float> getModule2DcwChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_2_D_C_W);
	}

	@Override
	public Channel<Float> getModule2DcaChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_2_D_C_A);
	}

	@Override
	public Channel<Float> getModule2DcvChannel() throws OpenemsException {
		return this.getSunSpecChannelOrError(S160SunSpecModel.S160.MODULE_2_D_C_V);
	}

	@Override
	public boolean isInitialized() {
		return this.isSunSpecInitializationCompleted();
	}

}
