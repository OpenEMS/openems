package io.openems.edge.controller.evcs.fixactivepower;

import java.time.LocalDateTime;

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
import io.openems.edge.evcs.api.ManagedEvcs;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs.FixActivePower", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEvcsFixActivePowerImpl extends AbstractOpenemsComponent
		implements ControllerEvcsFixActivePower, Controller, OpenemsComponent {

	private static final int RUN_EVERY_MINUTES = 1;

	@Reference
	private ComponentManager componentManager;

	private LocalDateTime lastRun = LocalDateTime.MIN;
	private Config config;

	public ControllerEvcsFixActivePowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEvcsFixActivePower.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		var now = LocalDateTime.now(this.componentManager.getClock());

		// Execute only every ... minutes
		if (this.lastRun.plusMinutes(RUN_EVERY_MINUTES).isAfter(now)) {
			return;
		}

		ManagedEvcs evcs = this.componentManager.getComponent(this.config.evcs_id());

		// set charge power
		evcs.setChargePowerLimit(this.config.power());
		this.lastRun = now;
	}

}
