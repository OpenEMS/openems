package io.openems.edge.core.sum;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.types.MeterType.GRID;
import static io.openems.edge.common.test.TestUtils.withValue;
import static io.openems.edge.timeofusetariff.test.DummyTariffGridSellProvider.fromQuarterlyGridSellPrices;
import static io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider.fromQuarterlyPrices;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.MeterType;
import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;
import io.openems.edge.meter.test.DummyElectricityMeter;
import io.openems.edge.timeofusetariff.test.DummyTariffManager;

public class SumImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
		final var clock = createDummyClock();
		final var sut = new SumImpl();
		final var grid = new DummyElectricityMeter("meter0") //
				.withMeterType(GRID); //
		final var pv = new DummyElectricityMeter("meter1") //
				.withMeterType(MeterType.PRODUCTION); //
		final var evcs = new DummyManagedEvcs("evcs0", new DummyEvcsPower(new DisabledRampFilter())) //
				.withMeterType(MeterType.MANAGED_CONSUMPTION_METERED);
		final var tariffManager = new DummyTariffManager() //
				.withTariffGridBuyProvider(fromQuarterlyPrices(clock, 1.0, 1.1, 1.2)) //
				.withTariffGridSellProvider(fromQuarterlyGridSellPrices(clock, 2.0, 2.1, 2.2));
		final var test = new ComponentTest(sut) //
				.addComponent(grid) //
				.addComponent(pv) //
				.addComponent(evcs) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("tariffManager", tariffManager) //
				.activate(MyConfig.create() //
						.setGridMinActivePower(0) //
						.setIgnoreStateComponents() //
						.build()); //

		grid.withActivePower(-1000);
		pv.withActivePower(5555);
		evcs.withActivePower(1000);
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> sut.updateChannelsBeforeProcessImage()) //
				.output(Sum.ChannelId.GRID_ACTIVE_POWER, -1000) //
				.output(Sum.ChannelId.PRODUCTION_ACTIVE_POWER, 5555) //
				.output(Sum.ChannelId.UNMANAGED_PRODUCTION_ACTIVE_POWER, 5555) //
				.output(Sum.ChannelId.ESS_ACTIVE_POWER, null) //
				.output(Sum.ChannelId.ESS_DISCHARGE_POWER, null) //
				.output(Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 4555) //
				.output(Sum.ChannelId.UNMANAGED_CONSUMPTION_ACTIVE_POWER, 3555) //

				.output(Sum.ChannelId.GRID_BUY_PRICE, 1.0) //

				.output(Sum.ChannelId.GRID_MIN_ACTIVE_POWER, -1000) //
				.output(Sum.ChannelId.GRID_MAX_ACTIVE_POWER, 0) //
				.output(Sum.ChannelId.PRODUCTION_MAX_ACTIVE_POWER, 5555) //
				.output(Sum.ChannelId.CONSUMPTION_MAX_ACTIVE_POWER, 4555));

		grid.withActivePower(-2000);
		pv.withActivePower(6666);
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> sut.updateChannelsBeforeProcessImage()) //
				.output(Sum.ChannelId.GRID_ACTIVE_POWER, -2000) //
				.output(Sum.ChannelId.PRODUCTION_ACTIVE_POWER, 6666) //
				.output(Sum.ChannelId.UNMANAGED_PRODUCTION_ACTIVE_POWER, 6666) //
				.output(Sum.ChannelId.ESS_ACTIVE_POWER, null) //
				.output(Sum.ChannelId.ESS_DISCHARGE_POWER, null) //
				.output(Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 4666) //
				.output(Sum.ChannelId.UNMANAGED_CONSUMPTION_ACTIVE_POWER, 3666) //

				.output(Sum.ChannelId.GRID_MIN_ACTIVE_POWER, -2000) //
				.output(Sum.ChannelId.GRID_MAX_ACTIVE_POWER, 0) //
				.output(Sum.ChannelId.PRODUCTION_MAX_ACTIVE_POWER, 6666) //
				.output(Sum.ChannelId.CONSUMPTION_MAX_ACTIVE_POWER, 4666));

		grid.withActivePower(3000);
		test.next(new TestCase() //
				.onBeforeProcessImage(() -> sut.updateChannelsBeforeProcessImage()) //
				.output(Sum.ChannelId.GRID_ACTIVE_POWER, 3000) //
				.output(Sum.ChannelId.PRODUCTION_ACTIVE_POWER, 6666) //
				.output(Sum.ChannelId.UNMANAGED_PRODUCTION_ACTIVE_POWER, 6666) //
				.output(Sum.ChannelId.ESS_ACTIVE_POWER, null) //
				.output(Sum.ChannelId.ESS_DISCHARGE_POWER, null) //
				.output(Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 9666) //
				.output(Sum.ChannelId.UNMANAGED_CONSUMPTION_ACTIVE_POWER, 8666) //

				.output(Sum.ChannelId.GRID_MIN_ACTIVE_POWER, -2000) //
				.output(Sum.ChannelId.GRID_MAX_ACTIVE_POWER, 3000) //
				.output(Sum.ChannelId.PRODUCTION_MAX_ACTIVE_POWER, 6666) //
				.output(Sum.ChannelId.CONSUMPTION_MAX_ACTIVE_POWER, 9666) //

				.output(Sum.ChannelId.PRODUCTION_TO_CONSUMPTION_POWER, 6666) //
				.output(Sum.ChannelId.PRODUCTION_TO_GRID_POWER, 0) //
				.output(Sum.ChannelId.PRODUCTION_TO_ESS_POWER, 0) //
				.output(Sum.ChannelId.GRID_TO_CONSUMPTION_POWER, 3000) //
				.output(Sum.ChannelId.ESS_TO_CONSUMPTION_POWER, 0) //
				.output(Sum.ChannelId.GRID_TO_ESS_POWER, 0) //
		);
	}

	@Test
	public void testDebugLog() throws OpenemsException, Exception {
		final var sut = new SumImpl();
		assertEquals("State:Ok", sut.debugLog());

		withValue(sut, Sum.ChannelId.ESS_SOC, 50);
		assertEquals("State:Ok Ess SoC:50 %", sut.debugLog());

		withValue(sut, Sum.ChannelId.ESS_ACTIVE_POWER, 1234);
		withValue(sut, Sum.ChannelId.ESS_SOC, null);
		assertEquals("State:Ok Ess L:1234 W", sut.debugLog());

		withValue(sut, Sum.ChannelId.ESS_SOC, 50);
		assertEquals("State:Ok Ess SoC:50 %|L:1234 W", sut.debugLog());

		withValue(sut, Sum.ChannelId.GRID_ACTIVE_POWER, 5678);
		assertEquals("State:Ok Ess SoC:50 %|L:1234 W Grid:5678 W", sut.debugLog());

		withValue(sut, Sum.ChannelId.GRID_GENSET_ACTIVE_POWER, 555);
		assertEquals("State:Ok Ess SoC:50 %|L:1234 W Grid:5678 W Genset:555 W", sut.debugLog());

		withValue(sut, Sum.ChannelId.PRODUCTION_ACTIVE_POWER, 7777);
		assertEquals("State:Ok Ess SoC:50 %|L:1234 W Grid:5678 W Genset:555 W Production:7777 W", sut.debugLog());

		withValue(sut, Sum.ChannelId.PRODUCTION_AC_ACTIVE_POWER, 3333);
		withValue(sut, Sum.ChannelId.PRODUCTION_DC_ACTUAL_POWER, 4444);
		assertEquals(
				"State:Ok Ess SoC:50 %|L:1234 W Grid:5678 W Genset:555 W Production Total:7777 W,AC:3333 W,DC:4444 W",
				sut.debugLog());

		withValue(sut, Sum.ChannelId.CONSUMPTION_ACTIVE_POWER, 1111);
		assertEquals(
				"State:Ok Ess SoC:50 %|L:1234 W Grid:5678 W Genset:555 W Production Total:7777 W,AC:3333 W,DC:4444 W Consumption:1111 W",
				sut.debugLog());
	}
}
