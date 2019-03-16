package io.openems.edge.wago;

import java.util.concurrent.atomic.AtomicInteger;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.channel.internal.BooleanReadChannel;

public class Fieldbus400DI extends FieldbusModule {

	private final static AtomicInteger count = new AtomicInteger(0);
	private final static String ID_TEMPLATE = "DIGITAL_INPUT_M";

	private final AbstractModbusElement<?>[] inputElements;
	private final AbstractModbusElement<?>[] outputElements;
	private final BooleanReadChannel[] readChannels;

	public Fieldbus400DI(Wago parent, int inputOffset, int outputOffset, int channelsCount) {
		String id = ID_TEMPLATE + count.incrementAndGet();

		this.readChannels = new BooleanReadChannel[channelsCount];
		this.inputElements = new AbstractModbusElement<?>[channelsCount];
		for (int i = 0; i < channelsCount; i++) {
			BooleanReadChannel channel = new BooleanReadChannel(parent, new FieldbusChannel(id + "_C" + (i + 1)));
			this.readChannels[i] = channel;
			parent.addChannel(channel);

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
