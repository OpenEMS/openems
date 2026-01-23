package io.openems.edge.bridge.modbus.sunspec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent.SunSpecModelEntry;
import io.openems.edge.bridge.modbus.sunspec.dummy.MyConfig;
import io.openems.edge.bridge.modbus.sunspec.dummy.MySunSpecComponentImpl;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.ComponentTest;

public class SunSpecStartTest {

	@Test
	public void testDefaultStartBehaviour() throws Exception {
		final var sut = new MySunSpecComponentImpl(List.of(//
				SunSpecModelEntry.create(DefaultSunSpecModel.S_1) //
						.setRequired(true) //
						.build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_101).build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_103).build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_701) //
						.setRequired(true) //
						.build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_702).build() //
		));

		final var bridge = new DummyModbusBridge("modbus0") //
				.withRegisters(40000, 0x5375, 0x6e53) // isSunSpec
				.withRegisters(40002, 1, 66) // Block 1
				.withRegisters(40070, 101, 50) // Block 101
				.withRegisters(40122, 103, 50) // Block 103
				.withRegisters(40174, 701, 121) // Block 701
				.withRegisters(40297, 702, 50) // Block 702
				.withRegisters(40349, 0xFFFF, 0); // END_OF_MAP

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", bridge) //
				.activate(MyConfig.create() //
						.setId("cmp0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setReadFromModbusBlock(1) //
						.build())
				.next(new AbstractComponentTest.TestCase()) //
				.next(new AbstractComponentTest.TestCase()) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_1));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_101));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_103));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_701));
					assertTrue(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_702));
					assertTrue(sut.areRequiredModelsRead());
					assertTrue(sut.isSunSpecInitializationCompleted());
				})) //
				.deactivate();
	}

	@Test
	public void testNotAllRequiredModelsRead() throws Exception {
		final var sut = new MySunSpecComponentImpl(List.of(//
				SunSpecModelEntry.create(DefaultSunSpecModel.S_1) //
						.setRequired(true) //
						.build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_101).build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_103).build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_701) //
						.setRequired(true) //
						.build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_702).build() //
		));

		final var bridge = new DummyModbusBridge("modbus0") //
				.withRegisters(40000, 0x5375, 0x6e53) // isSunSpec
				.withRegisters(40002, 1, 66) // Block 1
				.withRegisters(40070, 101, 50) // Block 101
				.withRegisters(40122, 103, 50) // Block 103
				.withRegisters(40174, 702, 50) // Block 702
				.withRegisters(40226, 0xFFFF, 0); // END_OF_MAP

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", bridge) //
				.activate(MyConfig.create() //
						.setId("cmp0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setReadFromModbusBlock(1) //
						.build())
				.next(new AbstractComponentTest.TestCase()) //
				.next(new AbstractComponentTest.TestCase()) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_1));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_101));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_103));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertFalse(sut.hasModelRead(DefaultSunSpecModel.S_701));
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_702));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertFalse(sut.areRequiredModelsRead());
					assertTrue(sut.isSunSpecInitializationCompleted());
				})) //
				.deactivate();
	}

	@Test
	public void testReinitializeSunSpecChannels() throws Exception {
		final var sut = new MySunSpecComponentImpl(List.of(//
				SunSpecModelEntry.create(DefaultSunSpecModel.S_1) //
						.setRequired(true) //
						.build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_101).build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_103).build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_701) //
						.setRequired(true) //
						.build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_702).build() //
		));

		final var bridge = new DummyModbusBridge("modbus0") //
				.withRegisters(40000, 0x5375, 0x6e53) // isSunSpec
				.withRegisters(40002, 1, 66) // Block 1
				.withRegisters(40070, 101, 50) // Block 101
				.withRegisters(40122, 103, 50) // Block 103
				.withRegisters(40174, 702, 50) // Block 702
				.withRegisters(40226, 0xFFFF, 0); // END_OF_MAP

		final var initialNumberOfChannels = sut.channels().size();

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", bridge) //
				.activate(MyConfig.create() //
						.setId("cmp0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setReadFromModbusBlock(1) //
						.build())
				.next(new AbstractComponentTest.TestCase()) //
				.next(new AbstractComponentTest.TestCase()) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_1));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_101));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_103));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertFalse(sut.hasModelRead(DefaultSunSpecModel.S_701));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_702));
					assertFalse(sut.areRequiredModelsRead());
					assertTrue(sut.isSunSpecInitializationCompleted());

					assertNotEquals(initialNumberOfChannels, sut.channels().size());
					sut.reinitializeSunSpecChannels();
					assertEquals(initialNumberOfChannels, sut.channels().size());

					bridge.withRegisters(40174, 701, 121) // Block 701
							.withRegisters(40297, 702, 50) // Block 702
							.withRegisters(40349, 0xFFFF, 0); // END_OF_MAP
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertFalse(sut.hasModelRead(DefaultSunSpecModel.S_1));
					assertFalse(sut.hasModelRead(DefaultSunSpecModel.S_101));
					assertFalse(sut.hasModelRead(DefaultSunSpecModel.S_103));
					assertFalse(sut.hasModelRead(DefaultSunSpecModel.S_702));
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_1));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_101));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_103));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_701));
					assertTrue(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_702));
					assertTrue(sut.areRequiredModelsRead());
					assertTrue(sut.isSunSpecInitializationCompleted());
				})) //
				.deactivate();
	}

	@Test
	public void testReinitializeSunSpecChannelsDuplicatedModels() throws Exception {
		final var sut = new MySunSpecComponentImpl(List.of(//
				SunSpecModelEntry.create(DefaultSunSpecModel.S_1) //
						.setRequired(true) //
						.build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_101).build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_103).build(), //
				SunSpecModelEntry.create(DefaultSunSpecModel.S_702) //
						.setRequired(true) //
						.build() //
		));

		final var bridge = new DummyModbusBridge("modbus0") //
				.withRegisters(40000, 0x5375, 0x6e53) // isSunSpec
				.withRegisters(40002, 1, 66) // Block 1
				.withRegisters(40070, 101, 50) // Block 101
				.withRegisters(40122, 103, 50) // Block 103
				.withRegisters(40174, 1, 50) // Block 1
				.withRegisters(40226, 702, 50) // Block 702
				.withRegisters(40278, 0xFFFF, 0); // END_OF_MAP

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", bridge) //
				.activate(MyConfig.create() //
						.setId("cmp0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setReadFromModbusBlock(1) //
						.build())
				.next(new AbstractComponentTest.TestCase()) //
				.next(new AbstractComponentTest.TestCase()) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_1));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_101));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_103));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertFalse(sut.hasModelRead(DefaultSunSpecModel.S_702));
					assertFalse(sut.areRequiredModelsRead());
					assertFalse(sut.isSunSpecInitializationCompleted());
				})) //
				.next(new AbstractComponentTest.TestCase().also(t -> {
					assertTrue(sut.hasModelRead(DefaultSunSpecModel.S_702));
					assertTrue(sut.areRequiredModelsRead());
					assertTrue(sut.isSunSpecInitializationCompleted());
				})) //
				.deactivate();
	}

}
