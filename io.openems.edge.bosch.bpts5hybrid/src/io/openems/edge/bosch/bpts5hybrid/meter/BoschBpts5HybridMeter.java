package io.openems.edge.bosch.bpts5hybrid.meter;

import java.io.IOException;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bosch.bpts5hybrid.core.BoschBpts5HybridCore;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bosch.BPTS5Hybrid.Meter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
		} //
)
public class BoschBpts5HybridMeter extends AbstractOpenemsComponent implements ElectricityMeter, OpenemsComponent {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private BoschBpts5HybridCore core;

	@Reference
	private ConfigurationAdmin cm;

	public BoschBpts5HybridMeter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config)
			throws OpenemsNamedException, ConfigurationException, IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'core'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "core", config.core_id())) {
			return;
		}
		this.core.setMeter(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.core != null) {
			this.core.setMeter(null);
		}
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "Meter: Power:" + this.getActivePower().get();
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}
}
