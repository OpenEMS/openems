package io.openems.edge.wago;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;

public class Fieldbus523RO1Ch extends FieldbusModule {

	private static final String ID_TEMPLATE = "RELAY_M";

	private final CoilElement[] inputCoil0Elements;
	private final CoilElement[] inputCoil512Elements;
	private final CoilElement[] outputCoil512Elements;
	private final BooleanReadChannel[] readChannels;

	public Fieldbus523RO1Ch(IoWagoImpl parent, int moduleCount, int coilOffset0, int coilOffset512) {
		var id = ID_TEMPLATE + moduleCount;

		BooleanWriteChannel channel1;
		{
			var doc = new BooleanDoc() //
					.accessMode(AccessMode.READ_WRITE);
			doc.persistencePriority(PersistencePriority.HIGH);
			var channelId = new FieldbusChannelId(id, doc);
			channel1 = (BooleanWriteChannel) parent.addChannel(channelId);
		}
		BooleanReadChannel channel2;
		{
			OpenemsTypeDoc<Boolean> doc = new BooleanDoc();
			var channelId = new FieldbusChannelId(id + "_HAND", doc);
			channel2 = parent.addChannel(channelId);
		}
		this.readChannels = new BooleanReadChannel[] { channel1, channel2 };

		this.inputCoil0Elements = new CoilElement[] { //
				parent.createModbusCoilElement(channel2.channelId(), coilOffset0), //
				new CoilElement(coilOffset0 + 1) //
		};
		this.inputCoil512Elements = new CoilElement[] { //
				parent.createModbusCoilElement(channel1.channelId(), coilOffset512), //
				new CoilElement(coilOffset512 + 1), //
		};
		this.outputCoil512Elements = new CoilElement[] { //
				parent.createModbusCoilElement(channel1.channelId(), coilOffset512), //
				new CoilElement(coilOffset512 + 1) //
		};
	}

	@Override
	public String getName() {
		return "WAGO I/O 750-523 1-channel relay output module";
	}

	@Override
	public CoilElement[] getInputCoil0Elements() {
		return this.inputCoil0Elements;
	}

	@Override
	public CoilElement[] getInputCoil512Elements() {
		return this.inputCoil512Elements;
	}

	@Override
	public CoilElement[] getOutputCoil512Elements() {
		return this.outputCoil512Elements;
	}

	@Override
	public BooleanReadChannel[] getChannels() {
		return this.readChannels;
	}
}
