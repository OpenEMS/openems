package io.openems.edge.io.revpi.dio;

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

@Designate(ocd = RevPiDioConfig.class, factory = true)
@Component(//
		name = "IO.RevolutionPi.DigitalIO", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class RevPiDioImpl extends AbstractOpenemsComponent
		implements DigitalInput, DigitalOutput, OpenemsComponent, EventHandler {
	public RevPiDioImpl() {
		super(OpenemsComponent.ChannelId.values(), DigitalOutput.ChannelId.values(), //
				DigitalInput.ChannelId.values(), RevPiDio.ChannelId.values());
	}

	private RevPiHardwareApiDio RevPiDioDevice;

	@Activate
	void activate(ComponentContext context, RevPiDioConfig config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		final String prefixDin = config.prefixDigitalIn();
		final String prefixDout = config.prefixDigitalOut();
		final String isDinUsable = config.inputUsed();
		final String isDoutUsable = config.outputUsed();
		final int offsetIn = config.firstInputIndex() - 1;
		final int offsetOut = config.firstOutputIndex() - 1;

		RevPiDioDevice = new RevPiHardwareApiDio(prefixDin, prefixDout, isDinUsable, isDoutUsable, offsetIn, offsetOut);

		// Setup channels
		for (RevPiDio.ChannelId id : RevPiDio.ChannelId.values()) {
			if (RevPiDio.isReadChannel(id)) {
				RevPiDioDevice.setupReadChannel(channel(id));
			} else if (RevPiDio.isDebugChannel(id)) {
				RevPiDioDevice.setupWriteDbgChannel(channel(id));
			} else if (RevPiDio.isWriteChannel(id)) {
				RevPiDioDevice.setupWriteChannel(channel(id));
			}
		}

		RevPiDioDevice.installOnDataOutCallback();

		if (config.initOutputFromHardware()) {
			RevPiDioDevice.eventBeforeProcessImage();
		} else {
			RevPiDioDevice.setAllOutput(false);
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		RevPiDioDevice.setAllOutput(false);
		super.deactivate();
		RevPiDioDevice.close();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			RevPiDioDevice.eventBeforeProcessImage();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			RevPiDioDevice.eventExecuteWrite();
			break;
		}
	}

	@Override
	public String debugLog() {
		return RevPiDioDevice.debugLog();
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		BooleanReadChannel[] readChannels = new BooleanReadChannel[RevPiDioDevice.getInputChannels().size()];
		int idx = 0;
		for (RevPiDigitalReadChannel channel : RevPiDioDevice.getInputChannels()) {
			readChannels[idx] = channel.getReadChannel();
			idx++;
		}
		return readChannels;
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		BooleanWriteChannel[] writeChannels = new BooleanWriteChannel[RevPiDioDevice.getOutputChannels().size()];
		int idx = 0;
		for (RevPiDigitalWriteChannel channel : RevPiDioDevice.getOutputChannels()) {
			writeChannels[idx] = channel.getWriteChannel();
			idx++;
		}
		return writeChannels;
	}
}
