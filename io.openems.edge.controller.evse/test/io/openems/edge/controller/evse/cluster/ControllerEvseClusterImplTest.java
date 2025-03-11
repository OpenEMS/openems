package io.openems.edge.controller.evse.cluster;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.controller.evse.cluster.ControllerEvseClusterImpl.calculate;
import static io.openems.edge.evse.api.SingleThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.MINIMUM;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.SURPLUS;
import static io.openems.edge.evse.api.chargepoint.Mode.Actual.ZERO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.test.DummyControllerEvseSingle;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint.ApplyCharge;

public class ControllerEvseClusterImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		new ControllerTest(new ControllerEvseClusterImpl()) //
				.addReference("sum", new DummySum()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setDebugMode(true) //
						.setCtrlIds("ctrlEvseSingle0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void testCalculate() {
		var outputs = calculate(//
				new DummySum() //
						.withGridActivePower(-11000) //
						.withEssDischargePower(0), //
				List.of(//
						new DummyControllerEvseSingle("evse0") //
								.withParams(new Params(true, SURPLUS, 0, new Limit(THREE_PHASE, 6000, 16000),
										ImmutableList.of())), //
						new DummyControllerEvseSingle("evse1") //
								.withParams(new Params(true, MINIMUM, 0, new Limit(THREE_PHASE, 6000, 16000),
										ImmutableList.of())), //
						new DummyControllerEvseSingle("evse2") //
								.withParams(new Params(true, ZERO, 0, new Limit(THREE_PHASE, 6000, 16000), //
										ImmutableList.of()))),
				log -> doNothing());

		assertEquals(9942, ((ApplyCharge.SetCurrent) outputs.get(0).ac()).current());
		assertEquals(6000, ((ApplyCharge.SetCurrent) outputs.get(1).ac()).current());
		assertTrue(outputs.get(2).ac() instanceof ApplyCharge.Zero);
	}
}
