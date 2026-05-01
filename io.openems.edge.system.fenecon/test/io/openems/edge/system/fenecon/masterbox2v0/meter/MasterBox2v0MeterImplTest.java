package io.openems.edge.system.fenecon.masterbox2v0.meter;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.system.fenecon.masterbox2v0.DummyMasterBox2v0;
import io.openems.edge.system.fenecon.masterbox2v0.enums.StateEnergyMeter;

public class MasterBox2v0MeterImplTest {

	@Test
	public void test() throws Exception {

		var ioc = new DummyMasterBox2v0("ioc0") //
				.withVoltageL1EnergyMeter(2000) //
				.withVoltageL2EnergyMeter(2000) //
				.withVoltageL3EnergyMeter(2000) //
				.withCurrentL1EnergyMeter(3200) //
				.withCurrentL2EnergyMeter(3200) //
				.withCurrentL3EnergyMeter(3200) //
				.withActivePowerL1EnergyMeter(640) //
				.withActivePowerL2EnergyMeter(640) //
				.withActivePowerL3EnergyMeter(640) //
				.withTimeStampEnergyMeter(100000L) //
				.withStatusEnergyMeter(0);

		var meter = new MasterBox2v0MeterImpl();

		new ComponentTest(meter) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ioc", ioc) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setIocId("ioc0") //
						.build()) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.activateStrictMode() //
						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
						.output(MasterBox2v0Meter.ChannelId.TIME_STAMP, 100000L) //
						.output(MasterBox2v0Meter.ChannelId.STATUS, StateEnergyMeter.NO_ERROR) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 1920) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 640) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 640) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 640) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 2000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 2000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 2000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 2000) //
						.output(ElectricityMeter.ChannelId.CURRENT, 9600) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 3200) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 3200) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 3200) //
						.output(ElectricityMeter.ChannelId.FREQUENCY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
				);
	}
}
