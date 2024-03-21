package io.openems.edge.bridge.modbus;

import org.junit.Ignore;
import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.common.test.TestUtils;

public class BridgeModbusTcpImplTest {

	private static final String MODBUS_ID = "modbus0";
	private static final String DEVICE_ID = "device0";
	private static final int UNIT_ID = 1;
	private static final int CYCLE_TIME = 100;

	private static final ChannelAddress REGISTER_100 = new ChannelAddress(DEVICE_ID, "Register100");
	private static final ChannelAddress MODBUS_COMMUNICATION_FAILED = new ChannelAddress(DEVICE_ID,
			"ModbusCommunicationFailed");

	@Ignore
	@Test
	public void test() throws Exception {
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(CYCLE_TIME);

		var port = TestUtils.findRandomOpenPortOnAllLocalInterfaces();
		ModbusSlave slave = null;
		try {
			/*
			 * Open Modbus/TCP Slave
			 */
			slave = ModbusSlaveFactory.createTCPSlave(port, 1);
			var processImage = new SimpleProcessImage(UNIT_ID);
			Register register100 = new SimpleRegister(123);
			processImage.addRegister(100, register100);
			slave.addProcessImage(UNIT_ID, processImage);
			slave.open();

			/*
			 * Instantiate Modbus-Bridge
			 */
			var sut = new BridgeModbusTcpImpl();
			var device = new MyModbusComponent(DEVICE_ID, sut, UNIT_ID);
			var test = new ComponentTest(sut) //
					.addComponent(device) //
					.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
					.activate(MyConfigTcp.create() //
							.setId(MODBUS_ID) //
							.setIp("127.0.0.1") //
							.setPort(port) //
							.setInvalidateElementsAfterReadErrors(1) //
							.setLogVerbosity(LogVerbosity.NONE) //
							.build());

			/*
			 * Successfully read Register
			 */
			test //
					.next(new TestCase() //
							.onAfterProcessImage(sleep)) //
					.next(new TestCase() //
							.onAfterProcessImage(sleep) //
							.output(REGISTER_100, 123) //
							.output(MODBUS_COMMUNICATION_FAILED, false)); //

			/*
			 * Reading Register fails after debounce of 10
			 */
			processImage.removeRegister(register100);
			for (var i = 0; i < 9; i++) {
				test.next(new TestCase() //
						.onAfterProcessImage(sleep));
			}
			test //
					.next(new TestCase() //
							.onAfterProcessImage(sleep) //
							.output(MODBUS_COMMUNICATION_FAILED, false)) //
					.next(new TestCase() //
							.onAfterProcessImage(sleep) //
							.output(MODBUS_COMMUNICATION_FAILED, true));

			/*
			 * Successfully read Register
			 */
			processImage.addRegister(100, register100);
			test //
					.next(new TestCase() //
							.onAfterProcessImage(sleep) //
							.output(REGISTER_100, 123) //
							.output(MODBUS_COMMUNICATION_FAILED, false)); //
		} finally {
			if (slave != null) {
				slave.close();
			}
		}
	}

	@Test
	public void testSkips() throws Exception {
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(CYCLE_TIME);

		var port = TestUtils.findRandomOpenPortOnAllLocalInterfaces();
		ModbusSlave slave = null;
		try {
			/*
			 * Open Modbus/TCP Slave
			 */
			slave = ModbusSlaveFactory.createTCPSlave(port, 1);
			var processImage = new SimpleProcessImage(UNIT_ID);
			Register register100 = new SimpleRegister(123);
			processImage.addRegister(100, register100);
			slave.addProcessImage(UNIT_ID, processImage);
			slave.open();

			/*
			 * Instantiate Modbus-Bridge
			 */
			var sut = new BridgeModbusTcpImpl();
			var device = new MyModbusComponent(DEVICE_ID, sut, UNIT_ID);
			var test = new ComponentTest(sut) //
					.addComponent(device) //
					.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
					.activate(MyConfigTcp.create() //
							.setId(MODBUS_ID) //
							.setIp("127.0.0.1") //
							.setPort(port) //
							.setInvalidateElementsAfterReadErrors(1) //
							.setLogVerbosity(LogVerbosity.NONE) //
							.setIntervalBetweenAccesses(150)
							.build());

			test.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(REGISTER_100, 123) //
						.output(MODBUS_COMMUNICATION_FAILED, false)); //
			register100 = new SimpleRegister(1234);
			processImage.addRegister(100, register100);
			test
				.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(REGISTER_100, 123) //
						.output(MODBUS_COMMUNICATION_FAILED, false)); //
		} finally {
			if (slave != null) {
				slave.close();
			}
		}
	}

	private static class MyModbusComponent extends DummyModbusComponent {

		public MyModbusComponent(String id, AbstractModbusBridge bridge, int unitId) throws OpenemsException {
			super(id, bridge, unitId, ChannelId.values());
		}

		public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
			REGISTER_100(Doc.of(OpenemsType.INTEGER)); //

			private final Doc doc;

			private ChannelId(Doc doc) {
				this.doc = doc;
			}

			@Override
			public Doc doc() {
				return this.doc;
			}
		}

		@Override
		protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
			return new ModbusProtocol(this, //
					new FC3ReadRegistersTask(100, Priority.HIGH, //
							m(ChannelId.REGISTER_100, new UnsignedWordElement(100) //
							))); //
		}

	}
}
