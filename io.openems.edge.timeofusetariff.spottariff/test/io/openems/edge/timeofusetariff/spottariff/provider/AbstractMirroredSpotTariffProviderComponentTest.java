package io.openems.edge.timeofusetariff.spottariff.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.openems.common.tariff.api.Tariff;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.utils.BehaviorSubject;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TariffComponent;

class AbstractMirroredSpotTariffProviderComponentTest {

	@Test
	void withoutUnderlyingProvider_delegatesToSafeDefaults() {
		var sut = new TestComponent();

		assertFalse(sut.isSpotTariffSupported(BiddingZone.GERMANY));
		assertNull(sut.getSpotTariff(BiddingZone.GERMANY));
		assertEquals(0, sut.getRanking());
		assertEquals(List.of(), sut.getInfosAboutLoadedTariffs().getValue());

		// must not throw, even without an underlying provider
		sut.ungetSpotTariff(mock(SpotTariff.class));
	}

	@Test
	void withUnderlyingProvider_delegatesAllCallsToProvider() {
		var provider = mock(SpotTariffProvider.class);
		var spotTariff = mock(SpotTariff.class);
		var infos = new BehaviorSubject<>(List.<SpotTariffProvider.LoadedTariffInfo>of());

		when(provider.isSpotTariffSupported(BiddingZone.GERMANY)).thenReturn(true);
		when(provider.getSpotTariff(BiddingZone.GERMANY)).thenReturn(spotTariff);
		when(provider.getInfosAboutLoadedTariffs()).thenReturn(infos);
		when(provider.getRanking()).thenReturn(42);

		var sut = new TestComponent();
		sut.setProvider(provider);

		assertTrue(sut.isSpotTariffSupported(BiddingZone.GERMANY));
		assertEquals(spotTariff, sut.getSpotTariff(BiddingZone.GERMANY));
		assertEquals(infos, sut.getInfosAboutLoadedTariffs());
		assertEquals(42, sut.getRanking());

		sut.ungetSpotTariff(spotTariff);
		verify(provider, times(1)).ungetSpotTariff(spotTariff);
	}

	private static class TestComponent extends AbstractMirroredSpotTariffProviderComponent {

		private SpotTariffProvider provider;

		protected TestComponent() {
			super(OpenemsComponent.ChannelId.values(), TariffComponent.ChannelId.values());
		}

		public void setProvider(SpotTariffProvider provider) {
			this.provider = provider;
		}

		@Override
		public SpotTariffProvider getUnderlyingProvider() {
			return this.provider;
		}

		@Override
		protected void setChannelsForUpdateFailures(List<TariffUpdateStatus> updateFailures) {
			// not relevant for this test
		}

		@Override
		protected String getChannelPrefixForTariff(Tariff tariff) {
			return "";
		}

		@Override
		public Clock getClock() {
			return Clock.systemUTC();
		}
	}
}