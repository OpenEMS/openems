package io.openems.edge.bridge.modbus;

import static io.openems.common.test.TestUtils.findRandomOpenPortOnAllLocalInterfaces;
import static io.openems.edge.bridge.modbus.api.ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED;
import io.openems.edge.common.test.DummyConfigurationAdmin;

import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingRunnable;
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

import java.util.Dictionary;
import java.util.Hashtable;

public class BridgeModbusTcpImplTest {

	private static final int UNIT_ID = 1;
	private static final int CYCLE_TIME = 100;

	@Test
	public void test() throws Exception {
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(CYCLE_TIME);

		var port = findRandomOpenPortOnAllLocalInterfaces();
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
			var test = new ComponentTest(sut) //
					.activate(MyConfigTcp.create() //
							.setId("modbus0") //
							.setIp("127.0.0.1") //
							.setPort(port) //
							.setInvalidateElementsAfterReadErrors(1) //
							.setLogVerbosity(LogVerbosity.NONE) //
							.build());
			test.addComponent(new MyModbusComponent("device0", sut, UNIT_ID));

			/*
			 * Successfully read Register
			 */
			test //
					.next(new TestCase() //
							.onAfterProcessImage(sleep)) //
					.next(new TestCase() //
							.onAfterProcessImage(sleep) //
							.output("device0", MyModbusComponent.ChannelId.REGISTER_100, 123) //
							.output("device0", MODBUS_COMMUNICATION_FAILED, false)); //

			/*
			 * Remove Protocol and unset channel values
			 */
			sut.removeProtocol("device0");

			test //
					.next(new TestCase() //
							.onAfterProcessImage(sleep)) //
					.next(new TestCase() //
							.onAfterProcessImage(sleep) //
							.output("device0", MyModbusComponent.ChannelId.REGISTER_100, null) //
							.output("device0", MODBUS_COMMUNICATION_FAILED, false)); //

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

			var cm = new DummyConfigurationAdmin();
			var cfg = cm.createFactoryConfiguration("Core.Cycle", null);
			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put("cycleTime", 100);
			cfg.update(properties);

			// interval = 0, should not change original modbus behavior
			int numTests = 1;
			for (int i = 0; i < numTests; i++) {
				var sut = new BridgeModbusTcpImpl();
				var device = new MyModbusComponent("device0", sut, UNIT_ID);
				var test = new ComponentTest(sut) //
						.addComponent(device) //
						.addReference("cm", cm);

				test.activate(MyConfigTcp.create() //
						.setId("modbus0") //
						.setIp("127.0.0.1") //
						.setPort(port) //
						.setInvalidateElementsAfterReadErrors(1) //
						.setLogVerbosity(LogVerbosity.NONE) //
						.setIntervalBetweenAccesses(0)
						.build());

				processImage.addRegister(100, new SimpleRegister(11));
				test.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output("device0", MyModbusComponent.ChannelId.REGISTER_100, 11) //
						.output("device0", MODBUS_COMMUNICATION_FAILED, false)); //

				processImage.addRegister(100, new SimpleRegister(22));
				test.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output("device0", MyModbusComponent.ChannelId.REGISTER_100, 22) //
						.output("device0", MODBUS_COMMUNICATION_FAILED, false)); //
				test.next(new TestCase() //
						.onAfterProcessImage(sleep) //
						.output("device0", MyModbusComponent.ChannelId.REGISTER_100, 22) //
						.output("device0", MODBUS_COMMUNICATION_FAILED, false)); //

				// Important! Otherwise, new sut cannot connect to the slave (only 1 slave thread)
				sut.deactivate();
			}
			// 0 <= interval < maxInterval
			numTests = 7;
			int maxInterval = CYCLE_TIME * 3;
			for (int i = 0; i < numTests; i++) {
				var sut = new BridgeModbusTcpImpl();
				var device = new MyModbusComponent("device0", sut, UNIT_ID);
				var test = new ComponentTest(sut) //
						.addComponent(device)
						.addReference("cm", cm);

				int interval = maxInterval * i / numTests;
				int skips = (int)Math.ceil(interval * 1.0 / CYCLE_TIME) - 1;

				System.out.println("Interval=" + interval + ", skips=" + skips);

				test.activate(MyConfigTcp.create()
						.setId("modbus0")
						.setIp("127.0.0.1")
						.setPort(port)
						.setInvalidateElementsAfterReadErrors(1)
						.setLogVerbosity(LogVerbosity.DEBUG_LOG)
						.setIntervalBetweenAccesses(interval)
						.build());

				processImage.addRegister(100, new SimpleRegister(111));
				test.next(new TestCase()
						.onAfterProcessImage(sleep)
						.output("device0", MyModbusComponent.ChannelId.REGISTER_100, 111)
						.output("device0", MODBUS_COMMUNICATION_FAILED, false));

				processImage.addRegister(100, new SimpleRegister(222));
				for (int j = 0; j < skips; j++) {
					test.next(new TestCase()
							.onAfterProcessImage(sleep)
							.output("device0", MyModbusComponent.ChannelId.REGISTER_100, 111)
							.output("device0", MODBUS_COMMUNICATION_FAILED, false));
				}
				test.next(new TestCase()
						.onAfterProcessImage(sleep)
						.output("device0", MyModbusComponent.ChannelId.REGISTER_100, 222)
						.output("device0", MODBUS_COMMUNICATION_FAILED, false));

				processImage.addRegister(100, new SimpleRegister(333));
				for (int j = 0; j < skips; j++) {
					test.next(new TestCase()
							.onAfterProcessImage(sleep)
							.output("device0", MyModbusComponent.ChannelId.REGISTER_100, 222)
							.output("device0", MODBUS_COMMUNICATION_FAILED, false));
				}
				test.next(new TestCase()
						.onAfterProcessImage(sleep)
						.output("device0", MyModbusComponent.ChannelId.REGISTER_100, 333)
						.output("device0", MODBUS_COMMUNICATION_FAILED, false));

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
		protected ModbusProtocol defineModbusProtocol() {
			return new ModbusProtocol(this, //
					new FC3ReadRegistersTask(100, Priority.HIGH, //
							m(ChannelId.REGISTER_100, new UnsignedWordElement(100)))); //
		}

	}
}
