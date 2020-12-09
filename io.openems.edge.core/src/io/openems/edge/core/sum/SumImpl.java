package io.openems.edge.core.sum;

import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

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
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.VirtualMeter;
import io.openems.edge.timedata.api.Timedata;

@Component(//
		name = "Core.Sum", //
		immediate = true, //
		property = { //
				"id=" + OpenemsConstants.SUM_ID, //
				"enabled=true" //
		})
public class SumImpl extends AbstractOpenemsComponent implements Sum, OpenemsComponent, ModbusSlave {

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
	public void updateChannelsBeforeProcessImage() {
		this.calculateChannelValues();
		this.calculateState();
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

		final CalculateIntegerSum essReactivePower = new CalculateIntegerSum();

		final CalculateIntegerSum essMaxApparentPower = new CalculateIntegerSum();
		final CalculateGridMode essGridMode = new CalculateGridMode();
		final CalculateLongSum essActiveChargeEnergy = new CalculateLongSum();
		final CalculateLongSum essActiveDischargeEnergy = new CalculateLongSum();
		final CalculateLongSum essDcChargeEnergy = new CalculateLongSum();
		final CalculateLongSum essDcDischargeEnergy = new CalculateLongSum();
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
				essSoc.addValue(ess.getSocChannel());
				essActivePower.addValue(ess.getActivePowerChannel());
				essReactivePower.addValue(ess.getReactivePowerChannel());
				essMaxApparentPower.addValue(ess.getMaxApparentPowerChannel());
				essGridMode.addValue(ess.getGridModeChannel());
				essActiveChargeEnergy.addValue(ess.getActiveChargeEnergyChannel());
				essActiveDischargeEnergy.addValue(ess.getActiveDischargeEnergyChannel());
				essCapacity.addValue(ess.getCapacityChannel());

				if (ess instanceof AsymmetricEss) {
					AsymmetricEss e = (AsymmetricEss) ess;
					essActivePowerL1.addValue(e.getActivePowerL1Channel());
					essActivePowerL2.addValue(e.getActivePowerL2Channel());
					essActivePowerL3.addValue(e.getActivePowerL3Channel());
				} else {
					essActivePowerL1.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
					essActivePowerL2.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
					essActivePowerL3.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
				}

				if (ess instanceof HybridEss) {
					HybridEss e = (HybridEss) ess;
					essDcChargeEnergy.addValue(e.getDcChargeEnergyChannel());
					essDcDischargeEnergy.addValue(e.getDcDischargeEnergyChannel());
				} else {
					essDcChargeEnergy.addValue(ess.getActiveChargeEnergyChannel());
					essDcDischargeEnergy.addValue(ess.getActiveDischargeEnergyChannel());
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
					gridActivePower.addValue(meter.getActivePowerChannel());
					gridMinActivePower.addValue(meter.getMinActivePowerChannel());
					gridMaxActivePower.addValue(meter.getMaxActivePowerChannel());
					gridBuyActiveEnergy.addValue(meter.getActiveProductionEnergyChannel());
					gridSellActiveEnergy.addValue(meter.getActiveConsumptionEnergyChannel());

					if (meter instanceof AsymmetricMeter) {
						AsymmetricMeter m = (AsymmetricMeter) meter;
						gridActivePowerL1.addValue(m.getActivePowerL1Channel());
						gridActivePowerL2.addValue(m.getActivePowerL2Channel());
						gridActivePowerL3.addValue(m.getActivePowerL3Channel());
					} else {
						gridActivePowerL1.addValue(meter.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
						gridActivePowerL2.addValue(meter.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
						gridActivePowerL3.addValue(meter.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
					}
					break;

				case PRODUCTION:
					/*
					 * Production-Meter
					 */
					productionAcActivePower.addValue(meter.getActivePowerChannel());
					productionMaxAcActivePower.addValue(meter.getMaxActivePowerChannel());
					productionAcActiveEnergy.addValue(meter.getActiveProductionEnergyChannel());
					productionAcActiveEnergyNegative.addValue(meter.getActiveConsumptionEnergyChannel());

					if (meter instanceof AsymmetricMeter) {
						AsymmetricMeter m = (AsymmetricMeter) meter;
						productionAcActivePowerL1.addValue(m.getActivePowerL1Channel());
						productionAcActivePowerL2.addValue(m.getActivePowerL2Channel());
						productionAcActivePowerL3.addValue(m.getActivePowerL3Channel());
					} else {
						productionAcActivePowerL1.addValue(meter.getActivePowerChannel(),
								CalculateIntegerSum.DIVIDE_BY_THREE);
						productionAcActivePowerL2.addValue(meter.getActivePowerChannel(),
								CalculateIntegerSum.DIVIDE_BY_THREE);
						productionAcActivePowerL3.addValue(meter.getActivePowerChannel(),
								CalculateIntegerSum.DIVIDE_BY_THREE);
					}
					break;

				}

			} else if (component instanceof EssDcCharger) {
				/*
				 * Ess DC-Charger
				 */
				EssDcCharger charger = (EssDcCharger) component;
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
		Integer essActivePowerSum = essActivePower.calculate();
		this._setEssActivePower(essActivePowerSum);
		Integer essActivePowerL1Sum = essActivePowerL1.calculate();
		this._setEssActivePowerL1(essActivePowerL1Sum);
		Integer essActivePowerL2Sum = essActivePowerL2.calculate();
		this._setEssActivePowerL2(essActivePowerL2Sum);
		Integer essActivePowerL3Sum = essActivePowerL3.calculate();
		this._setEssActivePowerL3(essActivePowerL3Sum);

		Integer essReactivePowerSum = essReactivePower.calculate();
		this._setEssReactivePower(essReactivePowerSum);

		Integer essMaxApparentPowerSum = essMaxApparentPower.calculate();
		this._setEssMaxApparentPower(essMaxApparentPowerSum);
		this._setGridMode(essGridMode.calculate());

		Long essActiveChargeEnergySum = essActiveChargeEnergy.calculate();
		essActiveChargeEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.ESS_ACTIVE_CHARGE_ENERGY,
				essActiveChargeEnergySum);
		Long essActiveDischargeEnergySum = essActiveDischargeEnergy.calculate();
		essActiveDischargeEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.ESS_ACTIVE_DISCHARGE_ENERGY,
				essActiveDischargeEnergySum);

		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_DC_CHARGE_ENERGY, essDcChargeEnergy.calculate());
		this.energyValuesHandler.setValue(Sum.ChannelId.ESS_DC_DISCHARGE_ENERGY, essDcDischargeEnergy.calculate());

		Integer essCapacitySum = essCapacity.calculate();
		this._setEssCapacity(essCapacitySum);

		// Grid
		Integer gridActivePowerSum = gridActivePower.calculate();
		this._setGridActivePower(gridActivePowerSum);
		Integer gridActivePowerL1Sum = gridActivePowerL1.calculate();
		this._setGridActivePowerL1(gridActivePowerL1Sum);
		Integer gridActivePowerL2Sum = gridActivePowerL2.calculate();
		this._setGridActivePowerL2(gridActivePowerL2Sum);
		Integer gridActivePowerL3Sum = gridActivePowerL3.calculate();
		this._setGridActivePowerL3(gridActivePowerL3Sum);
		this._setGridMinActivePower(gridMinActivePower.calculate());
		Integer gridMaxActivePowerSum = gridMaxActivePower.calculate();
		this._setGridMaxActivePower(gridMaxActivePowerSum);

		Long gridBuyActiveEnergySum = gridBuyActiveEnergy.calculate();
		gridBuyActiveEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.GRID_BUY_ACTIVE_ENERGY,
				gridBuyActiveEnergySum);
		Long gridSellActiveEnergySum = gridSellActiveEnergy.calculate();
		gridSellActiveEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.GRID_SELL_ACTIVE_ENERGY,
				gridSellActiveEnergySum);

		// Production
		Integer productionAcActivePowerSum = productionAcActivePower.calculate();
		this._setProductionAcActivePower(productionAcActivePowerSum);
		Integer productionAcActivePowerL1Sum = productionAcActivePowerL1.calculate();
		this._setProductionAcActivePowerL1(productionAcActivePowerL1Sum);
		Integer productionAcActivePowerL2Sum = productionAcActivePowerL2.calculate();
		this._setProductionAcActivePowerL2(productionAcActivePowerL2Sum);
		Integer productionAcActivePowerL3Sum = productionAcActivePowerL3.calculate();
		this._setProductionAcActivePowerL3(productionAcActivePowerL3Sum);
		Integer productionDcActualPowerSum = productionDcActualPower.calculate();
		this._setProductionDcActualPower(productionDcActualPowerSum);
		this._setProductionActivePower(TypeUtils.sum(productionAcActivePowerSum, productionDcActualPowerSum));

		Integer productionMaxAcActivePowerSum = productionMaxAcActivePower.calculate();
		this._setProductionMaxAcActivePower(productionMaxAcActivePowerSum);
		Integer productionMaxDcActualPowerSum = productionMaxDcActualPower.calculate();
		this._setProductionMaxDcActualPower(productionMaxDcActualPowerSum);
		this._setProductionMaxActivePower(TypeUtils.sum(productionMaxAcActivePowerSum, productionMaxDcActualPowerSum));

		Long productionAcActiveEnergySum = productionAcActiveEnergy.calculate();
		productionAcActiveEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_AC_ACTIVE_ENERGY,
				productionAcActiveEnergySum);
		Long productionDcActiveEnergySum = productionDcActiveEnergy.calculate();
		productionDcActiveEnergySum = this.energyValuesHandler.setValue(Sum.ChannelId.PRODUCTION_DC_ACTIVE_ENERGY,
				productionDcActiveEnergySum);
		Long productionActiveEnergySum = TypeUtils.sum(productionAcActiveEnergySum, productionDcActiveEnergySum);
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

		Long enterTheSystem = TypeUtils.sum(essActiveDischargeEnergySum, gridBuyActiveEnergySum,
				productionAcActiveEnergySum);
		Long leaveTheSystem = TypeUtils.sum(essActiveChargeEnergySum, gridSellActiveEnergySum,
				/* handling corner-case */ productionAcActiveEnergyNegative.calculate());
		this.energyValuesHandler.setValue(Sum.ChannelId.CONSUMPTION_ACTIVE_ENERGY,
				Optional.ofNullable(enterTheSystem).orElse(0L) - Optional.ofNullable(leaveTheSystem).orElse(0L));

		// Further calculated Channels
		this.getEssDischargePowerChannel()
				.setNextValue(TypeUtils.subtract(essActivePowerSum, productionDcActualPowerSum));
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
			Level level = component.getState();
			if (level.getValue() > highestLevel.getValue()) {
				highestLevel = level;
			}
		}
		this.getStateChannel().setNextValue(highestLevel);
	}

	@Override
	public String debugLog() {
		StringBuilder result = new StringBuilder();
		// State
		Level state = this.getState();
		result.append("State:" + state.getName() + " ");
		// Ess
		Value<Integer> essSoc = this.getEssSoc();
		Value<Integer> essActivePower = this.getEssActivePower();
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
		Value<Integer> gridActivePower = this.getGridActivePower();
		if (gridActivePower.isDefined()) {
			result.append("Grid:" + gridActivePower.asString() + " ");
		}
		// Production
		Value<Integer> production = this.getProductionActivePower();
		if (production.isDefined()) {
			result.append("Production");
			Value<Integer> productionAc = this.getProductionAcActivePower();
			Value<Integer> productionDc = this.getProductionDcActualPower();
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
		Value<Integer> consumptionActivePower = this.getConsumptionActivePower();
		if (consumptionActivePower.isDefined()) {
			result.append("Consumption:" + consumptionActivePower.asString() + " ");
		}
		// Remove last 'space' character and return result
		String resultString = result.toString();
		return resultString.substring(0, resultString.length() - 1);
	}
}
