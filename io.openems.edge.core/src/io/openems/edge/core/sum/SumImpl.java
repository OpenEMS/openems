package io.openems.edge.core.sum;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.IntUtils.sumInteger;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.TypeUtils.subtract;
import static io.openems.edge.common.type.TypeUtils.sum;
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
import io.openems.common.types.MeterType;
import io.openems.edge.common.channel.calculate.CalculateAverage;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.calculate.CalculateLongSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.sum.SumOptions;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.CalculateGridMode;
import io.openems.edge.ess.api.CalculateSoc;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.evcs.api.MetaEvcs;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

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
		for (String channelId : config.ignoreStateComponents()) {
			if (channelId.isEmpty()) {
				continue;
			}
			this.ignoreStateComponents.add(channelId);
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
		this.calculateChannelValues();
		this.calculateState();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	/**
	 * Calculates the sum-value for each Channel.
	 */
	private void calculateChannelValues() {
		// Ess
		final var essSoc = new CalculateSoc();
		final var essActivePower = new CalculateIntegerSum();
		final var essActivePowerL1 = new CalculateIntegerSum();
		final var essActivePowerL2 = new CalculateIntegerSum();
		final var essActivePowerL3 = new CalculateIntegerSum();

		final var essReactivePower = new CalculateIntegerSum();

		final var essMaxApparentPower = new CalculateIntegerSum();
		final var essGridMode = new CalculateGridMode();
		final var essActiveChargeEnergy = new CalculateLongSum();
		final var essActiveDischargeEnergy = new CalculateLongSum();
		final var essDcChargeEnergy = new CalculateLongSum();
		final var essDcDischargeEnergy = new CalculateLongSum();
		final var essCapacity = new CalculateIntegerSum();
		final var essDcDischargePower = new CalculateIntegerSum();

		// Grid
		final var gridActivePower = new CalculateIntegerSum();
		final var gridActivePowerL1 = new CalculateIntegerSum();
		final var gridActivePowerL2 = new CalculateIntegerSum();
		final var gridActivePowerL3 = new CalculateIntegerSum();
		final var gridBuyPrice = new CalculateAverage();
		final var gridBuyActiveEnergy = new CalculateLongSum();
		final var gridSellActiveEnergy = new CalculateLongSum();

		// Grid Genset
		final var gridGensetActivePower = new CalculateIntegerSum();
		final var gridGensetActivePowerL1 = new CalculateIntegerSum();
		final var gridGensetActivePowerL2 = new CalculateIntegerSum();
		final var gridGensetActivePowerL3 = new CalculateIntegerSum();

		// Production
		final var productionAcActivePower = new CalculateIntegerSum();
		final var productionAcActivePowerL1 = new CalculateIntegerSum();
		final var productionAcActivePowerL2 = new CalculateIntegerSum();
		final var productionAcActivePowerL3 = new CalculateIntegerSum();
		final var productionDcActualPower = new CalculateIntegerSum();
		final var productionAcActiveEnergy = new CalculateLongSum();
		final var productionDcActiveEnergy = new CalculateLongSum();

		// handling the corner-case of wrongly measured negative production, due to
		// cabling errors, etc.
		final var productionAcActiveEnergyNegative = new CalculateLongSum();

		// Consumption
		final var managedConsumptionActivePower = new CalculateIntegerSum();

		for (var component : this.componentManager.getEnabledComponents()) {
			if (component instanceof SumOptions sumOption && !sumOption.addToSum()) {
				continue;
			}

			switch (component) {
			/*
			 * Ess
			 */
			case SymmetricEss ess -> {
				essSoc.add(ess);
				essActivePower.addValue(ess.getActivePowerChannel());
				essReactivePower.addValue(ess.getReactivePowerChannel());
				essMaxApparentPower.addValue(ess.getMaxApparentPowerChannel());
				essGridMode.addValue(ess.getGridModeChannel());
				essActiveChargeEnergy.addValue(ess.getActiveChargeEnergyChannel());
				essActiveDischargeEnergy.addValue(ess.getActiveDischargeEnergyChannel());
				essCapacity.addValue(ess.getCapacityChannel());

				switch (ess) {
				case AsymmetricEss e -> {
					essActivePowerL1.addValue(e.getActivePowerL1Channel());
					essActivePowerL2.addValue(e.getActivePowerL2Channel());
					essActivePowerL3.addValue(e.getActivePowerL3Channel());
				}
				default -> {
					essActivePowerL1.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
					essActivePowerL2.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
					essActivePowerL3.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
				}
				}

				switch (ess) {
				case HybridEss e -> {
					essDcChargeEnergy.addValue(e.getDcChargeEnergyChannel());
					essDcDischargeEnergy.addValue(e.getDcDischargeEnergyChannel());
					essDcDischargePower.addValue(e.getDcDischargePowerChannel());
				}
				default -> {
					essDcChargeEnergy.addValue(ess.getActiveChargeEnergyChannel());
					essDcDischargeEnergy.addValue(ess.getActiveDischargeEnergyChannel());
					essDcDischargePower.addValue(ess.getActivePowerChannel());
				}
				}
			}

			/*
			 * Meter
			 */
			case ElectricityMeter meter -> {
				switch (meter.getMeterType()) {
				case PRODUCTION_AND_CONSUMPTION -> // TODO
					// Production Power is positive, Consumption is negative
					doNothing();

				case CONSUMPTION_METERED -> // TODO
					// Consumption is positive
					doNothing();

				case MANAGED_CONSUMPTION_METERED -> {
					if (meter instanceof MetaEvcs) {
						continue;
					}
					managedConsumptionActivePower.addValue(meter.getActivePowerChannel());
				}

				case CONSUMPTION_NOT_METERED -> // TODO
					// Consumption is positive
					doNothing();

				case GRID, GRID_GENSET -> {
					gridActivePower.addValue(meter.getActivePowerChannel());
					gridBuyActiveEnergy.addValue(meter.getActiveProductionEnergyChannel());
					gridSellActiveEnergy.addValue(meter.getActiveConsumptionEnergyChannel());
					gridActivePowerL1.addValue(meter.getActivePowerL1Channel());
					gridActivePowerL2.addValue(meter.getActivePowerL2Channel());
					gridActivePowerL3.addValue(meter.getActivePowerL3Channel());

					if (meter.getMeterType() == MeterType.GRID_GENSET) {
						gridGensetActivePower.addValue(meter.getActivePowerChannel());
						gridGensetActivePowerL1.addValue(meter.getActivePowerL1Channel());
						gridGensetActivePowerL2.addValue(meter.getActivePowerL2Channel());
						gridGensetActivePowerL3.addValue(meter.getActivePowerL3Channel());
					}
				}

				case PRODUCTION -> {
					/*
					 * Production-Meter
					 */
					productionAcActivePower.addValue(meter.getActivePowerChannel());
					productionAcActiveEnergy.addValue(meter.getActiveProductionEnergyChannel());
					productionAcActiveEnergyNegative.addValue(meter.getActiveConsumptionEnergyChannel());
					productionAcActivePowerL1.addValue(meter.getActivePowerL1Channel());
					productionAcActivePowerL2.addValue(meter.getActivePowerL2Channel());
					productionAcActivePowerL3.addValue(meter.getActivePowerL3Channel());
				}
				}
			}

			/*
			 * Ess DC-Charger
			 */
			case EssDcCharger charger -> {
				productionDcActualPower.addValue(charger.getActualPowerChannel());
				productionDcActiveEnergy.addValue(charger.getActualEnergyChannel());
			}

			/*
			 * Time-of-Use-Tariff
			 */
			case TimeOfUseTariff tou -> {
				gridBuyPrice.addValue(tou.getPrices().getFirst());
			}

			default -> doNothing();
			}
		}

		/*
		 * Set values
		 */
		// Ess
		setValue(this, Sum.ChannelId.ESS_SOC, essSoc.calculate());

		final var essActivePowerSum = essActivePower.calculate();
		setValue(this, Sum.ChannelId.ESS_ACTIVE_POWER, essActivePowerSum);
		final var essActivePowerL1Sum = essActivePowerL1.calculate();
		setValue(this, Sum.ChannelId.ESS_ACTIVE_POWER_L1, essActivePowerL1Sum);
		final var essActivePowerL2Sum = essActivePowerL2.calculate();
		setValue(this, Sum.ChannelId.ESS_ACTIVE_POWER_L2, essActivePowerL2Sum);
		final var essActivePowerL3Sum = essActivePowerL3.calculate();
		setValue(this, Sum.ChannelId.ESS_ACTIVE_POWER_L3, essActivePowerL3Sum);

		setValue(this, Sum.ChannelId.ESS_REACTIVE_POWER, essReactivePower.calculate());

		setValue(this, Sum.ChannelId.ESS_MAX_APPARENT_POWER, essMaxApparentPower.calculate());
		final var gridMode = essGridMode.calculate();
		setValue(this, Sum.ChannelId.GRID_MODE, gridMode);
		this.calculateOffGridTime.update(gridMode == GridMode.OFF_GRID);
		this.calculateOffGridGensetTime.update(gridMode == GridMode.OFF_GRID_GENSET);

		var essActiveChargeEnergySum = essActiveChargeEnergy.calculate();
		essActiveChargeEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.ESS_ACTIVE_CHARGE_ENERGY,
				essActiveChargeEnergySum);
		var essActiveDischargeEnergySum = essActiveDischargeEnergy.calculate();
		essActiveDischargeEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.ESS_ACTIVE_DISCHARGE_ENERGY,
				essActiveDischargeEnergySum);

		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_DC_CHARGE_ENERGY, essDcChargeEnergy.calculate());
		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_DC_DISCHARGE_ENERGY, essDcDischargeEnergy.calculate());

		setValue(this, Sum.ChannelId.ESS_CAPACITY, essCapacity.calculate());

		// Grid
		final var gridActivePowerSum = gridActivePower.calculate();
		setValue(this, Sum.ChannelId.GRID_ACTIVE_POWER, gridActivePowerSum);
		final var gridActivePowerL1Sum = gridActivePowerL1.calculate();
		setValue(this, Sum.ChannelId.GRID_ACTIVE_POWER_L1, gridActivePowerL1Sum);
		final var gridActivePowerL2Sum = gridActivePowerL2.calculate();
		setValue(this, Sum.ChannelId.GRID_ACTIVE_POWER_L2, gridActivePowerL2Sum);
		final var gridActivePowerL3Sum = gridActivePowerL3.calculate();
		setValue(this, Sum.ChannelId.GRID_ACTIVE_POWER_L3, gridActivePowerL3Sum);
		setValue(this, Sum.ChannelId.GRID_BUY_PRICE, gridBuyPrice.calculate());

		final var gridBuyActiveEnergySum = this.energyValuesHandler.setValue(//
				Sum.ChannelId.GRID_BUY_ACTIVE_ENERGY, gridBuyActiveEnergy.calculate());
		final var gridSellActiveEnergySum = this.energyValuesHandler.setValue(//
				Sum.ChannelId.GRID_SELL_ACTIVE_ENERGY, gridSellActiveEnergy.calculate());

		// Grid Genset
		setValue(this, Sum.ChannelId.GRID_GENSET_ACTIVE_POWER, gridGensetActivePower.calculate());
		setValue(this, Sum.ChannelId.GRID_GENSET_ACTIVE_POWER_L1, gridGensetActivePowerL1.calculate());
		setValue(this, Sum.ChannelId.GRID_GENSET_ACTIVE_POWER_L2, gridGensetActivePowerL2.calculate());
		setValue(this, Sum.ChannelId.GRID_GENSET_ACTIVE_POWER_L3, gridGensetActivePowerL3.calculate());

		// Production
		final var productionAcActivePowerSum = productionAcActivePower.calculate();
		setValue(this, Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER, productionAcActivePowerSum);
		final var productionAcActivePowerL1Sum = productionAcActivePowerL1.calculate();
		setValue(this, Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER_L1, productionAcActivePowerL1Sum);
		final var productionAcActivePowerL2Sum = productionAcActivePowerL2.calculate();
		setValue(this, Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER_L2, productionAcActivePowerL2Sum);
		final var productionAcActivePowerL3Sum = productionAcActivePowerL3.calculate();
		setValue(this, Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER_L3, productionAcActivePowerL3Sum);
		final var productionDcActualPowerSum = productionDcActualPower.calculate();
		setValue(this, Sum.ChannelId.PRODUCTION_DC_ACTUAL_POWER, productionDcActualPowerSum);
		final var productionActivePower = sumInteger(productionAcActivePowerSum, productionDcActualPowerSum);
		setValue(this, Sum.ChannelId.PRODUCTION_ACTIVE_POWER, productionActivePower);
		// TODO calculate actual "Unmanaged"-ProductionActivePower
		setValue(this, Sum.ChannelId.UNMANAGED_PRODUCTION_ACTIVE_POWER, productionActivePower);

		final var productionAcActiveEnergySum = this.energyValuesHandler.setValue(//
				Sum.ChannelId.PRODUCTION_AC_ACTIVE_ENERGY, productionAcActiveEnergy.calculate());
		final var productionDcActiveEnergySum = this.energyValuesHandler.setValue(//
				Sum.ChannelId.PRODUCTION_DC_ACTIVE_ENERGY, productionDcActiveEnergy.calculate());
		this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_ACTIVE_ENERGY,
				sum(productionAcActiveEnergySum, productionDcActiveEnergySum));

		// Consumption
		final var consumptionActivePower = sumInteger(//
				essActivePowerSum, gridActivePowerSum, productionAcActivePowerSum);
		setValue(this, Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, consumptionActivePower);
		setValue(this, Sum.ChannelId.CONSUMPTION_ACTIVE_POWER_L1, sumInteger(//
				essActivePowerL1Sum, gridActivePowerL1Sum, productionAcActivePowerL1Sum));
		setValue(this, Sum.ChannelId.CONSUMPTION_ACTIVE_POWER_L2, sumInteger(//
				essActivePowerL2Sum, gridActivePowerL2Sum, productionAcActivePowerL2Sum));
		setValue(this, Sum.ChannelId.CONSUMPTION_ACTIVE_POWER_L3, sumInteger(//
				essActivePowerL3Sum, gridActivePowerL3Sum, productionAcActivePowerL3Sum));
		setValue(this, Sum.ChannelId.UNMANAGED_CONSUMPTION_ACTIVE_POWER,
				subtract(consumptionActivePower, managedConsumptionActivePower.calculate()));

		final var enterTheSystem = sum(essActiveDischargeEnergySum, gridBuyActiveEnergySum,
				productionAcActiveEnergySum);
		final var leaveTheSystem = sum(essActiveChargeEnergySum, gridSellActiveEnergySum,
				/* handling corner-case */ productionAcActiveEnergyNegative.calculate());
		final var consumptionActiveEnergy = Optional.ofNullable(enterTheSystem).orElse(0L)
				- Optional.ofNullable(leaveTheSystem).orElse(0L);
		this.energyValuesHandler.setValue(Sum.ChannelId.CONSUMPTION_ACTIVE_ENERGY, consumptionActiveEnergy);

		// Further calculated Channels
		final var essDischargePowerSum = essDcDischargePower.calculate();
		this.getEssDischargePowerChannel().setNextValue(essDischargePowerSum);

		this.updateExtremeEverValues();

		// Power & Energy distribution
		PowerDistribution.of(gridActivePowerSum, productionActivePower, essDischargePowerSum) //
				.updateChannels(this);
	}

	/**
	 * Combines the State of all Components.
	 */
	private void calculateState() {
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
