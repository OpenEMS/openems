package io.openems.edge.wago;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;

public class Fieldbus8xxAI extends FieldbusAnalogModule {

	private static final String ID_TEMPLATE = "ANALOG_INPUT_M";

	private final AbstractModbusElement<?>[] inputElements;
	private final AbstractModbusElement<?>[] outputElements;
	private final IntegerReadChannel[] readChannels;

	public Fieldbus8xxAI(Wago parent, int moduleCount, int inputOffset, int outputOffset, int channelsCount) {
		String id = ID_TEMPLATE + moduleCount;

		this.readChannels = new IntegerReadChannel[channelsCount];
		this.inputElements = new AbstractModbusElement<?>[channelsCount];
		for (int i = 0; i < channelsCount; i++) {
			IntegerDoc doc = new IntegerDoc();
			FieldbusAnalogChannelId channelId = new FieldbusAnalogChannelId(id + "_C" + (i + 1), doc);
			IntegerReadChannel channel = parent.addAnalogChannel(channelId);
			this.readChannels[i] = channel;

			AbstractModbusElement<?> element = parent.createAnalogModbusElement(channel.channelId(), inputOffset + i);
			this.inputElements[i] = element;
		}
		this.outputElements = new AbstractModbusElement<?>[] {};
	}

	@Override
	public String getName() {
		return "WAGO I/O 750-496/000-000 " + this.readChannels.length + "-channel digital input module";
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
	public IntegerReadChannel[] getChannels() {
		return this.readChannels;
	}

}
