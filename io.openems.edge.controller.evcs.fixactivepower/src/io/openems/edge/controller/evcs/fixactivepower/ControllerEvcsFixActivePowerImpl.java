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
		if (this.updateTimerExpired()) {
			ManagedEvcs evcs = this.componentManager.getComponent(this.config.evcs_id());
			var powerLimit = this.config.power();

			evcs.setChargePowerLimit(powerLimit);
		}
	}

	private boolean updateTimerExpired() {
		var now = LocalDateTime.now(this.componentManager.getClock());
		var result = !this.lastRun.plusSeconds(this.config.updateFrequency()).isAfter(now);
		if (result) {
			this.lastRun = now;
		}
		return result;
	}

}
