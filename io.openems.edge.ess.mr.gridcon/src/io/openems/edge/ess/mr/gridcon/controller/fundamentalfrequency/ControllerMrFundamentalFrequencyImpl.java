package io.openems.edge.ess.mr.gridcon.controller.fundamentalfrequency;

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
import io.openems.edge.ess.mr.gridcon.enums.FundamentalFrequencyMode;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.MR.FundamentalFrequency", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerMrFundamentalFrequencyImpl extends AbstractOpenemsComponent
		implements ControllerMrFundamentalFrequency, Controller, OpenemsComponent {

	@Reference
	private ComponentManager componentManager;

	private Config config;

	public ControllerMrFundamentalFrequencyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerMrFundamentalFrequency.ChannelId.values() //
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

		gridcon.setFundamentalFrequencyMode(this.config.fundamentalFrequencyMode());

		if (this.config.fundamentalFrequencyMode() == FundamentalFrequencyMode.PFC_COS_PHI) {
			gridcon.setCosPhiSetPoint1(this.config.cosPhiSetPoint1());
			gridcon.setCosPhiSetPoint2(this.config.cosPhiSetPoint2());
		}
	}

}
