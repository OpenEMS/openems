package io.openems.edge.core.meta;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.types.CurrencyConfig.EUR;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.cycleSubscriber;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;

import org.junit.Test;

import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MetaImplTest {

	@Test
	public void test() throws Exception {
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);

		final var oem = new DummyOpenemsEdgeOem();

		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		final var executor = dummyBridgeHttpExecutor(clock, true);
		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> fetcher, //
				() -> executor//
		);

		new ComponentTest(new MetaImpl()) //
				.addReference("cm", cm) //
				.addReference("oem", oem) //
				.addReference("httpBridgeFactory", factory)//
				.activate(MyConfig.create() //
						.setCurrency(EUR) //
						.setGridFeedInLimitationType(GridFeedInLimitationType.NO_LIMITATION) //
						.build());
	}
}
