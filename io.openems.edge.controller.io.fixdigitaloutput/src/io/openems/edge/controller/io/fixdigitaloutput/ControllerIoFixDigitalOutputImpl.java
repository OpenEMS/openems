package io.openems.edge.controller.io.fixdigitaloutput;

import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Io.FixDigitalOutput", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerIoFixDigitalOutputImpl extends AbstractOpenemsComponent
		implements ControllerIoFixDigitalOutput, Controller, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	/** Stores the ChannelAddress of the WriteChannel. */
	private ChannelAddress outputChannelAddress = null;
	/** Takes the configured "isOn" setting. */
	private boolean isOn = false;

	public ControllerIoFixDigitalOutputImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerIoFixDigitalOutput.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		// parse config
		this.isOn = config.isOn();
		this.outputChannelAddress = ChannelAddress.fromString(config.outputChannelAddress());

		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws IllegalArgumentException, OpenemsNamedException {
		if (this.isOn) {
			this.switchOn();
		} else {
			this.switchOff();
		}
	}

	/**
	 * Switch the output ON.
	 *
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void switchOn() throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(true);
	}

	/**
	 * Switch the output OFF.
	 *
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void switchOff() throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(false);
	}

	private void setOutput(boolean value) throws IllegalArgumentException, OpenemsNamedException {
		WriteChannel<Boolean> channel = this.componentManager.getChannel(this.outputChannelAddress);
		if (channel.value().asOptional().equals(Optional.of(value))) {
			// it is already in the desired state
		} else {
			channel.setNextWriteValue(value);
		}
	}
}
