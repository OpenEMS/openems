package io.openems.edge.timeofusetariff.api;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;

import io.openems.common.tariff.api.Tariff;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.dummy.DummySpotTariff;
import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.types.TariffType;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.FunctionUtils;
import io.openems.common.utils.TimeRangeValues;
import io.openems.common.utils.TimeSpan;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

class AbstractTariffComponentTest {

	@Test
	void testChannelIds() throws Exception {
		final var clock = createDummyClock();
		final var tariffGermany = createSpotTariffWithStaticPrice(BiddingZone.GERMANY, 12.0, clock);
		final var tariffAustria = createSpotTariffWithStaticPrice(BiddingZone.AUSTRIA, 13.0, clock);

		final var component = new DummyTariffComponent(clock);
		new ComponentTest(component) //
				.activate(new EmptyConfigImpl("dummyTariff")) //
				.next(new TestCase("Add germany spot tariff") //
						.onBeforeProcessImage(() -> {
							assertFalse(component.channels().stream()
									.anyMatch(c -> c.channelId().name().startsWith("GERMANY_")));
							component.setLoadedTariffs(tariffGermany);
							assertTrue(component.channels().stream()
									.anyMatch(c -> c.channelId().name().startsWith("GERMANY_")));
						})) //

				.next(new TestCase("Test germany spot tariff channels") //
						.output(component.getChannelTracker(tariffGermany).getChannelIds().currentPrice, 12.0) //
						.output(component.getChannelTracker(tariffGermany).getChannelIds().currency, "EUR") //
				)

				.next(new TestCase("Add austria spot tariff").onBeforeProcessImage(() -> {
					assertFalse(
							component.channels().stream().anyMatch(c -> c.channelId().name().startsWith("AUSTRIA_")));
					component.setLoadedTariffs(tariffGermany, tariffAustria);
					assertTrue(
							component.channels().stream().anyMatch(c -> c.channelId().name().startsWith("AUSTRIA_")));
				}))

				.next(new TestCase("Test germany & austria spot tariff channels") //
						.output(component.getChannelTracker(tariffGermany).getChannelIds().currentPrice, 12.0) //
						.output(component.getChannelTracker(tariffAustria).getChannelIds().currentPrice, 13.0)) //

				.next(new TestCase("Remove austria again") //
						.onBeforeProcessImage(() -> {
							component.setLoadedTariffs(tariffGermany);
							assertFalse(component.channels().stream()
									.anyMatch(c -> c.channelId().name().startsWith("AUSTRIA_")));
						}));
	}

	private static SpotTariff createSpotTariffWithStaticPrice(BiddingZone biddingZone, double price, Clock clock) {
		final var timeSpan = TimeSpan.between(clock.instant(), clock.instant().plus(1, ChronoUnit.HOURS));
		final var prices = TimeRangeValues.builder(timeSpan, DurationUnit.ofMinutes(15), Double.class) //
				.setByPosition(0, price) //
				.fillMissingDataWithPreviousData() //
				.build();
		final var tariffData = new TariffData(new TariffType.DayAheadEpexSpot(biddingZone), prices,
				Currency.getInstance("EUR"), clock.instant(), clock.instant(), "");

		return new DummySpotTariff(biddingZone, new BehaviorSubject<>(tariffData),
				new BehaviorSubject<>(new TariffUpdateStatus.Successful(clock.instant())), FunctionUtils::doNothing);
	}

	@Nested
	@DisplayName("testTariffChannelId()")
	class TariffChannelIdTest {

		@Test
		void testAllChannelIds() throws IllegalAccessException {
			final var instance = new AbstractTariffComponent.TariffChannelId("DUMMY");
			final Set<ChannelId> allChannelIds = Set.of(instance.getAllChannelIds());
			final var declaredFields = AbstractTariffComponent.TariffChannelId.class.getDeclaredFields();

			for (var field : declaredFields) {
				if (ChannelId.class.isAssignableFrom(field.getType())) {
					var channelId = (ChannelId) field.get(instance);
					Assertions.assertTrue(allChannelIds.contains(channelId),
							"ChannelId field '" + field.getName() + "' is not present in getAllChannelIds()");
				}
			}
		}

	}

	private static class DummyTariffComponent extends AbstractTariffComponent implements EventHandler {

		private final Clock clock;
		private final BehaviorSubject<List<TariffProvider.LoadedTariffInfo>> infosAboutLoadedTariffs = new BehaviorSubject<>(
				List.of());

		private DummyTariffComponent(Clock clock) {
			super(OpenemsComponent.ChannelId.values(), TariffComponent.ChannelId.values());
			this.clock = clock;
		}

		public void setLoadedTariffs(Tariff... tariffs) {
			this.infosAboutLoadedTariffs.setValue(Arrays.stream(tariffs)
					.map(t -> new TariffProvider.LoadedTariffInfo(t, t.getLastUpdateStatus().getValue())) //
					.toList());
		}

		@Activate
		protected void activate(ComponentContext context, EmptyConfig config) {
			super.activate(context, config.id(), "", true);
		}

		@Override
		protected void setChannelsForUpdateFailures(List<TariffUpdateStatus> updateFailures) {
			// not needed for this unit test
		}

		@Override
		protected String getChannelPrefixForTariff(Tariff tariff) {
			if (tariff instanceof SpotTariff spotTariff) {
				return spotTariff.getBiddingZone().name();
			}
			return "DUMMY";
		}

		@Override
		public BehaviorSubject<List<TariffProvider.LoadedTariffInfo>> getInfosAboutLoadedTariffs() {
			return this.infosAboutLoadedTariffs;
		}

		@Override
		public Clock getClock() {
			return this.clock;
		}

		public TariffChannelTracker getChannelTracker(Tariff tariff) {
			return this.channelTrackers.get(tariff);
		}
	}

	private @interface EmptyConfig {
		@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
		String id() default "dummy";
	}

	@SuppressWarnings("all")
	private static class EmptyConfigImpl extends AbstractComponentConfig implements EmptyConfig {
		public EmptyConfigImpl(String id) {
			super(EmptyConfig.class, id);
		}
	}
}
