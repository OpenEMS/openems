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
	private static final String DUMMY1_ALIAS = "This is Dummy1";
	private static final String DUMMY2_ID = "dummy2";
	private static final String DUMMY2_ALIAS = DUMMY2_ID;
	private static final String DUMMY10_ID = "dummy10";

	private static final String ANY_DUMMY = "dummy*";

	private static final ChannelAddress SUM_ESS_SOC = new ChannelAddress("_sum", "EssSoc");

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
		components.add(new DummyController(DUMMY1_ID, DUMMY1_ALIAS) {
			@Override
			public String debugLog() {
				return "def:uvw";
			}
		});
		components.add(new DummyController(DUMMY2_ID, DUMMY2_ALIAS) {
			@Override
			public String debugLog() {
				return "ghi:rst";
			}
		});
		components.add(new DummyController(DUMMY10_ID) {
			@Override
			public String debugLog() {
				return "jkl:opq";
			}
		});

		var sut = new ControllerDebugLogImpl();
		new ControllerTest(sut) //
				.addReference("components", components) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setShowAlias(true) //
						.setCondensedOutput(true) //
						.setAdditionalChannels(new String[] { //
								SUM_ESS_SOC.toString() //
						}) //
						.setIgnoreComponents(new String[] { //
								DUMMY0_ID //
						}) //
						.build()) //
				.next(new TestCase() //
						.input(SUM_ESS_SOC, 50));

		assertEquals(
				"_sum[Core.Sum|foo:bar|EssSoc:50 %] dummy1[This is Dummy1|def:uvw] dummy2[ghi:rst] dummy10[jkl:opq]",
				sut.getLogMessage());

	}

	@Test
	public void testWildcard() throws Exception {
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

		var sut = new ControllerDebugLogImpl();
		new ControllerTest(sut) //
				.addReference("components", components) //
				.addComponent(components.get(0)) //
				.addComponent(components.get(1)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setCondensedOutput(true) //
						.setAdditionalChannels(new String[] { //
								SUM_ESS_SOC.toString() //
						}) //
						.setIgnoreComponents(new String[] { //
								ANY_DUMMY //
						}) //
						.build()) //
				.next(new TestCase() //
						.input(SUM_ESS_SOC, 50));

		assertEquals("_sum[foo:bar|EssSoc:50 %]", sut.getLogMessage());

	}

}
