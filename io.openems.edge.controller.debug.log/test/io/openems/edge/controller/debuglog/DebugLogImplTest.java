package io.openems.edge.controller.debuglog;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.controller.test.DummyController;

public class DebugLogImplTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String DUMMY0_ID = "dummy0";
	private static final String DUMMY1_ID = "dummy1";

	private final static ChannelAddress SUM_ESS_SOC = new ChannelAddress("_sum", "EssSoc");

	@Test
	public void test() throws Exception {
		List<OpenemsComponent> components = new ArrayList<>();
		components.add(new DummySum() {
			@Override
			public String debugLog() {
				return "foo:bar";
			}
		});
		components.add(new DummyController(DUMMY0_ID) {
			@Override
			public String debugLog() {
				return "abc:xyz";
			}
		});
		components.add(new DummyController(DUMMY1_ID) {
			@Override
			public String debugLog() {
				return "def:uvw";
			}
		});

		DebugLogImpl sut = new DebugLogImpl();
		new ControllerTest(sut) //
				.addReference("components", components) //
				.addComponent(components.get(0)) //
				.addComponent(components.get(1)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setAdditionalChannels(new String[] { //
								SUM_ESS_SOC.toString() //
						}) //
						.setIgnoreComponents(new String[] { //
								DUMMY0_ID //
						}) //
						.build()) //
				.next(new TestCase() //
						.input(SUM_ESS_SOC, 50));

		assertEquals("_sum[foo:bar|EssSoc:50 %] dummy1[def:uvw]", sut.getLogMessage());

	}

}
