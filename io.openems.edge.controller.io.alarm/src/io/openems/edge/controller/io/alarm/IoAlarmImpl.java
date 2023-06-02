package io.openems.edge.controller.io.alarm;

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
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.IO.Alarm", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class IoAlarmImpl extends AbstractOpenemsComponent implements IoAlarm, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(IoAlarmImpl.class);

	@Reference
	private ComponentManager componentManager;

	private Config config;

	public IoAlarmImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				IoAlarm.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws IllegalArgumentException, OpenemsNamedException {
		var setOutput = false;

		for (String channelAddress : this.config.inputChannelAddress()) {
			var channel = this.componentManager.getChannel(ChannelAddress.fromString(channelAddress));
			// Reading the value of all input channels
			boolean isStateChannelSet = TypeUtils.getAsType(OpenemsType.BOOLEAN, channel.value().getOrError());

			if (isStateChannelSet) {
				// If Channel was set: signal true
				setOutput = true;
				break;
			}
		}

		// Set Output Channel
		WriteChannel<Boolean> outputChannel = this.componentManager
				.getChannel(ChannelAddress.fromString(this.config.outputChannelAddress()));
		var currentValueOpt = outputChannel.value().asOptional();
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != setOutput) {
			this.logInfo(this.log, "Set output [" + outputChannel.address() + "] " + setOutput + ".");
			outputChannel.setNextWriteValue(setOutput);
		}
	}
}
