package io.openems.edge.meter.opendtu;

import static io.openems.common.types.MeterType.PRODUCTION;
import org.junit.Test;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SinglePhase;


public class MeterOpenDtuImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterOpenDtuImpl()) //
				.activate(MyConfig.create() //
						.setId("meterOpenDTU0") //
						.setIp("127.0.0.1") //
						.setPhase(SinglePhase.L1) //
						.setType(PRODUCTION) //

						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
