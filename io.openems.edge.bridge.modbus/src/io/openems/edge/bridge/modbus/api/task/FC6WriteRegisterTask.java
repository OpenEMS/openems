package io.openems.edge.bridge.modbus.api.task;

import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.AbstractWordElement;

public class FC6WriteRegisterTask extends
		AbstractWriteTask.Single<WriteSingleRegisterRequest, WriteSingleRegisterResponse, AbstractWordElement<?, ?>> {

	public FC6WriteRegisterTask(int startAddress, AbstractWordElement<?, ?> element) {
		super("FC6WriteRegister", WriteSingleRegisterResponse.class, startAddress, element);
	}

	@Override
	protected WriteSingleRegisterRequest createModbusRequest() throws OpenemsException {
		var valueOpt = this.element.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			var registers = valueOpt.get();

			if (registers.length == 1 && registers[0] != null) {
				// found value -> write
				var register = registers[0];
				return new WriteSingleRegisterRequest(this.startAddress, register);

			} else {
				throw new OpenemsException("Expected exactly one register. Got [" + registers.length + "]");
			}

		} else {
			return null;
		}
	}
}
