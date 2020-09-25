package io.openems.edge.wago;

import io.openems.common.channel.AccessMode;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;

public class Fieldbus5xxDO extends FieldbusModule {

	private static final String ID_TEMPLATE = "DIGITAL_OUTPUT_M";

	private final AbstractModbusElement<?>[] inputElements;
	private final AbstractModbusElement<?>[] outputElements;
	private final BooleanReadChannel[] readChannels;

	public Fieldbus5xxDO(Wago parent, int moduleCount, int inputOffset, int outputOffset, int channelsCount) {
		String id = ID_TEMPLATE + moduleCount;

		this.readChannels = new BooleanReadChannel[channelsCount];
		this.inputElements = new AbstractModbusElement<?>[channelsCount];
		this.outputElements = new AbstractModbusElement<?>[channelsCount];

		for (int i = 0; i < channelsCount; i++) {
			OpenemsTypeDoc<Boolean> doc = new BooleanDoc() //
					.accessMode(AccessMode.READ_WRITE);
			FieldbusChannelId channelId = new FieldbusChannelId(id + "_C" + (i + 1), doc);
			BooleanWriteChannel channel = (BooleanWriteChannel) parent.addChannel(channelId);

			this.readChannels[i] = channel;

			this.inputElements[i] = parent.createModbusElement(channel.channelId(), outputOffset + i);
			this.outputElements[i] = parent.createModbusElement(channel.channelId(), outputOffset + i);
		}
	}

	@Override
	public String getName() {
		return "WAGO I/O 750-5xx digital output module";
	}

	@Override
	public AbstractModbusElement<?>[] getInputElements() {
		return this.inputElements;
	}

	@Override
	public AbstractModbusElement<?>[] getOutputElements() {
		return this.outputElements;
	}

	@Override
	public int getOutputCoils() {
		return this.outputElements.length;
	}

	@Override
	public int getInputCoils() {
		return this.inputElements.length;
	}

	@Override
	public BooleanReadChannel[] getChannels() {
		return this.readChannels;
	}
}
