package io.openems.edge.controller.io.alarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.io.alarm", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class IOAlarm extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(IOAlarm.class);

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	public IOAlarm() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {

		super.activate(context, config.id(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws IllegalArgumentException, OpenemsNamedException {
		List<Boolean> inputs = new ArrayList<Boolean>();
		Boolean value;
		/**
		 * Reading all the input channel address from the config, and adding the boolean
		 * values into an inputs array.
		 */
		try {
			for (String channelAddress : this.config.inputChannelAddress()) {
				StateChannel channel = this.componentManager.getChannel(ChannelAddress.fromString(channelAddress));
				value = TypeUtils.getAsType(OpenemsType.BOOLEAN, channel.value().getOrError());
				inputs.add(value);
			}
		} catch (Exception e) {
			this.logError(this.log, e.getClass().getSimpleName() + ": " + e.getMessage());
			return;
		}

		/**
		 * traversing the elements in the input array, and signaling true if any one
		 * value from the configuration is true.
		 */
		boolean signal = false;
		for (Boolean input : inputs) {
			if (input) {
				signal = true;
				break;
			}
		}
		String outputChannelAddress = this.config.outputChannelAddress();
		try {

			WriteChannel<Boolean> outputChannel = this.componentManager
					.getChannel(ChannelAddress.fromString(outputChannelAddress));
			Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();
			if (!currentValueOpt.isPresent() || currentValueOpt.get() != signal) {
				this.logInfo(this.log, "Set output [" + outputChannel.address() + "] " + (signal) + ".");
				outputChannel.setNextWriteValue(signal);
			}
		} catch (Exception e) {
			this.logError(this.log, "Unable to set output: [" + outputChannelAddress + "] " + e.getMessage());
		}
	}
}
