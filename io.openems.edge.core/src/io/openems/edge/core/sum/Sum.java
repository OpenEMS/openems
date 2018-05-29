package io.openems.edge.core.sum;

import java.util.Map;

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
		 * <li>Interface: Sum (origin: Ess Symmetric Readonly)
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
		 * Grid-Meter: Active Power
		 * 
		 * <ul>
		 * <li>Interface: Sum (origin: Meter Symmetric)
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		GRIDMETER_ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(SymmetricMeter.POWER_DOC_TEXT));

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
	private final AverageInteger essSoc;
	private final SumInteger essActivePower;

	/*
	 * Grid-Meter
	 */
	private final SumInteger gridmeterActivePower;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private void addEss(Ess ess) {
		this.essSoc.addComponent(ess);
		if (ess instanceof SymmetricEss) {
			this.essActivePower.addComponent(ess);
		}
	}

	protected void removeEss(Ess ess) {
		this.essSoc.removeComponent(ess);
		this.essActivePower.removeComponent(ess);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private void addMeter(Meter meter) {
		switch (meter.getMeterType()) {
		case CONSUMPTION:
			// TODO
			break;

		case GRID:
			/*
			 * Grid-Meter
			 */
			if (meter instanceof SymmetricMeter) {
				this.gridmeterActivePower.addComponent(meter);
			}
			break;

		case PRODUCTION:
			// TODO
			break;
		}
	}

	protected void removeMeter(Meter meter) {
		this.gridmeterActivePower.removeComponent(meter);
	}

	public Sum() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		this.essSoc = new AverageInteger(this, ChannelId.ESS_SOC, Ess.ChannelId.SOC);
		this.essActivePower = new SumInteger(this, ChannelId.ESS_ACTIVE_POWER,
				SymmetricEssReadonly.ChannelId.ACTIVE_POWER);
		this.gridmeterActivePower = new SumInteger(this, ChannelId.GRIDMETER_ACTIVE_POWER,
				SymmetricMeter.ChannelId.ACTIVE_POWER);
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
		return "ESS SoC:" + this.getEssSoc().value().asString() //
				+ "|L:" + this.getEssActivePower().value().asString() //
				+ " Grid-Meter L:" + this.getGridmeterActivePower().value().asString() //
		// + "|Allowed:" +
		// this.channel(ChannelId.ALLOWED_CHARGE).value().asStringWithoutUnit() + ";"
		// + this.channel(ChannelId.ALLOWED_DISCHARGE).value().asString() //
		// + "|" + this.getGridMode().value().asOptionString();
		;
	}

	public Channel<Integer> getEssSoc() {
		return this.channel(ChannelId.ESS_SOC);
	}

	public Channel<Integer> getEssActivePower() {
		return this.channel(ChannelId.ESS_ACTIVE_POWER);
	}

	public Channel<Integer> getGridmeterActivePower() {
		return this.channel(ChannelId.GRIDMETER_ACTIVE_POWER);
	}
}
