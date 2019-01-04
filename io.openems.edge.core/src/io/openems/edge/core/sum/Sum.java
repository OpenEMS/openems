package io.openems.edge.core.sum;

import java.util.List;
import java.util.Map;
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

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.channel.calculate.CalculateAverage;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.CalculateGridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.api.SymmetricEss.GridMode;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Enables access to sum/average data.
 */
@Component( //
		name = "Core.Sum", //
		immediate = true, //
		property = { //
				"id=_sum", //
				"enabled=true", //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		})
public class Sum extends AbstractOpenemsComponent implements OpenemsComponent, ModbusSlave, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Ess: Average State of Charge
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: Ess)
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		ESS_SOC(new Doc().type(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		/**
		 * Ess: Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricEss})
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ESS_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(ManagedSymmetricEss.POWER_DOC_TEXT)),
		/**
		 * Grid: Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		GRID_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(SymmetricMeter.POWER_DOC_TEXT)),
		/**
		 * Grid: Minimum Ever Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values or '0'
		 * </ul>
		 */
		GRID_MIN_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Grid: Maximum Ever Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		GRID_MAX_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: Meter Symmetric and ESS DC Charger)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: AC Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: Meter Symmetric)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_AC_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: DC Actual Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: ESS DC Charger)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_DC_ACTUAL_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: Maximum Ever Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		PRODUCTION_MAX_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: Maximum Ever AC Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		PRODUCTION_MAX_AC_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Production: Maximum Ever DC Actual Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link EssDcCharger}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		PRODUCTION_MAX_DC_ACTUAL_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Consumption: Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * <li>Note: the value is calculated using the data from Grid-Meter,
		 * Production-Meter and charge/discharge of battery.
		 * </ul>
		 */
		CONSUMPTION_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		/**
		 * Consumption: Maximum Ever Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: @see {@link SymmetricMeter}))
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values or '0'
		 * </ul>
		 */
		CONSUMPTION_MAX_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),

		/**
		 * Gridmode
		 * 
		 * <ul>
		 * <li>Interface: Gridmode (origin: @see {@link SymmetricEss}))
		 * <li>Type: Integer
		 * <li>Values: '0' = UNDEFINED, '1' = ON GRID, '2' = OFF GRID
		 * </ul>
		 */
		GRID_MODE(new Doc() //
				.type(OpenemsType.INTEGER).options(GridMode.values())),

		/**
		 * Max Apparent Power
		 * 
		 * <ul>
		 * <li>Interface: Max Apparent Power (origin: @see {@link SymmetricEss}))
		 * <li>Type: Integer
		 * <li>Unit: VA
		 * </ul>
		 */
		ESS_MAX_APPARENT_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable() {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(), //
				ModbusSlaveNatureTable.of(Sum.class, 220) //
						.channel(0, ChannelId.ESS_SOC, ModbusType.UINT16) //
						.channel(1, ChannelId.ESS_ACTIVE_POWER, ModbusType.FLOAT32) //
						.float32Reserved(3) // ChannelId.ESS_MIN_ACTIVE_POWER
						.float32Reserved(5) // ChannelId.ESS_MAX_ACTIVE_POWER
						.float32Reserved(7) // ChannelId.ESS_REACTIVE_POWER
						.float32Reserved(9) // ChannelId.ESS_MIN_REACTIVE_POWER
						.float32Reserved(11) // ChannelId.ESS_MAX_REACTIVE_POWER
						.channel(13, ChannelId.GRID_ACTIVE_POWER, ModbusType.FLOAT32) //
						.channel(15, ChannelId.GRID_MIN_ACTIVE_POWER, ModbusType.FLOAT32) //
						.channel(17, ChannelId.GRID_MAX_ACTIVE_POWER, ModbusType.FLOAT32) //
						.float32Reserved(19) // ChannelId.GRID_REACTIVE_POWER
						.float32Reserved(21) // ChannelId.GRID_MIN_REACTIVE_POWER
						.float32Reserved(23) // ChannelId.GRID_MAX_REACTIVE_POWER
						.channel(25, ChannelId.PRODUCTION_ACTIVE_POWER, ModbusType.FLOAT32) //
						.channel(27, ChannelId.PRODUCTION_MAX_ACTIVE_POWER, ModbusType.FLOAT32) //
						.channel(29, ChannelId.PRODUCTION_AC_ACTIVE_POWER, ModbusType.FLOAT32) //
						.channel(31, ChannelId.PRODUCTION_MAX_AC_ACTIVE_POWER, ModbusType.FLOAT32) //
						.float32Reserved(33) // ChannelId.PRODUCTION_AC_REACTIVE_POWER
						.float32Reserved(35) // ChannelId.PRODUCTION_MAX_AC_REACTIVE_POWER
						.channel(37, ChannelId.PRODUCTION_DC_ACTUAL_POWER, ModbusType.FLOAT32) //
						.channel(39, ChannelId.PRODUCTION_MAX_DC_ACTUAL_POWER, ModbusType.FLOAT32) //
						.channel(41, ChannelId.CONSUMPTION_ACTIVE_POWER, ModbusType.FLOAT32) //
						.channel(43, ChannelId.CONSUMPTION_MAX_ACTIVE_POWER, ModbusType.FLOAT32) //
						.float32Reserved(45) // ChannelId.CONSUMPTION_REACTIVE_POWER
						.float32Reserved(47) // ChannelId.CONSUMPTION_MAX_REACTIVE_POWER
						.build());
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(id=_sum)))")
	private volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	public Sum() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Map<String, Object> properties) {
		super.activate(context, "_sum", "_sum", true);
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

	public Channel<Integer> getEssSoc() {
		return this.channel(ChannelId.ESS_SOC);
	}

	public Channel<Integer> getEssActivePower() {
		return this.channel(ChannelId.ESS_ACTIVE_POWER);
	}

	public Channel<Integer> getEssMaxApparentPower() {
		return this.channel(ChannelId.ESS_MAX_APPARENT_POWER);
	}

	public Channel<Integer> getGridActivePower() {
		return this.channel(ChannelId.GRID_ACTIVE_POWER);
	}

	public Channel<Integer> getGridMinActivePower() {
		return this.channel(ChannelId.GRID_MIN_ACTIVE_POWER);
	}

	public Channel<Integer> getGridMaxActivePower() {
		return this.channel(ChannelId.GRID_MAX_ACTIVE_POWER);
	}

	public Channel<Integer> getProductionActivePower() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER);
	}

	public Channel<Integer> getProductionAcActivePower() {
		return this.channel(ChannelId.PRODUCTION_AC_ACTIVE_POWER);
	}

	public Channel<Integer> getProductionDcActualPower() {
		return this.channel(ChannelId.PRODUCTION_DC_ACTUAL_POWER);
	}

	public Channel<Integer> getProductionMaxActivePower() {
		return this.channel(ChannelId.PRODUCTION_MAX_ACTIVE_POWER);
	}

	public Channel<Integer> getProductionMaxAcActivePower() {
		return this.channel(ChannelId.PRODUCTION_MAX_AC_ACTIVE_POWER);
	}

	public Channel<Integer> getProductionMaxDcActualPower() {
		return this.channel(ChannelId.PRODUCTION_MAX_DC_ACTUAL_POWER);
	}

	public Channel<Integer> getConsumptionActivePower() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER);
	}

	public Channel<Integer> getConsumptionMaxActivePower() {
		return this.channel(ChannelId.CONSUMPTION_MAX_ACTIVE_POWER);
	}

	public Channel<Integer> getGridMode() {
		return this.channel(ChannelId.GRID_MODE);
	}

}
