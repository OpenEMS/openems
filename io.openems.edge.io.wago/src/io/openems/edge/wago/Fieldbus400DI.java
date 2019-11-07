package io.openems.edge.wago;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;

public class Fieldbus400DI extends FieldbusModule {


	private static final String ID_TEMPLATE = "DIGITAL_INPUT_M";
	private final AbstractModbusElement<?>[] inputElements;
	private final AbstractModbusElement<?>[] outputElements;
	private final BooleanReadChannel[] readChannels;

	public Fieldbus400DI(Wago parent, int moduleCount, int inputOffset, int outputOffset, int channelsCount) {
		String id = ID_TEMPLATE + moduleCount;

		this.readChannels = new BooleanReadChannel[channelsCount];
		this.inputElements = new AbstractModbusElement<?>[channelsCount];
		for (int i = 0; i < channelsCount; i++) {
			BooleanDoc doc = new BooleanDoc();
			FieldbusChannelId channelId = new FieldbusChannelId(id + "_C" + (i + 1), doc);
			BooleanReadChannel channel = parent.addChannel(channelId);
			this.readChannels[i] = channel;

			AbstractModbusElement<?> element = parent.createModbusElement(channel.channelId(), inputOffset + i);
			this.inputElements[i] = element;
		}
		this.outputElements = new AbstractModbusElement<?>[] {};
	}

	@Override
	public String getName() {
		return "WAGO I/O 750-400 " + this.readChannels.length + "-channel digital input module";
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
		return 0;
	}

	@Override
	public int getInputCoils() {
		return this.readChannels.length;
	}

	@Override
	public BooleanReadChannel[] getChannels() {
		return this.readChannels;
	}

}
