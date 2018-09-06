package io.openems.edge.wagoFB;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.component.OpenemsComponent;

public class WagoFBInput extends AbstractOpenemsModbusComponent implements OpenemsComponent {

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return null;

//		List<FC1ReadCoilsTask> ranges = new ArrayList<>();
//		HashMap<String, List<String>> channels;
//		try {
//			channels = WagoFB.getConfig(ip.value());
//			for (String key : channels.keySet()) {
//				switch (key) {
//				case "DI": {
//					List<CoilElement> elements = new ArrayList<>();
//					int count = 0;
//					for (@SuppressWarnings("unused") String channel : channels.get(key)) {
//						ModbusCoilReadChannel ch = new ModbusCoilReadChannel(Integer.toString(count), this);
//						this.channel.add(ch);
//						elements.add(new CoilElement(count, ch));
//						count++;
//						if (count % 63 == 0) {
//							ranges.add(new ModbusCoilRange(elements.get(0).getAddress(),
//									elements.toArray(new CoilElement[elements.size()])));
//							elements.clear();
//						}
//					}
//					if (this.channel.size() > 0) {
//						ranges.add(new ModbusCoilRange(elements.get(0).getAddress(),
//								elements.toArray(new CoilElement[elements.size()])));
//					}
//				}
//				break;
//				}
//			}
//		} catch (InvalidValueException e) {
//			log.error("Ip-Address is Invalid", e);
//		}
//		ModbusProtocol protocol = new ModbusProtocol(ranges.toArray(new ModbusRange[ranges.size()]));
//		return protocol;
	}
}
