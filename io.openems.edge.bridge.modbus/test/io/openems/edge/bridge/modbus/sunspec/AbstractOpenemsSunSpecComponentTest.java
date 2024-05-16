package io.openems.edge.bridge.modbus.sunspec;

import static io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent.preprocessModbusElements;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.BridgeModbusTcpImpl;
import io.openems.edge.bridge.modbus.MyConfigTcp;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.sunspec.dummy.MyConfig;
import io.openems.edge.bridge.modbus.sunspec.dummy.MySunSpecComponentImpl;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TestUtils;

public class AbstractOpenemsSunSpecComponentTest {

	@Test
	public void testPreprocessModbusElements() throws OpenemsException {
		var elements = new ArrayList<ModbusElement>();
		var startAddress = 0;
		for (var point : DefaultSunSpecModel.S_701.points()) {
			var element = point.get().generateModbusElement(startAddress);
			startAddress += element.length;
			elements.add(element);
		}

		var sut = preprocessModbusElements(elements);
		assertEquals(2, sut.size()); // two sublists
		assertEquals(69, sut.get(0).size()); // first task
		assertEquals(1, sut.get(1).size()); // second task
		assertEquals(StringWordElement.class, sut.get(1).get(0).getClass()); // second task
	}

	private static final int UNIT_ID = 1;

	@Before
	public void changeLogLevel() {
		java.lang.System.setProperty("org.ops4j.pax.logging.DefaultServiceLog.level", "INFO");
	}

	private static ImmutableSortedMap.Builder<Integer, Integer> generateSunSpec() {
		var b = ImmutableSortedMap.<Integer, Integer>naturalOrder() //
				.put(40000, 0x5375) // SunSpec identifier
				.put(40001, 0x6e53) // SunSpec identifier

				.put(40002, 1) // SunSpec Block-ID
				.put(40003, 66); // Length of the SunSpec Block
		IntStream.range(40004, 40070).forEach(i -> b.put(i, 0));
		b //
				.put(40070, 123) // SunSpec Block-ID
				.put(40071, 24); // Length of the SunSpec Block
		IntStream.range(40072, 40096).forEach(i -> b.put(i, 0));
		b //
				.put(40096, 999) // SunSpec Block-ID
				.put(40097, 10) // Length of the SunSpec Block

				.put(40108, 702) // SunSpec Block-ID
				.put(40109, 50); // Length of the SunSpec Block
		IntStream.range(40110, 40160).forEach(i -> b.put(i, 0));
		return b;
	}

	// Disabled because of timing issues in CI
	@Ignore
	@Test
	public void test() throws Exception {
		var port = TestUtils.findRandomOpenPortOnAllLocalInterfaces();
		ModbusSlave slave = null;
		try {
			/*
			 * Open Modbus/TCP Slave
			 */
			slave = ModbusSlaveFactory.createTCPSlave(port, 1);
			slave.open();

			/*
			 * Instantiate Modbus-Bridge
			 */
			var bridge = new BridgeModbusTcpImpl();
			var testBridge = new ComponentTest(bridge) //
					.activate(MyConfigTcp.create() //
							.setId("modbus0") //
							.setIp("127.0.0.1") //
							.setPort(port) //
							.setInvalidateElementsAfterReadErrors(1) //
							.setLogVerbosity(LogVerbosity.READS_AND_WRITES_VERBOSE) //
							.build());

			var cmp = new MySunSpecComponentImpl();
			var testCmp = new ComponentTest(cmp) //
					.addReference("cm", new DummyConfigurationAdmin()) //
					.addReference("setModbus", bridge) //
					.activate(MyConfig.create() //
							.setId("cmp0") //
							.setModbusId("modbus0") //
							.setModbusUnitId(UNIT_ID) //
							.setReadFromModbusBlock(1) //
							.build());

			testWithEndOfMap(slave, bridge, testBridge, cmp, testCmp);
			testWithIllegalAddress(slave, bridge, testBridge, cmp, testCmp);

			assertFalse(cmp.getSunSpecChannel(DefaultSunSpecModel.S103.APH_A).isPresent());
			assertNotNull(cmp.getSunSpecChannelOrError(DefaultSunSpecModel.S702.W_MAX_RTG));

		} finally {
			if (slave != null) {
				slave.close();
			}
		}
	}

	private static void cycle(ComponentTest testBridge, ComponentTest testCmp, int count, int sleep) throws Exception {
		for (var i = 0; i < count; i++) {
			testBridge.next(new TestCase());
			testCmp.next(new TestCase());
			Thread.sleep(sleep); // TODO required?
		}
	}

	private static void testWithEndOfMap(ModbusSlave slave, BridgeModbusTcpImpl bridge, ComponentTest testBridge,
			MySunSpecComponentImpl cmp, ComponentTest testCmp) throws Exception {
		var processImage = new SimpleProcessImage(UNIT_ID);
		generateSunSpec() //
				.put(40160, 0xFFFF) // END_OF_MAP
				.put(40161, 0)// Behind the end
				.build() //
				.entrySet().stream() //
				.forEach(e -> processImage.addRegister(e.getKey(), new SimpleRegister(e.getValue())));
		slave.addProcessImage(UNIT_ID, processImage);

		cycle(testBridge, testCmp, 5, 100);

		assertEquals(58, cmp.channels().size());
	}

	private static void testWithIllegalAddress(ModbusSlave slave, BridgeModbusTcpImpl bridge, ComponentTest testBridge,
			MySunSpecComponentImpl cmp, ComponentTest testCmp) throws Exception {
		var processImage = new SimpleProcessImage(UNIT_ID);
		generateSunSpec() //
				.build() //
				.entrySet().stream() //
				.forEach(e -> processImage.addRegister(e.getKey(), new SimpleRegister(e.getValue())));
		slave.addProcessImage(UNIT_ID, processImage);

		cycle(testBridge, testCmp, 2, 100);
		cycle(testBridge, testCmp, 1, 2000); // wait for defective component
		cycle(testBridge, testCmp, 2, 100);

		assertEquals(58, cmp.channels().size());
	}
}
