package io.openems.edge.ess.fenecon.commercial40;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.channel.ReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.symmetric.readonly.api.EssSymmetricReadonly;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Ess.Fenecon.Commercial40", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class EssFeneconCommercial40 extends AbstractOpenemsComponent implements EssSymmetricReadonly, OpenemsComponent {

	public EssFeneconCommercial40() {
		this.addChannels( //
				new ReadChannel(Ess.ChannelId.SOC), //
				new ReadChannel(EssSymmetricReadonly.ChannelId.ACTIVE_POWER), //
				new ReadChannel(EssSymmetricReadonly.ChannelId.REACTIVE_POWER));
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private volatile BridgeModbusTcp modbus = null;

	@Activate
	void activate(Config config) {
		super.activate(config.id(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

}
