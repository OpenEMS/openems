package io.openems.backend.tariff.provider.smard;

import java.time.Clock;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.tariff.manager.BackendSpotTariffProvider;
import io.openems.backend.tariff.provider.AbstractMirroredSpotTariffProviderComponent;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.logger.DebugLoggable;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.tariff.smard.SmardTariffConfig;
import io.openems.common.tariff.smard.SmardTariffProvider;
import io.openems.common.types.BiddingZone;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Tariff.SpotTariff.SMARD", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BackendSmardTariffProviderImpl extends AbstractMirroredSpotTariffProviderComponent implements
		BackendSmardTariffProvider, SpotTariffProvider, BackendSpotTariffProvider, TariffProvider, DebugLoggable {

	@Reference
	private BridgeHttpFactory bridgeHttpFactory;

	private final Logger log = LoggerFactory.getLogger(BackendSmardTariffProviderImpl.class);

	protected Clock clock;

	private Config config;
	private SmardTariffProvider provider;

	public BackendSmardTariffProviderImpl() {
		super("SmardTariffProvider");
		this.clock = Clock.systemDefaultZone();
	}

	@Activate
	private void activate(Config config) {
		this.config = config;
		this.logInfo(this.log, "Activate [" + "biddingZones=" //
				+ (config.biddingZones().length == 0 //
						? "ALL"
						: Arrays.stream(config.biddingZones()) //
								.map(BiddingZone::name) //
								.collect(Collectors.joining(", ")))
				+ "]");

		final var providerConfig = new SmardTariffConfig(config.service_ranking());
		this.provider = new SmardTariffProvider(providerConfig, this.bridgeHttpFactory, this.clock, config.id());
		this.provider.activate();
	}

	@Deactivate
	private void deactivate() {
		if (this.provider != null) {
			this.provider.deactivate();
			this.provider = null;
		}
	}

	@Override
	public SpotTariffProvider getUnderlyingProvider() {
		return this.provider;
	}

	@Override
	public BiddingZone[] getFetchableBiddingZones() {
		if (this.config.biddingZones().length == 0) {
			return Arrays.stream(BiddingZone.values()) //
					.filter(x -> x.getEicCode() != null && !x.getEicCode().isEmpty()) //
					.toArray(BiddingZone[]::new);
		}
		return this.config.biddingZones();
	}
}