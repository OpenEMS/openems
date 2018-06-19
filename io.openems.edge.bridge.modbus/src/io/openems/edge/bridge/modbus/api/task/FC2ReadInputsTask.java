package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesResponse;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;

/**
 * Implements a Read Inputs abstractTask, implementing Modbus function code 2
 * (http://www.simplymodbus.ca/FC02.htm)
 */
public class FC2ReadInputsTask extends AbstractReadDigitalInputsTask implements ReadTask {

	public FC2ReadInputsTask(int startAddress, Priority priority, AbstractModbusElement<?>... elements) {
		super(startAddress, priority, elements);
	}

	@Override
	protected byte[] getBytes(ModbusResponse response) {
			ReadInputDiscretesResponse readInputDiscretesResponse = (ReadInputDiscretesResponse) response;
			return readInputDiscretesResponse.getDiscretes().getBytes();		
	}
	
	@Override
	protected String getActiondescription() {		
		return "FC2 Read Coils";
	}

	@Override
	protected String getExpectedInputClassname() {
		return "ReadInputDiscretesResponse";
	}
	
	@Override
	protected ModbusRequest getRequest() {
		return new ReadInputDiscretesRequest(getStartAddress(), getLength());
	}
}
