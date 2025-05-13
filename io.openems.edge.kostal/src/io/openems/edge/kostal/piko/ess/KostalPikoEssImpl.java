package io.openems.edge.kostal.piko.ess;

import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.kostal.piko.core.api.KostalPikoCore;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Kostal.Piko.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
})
public class KostalPikoEssImpl extends AbstractOpenemsComponent
		implements KostalPikoEss, SymmetricEss, OpenemsComponent {

	private final AtomicReference<KostalPikoCore> core = new AtomicReference<>();

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setCore(KostalPikoCore core) {
		this.core.set(core);
		core.setEss(this);
	}

	protected void unsetCore(KostalPikoCore core) {
		this.core.compareAndSet(core, null);
		core.unsetEss(this);
	}

	public KostalPikoEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				KostalPikoEss.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		// update filter for 'Core'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Core", config.core_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString(); //
	}
}
