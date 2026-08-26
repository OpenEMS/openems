package io.openems.edge.bosch.bpts5hybrid.pv;

import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;

import java.io.IOException;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bosch.bpts5hybrid.core.BoschBpts5HybridCore;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bosch.BPTS5Hybrid.Pv", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
@GenerateTargetsFromReferences("core")
public class BoschBpts5HybridPvImpl extends AbstractOpenemsComponent
		implements BoschBpts5HybridPv, EssDcCharger, OpenemsComponent {

	private static final int PEAK_POWER = 5_500;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, target = "(&(id=${config.core_id})(enabled=true))")
	private BoschBpts5HybridCore core;

	public BoschBpts5HybridPvImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				BoschBpts5HybridPv.ChannelId.values() //
		);
		this._setMaxActualPower(PEAK_POWER); // TODO: get from read worker
	}

	@Activate
	protected void activate(ComponentContext context, Config config)
			throws OpenemsNamedException, ConfigurationException, IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.core.setPv(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.core != null) {
			this.core.setPv(null);
		}
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "PV:" + this.getActualPower().asString();
	}
}
