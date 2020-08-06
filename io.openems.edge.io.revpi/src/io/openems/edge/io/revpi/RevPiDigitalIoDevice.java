package io.openems.edge.io.revpi;

import java.io.IOException;
import java.util.Optional;

import org.clehne.revpi.dataio.DataInOut;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.RevolutionPi.DigitalIO", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class RevPiDigitalIoDevice extends AbstractOpenemsComponent
		implements DigitalOutput, DigitalInput, OpenemsComponent, EventHandler {

	private static final Object INVALIDATE_CHANNEL = null;

	private final Logger log = LoggerFactory.getLogger(RevPiDigitalIoDevice.class);
	private final BooleanWriteChannel[] digitalOutputChannels;
	private final BooleanReadChannel[] digitalInputChannels;

	// maybe used, when two DIO boards are attached to the Revolutionpi
	@SuppressWarnings("unused")
	private Config config = null;

	private DataInOut revPiHardware;

	public RevPiDigitalIoDevice() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				DigitalInput.ChannelId.values(), //
				RevPiDioChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(RevPiDioChannelId.OUT_1), //
				this.channel(RevPiDioChannelId.OUT_2), //
				this.channel(RevPiDioChannelId.OUT_3), //
				this.channel(RevPiDioChannelId.OUT_4), //
				this.channel(RevPiDioChannelId.OUT_5), //
				this.channel(RevPiDioChannelId.OUT_6), //
				this.channel(RevPiDioChannelId.OUT_7), //
				this.channel(RevPiDioChannelId.OUT_8), //
				this.channel(RevPiDioChannelId.OUT_9), //
				this.channel(RevPiDioChannelId.OUT_10), //
				this.channel(RevPiDioChannelId.OUT_11), //
				this.channel(RevPiDioChannelId.OUT_12), //
				this.channel(RevPiDioChannelId.OUT_13), //
				this.channel(RevPiDioChannelId.OUT_14) //
		};

		this.digitalInputChannels = new BooleanReadChannel[] { //
				this.channel(RevPiDioChannelId.IN_1), //
				this.channel(RevPiDioChannelId.IN_2), //
				this.channel(RevPiDioChannelId.IN_3), //
				this.channel(RevPiDioChannelId.IN_4), //
				this.channel(RevPiDioChannelId.IN_5), //
				this.channel(RevPiDioChannelId.IN_6), //
				this.channel(RevPiDioChannelId.IN_7), //
				this.channel(RevPiDioChannelId.IN_8), //
				this.channel(RevPiDioChannelId.IN_9), //
				this.channel(RevPiDioChannelId.IN_10), //
				this.channel(RevPiDioChannelId.IN_11), //
				this.channel(RevPiDioChannelId.IN_12), //
				this.channel(RevPiDioChannelId.IN_13), //
				this.channel(RevPiDioChannelId.IN_14) //
		};
	}

	private void deactivateAllWriteChannels() {
		for (BooleanWriteChannel channel : this.digitalOutputChannels) {
			try {
				channel.setNextWriteValue(Boolean.FALSE);
			} catch (OpenemsNamedException e) {
				// ignore
			}
		}
	}

	private void updateChannelValues() {
		// read all digital in pins
		for (int i = 0; i < this.digitalInputChannels.length; i++) {
			try {
				boolean in = this.revPiHardware.getDataIn(i + 1);
				Optional<Boolean> inOpt = Optional.ofNullable(in);

				if (this.digitalInputChannels[i].value().asOptional().equals(inOpt)) {
					// channel already in the desired state
				} else {
					this.digitalInputChannels[i].setNextValue(in);
				}
			} catch (Exception e) {
				this.logError(this.log, "Unable to update channel values ex: " + e.getMessage());
				this.digitalInputChannels[i].setNextValue(INVALIDATE_CHANNEL);
			}
		}
	}

	private void installOnDataOutCallback(BooleanWriteChannel wc, final int idx) {
		wc.onUpdate((newValue) -> {
			try {
				if (this.revPiHardware != null) {
					this.revPiHardware.setDataOut(idx + 1, newValue.orElse(false));
				}
			} catch (Exception e) {
				this.logError(this.log, "Unable to set data out " + (idx + 1));
			}
		});
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		for (int i = 0; i < this.digitalOutputChannels.length; i++) {
			this.installOnDataOutCallback(this.digitalOutputChannels[i], i);
		}
		this.deactivateAllWriteChannels();
		this.revPiHardware = new DataInOut();
	}

	@Deactivate
	protected void deactivate() {
		this.deactivateAllWriteChannels();
		super.deactivate();
		try {
			this.revPiHardware.close();
		} catch (IOException e) {
			this.logError(this.log, "Exception on closing driver ex: " + e.getMessage());
			e.printStackTrace();
		}
		this.revPiHardware = null;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannelValues();
			break;
		}
	}

	@Override
	public String debugLog() {
		StringBuilder b = new StringBuilder();
		int i = 0;
		b.append("IN:");
		for (BooleanReadChannel channel : this.digitalInputChannels) {
			Optional<Boolean> valueOpt = channel.value().asOptional();
			if (valueOpt.isPresent()) {
				if (valueOpt.get()) {
					b.append("1");
				} else {
					b.append("0");
				}
			} else {
				b.append("-");
			}
			if ((i++) % 4 == 3) {
				b.append(" ");
			}
		}
		i = 0;
		b.append("  OUT:");
		for (BooleanWriteChannel channel : this.digitalOutputChannels) {
			Optional<Boolean> valueOpt = channel.value().asOptional();
			if (valueOpt.isPresent()) {
				if (valueOpt.get()) {
					b.append("1");
				} else {
					b.append("0");
				}
			} else {
				b.append("-");
			}
			if ((i++) % 4 == 3) {
				b.append(" ");
			}
		}
		return b.toString();
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		return this.digitalInputChannels;
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

}
