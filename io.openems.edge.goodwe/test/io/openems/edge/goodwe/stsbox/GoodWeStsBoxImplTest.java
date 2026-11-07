package io.openems.edge.goodwe.stsbox;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Hashtable;

import org.junit.jupiter.api.Test;
import org.osgi.framework.FrameworkUtil;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummySerialNumberStorage;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.MultiplexingMode;

class GoodWeStsBoxImplTest {

	@Test
	void testFilter() throws Exception {
		final var filter = FrameworkUtil.createFilter("(&(id=)(enabled=true))");

		final var properties = new Hashtable<String, Object>();
		properties.put("enabled", true);

		assertFalse(filter.match(properties));

		properties.put("id", "meter0");
		assertFalse(filter.match(properties));

		properties.put("id", "");
		assertTrue(filter.match(properties));
	}

	@Test
	void test() throws Exception {
		getComponentTest(60);
	}

	@Test
	void testRunTimeConversion() throws Exception {
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
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
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
