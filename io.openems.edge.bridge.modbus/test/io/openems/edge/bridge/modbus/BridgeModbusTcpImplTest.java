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
	public void testSkipInterval() throws Exception {
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(CYCLE_TIME);

		var port = TestUtils.findRandomOpenPortOnAllLocalInterfaces();
		ModbusSlave slave = null;
		try {
			/*
			 * Open Modbus/TCP Slave
			 */
			slave = ModbusSlaveFactory.createTCPSlave(port, 1);
			var processImage = new SimpleProcessImage(UNIT_ID);
			slave.addProcessImage(UNIT_ID, processImage);
			slave.open();

			int NUM_TESTS, MAX_INTERVAL;

			// interval = 0, should not change original modbus behavior
			NUM_TESTS = 1;
			for (int i = 0; i < NUM_TESTS; i++) {
				var sut = new BridgeModbusTcpImpl();
				var device = new MyModbusComponent(DEVICE_ID, sut, UNIT_ID);
				var test = new ComponentTest(sut) //
						.addComponent(device) //
						.addReference("cycle", new DummyCycle(CYCLE_TIME));

				test.activate(MyConfigTcp.create() //
						.setId(MODBUS_ID) //
						.setIp("127.0.0.1") //
						.setPort(port) //
						.setInvalidateElementsAfterReadErrors(1) //
						.setLogVerbosity(LogVerbosity.DEBUG_LOG) //
						.setIntervalBetweenAccesses(0)
						.build());

				processImage.addRegister(100, new SimpleRegister(11));
				test.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(REGISTER_100, 11) //
						.output(MODBUS_COMMUNICATION_FAILED, false)); //

				processImage.addRegister(100, new SimpleRegister(22));
				test.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(REGISTER_100, 22) //
						.output(MODBUS_COMMUNICATION_FAILED, false)); //
				test.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output(REGISTER_100, 22) //
						.output(MODBUS_COMMUNICATION_FAILED, false)); //

				// Important! Otherwise, new sut cannot connect to the slave (only 1 slave thread)
				sut.deactivate();
			}
			// 0 <= interval < MAX_INTERVAL
			NUM_TESTS = 7;
			MAX_INTERVAL = CYCLE_TIME * 3;
			for (int i = 0; i < NUM_TESTS; i++) {
				var sut = new BridgeModbusTcpImpl();
				var device = new MyModbusComponent(DEVICE_ID, sut, UNIT_ID);
				var test = new ComponentTest(sut) //
						.addComponent(device) //
						.addReference("cycle", new DummyCycle(CYCLE_TIME));

				int interval = MAX_INTERVAL * i / NUM_TESTS;
				int skips = (int)Math.ceil(interval * 1.0 / CYCLE_TIME) - 1;

				System.out.println("Interval=" + interval + ", skips=" + skips);

				test.activate(MyConfigTcp.create()
						.setId(MODBUS_ID)
						.setIp("127.0.0.1")
						.setPort(port)
						.setInvalidateElementsAfterReadErrors(1)
						.setLogVerbosity(LogVerbosity.DEBUG_LOG)
						.setIntervalBetweenAccesses(interval)
						.build());

				processImage.addRegister(100, new SimpleRegister(111));
				test.next(new TestCase()
						.onAfterProcessImage(sleep)
						.output(REGISTER_100, 111)
						.output(MODBUS_COMMUNICATION_FAILED, false));

				processImage.addRegister(100, new SimpleRegister(222));
				for (int j = 0; j < skips; j++) {
					test.next(new TestCase()
							.onAfterProcessImage(sleep)
							.output(REGISTER_100, 111)
							.output(MODBUS_COMMUNICATION_FAILED, false));
				}
				test.next(new TestCase()
						.onAfterProcessImage(sleep)
						.output(REGISTER_100, 222)
						.output(MODBUS_COMMUNICATION_FAILED, false));

				processImage.addRegister(100, new SimpleRegister(333));
				for (int j = 0; j < skips; j++) {
					test.next(new TestCase()
							.onAfterProcessImage(sleep)
							.output(REGISTER_100, 222)
							.output(MODBUS_COMMUNICATION_FAILED, false));
				}
				test.next(new TestCase()
						.onAfterProcessImage(sleep)
						.output(REGISTER_100, 333)
						.output(MODBUS_COMMUNICATION_FAILED, false));

				sut.deactivate();
			}
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
