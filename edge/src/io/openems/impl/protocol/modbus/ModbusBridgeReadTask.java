package io.openems.impl.protocol.modbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.api.bridge.BridgeReadTask;
import io.openems.api.channel.DebugChannel;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.internal.CoilElement;
import io.openems.impl.protocol.modbus.internal.DoublewordElement;
import io.openems.impl.protocol.modbus.internal.DummyElement;
import io.openems.impl.protocol.modbus.internal.WordElement;
import io.openems.impl.protocol.modbus.internal.range.ModbusCoilRange;
import io.openems.impl.protocol.modbus.internal.range.ModbusRange;

public class ModbusBridgeReadTask extends BridgeReadTask {

	private int modbusUnitId;
	private ModbusBridge modbusBridge;
	private ModbusRange range;
	protected final Logger log;
	private DebugChannel<Boolean> rangeRead;

	public ModbusBridgeReadTask(int modbusUnitId, ModbusBridge bridge, ModbusRange range) {
		log = LoggerFactory.getLogger(this.getClass());
		this.modbusUnitId = modbusUnitId;
		this.modbusBridge = bridge;
		this.range = range;
		this.rangeRead = new DebugChannel<>("Range" + range.getStartAddress() + "Read", bridge);
	}

	public ModbusRange getRange() {
		return range;
	}

	@Override
	protected void run() {
		rangeRead.setValue(true);
		if (range instanceof ModbusCoilRange) {
			try {
				// Query using this Range
				boolean[] coils = modbusBridge.queryCoil(modbusUnitId, range);

				// Fill channels
				int position = 0;
				for (ModbusElement<?> element : range.getElements()) {
					if (element instanceof CoilElement) {
						// Use _one_ Register for the element
						((CoilElement) element).setValue(coils[position]);
					} else {
						log.warn("Element type not defined: Element [" + element.getAddress() + "], Bridge ["
								+ modbusBridge + "], Coil [" + range.getStartAddress() + "]: ");
					}
					position += element.getLength();
				}
			} catch (OpenemsModbusException e) {
				log.error("Modbus query failed. " //
						+ "Bridge [" + modbusBridge.id() + "], Coil [" + range.getStartAddress() + "]: {}",
						e.getMessage());
				modbusBridge.triggerInitialize();
				// set all elements to invalid
				for (ModbusElement<?> element : range.getElements()) {
					element.setValue(null);
				}
			}
		} else {
			try {
				// Query using this Range
				InputRegister[] registers = modbusBridge.query(modbusUnitId, range);

				// Fill channels
				int position = 0;
				for (ModbusElement<?> element : range.getElements()) {
					if (element instanceof DummyElement) {
						// ignore dummy
					} else if (element instanceof WordElement) {
						// Use _one_ Register for the element
						((WordElement) element).setValue(registers[position]);
					} else if (element instanceof DoublewordElement) {
						// Use _two_ registers for the element
						((DoublewordElement) element).setValue(registers[position], registers[position + 1]);
					} else {
						log.warn("Element type not defined: Element [" + element.getAddress() + "], Bridge ["
								+ modbusBridge + "], Range [" + range.getStartAddress() + "]: ");
					}
					position += element.getLength();
				}
			} catch (OpenemsModbusException e) {
				log.error("Modbus query failed. " //
						+ "Bridge [" + modbusBridge.id() + "], Range [" + range.getStartAddress() + "]: {}",
						e.getMessage());
				modbusBridge.triggerInitialize();
				// set all elements to invalid
				for (ModbusElement<?> element : range.getElements()) {
					element.setValue(null);
				}
			}
		}
		rangeRead.setValue(false);
	}

}
