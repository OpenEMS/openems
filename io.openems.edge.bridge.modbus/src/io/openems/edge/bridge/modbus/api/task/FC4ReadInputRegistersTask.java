package io.openems.edge.bridge.modbus.api.task;

import java.util.stream.Stream;

import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Implements a Read Input Register Task, implementing Modbus function code 4
 * (http://www.simplymodbus.ca/FC04.htm).
 */
public class FC4ReadInputRegistersTask
		extends AbstractReadRegistersTask<ReadInputRegistersRequest, ReadInputRegistersResponse> {

	public FC4ReadInputRegistersTask(int startAddress, Priority priority, ModbusElement... elements) {
		super("FC4ReadInputRegisters", ReadInputRegistersResponse.class, startAddress, priority, elements);
	}

	@Override
	protected ReadInputRegistersRequest createModbusRequest() {
		return new ReadInputRegistersRequest(this.startAddress, this.length);
	}

	@Override
	protected Register[] parseResponse(ReadInputRegistersResponse response) throws OpenemsException {
		return Stream.of(response.getRegisters()) //
				.map(r -> {
					var bs = r.toBytes();
					return new SimpleRegister(bs[0], bs[1]);
				}) //
				.toArray(Register[]::new);
	}

	@Override
	protected String payloadToString(ReadInputRegistersResponse response) {
		return ModbusUtils.registersToHexString(response.getRegisters());
	}
}