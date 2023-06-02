package io.openems.edge.ess.mr.gridcon.controller.balancing;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.mr.gridcon.GridconPcs;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.MR.SetBalancing", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SetBalancingImpl extends AbstractOpenemsComponent implements SetBalancing, Controller, OpenemsComponent {

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	public SetBalancingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				SetBalancing.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		GridconPcs gridcon = this.componentManager.getComponent(this.config.gridcon_id());

		gridcon.setBalancingMode(this.config.balancingMode());
	}

}
