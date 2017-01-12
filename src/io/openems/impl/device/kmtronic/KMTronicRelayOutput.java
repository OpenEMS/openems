package io.openems.impl.device.kmtronic;

import io.openems.api.device.nature.io.OutputNature;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.modbus.ModbusCoilWriteChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.CoilElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusCoilRange;

public class KMTronicRelayOutput extends ModbusDeviceNature implements OutputNature {

	private ModbusCoilWriteChannel[] outputs;

	public KMTronicRelayOutput(String thingId) throws ConfigException {
		super(thingId);
	}

	@Override protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		if (outputs == null) {
			outputs = new ModbusCoilWriteChannel[8];
		}
		return new ModbusProtocol(
				new WriteableModbusCoilRange(0, new CoilElement(0, outputs[0] = new ModbusCoilWriteChannel("1", this))),
				new WriteableModbusCoilRange(1, new CoilElement(1, outputs[1] = new ModbusCoilWriteChannel("2", this))),
				new WriteableModbusCoilRange(2, new CoilElement(2, outputs[2] = new ModbusCoilWriteChannel("3", this))),
				new WriteableModbusCoilRange(3, new CoilElement(3, outputs[3] = new ModbusCoilWriteChannel("4", this))),
				new WriteableModbusCoilRange(4, new CoilElement(4, outputs[4] = new ModbusCoilWriteChannel("5", this))),
				new WriteableModbusCoilRange(5, new CoilElement(5, outputs[5] = new ModbusCoilWriteChannel("6", this))),
				new WriteableModbusCoilRange(6, new CoilElement(6, outputs[6] = new ModbusCoilWriteChannel("7", this))),
				new WriteableModbusCoilRange(7,
						new CoilElement(7, outputs[7] = new ModbusCoilWriteChannel("8", this))));
	}

	@Override public ModbusCoilWriteChannel[] setOutput() {
		return outputs;
	}

}
