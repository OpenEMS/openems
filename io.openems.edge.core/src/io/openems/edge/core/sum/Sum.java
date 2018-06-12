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
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.sum.internal.AverageInteger;
import io.openems.edge.core.sum.internal.SumInteger;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.symmetric.api.SymmetricEss;
import io.openems.edge.ess.symmetric.readonly.api.SymmetricEssReadonly;
import io.openems.edge.meter.api.Meter;
import io.openems.edge.meter.symmetric.api.SymmetricMeter;

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
				.text(SymmetricEss.POWER_DOC_TEXT)),
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
		 * <li>Interface: Sum (origin: Meter Symmetric)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: should be only positive
		 * </ul>
		 */
		PRODUCTION_ACTIVE_POWER(new Doc() //
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
	private final List<Ess> esss = new CopyOnWriteArrayList<>();
	private final AverageInteger<Ess> essSoc;
	private final SumInteger<SymmetricEssReadonly> essActivePower;

	/*
	 * Grid
	 */
	private final SumInteger<SymmetricMeter> gridActivePower;
	private final SumInteger<SymmetricMeter> gridMinActivePower;
	private final SumInteger<SymmetricMeter> gridMaxActivePower;

	/*
	 * Production
	 */
	private final SumInteger<SymmetricMeter> productionActivePower;
	private final SumInteger<SymmetricMeter> productionMaxActivePower;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private void addEss(Ess ess) {
		this.esss.add(ess);
		this.essSoc.addComponent(ess);
		if (ess instanceof SymmetricEssReadonly) {
			this.essActivePower.addComponent((SymmetricEssReadonly) ess);
		}
		this.calculateMaxConsumption.accept(null /* ignored */);
	}

	protected void removeEss(Ess ess) {
		this.esss.remove(ess);
		this.essSoc.removeComponent(ess);
		this.essActivePower.removeComponent(ess);
	}

	private final Consumer<Value<Integer>> calculateMaxConsumption = ignoreValue -> {
		int ess = 0;
		for (Ess e : this.esss) {
			ess += e.getMaxActivePower().getNextValue().orElse(0);
		}
		int grid = this.getGridMaxActivePower().getNextValue().orElse(0);
		int production = this.getProductionMaxActivePower().getNextValue().orElse(0);
		int consumption = ess + grid + production;
		this.getConsumptionMaxActivePower().setNextValue(consumption);
	};

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private void addMeter(Meter meter) {
		switch (meter.getMeterType()) {
		case CONSUMPTION_METERED:
			// TODO
			break;

		case CONSUMPTION_NOT_METERED:
			// TODO
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
				this.productionActivePower.addComponent((SymmetricMeter) meter);
				this.productionMaxActivePower.addComponent((SymmetricMeter) meter);
			}
			break;
		}
	}

	protected void removeMeter(Meter meter) {
		this.gridActivePower.removeComponent(meter);
		this.gridMinActivePower.removeComponent(meter);
		this.gridMaxActivePower.removeComponent(meter);
		this.productionMaxActivePower.removeComponent(meter);
	}

	public Sum() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		/*
		 * Ess
		 */
		this.essSoc = new AverageInteger<Ess>(this, ChannelId.ESS_SOC, Ess.ChannelId.SOC);
		this.essActivePower = new SumInteger<SymmetricEssReadonly>(this, ChannelId.ESS_ACTIVE_POWER,
				SymmetricEssReadonly.ChannelId.ACTIVE_POWER);
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
		this.productionActivePower = new SumInteger<SymmetricMeter>(this, ChannelId.PRODUCTION_ACTIVE_POWER,
				SymmetricMeter.ChannelId.ACTIVE_POWER);
		this.productionMaxActivePower = new SumInteger<SymmetricMeter>(this, ChannelId.PRODUCTION_MAX_ACTIVE_POWER,
				SymmetricMeter.ChannelId.MAX_ACTIVE_POWER);
		/*
		 * Consumption
		 */
		this.getGridMaxActivePower().onSetNextValue(calculateMaxConsumption);
		this.getProductionMaxActivePower().onSetNextValue(calculateMaxConsumption);
		final Consumer<Value<Integer>> calculateConsumption = ignoreValue -> {
			int ess = this.getEssActivePower().getNextValue().orElse(0);
			int grid = this.getGridActivePower().getNextValue().orElse(0);
			int production = this.getProductionActivePower().getNextValue().orElse(0);
			int consumption = ess + grid + production;
			this.getConsumptionActivePower().setNextValue(consumption);
		};
		this.getEssActivePower().onSetNextValue(calculateConsumption);
		this.getGridActivePower().onSetNextValue(calculateConsumption);
		this.getProductionActivePower().onSetNextValue(calculateConsumption);
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
		return "Ess SoC:" + this.getEssSoc().value().asString() //
				+ "|L:" + this.getEssActivePower().value().asString() //
				+ " Grid L:" + this.getGridActivePower().value().asString() //
				+ " Production L:" + this.getProductionActivePower().value().asString() //
				+ " Consumption L:" + this.getConsumptionActivePower().value().asString() //
		;
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

	public Channel<Integer> getProductionMaxActivePower() {
		return this.channel(ChannelId.PRODUCTION_MAX_ACTIVE_POWER);
	}

	public Channel<Integer> getConsumptionActivePower() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER);
	}

	public Channel<Integer> getConsumptionMaxActivePower() {
		return this.channel(ChannelId.CONSUMPTION_MAX_ACTIVE_POWER);
	}
}
