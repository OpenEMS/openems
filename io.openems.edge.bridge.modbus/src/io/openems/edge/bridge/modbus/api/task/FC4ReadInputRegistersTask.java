package io.openems.edge.bridge.modbus.api.task;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;

/**
 * Implements a Read Input Register task, implementing Modbus function code 4
 * (http://www.simplymodbus.ca/FC04.htm)
 */
public class FC4ReadInputRegistersTask extends Task implements ReadTask {

	private final Logger log = LoggerFactory.getLogger(FC4ReadInputRegistersTask.class);

	public FC4ReadInputRegistersTask(int startAddress, Priority priority, AbstractModbusElement<?>... elements) {
		super(startAddress, priority, elements);
	}

	public void executeQuery(AbstractModbusBridge bridge) throws OpenemsException {
		// Query this Task
		int startAddress = this.getStartAddress();
		int length = this.getLength();
		InputRegister[] response;
		try {
			/*
			 * First try
			 */
			response = this.readRegisters(bridge, this.getUnitId(), startAddress, length);
		} catch (OpenemsException | ModbusException e) {
			/*
			 * Second try: with new connection
			 */
			bridge.closeModbusConnection();
			try {
				response = this.readRegisters(bridge, this.getUnitId(), startAddress, length);
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
								Arrays.copyOfRange(response, position, position + element.getLength()));
					}
				} catch (OpenemsException e) {
					log.warn("Unable to fill modbus element. UnitId [" + this.getUnitId() + "] Address [" + startAddress
							+ "] Length [" + length + "]: " + e.getMessage());
				}
			}
			position += modbusElement.getLength();
		}
	}

	private InputRegister[] readRegisters(AbstractModbusBridge bridge, int unitId, int startAddress, int length)
			throws OpenemsException, ModbusException {
		ModbusTransaction transaction = bridge.getNewModbusTransaction();
		ReadInputRegistersRequest request = new ReadInputRegistersRequest(startAddress, length);
		request.setUnitID(unitId);
		transaction.setRequest(request);
		transaction.execute();
		ModbusResponse response = transaction.getResponse();
		if (response instanceof ReadInputRegistersResponse) {
			ReadInputRegistersResponse registersResponse = (ReadInputRegistersResponse) response;
			return registersResponse.getRegisters();
		} else {
			throw new OpenemsException("Unexpected Modbus response. Expected [ReadMultipleRegistersResponse], got ["
					+ response.getClass().getSimpleName() + "]");
		}
	}

	@Override
	public String toString() {
		return "FC3 Read Registers [" + this.getStartAddress() + "/0x" + Integer.toHexString(this.getStartAddress())
				+ ";length=" + this.getLength() + "]";
	}
}
