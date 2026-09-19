package io.openems.edge.bosch.bpts5hybrid.ess;

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
import io.openems.edge.ess.api.SymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bosch.BPTS5Hybrid.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
@GenerateTargetsFromReferences("core")
public class BoschBpts5HybridEssImpl extends AbstractOpenemsComponent
		implements BoschBpts5HybridEss, SymmetricEss, OpenemsComponent {

	private static final int CAPACITY = 8_800;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, target = "(&(id=${config.core_id})(enabled=true))")
	private BoschBpts5HybridCore core;

	public BoschBpts5HybridEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				BoschBpts5HybridEss.ChannelId.values() //
		);
		this._setCapacity(CAPACITY); // TODO: get from read worker
	}

	@Activate
	private void activate(ComponentContext context, Config config)
			throws OpenemsNamedException, ConfigurationException, IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.core.setEss(this);
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
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString();
	}
}
