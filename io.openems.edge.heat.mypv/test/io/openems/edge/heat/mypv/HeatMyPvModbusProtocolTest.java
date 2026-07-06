package io.openems.edge.heat.mypv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfo;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.ManagedHeatElement;
import io.openems.edge.meter.api.ElectricityMeter;

class HeatMyPvModbusProtocolTest {

	private HeatMyPvImpl sut;

	private static HeatMyPvImpl activatedSut(boolean readOnly) throws Exception {
		var sut = new HeatMyPvImpl();
		new ComponentTest(sut) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(readOnly) //
						.setMaxHeatPower(3000) //
						.build());
		return sut;
	}

	@BeforeEach
	void setUp() throws Exception {
		this.sut = activatedSut(true);
	}

	@AfterEach
	void tearDown() {
		this.sut.deactivate();
	}

	@Test
	void testActivePowerMappedToRegister1000() {
		assertEquals(new ChannelMetaInfo(1000),
				this.sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).getMetaInfo());
	}

	@Test
	void testTemperatureMappedToRegister1001() {
		assertEquals(new ChannelMetaInfo(1001), this.sut.channel(Heat.ChannelId.TEMPERATURE).getMetaInfo());
	}

	@Test
	void testVoltageL1MappedToRegister1061() {
		assertEquals(new ChannelMetaInfo(1061), this.sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L1).getMetaInfo());
	}

	@Test
	void testCurrentL1MappedToRegister1062() {
		assertEquals(new ChannelMetaInfo(1062), this.sut.channel(ElectricityMeter.ChannelId.CURRENT_L1).getMetaInfo());
	}

	@Test
	void testVoltageL2MappedToRegister1067() {
		assertEquals(new ChannelMetaInfo(1067), this.sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L2).getMetaInfo());
	}

	@Test
	void testCurrentL2MappedToRegister1068() {
		assertEquals(new ChannelMetaInfo(1068), this.sut.channel(ElectricityMeter.ChannelId.CURRENT_L2).getMetaInfo());
	}

	@Test
	void testVoltageL3MappedToRegister1072() {
		assertEquals(new ChannelMetaInfo(1072), this.sut.channel(ElectricityMeter.ChannelId.VOLTAGE_L3).getMetaInfo());
	}

	@Test
	void testCurrentL3MappedToRegister1073() {
		assertEquals(new ChannelMetaInfo(1073), this.sut.channel(ElectricityMeter.ChannelId.CURRENT_L3).getMetaInfo());
	}

	@Test
	void testActivePowerL1MappedToRegister1074() {
		assertEquals(new ChannelMetaInfo(1074),
				this.sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L1).getMetaInfo());
	}

	@Test
	void testActivePowerL2MappedToRegister1075() {
		assertEquals(new ChannelMetaInfo(1075),
				this.sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L2).getMetaInfo());
	}

	@Test
	void testActivePowerL3MappedToRegister1076() {
		assertEquals(new ChannelMetaInfo(1076),
				this.sut.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3).getMetaInfo());
	}

	@Test
	void testStatusMappedToRegister1077() {
		assertEquals(new ChannelMetaInfo(1077), this.sut.channel(Heat.ChannelId.STATUS).getMetaInfo());
	}

	@Test
	void testTargetActivePowerNotMappedInReadOnly() {
		assertNull(this.sut.channel(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER).getMetaInfo());
	}

	@Test
	void testTargetActivePowerMappedToRegister1000WhenWritable() throws Exception {
		var writableSut = activatedSut(false);
		assertEquals(new ChannelMetaInfo(1000),
				writableSut.channel(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER).getMetaInfo());
		writableSut.deactivate();
	}
}