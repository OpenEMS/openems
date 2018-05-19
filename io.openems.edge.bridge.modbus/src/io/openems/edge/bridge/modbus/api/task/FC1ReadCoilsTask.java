package io.openems.edge.bridge.modbus.api.task;

import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;
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

	public void executeQuery(AbstractModbusBridge bridge) throws OpenemsException {
		// Query this Task
		int startAddress = this.getStartAddress();
		int length = this.getLength();
		boolean[] response;
		try {
			/*
			 * First try
			 */
			response = this.readCoils(bridge, this.getUnitId(), startAddress, length);
		} catch (OpenemsException | ModbusException e) {
			/*
			 * Second try: with new connection
			 */
			bridge.closeModbusConnection();
			try {
				response = this.readCoils(bridge, this.getUnitId(), startAddress, length);
			} catch (ModbusException e2) {
				throw new OpenemsException("Transaction failed: " + e.getMessage(), e2);
			}
		}

		// Verify response length
		if (response.length < length) {
			throw new OpenemsException(
					"Received message is too short. Expected [" + length + "], got [" + response.length + "]");
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
						element.setInputCoil(response[position]);
					}
				} catch (OpenemsException e) {
					log.warn("Unable to fill modbus element. UnitId [" + this.getUnitId() + "] Address [" + startAddress
							+ "] Length [" + length + "]: " + e.getMessage());
				}
			}
			position++;
		}
	}

	private boolean[] readCoils(AbstractModbusBridge bridge, int unitId, int startAddress, int length)
			throws OpenemsException, ModbusException {
		ModbusTransaction transaction = bridge.getNewModbusTransaction();
		ReadCoilsRequest request = new ReadCoilsRequest(startAddress, length);
		request.setUnitID(unitId);
		transaction.setRequest(request);
		transaction.execute();
		ModbusResponse response = transaction.getResponse();
		if (response instanceof ReadCoilsResponse) {
			ReadCoilsResponse coilsResponse = (ReadCoilsResponse) response;
			return toBooleanArray(coilsResponse.getCoils().getBytes());
		} else {
			throw new OpenemsException("Unexpected Modbus response. Expected [ReadCoilsResponse], got ["
					+ response.getClass().getSimpleName() + "]");
		}
	}

	@Override
	public String toString() {
		return "FC1 Read Coils [" + this.getStartAddress() + "/0x" + Integer.toHexString(this.getStartAddress())
				+ ";length=" + this.getLength() + "]";
	}

	/*
	 * Static Methods
	 */
	static boolean[] toBooleanArray(byte[] bytes) {
		BitSet bits = BitSet.valueOf(bytes);
		boolean[] bools = new boolean[bytes.length * 8];
		for (int i = bits.nextSetBit(0); i != -1; i = bits.nextSetBit(i + 1)) {
			bools[i] = true;
		}
		return bools;
	}
}
