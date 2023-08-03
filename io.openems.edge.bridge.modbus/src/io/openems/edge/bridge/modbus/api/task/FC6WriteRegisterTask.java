package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.AbstractSingleWordElement;

public class FC6WriteRegisterTask extends
		AbstractWriteTask.Single<WriteSingleRegisterRequest, WriteSingleRegisterResponse, AbstractSingleWordElement<?, ?>> {

	public FC6WriteRegisterTask(int startAddress, AbstractSingleWordElement<?, ?> element) {
		super("FC6WriteRegister", WriteSingleRegisterResponse.class, startAddress, element);
	}

	@Override
	protected WriteSingleRegisterRequest createModbusRequest() throws OpenemsException {
		var registers = this.element.getNextWriteValueAndReset();
		if (registers == null) {
			return null;
		}

		if (registers.length == 1 && registers[0] != null) {
			// found value -> write
			var register = registers[0];
			return new WriteSingleRegisterRequest(this.startAddress, register);

		} else {
			throw new OpenemsException("Expected exactly one register. Got [" + registers.length + "]");
		}
	}

	@Override
	protected String payloadToString(WriteSingleRegisterRequest request) {
		return ModbusUtils.registersToHexString(request.getRegister());
	}
}
