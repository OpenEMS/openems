package io.openems.edge.energy.api.simulation;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.edge.energy.api.Environment.PRODUCTION;
import static io.openems.edge.energy.api.simulation.GocUtils.calculatePeriodDurationHourFromIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.meta.GridBuySoftLimit;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.energy.api.EnergyConstants;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.test.DummyPredictor;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.test.DummyTariffGridSellProvider;
import io.openems.edge.timeofusetariff.test.DummyTariffManager;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

class GlobalOptimizationContextTest {

	private static final TimeLeapClock CLOCK = new TimeLeapClock(Instant.ofEpochSecond(946684800), ZoneId.of("UTC"));
	private static final Instant NOW = Instant.now(CLOCK);

	private ComponentManager cm;
	private Meta meta;
	private Sum sum;
	private DummyPredictorManager predictorManager;
	private DummyTariffManager tariffManager;

	@BeforeEach
	void before() throws OpenemsNamedException {
		this.cm = new DummyComponentManager(CLOCK);
		this.meta = new DummyMeta()//
				.withGridBuySoftLimit(JSCalendar.Tasks.<GridBuySoftLimit>create(CLOCK)//
						.add(t -> t//
								.setStart("06:00") //
								.setDuration(Duration.ofHours(12)) //
								.addRecurrenceRule(b -> b //
										.setFrequency(DAILY)) //
								.setPayload(new GridBuySoftLimit(2000))) //
						.add(t -> t//
								.setPayload(new GridBuySoftLimit(6000))) //
						.build());
		this.sum = new DummySum() //
				.withEssCapacity(10000) //
				.withEssSoc(50) //
				.withEssMinDischargePower(-4000) //
				.withEssMaxDischargePower(5000);
		this.predictorManager = new DummyPredictorManager(//
				new DummyPredictor("predictor0", this.cm, Prediction.from(this.sum, //
						EnergyConstants.SUM_UNMANAGED_CONSUMPTION, NOW, new Integer[] { //
								4000, 8000, 6000, 2000, 3000, 5000, 7000, 9000, //
								4001, 8001, 6001, 2001, 3001, 5001, 7001, 9001, //
								4002, 8002, 6002, 2002, 3002, 5002, 7002, 9002, //
								4003, 8003, 6003, 2003, 3003, 5003, 7003, 9003, //
								4004, 8004, 6004, 2004, 3004, 5004, 7004, 9004, //
						}), EnergyConstants.SUM_UNMANAGED_CONSUMPTION), //
				new DummyPredictor("predictor1", this.cm, Prediction.from(this.sum, //
						EnergyConstants.SUM_PRODUCTION, NOW, new Integer[] { //
								8000, 9000, 10000, 11000, 7000, 4000, 3000, 5000, //
								8001, 9001, 10001, 11001, 7001, 4001, 3001, 5001, //
								8002, 9002, 10002, 11002, 7002, 4002, 3002, 5002, //
								8003, 9003, 10003, 11003, 7003, 4003, 3003, 5003, //
								8004, 9004, 10004, 11004, 7004, 4004, 3004, 5004, //
						}), EnergyConstants.SUM_PRODUCTION));

		final var tariffGridBuy = new DummyTimeOfUseTariffProvider(CLOCK, TimeOfUsePrices.from(Instant.now(CLOCK), //
				-11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, //
				11.1, 12.1, 13.1, 14.1, 15.1, 16.1, 17.1, 18.1, //
				11.2, 12.2, 13.2, 14.2, 15.2, 16.2, 17.2, 18.2, //
				11.3, 12.3, 13.3, 14.3, 15.3, 16.3, 17.3, 18.3, //
				11.4, 12.4, 13.4, 14.4, 15.4, 16.4, 17.4, 18.4 //
		));
		final var tariffGridSell = new DummyTariffGridSellProvider(CLOCK, TimeOfUsePrices.from(Instant.now(CLOCK), //
				-21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0, 28.0, //
				21.1, 22.1, 23.1, 24.1, 25.1, 26.1, 27.1, 28.1, //
				21.2, 22.2, 23.2, 24.2, 25.2, 26.2, 27.2, 28.2, //
				21.3, 22.3, 23.3, 24.3, 25.3, 26.3, 27.3, 28.3, //
				21.4, 22.4, 23.4, 24.4, 25.4, 26.4, 27.4, 28.4 //
		));
		this.tariffManager = new DummyTariffManager()//
				.withTariffGridBuyProvider(tariffGridBuy)//
				.withTariffGridSellProvider(tariffGridSell);
	}

