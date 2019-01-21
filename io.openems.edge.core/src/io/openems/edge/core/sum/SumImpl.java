package io.openems.edge.core.sum;

import java.util.List;
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
import io.openems.edge.common.channel.calculate.CalculateAverage;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.doc.Level;
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

@Component( //
		name = "Core.Sum", //
		immediate = true, //
		property = { //
				"id=" + OpenemsConstants.SUM_ID, //
				"enabled=true", //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		})
public class SumImpl extends AbstractOpenemsComponent implements Sum, OpenemsComponent, ModbusSlave, EventHandler {

	@Override
	public ModbusSlaveTable getModbusSlaveTable() {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(), //
				Sum.getModbusSlaveNatureTable());
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(id=" + OpenemsConstants.SUM_ID + ")))")
	private volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	public SumImpl() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context) {
		super.activate(context, OpenemsConstants.SUM_ID, true);
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

		// Grid
		final CalculateIntegerSum gridActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum gridMinActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum gridMaxActivePower = new CalculateIntegerSum();

		// Production
		final CalculateIntegerSum productionAcActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum productionMaxAcActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum productionDcActualPower = new CalculateIntegerSum();
		final CalculateIntegerSum productionMaxDcActualPower = new CalculateIntegerSum();

		for (OpenemsComponent component : this.components) {
			if (component instanceof SymmetricEss) {
				/*
				 * Ess
				 */
				SymmetricEss ess = (SymmetricEss) component;

				if (ess instanceof MetaEss) {
					// ignore this Ess
					return;
				}
				essSoc.addValue(ess.getSoc());
				essActivePower.addValue(ess.getActivePower());
				essMaxApparentPower.addValue(ess.getMaxApparentPower());
				essGridMode.addValue(ess.getGridMode());

			} else if (component instanceof SymmetricMeter) {
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
					break;

				case PRODUCTION:
					/*
					 * Production-Meter
					 */
					productionAcActivePower.addValue(meter.getActivePower());
					productionMaxAcActivePower.addValue(meter.getMaxActivePower());
					break;

				}

			} else if (component instanceof EssDcCharger) {
				/*
				 * Ess DC-Charger
				 */
				EssDcCharger charger = (EssDcCharger) component;
				productionDcActualPower.addValue(charger.getActualPower());
				productionMaxDcActualPower.addValue(charger.getMaxActualPower());
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

		// Grid
		Integer gridActivePowerSum = gridActivePower.calculate();
		this.getGridActivePower().setNextValue(gridActivePowerSum);
		this.getGridMinActivePower().setNextValue(gridMinActivePower.calculate());
		Integer gridMaxActivePowerSum = gridMaxActivePower.calculate();
		this.getGridMaxActivePower().setNextValue(gridMaxActivePowerSum);

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

		// Consumption
		this.getConsumptionActivePower().setNextValue(TypeUtils.sum( //
				essActivePowerSum, gridActivePowerSum, productionAcActivePowerSum));
		this.getConsumptionMaxActivePower().setNextValue(TypeUtils.sum( //
				essMaxApparentPowerSum, gridMaxActivePowerSum, productionMaxAcActivePowerSum));
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
		Level state = this.getState().value().asEnum();
		Value<Integer> productionAc = this.getProductionAcActivePower().value();
		Value<Integer> productionDc = this.getProductionDcActualPower().value();
		String production;
		if (productionAc.asOptional().isPresent() && productionDc.asOptional().isPresent()) {
			production = " Production:" + this.getProductionActivePower().value().asString();
		} else {
			production = " Production Total:" + this.getProductionActivePower().value().asString() //
					+ ",AC:" + productionAc.asString() //
					+ ",DC:" + productionDc.asString(); //
		}
		return "State:" + state.getName() //
				+ " Ess SoC:" + this.getEssSoc().value().asString() //
				+ "|L:" + this.getEssActivePower().value().asString() //
				+ " Grid:" + this.getGridActivePower().value().asString() //
				+ production //
				+ " Consumption L:" + this.getConsumptionActivePower().value().asString(); //
	}

}
