package io.openems.impl.protocol.modbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.api.bridge.BridgeWriteTask;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.internal.CoilElement;
import io.openems.impl.protocol.modbus.internal.DoublewordElement;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.WordElement;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusCoilRange;
import io.openems.impl.protocol.modbus.internal.range.WriteableModbusRange;

public class ModbusBridgeWriteTask extends BridgeWriteTask {

	private int modbusUnitId;
	private ModbusBridge modbusBridge;
	private WriteableModbusRange range;
	protected final Logger log;

	public ModbusBridgeWriteTask(int modbusUnitId, ModbusBridge bridge, WriteableModbusRange range) {
		log = LoggerFactory.getLogger(this.getClass());
		this.modbusUnitId = modbusUnitId;
		this.modbusBridge = bridge;
		this.range = range;
	}

	@Override
	protected void run() {
		if (range instanceof WriteableModbusCoilRange) {
			/*
			 * Build a list of start-addresses of elements without holes. The start-addresses map to a list
			 * of elements and their values (sorted by order of insertion using LinkedHashMap)
			 */
			LinkedHashMap<Integer, LinkedHashMap<ModbusElement<?>, Boolean>> elements = new LinkedHashMap<>();
			Integer nextStartAddress = null;
			LinkedHashMap<ModbusElement<?>, Boolean> values = new LinkedHashMap<ModbusElement<?>, Boolean>();
			for (ModbusElement<?> element : range.getElements()) {
				// Test if Channel is a dummy or writable and receive write value
				boolean value = false;
				if (element instanceof DummyElement) {
					value = false;
				} else if (element.getChannel() != null && element.getChannel() instanceof ModbusCoilWriteChannel) {
					Optional<Boolean> valueOptional = ((ModbusCoilWriteChannel) element.getChannel()).writeShadowCopy();
					if (valueOptional.isPresent()) {
						value = valueOptional.get();
					} else {
						continue;
					}
				} else {
					// no dummy, no WriteChannel, no value -> try next element
					continue;
				}
				// Test if there is a "hole", if yes -> create new values map
				if (nextStartAddress == null || nextStartAddress != element.getAddress()) {
					values = new LinkedHashMap<ModbusElement<?>, Boolean>();
					elements.put(element.getAddress(), values);
				}
				// store this element and the value
				values.put(element, value);
				// store for next run
				nextStartAddress = element.getAddress() + element.getLength();
			}

			/*
			 * Write all elements
			 */
			for (Entry<Integer, LinkedHashMap<ModbusElement<?>, Boolean>> entry : elements.entrySet()) {
				/*
				 * Get start address and all registers
				 */
				int address = entry.getKey();
				List<Boolean> coils = new ArrayList<>();
				for (Entry<ModbusElement<?>, Boolean> value : entry.getValue().entrySet()) {
					ModbusElement<?> element = value.getKey();
					if (element instanceof CoilElement) {
						coils.add(value.getValue());
					} else { // DummyElement -> write false;
						for (int i = 0; i < element.getLength(); i++) {
							coils.add(false);
						}
					}
				}
				/*
				 * Write
				 */
				try {
					modbusBridge.writeCoil(modbusUnitId, address, coils);
				} catch (OpenemsModbusException e) {
					log.error("Bridge [" + modbusBridge.id() + "], Range [" + range.getStartAddress() + "/0x"
							+ Integer.toHexString(range.getStartAddress()) + "]: " + e.getMessage());
					modbusBridge.triggerInitialize();
				}
			}
		} else {
			/*
			 * Build a list of start-addresses of elements without holes. The start-addresses map to a list
			 * of elements and their values (sorted by order of insertion using LinkedHashMap)
			 */
			LinkedHashMap<Integer, LinkedHashMap<ModbusElement<?>, Long>> elements = new LinkedHashMap<>();
			Integer nextStartAddress = null;
			LinkedHashMap<ModbusElement<?>, Long> values = new LinkedHashMap<ModbusElement<?>, Long>();
			;
			for (ModbusElement<?> element : range.getElements()) {
				// Test if Channel is a dummy or writable and receive write value
				long value = 0L;
				if (element instanceof DummyElement) {
					value = 0L;
				} else if (element.getChannel() != null && element.getChannel() instanceof ModbusWriteLongChannel) {
					Optional<Long> valueOptional = ((ModbusWriteLongChannel) element.getChannel()).writeShadowCopy();
					if (valueOptional.isPresent()) {
						value = valueOptional.get();
					} else {
						continue;
					}
				} else {
					// no dummy, no WriteChannel, no value -> try next element
					continue;
				}
				// Test if there is a "hole", if yes -> create new values map
				if (nextStartAddress == null || nextStartAddress != element.getAddress()) {
					values = new LinkedHashMap<ModbusElement<?>, Long>();
					elements.put(element.getAddress(), values);
				}
				// store this element and the value
				values.put(element, value);
				// store for next run
				nextStartAddress = element.getAddress() + element.getLength();
			}

			/*
			 * Write all elements
			 */
			for (Entry<Integer, LinkedHashMap<ModbusElement<?>, Long>> entry : elements.entrySet()) {
				/*
				 * Get start address and all registers
				 */
				int address = entry.getKey();
				List<Register> registers = new ArrayList<>();
				for (Entry<ModbusElement<?>, Long> value : entry.getValue().entrySet()) {
					ModbusElement<?> element = value.getKey();
					if (element instanceof WordElement) {
						registers.add( //
								((WordElement) element).toRegister(value.getValue()));
					} else if (element instanceof DoublewordElement) {
						registers.addAll(Arrays.asList( //
								((DoublewordElement) element).toRegisters(value.getValue())));
					} else { // DummyElement -> write 0;
						for (int i = 0; i < element.getLength(); i++) {
							registers.add(new SimpleRegister(value.getValue().intValue()));
						}
					}
				}
				/*
				 * Write
				 */
				try {
					modbusBridge.write(modbusUnitId, address, registers);
				} catch (OpenemsModbusException e) {
					log.error("Bridge [" + modbusBridge.id() + "], Range [" + range.getStartAddress() + "/0x"
							+ Integer.toHexString(range.getStartAddress()) + "]: " + e.getMessage());
					modbusBridge.triggerInitialize();
				}
			}
		}
	}

}