	@Test
	void testComplete() {
		var goc = GlobalOptimizationContext.builder() //
				.setComponentManager(this.cm) //
				.setMeta(this.meta) //
				.setEnvironment(PRODUCTION) //
				.setEnergyScheduleHandlers(ImmutableList.of()) //
				.setSum(this.sum) //
				.setPredictorManager(this.predictorManager) //
				.setTariffManager(this.tariffManager) //
				.build();

		assertEquals(4000, goc.ess().maxChargePower());
		assertEquals(5000, goc.ess().maxDischargePower());
		assertEquals(28, goc.periods().size());

		final var p0 = goc.periods().get(0);
		assertEquals(1500, p0.gridBuySoftLimit().intValue());

		final int production = p0.data().production();
		assertEquals(2000, production);

		final var consumption = p0.data().consumption();
		assertTrue(consumption.isPresent());
		assertEquals(1000, consumption.get().actual());

		final var gridBuyPrice = p0.data().gridBuyPrice();
		assertTrue(gridBuyPrice.isPresent());
		assertEquals(-11.0, gridBuyPrice.get().actual(), 0.001);
		assertEquals(0.0, gridBuyPrice.get().normalized(), 0.001);
		assertEquals(14.7, gridBuyPrice.get().positiveShifted(), 0.001);

		final var gridSellPrice = p0.data().gridSellPrice();
		assertTrue(gridSellPrice.isPresent());
		assertEquals(-21.0, gridSellPrice.get().actual(), 0.001);
		assertEquals(0.0, gridSellPrice.get().normalized(), 0.001);
		assertEquals(24.7, gridSellPrice.get().positiveShifted(), 0.001);
	}

	@Test
	void testWithoutGridBuyPrices() {
		var goc = GlobalOptimizationContext.builder() //
				.setComponentManager(this.cm) //
				.setMeta(this.meta) //
				.setEnvironment(PRODUCTION) //
				.setEnergyScheduleHandlers(ImmutableList.of()) //
				.setSum(this.sum) //
				.setPredictorManager(this.predictorManager) //
				.setTariffManager(this.tariffManager//
						.withTariffGridBuyProvider(DummyTimeOfUseTariffProvider.empty(CLOCK))) //
				.build();

		assertEquals(4000 /* -W */, goc.ess().maxChargePower());
		assertEquals(5000 /* W */, goc.ess().maxDischargePower());
		assertEquals(28, goc.periods().size());

		final var p0 = goc.periods().get(0);
		assertEquals(1500 /* Wh */, p0.gridBuySoftLimit().intValue());

		final int production = p0.data().production();
		assertEquals(2000, production);

		final var consumption = p0.data().consumption();
		assertTrue(consumption.isPresent());
		assertEquals(1000, consumption.get().actual());

		final var gridBuyPrice = p0.data().gridBuyPrice();
		assertFalse(gridBuyPrice.isPresent());

		final var gridSellPrice = p0.data().gridSellPrice();
		assertTrue(gridSellPrice.isPresent());
		assertEquals(-21.0, gridSellPrice.get().actual(), 0.001);
		assertEquals(0.0, gridSellPrice.get().normalized(), 0.001);
		assertEquals(24.7, gridSellPrice.get().positiveShifted(), 0.001);
	}

	@Test
	void testCalculatePeriodDurationHourFromIndex() {
		assertEquals(24, calculatePeriodDurationHourFromIndex(ZonedDateTime.parse("2020-03-04T14:00:00.00Z")));
		assertEquals(24 + 3, calculatePeriodDurationHourFromIndex(ZonedDateTime.parse("2020-03-04T14:15:00.00Z")));
		assertEquals(24 + 2, calculatePeriodDurationHourFromIndex(ZonedDateTime.parse("2020-03-04T14:30:00.00Z")));
		assertEquals(24 + 1, calculatePeriodDurationHourFromIndex(ZonedDateTime.parse("2020-03-04T14:45:00.00Z")));
		assertEquals(24, calculatePeriodDurationHourFromIndex(ZonedDateTime.parse("2020-03-04T15:00:00.00Z")));
	}
}
