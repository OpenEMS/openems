package io.openems.edge.core.sum;

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
import io.openems.edge.common.channel.calculate.CalculateAverage;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.calculate.CalculateLongSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.CalculateGridMode;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.VirtualMeter;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = Sum.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"enabled=true" //
		})
public class SumImpl extends AbstractOpenemsComponent implements Sum, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	private final EnergyValuesHandler energyValuesHandler;
	private final Set<String> ignoreStateComponents = new HashSet<>();

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
		this.applyConfig(config);

		this.energyValuesHandler.activate();

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.applyConfig(config);

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	private synchronized void applyConfig(Config config) {
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

	/**
	 * Calculates the sum-value for each Channel.
	 */
	private void calculateChannelValues() {
		// Ess
		final var essSoc = new CalculateAverage();
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
		final var gridMinActivePower = new CalculateIntegerSum();
		final var gridMaxActivePower = new CalculateIntegerSum();
		final var gridBuyActiveEnergy = new CalculateLongSum();
		final var gridSellActiveEnergy = new CalculateLongSum();

		// Production
		final var productionAcActivePower = new CalculateIntegerSum();
		final var productionAcActivePowerL1 = new CalculateIntegerSum();
		final var productionAcActivePowerL2 = new CalculateIntegerSum();
		final var productionAcActivePowerL3 = new CalculateIntegerSum();
		final var productionMaxAcActivePower = new CalculateIntegerSum();
		final var productionDcActualPower = new CalculateIntegerSum();
		final var productionMaxDcActualPower = new CalculateIntegerSum();
		final var productionAcActiveEnergy = new CalculateLongSum();
		final var productionDcActiveEnergy = new CalculateLongSum();

		// handling the corner-case of wrongly measured negative production, due to
		// cabling errors, etc.
		final var productionAcActiveEnergyNegative = new CalculateLongSum();

		for (OpenemsComponent component : this.componentManager.getEnabledComponents()) {
			if (component instanceof SymmetricEss) {
				/*
				 * Ess
				 */
				var ess = (SymmetricEss) component;

				if (ess instanceof MetaEss) {
					// ignore this Ess
					continue;
				}
				essSoc.addValue(ess.getSocChannel());
				essActivePower.addValue(ess.getActivePowerChannel());
				essReactivePower.addValue(ess.getReactivePowerChannel());
				essMaxApparentPower.addValue(ess.getMaxApparentPowerChannel());
				essGridMode.addValue(ess.getGridModeChannel());
				essActiveChargeEnergy.addValue(ess.getActiveChargeEnergyChannel());
				essActiveDischargeEnergy.addValue(ess.getActiveDischargeEnergyChannel());
				essCapacity.addValue(ess.getCapacityChannel());

				if (ess instanceof AsymmetricEss) {
					var e = (AsymmetricEss) ess;
					essActivePowerL1.addValue(e.getActivePowerL1Channel());
					essActivePowerL2.addValue(e.getActivePowerL2Channel());
					essActivePowerL3.addValue(e.getActivePowerL3Channel());
				} else {
					essActivePowerL1.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
					essActivePowerL2.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
					essActivePowerL3.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
				}

				if (ess instanceof HybridEss) {
					var e = (HybridEss) ess;
					essDcChargeEnergy.addValue(e.getDcChargeEnergyChannel());
					essDcDischargeEnergy.addValue(e.getDcDischargeEnergyChannel());
					essDcDischargePower.addValue(e.getDcDischargePowerChannel());
				} else {
					essDcChargeEnergy.addValue(ess.getActiveChargeEnergyChannel());
					essDcDischargeEnergy.addValue(ess.getActiveDischargeEnergyChannel());
				}

			} else if (component instanceof ElectricityMeter meter) {
				if (component instanceof VirtualMeter) {
					if (!((VirtualMeter) component).addToSum()) {
						// Ignore VirtualMeter if "addToSum" is not activated (default)
						continue;
					}
				}

				/*
				 * Meter
				 */
				switch (meter.getMeterType()) {
				case PRODUCTION_AND_CONSUMPTION:
					// TODO PRODUCTION_AND_CONSUMPTION
					break;

				case CONSUMPTION_METERED:
					// TODO CONSUMPTION_METERED
					break;

				case CONSUMPTION_NOT_METERED:
					// TODO CONSUMPTION_NOT_METERED
					break;

				case GRID:
					/*
					 * Grid-Meter
					 */
					gridActivePower.addValue(meter.getActivePowerChannel());
					gridMinActivePower.addValue(meter.getMinActivePowerChannel());
					gridMaxActivePower.addValue(meter.getMaxActivePowerChannel());
					gridBuyActiveEnergy.addValue(meter.getActiveProductionEnergyChannel());
					gridSellActiveEnergy.addValue(meter.getActiveConsumptionEnergyChannel());
					gridActivePowerL1.addValue(meter.getActivePowerL1Channel());
					gridActivePowerL2.addValue(meter.getActivePowerL2Channel());
					gridActivePowerL3.addValue(meter.getActivePowerL3Channel());
					break;

				case PRODUCTION:
					/*
					 * Production-Meter
					 */
					productionAcActivePower.addValue(meter.getActivePowerChannel());
					productionMaxAcActivePower.addValue(meter.getMaxActivePowerChannel());
					productionAcActiveEnergy.addValue(meter.getActiveProductionEnergyChannel());
					productionAcActiveEnergyNegative.addValue(meter.getActiveConsumptionEnergyChannel());
					productionAcActivePowerL1.addValue(meter.getActivePowerL1Channel());
					productionAcActivePowerL2.addValue(meter.getActivePowerL2Channel());
					productionAcActivePowerL3.addValue(meter.getActivePowerL3Channel());
					break;

				}

			} else if (component instanceof EssDcCharger) {
				/*
				 * Ess DC-Charger
				 */
				var charger = (EssDcCharger) component;
				productionDcActualPower.addValue(charger.getActualPowerChannel());
				productionMaxDcActualPower.addValue(charger.getMaxActualPowerChannel());
				productionDcActiveEnergy.addValue(charger.getActualEnergyChannel());
			}
		}

		/*
		 * Set values
		 */
		// Ess
		this.getEssSocChannel().setNextValue(essSoc.calculate());
		var essActivePowerSum = essActivePower.calculate();
		this._setEssActivePower(essActivePowerSum);
		var essActivePowerL1Sum = essActivePowerL1.calculate();
		this._setEssActivePowerL1(essActivePowerL1Sum);
		var essActivePowerL2Sum = essActivePowerL2.calculate();
		this._setEssActivePowerL2(essActivePowerL2Sum);
		var essActivePowerL3Sum = essActivePowerL3.calculate();
		this._setEssActivePowerL3(essActivePowerL3Sum);

		var essReactivePowerSum = essReactivePower.calculate();
		this._setEssReactivePower(essReactivePowerSum);

		var essMaxApparentPowerSum = essMaxApparentPower.calculate();
		this._setEssMaxApparentPower(essMaxApparentPowerSum);
		this._setGridMode(essGridMode.calculate());

		var essActiveChargeEnergySum = essActiveChargeEnergy.calculate();
		essActiveChargeEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.ESS_ACTIVE_CHARGE_ENERGY,
				essActiveChargeEnergySum);
		var essActiveDischargeEnergySum = essActiveDischargeEnergy.calculate();
		essActiveDischargeEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.ESS_ACTIVE_DISCHARGE_ENERGY,
				essActiveDischargeEnergySum);

		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_DC_CHARGE_ENERGY, essDcChargeEnergy.calculate());
		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_DC_DISCHARGE_ENERGY, essDcDischargeEnergy.calculate());

		var essCapacitySum = essCapacity.calculate();
		this._setEssCapacity(essCapacitySum);

		// Grid
		var gridActivePowerSum = gridActivePower.calculate();
		this._setGridActivePower(gridActivePowerSum);
		var gridActivePowerL1Sum = gridActivePowerL1.calculate();
		this._setGridActivePowerL1(gridActivePowerL1Sum);
		var gridActivePowerL2Sum = gridActivePowerL2.calculate();
		this._setGridActivePowerL2(gridActivePowerL2Sum);
		var gridActivePowerL3Sum = gridActivePowerL3.calculate();
		this._setGridActivePowerL3(gridActivePowerL3Sum);
		this._setGridMinActivePower(gridMinActivePower.calculate());
		var gridMaxActivePowerSum = gridMaxActivePower.calculate();
		this._setGridMaxActivePower(gridMaxActivePowerSum);

		var gridBuyActiveEnergySum = gridBuyActiveEnergy.calculate();
		gridBuyActiveEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.GRID_BUY_ACTIVE_ENERGY,
				gridBuyActiveEnergySum);
		var gridSellActiveEnergySum = gridSellActiveEnergy.calculate();
		gridSellActiveEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.GRID_SELL_ACTIVE_ENERGY,
				gridSellActiveEnergySum);

		// Production
		var productionAcActivePowerSum = productionAcActivePower.calculate();
		this._setProductionAcActivePower(productionAcActivePowerSum);
		var productionAcActivePowerL1Sum = productionAcActivePowerL1.calculate();
		this._setProductionAcActivePowerL1(productionAcActivePowerL1Sum);
		var productionAcActivePowerL2Sum = productionAcActivePowerL2.calculate();
		this._setProductionAcActivePowerL2(productionAcActivePowerL2Sum);
		var productionAcActivePowerL3Sum = productionAcActivePowerL3.calculate();
		this._setProductionAcActivePowerL3(productionAcActivePowerL3Sum);
		var productionDcActualPowerSum = productionDcActualPower.calculate();
		this._setProductionDcActualPower(productionDcActualPowerSum);
		this._setProductionActivePower(TypeUtils.sum(productionAcActivePowerSum, productionDcActualPowerSum));

		var productionMaxAcActivePowerSum = productionMaxAcActivePower.calculate();
		this._setProductionMaxAcActivePower(productionMaxAcActivePowerSum);
		var productionMaxDcActualPowerSum = productionMaxDcActualPower.calculate();
		this._setProductionMaxDcActualPower(productionMaxDcActualPowerSum);
		this._setProductionMaxActivePower(TypeUtils.sum(productionMaxAcActivePowerSum, productionMaxDcActualPowerSum));

		var productionAcActiveEnergySum = productionAcActiveEnergy.calculate();
		productionAcActiveEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_AC_ACTIVE_ENERGY,
				productionAcActiveEnergySum);
		var productionDcActiveEnergySum = productionDcActiveEnergy.calculate();
		productionDcActiveEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_DC_ACTIVE_ENERGY,
				productionDcActiveEnergySum);
		var productionActiveEnergySum = TypeUtils.sum(productionAcActiveEnergySum, productionDcActiveEnergySum);
		productionActiveEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_ACTIVE_ENERGY,
				productionActiveEnergySum);

		// Consumption
		this._setConsumptionActivePower(TypeUtils.sum(//
				essActivePowerSum, gridActivePowerSum, productionAcActivePowerSum));
		this._setConsumptionActivePowerL1(TypeUtils.sum(//
				essActivePowerL1Sum, gridActivePowerL1Sum, productionAcActivePowerL1Sum));
		this._setConsumptionActivePowerL2(TypeUtils.sum(//
				essActivePowerL2Sum, gridActivePowerL2Sum, productionAcActivePowerL2Sum));
		this._setConsumptionActivePowerL3(TypeUtils.sum(//
				essActivePowerL3Sum, gridActivePowerL3Sum, productionAcActivePowerL3Sum));
		this._setConsumptionMaxActivePower(TypeUtils.sum(//
				essMaxApparentPowerSum, gridMaxActivePowerSum, productionMaxAcActivePowerSum));

		var enterTheSystem = TypeUtils.sum(essActiveDischargeEnergySum, gridBuyActiveEnergySum,
				productionAcActiveEnergySum);
		var leaveTheSystem = TypeUtils.sum(essActiveChargeEnergySum, gridSellActiveEnergySum,
				/* handling corner-case */ productionAcActiveEnergyNegative.calculate());
		this.energyValuesHandler.setValue(Sum.ChannelId.CONSUMPTION_ACTIVE_ENERGY,
				Optional.ofNullable(enterTheSystem).orElse(0L) - Optional.ofNullable(leaveTheSystem).orElse(0L));

		// Further calculated Channels
		var essDischargePowerSum = essDcDischargePower.calculate();
		this.getEssDischargePowerChannel().setNextValue(essDischargePowerSum);
	}

	/**
	 * Combines the State of all Components.
	 */
	private void calculateState() {
		var highestLevel = Level.OK;
		var hasIgnoredComponentStates = false;
		for (OpenemsComponent component : this.componentManager.getEnabledComponents()) {
			if (component == this) {
				// ignore myself
				continue;
			}
			var level = component.getState();
			if (this.ignoreStateComponents.contains(component.id()) && level != Level.OK) {
				// This Components State should be ignored
				hasIgnoredComponentStates = true;

			} else {
				this._setHasIgnoredComponentStates(false);
				// Calculate highest State Level
				if (level.getValue() > highestLevel.getValue()) {
					highestLevel = level;
				}
			}
		}

		// There is at least one ignored State -> show info
		if (hasIgnoredComponentStates) {
			this._setHasIgnoredComponentStates(true);
			// Note: this sets the StateChannel 'HAS_IGNORED_COMPONENT_STATES' to true,
			// which sets the Sum 'STATE'-Channel to 'INFO'. We override this below with
			// 'highestLevel'.
			if (Level.INFO.getValue() > highestLevel.getValue()) {
				highestLevel = Level.INFO;
			}
		}

		this.getStateChannel().setNextValue(highestLevel);
	}

	@Override
	public String debugLog() {
		var result = new StringBuilder();
		// State
		var state = this.getState();
		result.append("State:" + state.getName() + " ");
		// Ess
		var essSoc = this.getEssSoc();
		var essActivePower = this.getEssActivePower();
		if (essSoc.isDefined() || essActivePower.isDefined()) {
			result.append("Ess ");
			if (essSoc.isDefined() && essActivePower.isDefined()) {
				result.append("SoC:" + essSoc.asString() + "|L:" + essActivePower.asString());
			} else if (essSoc.isDefined()) {
				result.append("SoC:" + essSoc.asString());
			} else {
				result.append("L:" + essActivePower.asString());
			}
			result.append(" ");
		}
		// Grid
		var gridActivePower = this.getGridActivePower();
		if (gridActivePower.isDefined()) {
			result.append("Grid:" + gridActivePower.asString() + " ");
		}
		// Production
		var production = this.getProductionActivePower();
		if (production.isDefined()) {
			result.append("Production");
			var productionAc = this.getProductionAcActivePower();
			var productionDc = this.getProductionDcActualPower();
			if (productionAc.isDefined() && productionDc.isDefined()) {
				result.append(" Total:" + production.asString() //
						+ ",AC:" + productionAc.asString() //
						+ ",DC:" + productionDc.asString()); //
			} else {
				result.append(":");
				result.append(production.asString());
			}
			result.append(" ");
		}
		// Consumption
		var consumptionActivePower = this.getConsumptionActivePower();
		if (consumptionActivePower.isDefined()) {
			result.append("Consumption:" + consumptionActivePower.asString() + " ");
		}
		// Remove last 'space' character and return result
		var resultString = result.toString();
		return resultString.substring(0, resultString.length() - 1);
	}
}
