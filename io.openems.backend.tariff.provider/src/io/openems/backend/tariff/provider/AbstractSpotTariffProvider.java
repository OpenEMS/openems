package io.openems.backend.tariff.provider;

import java.time.Clock;
import java.util.stream.Collectors;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.common.logger.DebugLoggable;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;

public abstract class AbstractSpotTariffProvider extends AbstractOpenemsBackendComponent
		implements SpotTariffProvider, DebugLoggable {

	protected AbstractSpotTariffProvider(String name) {
		super(name);
	}

	@Override
	public String debugLog() {
		final var logPrefix = "[" + this.getName() + "] ";

		var infos = this.getInfosAboutLoadedTariffs().getValue();
		if (infos.isEmpty()) {
			return logPrefix + "-";
		}

		final var now = Clock.systemDefaultZone().instant();
		return logPrefix + infos.stream() //
				.map(i -> i.tariff().getDebugLogOutput(now)) //
				.collect(Collectors.joining("|"));
	}
}
