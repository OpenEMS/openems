package io.openems.edge.controller.channelsinglethreshold;

import java.time.Duration;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.singlethreshold.AbstractSingleThreshold;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.ChannelSingleThreshold", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ChannelSingleThreshold extends AbstractSingleThreshold implements Controller, OpenemsComponent {

	@Reference
	protected ComponentManager componentManager;

	public ChannelSingleThreshold() {
		super();
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	/**
	 * Length of hysteresis. States are not changed quicker than this.
	 */

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		/*
		 * parse config
		 */
		this.threshold = config.threshold();
		this.hysteresis = Duration.ofMinutes(config.hysteresis());
		this.invertOutput = config.invert();
		this.inputChannelAddress = ChannelAddress.fromString(config.inputChannelAddress());
		this.outputChannelAddress = ChannelAddress.fromString(config.outputChannelAddress());
		this.mode = config.mode();

		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * The current state in the State Machine
	 */

	@Override
	public void run() throws IllegalArgumentException, OpenemsNamedException {
		super.run();
	}

}
