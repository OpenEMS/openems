package io.openems.impl.protocol.modbus.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.internal.ModbusProtocol;
import io.openems.impl.protocol.modbus.internal.Range;

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

	public void update(ModbusTransaction modbusTransaction) {
		log.info("Update ModbusDevice: " + modbusTransaction + " - Protocol: " + protocol);
		/**
		 * Update required ranges
		 */
		for (Range range : protocol.getRequiredRanges()) {
			log.info("Range: " + range.getStartAddress());

		}
		;
		// TODO
		/**
		 * Update other ranges
		 */
	}

	protected abstract ModbusProtocol defineModbusProtocol() throws OpenemsModbusException;
}
