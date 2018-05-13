package io.openems.edge.bridge.modbus.api.task;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.AbstractModbusMaster;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * Implements a Write Single Coil task, using Modbus function code 5
 * (http://www.simplymodbus.ca/FC05.htm)
 */
public class FC5WriteCoilTask extends Task implements WriteTask {

	private final Logger log = LoggerFactory.getLogger(FC5WriteCoilTask.class);

	public FC5WriteCoilTask(int startAddress, AbstractModbusElement<?> element) {
		super(startAddress, Priority.HIGH /* Write Tasks always have HIGH priority */, element);
	}

	@Override
	public void executeWrite(AbstractModbusMaster master) throws ModbusException {
		ModbusElement<?> element = this.getElements()[0];
		if (element instanceof ModbusCoilElement) {
			Optional<Boolean> valueOpt = ((ModbusCoilElement) element).getNextWriteValueAndReset();
			if (valueOpt.isPresent()) {
				// found value -> write
				master.writeCoil(this.getUnitId(), this.getStartAddress(), valueOpt.get());
			}
		} else {
			log.warn("Unable to execute Write for ModbusElement [" + element + "]: No ModbusCoilElement!");
		}
	}

	@Override
	public String toString() {
		return "FC5WriteCoilTask [address=" + this.getStartAddress() + "]";
	}
}
