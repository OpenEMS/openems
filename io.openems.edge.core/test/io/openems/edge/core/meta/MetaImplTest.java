package io.openems.edge.core.meta;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.types.CurrencyConfig.EUR;
import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.test.TestUtils.withValue;
import static org.junit.Assert.assertEquals;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.Test;

import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class MetaImplTest {

	private static final UUID UID_1 = UUID.randomUUID();
	private static final UUID UID_2 = UUID.randomUUID();

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
						.setGridSoftLimit(buildJsonArray() //
								.add(buildJsonObject() //
										.addProperty("@type", "Task") //
										.addProperty("uid", UID_1) //
										.addProperty("start", "08:00:00") //
										.addProperty("duration", "PT1H") //
										.add("recurrenceRules", buildJsonArray() //
												.add(buildJsonObject() //
														.addProperty("frequency", "daily") //
														.build()) //
												.build()) //
										.add("openems.io:payload", buildJsonObject() //
												.addProperty("power", 6789) //
												.build()) //
										.build()) //
								.add(buildJsonObject() //
										.addProperty("@type", "Task") //
										.addProperty("uid", UID_2) //
										.addProperty("start", "10:00:00") //
										.addProperty("duration", "PT1H") //
										.add("recurrenceRules", buildJsonArray() //
												.add(buildJsonObject() //
														.addProperty("frequency", "daily") //
														.build()) //
												.build()) //
										.add("openems.io:payload", buildJsonObject() //
												.addProperty("power", 50000) //
												.build()) //
										.build()) //
								.build().toString()) //
						.build());

		final var ots = sut.getGridBuySoftLimit() //
				.getOneTasksBetween(clock.now(), clock.now().plusHours(48)).iterator();
		{
			// from 00:00 to 08:00: fallback task
			final var ot = ots.next();
			assertEquals("2020-01-01T00:00Z", ot.start().toString());
			assertEquals("PT8H", ot.duration().toString());
			assertEquals(22170, ot.payload().power());
		}
		{
			// from 08:00 to 09:00: first task
			final var ot = ots.next();
			assertEquals(UID_1, ot.parentTask().uid());
			assertEquals("2020-01-01T08:00Z", ot.start().toString());
			assertEquals("PT1H", ot.duration().toString());
			assertEquals(6789, ot.payload().power());
		}
		{
			// from 09:00 to 10:00: fallback task
			final var ot = ots.next();
			assertEquals("2020-01-01T09:00Z", ot.start().toString());
			assertEquals("PT1H", ot.duration().toString());
			assertEquals(22170, ot.payload().power());
		}
		{
			// from 10:00 to 11:00: second task; 50.000 curtailed to Grid-Buy-Hard-Limit
			final var ot = ots.next();
			assertEquals(UID_2, ot.parentTask().uid());
			assertEquals("2020-01-01T10:00Z", ot.start().toString());
			assertEquals("PT1H", ot.duration().toString());
			assertEquals(22170, ot.payload().power());
		}
		{
			// from 11:00: fallback task
			final var ot = ots.next();
			assertEquals("2020-01-01T11:00Z", ot.start().toString());
			assertEquals("PT21H", ot.duration().toString());
			assertEquals(22170, ot.payload().power());
		}

		// Validate Hard Limits
		assertEquals(22170, sut.getGridBuyHardLimit());
		assertEquals(22170, sut.getGridSellHardLimit());

		withValue(sut, Meta.ChannelId.MAXIMUM_GRID_FEED_IN_LIMIT, 123456);
		assertEquals(22170, sut.getGridBuyHardLimit());

		withValue(sut, Meta.ChannelId.MAXIMUM_GRID_FEED_IN_LIMIT, 12345);
		assertEquals(12345, sut.getGridBuyHardLimit());

		// Test Live Channels
		test //
				.next(new TestCase() //
						.output(Meta.ChannelId.GRID_BUY_SOFT_LIMIT, 22170)) //
				.next(new TestCase() //
						.timeleap(clock, 8, ChronoUnit.HOURS) //
						.output(Meta.ChannelId.GRID_BUY_SOFT_LIMIT, 6789)) //
				.next(new TestCase() //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(Meta.ChannelId.GRID_BUY_SOFT_LIMIT, 22170)) //
				.deactivate();
	}
}
