package io.openems.edge.bridge.modbus.impl.internal;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.protocol.Range;
import io.openems.edge.bridge.modbus.protocol.RegisterElement;

public class ReadTask extends Task {

	private final Logger log = LoggerFactory.getLogger(ReadTask.class);
	private final Range range;
	// private DebugChannel<Boolean> rangeRead;

	public ReadTask(String sourceId, int modbusUnitId, Range range) {
		super(sourceId, modbusUnitId);
		this.range = range;
		// this.rangeRead = new DebugChannel<>("Range" + range.getStartAddress() +
		// "Read", bridge);
	}

	public Range getRange() {
		return range;
	}

	// protected void run() {
	// rangeRead.setValue(true);
	// if (range instanceof ModbusCoilRange) {
	// try {
	// // Query using this Range
	// boolean[] coils = modbusBridge.queryCoil(modbusUnitId, range);
	//
	// // Fill channels
	// int position = 0;
	// for (ModbusElement<?> element : range.getElements()) {
	// if (element instanceof CoilElement) {
	// // Use _one_ Register for the element
	// ((CoilElement) element).setValue(coils[position]);
	// } else {
	// log.warn("Element type not defined: Element [" + element.getAddress() + "],
	// Bridge ["
	// + modbusBridge + "], Coil [" + range.getStartAddress() + "]: ");
	// }
	// position += element.getLength();
	// }
	// } catch (OpenemsModbusException e) {
	// log.error("Modbus query failed. " //
	// + "Bridge [" + modbusBridge.id() + "], Coil [" + range.getStartAddress() +
	// "]: {}",
	// e.getMessage());
	// modbusBridge.triggerInitialize();
	// // set all elements to invalid
	// for (ModbusElement<?> element : range.getElements()) {
	// element.setValue(null);
	// }
	// }
	// } else {
	// try {
	//
	// // modbusBridge.query(modbusUnitId, range);
	//
	//
	// } catch (OpenemsModbusException e) {
	// log.error("Modbus query failed. " //
	// + "Bridge [" + modbusBridge.id() + "], Range [" + range.getStartAddress() +
	// "]: {}",
	// e.getMessage());
	// modbusBridge.triggerInitialize();
	// // set all elements to invalid
	// for (ModbusElement<?> element : range.getElements()) {
	// element.setValue(null);
	// }
	// }
	// }
	// rangeRead.setValue(false);
	// }

	@Override
	public String toString() {
		return "ModbusReadTask UnitId: " + this.modbusUnitId + ", StartAddress: " + this.range.getStartAddress()
				+ ", Length: " + this.range.getLength();
	}

	@Override
	protected void _execute(ModbusTCPMaster master) throws ModbusException {
		// Query this Range
		int startAddress = this.range.getStartAddress();
		int length = this.range.getLength();
		Register[] registers = master.readMultipleRegisters(this.modbusUnitId, startAddress, length);

		// Fill elements
		int position = 0;
		for (RegisterElement<?> element : this.range.getElements()) {
			try {
				if (element.isIgnored()) {
					// ignore dummy
				} else {
					element.setInputRegisters(Arrays.copyOfRange(registers, position, position + element.getLength()));
				}
			} catch (OpenemsException e) {
				log.warn("Unable to fill modbus element. UnitId [" + this.modbusUnitId + "] Address [" + startAddress
						+ "] Length [" + length + "]: " + e.getMessage());
			}
			position += element.getLength();
		}
	}

}
