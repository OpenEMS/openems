package de.fenecon.femscore.modbus.protocol;

import java.util.Arrays;

import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.net.SerialConnection;

public class ElementRange {
	private int startAddress;
	private Element<?>[] elements;

	public ElementRange(int startAddress, Element<?>... elements) {
		this.startAddress = startAddress;
		this.elements = elements;
		for (Element<?> element : elements) {
			element.setElementRange(this);
		}
	}

	/*
	 * Returns the total number of words (lengths) of all elements
	 */
	public int getTotalLength() {
		int length = 0;
		for (Element<?> element : elements) {
			length += element.getLength();
		}
		return length;
	}

	public ModbusSerialTransaction getModbusSerialTransaction(SerialConnection serialConnection, int unitid) {
		ModbusSerialTransaction modbusSerialTransaction = null;
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(startAddress, getTotalLength());
		req.setUnitID(unitid);
		req.setHeadless();
		modbusSerialTransaction = new ModbusSerialTransaction(serialConnection);
		modbusSerialTransaction.setRequest(req);
		return modbusSerialTransaction;
	}

	public void dispose() {
		// nothing to dispose for now...
	}

	@Override
	public String toString() {
		return "ModbusElementRange [startAddress=" + startAddress + ", words=" + Arrays.toString(elements) + "]";
	}

	public Element<?>[] getElements() {
		return elements;
	}

	public int getStartAddress() {
		return startAddress;
	}
}
