package io.openems.edge.meter.virtual.subtract;

import static io.openems.common.types.MeterType.GRID;

import java.util.List;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class VirtualSubtractMeterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new VirtualSubtractMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("minuend", new DummyElectricityMeter("meter1")) //
				.addReference("subtrahends", List.of(//
						new DummyElectricityMeter("meter2"), //
						new DummyManagedSymmetricEss("ess0"))) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setType(GRID) //
						.setAddToSum(true) //
						.setMinuendId("meter1") //
						.setSubtrahendsIds("meter2", "ess0") //
						.build()) //
				.next(new TestCase() //
						.input("meter1", ElectricityMeter.ChannelId.ACTIVE_POWER, 5_000) //
						.input("meter2", ElectricityMeter.ChannelId.ACTIVE_POWER, 2_000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 4_000) //
						.output("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, -1000)) //
				.next(new TestCase() //
						.input("meter1", ElectricityMeter.ChannelId.ACTIVE_POWER, null) //
						.input("meter2", ElectricityMeter.ChannelId.ACTIVE_POWER, 2_000) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 4_000) //
						.output("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, null)) //
				.next(new TestCase() //
						.input("meter1", ElectricityMeter.ChannelId.ACTIVE_POWER, 5_000) //
						.input("meter2", ElectricityMeter.ChannelId.ACTIVE_POWER, null) //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 4_000) //
						.output("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 1000));
	}
}