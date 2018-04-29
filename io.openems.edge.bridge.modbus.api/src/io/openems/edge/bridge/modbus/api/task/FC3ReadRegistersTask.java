package io.openems.edge.bridge.modbus.api.task;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.bridge.modbus.facade.MyModbusMaster;

/**
 * Implements a Read Holding Register task, implementing Modbus function code 3
 * (http://www.simplymodbus.ca/FC03.htm)
 * 
 * @author stefan.feilmeier
 *
 */
public class FC3ReadRegistersTask extends Task implements ReadTask {

	private final Logger log = LoggerFactory.getLogger(FC3ReadRegistersTask.class);

	public FC3ReadRegistersTask(int startAddress, Priority priority, AbstractModbusElement<?>... elements) {
		super(startAddress, priority, elements);
	}

	public void executeQuery(MyModbusMaster master) throws ModbusException {
		// Query this Task
		int startAddress = this.getStartAddress();
		int length = this.getLength();
		Register[] registers;
		// log.debug("FC3 Read Holding Registers [" + startAddress + "/0x" +
		// Integer.toHexString(startAddress) + "]");
		try {
			registers = master.readMultipleRegisters(this.getUnitId(), startAddress, length);
		} catch (ClassCastException e) {
			log.warn("FC3 Read Holding Registers failed [" + startAddress + "/0x" + Integer.toHexString(startAddress)
					+ "]: " + e.getMessage());
			return;
		}
		// Fill elements
		int position = 0;
		for (ModbusElement<?> modbusElement : this.getElements()) {
			if (!(modbusElement instanceof ModbusRegisterElement)) {
				log.error("A ModbusRegisterElement is required for a FC3ReadHoldingRegisterTask! Element ["
						+ modbusElement + "]");
			} else {
				// continue with correctly casted ModbusRegisterElement
				ModbusRegisterElement<?> element = (ModbusRegisterElement<?>) modbusElement;
				try {
					if (element.isIgnored()) {
						// ignore dummy
					} else {
						element.setInputRegisters(
								Arrays.copyOfRange(registers, position, position + element.getLength()));
					}
				} catch (OpenemsException e) {
					log.warn("Unable to fill modbus element. UnitId [" + this.getUnitId() + "] Address [" + startAddress
							+ "] Length [" + length + "]: " + e.getMessage());
				}
			}
			position += modbusElement.getLength();
		}
	}

	@Override
	public String toString() {
		return "FC3ReadRegistersTask [startAddress=" + this.getStartAddress() + ", length=" + this.getLength() + "]";
	}
}
