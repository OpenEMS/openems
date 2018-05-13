package io.openems.edge.bridge.modbus.api.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.AbstractModbusMaster;
import com.ghgande.j2mod.modbus.util.BitVector;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * Implements a Read Coils task, implementing Modbus function code 1
 * (http://www.simplymodbus.ca/FC01.htm)
 */
public class FC1ReadCoilsTask extends Task implements ReadTask {

	private final Logger log = LoggerFactory.getLogger(FC1ReadCoilsTask.class);

	public FC1ReadCoilsTask(int startAddress, Priority priority, AbstractModbusElement<?>... elements) {
		super(startAddress, priority, elements);
	}

	public void executeQuery(AbstractModbusMaster master) throws ModbusException {
		// Query this Task
		int startAddress = this.getStartAddress();
		int length = this.getLength();
		BitVector bits;
		try {
			bits = master.readCoils(this.getUnitId(), startAddress, length);
		} catch (ClassCastException e) {
			log.warn("FC1 Read Coils failed [" + startAddress + "/0x" + Integer.toHexString(startAddress) + "]: "
					+ e.getMessage());
			return;
		}
		// Fill elements
		int position = 0;
		for (ModbusElement<?> modbusElement : this.getElements()) {
			if (!(modbusElement instanceof ModbusCoilElement)) {
				log.error("A ModbusCoilElement is required for a FC1ReadCoilsTask! Element [" + modbusElement + "]");
			} else {
				// continue with correctly casted ModbusCoilElement
				ModbusCoilElement element = (ModbusCoilElement) modbusElement;
				try {
					if (element.isIgnored()) {
						// ignore dummy
					} else {
						element.setInputCoil(bits.getBit(position));
					}
				} catch (OpenemsException e) {
					log.warn("Unable to fill modbus element. UnitId [" + this.getUnitId() + "] Address [" + startAddress
							+ "] Length [" + length + "]: " + e.getMessage());
				}
			}
			position++;
		}
	}

	@Override
	public String toString() {
		return "FC3ReadRegistersTask [startAddress=" + this.getStartAddress() + ", length=" + this.getLength() + "]";
	}
}
