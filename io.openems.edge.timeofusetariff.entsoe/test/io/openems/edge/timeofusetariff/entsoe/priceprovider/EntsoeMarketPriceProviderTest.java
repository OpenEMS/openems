package io.openems.edge.timeofusetariff.entsoe.priceprovider;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.Clock;

import org.junit.Test;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.bridge.http.time.periodic.DummyPeriodicExecutorFactory;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.types.EntsoeBiddingZone;
import io.openems.edge.common.component.ClockProvider;

public class EntsoeMarketPriceProviderTest {
	@Test
	public void testPool() {
		var pool = this.createPool();

		var testConfig1and2 = new EntsoeConfiguration(EntsoeBiddingZone.GERMANY, "test");
		var instance1 = pool.get(testConfig1and2);
		var instance2 = pool.get(testConfig1and2);

		var testConfig3 = new EntsoeConfiguration(EntsoeBiddingZone.BELGIUM, "test");
		var instance3 = pool.get(testConfig3);

		assertEquals(instance1, instance2);
		assertNotEquals(instance1, instance3);

		assertEquals(2, pool.initializedProviders.size());

		pool.unget(instance3);
		assertEquals(1, pool.initializedProviders.size());

		pool.unget(instance2);
		assertEquals(1, pool.initializedProviders.size());
		pool.unget(instance1);
		assertEquals(0, pool.initializedProviders.size());

		instance1 = pool.get(testConfig1and2);
		assertEquals(1, pool.initializedProviders.size());
		pool.deactivate();
		assertEquals(0, pool.initializedProviders.size());
	}

	private EntsoeMarketPriceProviderPoolImpl createPool() {
		final var clock = createDummyClock();
		final var dummyOem = new DummyOpenemsEdgeOem();

		var dummyClockProvider = new ClockProvider() {
			@Override
			public Clock getClock() {
				return clock;
			}
		};

		return new EntsoeMarketPriceProviderPoolImpl(//
				dummyOem, //
				dummyClockProvider, //
				DummyBridgeHttpFactory.ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
						DummyBridgeHttpFactory::dummyBridgeHttpExecutor), //
				new DummyPeriodicExecutorFactory() //
		);
	}
}
