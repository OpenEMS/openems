package io.openems.edge.core.sum;

import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.edge.common.channel.calculate.CalculateAverage;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.calculate.CalculateLongSum;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.CalculateGridMode;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.VirtualMeter;
import io.openems.edge.timedata.api.Timedata;

@Component(//
		name = "Core.Sum", //
		immediate = true, //
		property = { //
				"id=" + OpenemsConstants.SUM_ID, //
				"enabled=true", //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		})
public class SumImpl extends AbstractOpenemsComponent implements Sum, OpenemsComponent, ModbusSlave, EventHandler {

	@Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	private final EnergyValuesHandler energyValuesHandler;

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
	void activate(ComponentContext context) {
		super.activate(context, OpenemsConstants.SUM_ID, "Sum", true);
		this.energyValuesHandler.activate();
	}

	@Deactivate
	protected void deactivate() {
		this.energyValuesHandler.deactivate();
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.calculateChannelValues();
			this.calculateState();
		}
	}

	/**
	 * Calculates the sum-value for each Channel.
	 */
	private void calculateChannelValues() {
		// Ess
		final CalculateAverage essSoc = new CalculateAverage();
		final CalculateIntegerSum essActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum essActivePowerL1 = new CalculateIntegerSum();
		final CalculateIntegerSum essActivePowerL2 = new CalculateIntegerSum();
		final CalculateIntegerSum essActivePowerL3 = new CalculateIntegerSum();
		final CalculateIntegerSum essMaxApparentPower = new CalculateIntegerSum();
		final CalculateGridMode essGridMode = new CalculateGridMode();
		final CalculateLongSum essActiveChargeEnergy = new CalculateLongSum();
		final CalculateLongSum essActiveDischargeEnergy = new CalculateLongSum();
		final CalculateIntegerSum essCapacity = new CalculateIntegerSum();

		// Grid
		final CalculateIntegerSum gridActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum gridActivePowerL1 = new CalculateIntegerSum();
		final CalculateIntegerSum gridActivePowerL2 = new CalculateIntegerSum();
		final CalculateIntegerSum gridActivePowerL3 = new CalculateIntegerSum();
		final CalculateIntegerSum gridMinActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum gridMaxActivePower = new CalculateIntegerSum();
		final CalculateLongSum gridBuyActiveEnergy = new CalculateLongSum();
		final CalculateLongSum gridSellActiveEnergy = new CalculateLongSum();

		// Production
		final CalculateIntegerSum productionAcActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum productionAcActivePowerL1 = new CalculateIntegerSum();
		final CalculateIntegerSum productionAcActivePowerL2 = new CalculateIntegerSum();
		final CalculateIntegerSum productionAcActivePowerL3 = new CalculateIntegerSum();
		final CalculateIntegerSum productionMaxAcActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum productionDcActualPower = new CalculateIntegerSum();
		final CalculateIntegerSum productionMaxDcActualPower = new CalculateIntegerSum();
		final CalculateLongSum productionAcActiveEnergy = new CalculateLongSum();
		final CalculateLongSum productionDcActiveEnergy = new CalculateLongSum();
		// handling the corner-case of wrongly measured negative production, due to
		// cabling errors, etc.
		final CalculateLongSum productionAcActiveEnergyNegative = new CalculateLongSum();

		for (OpenemsComponent component : this.componentManager.getEnabledComponents()) {
			if (component instanceof SymmetricEss) {
				/*
				 * Ess
				 */
				SymmetricEss ess = (SymmetricEss) component;

				if (ess instanceof MetaEss) {
					// ignore this Ess
					continue;
				}
				essSoc.addValue(ess.getSoc());
				essActivePower.addValue(ess.getActivePower());
				essMaxApparentPower.addValue(ess.getMaxApparentPower());
				essGridMode.addValue(ess.getGridMode());
				essActiveChargeEnergy.addValue(ess.getActiveChargeEnergy());
				essActiveDischargeEnergy.addValue(ess.getActiveDischargeEnergy());
				essCapacity.addValue(ess.getCapacity());

				if (ess instanceof AsymmetricEss) {
					AsymmetricEss e = (AsymmetricEss) ess;
					essActivePowerL1.addValue(e.getActivePowerL1());
					essActivePowerL2.addValue(e.getActivePowerL2());
					essActivePowerL3.addValue(e.getActivePowerL3());
				} else {
					essActivePowerL1.addValue(ess.getActivePower(), CalculateIntegerSum.DIVIDE_BY_THREE);
					essActivePowerL2.addValue(ess.getActivePower(), CalculateIntegerSum.DIVIDE_BY_THREE);
					essActivePowerL3.addValue(ess.getActivePower(), CalculateIntegerSum.DIVIDE_BY_THREE);
				}

			} else if (component instanceof SymmetricMeter) {
				if (component instanceof VirtualMeter) {
					if (!((VirtualMeter) component).addToSum()) {
						// Ignore VirtualMeter if "addToSum" is not activated (default)
						continue;
					}
				}

				/*
				 * Meter
				 */
				SymmetricMeter meter = (SymmetricMeter) component;
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
					gridActivePower.addValue(meter.getActivePower());
					gridMinActivePower.addValue(meter.getMinActivePower());
					gridMaxActivePower.addValue(meter.getMaxActivePower());
					gridBuyActiveEnergy.addValue(meter.getActiveProductionEnergy());
					gridSellActiveEnergy.addValue(meter.getActiveConsumptionEnergy());

					if (meter instanceof AsymmetricMeter) {
						AsymmetricMeter m = (AsymmetricMeter) meter;
						gridActivePowerL1.addValue(m.getActivePowerL1());
						gridActivePowerL2.addValue(m.getActivePowerL2());
						gridActivePowerL3.addValue(m.getActivePowerL3());
					} else {
						gridActivePowerL1.addValue(meter.getActivePower(), CalculateIntegerSum.DIVIDE_BY_THREE);
						gridActivePowerL2.addValue(meter.getActivePower(), CalculateIntegerSum.DIVIDE_BY_THREE);
						gridActivePowerL3.addValue(meter.getActivePower(), CalculateIntegerSum.DIVIDE_BY_THREE);
					}
					break;

				case PRODUCTION:
					/*
					 * Production-Meter
					 */
					productionAcActivePower.addValue(meter.getActivePower());
					productionMaxAcActivePower.addValue(meter.getMaxActivePower());
					productionAcActiveEnergy.addValue(meter.getActiveProductionEnergy());
					productionAcActiveEnergyNegative.addValue(meter.getActiveConsumptionEnergy());

					if (meter instanceof AsymmetricMeter) {
						AsymmetricMeter m = (AsymmetricMeter) meter;
						productionAcActivePowerL1.addValue(m.getActivePowerL1());
						productionAcActivePowerL2.addValue(m.getActivePowerL2());
						productionAcActivePowerL3.addValue(m.getActivePowerL3());
					} else {
						productionAcActivePowerL1.addValue(meter.getActivePower(), CalculateIntegerSum.DIVIDE_BY_THREE);
						productionAcActivePowerL2.addValue(meter.getActivePower(), CalculateIntegerSum.DIVIDE_BY_THREE);
						productionAcActivePowerL3.addValue(meter.getActivePower(), CalculateIntegerSum.DIVIDE_BY_THREE);
					}
					break;

				}

			} else if (component instanceof EssDcCharger) {
				/*
				 * Ess DC-Charger
				 */
				EssDcCharger charger = (EssDcCharger) component;
				productionDcActualPower.addValue(charger.getActualPower());
				productionMaxDcActualPower.addValue(charger.getMaxActualPower());
				productionDcActiveEnergy.addValue(charger.getActualEnergy());
			}
		}

		/*
		 * Set values
		 */
		// Ess
		this.getEssSoc().setNextValue(essSoc.calculate());
		Integer essActivePowerSum = essActivePower.calculate();
		this.getEssActivePower().setNextValue(essActivePowerSum);
		Integer essActivePowerL1Sum = essActivePowerL1.calculate();
		this.getEssActivePowerL1().setNextValue(essActivePowerL1Sum);
		Integer essActivePowerL2Sum = essActivePowerL2.calculate();
		this.getEssActivePowerL2().setNextValue(essActivePowerL2Sum);
		Integer essActivePowerL3Sum = essActivePowerL3.calculate();
		this.getEssActivePowerL3().setNextValue(essActivePowerL3Sum);
		Integer essMaxApparentPowerSum = essMaxApparentPower.calculate();
		this.getEssMaxApparentPower().setNextValue(essMaxApparentPowerSum);
		this.getGridMode().setNextValue(essGridMode.calculate());

		Long essActiveChargeEnergySum = essActiveChargeEnergy.calculate();
		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_ACTIVE_CHARGE_ENERGY, essActiveChargeEnergySum);
		Long essActiveDischargeEnergySum = essActiveDischargeEnergy.calculate();
		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_ACTIVE_DISCHARGE_ENERGY, essActiveDischargeEnergySum);

		Integer essCapacitySum = essCapacity.calculate();
		this.getEssCapacity().setNextValue(essCapacitySum);

		// Grid
		Integer gridActivePowerSum = gridActivePower.calculate();
		this.getGridActivePower().setNextValue(gridActivePowerSum);
		Integer gridActivePowerL1Sum = gridActivePowerL1.calculate();
		this.getGridActivePowerL1().setNextValue(gridActivePowerL1Sum);
		Integer gridActivePowerL2Sum = gridActivePowerL2.calculate();
		this.getGridActivePowerL2().setNextValue(gridActivePowerL2Sum);
		Integer gridActivePowerL3Sum = gridActivePowerL3.calculate();
		this.getGridActivePowerL3().setNextValue(gridActivePowerL3Sum);
		this.getGridMinActivePower().setNextValue(gridMinActivePower.calculate());
		Integer gridMaxActivePowerSum = gridMaxActivePower.calculate();
		this.getGridMaxActivePower().setNextValue(gridMaxActivePowerSum);

		Long gridBuyActiveEnergySum = gridBuyActiveEnergy.calculate();
		this.energyValuesHandler.setValue(Sum.ChannelId.GRID_BUY_ACTIVE_ENERGY, gridBuyActiveEnergySum);
		Long gridSellActiveEnergySum = gridSellActiveEnergy.calculate();
		this.energyValuesHandler.setValue(Sum.ChannelId.GRID_SELL_ACTIVE_ENERGY, gridSellActiveEnergySum);

		// Production
		Integer productionAcActivePowerSum = productionAcActivePower.calculate();
		this.getProductionAcActivePower().setNextValue(productionAcActivePowerSum);
		Integer productionAcActivePowerL1Sum = productionAcActivePowerL1.calculate();
		this.getProductionAcActivePowerL1().setNextValue(productionAcActivePowerL1Sum);
		Integer productionAcActivePowerL2Sum = productionAcActivePowerL2.calculate();
		this.getProductionAcActivePowerL2().setNextValue(productionAcActivePowerL2Sum);
		Integer productionAcActivePowerL3Sum = productionAcActivePowerL3.calculate();
		this.getProductionAcActivePowerL3().setNextValue(productionAcActivePowerL3Sum);
		Integer productionDcActualPowerSum = productionDcActualPower.calculate();
		this.getProductionDcActualPower().setNextValue(productionDcActualPowerSum);
		this.getProductionActivePower()
				.setNextValue(TypeUtils.sum(productionAcActivePowerSum, productionDcActualPowerSum));

		Integer productionMaxAcActivePowerSum = productionMaxAcActivePower.calculate();
		this.getProductionMaxAcActivePower().setNextValue(productionMaxAcActivePowerSum);
		Integer productionMaxDcActualPowerSum = productionMaxDcActualPower.calculate();
		this.getProductionMaxDcActualPower().setNextValue(productionMaxDcActualPowerSum);
		this.getProductionMaxActivePower()
				.setNextValue(TypeUtils.sum(productionMaxAcActivePowerSum, productionMaxDcActualPowerSum));

		Long productionAcActiveEnergySum = productionAcActiveEnergy.calculate();
		this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_AC_ACTIVE_ENERGY, productionAcActiveEnergySum);
		Long productionDcActiveEnergySum = productionDcActiveEnergy.calculate();
		this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_DC_ACTIVE_ENERGY, productionDcActiveEnergySum);
		Long productionActiveEnergySum = TypeUtils.sum(productionAcActiveEnergySum, productionDcActiveEnergySum);
		this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_ACTIVE_ENERGY, productionActiveEnergySum);

		// Consumption
		this.getConsumptionActivePower().setNextValue(TypeUtils.sum(//
				essActivePowerSum, gridActivePowerSum, productionAcActivePowerSum));
		this.getConsumptionActivePowerL1().setNextValue(TypeUtils.sum(//
				essActivePowerL1Sum, gridActivePowerL1Sum, productionAcActivePowerL1Sum));
		this.getConsumptionActivePowerL2().setNextValue(TypeUtils.sum(//
				essActivePowerL2Sum, gridActivePowerL2Sum, productionAcActivePowerL2Sum));
		this.getConsumptionActivePowerL3().setNextValue(TypeUtils.sum(//
				essActivePowerL3Sum, gridActivePowerL3Sum, productionAcActivePowerL3Sum));
		this.getConsumptionMaxActivePower().setNextValue(TypeUtils.sum(//
				essMaxApparentPowerSum, gridMaxActivePowerSum, productionMaxAcActivePowerSum));

		Long enterTheSystem = TypeUtils.sum(essActiveDischargeEnergySum, gridBuyActiveEnergySum,
				productionActiveEnergySum);
		Long leaveTheSystem = TypeUtils.sum(essActiveChargeEnergySum, gridSellActiveEnergySum,
				/* handling corner-case */ productionAcActiveEnergyNegative.calculate());
		this.energyValuesHandler.setValue(Sum.ChannelId.CONSUMPTION_ACTIVE_ENERGY,
				Optional.ofNullable(enterTheSystem).orElse(0L) - Optional.ofNullable(leaveTheSystem).orElse(0L));
	}

	/**
	 * Combines the State of all Components.
	 */
	private void calculateState() {
		Level highestLevel = Level.OK;
		for (OpenemsComponent component : this.componentManager.getEnabledComponents()) {
			if (component == this) {
				// ignore myself
				continue;
			}
			Level level = component.getState().getNextValue().asEnum();
			if (level.getValue() > highestLevel.getValue()) {
				highestLevel = level;
			}
		}
		this.getState().setNextValue(highestLevel);
	}

	@Override
	public String debugLog() {
		StringBuilder result = new StringBuilder();
		// State
		Level state = this.getState().value().asEnum();
		result.append("State:" + state.getName() + " ");
		// Ess
		Value<Integer> essSoc = this.getEssSoc().value();
		Value<Integer> essActivePower = this.getEssActivePower().value();
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
		Value<Integer> gridActivePower = this.getGridActivePower().value();
		if (gridActivePower.isDefined()) {
			result.append("Grid:" + gridActivePower.asString() + " ");
		}
		// Production
		Value<Integer> production = this.getProductionActivePower().value();
		if (production.isDefined()) {
			result.append("Production");
			Value<Integer> productionAc = this.getProductionAcActivePower().value();
			Value<Integer> productionDc = this.getProductionDcActualPower().value();
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
		Value<Integer> consumptionActivePower = this.getConsumptionActivePower().value();
		if (consumptionActivePower.isDefined()) {
			result.append("Consumption:" + consumptionActivePower.asString() + " ");
		}
		// Remove last 'space' character and return result
		String resultString = result.toString();
		return resultString.substring(0, resultString.length() - 1);
	}

}
