package io.openems.edge.controller.debuglog;

import static io.openems.edge.common.sum.Sum.ChannelId.ESS_SOC;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.controller.test.DummyController;

public class ControllerDebugLogImplTest {

	@Test
	public void test() throws Exception {
		List<OpenemsComponent> components = new ArrayList<>();
		components.add(new DummySum() {
			@Override
			public String debugLog() {
				return "foo:bar";
			}
		});
		components.add(new DummyController("dummy0") {
			@Override
			public String debugLog() {
				return "abc:xyz";
			}
		});
		components.add(new DummyController("dummy1", "This is Dummy1") {

			@Override
			public String debugLog() {
				return "def:uvw";
			}
		});
		components.add(new DummyController("dummy2", "dummy2") {
			@Override
			public String debugLog() {
				return "ghi:rst";
			}
		});
		components.add(new DummyController("dummy10") {
			@Override
			public String debugLog() {
				return "jkl:opq";
			}
		});

		var sut = new ControllerDebugLogImpl();
		new ControllerTest(sut) //
				.addReference("components", components) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setShowAlias(true) //
						.setCondensedOutput(true) //
						.setAdditionalChannels("_sum/EssSoc", "_sum/FooBar") //
						.setIgnoreComponents("dummy0") //
						.build()) //
				.next(new TestCase() //
						.input(ESS_SOC, 50));

		assertEquals(
				"_sum[Core.Sum|foo:bar|EssSoc:50 %|FooBar:CHANNEL_IS_NOT_DEFINED] dummy1[This is Dummy1|def:uvw] dummy2[ghi:rst] dummy10[jkl:opq]",
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
		components.add(new DummyController("dummy0") {
			@Override
			public String debugLog() {
				return "abc:xyz";
			}
		});
		components.add(new DummyController("dummy1") {

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
						.setId("ctrl0") //
						.setCondensedOutput(true) //
						.setAdditionalChannels("_sum/EssSoc") //
						.setIgnoreComponents("dummy*") //
						.build()) //
				.next(new TestCase() //
						.input(ESS_SOC, 50)) //
				.deactivate();

		assertEquals("_sum[foo:bar|EssSoc:50 %]", sut.getLogMessage());

	}

}
