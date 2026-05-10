package io.openems.edge.core.sum;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.core.sum.ExtremeEverValues.Range.NEGATIVE;
import static io.openems.edge.core.sum.ExtremeEverValues.Range.POSTIVE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.utils.IntUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.core.sum.handler.ChargerHandlerImpl;
import io.openems.edge.core.sum.handler.EssHandlerImpl;
import io.openems.edge.core.sum.handler.MeterHandlerImpl;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.timeofusetariff.api.TariffManager;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = Sum.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"enabled=true" //
		})
public class SumImpl extends AbstractOpenemsComponent implements Sum, OpenemsComponent, ModbusSlave, TimedataProvider {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile Timedata timedata = null;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private EssHandlerImpl essHandler;

	@Reference
	private MeterHandlerImpl meterHandler;

	@Reference
	private ChargerHandlerImpl chargerHandler;

	@Reference
	private TariffManager tariffManager;

	protected final CalculateEnergyFromPower calculateProductionToConsumptionEnergy = new CalculateEnergyFromPower(this,
			Sum.ChannelId.PRODUCTION_TO_CONSUMPTION_ENERGY);
	protected final CalculateEnergyFromPower calculateProductionToGridEnergy = new CalculateEnergyFromPower(this,
			Sum.ChannelId.PRODUCTION_TO_GRID_ENERGY);
	protected final CalculateEnergyFromPower calculateProductionToEssEnergy = new CalculateEnergyFromPower(this,
			Sum.ChannelId.PRODUCTION_TO_ESS_ENERGY);
	protected final CalculateEnergyFromPower calculateGridToConsumptionEnergy = new CalculateEnergyFromPower(this,
			Sum.ChannelId.GRID_TO_CONSUMPTION_ENERGY);
	protected final CalculateEnergyFromPower calculateEssToConsumptionEnergy = new CalculateEnergyFromPower(this,
			Sum.ChannelId.ESS_TO_CONSUMPTION_ENERGY);
	protected final CalculateEnergyFromPower calculateGridToEssEnergy = new CalculateEnergyFromPower(this,
			Sum.ChannelId.GRID_TO_ESS_ENERGY);
	protected final CalculateEnergyFromPower calculateEssToGridEnergy = new CalculateEnergyFromPower(this,
			Sum.ChannelId.ESS_TO_GRID_ENERGY);

	private final EnergyValuesHandler energyValuesHandler;
	private final Set<String> ignoreStateComponents = new HashSet<>();

	private final CalculateActiveTime calculateOffGridTime = new CalculateActiveTime(this,
			Sum.ChannelId.GRID_MODE_OFF_GRID_TIME);
	private final CalculateActiveTime calculateOffGridGensetTime = new CalculateActiveTime(this,
			Sum.ChannelId.GRID_MODE_OFF_GRID_GENSET_TIME);

