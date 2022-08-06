package io.openems.edge.io.revpi.hardware.api.dio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import org.clehne.revpi.dataio.DataInOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;

public class RevPiHardwareApiDio {

	private static final Object INVALIDATE_CHANNEL = null;
	private final Logger log = LoggerFactory.getLogger(RevPiHardwareApiDio.class);
	private String PrefixDigitalIn;
	private String PrefixDigitalOut;
	private String IsDigitalInUsable;
	private String IsDigitalOutUsable;
	private int IdOffsetIn = 0;
	private int IdOffsetOut = 0;
	private ArrayList<RevPiDigitalReadChannel> digitalInChannels = new ArrayList<RevPiDigitalReadChannel>();
	private ArrayList<RevPiDigitalWriteChannel> digitalOutChannels = new ArrayList<RevPiDigitalWriteChannel>();
	private ArrayList<RevPiDigitalReadChannel> digitalOutDbgChannels = new ArrayList<RevPiDigitalReadChannel>();
	private DataInOut revPiHardware;

	/**
	 * Constructor to create new RevPiDigitalIo
	 */
	public RevPiHardwareApiDio(String prefixDigitalIn, String prefixDigitalOut, String isDigitalInUsable,
			String isDigitalOutUsable, int offsetIn, int offsetOut) {
		PrefixDigitalIn = prefixDigitalIn;
		PrefixDigitalOut = prefixDigitalOut;
		IsDigitalInUsable = isDigitalInUsable;
		IsDigitalOutUsable = isDigitalOutUsable;
		IdOffsetIn = offsetIn;
		IdOffsetOut = offsetOut;

		try {
			revPiHardware = new DataInOut();
		} catch (Exception e) {
			this.log.error("RevPi Hardware not accessible: " + e.getMessage());
		} catch (Error e) {
			this.log.error("RevPi Hardware not accessible: " + e.getMessage());
		}
	}

	/**
	 * Shuts down RevPi Hardware connection.
	 */
	public void close() {
		try {
			revPiHardware.close();
		} catch (Exception e) {
			// We do not want to spam test logs - already covered by the constructor check
			if (null != this.revPiHardware) {
				this.log.error("Exception on closing driver ex: " + e.getMessage());
			}
		}
		this.revPiHardware = null;
	}

	/**
	 * Takes care of readable debug logging.
	 */
	public String debugLog() {
		StringBuilder b = new StringBuilder();
		int i = 0;
		b.append("In: |");
		for (RevPiDigitalReadChannel channel : this.digitalInChannels) {
			Optional<Boolean> valueOpt = channel.getReadChannel().value().asOptional();
			b.append(channel.getChannelName().split("_")[1]);
			// b.append(">");

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
				b.append("| ");
			}
			b.append("|");
		}
		i = 0;

