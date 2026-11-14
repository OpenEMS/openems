package io.openems.common.tariff.api.spottariff.dummy;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.ChronoUnit;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import io.openems.common.types.BiddingZone;
import io.openems.common.utils.TimeSpan;

class DummySpotTariffProviderTest {

	@Test
	void test() {
		final var clock = createDummyClock();
		var provider = new DummySpotTariffProvider();

		assertFalse(provider.isSpotTariffSupported(BiddingZone.GERMANY));
		provider.setRandomTariffData(BiddingZone.GERMANY,
				TimeSpan.between(clock.instant(), clock.instant().plus(2, ChronoUnit.DAYS)),
				Currency.getInstance("EUR"));
		assertTrue(provider.isSpotTariffSupported(BiddingZone.GERMANY));

		assertEquals(0, provider.getInfosAboutLoadedTariffs().getValue().size());
	}

}
