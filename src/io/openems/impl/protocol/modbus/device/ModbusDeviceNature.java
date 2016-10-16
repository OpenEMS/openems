package io.openems.impl.protocol.modbus.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.bridge.ModbusBridge;
import io.openems.impl.protocol.modbus.internal.Element;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.ModbusRange;
import io.openems.impl.protocol.modbus.internal.WordElement;

public abstract class ModbusDeviceNature implements DeviceNature {

	private static Logger log = LoggerFactory.getLogger(ModbusDeviceNature.class);

	private ModbusProtocol protocol = null;
	private final String thingId;

	public ModbusDeviceNature(String thingId) {
		this.thingId = thingId;
	}

	// TODO make protected
	public ModbusProtocol getProtocol() throws OpenemsModbusException {
		if (protocol == null) {
			this.protocol = defineModbusProtocol();
		}
		return protocol;
	}

	@Override
	public String getThingId() {
		return thingId;
	}

	protected abstract ModbusProtocol defineModbusProtocol() throws OpenemsModbusException;

	protected void update(int modbusUnitId, ModbusBridge modbusBridge) {
		ModbusTransaction trans;
		try {
			trans = modbusBridge.getTransaction();
		} catch (OpenemsModbusException e) {
			log.error(e.getMessage());
			for (ModbusRange range : protocol.getRequiredRanges()) {
				for (Element element : range.getElements()) {
					element.setValue(null);
				}
			}
			return;
		}
		log.info("Update ModbusDevice: " + trans + " - Protocol: " + protocol);
		/**
		 * Update required ranges
		 */
		for (ModbusRange range : protocol.getRequiredRanges()) {
			log.info("Range: " + range.getStartAddress());
			try {
				// Query using this Range
				Register[] registers;

				registers = modbusBridge.query(modbusUnitId, range);

				// Fill channels
				int position = 0;
				for (Element element : range.getElements()) {
					int length = element.getLength();
					if (element instanceof WordElement) {
						// Use _one_ Register for the element
						((WordElement) element).setValue(registers[position]);
					}
					// TODO
					// else if (element instanceof DoublewordElement) {
					// ((DoublewordElement) element).update(registers[position], registers[position + 1]);
					// }
					position += length;
				}
			} catch (OpenemsModbusException e) {
				log.error(
						"Modbus query failed. " //
								+ "Bridge [" + modbusBridge + "], Range [" + range.getStartAddress() + "]: {}",
						e.getMessage());
				// set all elements to invalid
				for (Element element : range.getElements()) {
					element.setValue(null);
				}
			}
		}
		;
		// TODO
		/**
		 * Update other ranges
		 */
	}
}
