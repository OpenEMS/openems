package io.openems.edge.wago;

import java.util.concurrent.atomic.AtomicInteger;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.channel.BooleanReadChannel;

public class Fieldbus400DI2Ch extends FieldbusModule {

	private final static AtomicInteger count = new AtomicInteger(0);
	private final static String ID_TEMPLATE = "DIGITAL_INPUT_M";

	private final AbstractModbusElement<?>[] inputElements;
	private final AbstractModbusElement<?>[] outputElements;
	private final BooleanReadChannel[] readChannels;

	public Fieldbus400DI2Ch(Wago parent, int inputOffset, int outputOffset) {
		String id = ID_TEMPLATE + count.incrementAndGet();

		BooleanReadChannel channel1 = new BooleanReadChannel(parent, new FieldbusChannel(id + "_C1"));
		BooleanReadChannel channel2 = new BooleanReadChannel(parent, new FieldbusChannel(id + "_C2"));
		this.readChannels = new BooleanReadChannel[] { channel1, channel2 };

		parent.addChannel(channel1);
		parent.addChannel(channel2);

		this.inputElements = new AbstractModbusElement<?>[] { //
				parent.createModbusElement(channel1.channelId(), inputOffset), //
				parent.createModbusElement(channel2.channelId(), inputOffset + 1), //
		};

		this.outputElements = new AbstractModbusElement<?>[] {};
	}

	@Override
	public String getName() {
		return "WAGO I/O 750-400 2-channel digital input module";
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
		return 2;
	}

	@Override
	public BooleanReadChannel[] getChannels() {
		return this.readChannels;
	}

}