	private final ExtremeEverValues extremeEverValues = ExtremeEverValues.create(SINGLETON_SERVICE_PID) //
			.add(Sum.ChannelId.GRID_MIN_ACTIVE_POWER, "gridMinActivePower", //
					NEGATIVE, Sum.ChannelId.GRID_ACTIVE_POWER) //
			.add(Sum.ChannelId.GRID_MAX_ACTIVE_POWER, "gridMaxActivePower", //
					POSTIVE, Sum.ChannelId.GRID_ACTIVE_POWER) //
			.add(Sum.ChannelId.PRODUCTION_MAX_ACTIVE_POWER, "productionMaxActivePower", //
					POSTIVE, Sum.ChannelId.PRODUCTION_ACTIVE_POWER) //
			.add(Sum.ChannelId.CONSUMPTION_MAX_ACTIVE_POWER, "consumptionMaxActivePower", //
					POSTIVE, Sum.ChannelId.CONSUMPTION_ACTIVE_POWER) //
			.add(Sum.ChannelId.ESS_MIN_DISCHARGE_POWER, "essMinDischargePower", //
					NEGATIVE, Sum.ChannelId.ESS_DISCHARGE_POWER) //
			.add(Sum.ChannelId.ESS_MAX_DISCHARGE_POWER, "essMaxDischargePower", //
					POSTIVE, Sum.ChannelId.ESS_DISCHARGE_POWER) //
			.build();

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Sum.getModbusSlaveNatureTable(accessMode));
	}

	public SumImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Sum.ChannelId.values() //
		);
		this.energyValuesHandler = new EnergyValuesHandler(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.applyConfig(context, config);

		this.energyValuesHandler.activate();

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.applyConfig(context, config);

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	private synchronized void applyConfig(ComponentContext context, Config config) {
		// Read max power values
		this.extremeEverValues.initializeFromContext(context);

		// Parse Ignore States
		this.ignoreStateComponents.clear();
		for (String id : config.ignoreStateComponents()) {
			if (!id.isEmpty()) {
				this.ignoreStateComponents.add(id);
			}
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.energyValuesHandler.deactivate();
		super.deactivate();
	}

	@Override
	public void updateChannelsBeforeProcessImage() {
		this.essHandler.calculate();
		this.assignEssChannels();

		this.meterHandler.calculate();
		this.assignGridChannels();
		this.assignProductionChannels();

		this.chargerHandler.calculate();

		this.assignConsumptionChannels();
		this.calculatePowerDistributionAndSet();

		this.assignTariffChannels();

		this.updateExtremeEverValues();
		this.calculateAndSetSystemState();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	private void assignEssChannels() {
		this.getEssSocChannel().setNextValue(this.essHandler.getSoc());
		setValue(this, Sum.ChannelId.ESS_ACTIVE_POWER, this.essHandler.getActivePower());
		setValue(this, Sum.ChannelId.ESS_ACTIVE_POWER_L1, this.essHandler.getActivePowerL1());
		setValue(this, Sum.ChannelId.ESS_ACTIVE_POWER_L2, this.essHandler.getActivePowerL2());
		setValue(this, Sum.ChannelId.ESS_ACTIVE_POWER_L3, this.essHandler.getActivePowerL3());
		setValue(this, Sum.ChannelId.ESS_REACTIVE_POWER, this.essHandler.getReactivePower());
		setValue(this, Sum.ChannelId.ESS_MAX_APPARENT_POWER, this.essHandler.getMaxApparentPower());
		setValue(this, Sum.ChannelId.ESS_CAPACITY, this.essHandler.getCapacity());

		var gridMode = this.essHandler.getGridMode();
		setValue(this, Sum.ChannelId.GRID_MODE, gridMode);
		this.calculateOffGridTime.update(gridMode == GridMode.OFF_GRID);
		this.calculateOffGridGensetTime.update(gridMode == GridMode.OFF_GRID_GENSET);

		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_ACTIVE_CHARGE_ENERGY,
				this.essHandler.getActiveChargeEnergy());
		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_ACTIVE_DISCHARGE_ENERGY,
				this.essHandler.getActiveDischargeEnergy());
		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_DC_CHARGE_ENERGY, this.essHandler.getDcChargeEnergy());
		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_DC_DISCHARGE_ENERGY,
				this.essHandler.getDcDischargeEnergy());
		this.getEssDischargePowerChannel().setNextValue(this.essHandler.getDcDischargePower());
	}

	private void assignGridChannels() {
		setValue(this, Sum.ChannelId.GRID_ACTIVE_POWER, this.meterHandler.getGridActivePower());
		setValue(this, Sum.ChannelId.GRID_ACTIVE_POWER_L1, this.meterHandler.getGridActivePowerL1());
		setValue(this, Sum.ChannelId.GRID_ACTIVE_POWER_L2, this.meterHandler.getGridActivePowerL2());
		setValue(this, Sum.ChannelId.GRID_ACTIVE_POWER_L3, this.meterHandler.getGridActivePowerL3());

		setValue(this, Sum.ChannelId.GRID_GENSET_ACTIVE_POWER, this.meterHandler.gridGensetActivePower());
		setValue(this, Sum.ChannelId.GRID_GENSET_ACTIVE_POWER_L1, this.meterHandler.gridGensetActivePowerL1());
		setValue(this, Sum.ChannelId.GRID_GENSET_ACTIVE_POWER_L2, this.meterHandler.gridGensetActivePowerL2());
		setValue(this, Sum.ChannelId.GRID_GENSET_ACTIVE_POWER_L3, this.meterHandler.gridGensetActivePowerL3());

		this.energyValuesHandler.setValue(Sum.ChannelId.GRID_BUY_ACTIVE_ENERGY,
				this.meterHandler.getGridBuyActiveEnergy());
		this.energyValuesHandler.setValue(Sum.ChannelId.GRID_SELL_ACTIVE_ENERGY,
				this.meterHandler.getGridSellActiveEnergy());
	}

	private void assignProductionChannels() {
		var acPower = this.meterHandler.getProductionAcActivePower();
		setValue(this, Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER, acPower);
		setValue(this, Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER_L1, this.meterHandler.getProductionAcActivePowerL1());
		setValue(this, Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER_L2, this.meterHandler.getProductionAcActivePowerL2());
		setValue(this, Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER_L3, this.meterHandler.getProductionAcActivePowerL3());

		var dcPower = this.chargerHandler.getProductionDcActualPower();
		setValue(this, Sum.ChannelId.PRODUCTION_DC_ACTUAL_POWER, dcPower);
		var totalPower = IntUtils.sumInteger(acPower, dcPower);
		setValue(this, Sum.ChannelId.PRODUCTION_ACTIVE_POWER, totalPower);
		// TODO calculate actual "Unmanaged"-ProductionActivePower
		setValue(this, Sum.ChannelId.UNMANAGED_PRODUCTION_ACTIVE_POWER, totalPower);

		var acEnergy = this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_AC_ACTIVE_ENERGY,
				this.meterHandler.getProductionAcActiveEnergy());
		var dcEnergy = this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_DC_ACTIVE_ENERGY,
				this.chargerHandler.getProductionDcActiveEnergy());
		this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_ACTIVE_ENERGY, TypeUtils.sum(acEnergy, dcEnergy));
	}

	private void assignConsumptionChannels() {
		var ess = this.essHandler.getActivePower();
		var grid = this.meterHandler.getGridActivePower();
		var prod = this.meterHandler.getProductionAcActivePower();

		var consumptionPower = IntUtils.sumInteger(ess, grid, prod);
		setValue(this, Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, consumptionPower);

		setValue(this, Sum.ChannelId.CONSUMPTION_ACTIVE_POWER_L1,
				IntUtils.sumInteger(this.essHandler.getActivePowerL1(), this.meterHandler.getGridActivePowerL1(),
						this.meterHandler.getProductionAcActivePowerL1()));
		setValue(this, Sum.ChannelId.CONSUMPTION_ACTIVE_POWER_L2,
				IntUtils.sumInteger(this.essHandler.getActivePowerL2(), this.meterHandler.getGridActivePowerL2(),
						this.meterHandler.getProductionAcActivePowerL2()));
		setValue(this, Sum.ChannelId.CONSUMPTION_ACTIVE_POWER_L3,
				IntUtils.sumInteger(this.essHandler.getActivePowerL3(), this.meterHandler.getGridActivePowerL3(),
						this.meterHandler.getProductionAcActivePowerL3()));

		setValue(this, Sum.ChannelId.UNMANAGED_CONSUMPTION_ACTIVE_POWER,
				TypeUtils.subtract(consumptionPower, this.meterHandler.getManagedConsumptionActivePower()));

		// Energy Calculation
		var enter = TypeUtils.sum(this.essHandler.getActiveDischargeEnergy(),
				this.meterHandler.getGridBuyActiveEnergy(), this.meterHandler.getProductionAcActiveEnergy());
		var leave = TypeUtils.sum(this.essHandler.getActiveChargeEnergy(), this.meterHandler.getGridSellActiveEnergy(),
				this.meterHandler.getProductionAcActiveEnergyNegative());

		var consumptionEnergy = Optional.ofNullable(enter).orElse(0L) - Optional.ofNullable(leave).orElse(0L);
		this.energyValuesHandler.setValue(Sum.ChannelId.CONSUMPTION_ACTIVE_ENERGY, consumptionEnergy);
	}

	private void assignTariffChannels() {
		final var now = this.componentManager.getClock().instant();
		setValue(this, Sum.ChannelId.GRID_BUY_PRICE, this.tariffManager.getGridBuyDayAheadPrices().getAt(now));
		setValue(this, Sum.ChannelId.GRID_SELL_PRICE, this.tariffManager.getGridSellDayAheadPrices().getAt(now));
	}

	private void calculateAndSetSystemState() {
		var highestLevel = Level.OK;
		var hasIgnoredComponentStates = false;
		for (var component : this.componentManager.getEnabledComponents()) {
			if (component == this) {
				// ignore myself
				continue;
			}
			var level = component.getState();
			if (this.ignoreStateComponents.contains(component.id()) && level != Level.OK) {
				// This Components State should be ignored
				hasIgnoredComponentStates = true;

			} else {
				setValue(this, Sum.ChannelId.HAS_IGNORED_COMPONENT_STATES, false);
				// Calculate highest State Level
				if (level.getValue() > highestLevel.getValue()) {
					highestLevel = level;
				}
			}
		}

		// There is at least one ignored State -> show info
		if (hasIgnoredComponentStates) {
			setValue(this, Sum.ChannelId.HAS_IGNORED_COMPONENT_STATES, true);
			// Note: this sets the StateChannel 'HAS_IGNORED_COMPONENT_STATES' to true,
			// which sets the Sum 'STATE'-Channel to 'INFO'. We override this below with
			// 'highestLevel'.
			if (Level.INFO.getValue() > highestLevel.getValue()) {
				highestLevel = Level.INFO;
			}
		}
		this.getStateChannel().setNextValue(highestLevel);
	}

	private void calculatePowerDistributionAndSet() {
		PowerDistribution.of(this.meterHandler.getGridActivePower(),
				IntUtils.sumInteger(this.meterHandler.getProductionAcActivePower(),
						this.chargerHandler.getProductionDcActualPower()),
				this.essHandler.getDcDischargePower()).updateChannels(this);
	}

	/**
	 * Calculates maximum/minimum ever values for respective Channels. Extreme
	 * values are persisted in the Config of Core.Sum component once per day.
	 */
	private void updateExtremeEverValues() {
		this.extremeEverValues.update(this, this.cm);
	}

	@Override
	public String debugLog() {
		final var result = new ArrayList<String>();

		// State
		final var state = this.getState();
		result.add(new StringBuilder("State:") //
				.append(state.getName()) //
				.toString());
		// Ess
		final var essSoc = this.getEssSoc();
		final var essActivePower = this.getEssActivePower();
		if (essSoc.isDefined() || essActivePower.isDefined()) {
			final var b = new StringBuilder("Ess ");
			if (essSoc.isDefined() && essActivePower.isDefined()) {
				b.append("SoC:").append(essSoc.asString()).append("|L:").append(essActivePower.asString());
			} else if (essSoc.isDefined()) {
				b.append("SoC:").append(essSoc.asString());
			} else {
				b.append("L:").append(essActivePower.asString());
			}
			result.add(b.toString());
		}
		// Grid
		final var gridActivePower = this.getGridActivePower();
		if (gridActivePower.isDefined()) {
			result.add(new StringBuilder("Grid:") //
					.append(gridActivePower.asString()) //
					.toString());
		}

		// Grid Genset
		final var gridGensetActivePower = this.getGridGensetActivePower();
		if (gridGensetActivePower.isDefined()) {
			result.add(new StringBuilder("Genset:") //
					.append(gridGensetActivePower.asString()) //
					.toString());
		}

		// Production
		final var production = this.getProductionActivePower();
		if (production.isDefined()) {
			final var b = new StringBuilder("Production");
			var productionAc = this.getProductionAcActivePower();
			var productionDc = this.getProductionDcActualPower();
			if (productionAc.isDefined() && productionDc.isDefined()) {
				b //
						.append(" Total:").append(production.asString()) //
						.append(",AC:").append(productionAc.asString()) //
						.append(",DC:").append(productionDc.asString()); //
			} else {
				b.append(":").append(production.asString());
			}
			result.add(b.toString());
		}
		// Consumption
		final var consumptionActivePower = this.getConsumptionActivePower();
		if (consumptionActivePower.isDefined()) {
			result.add(new StringBuilder("Consumption:") //
					.append(consumptionActivePower.asString()) //
					.toString());
		}

		return String.join(" ", result);
	}
}
