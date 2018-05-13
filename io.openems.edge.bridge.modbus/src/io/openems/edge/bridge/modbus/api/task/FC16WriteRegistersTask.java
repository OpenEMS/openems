package io.openems.edge.bridge.modbus.api.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.AbstractModbusMaster;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;

/**
 * Implements a Write Holding Registers task, using Modbus function code 16
 * (http://www.simplymodbus.ca/FC16.htm)
 */
public class FC16WriteRegistersTask extends Task implements WriteTask {

	private final Logger log = LoggerFactory.getLogger(FC16WriteRegistersTask.class);

	public FC16WriteRegistersTask(int startAddress, AbstractModbusElement<?>... elements) {
		super(startAddress, Priority.HIGH /* Write Tasks always have HIGH priority */, elements);
	}

	private class CombinedWriteRegisters {
		public final int startAddress;
		private final List<Register> registers = new ArrayList<>();

		public CombinedWriteRegisters(int startAddress) {
			this.startAddress = startAddress;
		}

		public void add(Register... registers) {
			for (Register register : registers) {
				this.registers.add(register);
			}
		}

		public Register[] getRegisters() {
			return this.registers.toArray(new Register[this.registers.size()]);
		}

		public int getLastAddress() {
			return this.startAddress + registers.size() - 1;
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder(
					"address [" + this.startAddress + "/0x" + Integer.toHexString(this.startAddress) + "] values [");
			for (int i = 0; i < this.registers.size(); i++) {
				b.append(this.registers.get(i).getValue());
				if (i < this.registers.size() - 1) {
					b.append(",");
				}
			}
			b.append("]");
			return b.toString();
		}
	}

	@Override
	public void executeWrite(AbstractModbusMaster master) throws ModbusException {
		List<CombinedWriteRegisters> writes = mergeWriteRegisters();
		// Execute combined writes
		for (CombinedWriteRegisters write : writes) {
			master.writeMultipleRegisters(this.getUnitId(), write.startAddress, write.getRegisters());
		}
	}

	/**
	 * Combine WriteRegisters without holes inbetween
	 * 
	 * @return
	 */
	private List<CombinedWriteRegisters> mergeWriteRegisters() {
		List<CombinedWriteRegisters> writes = new ArrayList<>();
		ModbusElement<?>[] elements = this.getElements();
		for (int i = 0; i < elements.length; i++) {
			ModbusElement<?> element = elements[i];
			if (element instanceof ModbusRegisterElement) {
				Optional<Register[]> valueOpt = ((ModbusRegisterElement<?>) element).getNextWriteValueAndReset();
				if (valueOpt.isPresent()) {
					// found value -> add to 'writes'
					CombinedWriteRegisters write;
					if (writes.isEmpty() /* no writes created yet */
							|| writes.get(writes.size() - 1).getLastAddress() + 1 != element
									.getStartAddress() /* there is a hole between last element and current element */) {
						write = new CombinedWriteRegisters(element.getStartAddress());
						writes.add(write);
					} else {
						write = writes.get(writes.size() - 1); // no hole -> combine writes
					}
					write.add(valueOpt.get());
				}
			} else {
				log.warn("Unable to execute Write for ModbusElement [" + element + "]: No ModbusRegisterElement!");
			}
		}
		return writes;
	}

	@Override
	public String toString() {
		return "FC16WriteRegistersTask [start=" + this.getStartAddress() + ", length=" + this.getLength() + "]";
	}
}
