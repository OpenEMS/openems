package io.openems.edge.bridge.modbus.api.task;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteCoilResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * Implements a Write Single Coil abstractTask, using Modbus function code 5
 * (http://www.simplymodbus.ca/FC05.htm)
 */
public class FC5WriteCoilTask extends AbstractTask implements WriteTask {

	private final Logger log = LoggerFactory.getLogger(FC5WriteCoilTask.class);
	
	public FC5WriteCoilTask(int startAddress, AbstractModbusElement<?> element) {
		super(startAddress, element);
	}

	@Override
	public void executeWrite(AbstractModbusBridge bridge) throws OpenemsException {
		ModbusElement<?> element = this.getElements()[0];
		if (element instanceof ModbusCoilElement) {
			Optional<Boolean> valueOpt = ((ModbusCoilElement) element).getNextWriteValueAndReset();
			if (valueOpt.isPresent()) {
				// found value -> write
				try {
					/*
					 * First try
					 */
					this.writeCoil(bridge, this.getParent().getUnitId(), this.getStartAddress(), valueOpt.get());
				} catch (OpenemsException | ModbusException e) {
					/*
					 * Second try: with new connection
					 */
					bridge.closeModbusConnection();
					try {
						this.writeCoil(bridge, this.getParent().getUnitId(), this.getStartAddress(), valueOpt.get());
					} catch (ModbusException e2) {
						throw new OpenemsException("Transaction failed: " + e.getMessage(), e2);
					}
				}
			}
		} else {
			log.warn("Unable to execute Write for ModbusElement [" + element + "]: No ModbusCoilElement!");
		}
	}

	private void writeCoil(AbstractModbusBridge bridge, int unitId, int startAddress, boolean value)
			throws OpenemsException, ModbusException {

		WriteCoilRequest request = new WriteCoilRequest(startAddress, value);
		ModbusResponse response = Utils.getResponse(request, unitId, bridge);

		if (!(response instanceof WriteCoilResponse)) {
			throw new OpenemsException("Unexpected Modbus response. Expected [WriteCoilResponse], got ["
					+ response.getClass().getSimpleName() + "]");
		}
	}

	@Override
	protected String getActiondescription() {
		return "FC5 Write Coil ";
	}
}
