package de.fenecon.femscore.modbus.device;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.Register;

import de.fenecon.femscore.modbus.ModbusConnection;
import de.fenecon.femscore.modbus.protocol.Element;
import de.fenecon.femscore.modbus.protocol.ModbusProtocol;

public abstract class WritableModbusDevice extends ModbusDevice {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(WritableModbusDevice.class);

	private final ModbusProtocol writeProtocol;
	protected final Map<Element<?>, Register[]> writeQueue = new HashMap<Element<?>, Register[]>();

	public WritableModbusDevice(String name, String modbusid, int unitid) {
		super(name, modbusid, unitid);

		// Initialize write protocol
		this.writeProtocol = new ModbusProtocol(); // Write-Protocol
		Set<String> writeElements = getWriteElements();
		if (writeElements != null) {
			// TODO writeElements should be removed from remainingElements also
			for (String id : writeElements) {
				writeProtocol.addElementRange(protocol.getElement(id).getElementRange());
			}
		}
	}

	public void addToWriteQueue(Element<?> element, Register[] registers) {
		writeQueue.put(element, registers);
	}

	public void addToWriteQueue(String id, Register[] registers) {
		Element<?> element = writeProtocol.getElement(id);
		if (element != null) {
			writeQueue.put(element, registers);
		}
	}

	public abstract Set<String> getWriteElements();

	public void executeModbusWrite(ModbusConnection modbusConnection) throws Exception {
		for (Entry<Element<?>, Register[]> entry : writeQueue.entrySet()) {
			// TODO: combine writes to one write
			if (entry.getValue().length > 1) {
				/*
				 * log.info("Writing Multiple " + entry.getKey().getName() +
				 * ", 0x" + Integer.toHexString(entry.getKey().getAddress()) +
				 * ", " + entry.getValue()[0].getValue() + ", " +
				 * entry.getValue()[1].getValue());
				 */
				modbusConnection.write(this.unitid, entry.getKey().getAddress(), entry.getValue());
			} else {
				/*
				 * log.info("Writing Single " + entry.getKey().getName() +
				 * ", 0x" + Integer.toHexString(entry.getKey().getAddress()) +
				 * ", " + entry.getValue()[0].getValue());
				 */
				modbusConnection.write(this.unitid, entry.getKey().getAddress(), entry.getValue()[0]);
			}
		}
	}

	@Override
	public String toString() {
		return "ModbusWritableDevice [name=" + name + ", unitid=" + unitid + "]";
	}
}
