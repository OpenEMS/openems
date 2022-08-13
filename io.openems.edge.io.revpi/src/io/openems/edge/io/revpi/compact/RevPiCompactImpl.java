package io.openems.edge.io.revpi.compact;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.revpi.hardware.api.dio.RevPiDigitalReadChannel;
import io.openems.edge.io.revpi.hardware.api.dio.RevPiDigitalWriteChannel;
import io.openems.edge.io.revpi.hardware.api.dio.RevPiHardwareApiDio;

@Designate(ocd = RevPiCompactConfig.class, factory = true)
@Component(//
		name = "IO.RevolutionPi.CompactIO", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class RevPiCompactImpl extends AbstractOpenemsComponent
		implements DigitalInput, DigitalOutput, OpenemsComponent, EventHandler {
	public RevPiCompactImpl() {
		super(OpenemsComponent.ChannelId.values(), DigitalOutput.ChannelId.values(), //
				DigitalInput.ChannelId.values(), RevPiCompact.ChannelId.values());
	}

	private RevPiHardwareApiDio RevPiCompactDio;

	@Activate
	void activate(ComponentContext context, RevPiCompactConfig config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		final String prefixDin = config.prefixDigitalIn();
		final String prefixDout = config.prefixDigitalOut();
		final String isDinUsable = config.inputUsed();
		final String isDoutUsable = config.outputUsed();
		final int offsetIn = config.firstInputIndex() - 1;
		final int offsetOut = config.firstOutputIndex() - 1;

		RevPiCompactDio = new RevPiHardwareApiDio(prefixDin, prefixDout, isDinUsable, isDoutUsable, offsetIn,
				offsetOut);

		// Setup channels
		for (RevPiCompact.ChannelId id : RevPiCompact.ChannelId.values()) {
			if (RevPiCompact.isReadChannel(id)) {
				RevPiCompactDio.setupReadChannel(channel(id));
			} else if (RevPiCompact.isDebugChannel(id)) {
				RevPiCompactDio.setupWriteDbgChannel(channel(id));
			} else if (RevPiCompact.isWriteChannel(id)) {
				RevPiCompactDio.setupWriteChannel(channel(id));
			}
		}

		RevPiCompactDio.installOnDataOutCallback();

		if (config.initOutputFromHardware()) {
			RevPiCompactDio.eventBeforeProcessImage();
		} else {
			RevPiCompactDio.setAllOutput(false);
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		RevPiCompactDio.setAllOutput(false);
		super.deactivate();
		RevPiCompactDio.close();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			RevPiCompactDio.eventBeforeProcessImage();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			RevPiCompactDio.eventExecuteWrite();
			break;
		}
	}

	@Override
	public String debugLog() {
		return RevPiCompactDio.debugLog();
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		BooleanReadChannel[] readChannels = new BooleanReadChannel[RevPiCompactDio.getInputChannels().size()];
		int idx = 0;
		for (RevPiDigitalReadChannel channel : RevPiCompactDio.getInputChannels()) {
			readChannels[idx] = channel.getReadChannel();
			idx++;
		}
		return readChannels;
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		BooleanWriteChannel[] writeChannels = new BooleanWriteChannel[RevPiCompactDio.getOutputChannels().size()];
		int idx = 0;
		for (RevPiDigitalWriteChannel channel : RevPiCompactDio.getOutputChannels()) {
			writeChannels[idx] = channel.getWriteChannel();
			idx++;
		}
		return writeChannels;
	}
}