		b.append("  Out: |");
		for (RevPiDigitalWriteChannel channel : this.digitalOutChannels) {
			Optional<Boolean> valueOpt = channel.getWriteChannel().value().asOptional();
			b.append(channel.getChannelName().split("_")[1]);
			// b.append(">");

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
			b.append("|");
		}
		return b.toString();
	}

	/**
	 * Execute on Cycle Event "Before Process Image".
	 */
	public void eventBeforeProcessImage() {
		this.updateDataInChannels();
		this.updateDataOutChannels();
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 */
	public void eventExecuteWrite() {
		for (RevPiDigitalWriteChannel channel : this.digitalOutChannels) {
			executeWrite(channel);
		}
	}

	/**
	 * Gets all digital input channels.
	 * 
	 * @return ArrayList of input channels.
	 */
	public ArrayList<RevPiDigitalReadChannel> getInputChannels() {
		return digitalInChannels;
	}

	/**
	 * Gets all digital output channels.
	 * 
	 * @return ArrayList of output channels.
	 */
	public ArrayList<RevPiDigitalWriteChannel> getOutputChannels() {
		return digitalOutChannels;
	}

	/**
	 * Callback to write the internal output states to the hardware outputs.
	 */
	public void installOnDataOutCallback() {
		for (RevPiDigitalWriteChannel channel : this.digitalOutChannels) {
			channel.getWriteChannel().onUpdate((newValue) -> {
				executeWrite(channel);
			});
		}
	}

	/**
	 * Sets all internal output states to the provided value.
	 * 
	 * @param setOn boolean indication on / off state
	 */
	public void setAllOutput(boolean setOn) {
		for (RevPiDigitalWriteChannel channel : this.digitalOutChannels) {
			try {
				if (setOn) {
					channel.getWriteChannel().setNextWriteValue(Boolean.TRUE);
				} else {
					channel.getWriteChannel().setNextWriteValue(Boolean.FALSE);
				}
			} catch (OpenemsNamedException e) {
				String switchState = setOn ? "On" : "Off";
				this.log.error("Unable to set output channel " + channel.getChannelName() + " to: " + switchState
						+ "Error-MSG:" + e.getMessage());
			}
		}
	}

	/**
	 * Sets up a digital input channel
	 * 
	 * @param readChannel BooleanReadChannel to be set up.
	 */
	public void setupReadChannel(BooleanReadChannel readChannel) {
		try {
			int channelIdxRelative = getChannelIndexBySplit(readChannel.channelId().id());
			int channelIdxAbsolute = channelIdxRelative + IdOffsetIn;
			boolean isUsed = parseIsChannelUsed(IsDigitalInUsable, channelIdxRelative);

			if (isUsed) {
				digitalInChannels.add(new RevPiDigitalReadChannel(readChannel, channelIdxAbsolute, PrefixDigitalIn));
			}
		} catch (Exception e) {
			this.log.error("Unable to setup read channel " + readChannel.channelId().id() + " - " + e);
		}
	}

	/**
	 * Sets up a digital output debug channel
	 * 
	 * @param dbgChannel BooleanReadChannel to be set up for debugging.
	 */
	public void setupWriteDbgChannel(BooleanReadChannel dbgChannel) {
		try {
			int channelIdxRelative = getChannelIndexBySplit(dbgChannel.channelId().id());
			int channelIdxAbsolute = channelIdxRelative + IdOffsetIn;
			boolean isUsed = parseIsChannelUsed(IsDigitalInUsable, channelIdxRelative);

			if (isUsed) {
				digitalOutDbgChannels.add(new RevPiDigitalReadChannel(dbgChannel, channelIdxAbsolute, PrefixDigitalIn));
			}
		} catch (Exception e) {
			this.log.error("Unable to setup debug channel " + dbgChannel.channelId().id());
		}
	}

	/**
	 * Sets up a digital output channel
	 * 
	 * @param writeChannel BooleanWriteChannel to be set up.
	 */
	public void setupWriteChannel(BooleanWriteChannel writeChannel) {
		try {
			int channelIdxRelative = getChannelIndexBySplit(writeChannel.channelId().id());
			int channelIdxAbsolute = channelIdxRelative + IdOffsetOut;
			boolean isUsed = parseIsChannelUsed(IsDigitalOutUsable, channelIdxRelative);

			if (isUsed) {
				digitalOutChannels.add(new RevPiDigitalWriteChannel(writeChannel, channelIdxAbsolute, PrefixDigitalOut));
			}
		} catch (Exception e) {
			this.log.error("Unable to setup write channel " + writeChannel.channelId().id());
		}
	}

	/**
	 * Reads the state of the hardware inputs for all channels and updates the
	 * internal states.
	 */
	private void updateDataInChannels() {
		// read all digital in channels
		for (RevPiDigitalReadChannel channel : this.digitalInChannels) {
			try {
				boolean in = getDigital(channel);
				Optional<Boolean> inOpt = Optional.ofNullable(in);

				if (channel.getReadChannel().value().asOptional().equals(inOpt)) {
					// channel already in the desired state
				} else {
					channel.getReadChannel().setNextValue(in);
				}
			} catch (Exception e) {
				// We do not want to spam test logs - already covered by the constructor check
				if (null != this.revPiHardware) {
					this.log.error("Unable to update input channel values ex: " + e.getMessage());
				}
				channel.getReadChannel().setNextValue(INVALIDATE_CHANNEL);
			}
		}
	}

	/**
	 * Reads the state of the hardware outputs for all channels and updates the
	 * internal states.
	 */
	private void updateDataOutChannels() {
		// read all digital out channels
		for (RevPiDigitalWriteChannel channel : this.digitalOutChannels) {
			try {
				boolean out = getDigital(channel);
				Optional<Boolean> inOpt = Optional.ofNullable(out);

				if (channel.getWriteChannel().value().asOptional().equals(inOpt)) {
					// channel already in the desired state
				} else {
					channel.getWriteChannel().setNextValue(out);
				}
			} catch (Exception e) {
				// We do not want to spam test logs - already covered by the constructor check
				if (null != this.revPiHardware) {
					this.log.error("Unable to update output channel values ex: " + e.getMessage());
				}
				channel.getWriteChannel().setNextValue(INVALIDATE_CHANNEL);
			}
		}
	}

	/**
	 * Writes the value contained in the channel to the hardware outputs.
	 * 
	 * @param channel RevPiDigitalWriteChannel to be set on the hardware.
	 */
	private void executeWrite(RevPiDigitalWriteChannel channel) {
		Boolean readValue = channel.getWriteChannel().value().get();
		Optional<Boolean> writeValue = channel.getWriteChannel().getNextWriteValueAndReset();
		if (!writeValue.isPresent()) {
			// no write value
			return;
		}

		if (Objects.equals(readValue, writeValue.get())) {
			// read value = write value
			return;
		}

		try {
			setDigital(channel, writeValue.get());
		} catch (Exception e) {
			this.log.error("Unable to set " + channel.getChannelName());
		}
	}

	/**
	 * Gets the channel index by splitting the provided channel Id.
	 * 
	 * @param channelId String containing channel Id.
	 */
	protected int getChannelIndexBySplit(String channelId) throws Exception {
		int channelIndex = -1;

		try {
			if (!channelId.isEmpty()) {
				channelIndex = Integer.parseInt(channelId.replaceAll("\\D+", ""));
			}
		} catch (Exception e) {
			channelIndex = -1;
			throw new Exception("Channel id " + channelId + " does not contain a valid index: " + e);
		}
		return channelIndex;
	}

	/**
	 * Reads the data from the given read channel's hardware port.
	 * 
	 * @param channel RevPiDigitalReadChannel.
	 * @return True or false depending on the hardware input port's state.
	 * @throws Exception if reading the value fails.
	 */
	private boolean getDigital(RevPiDigitalReadChannel channel) throws Exception {
		return this.revPiHardware.getDigital(channel.getChannelName());
	}

	/**
	 * Reads the data from the given write channel's hardware port.
	 * 
	 * @param channel RevPiDigitalWriteChannel.
	 * @return True or false depending on the hardware output port's state.
	 * @throws Exception if the retrieval of the value failed.
	 */
	private boolean getDigital(RevPiDigitalWriteChannel channel) throws Exception {
		return this.revPiHardware.getDigital(channel.getChannelName());
	}

	/**
	 * Splits and parses the input string to determine if the channel is configured
	 * to be used.
	 * 
	 * @param isUsedString String to be split and parsed.
	 * @param channelIndexRelative Index to the the used info for.
	 * @return True if the channel is configured to be used.
	 */
	protected boolean parseIsChannelUsed(String isUsedString, int channelIndexRelative) {
		String[] usedList = isUsedString.split("\\|");
		boolean isUsed = false;
		int arrayIndex = channelIndexRelative - 1;
		try {
			isUsed = (1 == Integer.parseInt(usedList[arrayIndex]));
		} catch (Exception e) {
			this.log.error("Unable to parse if channel with index " + channelIndexRelative + " and input string "
					+ isUsedString + " is used. Error: " + e.getMessage());
		}
		return isUsed;
	}

	/**
	 * Writes the data to the given write channel's hardware port.
	 * 
	 * @param channel RevPiDigitalWriteChannel.
	 * @param value Boolean value to be set.
	 * @throws IOException if reading the value fails.
	 */
	private void setDigital(RevPiDigitalWriteChannel channel, boolean value) throws IOException {
		if (this.revPiHardware != null) {
			this.revPiHardware.setDigital(channel.getChannelName(), value);
		}
	}
}
