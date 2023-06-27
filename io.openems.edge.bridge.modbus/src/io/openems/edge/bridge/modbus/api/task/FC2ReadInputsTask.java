package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesResponse;
import com.ghgande.j2mod.modbus.util.BitVector;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Inputs Task, implementing Modbus function code 2
 * (http://www.simplymodbus.ca/FC02.htm).
 */
public class FC2ReadInputsTask extends AbstractReadDigitalInputsTask implements ReadTask {

	public FC2ReadInputsTask(int startAddress, Priority priority, AbstractModbusElement<?>... elements) {
		super(startAddress, priority, elements);
	}

	@Override
	protected BitVector getBitVector(ModbusResponse response) {
		var readInputDiscretesResponse = (ReadInputDiscretesResponse) response;
		return readInputDiscretesResponse.getDiscretes();
	}

	@Override
	protected String getActiondescription() {
		return "FC2ReadCoils";
	}

	@Override
	protected String getExpectedInputClassname() {
		return "ReadInputDiscretesResponse";
	}

	@Override
	protected ModbusRequest createModbusRequest(int startAddress, int length) {
		return new ReadInputDiscretesRequest(startAddress, length);
	}
}
