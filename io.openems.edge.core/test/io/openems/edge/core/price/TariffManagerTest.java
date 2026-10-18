package io.openems.edge.core.price;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.openems.common.services.currency.DummyCurrencyExchange;
import io.openems.common.tariff.api.spottariff.dummy.DummySpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.utils.TimeSpan;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.test.DummyMeta;

class TariffManagerTest {

	@Nested
	@DisplayName("spotTariffTest()")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class SpotTariffTest {

		private static Clock clock;
		private static DummyCurrencyExchange currencyExchange;
		private static DummySpotTariffProvider spotTariffProvider;

		private static final double EUR_TO_CHF_CONVERSION_RATE = 1.5;
		private static final double STATIC_SPOT_PRICE = 12.0;

		@BeforeAll
		static void setUp() {
			clock = createDummyClock();

			currencyExchange = new DummyCurrencyExchange();
			currencyExchange.setExchangeRate(Currency.EUR.getJavaCurrency(), Currency.CHF.getJavaCurrency(),
					EUR_TO_CHF_CONVERSION_RATE);

			final var timeSpan = TimeSpan.between(clock.instant(), clock.instant().plus(2, ChronoUnit.HOURS));
			spotTariffProvider = new DummySpotTariffProvider(clock);
			spotTariffProvider.setStaticTariffData(BiddingZone.GERMANY, timeSpan, STATIC_SPOT_PRICE,
					Currency.EUR.getJavaCurrency());
		}

		private static TariffManagerImpl createTariffManager(Meta meta) {
			var tariffManager = new TariffManagerImpl();
			tariffManager.meta = meta;
			tariffManager.currencyExchange = currencyExchange;
			tariffManager.spotTariffProviders = List.of(spotTariffProvider);
			tariffManager.bindSpotTariffProvider(spotTariffProvider);

			return tariffManager;
		}

		@Test
		void testGetTariffData() {
			final var meta = new DummyMeta().withCurrency(Currency.EUR);
			final var tariffManager = createTariffManager(meta);

			final var spotTariffTracker = tariffManager.getSpotTariff(BiddingZone.GERMANY);
			assertEquals(//
					spotTariffProvider.getSpotTariff(BiddingZone.GERMANY).getTariffData().getValue(), //
					spotTariffTracker.getTariffData().getValue() //
			);

			tariffManager.ungetSpotTariff(spotTariffTracker);
		}

		@Test
		@Order(1)
		void testTariffProviderLifecycle() {
			assertEquals(0, spotTariffProvider.getInfosAboutLoadedTariffs().getValue().size());

			final var meta = new DummyMeta().withCurrency(Currency.EUR);
			final var tariffManager = createTariffManager(meta);

			final var tracker1 = tariffManager.getSpotTariff(BiddingZone.GERMANY);
			assertEquals(1, spotTariffProvider.getInfosAboutLoadedTariffs().getValue().size());
			final var tracker2 = tariffManager.getSpotTariff(BiddingZone.GERMANY);
			assertEquals(1, spotTariffProvider.getInfosAboutLoadedTariffs().getValue().size());
			tariffManager.ungetSpotTariff(tracker2);
			assertEquals(1, spotTariffProvider.getInfosAboutLoadedTariffs().getValue().size());
			tariffManager.ungetSpotTariff(tracker1);
			assertEquals(0, spotTariffProvider.getInfosAboutLoadedTariffs().getValue().size());
		}

		@Test
		void testCurrencyChange() {
			final var meta = new DummyMeta().withCurrency(Currency.CHF);
			final var tariffManager = createTariffManager(meta);

			final var spotTariffTracker = tariffManager.getSpotTariff(BiddingZone.GERMANY);
			assertEquals(//
					spotTariffProvider.getSpotTariff(BiddingZone.GERMANY).getTariffData().getValue()
							.withCurrency(Currency.CHF.getJavaCurrency(), currencyExchange), //
					spotTariffTracker.getTariffData().getValue() //
			);
			assertEquals(STATIC_SPOT_PRICE * EUR_TO_CHF_CONVERSION_RATE,
					spotTariffTracker.getTariffData().getValue().prices().getFirst());

			// Test change of exchange rates
			currencyExchange.setExchangeRate(Currency.EUR.getJavaCurrency(), Currency.CHF.getJavaCurrency(),
					EUR_TO_CHF_CONVERSION_RATE * 2);
			assertEquals(STATIC_SPOT_PRICE * EUR_TO_CHF_CONVERSION_RATE * 2,
					spotTariffTracker.getTariffData().getValue().prices().getFirst());
			currencyExchange.setExchangeRate(Currency.EUR.getJavaCurrency(), Currency.CHF.getJavaCurrency(),
					EUR_TO_CHF_CONVERSION_RATE);

			// Test change of currency
			meta.withCurrency(Currency.EUR);
			assertEquals(//
					spotTariffProvider.getSpotTariff(BiddingZone.GERMANY).getTariffData().getValue(), //
					spotTariffTracker.getTariffData().getValue() //
			);

			tariffManager.ungetSpotTariff(spotTariffTracker);
		}

		@Test
		void testTariffCombination() {
			final var meta = new DummyMeta().withCurrency(Currency.EUR);
			final var tariffManager = createTariffManager(meta);

			final var timeSpan = TimeSpan.between(clock.instant().plus(1, ChronoUnit.HOURS),
					clock.instant().plus(5, ChronoUnit.HOURS));

			final var secondTariffProvider = new DummySpotTariffProvider(clock) {
				@Override
				public int getRanking() {
					return 5;
				}
			};
			secondTariffProvider.setStaticTariffData(BiddingZone.GERMANY, timeSpan, STATIC_SPOT_PRICE + 2,
					Currency.CHF.getJavaCurrency());
			tariffManager.spotTariffProviders = List.of(spotTariffProvider, secondTariffProvider);
			tariffManager.bindSpotTariffProvider(secondTariffProvider);

			final var spotTariffTracker = tariffManager.getSpotTariff(BiddingZone.GERMANY);
			final var finalData = spotTariffTracker.getTariffData().getValue();
			assertEquals(TimeSpan.between(clock.instant(), timeSpan.getEndExclusive()),
					finalData.prices().getTimeSpan());

			final var orginalTariff = spotTariffProvider.getSpotTariff(BiddingZone.GERMANY);
			final var originalTariffData = orginalTariff.getTariffData().getValue();

			Instant time = clock.instant();
			while (time.isBefore(timeSpan.getStartInclusive())) {
				assertEquals(originalTariffData.prices().getAt(time), finalData.prices().getAt(time));
				time = time.plus(originalTariffData.prices().getResolution().getDuration());
			}

			final var expectedSecondPrice = currencyExchange
					.convertPrice(STATIC_SPOT_PRICE + 2, Currency.CHF.getJavaCurrency(), Currency.EUR.getJavaCurrency())
					.getAsDouble();
			time = timeSpan.getStartInclusive();
			while (time.isBefore(timeSpan.getEndExclusive())) {
				assertEquals(expectedSecondPrice, finalData.prices().getAt(time));
				time = time.plus(finalData.prices().getResolution().getDuration());
			}
		}
	}
}
