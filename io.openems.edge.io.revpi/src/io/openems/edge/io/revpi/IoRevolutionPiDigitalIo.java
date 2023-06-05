package io.openems.edge.io.revpi;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import org.clehne.revpi.dataio.DataInOut;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
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
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class IoRevolutionPiDigitalIo extends AbstractOpenemsComponent
		implements DigitalOutput, DigitalInput, OpenemsComponent, EventHandler {

	private static final Object INVALIDATE_CHANNEL = null;

	private final Logger log = LoggerFactory.getLogger(IoRevolutionPiDigitalIo.class);
	private final BooleanWriteChannel[] channelOut;
	private final BooleanReadChannel[] channelIn;
	private final BooleanReadChannel[] channelOutDbg;

	// maybe used, when two DIO boards are attached to the Revolutionpi
	private Config config = null;
	private DataInOut revPiHardware;

	public IoRevolutionPiDigitalIo() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				DigitalInput.ChannelId.values(), //
				RevPiDigitalIoDevice.ChannelId.values() //
		);
		this.channelOut = new BooleanWriteChannel[] { //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_1), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_2), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_3), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_4), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_5), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_6), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_7), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_8), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_9), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_10), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_11), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_12), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_13), //
				this.channel(RevPiDigitalIoDevice.ChannelId.OUT_14) //
		};

		this.channelOutDbg = new BooleanReadChannel[] { //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT1), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT2), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT3), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT4), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT5), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT6), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT7), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT8), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT9), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT10), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT11), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT12), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT13), //
				this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT14) //
		};

		this.channelIn = new BooleanReadChannel[] { //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_1), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_2), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_3), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_4), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_5), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_6), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_7), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_8), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_9), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_10), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_11), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_12), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_13), //
				this.channel(RevPiDigitalIoDevice.ChannelId.IN_14) //
		};
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.revPiHardware = new DataInOut();
		if (this.config.initOutputFromHardware()) {
			this.readOutputFromHardwareOnce();
		} else {
			this.setAllOutput(false);
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.setAllOutput(false);
		super.deactivate();
		try {
			this.revPiHardware.close();
		} catch (IOException e) {
			this.logError(this.log, "Exception on closing driver ex: " + e.getMessage());
		}
		this.revPiHardware = null;
	}

	private void setAllOutput(boolean setOn) {
		for (BooleanWriteChannel ch : this.channelOut) {
			try {
				ch.setNextWriteValue(setOn);
			} catch (OpenemsNamedException e) {
				// ignore
			}
		}
	}

	private void readOutputFromHardwareOnce() {
		// read all digital out pins also, because pins have already been initialized
		// from outside
		for (var idx = 0; idx < this.channelOut.length; idx++) {
			try {
				var in = this.revPiHardware.getDataOut(idx + 1);
				this.channelOut[idx].setNextWriteValue(in);
			} catch (Exception e) {
				this.logError(this.log, "Unable to update channel values ex: " + e.getMessage());
				this.channelOut[idx].setNextValue(INVALIDATE_CHANNEL);
			}
		}
	}

	private void updateDataInChannels() {
		// read all digital in pins
		for (var i = 0; i < this.channelIn.length; i++) {
			try {
				var in = this.getData(i);
				Optional<Boolean> inOpt = Optional.ofNullable(in);

				if (this.channelIn[i].value().asOptional().equals(inOpt)) {
					// channel already in the desired state
				} else {
					this.channelIn[i].setNextValue(in);
				}
			} catch (Exception e) {
				this.logError(this.log, "Unable to update channel values ex: " + e.getMessage());
				this.channelIn[i].setNextValue(INVALIDATE_CHANNEL);
			}
		}
	}

	/**
	 * NOTE data out will only be set if the channel value changes.
	 */
	private void updateDataOutChannels() {

		// write new state to digital out pins
		for (var idx = 0; idx < this.channelOut.length; idx++) {
			try {
				var readValue = this.channelOut[idx].value().asOptional();
				var writeValue = this.channelOut[idx].getNextWriteValueAndReset();
				if (!writeValue.isPresent()) {
					// no write value
					continue;
				}
				if (Objects.equals(readValue, writeValue)) {
					// read value = write value
					continue;
				}

				if (this.revPiHardware != null) {
					this.revPiHardware.setDataOut(idx + 1, writeValue.get());
				}
				this.logInfo(this.log, this.channelOut[idx].channelId() + " " + writeValue.get());
				this.channelOut[idx].setNextValue(writeValue.get());

			} catch (Exception e) {
				this.logError(this.log, "Unable to update channel out values ex: " + e.getMessage());
				this.channelOut[idx].setNextValue(INVALIDATE_CHANNEL);
			}
		}

	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.eventBeforeProcessImage();
			break;

		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.eventExecuteWrite();
			break;
		}
	}

	/**
	 * Execute on Cycle Event "Before Process Image".
	 */
	private void eventBeforeProcessImage() {
		this.updateDataInChannels();
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 */
	private void eventExecuteWrite() {
		this.updateDataOutChannels();
	}

	/**
	 * Reads the data either from the given DATA IN hardware port.
	 * 
	 * @param idx the index
	 * @return the data
	 */
	private boolean getData(int idx) throws IOException {
		return this.revPiHardware.getDataIn(idx + 1);
	}

	private void appendBool(StringBuilder b, Optional<Boolean> val) {
		if (val.isPresent()) {
			if (val.get()) {
				b.append("1");
			} else {
				b.append("0");
			}
		} else {
			b.append("-");
		}
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder();
		var i = 0;
		b.append("IN:");
		for (BooleanReadChannel channel : this.channelIn) {
			var valueOpt = channel.value().asOptional();
			this.appendBool(b, valueOpt);
			if (i++ % 4 == 3) {
				b.append(" ");
			}
		}
		i = 0;
		b.append("  OUT:");

		this.channel(RevPiDigitalIoDevice.ChannelId.DEBUG_OUT1);

		for (BooleanReadChannel channel : this.channelOutDbg) {
			var valueOpt = channel.value().asOptional();
			this.appendBool(b, valueOpt);
			if (i++ % 4 == 3) {
				b.append(" ");
			}
		}
		return b.toString();
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		return this.channelIn;
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.channelOut;
	}

}
