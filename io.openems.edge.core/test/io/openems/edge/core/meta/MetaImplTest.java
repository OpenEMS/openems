package io.openems.edge.core.meta;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.types.CurrencyConfig.EUR;
import static io.openems.edge.common.test.TestUtils.withValue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

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
				() -> fetcher, //
				() -> executor//
		);

		final var sut = new MetaImpl();
		final var test = new ComponentTest(sut) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("oem", oem) //
				.addReference("httpBridgeFactory", factory)//
				.activate(MyConfig.create() //
						.setCurrency(EUR) //
						.setGridConnectionPointFuseLimit(32) //
						.setGridFeedInLimitationType(GridFeedInLimitationType.NO_LIMITATION) //
						.build());

		assertEquals(22170, sut.getGridBuyHardLimit());
		assertEquals(22170, sut.getGridSellHardLimit());

		withValue(sut, Meta.ChannelId.MAXIMUM_GRID_FEED_IN_LIMIT, 123456);
		assertEquals(22170, sut.getGridBuyHardLimit());

		withValue(sut, Meta.ChannelId.MAXIMUM_GRID_FEED_IN_LIMIT, 12345);
		assertEquals(12345, sut.getGridBuyHardLimit());

		test.deactivate();
	}
}
