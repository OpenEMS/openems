package io.openems.impl.device.wago;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.io.OutputNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.InvalidValueException;
import io.openems.impl.protocol.modbus.ModbusCoilWriteChannel;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;
import io.openems.impl.protocol.modbus.internal.CoilElement;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.range.ModbusCoilRange;
import io.openems.impl.protocol.modbus.internal.range.ModbusRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusCoilRange;

public class WagoFBOutput extends ModbusDeviceNature implements OutputNature {

	@ConfigInfo(title = "Ip-Address to download the wago configuration", type = Inet4Address.class)
	public ConfigChannel<Inet4Address> ip = new ConfigChannel<Inet4Address>("ip", this);

	private List<ModbusCoilWriteChannel> channel = new ArrayList<>();

	public WagoFBOutput(String thingId) throws ConfigException {
		super(thingId);
	}

	@Override
	public ModbusCoilWriteChannel[] setOutput() {
		return channel.toArray(new ModbusCoilWriteChannel[channel.size()]);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws ConfigException {
		List<ModbusCoilRange> ranges = new ArrayList<>();
		HashMap<String, List<String>> channels;
		try {
			channels = WagoFB.getConfig(ip.value());
			for (String key : channels.keySet()) {
				switch (key) {
				case "DO": {
					List<CoilElement> elements = new ArrayList<>();
					int count = 0;
					for (String channel : channels.get(key)) {
						ModbusCoilWriteChannel ch = new ModbusCoilWriteChannel(Integer.toString(count), this);
						this.channel.add(ch);
						elements.add(new CoilElement(512 + count, ch));
						count++;
						if (count % 63 == 0) {
							ranges.add(new ModbusCoilRange(elements.get(0).getAddress(),
									elements.toArray(new CoilElement[elements.size()])));
							elements.clear();
						}
					}
					if (this.channel.size() > 0) {
						ranges.add(new WriteableModbusCoilRange(elements.get(0).getAddress(),
								elements.toArray(new CoilElement[elements.size()])));
					}
				}
					break;
				}
			}
		} catch (InvalidValueException e) {
			log.error("Ip-Address is Invalid", e);
		}
		ModbusProtocol protocol = new ModbusProtocol(ranges.toArray(new ModbusRange[ranges.size()]));
		return protocol;
	}

}
