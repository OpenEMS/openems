package io.openems.edge.meter.openwb;
import static io.openems.common.types.MeterType.CONSUMPTION_METERED;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;



public class MeterOpenWbImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterOpenWbImpl()) //
				.activate(MyConfig.create() //
						.setId("meterOpenWB0") //
						.setIp("127.0.0.1") //
						.setPort(443) //
						.setType(CONSUMPTION_METERED) //

						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
