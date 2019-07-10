package io.openems.edge.core.sum;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
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
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.CalculateGridMode;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.VirtualMeter;

@Component(//
		name = "Core.Sum", //
		immediate = true, //
		property = { //
				"id=" + OpenemsConstants.SUM_ID, //
				"enabled=true", //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		})
public class SumImpl extends AbstractOpenemsComponent implements Sum, OpenemsComponent, ModbusSlave, EventHandler {

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Sum.getModbusSlaveNatureTable(accessMode));
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(id=" + OpenemsConstants.SUM_ID + ")))")
	private volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	public SumImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Sum.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context) {
		super.activate(context, OpenemsConstants.SUM_ID, "Sum", true);
	}

	@Deactivate
	protected void deactivate() {
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
		final CalculateIntegerSum essMaxApparentPower = new CalculateIntegerSum();
		final CalculateGridMode essGridMode = new CalculateGridMode();
		final CalculateLongSum essActiveChargeEnergy = new CalculateLongSum();
		final CalculateLongSum essActiveDischargeEnergy = new CalculateLongSum();

		// Grid
		final CalculateIntegerSum gridActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum gridMinActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum gridMaxActivePower = new CalculateIntegerSum();
		final CalculateLongSum gridBuyActiveEnergy = new CalculateLongSum();
		final CalculateLongSum gridSellActiveEnergy = new CalculateLongSum();

		// Production
		final CalculateIntegerSum productionAcActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum productionMaxAcActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum productionDcActualPower = new CalculateIntegerSum();
		final CalculateIntegerSum productionMaxDcActualPower = new CalculateIntegerSum();
		final CalculateLongSum productionAcActiveEnergy = new CalculateLongSum();
		final CalculateLongSum productionDcActiveEnergy = new CalculateLongSum();
		// handling the corner-case of wrongly measured negative production, due to
		// cabling errors, etc.
		final CalculateLongSum productionAcActiveEnergyNegative = new CalculateLongSum();

		for (OpenemsComponent component : this.components) {
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
					break;

				case PRODUCTION:
					/*
					 * Production-Meter
					 */
					productionAcActivePower.addValue(meter.getActivePower());
					productionMaxAcActivePower.addValue(meter.getMaxActivePower());
					productionAcActiveEnergy.addValue(meter.getActiveProductionEnergy());
					productionAcActiveEnergyNegative.addValue(meter.getActiveConsumptionEnergy());
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
		Integer essMaxApparentPowerSum = essMaxApparentPower.calculate();
		this.getEssMaxApparentPower().setNextValue(essMaxApparentPowerSum);
		this.getGridMode().setNextValue(essGridMode.calculate());

		Long essActiveChargeEnergySum = essActiveChargeEnergy.calculate();
		this.getEssActiveChargeEnergy().setNextValue(essActiveChargeEnergySum);
		Long essActiveDischargeEnergySum = essActiveDischargeEnergy.calculate();
		this.getEssActiveDischargeEnergy().setNextValue(essActiveDischargeEnergySum);

		// Grid
		Integer gridActivePowerSum = gridActivePower.calculate();
		this.getGridActivePower().setNextValue(gridActivePowerSum);
		this.getGridMinActivePower().setNextValue(gridMinActivePower.calculate());
		Integer gridMaxActivePowerSum = gridMaxActivePower.calculate();
		this.getGridMaxActivePower().setNextValue(gridMaxActivePowerSum);

		Long gridBuyActiveEnergySum = gridBuyActiveEnergy.calculate();
		this.getGridBuyActiveEnergy().setNextValue(gridBuyActiveEnergySum);
		Long gridSellActiveEnergySum = gridSellActiveEnergy.calculate();
		this.getGridSellActiveEnergy().setNextValue(gridSellActiveEnergySum);

		// Production
		Integer productionAcActivePowerSum = productionAcActivePower.calculate();
		this.getProductionAcActivePower().setNextValue(productionAcActivePowerSum);
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
		this.getProductionAcActiveEnergy().setNextValue(productionAcActiveEnergySum);
		Long productionDcActiveEnergySum = productionDcActiveEnergy.calculate();
		this.getProductionDcActiveEnergy().setNextValue(productionDcActiveEnergySum);
		Long productionActiveEnergySum = TypeUtils.sum(productionAcActiveEnergySum, productionDcActiveEnergySum);
		this.getProductionActiveEnergy().setNextValue(productionActiveEnergySum);

		// Consumption
		this.getConsumptionActivePower().setNextValue(TypeUtils.sum(//
				essActivePowerSum, gridActivePowerSum, productionAcActivePowerSum));
		this.getConsumptionMaxActivePower().setNextValue(TypeUtils.sum(//
				essMaxApparentPowerSum, gridMaxActivePowerSum, productionMaxAcActivePowerSum));

		Long enterTheSystem = TypeUtils.sum(essActiveDischargeEnergySum, gridBuyActiveEnergySum,
				productionActiveEnergySum);
		Long leaveTheSystem = TypeUtils.sum(essActiveChargeEnergySum, gridSellActiveEnergySum,
				/* handling corner-case */ productionAcActiveEnergyNegative.calculate());
		this.getConsumptionActiveEnergy().setNextValue(//
				Optional.ofNullable(enterTheSystem).orElse(0L) - Optional.ofNullable(leaveTheSystem).orElse(0L));
	}

	/**
	 * Combines the State of all Components.
	 */
	private void calculateState() {
		Level highestLevel = Level.OK;
		for (OpenemsComponent component : this.components) {
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
		Value<Integer> productionAc = this.getProductionAcActivePower().value();
		Value<Integer> productionDc = this.getProductionDcActualPower().value();
		if (productionAc.isDefined() || productionDc.isDefined()) {
			result.append("Production ");
			if (productionAc.isDefined() && productionDc.isDefined()) {
				result.append(" Total:" + production.asString() //
						+ ",AC:" + productionAc.asString() //
						+ ",DC:" + productionDc.asString()); //
			} else if (productionAc.isDefined()) {
				result.append("AC:" + productionAc.asString());
			} else {
				result.append("DC:" + productionDc.asString());
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
