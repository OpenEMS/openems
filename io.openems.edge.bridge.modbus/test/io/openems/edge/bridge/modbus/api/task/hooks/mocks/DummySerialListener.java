package io.openems.edge.bridge.modbus.api.task.hooks.mocks;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import io.openems.edge.bridge.modbus.test.ModbusSerialListenerMock;

import java.util.Arrays;

public class DummySerialListener extends ModbusSerialListenerMock {
	public DummySerialListener(AbstractSerialConnection serialCon) {
		super(serialCon);
	}

	@Override
	protected ModbusResponse handle(ModbusRequest request) throws Exception {
		return switch (request) {
			case ReadMultipleRegistersRequest req -> this.handle(req);
			default -> null;
		};
	}

	private ReadMultipleRegistersResponse handle(ReadMultipleRegistersRequest req) {
		ReadMultipleRegistersResponse response = new ReadMultipleRegistersResponse();

		var registers = new Register[req.getWordCount()];
		Arrays.fill(registers, new SimpleRegister(5));

		response.setRegisters(registers);
		return response;
	}
}
