package io.openems.edge.core.sum;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.channel.merger.AverageInteger;
import io.openems.edge.common.channel.merger.ChannelMergerSumInteger;
import io.openems.edge.common.channel.merger.SumInteger;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Enables access to sum/average data.
 */
@Component(name = "Core.Sum", immediate = true, property = { "id=_sum", "enabled=true" })
public class Sum extends AbstractOpenemsComponent implements OpenemsComponent {

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
		 * <li>Interface: Sum (origin: @see {@link SymmetricEssReadonly})
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
				.unit(Unit.WATT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/*
	 * Ess
	 */
	private final List<SymmetricEss> esss = new CopyOnWriteArrayList<>();
	private final AverageInteger<SymmetricEss> essSoc;
	private final SumInteger<SymmetricEss> essActivePower;

	/*
	 * Grid
	 */
	private final SumInteger<SymmetricMeter> gridActivePower;
	private final SumInteger<SymmetricMeter> gridMinActivePower;
	private final SumInteger<SymmetricMeter> gridMaxActivePower;

	/*
	 * Production
	 */
	private final SumInteger<SymmetricMeter> productionAcActivePower;
	private final SumInteger<SymmetricMeter> productionMaxAcActivePower;
	private final SumInteger<EssDcCharger> productionDcActualPower;
	private final SumInteger<EssDcCharger> productionMaxDcActualPower;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private void addEss(SymmetricEss ess) {
		if (ess instanceof MetaEss) {
			// ignore this Ess
			return;
		}
		this.esss.add(ess);
		this.essSoc.addComponent(ess);
		this.essActivePower.addComponent(ess);
		this.calculateMaxConsumption.accept(null /* ignored */);
	}

	protected void removeEss(SymmetricEss ess) {
		if (ess instanceof MetaEss) {
			// ignore this Ess
			return;
		}
		this.esss.remove(ess);
		this.essSoc.removeComponent(ess);
		this.essActivePower.removeComponent(ess);
	}

	private final Consumer<Value<Integer>> calculateMaxConsumption = ignoreValue -> {
		int ess = 0;
		for (SymmetricEss e : this.esss) {
			ess += e.getMaxApparentPower().getNextValue().orElse(0);
		}
		int grid = this.getGridMaxActivePower().getNextValue().orElse(0);
		int production = this.getProductionMaxActivePower().getNextValue().orElse(0);
		int consumption = ess + grid + production;
		this.getConsumptionMaxActivePower().setNextValue(consumption);
	};

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private void addMeter(SymmetricMeter meter) {
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
			if (meter instanceof SymmetricMeter) {
				this.gridActivePower.addComponent((SymmetricMeter) meter);
				this.gridMinActivePower.addComponent((SymmetricMeter) meter);
				this.gridMaxActivePower.addComponent((SymmetricMeter) meter);
			}
			break;

		case PRODUCTION:
			/*
			 * Production-Meter
			 */
			if (meter instanceof SymmetricMeter) {
				this.productionAcActivePower.addComponent((SymmetricMeter) meter);
				this.productionMaxAcActivePower.addComponent((SymmetricMeter) meter);
			}
			break;

		}
	}

	protected void removeMeter(SymmetricMeter meter) {
		this.gridActivePower.removeComponent(meter);
		this.gridMinActivePower.removeComponent(meter);
		this.gridMaxActivePower.removeComponent(meter);
		this.productionMaxAcActivePower.removeComponent(meter);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private void addEssDcCharger(EssDcCharger charger) {
		this.productionDcActualPower.addComponent(charger);
		this.productionMaxDcActualPower.addComponent(charger);
	}

	protected void removeEssDcCharger(EssDcCharger charger) {
		this.productionDcActualPower.removeComponent(charger);
		this.productionMaxDcActualPower.removeComponent(charger);
	}

	@SuppressWarnings("unchecked")
	public Sum() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		/*
		 * Ess
		 */
		this.essSoc = new AverageInteger<SymmetricEss>(this, ChannelId.ESS_SOC, SymmetricEss.ChannelId.SOC);
		this.essActivePower = new SumInteger<SymmetricEss>(this, ChannelId.ESS_ACTIVE_POWER,
				SymmetricEss.ChannelId.ACTIVE_POWER);
		/*
		 * Grid
		 */
		this.gridActivePower = new SumInteger<SymmetricMeter>(this, ChannelId.GRID_ACTIVE_POWER,
				SymmetricMeter.ChannelId.ACTIVE_POWER);
		this.gridMinActivePower = new SumInteger<SymmetricMeter>(this, ChannelId.GRID_MIN_ACTIVE_POWER,
				SymmetricMeter.ChannelId.MIN_ACTIVE_POWER);
		this.gridMaxActivePower = new SumInteger<SymmetricMeter>(this, ChannelId.GRID_MAX_ACTIVE_POWER,
				SymmetricMeter.ChannelId.MAX_ACTIVE_POWER);
		/*
		 * Production
		 */
		this.productionAcActivePower = new SumInteger<SymmetricMeter>(this, ChannelId.PRODUCTION_AC_ACTIVE_POWER,
				SymmetricMeter.ChannelId.ACTIVE_POWER);
		this.productionDcActualPower = new SumInteger<EssDcCharger>(this, ChannelId.PRODUCTION_DC_ACTUAL_POWER,
				EssDcCharger.ChannelId.ACTUAL_POWER);
		new ChannelMergerSumInteger( //
				/* target */ this.getProductionActivePower(), //
				/* sources */ (Channel<Integer>[]) new Channel<?>[] { //
						this.getProductionAcActivePower(), //
						this.getProductionDcActualPower() //
				});
		// TODO Charger needs a 'MaxActualPower' as well. And it needs to be considered
		// here.
		this.productionMaxAcActivePower = new SumInteger<SymmetricMeter>(this, ChannelId.PRODUCTION_MAX_AC_ACTIVE_POWER,
				SymmetricMeter.ChannelId.MAX_ACTIVE_POWER);
		this.productionMaxDcActualPower = new SumInteger<EssDcCharger>(this, ChannelId.PRODUCTION_MAX_DC_ACTUAL_POWER,
				EssDcCharger.ChannelId.MAX_ACTUAL_POWER);
		new ChannelMergerSumInteger( //
				/* target */ this.getProductionMaxActivePower(), //
				/* sources */ (Channel<Integer>[]) new Channel<?>[] { //
						this.getProductionMaxAcActivePower(), //
						this.getProductionMaxDcActualPower() //
				});

		/*
		 * Consumption
		 */
		this.getGridMaxActivePower().onSetNextValue(calculateMaxConsumption);
		this.getProductionMaxActivePower().onSetNextValue(calculateMaxConsumption);
		final Consumer<Value<Integer>> calculateConsumption = ignoreValue -> {
			int ess = this.getEssActivePower().getNextValue().orElse(0);
			int grid = this.getGridActivePower().getNextValue().orElse(0);
			int productionAc = this.getProductionAcActivePower().getNextValue().orElse(0);
			int consumption = ess + grid + productionAc;
			this.getConsumptionActivePower().setNextValue(consumption);
		};
		this.getEssActivePower().onSetNextValue(calculateConsumption);
		this.getGridActivePower().onSetNextValue(calculateConsumption);
		this.getProductionAcActivePower().onSetNextValue(calculateConsumption);
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
	public String debugLog() {
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
		return "Ess SoC:" + this.getEssSoc().value().asString() //
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
}
