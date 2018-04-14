package io.openems.edge.bridge.modbus.api.task;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;

/**
 * Implements a Write Holding Registers task, using Modbus function code 16
 * (http://www.simplymodbus.ca/FC16.htm)
 * 
 * @author stefan.feilmeier
 *
 */
public class FC16WriteRegistersTask extends FC3ReadRegistersTask implements WriteTask {

	private final Logger log = LoggerFactory.getLogger(FC16WriteRegistersTask.class);

	public FC16WriteRegistersTask(int startAddress, AbstractModbusElement<?>... elements) {
		super(startAddress, elements);
	}

	@Override
	public void executeWrite(ModbusTCPMaster master) throws ModbusException {
		// TODO find gaps and combine register ranges
		ModbusElement<?>[] elements = this.getElements();
		for (int i = 0; i < elements.length; i++) {
			ModbusElement<?> element = elements[i];
			if (element instanceof ModbusRegisterElement) {
				Optional<Register[]> valueOpt = ((ModbusRegisterElement<?>) element).getNextWriteValueAndReset();
				if (valueOpt.isPresent()) {
					master.writeMultipleRegisters(this.getUnitId(), element.getStartAddress(), valueOpt.get());
				}
			} else {
				log.warn("Unable to execute Write for ModbusElement [" + element + "]: No ModbusRegisterElement!");
			}
		}
	}
}
