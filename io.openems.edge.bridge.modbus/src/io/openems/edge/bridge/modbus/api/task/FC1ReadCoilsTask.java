package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;

/**
 * Implements a Read Coils abstractTask, implementing Modbus function code 1
 * (http://www.simplymodbus.ca/FC01.htm)
 */
public class FC1ReadCoilsTask extends AbstractReadDigitalInputsTask implements ReadTask {

	public FC1ReadCoilsTask(int startAddress, Priority priority, AbstractModbusElement<?>... elements) {
		super(startAddress, priority, elements);
	}

	@Override
	protected byte[] getBytes(ModbusResponse response) {
		ReadCoilsResponse coilsResponse = (ReadCoilsResponse) response;
		return coilsResponse.getCoils().getBytes();
	}

	@Override
	protected String getExpectedInputClassname() {
		return "ReadCoilsResponse";
	}

	@Override
	protected ModbusRequest getRequest() {
		return new ReadCoilsRequest(getStartAddress(), getLength());
	}

	@Override
	protected String getActiondescription() {
		return "FC1 Read Coils";
	}
}
