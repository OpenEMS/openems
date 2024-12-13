package io.openems.edge.bridge.modbus.sunspec;

import static io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent.preprocessModbusElements;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

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
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S1;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S101;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S103;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S701;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S701_ACType;
import io.openems.edge.bridge.modbus.sunspec.Point.ModbusElementPoint;
import io.openems.edge.bridge.modbus.sunspec.dummy.MyConfig;
import io.openems.edge.bridge.modbus.sunspec.dummy.MySunSpecComponentImpl;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.ChannelId;
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
			if (point.get() instanceof ModbusElementPoint mep) { // without BitFieldPoints
				var element = mep.generateModbusElement(startAddress);
				startAddress += element.length;
				elements.add(element);
			}
		}

		var sut = preprocessModbusElements(elements);
		assertEquals(2, sut.size()); // two sublists
		assertEquals(66, sut.get(0).size()); // first task
		assertEquals(1, sut.get(1).size()); // second task
		assertEquals(StringWordElement.class, sut.get(1).get(0).getClass()); // second task
	}

	private static final int UNIT_ID = 1;

	@Before
	public void changeLogLevel() {
		java.lang.System.setProperty("org.ops4j.pax.logging.DefaultServiceLog.level", "INFO");
	}

	@Test
	public void testReadFromModbus() throws Exception {
		var sut = new MySunSpecComponentImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(40000, 0x5375, 0x6e53) // isSunSpec
						.withRegisters(40002, 1, 66) // Block 1
						.withRegisters(40004, //
								new int[] { 0x4D79, 0x204D, 0x616E, 0x7566, 0x6163, 0x7475, 0x7265, 0x7200, 0, 0, 0, 0,
										0, 0, 0, 0 }, // S1_MN
								new int[] { 0x4D79, 0x204D, 0x6F64, 0x656C, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // S1_MD
								range(0, 43).map(i -> 0).toArray()) //
						.withRegisters(40070, 101, 50) // Block 101
						.withRegisters(40072, //
								new int[] { 123, 234, 345, 456, 1 }, //
								range(0, 45).map(i -> 0).toArray()) //
						.withRegisters(40122, 103, 50) // Block 103
						.withRegisters(40124, //
								new int[] { 124, 235, 346, 457, 1 }, //
								range(0, 45).map(i -> 0).toArray()) //
						.withRegisters(40174, 701, 121) // Block 701
						.withRegisters(40176, //
								new int[] { 1, }, //
								range(0, 120).map(i -> 0).toArray()) //
						.withRegisters(40297, 702, 50) // Block 702
						.withRegisters(40299, //
								new int[] { 1, }, //
								range(0, 49).map(i -> 0).toArray()) //
						.withRegisters(40375, 0xFFFF) // END_OF_MAP
				) //
				.activate(MyConfig.create() //
						.setId("cmp0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(UNIT_ID) //
						.setReadFromModbusBlock(1) //
						.build())

				.next(new TestCase()) //
				.next(new TestCase() //
						.output(c(S1.MN), null) //
						.output(c(S1.MD), null))

				.next(new TestCase() //
						.output(c(S1.MN), "My Manufacturer") //
						.output(c(S1.MD), "My Model"))

				.next(new TestCase() //
						.output(c(S101.A), null) //
						.output(c(S101.APH_A), null) //
						.output(c(S103.A), null) //
						.output(c(S103.APH_A), null)) //

				.next(new TestCase() //
						.output(c(S101.A), 1230F) //
						.output(c(S101.APH_A), 2340F) //
						.output(c(S701.A_C_TYPE), S701_ACType.UNDEFINED)) //

				.next(new TestCase() //
						.output(c(S103.A), 1240F) //
						.output(c(S103.APH_A), 2350F) //
						.output(c(S701.A_C_TYPE), S701_ACType.SPLIT_PHASE)) //

				.deactivate();
	}

	private static ChannelId c(SunSpecPoint point) {
		return point.getChannelId();
	}

	private static ImmutableSortedMap.Builder<Integer, Integer> generateSunSpec() {
		var b = ImmutableSortedMap.<Integer, Integer>naturalOrder() //
				.put(40000, 0x5375) // SunSpec identifier
				.put(40001, 0x6e53) // SunSpec identifier

				.put(40002, 1) // SunSpec Block-ID
				.put(40003, 66); // Length of the SunSpec Block
		range(40004, 40070).forEach(i -> b.put(i, 0));
		b //
				.put(40070, 103) // SunSpec Block-ID
				.put(40071, 24); // Length of the SunSpec Block
		range(40072, 40096).forEach(i -> b.put(i, 0));
		b //
				.put(40096, 999) // SunSpec Block-ID
				.put(40097, 10) // Length of the SunSpec Block

				.put(40108, 702) // SunSpec Block-ID
				.put(40109, 50); // Length of the SunSpec Block
		range(40110, 40160).forEach(i -> b.put(i, 0));
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

			assertFalse(cmp.getSunSpecChannel(DefaultSunSpecModel.S101.APH_A).isPresent());
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

		assertEquals(101, cmp.channels().size());
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

		assertEquals(101, cmp.channels().size());
	}
}
