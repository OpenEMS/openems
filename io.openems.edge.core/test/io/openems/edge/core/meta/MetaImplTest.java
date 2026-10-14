package io.openems.edge.core.meta;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.types.CurrencyConfig.EUR;
import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.meta.GridSellLimit;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.types.Environment;
import io.openems.edge.common.meta.types.EnvironmentStatus;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;

class MetaImplTest {

	private static final UUID UID_1 = UUID.randomUUID();
	private static final UUID UID_2 = UUID.randomUUID();
	private static final UUID UID_3 = UUID.randomUUID();

	@Test
	void test() throws Exception {
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
						.setIsEssDischargeToGridAllowed(true) //
						.setGridConnectionPointFuseLimit(32) //
						.setGridFeedInLimitationType(GridFeedInLimitationType.DYNAMIC_LIMITATION) //
						.setMaximumGridFeedInLimit(12345) //
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
								.add(buildJsonObject() //
										.addProperty("@type", "Task") //
										.addProperty("uid", UID_3) //
										.add("openems.io:payload", buildJsonObject() //
												.addProperty("power", 10000) //
												.build()) //
										.build()) //
								.build().toString()) //
						.build());

		assertEquals(Environment.STABLE, sut.getEnvironment());
		assertEquals(EnvironmentStatus.STABLE.name(), //
				cm.getConfiguration(Meta.SINGLETON_SERVICE_PID, "?").getProperties().get("environmentStatus"));

		final var ots = sut.getGridBuyLimit().soft() //
				.getOneTasksBetween(clock.now(), clock.now().plusHours(48)).iterator();
		{
			// from 00:00 to 08:00: defined fallback task
			final var ot = ots.next();
			assertEquals(UID_3, ot.parentTask().uid());
			assertEquals("2020-01-01T00:00Z", ot.start().toString());
			assertEquals("PT8H", ot.duration().toString());
			assertEquals(10000, ot.payload().power());
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
			assertEquals(UID_3, ot.parentTask().uid());
			assertEquals("2020-01-01T09:00Z", ot.start().toString());
			assertEquals("PT1H", ot.duration().toString());
			assertEquals(10000, ot.payload().power());
		}
		{
			// from 10:00 to 11:00: second task; 50.000 curtailed to Grid-Buy-Hard-Limit
			final var ot = ots.next();
			assertEquals(UID_2, ot.parentTask().uid());
			assertEquals("2020-01-01T10:00Z", ot.start().toString());
			assertEquals("PT1H", ot.duration().toString());
			assertEquals(16170, ot.payload().power());
		}
		{
			// from 11:00: fallback task
			final var ot = ots.next();
			assertEquals(UID_3, ot.parentTask().uid());
			assertEquals("2020-01-01T11:00Z", ot.start().toString());
			assertEquals("PT21H", ot.duration().toString());
			assertEquals(10000, ot.payload().power());
		}

		// Validate Hard Limits
		assertEquals(16170, sut.getGridBuyLimit().hard().power());
		assertEquals(new GridSellLimit(new GridSellLimit.Hard(12345, 11728)), sut.getGridSellLimit());
		assertEquals(12345, sut.getGridSellHardLimit());
		assertEquals(11728, sut.getGridSellHardLimitWithBuffer());
		assertEquals(12345, sut.getEssDischargeToGridLimit());

		// Test Live Channels
		test //
				.next(new TestCase() //
						.output(Meta.ChannelId.GRID_BUY_SOFT_LIMIT, 10000)) //
				.next(new TestCase() //
						.timeleap(clock, 8, ChronoUnit.HOURS) //
						.output(Meta.ChannelId.GRID_BUY_SOFT_LIMIT, 6789)) //
				.next(new TestCase() //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(Meta.ChannelId.GRID_BUY_SOFT_LIMIT, 10000)) //
				.deactivate();
	}

	@Test
	void testEnvironmentStatusIsUpdatedWhenBetaComponentBindsAndUnbinds() throws Exception {
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);

		final var clock = createDummyClock();
		final var factory = ofBridgeImpl(//
				DummyBridgeHttpFactory::dummyEndpointFetcher, //
				() -> dummyBridgeHttpExecutor(clock, true));

		final var sut = new MetaImpl();
		final var test = new ComponentTest(sut) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("httpBridgeFactory", factory) //
				.activate(MyConfig.create() //
						.setCurrency(EUR) //
						.setEnvironment(Environment.BETA) //
						.build());

		assertEquals(EnvironmentStatus.BETA_MANUAL.name(), //
				cm.getConfiguration(Meta.SINGLETON_SERVICE_PID, "?").getProperties().get("environmentStatus"));
		assertEquals(Environment.BETA, sut.getEnvironment());

		final var betaComponent = mock(EvseChargePoint.class);
		sut.bindBetaComponent(betaComponent);
		assertEquals(EnvironmentStatus.BETA_ENFORCED.name(), //
				cm.getConfiguration(Meta.SINGLETON_SERVICE_PID, "?").getProperties().get("environmentStatus"));

		sut.unbindBetaComponent(betaComponent);
		assertEquals(EnvironmentStatus.BETA_MANUAL.name(), //
				cm.getConfiguration(Meta.SINGLETON_SERVICE_PID, "?").getProperties().get("environmentStatus"));
		assertEquals(Environment.BETA, sut.getEnvironment());

		test.deactivate();
	}

	@Test
	void testGetGridSellHardLimitWithZeroExportDynamicLimitation() throws Exception {
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);

		final var oem = new DummyOpenemsEdgeOem();
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		final var executor = dummyBridgeHttpExecutor(clock, true);
		final var factory = ofBridgeImpl(() -> fetcher, () -> executor);

		final var sut = new MetaImpl();
		new ComponentTest(sut) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("oem", oem) //
				.addReference("httpBridgeFactory", factory) //
				.activate(MyConfig.create() //
						.setCurrency(EUR) //
						.setGridConnectionPointFuseLimit(32) // → 22,170 W
						.setGridFeedInLimitationType(GridFeedInLimitationType.DYNAMIC_LIMITATION) //
						.setMaximumGridFeedInLimit(0) // Zero export
						.build());

		// Grid sell hard limit should be clamped to 0 (zero export enforced)
		assertEquals(0, sut.getGridSellHardLimit());
		assertEquals(Optional.of(0), sut.getConfiguredMaximumGridFeedInLimit());
	}

	@ParameterizedTest
	@CsvSource({ "-1", "-1000" })
	void testGetGridSellHardLimitWithInvalidDynamicLimitation(int configuredLimit) throws Exception {
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);

		final var oem = new DummyOpenemsEdgeOem();
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		final var executor = dummyBridgeHttpExecutor(clock, true);
		final var factory = ofBridgeImpl(() -> fetcher, () -> executor);

		final var sut = new MetaImpl();
		new ComponentTest(sut) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("oem", oem) //
				.addReference("httpBridgeFactory", factory) //
				.activate(MyConfig.create() //
						.setCurrency(EUR) //
						.setGridConnectionPointFuseLimit(32) // → 22,170 W
						.setGridFeedInLimitationType(GridFeedInLimitationType.DYNAMIC_LIMITATION) //
						.setMaximumGridFeedInLimit(configuredLimit) //
						.build());

		// Grid sell hard limit should fall back to fuse limit when no fixed limit is
		// set
		assertEquals(22170, sut.getGridSellHardLimit());
		assertEquals(Optional.empty(), sut.getConfiguredMaximumGridFeedInLimit());
	}

	@ParameterizedTest
	@CsvSource({ "12345,12345", "40000,22170" })
	void testGetGridSellHardLimitWithPositiveLimitDynamicLimitation(int configuredLimit, int hardLimit)
			throws Exception {
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);

		final var oem = new DummyOpenemsEdgeOem();
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		final var executor = dummyBridgeHttpExecutor(clock, true);
		final var factory = ofBridgeImpl(() -> fetcher, () -> executor);

		final var sut = new MetaImpl();
		new ComponentTest(sut) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("oem", oem) //
				.addReference("httpBridgeFactory", factory) //
				.activate(MyConfig.create() //
						.setCurrency(EUR) //
						.setGridConnectionPointFuseLimit(32) // → 22,170 W
						.setGridFeedInLimitationType(GridFeedInLimitationType.DYNAMIC_LIMITATION) //
						.setMaximumGridFeedInLimit(configuredLimit) //
						.build());

		// Grid sell hard limit should be the minimum of configured and fuse limits
		assertEquals(hardLimit, sut.getGridSellHardLimit());
		assertEquals(Optional.of(configuredLimit), sut.getConfiguredMaximumGridFeedInLimit());
	}

	@Test
	void testGetGridSellHardLimitWithNoLimitationType() throws Exception {
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);

		final var oem = new DummyOpenemsEdgeOem();
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		final var executor = dummyBridgeHttpExecutor(clock, true);
		final var factory = ofBridgeImpl(() -> fetcher, () -> executor);

		final var sut = new MetaImpl();
		new ComponentTest(sut) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("oem", oem) //
				.addReference("httpBridgeFactory", factory) //
				.activate(MyConfig.create() //
						.setCurrency(EUR) //
						.setGridConnectionPointFuseLimit(32) // → 22,170 W
						.setGridFeedInLimitationType(GridFeedInLimitationType.NO_LIMITATION) //
						.setMaximumGridFeedInLimit(5000) // Should be ignored
						.build());

		// Grid sell hard limit should be fuse limit (configured limit is ignored with
		// NO_LIMITATION)
		assertEquals(22170, sut.getGridSellHardLimit());
		assertEquals(Optional.empty(), sut.getConfiguredMaximumGridFeedInLimit());
	}

	@ParameterizedTest(name = "gridSellHardLimit={0} -> expected={1}")
	@CsvSource({ "1000,850", // 5% = 50 -> min buffer 150
			"10000,9500", // 5% = 500
			"3333,3166" // 5% = 166.65 -> round(3166.35) = 3166
	})
	void testGridSellLimitWithBuffer(int gridSellHardLimit, int expected) throws Exception {
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);

		final var clock = createDummyClock();
		final var factory = ofBridgeImpl(//
				DummyBridgeHttpFactory::dummyEndpointFetcher, //
				() -> dummyBridgeHttpExecutor(clock, true));

		final var sut = new MetaImpl();
		final var test = new ComponentTest(sut) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("httpBridgeFactory", factory) //
				.activate(MyConfig.create() //
						.setCurrency(EUR) //
						.setGridConnectionPointFuseLimit(32) //
						.setGridFeedInLimitationType(GridFeedInLimitationType.DYNAMIC_LIMITATION) //
						.setMaximumGridFeedInLimit(gridSellHardLimit) //
						.build());

		assertEquals(expected, sut.getGridSellLimit().hard().powerWithBuffer());
		assertEquals(expected, sut.getGridSellHardLimitWithBuffer());
		test.deactivate();
	}

	@Test
	public void testGetEssDischargeToGridLimitBlockedByConfig() throws Exception {
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);

		final var oem = new DummyOpenemsEdgeOem();
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		final var executor = dummyBridgeHttpExecutor(clock, true);
		final var factory = ofBridgeImpl(() -> fetcher, () -> executor);

		final var sut = new MetaImpl();
		new ComponentTest(sut) //
				.addReference("cm", cm) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("oem", oem) //
				.addReference("httpBridgeFactory", factory) //
				.activate(MyConfig.create() //
						.setCurrency(EUR) //
						.setIsEssDischargeToGridAllowed(false) //
						.setGridConnectionPointFuseLimit(32) //
						.setGridFeedInLimitationType(GridFeedInLimitationType.DYNAMIC_LIMITATION) //
						.setMaximumGridFeedInLimit(12345) //
						.build());

		assertEquals(0, sut.getEssDischargeToGridLimit());
	}
}
