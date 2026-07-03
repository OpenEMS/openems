package io.openems.edge.braiinsos;

import org.junit.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SinglePhase;

public class BraiinsImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new ControllerBraiinsSingleImpl()) //
				.activate(MyConfig.create() //
						.setId("braiins0") //
						.setEnabled(false) // Avoid running the Executor
						.setMode(Mode.MANUAL_ON) //
						.setIp("127.0.0.1") //
						.setUsername("root") //
						.setPassword("") //
						.setPhase(SinglePhase.L1) //
						.setType(MeterType.CONSUMPTION_METERED) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
