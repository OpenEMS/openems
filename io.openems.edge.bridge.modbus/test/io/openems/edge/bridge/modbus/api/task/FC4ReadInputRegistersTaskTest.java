package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.DummyModbusComponent;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.common.taskmanager.Priority;

public class FC4ReadInputRegistersTaskTest {

	/**
	 * Custom implementation that only implements {@link InputRegister} and not
	 * {@link Register}. This is the difference to {@link SimpleInputRegister}.
	 */
	public static class MyInputRegister implements InputRegister {

		private final SimpleInputRegister delegate;

		public MyInputRegister(int value) {
			this.delegate = new SimpleInputRegister(value);
		}

		@Override
		public int getValue() {
			return this.delegate.getValue();
		}

		@Override
		public int toUnsignedShort() {
			return this.delegate.toUnsignedShort();
		}

		@Override
		public short toShort() {
			return this.delegate.toShort();
		}

		@Override
		public byte[] toBytes() {
			return this.delegate.toBytes();
		}

	}

	@Test
	public void test() throws OpenemsException {
		var component = new DummyModbusComponent();
		var value = new AtomicReference<Long>();
		var element20 = new UnsignedDoublewordElement(20);
		element20.onUpdateCallback(v -> value.set(v));
		var element22 = new UnsignedDoublewordElement(22);
		element22.onUpdateCallback(v -> value.set(v));
		var task = new FC4ReadInputRegistersTask(20, Priority.LOW, element20, element22);
		task.setParent(component);
		var request = task.createModbusRequest();
		var response = request.getResponse();
		response.setRegisters(new InputRegister[] { //
				new MyInputRegister(987), new MyInputRegister(654), //
				new MyInputRegister(321), new MyInputRegister(0) });

		assertEquals(
				"FC4ReadInputRegisters [device0;unitid=1;priority=LOW;ref=20/0x14;length=4;response=03db 028e 0141 0000]",
				task.toLogMessage(LogVerbosity.READS_AND_WRITES_VERBOSE, request, response));

		assertNull(value.get());
		task.handleResponse(element20, 0, response.getRegisters());
		assertEquals(Long.valueOf(64684686), value.get());
		task.handleResponse(element22, 2, response.getRegisters());
		assertEquals(Long.valueOf(21037056), value.get());
	}
}
