package io.openems.edge.bosch.bpts5hybrid.pv;

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
import io.openems.edge.ess.dccharger.api.EssDcCharger;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bosch.BPTS5Hybrid.Pv", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
		} //
)
public class BoschBpts5HybridPv extends AbstractOpenemsComponent implements EssDcCharger, OpenemsComponent {

	private final int PEAK_POWER = 5_500;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private BoschBpts5HybridCore core;

	@Reference
	private ConfigurationAdmin cm;

	public BoschBpts5HybridPv() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				ChannelId.values() //
		);
		this._setMaxActualPower(this.PEAK_POWER); // TODO: get from read worker
	}

	@Activate
	void activate(ComponentContext context, Config config)
			throws OpenemsNamedException, ConfigurationException, IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'core'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "core", config.core_id())) {
			return;
		}
		this.core.setPv(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.core != null) {
			this.core.setEss(null);
		}
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "PV:" + this.getActualPower().asString();
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
}
