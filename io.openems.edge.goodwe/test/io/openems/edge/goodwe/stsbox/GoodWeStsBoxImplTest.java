package io.openems.edge.goodwe.stsbox;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.MultiplexingMode;

public class GoodWeStsBoxImplTest {

	@Test
	public void test() throws Exception {
		getComponentTest(60);
	}

	@Test
	public void testRunTimeConversion() throws Exception {
		getComponentTest(90) //
				.next(new TestCase() //
						.output(GoodWeStsBox.ChannelId.GENSET_RUN_TIME, 15));

		getComponentTest(245) //
				.next(new TestCase() //
						.output(GoodWeStsBox.ChannelId.GENSET_RUN_TIME, 41));

		getComponentTest(360).next(new TestCase().output(GoodWeStsBox.ChannelId.GENSET_RUN_TIME, 60));

	}

	private static ComponentTest getComponentTest(int runtime) throws Exception {
		return new ComponentTest(new GoodWeStsBoxImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("sts0") //
						.setModbusId("modbus0").setModbusUnitId(10) //
						.setGensetId("meter0") //
						.setPortMultiplexingMode(MultiplexingMode.GENSET) //
						.setRatedPower(10) //
						.setPreheatingTimeSeconds(60) //
						.setRuntime(runtime) //
						.setEnableCharge(EnableDisable.ENABLE) //
						.setChargeSocStart(45) //
						.setChargeSocEnd(65) //
						.setMaxPowerPercent(280) //
						.setVoltageUpperLimit(80) //
						.setFrequencyUpperLimit(65) //
						.setFrequencyLowerLimit(45) //
						.build());
	}
}
