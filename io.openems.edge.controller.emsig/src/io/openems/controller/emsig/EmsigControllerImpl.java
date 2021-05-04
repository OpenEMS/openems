package io.openems.controller.emsig;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.controller.emsig.ojalgo.OjalgoTest;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.EMSIG", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EmsigControllerImpl extends AbstractOpenemsComponent
		implements EmsigController, Controller, OpenemsComponent {

	private Config config = null;

	public EmsigControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				EmsigController.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		Instant now = Instant.now();
		OjalgoTest ojalgoTest = new OjalgoTest();
		try {
			ojalgoTest.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Time: " + Duration.between(now, Instant.now()).toMillis() + "ms");
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
	}
}
