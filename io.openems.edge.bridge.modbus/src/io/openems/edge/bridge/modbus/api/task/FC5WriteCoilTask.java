package io.openems.edge.bridge.modbus.api.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteCoilResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * Implements a Write Single Coil Task, using Modbus function code 5
 * (http://www.simplymodbus.ca/FC05.htm).
 */
public class FC5WriteCoilTask extends AbstractTask implements WriteTask {

	private final Logger log = LoggerFactory.getLogger(FC5WriteCoilTask.class);

	public FC5WriteCoilTask(int startAddress, ModbusCoilElement element) {
		super(startAddress, element);
	}

	@Override
	public int execute(AbstractModbusBridge bridge) throws OpenemsException {
		var noOfWrittenCoils = 0;
		ModbusElement<?> element = this.getElements()[0];
		if (element instanceof ModbusCoilElement) {
			var valueOpt = ((ModbusCoilElement) element).getNextWriteValueAndReset();
			if (valueOpt.isPresent()) {
				// found value -> write
				boolean value = valueOpt.get();
				try {
					/*
					 * First try
					 */
					this.writeCoil(bridge, this.getParent().getUnitId(), this.getStartAddress(), value);
					noOfWrittenCoils = 1;
				} catch (OpenemsException | ModbusException e) {
					/*
					 * Second try: with new connection
					 */
					bridge.closeModbusConnection();
					try {
						this.writeCoil(bridge, this.getParent().getUnitId(), this.getStartAddress(), value);
						noOfWrittenCoils = 1;
					} catch (ModbusException e2) {
						throw new OpenemsException("Transaction failed: " + e.getMessage(), e2);
					}
				}
			}
		} else {
			this.log.warn("Unable to execute Write for ModbusElement [" + element + "]: No ModbusCoilElement!");
		}
		return noOfWrittenCoils;
	}

	private void writeCoil(AbstractModbusBridge bridge, int unitId, int startAddress, boolean value)
			throws OpenemsException, ModbusException {
		var request = new WriteCoilRequest(startAddress, value);
		var response = Utils.getResponse(request, unitId, bridge);

		// debug output
		switch (this.getLogVerbosity(bridge)) {
		case READS_AND_WRITES:
			bridge.logInfo(this.log, "FC5WriteCoil " //
					+ "[" + unitId + ":" + startAddress + "/0x" + Integer.toHexString(startAddress) + "]: " //
					+ value);
			break;
		case WRITES:
		case DEV_REFACTORING:
		case NONE:
			break;
		}

		if (!(response instanceof WriteCoilResponse)) {
			throw new OpenemsException("Unexpected Modbus response. Expected [WriteCoilResponse], got ["
					+ response.getClass().getSimpleName() + "]");
		}
	}

	@Override
	protected String getActiondescription() {
		return "FC5 WriteCoil";
	}
}
